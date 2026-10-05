package com.mcc.construction.ui.common

import androidx.lifecycle.ViewModel
import com.mcc.construction.ui.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubmissionFailureTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class TestViewModel : ViewModel()

    @Test
    fun failureResetsBusyAndKeepsDraft() = runTest {
        val viewModel = TestViewModel()
        var busy = false
        var draft = "Keep this report"
        var confirmed = false

        viewModel.launchSubmission(
            isBusy = { busy },
            setBusy = { busy = it },
            failureMessage = "Test submission failed."
        ) {
            throw IllegalStateException("Simulated failure")
            @Suppress("UNREACHABLE_CODE")
            run {
                draft = ""
                confirmed = true
            }
        }

        assertFalse(busy)
        assertFalse(confirmed)
        assertEquals("Keep this report", draft)
    }

    @Test
    fun cancellationResetsBusy() = runTest {
        val viewModel = TestViewModel()
        var busy = false

        viewModel.launchSubmission(
            isBusy = { busy },
            setBusy = { busy = it },
            failureMessage = "Cancelled test."
        ) {
            throw CancellationException("Cancelled")
        }

        assertFalse(busy)
    }

    @Test
    fun duplicateTapDoesNotStartAnotherRequest() = runTest {
        val viewModel = TestViewModel()
        val release = CompletableDeferred<Unit>()
        var busy = false
        var attempts = 0

        fun submit() {
            viewModel.launchSubmission(
                isBusy = { busy },
                setBusy = { busy = it },
                failureMessage = "Test submission failed."
            ) {
                attempts++
                release.await()
            }
        }

        submit()
        submit()

        assertTrue(busy)
        assertEquals(1, attempts)

        release.complete(Unit)

        assertFalse(busy)
    }
}