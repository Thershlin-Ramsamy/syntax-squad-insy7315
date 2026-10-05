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
import androidx.compose.ui.res.stringResource
import com.mcc.construction.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object BackendFeedback {

    private val channel = Channel<Unit>(
        capacity = Channel.BUFFERED
    )

    val failures = channel.receiveAsFlow()

    fun reportFailure() {
        channel.trySend(Unit)
    }
}

/**
 * Does not expose exception text, tokens, URLs, or server internals to users.
 * Cancellation remains cancellation.
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

@Composable
fun BackendFeedbackHost() {
    var visible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        BackendFeedback.failures.collect {
            visible = true
        }
    }

    if (visible) {
        AlertDialog(
            onDismissRequest = {
                visible = false
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
                    stringResource(
                        R.string.backend_operation_failed
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        visible = false
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