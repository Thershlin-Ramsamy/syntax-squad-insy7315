package com.mcc.construction.ui.screens.worker

import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SafetyChecklistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(c: FakeAppContainer) = SafetyChecklistViewModel(
        c.authRepository, c.safetyRepository, c.projectRepository,
        c.taskRepository, c.syncRepository, c.clock
    )

    @Test
    fun cannotSubmitUntilEveryItemIsAnswered() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)
        val items = vm.uiState.value.items
        assertEquals(6, items.size)

        items.dropLast(1).forEach { vm.onAnswer(it.itemId, true) }
        assertEquals(ChecklistResult.INCOMPLETE, vm.uiState.value.result)
        assertFalse(vm.uiState.value.canSubmit)

        vm.onAnswer(items.last().itemId, true)
        assertEquals(ChecklistResult.PASS, vm.uiState.value.result)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun oneNoFailsTheChecklistAndIsRecorded() = runTest {
        val c = workerContainer()
        val vm = viewModel(c)
        keepCollecting(vm.uiState)
        val items = vm.uiState.value.items
        items.forEach { vm.onAnswer(it.itemId, true) }
        vm.onAnswer(items[2].itemId, false)
        assertEquals(ChecklistResult.FAIL, vm.uiState.value.result)

        vm.onSubmit()

        val inspection = vm.uiState.value.form.submitted
        assertNotNull(inspection)
        assertFalse(inspection!!.passed)
        assertTrue(inspection.comments.contains(items[2].title))
        assertEquals(inspection, c.safetyRepository.observeInspection(inspection.inspectionId).first())
    }

    @Test
    fun changingAnAnswerUpdatesTheResult() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)
        val items = vm.uiState.value.items
        items.forEach { vm.onAnswer(it.itemId, false) }
        items.forEach { vm.onAnswer(it.itemId, true) }

        assertEquals(ChecklistResult.PASS, vm.uiState.value.result)
        assertTrue(vm.uiState.value.failedItems.isEmpty())
    }

    @Test
    fun submittedChecklistCountsAsDoneToday() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)
        vm.uiState.value.items.forEach { vm.onAnswer(it.itemId, true) }
        vm.onSubmit()

        vm.onStartAgain()

        assertTrue(vm.uiState.value.lastToday?.passed == true)
        assertTrue(vm.uiState.value.form.answers.isEmpty())
    }
}
