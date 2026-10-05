package com.mcc.construction

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mcc.construction.data.remote.AppForeground
import com.mcc.construction.navigation.AppRoot
import com.mcc.construction.security.IdleSessionTimer
import com.mcc.construction.ui.adaptive.WindowWidthClass
import com.mcc.construction.ui.common.BackendFeedback
import com.mcc.construction.ui.common.BackendFeedbackHost
import com.mcc.construction.ui.theme.MCCConstructionTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container
        get() = (application as MccApplication).container

    private var sessionLocked by mutableStateOf(false)
    private var signOutRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val forcedWidthClass = when (
            intent?.getStringExtra("forceWidthClass")?.lowercase()
        ) {
            "compact" -> WindowWidthClass.Compact
            "medium" -> WindowWidthClass.Medium
            "expanded" -> WindowWidthClass.Expanded
            else -> null
        }

        setContent {
            MCCConstructionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (sessionLocked) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AppRoot(
                            forcedWidthClass = forcedWidthClass
                        )
                    }

                    BackendFeedbackHost()
                }
            }
        }

        lifecycleScope.launch {
            container.authRepository.session.collect { session ->
                if (session == null) {
                    /*
                     * Remove timing from a completed application session.
                     * A new sign-in starts a new idle period.
                     */
                    idleTimer.clear()
                } else {
                    idleTimer.attach(
                        session.user.userId,
                        SystemClock.elapsedRealtime()
                    )
                    checkIdleSession()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    checkIdleSession()
                    delay(1_000L)
                }
            }
        }

        lifecycleScope.launch {
            container.systemSettingsRepository.settings.collect {
                checkIdleSession()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        /*
         * Expiry is checked before foreground synchronization resumes.
         */
        checkIdleSession()
        AppForeground.setActive(!sessionLocked)
    }

    override fun onStop() {
        AppForeground.setActive(false)
        super.onStop()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        if (sessionLocked) return

        val session = container.authRepository.session.value ?: return
        idleTimer.attach(
            session.user.userId,
            SystemClock.elapsedRealtime()
        )

        val accepted = idleTimer.recordActivity(
            nowMillis = SystemClock.elapsedRealtime(),
            timeoutMinutes = container.systemSettingsRepository
                .settings.value.sessionTimeoutMinutes
        )

        if (!accepted) {
            expireSession()
        }
    }

    private fun checkIdleSession() {
        if (sessionLocked) return

        val session = container.authRepository.session.value ?: return

        idleTimer.attach(
            session.user.userId,
            SystemClock.elapsedRealtime()
        )

        if (
            idleTimer.isExpired(
                nowMillis = SystemClock.elapsedRealtime(),
                timeoutMinutes = container.systemSettingsRepository
                    .settings.value.sessionTimeoutMinutes
            )
        ) {
            expireSession()
        }
    }

    private fun expireSession() {
        if (signOutRunning) return

        signOutRunning = true
        sessionLocked = true
        AppForeground.setActive(false)

        lifecycleScope.launch {
            try {
                /*
                 * SupabaseAuthRepository.signOut() already clears its
                 * local session in finally when the server is unreachable.
                 */
                container.authRepository.signOut()

                idleTimer.clear()
                sessionLocked = false

                BackendFeedback.reportFailure(
                    "Your session expired because of inactivity. " +
                            "Sign in again to continue."
                )

                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    AppForeground.setActive(true)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                /*
                 * Keep protected content hidden if sign-out unexpectedly
                 * fails. A recreated activity will check expiry again.
                 */
                BackendFeedback.reportFailure(
                    "Your session has expired. Close and reopen the app " +
                            "to sign in again."
                )
            } finally {
                signOutRunning = false
            }
        }
    }

    private companion object {
        val idleTimer = IdleSessionTimer()
    }
}