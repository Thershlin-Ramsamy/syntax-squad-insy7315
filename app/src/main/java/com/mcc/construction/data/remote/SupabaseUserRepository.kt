package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Role
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.remote.dto.RoleRow
import com.mcc.construction.data.remote.dto.UserRow
import com.mcc.construction.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseUserRepository(
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport
) : UserRepository {
    override fun observeUsers(): Flow<List<User>> {
        return reads.observeRows<UserRow>("users", "user_id")
            .map { rows ->
                rows.map { it.toModel() }
                    .sortedWith(
                        compareBy<User> { it.lastName.lowercase() }
                            .thenBy { it.firstName.lowercase() }
                            .thenBy { it.userId }
                    )
            }
    }

    override fun observeUser(userId: Int): Flow<User?> {
        return observeUsers().map { users ->
            users.firstOrNull { it.userId == userId }
        }
    }

    override fun observeUsersWithRole(
        role: UserRole
    ): Flow<List<User>> {
        return observeUsers().map { users ->
            users.filter { it.role == role }
        }
    }

    override fun observeRoles(): Flow<List<Role>> {
        return reads.observeRows<RoleRow>("roles", "role_id")
            .map { rows ->
                rows.map { it.toModel() }
                    .sortedBy { it.roleId }
            }
    }

    override suspend fun assignRole(
        userId: Int,
        role: UserRole
    ) {
        require(userId > 0)

        writes.execute(
            BackendAction.USER_ROLE,
            buildJsonObject {
                put("user_id", userId)
                put("role_id", role.roleId)
            }
        )
    }

    override suspend fun setActive(
        userId: Int,
        active: Boolean
    ) {
        require(userId > 0)

        writes.execute(
            BackendAction.USER_ACTIVE,
            buildJsonObject {
                put("user_id", userId)
                put("active", active)
            }
        )
    }
}