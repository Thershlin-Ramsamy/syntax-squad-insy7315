package com.mcc.construction.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.StockLevel
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListDetailLayout
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.MetricInline
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SwitchRow
import com.mcc.construction.ui.components.selectedOutline
import com.mcc.construction.ui.format.formatRandWhole
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun InventoryRoute(
    onOpenMaterial: (Int) -> Unit,
    detailPane: (@Composable (materialId: Int) -> Unit)? = null,
    viewModel: InventoryViewModel = viewModel(factory = InventoryViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val twoPane = LocalWindowLayout.current.isExpanded && detailPane != null
    InventoryScreen(
        state = state,
        onProjectFilterChange = viewModel::onProjectFilterChange,
        onLowStockOnlyChange = viewModel::onLowStockOnlyChange,
        onMaterialClick = { id -> if (twoPane) viewModel.onSelectMaterial(id) else onOpenMaterial(id) },
        detailPane = if (twoPane) detailPane else null
    )
}

/** Materials on site with quantity, unit and supplier. Anything below its minimum stands out. */
@Composable
fun InventoryScreen(
    state: InventoryUiState,
    onProjectFilterChange: (Int?) -> Unit,
    onLowStockOnlyChange: (Boolean) -> Unit,
    onMaterialClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    detailPane: (@Composable (materialId: Int) -> Unit)? = null
) {
    if (state.items == null) {
        LoadingState(modifier)
        return
    }
    if (detailPane == null) {
        ScreenScaffold(modifier = modifier, scrollable = false) {
            InventoryColumn(state, onProjectFilterChange, onLowStockOnlyChange, onMaterialClick, highlightSelection = false)
        }
        return
    }
    ListDetailLayout(
        selected = state.paneMaterialId,
        emptyIcon = Icons.Filled.Inventory2,
        emptyTitle = stringResource(R.string.inventory_pick_title),
        emptyMessage = stringResource(R.string.inventory_pick_message),
        modifier = modifier,
        list = { InventoryColumn(state, onProjectFilterChange, onLowStockOnlyChange, onMaterialClick, highlightSelection = true) },
        detail = detailPane
    )
}

@Composable
private fun ColumnScope.InventoryColumn(
    state: InventoryUiState,
    onProjectFilterChange: (Int?) -> Unit,
    onLowStockOnlyChange: (Boolean) -> Unit,
    onMaterialClick: (Int) -> Unit,
    highlightSelection: Boolean
) {
    val selected = if (highlightSelection) state.paneMaterialId else null
    // Filters and totals scroll with the list, so a short window still shows materials.
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item(key = "filters") {
            Column {
                ChoiceChipGroup(
                    options = listOf<Int?>(null) + state.projects.map { it.projectId },
                    selected = state.projectFilter,
                    onSelect = onProjectFilterChange,
                    label = { id ->
                        val name = if (id == null) stringResource(R.string.filter_all_projects) else state.projects.first { it.projectId == id }.projectName
                        stringResource(R.string.filter_with_count, name, state.countFor(id))
                    }
                )
                Spacer(Modifier.height(Spacing.md))
                AppCard(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        MetricInline(state.visible.size.toString(), stringResource(R.string.inventory_metric_items), Modifier.weight(1f))
                        MetricInline(
                            state.lowStockCount.toString(),
                            stringResource(R.string.inventory_metric_low),
                            Modifier.weight(1f),
                            if (state.lowStockCount > 0) Tone.Danger else Tone.Success
                        )
                        MetricInline(state.totalValue.formatRandWhole(), stringResource(R.string.inventory_metric_value), Modifier.weight(1.6f))
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                SwitchRow(
                    title = stringResource(R.string.inventory_low_only),
                    checked = state.lowStockOnly,
                    onCheckedChange = onLowStockOnlyChange,
                    icon = Icons.Filled.WarningAmber,
                    tone = Tone.Warning
                )
            }
        }
        if (state.visible.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = stringResource(if (state.lowStockOnly) R.string.inventory_no_low_title else R.string.inventory_empty_title),
                    message = stringResource(if (state.lowStockOnly) R.string.inventory_no_low_message else R.string.inventory_empty_message),
                    tone = if (state.lowStockOnly) Tone.Success else Tone.Neutral
                )
            }
        }
        items(state.visible, key = { it.item.materialId }) { entry ->
            MaterialRow(entry, selected = entry.item.materialId == selected) { onMaterialClick(entry.item.materialId) }
        }
    }
}

@Composable
private fun MaterialRow(entry: InventoryItem, selected: Boolean, onClick: () -> Unit) {
    val item = entry.item
    val low = item.stockLevel != StockLevel.IN_STOCK
    ListRow(
        title = item.materialName,
        subtitle = if (low) {
            stringResource(
                R.string.inventory_quantity_of_minimum,
                stringResource(item.stockLevel.labelRes),
                item.quantityAvailable,
                item.unitType,
                item.minimumQuantity
            )
        } else {
            stringResource(R.string.inventory_quantity, item.quantityAvailable, item.unitType)
        },
        meta = stringResource(R.string.inventory_row_meta, entry.supplierName, entry.projectName),
        onClick = onClick,
        leading = { LeadingIcon(if (low) Icons.Filled.WarningAmber else Icons.Filled.Inventory2, item.stockLevel.tone) },
        modifier = Modifier.selectedOutline(selected)
    )
}

internal fun previewInventory(): InventoryUiState {
    val today = LocalDate.now()
    val riverside = Project(1, 2, "Riverside Estate Phase 2", "Centurion", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0)
    val harbour = riverside.copy(projectId = 2, projectName = "Harbour View Offices")
    return InventoryUiState(
        items = listOf(
            InventoryItem(Inventory(4, 2, 4, "Torch-on waterproofing membrane (4 mm)", 0, 15, "rolls", BigDecimal("1340")), harbour.projectName, "Derbigum SA"),
            InventoryItem(Inventory(2, 1, 2, "Rebar Y12 (6 m lengths)", 84, 90, "bundles", BigDecimal("742")), riverside.projectName, "Macsteel"),
            InventoryItem(Inventory(1, 1, 1, "Portland cement CEM II 42.5N (50 kg)", 240, 100, "bags", BigDecimal("118.50")), riverside.projectName, "PPC Cement"),
            InventoryItem(Inventory(7, 1, 6, "NFP face bricks", 18500, 10000, "bricks", BigDecimal("3.85")), riverside.projectName, "Corobrik")
        ),
        projects = listOf(riverside, harbour)
    )
}

@DevicePreviews
@Composable
private fun InventoryPreview() {
    PreviewSurface { InventoryScreen(previewInventory(), {}, {}, {}) }
}

@DevicePreviews
@Composable
private fun InventoryTwoPanePreview() {
    PreviewSurface { InventoryScreen(previewInventory(), {}, {}, {}, detailPane = { Text("Material $it") }) }
}
