package com.mcc.construction.ui.screens.worker

import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.data.repository.SafetyRepository
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SafetyChecklistFailureTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        container: FakeAppContainer,
        safetyRepository: SafetyRepository
    ): SafetyChecklistViewModel {
        return SafetyChecklistViewModel(
            authRepository =
                container.authRepository,
            safetyRepository =
                safetyRepository,
            projectRepository =
                container.projectRepository,
            taskRepository =
                container.taskRepository,
            syncRepository =
                container.syncRepository,
            clock =
                container.clock
        )
    }

    private fun answerEveryItem(
        viewModel: SafetyChecklistViewModel
    ) {
        viewModel.uiState.value.items.forEach {
            viewModel.onAnswer(
                itemId = it.itemId,
                isSafe = true
            )
        }
    }

    @Test
    fun failedRequestPreservesAnswersAndNotes() = runTest {
        val container = workerContainer()

        val failingRepository =
            object : SafetyRepository
            by container.safetyRepository {

                override suspend fun submitInspection(
                    projectId: Int,
                    inspectorId: Int,
                    passed: Boolean,
                    comments: String
                ): SafetyInspection {
                    throw IllegalStateException(
                        "Simulated network failure"
                    )
                }
            }

        val viewModel = viewModel(
            container,
            failingRepository
        )

        keepCollecting(viewModel.uiState)
        answerEveryItem(viewModel)

        viewModel.onNotesChange(
            "Barrier repaired before the inspection."
        )

        val answersBefore =
            viewModel.uiState.value.form.answers

        val notesBefore =
            viewModel.uiState.value.form.notes

        viewModel.onSubmit()

        val result =
            viewModel.uiState.value.form

        assertFalse(result.isSubmitting)
        assertNull(result.submitted)
        assertEquals(
            answersBefore,
            result.answers
        )
        assertEquals(
            notesBefore,
            result.notes
        )
        assertTrue(
            viewModel.uiState.value.canSubmit
        )
    }

    @Test
    fun retryAfterFailureCanSucceed() = runTest {
        val container = workerContainer()

        var attempts = 0

        val retryRepository =
            object : SafetyRepository
            by container.safetyRepository {

                override suspend fun submitInspection(
                    projectId: Int,
                    inspectorId: Int,
                    passed: Boolean,
                    comments: String
                ): SafetyInspection {
                    attempts++

                    if (attempts == 1) {
                        throw IllegalStateException(
                            "First request failed"
                        )
                    }

                    return container.safetyRepository
                        .submitInspection(
                            projectId = projectId,
                            inspectorId = inspectorId,
                            passed = passed,
                            comments = comments
                        )
                }
            }

        val viewModel = viewModel(
            container,
            retryRepository
        )

        keepCollecting(viewModel.uiState)
        answerEveryItem(viewModel)

        viewModel.onSubmit()

        assertNull(
            viewModel.uiState.value.form.submitted
        )
        assertFalse(
            viewModel.uiState.value.form.isSubmitting
        )

        viewModel.onSubmit()

        assertEquals(2, attempts)
        assertNotNull(
            viewModel.uiState.value.form.submitted
        )
        assertFalse(
            viewModel.uiState.value.form.isSubmitting
        )
    }

    @Test
    fun duplicateTapDoesNotStartSecondRequest() = runTest {
        val container = workerContainer()

        val releaseRequest =
            CompletableDeferred<Unit>()

        var attempts = 0

        val delayedRepository =
            object : SafetyRepository
            by container.safetyRepository {

                override suspend fun submitInspection(
                    projectId: Int,
                    inspectorId: Int,
                    passed: Boolean,
                    comments: String
                ): SafetyInspection {
                    attempts++

                    releaseRequest.await()

                    return container.safetyRepository
                        .submitInspection(
                            projectId = projectId,
                            inspectorId = inspectorId,
                            passed = passed,
                            comments = comments
                        )
                }
            }

        val viewModel = viewModel(
            container,
            delayedRepository
        )

        keepCollecting(viewModel.uiState)
        answerEveryItem(viewModel)

        viewModel.onSubmit()
        viewModel.onSubmit()

        assertEquals(1, attempts)
        assertTrue(
            viewModel.uiState.value.form.isSubmitting
        )

        releaseRequest.complete(Unit)

        assertNotNull(
            viewModel.uiState.value.form.submitted
        )
        assertFalse(
            viewModel.uiState.value.form.isSubmitting
        )
    }

    @Test
    fun editingDuringSubmissionDoesNotChangeSubmittedForm() =
        runTest {
            val container = workerContainer()

            val releaseRequest =
                CompletableDeferred<Unit>()

            val delayedRepository =
                object : SafetyRepository
                by container.safetyRepository {

                    override suspend fun submitInspection(
                        projectId: Int,
                        inspectorId: Int,
                        passed: Boolean,
                        comments: String
                    ): SafetyInspection {
                        releaseRequest.await()

                        return container.safetyRepository
                            .submitInspection(
                                projectId = projectId,
                                inspectorId = inspectorId,
                                passed = passed,
                                comments = comments
                            )
                    }
                }

            val viewModel = viewModel(
                container,
                delayedRepository
            )

            keepCollecting(viewModel.uiState)
            answerEveryItem(viewModel)

            viewModel.onNotesChange(
                "Original notes"
            )

            val originalAnswers =
                viewModel.uiState.value.form.answers

            val firstItem =
                viewModel.uiState.value.items.first()

            viewModel.onSubmit()

            viewModel.onNotesChange(
                "Changed during submission"
            )
            viewModel.onAnswer(
                firstItem.itemId,
                false
            )
            viewModel.onStartAgain()

            assertEquals(
                "Original notes",
                viewModel.uiState.value.form.notes
            )
            assertEquals(
                originalAnswers,
                viewModel.uiState.value.form.answers
            )

            releaseRequest.complete(Unit)

            assertNotNull(
                viewModel.uiState.value.form.submitted
            )
        }

    @Test
    fun cancellationResetsBusyStateAndPreservesForm() =
        runTest {
            val container = workerContainer()

            val cancellingRepository =
                object : SafetyRepository
                by container.safetyRepository {

                    override suspend fun submitInspection(
                        projectId: Int,
                        inspectorId: Int,
                        passed: Boolean,
                        comments: String
                    ): SafetyInspection {
                        throw CancellationException(
                            "Request cancelled"
                        )
                    }
                }

            val viewModel = viewModel(
                container,
                cancellingRepository
            )

            keepCollecting(viewModel.uiState)
            answerEveryItem(viewModel)

            viewModel.onNotesChange(
                "Keep these notes"
            )

            val originalAnswers =
                viewModel.uiState.value.form.answers

            viewModel.onSubmit()

            assertFalse(
                viewModel.uiState.value.form.isSubmitting
            )
            assertNull(
                viewModel.uiState.value.form.submitted
            )
            assertEquals(
                originalAnswers,
                viewModel.uiState.value.form.answers
            )
            assertEquals(
                "Keep these notes",
                viewModel.uiState.value.form.notes
            )
        }

    @Test
    fun successfulSubmissionCannotBeSubmittedAgain() =
        runTest {
            val container = workerContainer()

            var attempts = 0

            val countingRepository =
                object : SafetyRepository
                by container.safetyRepository {

                    override suspend fun submitInspection(
                        projectId: Int,
                        inspectorId: Int,
                        passed: Boolean,
                        comments: String
                    ): SafetyInspection {
                        attempts++

                        return container.safetyRepository
                            .submitInspection(
                                projectId = projectId,
                                inspectorId = inspectorId,
                                passed = passed,
                                comments = comments
                            )
                    }
                }

            val viewModel = viewModel(
                container,
                countingRepository
            )

            keepCollecting(viewModel.uiState)
            answerEveryItem(viewModel)

            viewModel.onSubmit()
            viewModel.onSubmit()

            assertEquals(1, attempts)
            assertNotNull(
                viewModel.uiState.value.form.submitted
            )
            assertFalse(
                viewModel.uiState.value.canSubmit
            )
        }
}