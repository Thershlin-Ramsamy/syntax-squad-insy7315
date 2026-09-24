package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskAssignment
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskPhoto
import com.mcc.construction.data.model.TaskStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** What a manager fills in to create a task. */
data class TaskDraft(
    val projectId: Int,
    val title: String,
    val description: String,
    val deadline: LocalDate,
    val priority: TaskPriority
)

/** Tasks and who they are assigned to (TASKS and USER_TASKS). */
interface TaskRepository {

    fun observeTasks(): Flow<List<Task>>

    fun observeTask(taskId: Int): Flow<Task?>

    fun observeTasksForProject(projectId: Int): Flow<List<Task>>

    /** Tasks with an assignment for [userId] — a worker's own task list. */
    fun observeTasksAssignedTo(userId: Int): Flow<List<Task>>

    fun observeAssignments(taskId: Int): Flow<List<TaskAssignment>>

    /** Every assignment on every task — who is working on what. */
    fun observeAllAssignments(): Flow<List<TaskAssignment>>

    /** Creates a new pending task. Assign people to it with [assignTask]. */
    suspend fun createTask(draft: TaskDraft): Task

    /**
     * Mirrors the class diagram's `Worker.updateTaskStatus()`. The project's
     * manager is notified when someone else moves the task on.
     */
    suspend fun updateStatus(taskId: Int, status: TaskStatus)

    /**
     * Mirrors the class diagram's `Manager.assignTask()`. The assigned worker
     * is sent a notification; assigning the same person twice does nothing.
     */
    suspend fun assignTask(taskId: Int, userId: Int): TaskAssignment

    /** Proof-of-work photos for [taskId], oldest first. */
    fun observeProofPhotos(taskId: Int): Flow<List<TaskPhoto>>

    /** Mirrors the class diagram's `Task.uploadImage()`. Queued if the device is offline. */
    suspend fun addProofPhoto(taskId: Int, userId: Int, uri: String): TaskPhoto

    suspend fun removeProofPhoto(photoId: Int)
}
