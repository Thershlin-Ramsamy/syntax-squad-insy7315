package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Role
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeUserRepository(seed: FakeSeedData) : UserRepository {

    private val users = MutableStateFlow(seed.users)
    private val roles = seed.roles

    override fun observeUsers(): Flow<List<User>> = users

    override fun observeUser(userId: Int): Flow<User?> =
        users.map { list -> list.firstOrNull { it.userId == userId } }

    override fun observeUsersWithRole(role: UserRole): Flow<List<User>> =
        users.map { list -> list.filter { it.role == role } }

    override fun observeRoles(): Flow<List<Role>> = flowOf(roles)

    override suspend fun assignRole(userId: Int, role: UserRole) {
        users.update { list ->
            list.map { if (it.userId == userId) it.copy(role = role) else it }
        }
    }

    /** Synchronous lookup for the other fakes, which need names for summaries. */
    internal fun findUser(userId: Int): User? = users.value.firstOrNull { it.userId == userId }

    internal fun findByEmail(email: String): User? =
        users.value.firstOrNull { it.email.equals(email, ignoreCase = true) }
}
