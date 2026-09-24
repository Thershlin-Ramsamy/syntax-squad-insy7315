package com.mcc.construction.ui.screens.tasks

import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.fake.testContainer
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TaskListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(c: FakeAppContainer) =
        TaskListViewModel(c.authRepository, c.taskRepository, c.projectRepository, c.clock)

    @Test
    fun workerSeesOnlyTheirOwnTasksInWorkOrder() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)

        val state = vm.uiState.value
        assertFalse(state.showsAllTasks)
        assertEquals(setOf(101, 102, 103, 105), state.items!!.map { it.task.taskId }.toSet())
        assertEquals(TaskStatus.IN_PROGRESS, state.items!!.first().task.status)
        assertEquals(TaskStatus.COMPLETED, state.items!!.last().task.status)
    }

    @Test
    fun filterNarrowsTheListAndCountsStayAccurate() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)

        vm.onFilterChange(TaskFilter.PENDING)

        val state = vm.uiState.value
        assertTrue(state.visible.all { it.task.status == TaskStatus.PENDING })
        assertEquals(state.visible.size, state.countFor(TaskFilter.PENDING))
        assertEquals(4, state.countFor(TaskFilter.ALL))
    }

    @Test
    fun managerSeesEveryTaskAndWorkerChangesShowUp() = runTest {
        val c = testContainer()
        c.authRepository.signIn("manager@mcc.co.za", "pw")
        val vm = viewModel(c)
        keepCollecting(vm.uiState)
        assertTrue(vm.uiState.value.showsAllTasks)

        c.taskRepository.updateStatus(102, TaskStatus.COMPLETED)

        val task = vm.uiState.value.items!!.single { it.task.taskId == 102 }.task
        assertEquals(TaskStatus.COMPLETED, task.status)
    }

    @Test
    fun paneFollowsTheSelection() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)
        assertEquals(vm.uiState.value.visible.first().task.taskId, vm.uiState.value.paneTaskId)

        vm.onSelectTask(105)

        assertEquals(105, vm.uiState.value.paneTaskId)
    }
}
