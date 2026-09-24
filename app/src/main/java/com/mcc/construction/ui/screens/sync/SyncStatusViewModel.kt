package com.mcc.construction.ui.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.OfflineSync
import com.mcc.construction.data.model.SyncStatus
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.SyncResult
import com.mcc.construction.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncStatusUiState(
    val status: SyncStatus = SyncStatus.Initial,
    val history: List<OfflineSync> = emptyList(),
    /** How many changes the last manual sync uploaded, until the next one starts. */
    val lastUploadCount: Int? = null
) {
    val canSyncNow: Boolean
        get() = status.isOnline && !status.isSyncing && status.pendingCount > 0
}

class SyncStatusViewModel(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val lastUploadCount = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SyncStatusUiState> = combine(
        syncRepository.status,
        syncRepository.observeHistory(),
        lastUploadCount
    ) { status, history, uploaded ->
        SyncStatusUiState(status = status, history = history, lastUploadCount = uploaded)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SyncStatusUiState(status = syncRepository.status.value)
    )

    fun onSyncNow() {
        lastUploadCount.value = null
        viewModelScope.launch {
            val result = syncRepository.syncNow()
            if (result is SyncResult.Success) lastUploadCount.value = result.uploaded
        }
    }

    fun onOfflineModeChange(enabled: Boolean) {
        lastUploadCount.value = null
        viewModelScope.launch { syncRepository.setOfflineMode(enabled) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer { SyncStatusViewModel(appContainer().syncRepository) }
        }
    }
}
