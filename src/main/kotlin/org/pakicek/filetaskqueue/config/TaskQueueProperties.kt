package org.pakicek.filetaskqueue.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "task-queue")
data class TaskQueueProperties(
    var maxConcurrentTasks: Int = 5,
    var retry: RetryConfig = RetryConfig(),
    var scheduler: SchedulerConfig = SchedulerConfig()
) {
    data class RetryConfig(
        var enabled: Boolean = true,
        var maxAttempts: Int = 3,
        var backoffDelayMs: Long = 5000
    )

    data class SchedulerConfig(
        var enabled: Boolean = true,
        var stuckTaskTimeoutMinutes: Long = 30,
        var checkIntervalMs: Long = 60000
    )
}