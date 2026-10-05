package com.mcc.construction.data.remote

import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.repository.UserFacingException
import org.junit.Test

class WritePolicyTest {

    @Test
    fun connectedOnlineModeAllowsWrite() {
        WritePolicy.requireOnline(
            SyncStatus.Initial.copy(
                isNetworkAvailable = true,
                isOfflineMode = false
            )
        )
    }

    @Test(expected = UserFacingException::class)
    fun missingConnectionRejectsWrite() {
        WritePolicy.requireOnline(
            SyncStatus.Initial.copy(
                isNetworkAvailable = false,
                isOfflineMode = false
            )
        )
    }

    @Test(expected = UserFacingException::class)
    fun simulatedOfflineModeRejectsWriteEvenWithNetwork() {
        WritePolicy.requireOnline(
            SyncStatus.Initial.copy(
                isNetworkAvailable = true,
                isOfflineMode = true
            )
        )
    }
}