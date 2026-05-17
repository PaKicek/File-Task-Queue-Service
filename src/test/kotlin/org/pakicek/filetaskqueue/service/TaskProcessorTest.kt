package org.pakicek.filetaskqueue.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.pakicek.filetaskqueue.domain.Task
import org.pakicek.filetaskqueue.domain.enums.TaskType

class TaskProcessorTest {

    private lateinit var taskProcessor: TaskProcessor

    @BeforeEach
    fun setup() {
        taskProcessor = TaskProcessor()
    }

    @Test
    fun `processTask should process IMAGE_PROCESSING task`() = runTest {
        val task = Task(
            id = 1L,
            fileName = "image.png",
            filePath = "/files/image.png",
            type = TaskType.IMAGE_PROCESSING
        )

        val result = taskProcessor.processTask(task)

        assertNotNull(result)
        assertTrue(result.contains("image"))
        assertTrue(result.contains("processed"))
    }

    @Test
    fun `processTask should process VIDEO_CONVERSION task`() = runTest {
        val task = Task(
            id = 1L,
            fileName = "video.mp4",
            filePath = "/files/video.mp4",
            type = TaskType.VIDEO_CONVERSION
        )

        val result = taskProcessor.processTask(task)

        assertNotNull(result)
        assertTrue(result.contains("video"))
        assertTrue(result.contains("processed"))
    }

    @Test
    fun `processTask should process DOCUMENT_PARSING task`() = runTest {
        val task = Task(
            id = 1L,
            fileName = "document.pdf",
            filePath = "/files/document.pdf",
            type = TaskType.DOCUMENT_PARSING
        )

        val result = taskProcessor.processTask(task)

        assertNotNull(result)
        assertTrue(result.contains("document"))
        assertTrue(result.contains("processed"))
    }

    @Test
    fun `processTask should process ARCHIVE_EXTRACTION task`() = runTest {
        val task = Task(
            id = 1L,
            fileName = "archive.zip",
            filePath = "/files/archive.zip",
            fileSize = 10240L,
            type = TaskType.ARCHIVE_EXTRACTION
        )

        val result = taskProcessor.processTask(task)

        assertNotNull(result)
        assertTrue(result.contains("archive"))
        assertTrue(result.contains("processed"))
    }

    @Test
    fun `processTask should process DATA_IMPORT task`() = runTest {
        val task = Task(
            id = 1L,
            fileName = "data.csv",
            filePath = "/files/data.csv",
            type = TaskType.DATA_IMPORT
        )

        val result = taskProcessor.processTask(task)

        assertNotNull(result)
        assertTrue(result.contains("data_import"))
        assertTrue(result.contains("processed"))
    }

    @Test
    fun `processTask should process CUSTOM task`() = runTest {
        val task = Task(
            id = 1L,
            fileName = "custom.dat",
            filePath = "/files/custom.dat",
            type = TaskType.CUSTOM
        )

        val result = taskProcessor.processTask(task)

        assertNotNull(result)
        assertTrue(result.contains("custom"))
        assertTrue(result.contains("processed"))
    }
}