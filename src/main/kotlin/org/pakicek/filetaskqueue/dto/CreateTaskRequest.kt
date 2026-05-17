package org.pakicek.filetaskqueue.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.pakicek.filetaskqueue.domain.enums.TaskType

data class CreateTaskRequest(
    @field:NotBlank(message = "File name is required")
    val fileName: String,

    @field:NotBlank(message = "File path is required")
    val filePath: String,

    val fileSize: Long? = null,

    val mimeType: String? = null,

    @field:NotNull(message = "Task type is required")
    val type: TaskType,

    val priority: Int = 0
)