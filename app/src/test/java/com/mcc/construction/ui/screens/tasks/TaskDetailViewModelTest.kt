package com.mcc.construction.ui.screens.tasks

import com.mcc.construction.R
import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.remote.WritePolicy
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TaskDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(c: FakeAppContainer, taskId: Int) = TaskDetailViewModel(
        taskId, c.authRepository, c.taskRepository, c.projectRepository,
        c.userRepository, c.syncRepository, c.clock
    )

    @Test
    fun showsTaskProjectAndAssignees() = runTest {
        val vm = viewModel(workerContainer(), 101)
        keepCollecting(vm.uiState)

        val state = vm.uiState.value
        assertEquals("Pour concrete foundation — Unit 4", state.task?.title)
        assertEquals("Riverside Estate Phase 2", state.project?.projectName)
        assertEquals(setOf("Sipho Dlamini", "Ayanda Zulu"), state.assignees.map { it.fullName }.toSet())
        assertTrue(state.canUpdate)
    }

    @Test
    fun pendingMovesToInProgressThenCompleted() = runTest {
        val c = workerContainer()
        val vm = viewModel(c, 102)
        keepCollecting(vm.uiState)
        vm.onPhotosPicked(listOf("content://proof/1"))

        vm.onAdvanceStatus()
        assertEquals(TaskStatus.IN_PROGRESS, vm.uiState.value.task?.status)
        assertEquals(R.string.task_started_message, vm.uiState.value.message?.textRes)

        vm.onAdvanceStatus()
        assertEquals(TaskStatus.COMPLETED, vm.uiState.value.task?.status)
        assertNull(vm.uiState.value.nextStatus)
        // The shared fake state carries the change to every other screen.
        assertEquals(TaskStatus.COMPLETED, c.taskRepository.observeTask(102).first()?.status)
    }

    @Test
    fun completingWithoutAPhotoAsksFirst() = runTest {
        val vm = viewModel(workerContainer(), 101)
        keepCollecting(vm.uiState)

        vm.onAdvanceStatus()
        assertTrue(vm.uiState.value.isCompleteWithoutPhotoDialogVisible)
        assertEquals(TaskStatus.IN_PROGRESS, vm.uiState.value.task?.status)

        vm.onCompleteWithoutPhoto()
        assertFalse(vm.uiState.value.isCompleteWithoutPhotoDialogVisible)
        assertEquals(TaskStatus.COMPLETED, vm.uiState.value.task?.status)
    }

    @Test
    fun photosAreCappedAndRemovable() = runTest {
        val vm = viewModel(workerContainer(), 101)
        keepCollecting(vm.uiState)

        vm.onPhotosPicked((1..7).map { "content://proof/$it" })
        assertEquals(TaskDetailViewModel.MAX_PHOTOS, vm.uiState.value.photos.size)
        assertTrue(vm.uiState.value.message!!.isError)

        vm.onRemovePhoto(vm.uiState.value.photos.first().photoId)
        assertEquals(TaskDetailViewModel.MAX_PHOTOS - 1, vm.uiState.value.photos.size)
    }

    @Test
    fun workerCannotUpdateSomeoneElsesTask() = runTest {
        val vm = viewModel(workerContainer(), 107)
        keepCollecting(vm.uiState)

        assertFalse(vm.uiState.value.canUpdate)
        vm.onAdvanceStatus()
        assertEquals(TaskStatus.BLOCKED, vm.uiState.value.task?.status)
    }

    @Test
    fun offlineCompletionIsRejected() = runTest {
        val c = workerContainer()
        c.syncRepository.setOfflineMode(true)
        var attempts = 0
        val repository = object : TaskRepository by c.taskRepository {
            override suspend fun updateStatus(taskId: Int, status: TaskStatus) {
                attempts++
                WritePolicy.requireOnline(c.syncRepository.status.value)
                c.taskRepository.updateStatus(taskId, status)
            }
        }
        val vm = TaskDetailViewModel(
            101, c.authRepository, repository, c.projectRepository,
            c.userRepository, c.syncRepository, c.clock
        )
        keepCollecting(vm.uiState)
        val pendingBefore = c.syncRepository.status.value.pendingCount

        vm.onAdvanceStatus()
        vm.onCompleteWithoutPhoto()

        assertEquals(1, attempts)
        assertEquals(TaskStatus.IN_PROGRESS, c.taskRepository.observeTask(101).first()?.status)
        assertEquals(pendingBefore, c.syncRepository.status.value.pendingCount)
        assertNull(vm.uiState.value.message)
        assertFalse(vm.uiState.value.isUpdating)
    }

    @Test
    fun unknownTaskIsReportedAsNotFound() = runTest {
        val vm = viewModel(workerContainer(), 9999)
        keepCollecting(vm.uiState)

        assertTrue(vm.uiState.value.isNotFound)
    }
}