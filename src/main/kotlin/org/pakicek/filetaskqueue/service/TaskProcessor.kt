package org.pakicek.filetaskqueue.service

import kotlinx.coroutines.delay
import org.pakicek.filetaskqueue.domain.Task
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class TaskProcessor {
    private val logger = LoggerFactory.getLogger(TaskProcessor::class.java)

    suspend fun processTask(task: Task): String {
        logger.info("Processing task id=${task.id}, type=${task.type}, fileName=${task.fileName}")

        return when (task.type) {
            TaskType.IMAGE_PROCESSING -> processImage(task)
            TaskType.VIDEO_CONVERSION -> processVideo(task)
            TaskType.DOCUMENT_PARSING -> processDocument(task)
            TaskType.ARCHIVE_EXTRACTION -> processArchive(task)
            TaskType.DATA_IMPORT -> processDataImport(task)
            TaskType.CUSTOM -> processCustom(task)
        }
    }

    private suspend fun processImage(task: Task): String {
        simulateWork(2000, 5000)
        return """{"type":"image","resolution":"1920x1080","format":"png","processed":true}"""
    }

    private suspend fun processVideo(task: Task): String {
        simulateWork(5000, 10000)
        return """{"type":"video","duration":120,"format":"mp4","bitrate":"2000kbps","processed":true}"""
    }

    private suspend fun processDocument(task: Task): String {
        simulateWork(1000, 3000)
        return """{"type":"document","pages":10,"words":5000,"processed":true}"""
    }

    private suspend fun processArchive(task: Task): String {
        simulateWork(2000, 4000)
        return """{"type":"archive","files":25,"totalSize":${task.fileSize},"processed":true}"""
    }

    private suspend fun processDataImport(task: Task): String {
        simulateWork(3000, 7000)
        return """{"type":"data_import","records":1000,"errors":0,"processed":true}"""
    }

    private suspend fun processCustom(task: Task): String {
        simulateWork(1000, 3000)
        return """{"type":"custom","status":"completed","processed":true}"""
    }

    private suspend fun simulateWork(minMs: Long, maxMs: Long) {
        val workTime = Random.nextLong(minMs, maxMs)
        delay(workTime)
    }
}