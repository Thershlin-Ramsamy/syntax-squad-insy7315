package com.mcc.construction.ui.screens.employees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.EmployeeStatus
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.tone

private const val ALL = "All"

@Composable
fun EmployeeListScreen(navController: NavHostController) {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    val employees = MockRepository.employees

    val options = remember { listOf(ALL) + EmployeeStatus.entries.map { it.label } }
    val filtered = if (filter == ALL) employees else employees.filter { it.status.label == filter }

    Column(Modifier.fillMaxSize()) {
        SegmentedFilter(
            options = options,
            selected = filter,
            onSelect = { filter = it },
            modifier = Modifier.padding(
                horizontal = Spacing.screenH,
                vertical = Spacing.md
            )
        )

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Groups,
                title = "Nobody $filter",
                message = "No employees currently have this status."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.screenH,
                    end = Spacing.screenH,
                    top = Spacing.xs,
                    bottom = Spacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { employee ->
                    ListRow(
                        title = employee.name,
                        subtitle = employee.jobTitle,
                        meta = employee.project,
                        onClick = {
                            navController.navigate(Routes.employeeProfile(employee.id))
                        },
                        leading = { Avatar(employee.name) },
                        trailing = {
                            StatusPill(employee.status.label, employee.status.tone)
                        }
                    )
                }
            }
        }
    }
}
