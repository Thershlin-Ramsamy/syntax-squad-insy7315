package com.mcc.construction.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

/**
 * A one-off confirmation or error for the user — "Clocked in at 07:02",
 * "Report saved offline". ViewModels put one in their UI state; the screen
 * shows it with [com.mcc.construction.ui.components.ActionFeedbackHost] and
 * calls back to clear it.
 *
 * Holds a string resource rather than text so ViewModels stay free of
 * Android resources. [id] makes two identical messages in a row still show
 * twice.
 */
@Immutable
data class UiMessage(
    @param:StringRes val textRes: Int,
    val formatArgs: List<Any> = emptyList(),
    val isError: Boolean = false,
    val id: Long = System.nanoTime()
)

@Composable
fun UiMessage.text(): String = stringResource(textRes, *formatArgs.toTypedArray())
