package com.mcc.construction.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppForeground {

    private val mutableActive = MutableStateFlow(false)

    val active: StateFlow<Boolean> = mutableActive.asStateFlow()

    fun setActive(active: Boolean) {
        mutableActive.value = active
    }
}