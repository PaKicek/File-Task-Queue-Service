package org.pakicek.filetaskqueue.repository

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.pakicek.filetaskqueue.domain.Task
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@Testcontainers
@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("task_queue_test_db")
            withUsername("test")
            withPassword("test")
        }

        @DynamicPropertySource
        @JvmStatic
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto", { "create-drop" })
        }
    }

    @Test
    fun `should save and find task by id`() {
        val task = Task(
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PENDING
        )

        val savedTask = taskRepository.save(task)
        val foundTask = taskRepository.findById(savedTask.id!!)

        assertTrue(foundTask.isPresent)
        assertEquals("test.pdf", foundTask.get().fileName)
        assertEquals(TaskStatus.PENDING, foundTask.get().status)
    }

    @Test
    fun `findByStatus should return tasks with specific status`() {
        taskRepository.save(
            Task(
                fileName = "file1.pdf",
                filePath = "/files/file1.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING
            )
        )
        taskRepository.save(
            Task(
                fileName = "file2.pdf",
                filePath = "/files/file2.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PROCESSING
            )
        )
        taskRepository.save(
            Task(
                fileName = "file3.pdf",
                filePath = "/files/file3.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING
            )
        )

        val pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING, PageRequest.of(0, 10))

        assertEquals(2, pendingTasks.content.size)
        assertTrue(pendingTasks.content.all { it.status == TaskStatus.PENDING })
    }

    @Test
    fun `findByType should return tasks with specific type`() {
        taskRepository.save(
            Task(
                fileName = "image.png",
                filePath = "/files/image.png",
                type = TaskType.IMAGE_PROCESSING
            )
        )
        taskRepository.save(
            Task(
                fileName = "doc.pdf",
                filePath = "/files/doc.pdf",
                type = TaskType.DOCUMENT_PARSING
            )
        )
        taskRepository.save(
            Task(
                fileName = "video.mp4",
                filePath = "/files/video.mp4",
                type = TaskType.VIDEO_CONVERSION
            )
        )

        val imageTasks = taskRepository.findByType(TaskType.IMAGE_PROCESSING, PageRequest.of(0, 10))

        assertEquals(1, imageTasks.content.size)
        assertEquals(TaskType.IMAGE_PROCESSING, imageTasks.content[0].type)
    }

    @Test
    fun `findPendingTasksOrderedByPriority should return tasks ordered by priority`() {
        taskRepository.save(
            Task(
                fileName = "low.pdf",
                filePath = "/files/low.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING,
                priority = 1
            )
        )
        taskRepository.save(
            Task(
                fileName = "high.pdf",
                filePath = "/files/high.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING,
                priority = 10
            )
        )
        taskRepository.save(
            Task(
                fileName = "medium.pdf",
                filePath = "/files/medium.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING,
                priority = 5
            )
        )

        val orderedTasks = taskRepository.findPendingTasksOrderedByPriority(TaskStatus.PENDING)

        assertEquals(3, orderedTasks.size)
        assertEquals(10, orderedTasks[0].priority)
        assertEquals(5, orderedTasks[1].priority)
        assertEquals(1, orderedTasks[2].priority)
    }

    @Test
    fun `findStuckTasks should return tasks stuck in processing`() {
        val stuckTime = LocalDateTime.now().minusHours(1)
        val recentTime = LocalDateTime.now().minusMinutes(5)

        val stuckTask = Task(
            fileName = "stuck.pdf",
            filePath = "/files/stuck.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PROCESSING
        )
        stuckTask.startedAt = stuckTime
        taskRepository.save(stuckTask)

        val recentTask = Task(
            fileName = "recent.pdf",
            filePath = "/files/recent.pdf",
            type = TaskType.DOCUMENT_PARSING,
            status = TaskStatus.PROCESSING
        )
        recentTask.startedAt = recentTime
        taskRepository.save(recentTask)

        val timeout = LocalDateTime.now().minusMinutes(30)
        val stuckTasks = taskRepository.findStuckTasks(timeout)

        assertEquals(1, stuckTasks.size)
        assertEquals("stuck.pdf", stuckTasks[0].fileName)
    }

    @Test
    fun `countByStatus should return correct count`() {
        taskRepository.save(
            Task(
                fileName = "file1.pdf",
                filePath = "/files/file1.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING
            )
        )
        taskRepository.save(
            Task(
                fileName = "file2.pdf",
                filePath = "/files/file2.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.PENDING
            )
        )
        taskRepository.save(
            Task(
                fileName = "file3.pdf",
                filePath = "/files/file3.pdf",
                type = TaskType.DOCUMENT_PARSING,
                status = TaskStatus.COMPLETED
            )
        )

        val pendingCount = taskRepository.countByStatus(TaskStatus.PENDING)
        val completedCount = taskRepository.countByStatus(TaskStatus.COMPLETED)

        assertEquals(2, pendingCount)
        assertEquals(1, completedCount)
    }
}