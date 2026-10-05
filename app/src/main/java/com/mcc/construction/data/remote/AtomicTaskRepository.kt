package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Task

interface AtomicTaskRepository : TaskRepository {

    suspend fun createAssignedTask(
        draft: TaskDraft,
        workerIds: List<Int>
    ): Task
}