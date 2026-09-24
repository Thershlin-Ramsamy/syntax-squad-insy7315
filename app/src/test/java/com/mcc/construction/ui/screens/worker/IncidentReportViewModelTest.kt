package com.mcc.construction.ui.screens.worker

import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.model.IncidentSeverity
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.WORKER_ID
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IncidentReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(c: FakeAppContainer, inspectionId: Int?) = IncidentReportViewModel(
        inspectionId, c.authRepository, c.safetyRepository, c.projectRepository,
        c.taskRepository, c.syncRepository
    )

    @Test
    fun missingSeverityAndDescriptionAreFlagged() = runTest {
        val vm = viewModel(workerContainer(), null)
        keepCollecting(vm.uiState)

        vm.onSubmit()

        assertEquals(setOf(IncidentField.SEVERITY, IncidentField.DESCRIPTION), vm.uiState.value.form.missing)
        assertNull(vm.uiState.value.form.submitted)
    }

    @Test
    fun standaloneIncidentIsSavedWithItsPhoto() = runTest {
        val c = workerContainer()
        val vm = viewModel(c, null)
        keepCollecting(vm.uiState)
        assertEquals(1, vm.uiState.value.form.projectId)

        vm.onSeveritySelected(IncidentSeverity.MINOR)
        vm.onAddPhrase("Slip or trip")
        vm.onPhotoPicked("content://photo/1")
        vm.onSubmit()

        val incident = vm.uiState.value.form.submitted
        assertNotNull(incident)
        assertEquals(2, incident!!.severityLevel)
        assertEquals(listOf("content://photo/1"), incident.photoUris)
        assertTrue(c.safetyRepository.observeIncidents().first().contains(incident))
    }

    @Test
    fun incidentFromAChecklistIsLinkedToIt() = runTest {
        val c = workerContainer()
        val inspection = c.safetyRepository.submitInspection(1, WORKER_ID, passed = false, comments = "Failed: PPE inspected")
        val vm = viewModel(c, inspection.inspectionId)
        keepCollecting(vm.uiState)
        assertEquals(inspection, vm.uiState.value.linkedInspection)

        vm.onSeveritySelected(IncidentSeverity.MODERATE)
        vm.onDescriptionChange("Torn glove found during PPE check")
        vm.onSubmit()

        assertEquals(inspection.inspectionId, vm.uiState.value.form.submitted?.inspectionId)
    }

    @Test
    fun highSeverityShowsTheUrgentPrompt() = runTest {
        val vm = viewModel(workerContainer(), null)
        keepCollecting(vm.uiState)

        vm.onSeveritySelected(IncidentSeverity.MODERATE)
        assertFalse(vm.uiState.value.isUrgent)
        vm.onSeveritySelected(IncidentSeverity.SERIOUS)
        assertTrue(vm.uiState.value.isUrgent)
    }

    @Test
    fun phrasesAreNotAddedTwice() = runTest {
        val vm = viewModel(workerContainer(), null)
        keepCollecting(vm.uiState)

        vm.onAddPhrase("Falling object")
        vm.onAddPhrase("No one was hurt")
        vm.onAddPhrase("Falling object")

        assertEquals("Falling object. No one was hurt", vm.uiState.value.form.description)
    }
}
