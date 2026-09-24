package com.mcc.construction.navigation

import com.mcc.construction.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavMenusTest {

    private fun routes(role: UserRole, compact: Boolean, heightDp: Int = 900) =
        NavMenus.visibleItems(role, compact, heightDp).map { it.route }

    @Test
    fun workerPhoneBarIsHomeTasksMessagesProfile() {
        assertEquals(
            listOf(Routes.WORKER_DASHBOARD, Routes.TASKS, Routes.MESSAGES, Routes.PROFILE),
            routes(UserRole.WORKER, compact = true)
        )
    }

    @Test
    fun managerPhoneBarIsDashboardProjectsEmployeesInventoryReports() {
        assertEquals(
            listOf(
                Routes.MANAGER_DASHBOARD,
                Routes.PROJECTS,
                Routes.EMPLOYEES,
                Routes.INVENTORY,
                Routes.REPORTS
            ),
            routes(UserRole.MANAGER, compact = true)
        )
    }

    @Test
    fun phoneBarNeverHoldsMoreThanFiveItems() {
        UserRole.entries.forEach { role ->
            assertTrue(role.name, routes(role, compact = true).size <= 5)
        }
    }

    @Test
    fun railAddsTheFullerMenuWhenThereIsRoom() {
        UserRole.entries.forEach { role ->
            val expected = NavMenus.primary(role) + NavMenus.secondary(role)
            assertEquals(role.name, expected, NavMenus.visibleItems(role, false, 900))
        }
    }

    @Test
    fun shortRailDropsSecondaryItemsButKeepsEveryPrimaryOne() {
        UserRole.entries.forEach { role ->
            assertEquals(role.name, NavMenus.primary(role), NavMenus.visibleItems(role, false, 360))
        }
    }

    @Test
    fun mediumHeightRailShowsOnlyWhatFits() {
        // (600 - 48) / 64 = 8 items fit.
        assertEquals(8, NavMenus.visibleItems(UserRole.MANAGER, false, 600).size)
    }

    @Test
    fun everyMenuRouteIsDrawnInsideTheChrome() {
        UserRole.entries.forEach { role ->
            (NavMenus.primary(role) + NavMenus.secondary(role)).forEach {
                assertTrue(it.route, it.route in NavMenus.chromeRoutes)
            }
        }
    }

    @Test
    fun homeRouteIsTheFirstPrimaryItem() {
        UserRole.entries.forEach { role ->
            assertEquals(NavMenus.primary(role).first().route, NavMenus.homeRoute(role))
        }
    }
}
