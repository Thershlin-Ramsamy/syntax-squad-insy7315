package com.mcc.construction.data.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED, BLOCKED }

enum class TaskPriority { LOW, MEDIUM, HIGH }

/** A row of the TASKS table. Who is doing it lives in [TaskAssignment]. */
data class Task(
    val taskId: Int,
    val projectId: Int,
    val title: String,
    val description: String,
    val deadline: LocalDate,
    val priority: TaskPriority,
    val status: TaskStatus
)

/**
 * A photo a worker attached to a task as proof of work. Not in the ERD yet —
 * see "Fields beyond the ERD" in docs/ARCHITECTURE.md. [uri] is the content URI
 * the Android photo picker returned.
 */
data class TaskPhoto(
    val photoId: Int,
    val taskId: Int,
    val userId: Int,
    val uri: String,
    val addedAt: LocalDateTime
)

/**
 * A row of the USER_TASKS table — the link between a worker and a task. A task
 * can be assigned to several people, which is why this is its own type rather
 * than an `assigneeId` on [Task].
 */
data class TaskAssignment(
    val assignmentId: Int,
    val userId: Int,
    val taskId: Int,
    val assignedDate: LocalDateTime
)
