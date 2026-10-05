package com.mcc.construction.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.stringResource
import com.mcc.construction.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Application-authored feedback.
 *
 * Do not populate customMessage from exception text, credentials,
 * tokens or raw server responses.
 */
data class BackendFailure(
    val customMessage: String? = null
)

object BackendFeedback {

    private val channel = Channel<BackendFailure>(
        capacity = Channel.BUFFERED
    )

    val failures = channel.receiveAsFlow()

    fun reportFailure() {
        channel.trySend(BackendFailure())
    }

    fun reportFailure(message: String) {
        channel.trySend(
            BackendFailure(
                customMessage = message
            )
        )
    }
}

/**
 * Existing callers remain compatible.
 *
 * Cancellation propagates. Other failures show safe feedback and
 * return null.
 */
suspend fun <T : Any> backendAttempt(
    block: suspend () -> T
): T? {
    return try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        BackendFeedback.reportFailure()
        null
    }
}

/**
 * MainActivity already includes this host.
 *
 * Each message remains visible until dismissed.
 */
@Composable
fun BackendFeedbackHost() {
    var currentFailure by remember {
        mutableStateOf<BackendFailure?>(null)
    }

    LaunchedEffect(Unit) {
        BackendFeedback.failures.collect { failure ->
            currentFailure = failure

            snapshotFlow {
                currentFailure
            }.first { it == null }
        }
    }

    val failure = currentFailure

    if (failure != null) {
        AlertDialog(
            onDismissRequest = {
                currentFailure = null
            },
            title = {
                Text(
                    stringResource(
                        R.string.backend_error_title
                    )
                )
            },
            text = {
                Text(
                    failure.customMessage
                        ?: stringResource(
                            R.string.backend_operation_failed
                        )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentFailure = null
                    }
                ) {
                    Text(
                        stringResource(
                            R.string.action_close
                        )
                    )
                }
            }
        )
    }
}