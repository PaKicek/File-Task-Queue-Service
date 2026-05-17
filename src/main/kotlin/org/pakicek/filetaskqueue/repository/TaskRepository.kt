package org.pakicek.filetaskqueue.repository

import org.pakicek.filetaskqueue.domain.Task
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TaskRepository : JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    fun findByStatus(status: TaskStatus, pageable: Pageable): Page<Task>

    fun findByType(type: TaskType, pageable: Pageable): Page<Task>

    @Query(
        """
        SELECT t FROM Task t 
        WHERE t.status = :status 
        ORDER BY t.priority DESC, t.createdAt ASC
        """
    )
    fun findPendingTasksOrderedByPriority(@Param("status") status: TaskStatus): List<Task>

    @Query(
        """
        SELECT t FROM Task t 
        WHERE t.status = org.pakicek.filetaskqueue.domain.enums.TaskStatus.PROCESSING
        AND t.startedAt IS NOT NULL
        AND t.startedAt < :timeout
        ORDER BY t.createdAt ASC
        """
    )
    fun findStuckTasks(@Param("timeout") timeout: LocalDateTime): List<Task>

    fun countByStatus(status: TaskStatus): Long
}