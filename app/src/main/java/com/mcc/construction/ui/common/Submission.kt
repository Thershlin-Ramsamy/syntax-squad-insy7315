package com.mcc.construction.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.construction.data.repository.UserFacingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Launches one submission.
 *
 * The caller owns its form and only clears it after confirmed success.
 * Busy state is set synchronously to prevent duplicate taps.
 */
fun ViewModel.launchSubmission(
    isBusy: () -> Boolean,
    setBusy: (Boolean) -> Unit,
    failureMessage: String,
    action: suspend () -> Unit
) {
    if (isBusy()) return

    setBusy(true)

    viewModelScope.launch {
        try {
            action()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: UserFacingException) {
            BackendFeedback.reportFailure(exception.userMessage)
        } catch (_: Exception) {
            BackendFeedback.reportFailure(failureMessage)
        } finally {
            setBusy(false)
        }
    }
}