package org.pakicek.filetaskqueue.domain.enums

enum class TaskStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    RETRYING
}