package org.pakicek.filetaskqueue.domain

import jakarta.persistence.*
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import java.time.LocalDateTime

@Entity
@Table(
    name = "tasks",
    indexes = [
        Index(name = "idx_task_status", columnList = "status"),
        Index(name = "idx_task_type", columnList = "type"),
        Index(name = "idx_task_created_at", columnList = "created_at"),
        Index(name = "idx_task_priority", columnList = "priority")
    ]
)
class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 255)
    val fileName: String,

    @Column(nullable = false, length = 500)
    val filePath: String,

    @Column
    val fileSize: Long? = null,

    @Column(length = 100)
    val mimeType: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: TaskType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: TaskStatus = TaskStatus.PENDING,

    @Column(nullable = false)
    val priority: Int = 0,

    @Column(length = 2000)
    var errorMessage: String? = null,

    @Column(columnDefinition = "TEXT")
    var result: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var startedAt: LocalDateTime? = null,

    @Column
    var completedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var retryCount: Int = 0,

    @Column
    var lastRetryAt: LocalDateTime? = null
) {
    fun canBeCancelled(): Boolean {
        return status in listOf(TaskStatus.PENDING, TaskStatus.RETRYING)
    }
}