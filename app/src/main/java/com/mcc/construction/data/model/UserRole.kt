package com.mcc.construction.data.model

/**
 * The three roles in the ROLES table.
 *
 * [roleId] mirrors `ROLES.RoleID`, so a role coming back from the API as an id
 * maps straight onto this enum with [fromRoleId]. Display text lives in
 * string resources (see `ui.labelRes`), never here, so the data layer stays
 * free of UI concerns.
 *
 * Declared in the order the UI lists them — most common role first.
 */
enum class UserRole(val roleId: Int) {
    WORKER(3),
    MANAGER(2),
    ADMINISTRATOR(1);

    companion object {
        fun fromRoleId(roleId: Int): UserRole? = entries.firstOrNull { it.roleId == roleId }
    }
}
