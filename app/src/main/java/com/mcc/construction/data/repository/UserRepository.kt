package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Role
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import kotlinx.coroutines.flow.Flow

/** Users and roles — the employee directory and administrator user management. */
interface UserRepository {

    fun observeUsers(): Flow<List<User>>

    fun observeUser(userId: Int): Flow<User?>

    fun observeUsersWithRole(role: UserRole): Flow<List<User>>

    fun observeRoles(): Flow<List<Role>>

    /** Mirrors the class diagram's `Role.assignRole(userID, roleID)`. Recorded in the audit log. */
    suspend fun assignRole(userId: Int, role: UserRole)

    /** Suspends ([active] false) or restores an account. Recorded in the audit log. */
    suspend fun setActive(userId: Int, active: Boolean)
}
