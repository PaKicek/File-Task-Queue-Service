package org.pakicek.filetaskqueue.service

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.pakicek.filetaskqueue.domain.enums.TaskType
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class MetricsService(
    private val meterRegistry: MeterRegistry
) {
    fun recordTaskCompletion(taskType: TaskType, startTime: LocalDateTime, endTime: LocalDateTime, success: Boolean) {
        val duration = Duration.between(startTime, endTime)

        Timer.builder("task.processing.duration")
            .tag("type", taskType.name)
            .tag("status", if (success) "success" else "failure")
            .register(meterRegistry)
            .record(duration)

        meterRegistry.counter(
            "task.processed.total",
            "type", taskType.name,
            "status", if (success) "success" else "failure"
        ).increment()
    }
}