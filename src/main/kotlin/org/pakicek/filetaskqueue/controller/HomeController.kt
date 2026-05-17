package org.pakicek.filetaskqueue.controller

import org.pakicek.filetaskqueue.repository.TaskRepository
import org.pakicek.filetaskqueue.domain.enums.TaskStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDateTime

@Controller
@RequestMapping("/")
class HomeController(
    private val taskRepository: TaskRepository
) {

    @GetMapping
    fun index(model: Model): String {
        val pendingCount = taskRepository.countByStatus(TaskStatus.PENDING)
        val processingCount = taskRepository.countByStatus(TaskStatus.PROCESSING)
        val completedCount = taskRepository.countByStatus(TaskStatus.COMPLETED)
        val failedCount = taskRepository.countByStatus(TaskStatus.FAILED)

        model.addAttribute("pendingCount", pendingCount)
        model.addAttribute("processingCount", processingCount)
        model.addAttribute("completedCount", completedCount)
        model.addAttribute("failedCount", failedCount)
        model.addAttribute("totalCount", pendingCount + processingCount + completedCount + failedCount)
        model.addAttribute("timestamp", LocalDateTime.now())

        return "index"
    }
}