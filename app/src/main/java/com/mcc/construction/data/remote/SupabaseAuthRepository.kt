package com.mcc.construction.data.remote

import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.remote.dto.UserRow
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SignInResult
import com.mcc.construction.data.repository.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SupabaseAuthRepository(
    private val client: SupabaseClient,
    scope: CoroutineScope,
    private val clock: Clock
) : AuthRepository {

    var onExplicitSignIn: (Int) -> Unit = {}

    private val mutex = Mutex()
    private val mutableSession = MutableStateFlow<UserSession?>(null)

    override val session: StateFlow<UserSession?> =
        mutableSession.asStateFlow()

    private val mutableProfileReadFailed = MutableStateFlow(false)

    val profileReadFailed: StateFlow<Boolean> =
        mutableProfileReadFailed.asStateFlow()

    init {
        scope.launch {
            client.auth.awaitInitialization()

            val profileRefresh = flow {
                while (true) {
                    emit(Unit)
                    delay(PROFILE_REFRESH_MILLIS)
                }
            }

            merge(
                client.auth.sessionStatus.map { Unit },
                profileRefresh
            ).collect {
                mutex.withLock {
                    refreshApplicationSession()
                }
            }
        }
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): SignInResult {
        client.auth.awaitInitialization()

        return mutex.withLock {
            mutableSession.value = null
            mutableProfileReadFailed.value = false

            try {
                client.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }

                val accountId = client.auth.currentUserOrNull()?.id
                    ?: throw BackendSessionRequiredException()

                val profile = readProfile(accountId)

                if (profile == null) {
                    client.auth.clearSession()
                    return@withLock SignInResult.UnknownAccount
                }

                if (!profile.isActive) {
                    client.auth.clearSession()
                    return@withLock SignInResult.AccountSuspended
                }

                val applicationSession = UserSession(
                    user = mapUser(profile),
                    signedInAt = LocalDateTime.now(clock)
                )

                onExplicitSignIn(profile.userId)
                mutableSession.value = applicationSession

                SignInResult.Success(applicationSession)
            } catch (exception: CancellationException) {
                withContext(NonCancellable) {
                    client.auth.clearSession()
                    mutableSession.value = null
                }

                throw exception
            } catch (exception: AuthRestException) {
                client.auth.clearSession()
                mutableSession.value = null

                if (exception.error == "invalid_credentials") {
                    SignInResult.InvalidPassword
                } else {
                    throw exception
                }
            } catch (exception: Exception) {
                client.auth.clearSession()
                mutableSession.value = null
                mutableProfileReadFailed.value = true
                throw exception
            }
        }
    }

    override suspend fun requestPasswordReset(email: String) {
        client.auth.awaitInitialization()

        client.auth.resetPasswordForEmail(
            email = email.trim()
        )
    }

    override suspend fun signOut() {
        client.auth.awaitInitialization()

        mutex.withLock {
            try {
                client.auth.signOut()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Local sign-out still completes if the server is unavailable.
            } finally {
                withContext(NonCancellable) {
                    client.auth.clearSession()
                    mutableSession.value = null
                    mutableProfileReadFailed.value = false
                }
            }
        }
    }

    private suspend fun refreshApplicationSession() {
        val accountId = client.auth.currentUserOrNull()?.id

        if (accountId == null) {
            mutableSession.value = null
            mutableProfileReadFailed.value = false
            return
        }

        try {
            val profile = readProfile(accountId)

            if (profile == null || !profile.isActive) {
                mutableSession.value = null
                mutableProfileReadFailed.value = false
                client.auth.clearSession()
                return
            }

            val previous = mutableSession.value

            val signedInAt = if (
                previous?.user?.userId == profile.userId
            ) {
                previous.signedInAt
            } else {
                LocalDateTime.now(clock)
            }

            mutableSession.value = UserSession(
                user = mapUser(profile),
                signedInAt = signedInAt
            )

            mutableProfileReadFailed.value = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            mutableSession.value = null
            mutableProfileReadFailed.value = true
        }
    }

    private suspend fun readProfile(accountId: String): UserRow? {
        val rows = client.from("users").select {
            filter {
                eq("auth_user_id", accountId)
            }
        }.decodeList<UserRow>()

        if (client.auth.currentUserOrNull()?.id != accountId) {
            throw BackendAccountChangedException()
        }

        return rows.singleOrNull()
    }

    private fun mapUser(row: UserRow): User {
        val role = requireNotNull(
            UserRole.fromRoleId(row.roleId)
        ) {
            "Unsupported application role."
        }

        return User(
            userId = row.userId,
            role = role,
            firstName = row.firstName,
            lastName = row.lastName,
            email = row.email,
            phone = row.phone,
            jobTitle = row.jobTitle,
            employeeNumber = row.employeeNumber,
            isActive = row.isActive
        )
    }

    companion object {
        private const val PROFILE_REFRESH_MILLIS = 30_000L
    }
}