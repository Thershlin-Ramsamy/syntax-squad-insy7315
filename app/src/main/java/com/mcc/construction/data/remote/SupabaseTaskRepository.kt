package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskAssignment
import com.mcc.construction.data.model.TaskPhoto
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.remote.dto.TaskAssignmentRow
import com.mcc.construction.data.remote.dto.TaskPhotoRow
import com.mcc.construction.data.remote.dto.TaskRow
import com.mcc.construction.data.repository.AtomicTaskRepository
import com.mcc.construction.data.repository.TaskDraft
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseTaskRepository(
    private val client: SupabaseClient,
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport,
    private val storage: BackendStorageService
) : AtomicTaskRepository {

    override fun observeTasks(): Flow<List<Task>> {
        return reads.observeRows<TaskRow>(
            table = "tasks",
            primaryKey = "task_id"
        ).map { rows ->
            rows.map { row ->
                mapTask(row)
            }.sortedWith(
                compareBy<Task> {
                    it.deadline
                }.thenBy {
                    it.taskId
                }
            )
        }
    }

    override fun observeTask(
        taskId: Int
    ): Flow<Task?> {
        return observeTasks().map { tasks ->
            tasks.firstOrNull {
                it.taskId == taskId
            }
        }
    }

    override fun observeTasksForProject(
        projectId: Int
    ): Flow<List<Task>> {
        return observeTasks().map { tasks ->
            tasks.filter {
                it.projectId == projectId
            }
        }
    }

    override fun observeTasksAssignedTo(
        userId: Int
    ): Flow<List<Task>> {
        return combine(
            observeTasks(),
            observeAllAssignments()
        ) { tasks, assignments ->
            val taskIds = assignments.filter {
                it.userId == userId
            }.map {
                it.taskId
            }.toSet()

            tasks.filter {
                it.taskId in taskIds
            }
        }
    }

    override fun observeAssignments(
        taskId: Int
    ): Flow<List<TaskAssignment>> {
        return observeAllAssignments().map { assignments ->
            assignments.filter {
                it.taskId == taskId
            }
        }
    }

    override fun observeAllAssignments():
            Flow<List<TaskAssignment>> {
        return reads.observeRows<TaskAssignmentRow>(
            table = "task_assignments",
            primaryKey = "assignment_id"
        ).map { rows ->
            rows.map { row ->
                mapAssignment(row)
            }.sortedBy {
                it.assignmentId
            }
        }
    }

    override suspend fun createTask(
        draft: TaskDraft
    ): Task {
        error(
            "Supabase task creation requires worker IDs. " +
                    "Use createAssignedTask."
        )
    }

    override suspend fun createAssignedTask(
        draft: TaskDraft,
        workerIds: List<Int>
    ): Task {
        require(draft.projectId > 0)
        require(draft.title.isNotBlank())

        val uniqueWorkerIds = workerIds.distinct().sorted()

        require(uniqueWorkerIds.size in 1..100) {
            "Choose between 1 and 100 workers."
        }

        require(uniqueWorkerIds.all { it > 0 })

        val row = writes.executeAs<TaskRow>(
            action = BackendAction.TASK_CREATE,
            payload = buildJsonObject {
                put("project_id", draft.projectId)
                put("title", draft.title.trim())
                put("description", draft.description.trim())
                put("deadline", draft.deadline.toString())
                put("priority", draft.priority.name)
                put(
                    "worker_ids",
                    JsonArray(
                        uniqueWorkerIds.map {
                            JsonPrimitive(it)
                        }
                    )
                )
            }
        )

        return mapTask(row)
    }

    override suspend fun updateStatus(
        taskId: Int,
        status: TaskStatus
    ) {
        require(taskId > 0)

        val actorId = writes.currentApplicationUserId()

        val current = client.from("tasks").select {
            filter {
                eq("task_id", taskId)
            }
        }.decodeSingle<TaskRow>()

        writes.requireCurrentUser(actorId)

        writes.execute(
            action = BackendAction.TASK_STATUS,
            payload = buildJsonObject {
                put("task_id", taskId)
                put("status", status.name)
                put("expected_revision", current.revision)
            }
        )
    }

    override suspend fun assignTask(
        taskId: Int,
        userId: Int
    ): TaskAssignment {
        require(taskId > 0)
        require(userId > 0)

        val row = writes.executeAs<TaskAssignmentRow>(
            action = BackendAction.TASK_ASSIGN,
            payload = buildJsonObject {
                put("task_id", taskId)
                put("user_id", userId)
            }
        )

        return mapAssignment(row)
    }

    override fun observeProofPhotos(
        taskId: Int
    ): Flow<List<TaskPhoto>> {
        return reads.observeRows<TaskPhotoRow>(
            table = "task_photos",
            primaryKey = "photo_id"
        ).map { rows ->
            rows.filter {
                it.taskId == taskId && !it.isRemoved
            }.sortedBy {
                it.photoId
            }.map { row ->
                mapPhoto(row)
            }
        }
    }

    override suspend fun addProofPhoto(
        taskId: Int,
        userId: Int,
        uri: String
    ): TaskPhoto {
        require(taskId > 0)
        writes.requireCurrentUser(userId)

        val task = client.from("tasks").select {
            filter {
                eq("task_id", taskId)
            }
        }.decodeSingle<TaskRow>()

        writes.requireCurrentUser(userId)

        val upload = storage.uploadPhoto(
            projectId = task.projectId,
            uri = uri
        )

        writes.requireCurrentUser(userId)

        val row = writes.executeAs<TaskPhotoRow>(
            action = BackendAction.TASK_PHOTO_FINALIZE,
            payload = buildJsonObject {
                put("upload_id", upload.uploadId)
                put("task_id", taskId)
            }
        )

        return mapPhoto(row)
    }

    override suspend fun removeProofPhoto(
        photoId: Int
    ) {
        require(photoId > 0)

        val actorId = writes.currentApplicationUserId()

        val photo = client.from("task_photos").select {
            filter {
                eq("photo_id", photoId)
            }
        }.decodeSingle<TaskPhotoRow>()

        writes.requireCurrentUser(actorId)

        // First revoke the photo's logical attachment.
        writes.execute(
            action = BackendAction.TASK_PHOTO_REMOVE,
            payload = buildJsonObject {
                put("photo_id", photoId)
            }
        )

        writes.requireCurrentUser(actorId)

        // Storage's delete policy permits the removed object.
        storage.deleteRemovedPhoto(photo.storagePath)
    }

    private fun mapTask(
        row: TaskRow
    ): Task {
        return Task(
            taskId = row.taskId,
            projectId = row.projectId,
            title = row.title,
            description = row.description,
            deadline = BackendTime.date(row.deadline),
            priority = TaskPriority.valueOf(row.priority),
            status = TaskStatus.valueOf(row.status)
        )
    }

    private fun mapAssignment(
        row: TaskAssignmentRow
    ): TaskAssignment {
        return TaskAssignment(
            assignmentId = row.assignmentId,
            userId = row.userId,
            taskId = row.taskId,
            assignedDate = BackendTime.timestamp(row.assignedDate)
        )
    }

    private suspend fun mapPhoto(
        row: TaskPhotoRow
    ): TaskPhoto {
        return TaskPhoto(
            photoId = row.photoId,
            taskId = row.taskId,
            userId = row.userId,
            uri = storage.signedUrl(
                bucketId = SupabaseConfig.PHOTO_BUCKET,
                path = row.storagePath
            ),
            addedAt = BackendTime.timestamp(row.addedAt)
        )
    }
}