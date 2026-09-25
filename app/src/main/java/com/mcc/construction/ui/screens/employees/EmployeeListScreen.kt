package com.mcc.construction.ui.screens.employees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.ListDetailLayout
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.MetricInline
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SearchField
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.components.selectedOutline
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun EmployeeListRoute(
    onOpenEmployee: (Int) -> Unit,
    detailPane: (@Composable (userId: Int) -> Unit)? = null,
    viewModel: EmployeeListViewModel = viewModel(factory = EmployeeListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val twoPane = LocalWindowLayout.current.isExpanded && detailPane != null
    EmployeeListScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onRoleFilterChange = viewModel::onRoleFilterChange,
        onEmployeeClick = { id -> if (twoPane) viewModel.onSelectEmployee(id) else onOpenEmployee(id) },
        detailPane = if (twoPane) detailPane else null
    )
}

/** The workforce: role, today's attendance and open work, searchable by name. */
@Composable
fun EmployeeListScreen(
    state: EmployeeListUiState,
    onQueryChange: (String) -> Unit,
    onRoleFilterChange: (UserRole?) -> Unit,
    onEmployeeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    detailPane: (@Composable (userId: Int) -> Unit)? = null
) {
    if (state.employees == null) {
        LoadingState(modifier)
        return
    }
    if (detailPane == null) {
        ScreenScaffold(modifier = modifier, scrollable = false) {
            EmployeeColumn(state, onQueryChange, onRoleFilterChange, onEmployeeClick, highlightSelection = false)
        }
        return
    }
    ListDetailLayout(
        selected = state.paneEmployeeId,
        emptyIcon = Icons.Filled.Groups,
        emptyTitle = stringResource(R.string.employees_pick_title),
        emptyMessage = stringResource(R.string.employees_pick_message),
        modifier = modifier,
        list = { EmployeeColumn(state, onQueryChange, onRoleFilterChange, onEmployeeClick, highlightSelection = true) },
        detail = detailPane
    )
}

@Composable
private fun ColumnScope.EmployeeColumn(
    state: EmployeeListUiState,
    onQueryChange: (String) -> Unit,
    onRoleFilterChange: (UserRole?) -> Unit,
    onEmployeeClick: (Int) -> Unit,
    highlightSelection: Boolean
) {
    SearchField(
        query = state.query,
        onQueryChange = onQueryChange,
        label = stringResource(R.string.employees_search),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.md))
    val selected = if (highlightSelection) state.paneEmployeeId else null
    // The search box stays put; the filters and roll call scroll with the list.
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item(key = "filters") {
            ChoiceChipGroup(
                options = listOf(null, UserRole.WORKER, UserRole.MANAGER),
                selected = state.roleFilter,
                onSelect = onRoleFilterChange,
                label = { role ->
                    val name = if (role == null) stringResource(R.string.filter_all) else stringResource(role.labelRes)
                    stringResource(R.string.filter_with_count, name, state.countFor(role))
                }
            )
        }
        item(key = "roll-call") {
            AppCard(Modifier.fillMaxWidth().padding(bottom = Spacing.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    MetricInline(state.countWith(PresenceStatus.ON_SITE).toString(), stringResource(R.string.presence_on_site), Modifier.weight(1f), Tone.Success)
                    MetricInline(state.countWith(PresenceStatus.FINISHED).toString(), stringResource(R.string.presence_finished), Modifier.weight(1f), Tone.Info)
                    MetricInline(state.countWith(PresenceStatus.NOT_IN).toString(), stringResource(R.string.presence_not_in), Modifier.weight(1f), Tone.Warning)
                }
            }
        }
        if (state.visible.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.PersonSearch,
                    title = stringResource(R.string.employees_empty_title),
                    message = stringResource(R.string.employees_empty_message, state.query.trim())
                )
            }
        }
        items(state.visible, key = { it.user.userId }) { item ->
            EmployeeRow(item, selected = item.user.userId == selected) { onEmployeeClick(item.user.userId) }
        }
    }
}

@Composable
private fun EmployeeRow(item: EmployeeItem, selected: Boolean, onClick: () -> Unit) {
    val user = item.user
    ListRow(
        title = user.fullName,
        subtitle = user.jobTitle,
        meta = if (user.isActive) item.today.detailText() else stringResource(R.string.employee_suspended_detail),
        onClick = onClick,
        leading = { Avatar(user.fullName) },
        trailing = {
            if (user.isActive) {
                StatusPill(stringResource(item.today.status.labelRes), item.today.status.tone, showDot = true)
            } else {
                StatusPill(stringResource(R.string.employee_suspended), Tone.Danger, showDot = true)
            }
        },
        modifier = Modifier.selectedOutline(selected)
    )
}

internal fun previewEmployees(): List<EmployeeItem> {
    val now = LocalDateTime.now().withHour(7).withMinute(2)
    fun user(id: Int, first: String, last: String, job: String, role: UserRole = UserRole.WORKER) =
        User(id, role, first, last, "${first.lowercase()}@mcc.co.za", "+27 82 445 1120", job, "MCC-20$id")
    return listOf(
        EmployeeItem(user(5, "Sipho", "Dlamini", "Site Operative"), AttendanceToday(PresenceStatus.ON_SITE, Attendance(1, 5, LocalDate.now(), now, null, null)), 3),
        EmployeeItem(user(6, "Thabo", "Mokoena", "Bricklayer"), AttendanceToday(PresenceStatus.FINISHED, null, 6.5), 2),
        EmployeeItem(user(8, "Anele", "Mthembu", "Electrician"), AttendanceToday.NotIn, 1),
        EmployeeItem(user(3, "Johan", "Botha", "Site Foreman", UserRole.MANAGER), AttendanceToday.NotIn, 0)
    )
}

@DevicePreviews
@Composable
private fun EmployeeListPreview() {
    PreviewSurface {
        EmployeeListScreen(EmployeeListUiState(employees = previewEmployees(), roleFilter = null), {}, {}, {})
    }
}

@DevicePreviews
@Composable
private fun EmployeeListTwoPanePreview() {
    PreviewSurface {
        EmployeeListScreen(
            EmployeeListUiState(employees = previewEmployees(), roleFilter = null),
            {}, {}, {},
            detailPane = { Text("Employee $it") }
        )
    }
}
