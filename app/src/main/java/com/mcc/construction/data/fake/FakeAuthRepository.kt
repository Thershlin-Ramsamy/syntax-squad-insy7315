package com.mcc.construction.data.fake

import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SignInResult
import com.mcc.construction.data.repository.UserSession
import java.time.Clock
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [AuthRepository].
 *
 * Any account in the seed data can sign in with any non-blank password — the
 * point is to demonstrate each role, not to check credentials. The demo
 * accounts are listed in [FakeSeedData] (worker@, manager@ and admin@mcc.co.za).
 *
 * @param latency how long a sign-in appears to take, so the loading state is
 *   visible. Tests pass [Duration.ZERO].
 */
class FakeAuthRepository(
    private val users: FakeUserRepository,
    private val clock: Clock,
    private val latency: Duration
) : AuthRepository {

    private val _session = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): SignInResult {
        delay(latency)
        val user = users.findByEmail(email.trim()) ?: return SignInResult.UnknownAccount
        if (password.isBlank()) return SignInResult.InvalidPassword

        val session = UserSession(user = user, signedInAt = LocalDateTime.now(clock))
        _session.value = session
        return SignInResult.Success(session)
    }

    override suspend fun requestPasswordReset(email: String) {
        delay(latency)
    }

    override suspend fun signOut() {
        _session.value = null
    }
}
