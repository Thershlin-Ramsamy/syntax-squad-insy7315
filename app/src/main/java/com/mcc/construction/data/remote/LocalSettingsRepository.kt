package com.mcc.construction.data.remote

import android.content.Context
import com.mcc.construction.data.model.AppSettings
import com.mcc.construction.data.repository.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LocalSettingsRepository(
    context: Context
) : SettingsRepository {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "mcc_device_settings",
            Context.MODE_PRIVATE
        )

    private val mutex = Mutex()

    private val mutableSettings = MutableStateFlow(
        AppSettings(
            notificationsEnabled = preferences.getBoolean(
                NOTIFICATIONS_ENABLED,
                true
            ),
            vibrationEnabled = preferences.getBoolean(
                VIBRATION_ENABLED,
                true
            )
        )
    )

    override val settings: StateFlow<AppSettings> =
        mutableSettings.asStateFlow()

    override suspend fun setNotificationsEnabled(
        enabled: Boolean
    ) {
        update { current ->
            current.copy(notificationsEnabled = enabled)
        }
    }

    override suspend fun setVibrationEnabled(
        enabled: Boolean
    ) {
        update { current ->
            current.copy(vibrationEnabled = enabled)
        }
    }

    private suspend fun update(
        transform: (AppSettings) -> AppSettings
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val updated = transform(mutableSettings.value)

                val saved = preferences.edit()
                    .putBoolean(
                        NOTIFICATIONS_ENABLED,
                        updated.notificationsEnabled
                    )
                    .putBoolean(
                        VIBRATION_ENABLED,
                        updated.vibrationEnabled
                    )
                    .commit()

                if (!saved) {
                    throw IOException(
                        "Device preferences could not be saved."
                    )
                }

                mutableSettings.value = updated
            }
        }
    }

    companion object {
        private const val NOTIFICATIONS_ENABLED =
            "notifications_enabled"

        private const val VIBRATION_ENABLED =
            "vibration_enabled"
    }
}