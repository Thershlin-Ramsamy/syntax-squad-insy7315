package com.mcc.construction.data.remote

import com.mcc.construction.data.model.SystemSettings

object SyncSchedule {

    fun intervalMillis(minutes: Int): Long {
        require(minutes in SystemSettings.SYNC_INTERVAL_OPTIONS) {
            "Unsupported sync interval."
        }

        return minutes.toLong() * 60_000L
    }
}