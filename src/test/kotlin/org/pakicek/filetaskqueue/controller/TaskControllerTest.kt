package org.pakicek.filetaskqueue.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.pakicek.filetaskqueue.dto.CreateTaskRequest

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class TaskControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
            registry.add("spring.liquibase.enabled", { "false" })
            registry.add("task-queue.scheduler.enabled", { "false" })
        }
    }

    @Test
    fun `POST tasks should create new task`() {
        val request = CreateTaskRequest(
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            fileSize = 1024,
            mimeType = "application/pdf",
            type = TaskType.DOCUMENT_PARSING,
            priority = 5
        )

        mockMvc.perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.fileName").value("test.pdf"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.type").value("DOCUMENT_PARSING"))
            .andExpect(jsonPath("$.priority").value(5))
    }

    @Test
    fun `POST tasks should return validation error for invalid request`() {
        val request = CreateTaskRequest(
            fileName = "",
            filePath = "",
            type = TaskType.DOCUMENT_PARSING
        )

        mockMvc.perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `GET tasks by id should return task`() {
        val request = CreateTaskRequest(
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING
        )

        val createResult = mockMvc.perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val taskId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        mockMvc.perform(get("/api/v1/tasks/$taskId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(taskId))
            .andExpect(jsonPath("$.fileName").value("test.pdf"))
    }

    @Test
    fun `GET tasks by id should return 404 for non-existent task`() {
        mockMvc.perform(get("/api/v1/tasks/99999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `GET tasks should return paginated list`() {
        for (i in 1..5) {
            val request = CreateTaskRequest(
                fileName = "test$i.pdf",
                filePath = "/files/test$i.pdf",
                type = TaskType.DOCUMENT_PARSING,
                priority = i
            )

            mockMvc.perform(
                post("/api/v1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
        }

        mockMvc.perform(
            get("/api/v1/tasks")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.totalElements").value(5))
    }

    @Test
    fun `GET tasks should filter by status`() {
        val request1 = CreateTaskRequest(
            fileName = "test1.pdf",
            filePath = "/files/test1.pdf",
            type = TaskType.DOCUMENT_PARSING
        )

        mockMvc.perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1))
        )

        mockMvc.perform(
            get("/api/v1/tasks")
                .param("status", "PENDING")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
    }

    @Test
    fun `GET tasks should filter by type`() {
        val request1 = CreateTaskRequest(
            fileName = "image.png",
            filePath = "/files/image.png",
            type = TaskType.IMAGE_PROCESSING
        )

        val request2 = CreateTaskRequest(
            fileName = "doc.pdf",
            filePath = "/files/doc.pdf",
            type = TaskType.DOCUMENT_PARSING
        )

        mockMvc.perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1))
        )

        mockMvc.perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2))
        )

        mockMvc.perform(
            get("/api/v1/tasks")
                .param("type", "IMAGE_PROCESSING")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content[0].type").value("IMAGE_PROCESSING"))
    }

    @Test
    fun `DELETE tasks should cancel pending task`() {
        val request = CreateTaskRequest(
            fileName = "test.pdf",
            filePath = "/files/test.pdf",
            type = TaskType.DOCUMENT_PARSING
        )

        val createResult = mockMvc.perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val taskId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        mockMvc.perform(delete("/api/v1/tasks/$taskId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.completedAt").exists())
    }

    @Test
    fun `POST start-processing should start task processing`() {
        mockMvc.perform(post("/api/v1/tasks/start-processing"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Task processing started"))
    }

    @Test
    fun `POST stop-processing should stop task processing`() {
        mockMvc.perform(post("/api/v1/tasks/stop-processing"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Task processing stopped"))
    }

    @Test
    fun `GET status should return processing status`() {
        mockMvc.perform(get("/api/v1/tasks/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.runningTasks").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }
}