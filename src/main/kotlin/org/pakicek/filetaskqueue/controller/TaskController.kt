package org.pakicek.filetaskqueue.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.pakicek.filetaskqueue.dto.CreateTaskRequest
import org.pakicek.filetaskqueue.dto.PageResponse
import org.pakicek.filetaskqueue.dto.TaskResponse
import org.pakicek.filetaskqueue.service.TaskExecutorService
import org.pakicek.filetaskqueue.service.TaskService
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Task Management", description = "APIs for managing file processing tasks")
class TaskController(
    private val taskService: TaskService,
    private val taskExecutorService: TaskExecutorService
) {
    private val logger = LoggerFactory.getLogger(TaskController::class.java)

    @PostMapping
    @Operation(summary = "Create a new task", description = "Creates a new file processing task")
    fun createTask(@Valid @RequestBody request: CreateTaskRequest): ResponseEntity<TaskResponse> {
        logger.info("POST /api/v1/tasks - Creating task: ${request.fileName}")
        val response = taskService.createTask(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Retrieves a task by its ID")
    fun getTaskById(
        @Parameter(description = "Task ID")
        @PathVariable id: Long
    ): ResponseEntity<TaskResponse> {
        logger.info("GET /api/v1/tasks/$id")
        val response = taskService.getTaskById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(summary = "Get all tasks", description = "Retrieves a paginated list of tasks with optional filters")
    fun getTasks(
        @Parameter(description = "Filter by task status")
        @RequestParam(required = false) status: TaskStatus?,

        @Parameter(description = "Filter by task type")
        @RequestParam(required = false) type: TaskType?,

        @Parameter(description = "Filter by creation date (after)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        createdAfter: LocalDateTime?,

        @Parameter(description = "Filter by creation date (before)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        createdBefore: LocalDateTime?,

        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<TaskResponse>> {
        logger.info("GET /api/v1/tasks - status=$status, type=$type, page=$page, size=$size")

        val response = taskService.getTasks(
            status = status,
            type = type,
            createdAfter = createdAfter,
            createdBefore = createdBefore,
            page = page,
            size = size
        )

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a task", description = "Cancels a pending task")
    fun cancelTask(
        @Parameter(description = "Task ID")
        @PathVariable id: Long
    ): ResponseEntity<TaskResponse> {
        logger.info("DELETE /api/v1/tasks/$id - Cancelling task")
        val response = taskService.cancelTask(id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/start-processing")
    @Operation(summary = "Start background processing", description = "Starts the background task processing")
    fun startProcessing(): ResponseEntity<Map<String, String>> {
        logger.info("POST /api/v1/tasks/start-processing")
        taskExecutorService.startProcessing()
        return ResponseEntity.ok(mapOf("message" to "Task processing started"))
    }

    @PostMapping("/stop-processing")
    @Operation(summary = "Stop background processing", description = "Stops the background task processing")
    fun stopProcessing(): ResponseEntity<Map<String, String>> {
        logger.info("POST /api/v1/tasks/stop-processing")
        taskExecutorService.stopProcessing()
        return ResponseEntity.ok(mapOf("message" to "Task processing stopped"))
    }

    @GetMapping("/status")
    @Operation(summary = "Get processing status", description = "Returns current processing status and statistics")
    fun getProcessingStatus(): ResponseEntity<Map<String, Any>> {
        logger.info("GET /api/v1/tasks/status")

        val status = mapOf(
            "runningTasks" to taskExecutorService.getRunningTasksCount(),
            "timestamp" to LocalDateTime.now()
        )

        return ResponseEntity.ok(status)
    }
}