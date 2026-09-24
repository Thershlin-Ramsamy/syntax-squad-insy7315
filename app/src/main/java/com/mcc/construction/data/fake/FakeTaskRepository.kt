package com.mcc.construction.data.fake

import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskAssignment
import com.mcc.construction.data.model.TaskPhoto
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.repository.TaskDraft
import com.mcc.construction.data.repository.TaskRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeTaskRepository(
    seed: FakeSeedData,
    private val sync: FakeSyncRepository,
    private val notifier: FakeNotifier,
    private val clock: Clock
) : TaskRepository {

    private val tasks = MutableStateFlow(seed.tasks)
    private val assignments = MutableStateFlow(seed.assignments)
    private val nextAssignmentId = AtomicInteger(seed.assignments.maxOf { it.assignmentId } + 1)
    private val photos = MutableStateFlow<List<TaskPhoto>>(emptyList())
    private val nextPhotoId = AtomicInteger(1)
    private val nextTaskId = AtomicInteger(seed.tasks.maxOf { it.taskId } + 1)

    override fun observeTasks(): Flow<List<Task>> = tasks

    override fun observeTask(taskId: Int): Flow<Task?> =
        tasks.map { list -> list.firstOrNull { it.taskId == taskId } }

    override fun observeTasksForProject(projectId: Int): Flow<List<Task>> =
        tasks.map { list -> list.filter { it.projectId == projectId } }

    override fun observeTasksAssignedTo(userId: Int): Flow<List<Task>> =
        combine(tasks, assignments) { taskList, assignmentList ->
            val taskIds = assignmentList.filter { it.userId == userId }.map { it.taskId }.toSet()
            taskList.filter { it.taskId in taskIds }
        }

    override fun observeAssignments(taskId: Int): Flow<List<TaskAssignment>> =
        assignments.map { list -> list.filter { it.taskId == taskId } }

    override fun observeAllAssignments(): Flow<List<TaskAssignment>> = assignments

    override suspend fun createTask(draft: TaskDraft): Task {
        val task = Task(
            taskId = nextTaskId.getAndIncrement(),
            projectId = draft.projectId,
            title = draft.title,
            description = draft.description,
            deadline = draft.deadline,
            priority = draft.priority,
            status = TaskStatus.PENDING
        )
        tasks.update { it + task }
        sync.recordChange(PendingChangeType.TASK_UPDATE, task.title)
        return task
    }

    override suspend fun updateStatus(taskId: Int, status: TaskStatus) {
        val task = tasks.value.firstOrNull { it.taskId == taskId } ?: return
        if (task.status == status) return
        tasks.update { list -> list.map { if (it.taskId == taskId) it.copy(status = status) else it } }
        sync.recordChange(PendingChangeType.TASK_UPDATE, task.title)

        val verb = when (status) {
            TaskStatus.IN_PROGRESS -> "started"
            TaskStatus.COMPLETED -> "completed"
            TaskStatus.BLOCKED -> "marked as blocked"
            TaskStatus.PENDING -> "moved back to pending"
        }
        notifier.notifyProjectManager(
            task.projectId,
            NotificationType.TASK,
            "${notifier.actorName()} $verb “${task.title}”."
        )
    }

    override suspend fun assignTask(taskId: Int, userId: Int): TaskAssignment {
        val existing = assignments.value.firstOrNull { it.taskId == taskId && it.userId == userId }
        if (existing != null) return existing

        val assignment = TaskAssignment(
            assignmentId = nextAssignmentId.getAndIncrement(),
            userId = userId,
            taskId = taskId,
            assignedDate = LocalDateTime.now(clock)
        )
        assignments.update { it + assignment }
        tasks.value.firstOrNull { it.taskId == taskId }?.let { task ->
            notifier.notify(userId, NotificationType.TASK, "${notifier.actorName()} assigned you “${task.title}”.")
        }
        return assignment
    }

    override fun observeProofPhotos(taskId: Int): Flow<List<TaskPhoto>> =
        photos.map { list -> list.filter { it.taskId == taskId } }

    override suspend fun addProofPhoto(taskId: Int, userId: Int, uri: String): TaskPhoto {
        val photo = TaskPhoto(
            photoId = nextPhotoId.getAndIncrement(),
            taskId = taskId,
            userId = userId,
            uri = uri,
            addedAt = LocalDateTime.now(clock)
        )
        photos.update { it + photo }
        tasks.value.firstOrNull { it.taskId == taskId }?.let {
            sync.recordChange(PendingChangeType.TASK_UPDATE, it.title)
        }
        return photo
    }

    override suspend fun removeProofPhoto(photoId: Int) {
        photos.update { list -> list.filterNot { it.photoId == photoId } }
    }
}
