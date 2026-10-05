package com.mcc.construction.ui.common

import kotlinx.coroutines.CancellationException

/**
 * Failed operations do not advance success UI.
 * Loading state is cleared even when the operation is cancelled.
 */
suspend fun runBackendMutation(
    onFailure: (Exception) -> Unit = {
        BackendFeedback.reportFailure()
    },
    onFinally: () -> Unit = {},
    block: suspend () -> Unit
) {
    try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        onFailure(exception)
    } finally {
        onFinally()
    }
}