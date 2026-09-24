package com.mcc.construction.data.repository

import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import java.time.LocalDateTime
import kotlinx.coroutines.flow.StateFlow

/** The signed-in user. A null session means nobody is signed in. */
data class UserSession(
    val user: User,
    val signedInAt: LocalDateTime
) {
    val role: UserRole get() = user.role
}

/** Every way a sign-in attempt can end. */
sealed interface SignInResult {
    data class Success(val session: UserSession) : SignInResult

    /** No account matches the email address. */
    data object UnknownAccount : SignInResult

    /** The account exists but the password was rejected. */
    data object InvalidPassword : SignInResult
}

/**
 * Sign-in, sign-out and the current session.
 *
 * [session] is the single source of truth for who is signed in and in which
 * role; the navigation chrome, profile and every role-aware screen read it
 * from here rather than holding their own copy.
 */
interface AuthRepository {

    val session: StateFlow<UserSession?>

    suspend fun signIn(email: String, password: String): SignInResult

    /**
     * Asks the server to email a password-reset link. Always completes normally,
     * whether or not the address has an account, so the UI cannot be used to
     * find out which email addresses are registered.
     */
    suspend fun requestPasswordReset(email: String)

    suspend fun signOut()
}
