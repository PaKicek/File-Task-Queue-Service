package org.pakicek.filetaskqueue.service

import org.pakicek.filetaskqueue.domain.Task
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.pakicek.filetaskqueue.dto.CreateTaskRequest
import org.pakicek.filetaskqueue.dto.PageResponse
import org.pakicek.filetaskqueue.dto.TaskResponse
import org.pakicek.filetaskqueue.exception.TaskNotFoundException
import org.pakicek.filetaskqueue.exception.TaskOperationException
import org.pakicek.filetaskqueue.repository.TaskRepository
import org.pakicek.filetaskqueue.repository.TaskSpecifications
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TaskService(
    private val taskRepository: TaskRepository
) {
    private val logger = LoggerFactory.getLogger(TaskService::class.java)

    @Transactional
    fun createTask(request: CreateTaskRequest): TaskResponse {
        logger.info("Creating task: fileName = ${request.fileName}, type = ${request.type}")

        val task = Task(
            fileName = request.fileName,
            filePath = request.filePath,
            fileSize = request.fileSize,
            mimeType = request.mimeType,
            type = request.type,
            priority = request.priority,
            status = TaskStatus.PENDING
        )

        val savedTask = taskRepository.save(task)
        logger.info("Task created with id = ${savedTask.id}")

        return TaskResponse.from(savedTask)
    }

    @Transactional(readOnly = true)
    fun getTaskById(id: Long): TaskResponse {
        logger.debug("Getting task by id = $id")

        val task = taskRepository.findById(id).orElseThrow { TaskNotFoundException("Task with id = $id not found") }

        return TaskResponse.from(task)
    }

    @Transactional(readOnly = true)
    fun getTasks(
        status: TaskStatus?,
        type: TaskType?,
        createdAfter: LocalDateTime?,
        createdBefore: LocalDateTime?,
        page: Int,
        size: Int
    ): PageResponse<TaskResponse> {
        logger.debug("Getting tasks: status={}, type={}, page={}, size={}", status, type, page, size)

        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "priority").and(Sort.by(Sort.Direction.ASC, "createdAt"))
        )

        val spec = TaskSpecifications.combine(status, type, createdAfter, createdBefore)
        val taskPage = taskRepository.findAll(spec, pageable)

        return PageResponse(
            content = taskPage.content.map { TaskResponse.from(it) },
            pageNumber = taskPage.number,
            pageSize = taskPage.size,
            totalElements = taskPage.totalElements,
            totalPages = taskPage.totalPages,
            last = taskPage.isLast
        )
    }

    @Transactional
    fun cancelTask(id: Long): TaskResponse {
        logger.info("Cancelling task id = $id")

        val task = taskRepository.findById(id).orElseThrow { TaskNotFoundException("Task with id = $id not found") }
        if (!task.canBeCancelled()) {
            throw TaskOperationException("Task with id = $id cannot be cancelled. Current status: ${task.status}")
        }

        task.status = TaskStatus.CANCELLED
        task.completedAt = LocalDateTime.now()

        val savedTask = taskRepository.save(task)
        logger.info("Task id = $id cancelled")

        return TaskResponse.from(savedTask)
    }

    @Transactional(readOnly = true)
    fun getPendingTasks(): List<Task> {
        return taskRepository.findPendingTasksOrderedByPriority(TaskStatus.PENDING)
    }

    @Transactional
    fun updateTaskStatus(taskId: Long, newStatus: TaskStatus, errorMessage: String? = null, result: String? = null) {
        val task = taskRepository.findById(taskId).orElseThrow { TaskNotFoundException("Task with id = $taskId not found") }
        task.status = newStatus

        when (newStatus) {
            TaskStatus.PROCESSING -> task.startedAt = LocalDateTime.now()
            TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED -> {
                task.completedAt = LocalDateTime.now()
            }
            else -> {}
        }

        task.errorMessage = errorMessage
        task.result = result

        taskRepository.save(task)
    }

    @Transactional
    fun updateTaskForRetry(taskId: Long, retryCount: Int) {
        val task = taskRepository.findById(taskId).orElseThrow { TaskNotFoundException("Task with id = $taskId not found") }

        task.status = TaskStatus.PENDING
        task.retryCount = retryCount
        task.lastRetryAt = LocalDateTime.now()
        task.startedAt = null
        task.completedAt = null

        taskRepository.save(task)
        logger.info("Task id = $taskId scheduled for retry #$retryCount")
    }
}