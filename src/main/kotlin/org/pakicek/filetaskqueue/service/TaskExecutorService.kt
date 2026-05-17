package org.pakicek.filetaskqueue.service

import kotlinx.coroutines.*
import org.pakicek.filetaskqueue.config.TaskQueueProperties
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Service
class TaskExecutorService(
    private val taskService: TaskService,
    private val taskProcessor: TaskProcessor,
    private val taskQueueProperties: TaskQueueProperties,
    private val applicationScope: CoroutineScope,
    private val metricsService: MetricsService
) {
    private val logger = LoggerFactory.getLogger(TaskExecutorService::class.java)
    private val runningTasksCount = AtomicInteger(0)
    private val runningTasks = ConcurrentHashMap<Long, Job>()

    @Volatile
    private var isProcessing = false

    fun startProcessing() {
        if (isProcessing) {
            logger.warn("Task processing already started")
            return
        }

        isProcessing = true
        logger.info("Starting task processing with max concurrent tasks: ${taskQueueProperties.maxConcurrentTasks}")
        applicationScope.launch {
            while (isProcessing) {
                try {
                    processAvailableTasks()
                    delay(1000)
                } catch (e: Exception) {
                    logger.error("Error in task processing loop", e)
                }
            }
        }
    }

    fun stopProcessing() {
        logger.info("Stopping task processing")
        isProcessing = false
        runningTasks.values.forEach { it.cancel() }
        runningTasks.clear()
    }

    private suspend fun processAvailableTasks() {
        val availableSlots = taskQueueProperties.maxConcurrentTasks - runningTasksCount.get()
        if (availableSlots <= 0) {
            return
        }

        val pendingTasks = taskService.getPendingTasks().take(availableSlots)
        pendingTasks.forEach { task ->
            val job = applicationScope.launch {
                processTask(task.id!!)
            }
            runningTasks[task.id!!] = job
        }
    }

    private suspend fun processTask(taskId: Long) {
        runningTasksCount.incrementAndGet()
        val startTime = LocalDateTime.now()
        var taskType: TaskType? = null
        var success = false

        try {
            logger.info("Starting to process task id=$taskId")

            taskService.updateTaskStatus(taskId, TaskStatus.PROCESSING)
            val taskResponse = taskService.getTaskById(taskId)
            taskType = taskResponse.type
            val task = taskResponse.let {
                org.pakicek.filetaskqueue.domain.Task(
                    id = it.id,
                    fileName = it.fileName,
                    filePath = it.filePath,
                    fileSize = it.fileSize,
                    mimeType = it.mimeType,
                    type = it.type,
                    status = it.status,
                    priority = it.priority,
                    retryCount = it.retryCount
                )
            }

            val result = taskProcessor.processTask(task)
            taskService.updateTaskStatus(taskId, TaskStatus.COMPLETED, result = result)
            success = true

            logger.info("Task id=$taskId completed successfully")

        } catch (e: CancellationException) {
            logger.warn("Task id=$taskId was cancelled")
            throw e
        } catch (e: Exception) {
            logger.error("Task id=$taskId failed", e)
            handleTaskFailure(taskId, e)
        } finally {
            runningTasksCount.decrementAndGet()
            runningTasks.remove(taskId)

            if (taskType != null) {
                try {
                    metricsService.recordTaskCompletion(taskType, startTime, LocalDateTime.now(), success)
                } catch (e: Exception) {
                    logger.error("Failed to record metrics", e)
                }
            }
        }
    }

    private suspend fun handleTaskFailure(taskId: Long, exception: Exception) {
        val taskResponse = taskService.getTaskById(taskId)
        val retryConfig = taskQueueProperties.retry

        if (retryConfig.enabled && taskResponse.retryCount < retryConfig.maxAttempts) {
            val newRetryCount = taskResponse.retryCount + 1
            logger.info("Scheduling retry $newRetryCount/${retryConfig.maxAttempts} for task id=$taskId")
            delay(retryConfig.backoffDelayMs)

            taskService.updateTaskForRetry(taskId, newRetryCount)
        } else {
            logger.error("Task id=$taskId failed after ${taskResponse.retryCount} retries")
            taskService.updateTaskStatus(taskId, TaskStatus.FAILED, errorMessage = exception.message)
        }
    }

    fun getRunningTasksCount(): Int = runningTasksCount.get()
}