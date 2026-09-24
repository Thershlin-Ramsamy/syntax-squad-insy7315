package com.mcc.construction.data.model

/**
 * A row of the ROLES table. Most code works with [UserRole] directly; this
 * type exists for screens that list or describe roles, such as user
 * management.
 */
data class Role(
    val roleId: Int,
    val roleName: String,
    val description: String
) {
    val type: UserRole? get() = UserRole.fromRoleId(roleId)
}

/**
 * A row of the USERS table.
 *
 * `USERS.RoleID` is resolved to [role] rather than carried as a bare id — every
 * screen needs the role, and resolving it once here saves each of them a lookup.
 * `PasswordHash` is deliberately absent: it never leaves the server.
 *
 * [jobTitle] and [employeeNumber] are not in the ERD yet. They are shown on
 * profile screens and are listed in docs/ARCHITECTURE.md under "Fields beyond
 * the ERD" so the backend team can add them to the USERS table or the API.
 */
data class User(
    val userId: Int,
    val role: UserRole,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val jobTitle: String,
    val employeeNumber: String
) {
    val roleId: Int get() = role.roleId
    val fullName: String get() = "$firstName $lastName"
}
