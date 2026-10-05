package com.mcc.construction.data.remote

import android.content.Context
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SystemSettingsRepository
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackendSessionPolicy(
    context: Context,
    private val auth: AuthRepository,
    private val settings: SystemSettingsRepository,
    private val clock: Clock,
    scope: CoroutineScope
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "mcc_idle_" + SupabaseConfig.fromBuildConfig().url.hashCode(),
            Context.MODE_PRIVATE
        )

    private val lock = Any()
    private var key: String? = null
    private var lastActivity: Long = 0L
    private var sessionStamp: String? = null

    init {
        scope.launch {
            while (true) {
                try {
                    expireIfNecessary()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // Retry expiry on the next tick.
                }

                delay(1_000L)
            }
        }
    }

    private fun loadActivity(): Boolean {
        val session = auth.session.value ?: return false
        val stamp = "${session.user.userId}:${session.signedInAt}"

        if (stamp != sessionStamp) {
            sessionStamp = stamp
            key = "user_${session.user.userId}"
            lastActivity = preferences.getLong(
                requireNotNull(key),
                clock.millis()
            )
        }

        return true
    }

    fun beginSession(userId: Int) = synchronized(lock) {
        key = "user_$userId"
        lastActivity = clock.millis()
        sessionStamp = null

        preferences.edit()
            .putLong(requireNotNull(key), lastActivity)
            .apply()
    }

    fun recordInteraction() = synchronized(lock) {
        if (
            loadActivity() &&
            !IdleTimeout.expired(
                clock.millis(),
                lastActivity,
                settings.settings.value.sessionTimeoutMinutes
            )
        ) {
            lastActivity = clock.millis()

            preferences.edit()
                .putLong(requireNotNull(key), lastActivity)
                .apply()
        }
    }

    suspend fun expireIfNecessary() {
        val stamp = synchronized(lock) {
            if (!loadActivity()) {
                key?.let {
                    preferences.edit().remove(it).apply()
                }

                key = null
                sessionStamp = null
                null
            } else if (
                IdleTimeout.expired(
                    clock.millis(),
                    lastActivity,
                    settings.settings.value.sessionTimeoutMinutes
                )
            ) {
                sessionStamp
            } else {
                null
            }
        }

        if (
            stamp != null &&
            auth.session.value?.let {
                "${it.user.userId}:${it.signedInAt}"
            } == stamp
        ) {
            auth.signOut()
        }
    }
}