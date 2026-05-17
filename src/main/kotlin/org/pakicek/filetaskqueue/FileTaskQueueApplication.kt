package org.pakicek.filetaskqueue

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class FileTaskQueueApplication

fun main(args: Array<String>) {
    runApplication<FileTaskQueueApplication>(*args)
}