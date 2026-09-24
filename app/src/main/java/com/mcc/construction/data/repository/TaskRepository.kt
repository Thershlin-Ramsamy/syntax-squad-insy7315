package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskAssignment
import com.mcc.construction.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

/** Tasks and who they are assigned to (TASKS and USER_TASKS). */
interface TaskRepository {

    fun observeTasks(): Flow<List<Task>>

    fun observeTask(taskId: Int): Flow<Task?>

    fun observeTasksForProject(projectId: Int): Flow<List<Task>>

    /** Tasks with an assignment for [userId] — a worker's own task list. */
    fun observeTasksAssignedTo(userId: Int): Flow<List<Task>>

    fun observeAssignments(taskId: Int): Flow<List<TaskAssignment>>

    /** Mirrors the class diagram's `Worker.updateTaskStatus()`. */
    suspend fun updateStatus(taskId: Int, status: TaskStatus)

    /** Mirrors the class diagram's `Manager.assignTask()`. */
    suspend fun assignTask(taskId: Int, userId: Int): TaskAssignment
}
