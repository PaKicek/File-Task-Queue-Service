package org.pakicek.filetaskqueue.service

import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.pakicek.filetaskqueue.domain.Task
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.pakicek.filetaskqueue.dto.CreateTaskRequest
import org.pakicek.filetaskqueue.exception.TaskNotFoundException
import org.pakicek.filetaskqueue.exception.TaskOperationException
import org.pakicek.filetaskqueue.repository.TaskRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.util.*

class TaskServiceTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var taskService: TaskService

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        taskService = TaskService(taskRepository)
    }

    @Test
    fun `createTask should create and save task`() {
        val request = CreateTaskRequest(
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            type = TaskType.DOCUMENT_PARSING,
            priority = 5
        )

        val savedTask = Task(
            id = 1L,
            fileName = request.fileName,
            filePath = request.filePath,
            fileSize = request.fileSize,
            mimeType = request.mimeType,
            type = request.type,
            priority = request.priority,
            status = TaskStatus.PENDING
        )

        every { taskRepository.save(any<Task>()) } returns savedTask

        val result = taskService.createTask(request)

        assertEquals(1L, result.id)
        assertEquals("test.pdf", result.fileName)
        assertEquals(TaskStatus.PENDING, result.status)
        assertEquals(TaskType.DOCUMENT_PARSING, result.type)
        assertEquals(5, result.priority)

        verify(exactly = 1) { taskRepository.save(any<Task>()) }
    }

    @Test
    fun `getTaskById should return task when exists`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PENDING
        )

        every { taskRepository.findById(taskId) } returns Optional.of(task)

        val result = taskService.getTaskById(taskId)

        assertEquals(taskId, result.id)
        assertEquals("test.pdf", result.fileName)
        verify(exactly = 1) { taskRepository.findById(taskId) }
    }

    @Test
    fun `getTaskById should throw exception when task not found`() {
        val taskId = 999L
        every { taskRepository.findById(taskId) } returns Optional.empty()

        val exception = assertThrows<TaskNotFoundException> {
            taskService.getTaskById(taskId)
        }

        assertTrue(exception.message!!.contains("999"))
        verify(exactly = 1) { taskRepository.findById(taskId) }
    }

    @Test
    fun `getTasks should return paginated tasks with filters`() {
        val tasks = listOf(
            Task(
                id = 1L,
                fileName = "file1.pdf",
                filePath = "/files/file1.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING,
                priority = 5
            ),
            Task(
                id = 2L,
                fileName = "file2.pdf",
                filePath = "/files/file2.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING,
                priority = 3
            )
        )
        val page = PageImpl(tasks, PageRequest.of(0, 20), 2)

        every {
            taskRepository.findAll(any<Specification<Task>>(), any<PageRequest>())
        } returns page

        val result = taskService.getTasks(
            status = TaskStatus.PENDING,
            type = null,
            createdAfter = null,
            createdBefore = null,
            page = 0,
            size = 20
        )

        assertEquals(2, result.content.size)
        assertEquals(0, result.pageNumber)
        assertEquals(20, result.pageSize)
        assertEquals(2L, result.totalElements)
        assertEquals(1, result.totalPages)

        verify(exactly = 1) { taskRepository.findAll(any<Specification<Task>>(), any<PageRequest>()) }
    }

    @Test
    fun `getTasks should return empty page when no tasks found`() {
        val emptyPage = PageImpl<Task>(emptyList(), PageRequest.of(0, 20), 0)

        every {
            taskRepository.findAll(any<Specification<Task>>(), any<PageRequest>())
        } returns emptyPage

        val result = taskService.getTasks(
            status = TaskStatus.COMPLETED,
            type = null,
            createdAfter = null,
            createdBefore = null,
            page = 0,
            size = 20
        )

        assertEquals(0, result.content.size)
        assertEquals(0L, result.totalElements)
        assertEquals(0, result.totalPages)
    }

    @Test
    fun `cancelTask should cancel pending task`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PENDING
        )

        every { taskRepository.findById(taskId) } returns Optional.of(task)
        every { taskRepository.save(any<Task>()) } returnsArgument 0

        val result = taskService.cancelTask(taskId)

        assertEquals(TaskStatus.CANCELLED, result.status)
        assertNotNull(result.completedAt)
        verify(exactly = 1) { taskRepository.save(any<Task>()) }
    }

    @Test
    fun `cancelTask should throw exception for processing task`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PROCESSING
        )

        every { taskRepository.findById(taskId) } returns Optional.of(task)

        val exception = assertThrows<TaskOperationException> {
            taskService.cancelTask(taskId)
        }

        assertTrue(exception.message!!.contains("cannot be cancelled"))
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `updateTaskStatus should update status to PROCESSING and set startedAt`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PENDING
        )

        every { taskRepository.findById(taskId) } returns Optional.of(task)
        every { taskRepository.save(any<Task>()) } returnsArgument 0

        taskService.updateTaskStatus(taskId, TaskStatus.PROCESSING)

        verify(exactly = 1) {
            taskRepository.save(match { savedTask ->
                savedTask.status == TaskStatus.PROCESSING && savedTask.startedAt != null
            })
        }
    }

    @Test
    fun `updateTaskStatus should update status to COMPLETED and set completedAt`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PROCESSING
        )

        every { taskRepository.findById(taskId) } returns Optional.of(task)
        every { taskRepository.save(any<Task>()) } returnsArgument 0

        taskService.updateTaskStatus(taskId, TaskStatus.COMPLETED, result = """{"status":"success"}""")

        verify(exactly = 1) {
            taskRepository.save(match { savedTask ->
                savedTask.status == TaskStatus.COMPLETED &&
                        savedTask.completedAt != null &&
                        savedTask.result == """{"status":"success"}"""
            })
        }
    }

    @Test
    fun `updateTaskStatus should update status to FAILED and set error message`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PROCESSING
        )

        every { taskRepository.findById(taskId) } returns Optional.of(task)
        every { taskRepository.save(any<Task>()) } returnsArgument 0

        taskService.updateTaskStatus(taskId, TaskStatus.FAILED, errorMessage = "Processing failed")

        verify(exactly = 1) {
            taskRepository.save(match { savedTask ->
                savedTask.status == TaskStatus.FAILED &&
                        savedTask.completedAt != null &&
                        savedTask.errorMessage == "Processing failed"
            })
        }
    }

    @Test
    fun `getPendingTasks should return ordered tasks`() {
        val tasks = listOf(
            Task(
                id = 1L,
                fileName = "file1.pdf",
                filePath = "/files/file1.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING,
                priority = 10
            ),
            Task(
                id = 2L,
                fileName = "file2.pdf",
                filePath = "/files/file2.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING,
                priority = 5
            )
        )

        every { taskRepository.findPendingTasksOrderedByPriority(TaskStatus.PENDING) } returns tasks

        val result = taskService.getPendingTasks()

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(10, result[0].priority)
        assertEquals(2L, result[1].id)
        assertEquals(5, result[1].priority)

        verify(exactly = 1) { taskRepository.findPendingTasksOrderedByPriority(TaskStatus.PENDING) }
    }

    @Test
    fun `updateTaskForRetry should reset task to PENDING status`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.FAILED,
            retryCount = 1
        )
        task.startedAt = java.time.LocalDateTime.now()
        task.completedAt = java.time.LocalDateTime.now()

        every { taskRepository.findById(taskId) } returns Optional.of(task)
        every { taskRepository.save(any<Task>()) } returnsArgument 0

        taskService.updateTaskForRetry(taskId, 2)

        verify(exactly = 1) {
            taskRepository.save(match { savedTask ->
                savedTask.status == TaskStatus.PENDING &&
                        savedTask.retryCount == 2 &&
                        savedTask.startedAt == null &&
                        savedTask.completedAt == null
            })
        }
    }
}