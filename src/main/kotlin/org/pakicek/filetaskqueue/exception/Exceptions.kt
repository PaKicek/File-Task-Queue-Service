package org.pakicek.filetaskqueue.exception

class TaskNotFoundException(message: String) : RuntimeException(message)

class TaskOperationException(message: String) : RuntimeException(message)