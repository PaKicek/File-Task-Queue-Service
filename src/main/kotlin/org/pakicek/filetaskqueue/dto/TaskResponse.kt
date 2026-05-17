package org.pakicek.filetaskqueue.dto

import org.pakicek.filetaskqueue.domain.Task
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import java.time.LocalDateTime

data class TaskResponse(
    val id: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long?,
    val mimeType: String?,
    val type: TaskType,
    val status: TaskStatus,
    val priority: Int,
    val errorMessage: String?,
    val result: String?,
    val createdAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val retryCount: Int
) {
    companion object {
        fun from(task: Task): TaskResponse {
            return TaskResponse(
                id = task.id!!,
                fileName = task.fileName,
                filePath = task.filePath,
                fileSize = task.fileSize,
                mimeType = task.mimeType,
                type = task.type,
                status = task.status,
                priority = task.priority,
                errorMessage = task.errorMessage,
                result = task.result,
                createdAt = task.createdAt,
                startedAt = task.startedAt,
                completedAt = task.completedAt,
                retryCount = task.retryCount
            )
        }
    }
}