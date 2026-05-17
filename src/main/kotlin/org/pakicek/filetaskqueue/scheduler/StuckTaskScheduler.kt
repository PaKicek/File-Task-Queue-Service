package org.pakicek.filetaskqueue.scheduler

import org.pakicek.filetaskqueue.config.TaskQueueProperties
import org.pakicek.filetaskqueue.repository.TaskRepository
import org.pakicek.filetaskqueue.service.TaskService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(prefix = "task-queue.scheduler", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class StuckTaskScheduler(
    private val taskRepository: TaskRepository,
    private val taskService: TaskService,
    private val taskQueueProperties: TaskQueueProperties
) {
    private val logger = LoggerFactory.getLogger(StuckTaskScheduler::class.java)

    @Scheduled(fixedDelayString = "\${task-queue.scheduler.check-interval-ms:60000}")
    fun checkStuckTasks() {
        logger.debug("Checking for stuck tasks...")

        val timeoutMinutes = taskQueueProperties.scheduler.stuckTaskTimeoutMinutes
        val timeout = LocalDateTime.now().minusMinutes(timeoutMinutes)
        val stuckTasks = taskRepository.findStuckTasks(timeout)
        if (stuckTasks.isNotEmpty()) {
            logger.warn("Found ${stuckTasks.size} stuck tasks")

            stuckTasks.forEach { task ->
                logger.warn("Resetting stuck task id=${task.id}, started at ${task.startedAt}")

                try {
                    taskService.updateTaskForRetry(task.id!!, task.retryCount + 1)
                } catch (e: Exception) {
                    logger.error("Failed to reset stuck task id=${task.id}", e)
                }
            }
        }
    }
}