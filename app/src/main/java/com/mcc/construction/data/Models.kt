package com.mcc.construction.data

enum class TaskStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

data class Task(
    val id: String,
    val title: String,
    val projectName: String,
    val location: String,
    val dueTime: String,
    val description: String,
    val assignedBy: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val safetyNote: String? = null
)

data class WorkerProfile(
    val name: String,
    val role: String,
    val site: String
)
