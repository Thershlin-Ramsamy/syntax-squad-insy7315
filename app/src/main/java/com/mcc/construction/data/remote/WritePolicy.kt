package com.mcc.construction.data.remote

import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.repository.UserFacingException

object WritePolicy {

    fun requireOnline(status: SyncStatus) {
        if (!status.isOnline) {
            throw UserFacingException(
                "This action needs an internet connection with offline " +
                        "mode switched off. Your entered information has been " +
                        "kept. Only messages support offline queuing."
            )
        }
    }
}