package org.pakicek.filetaskqueue.repository

import org.pakicek.filetaskqueue.domain.Task
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime

object TaskSpecifications {

    fun withStatus(status: TaskStatus?): Specification<Task> {
        return Specification { root, _, cb ->
            if (status == null) {
                null
            } else {
                cb.equal(root.get<TaskStatus>("status"), status)
            }
        }
    }

    fun withType(type: TaskType?): Specification<Task> {
        return Specification { root, _, cb ->
            if (type == null) {
                null
            } else {
                cb.equal(root.get<TaskType>("type"), type)
            }
        }
    }

    fun createdAfter(date: LocalDateTime?): Specification<Task> {
        return Specification { root, _, cb ->
            if (date == null) {
                null
            } else {
                cb.greaterThanOrEqualTo(root.get("createdAt"), date)
            }
        }
    }

    fun createdBefore(date: LocalDateTime?): Specification<Task> {
        return Specification { root, _, cb ->
            if (date == null) {
                null
            } else {
                cb.lessThanOrEqualTo(root.get("createdAt"), date)
            }
        }
    }

    fun combine(
        status: TaskStatus?,
        type: TaskType?,
        createdAfter: LocalDateTime?,
        createdBefore: LocalDateTime?
    ): Specification<Task> {
        var spec: Specification<Task>? = null

        if (status != null) {
            spec = withStatus(status)
        }

        if (type != null) {
            spec = if (spec == null) withType(type) else spec.and(withType(type))
        }

        if (createdAfter != null) {
            spec = if (spec == null) createdAfter(createdAfter) else spec.and(createdAfter(createdAfter))
        }

        if (createdBefore != null) {
            spec = if (spec == null) createdBefore(createdBefore) else spec.and(createdBefore(createdBefore))
        }

        return spec ?: Specification.where(null)
    }
}