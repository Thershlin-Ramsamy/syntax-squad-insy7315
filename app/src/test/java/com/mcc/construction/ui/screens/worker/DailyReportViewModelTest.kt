package com.mcc.construction.ui.screens.worker

import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.Weather
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.WORKER_ID
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

class DailyReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(c: FakeAppContainer) = DailyReportViewModel(
        c.authRepository, c.projectRepository, c.taskRepository,
        c.dailyReportRepository, c.syncRepository, c.clock
    )

    @Test
    fun defaultsToTheWorkersProject() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)

        val state = vm.uiState.value
        assertEquals(listOf(1), state.projects.map { it.projectId })
        assertEquals(1, state.form.projectId)
        assertTrue(state.finishedTaskTitles.contains("Morning safety walkaround"))
    }

    @Test
    fun submitWithMissingFieldsFlagsThemAndDoesNotConfirm() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)

        vm.onSubmitClick()

        val form = vm.uiState.value.form
        assertEquals(setOf(ReportField.WEATHER, ReportField.WORK_COMPLETED), form.missing)
        assertFalse(form.isConfirmVisible)
    }

    @Test
    fun quickFillsAvoidTyping() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)

        vm.onAddFinishedTasks()
        vm.onAddFinishedTasks()
        vm.onAddProblem("Weather delay")
        vm.onAddProblem("Weather delay")

        val form = vm.uiState.value.form
        assertEquals(1, Regex("Morning safety walkaround").findAll(form.workCompleted).count())
        assertEquals("Weather delay", form.problems)
    }

    @Test
    fun confirmedReportIsSavedWithPhotos() = runTest {
        val c = workerContainer()
        val vm = viewModel(c)
        keepCollecting(vm.uiState)

        vm.onWeatherSelected(Weather.WINDY)
        vm.onWorkCompletedChange("Fixed rebar on Unit 4")
        vm.onPhotosPicked(listOf("content://a", "content://b"))
        vm.onSubmitClick()
        assertTrue(vm.uiState.value.form.isConfirmVisible)
        vm.onConfirmSubmit()

        val submitted = vm.uiState.value.form.submitted
        assertNotNull(submitted)
        assertEquals("Windy", submitted!!.weather)
        assertEquals(listOf("content://a", "content://b"), submitted.photoUris)
        assertEquals(ReportStatus.SUBMITTED, submitted.status)
        assertEquals(submitted, c.dailyReportRepository.observeReportsBy(WORKER_ID).first().first())
    }

    @Test
    fun offlineSubmissionIsFlagged() = runTest {
        val c = workerContainer()
        c.syncRepository.setOfflineMode(true)
        val vm = viewModel(c)
        keepCollecting(vm.uiState)

        vm.onWeatherSelected(Weather.RAINY)
        vm.onWorkCompletedChange("Rain stopped work at 10:00")
        vm.onSubmitClick()
        vm.onConfirmSubmit()

        assertTrue(vm.uiState.value.form.submittedOffline)
    }

    @Test
    fun startAnotherClearsTheFormButKeepsTheProject() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)
        vm.onWeatherSelected(Weather.SUNNY)
        vm.onWorkCompletedChange("Work")
        vm.onSubmitClick()
        vm.onConfirmSubmit()

        vm.onStartAnother()

        val state = vm.uiState.value
        assertEquals(1, state.form.projectId)
        assertEquals("", state.form.workCompleted)
        assertNotNull(state.alreadySubmittedToday)
    }
}
