package com.mcc.construction.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A refresh revision shared by the real repositories.
 *
 * Repositories will refresh after a successful write, an Auth change,
 * a Realtime event or reconnect.
 *
 * This revision is not a cache and contains no user data.
 */
class BackendRefreshBus {
    private val mutableRevision = MutableStateFlow(0L)

    val revision: StateFlow<Long> =
        mutableRevision.asStateFlow()

    fun invalidate() {
        mutableRevision.update { current ->
            if (current == Long.MAX_VALUE) {
                0L
            } else {
                current + 1L
            }
        }
    }
}