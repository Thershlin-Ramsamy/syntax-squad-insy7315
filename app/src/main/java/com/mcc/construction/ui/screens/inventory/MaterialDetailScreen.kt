package com.mcc.construction.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.StockLevel
import com.mcc.construction.data.model.Supplier
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AccentButton
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.ChoiceChip
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.format.formatRand
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.tone
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Material Details wired to its ViewModel.
 *
 * @param embedded true in the inventory list's side pane.
 */
@Composable
fun MaterialDetailRoute(
    materialId: Int,
    onBack: () -> Unit,
    onOpenProject: (Int) -> Unit,
    embedded: Boolean = false,
    viewModel: MaterialDetailViewModel = viewModel(
        key = "material-$materialId",
        factory = MaterialDetailViewModel.factory(materialId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MaterialDetailScreen(
        state = state,
        embedded = embedded,
        onBack = onBack,
        onOpenProject = onOpenProject,
        onAmountChange = viewModel::onAmountChange,
        onQuickAmount = viewModel::onQuickAmount,
        onAdjust = viewModel::onAdjust,
        onMessageShown = viewModel::onMessageShown
    )
}

/** Stock level against the minimum, supplier contact, project, and a way to record use or a delivery. */
@Composable
fun MaterialDetailScreen(
    state: MaterialDetailUiState,
    embedded: Boolean,
    onBack: () -> Unit,
    onOpenProject: (Int) -> Unit,
    onAmountChange: (String) -> Unit,
    onQuickAmount: (Int) -> Unit,
    onAdjust: (StockChange) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.item
    val twoColumns = LocalWindowLayout.current.isExpanded && !embedded
    ActionFeedbackHost(state.message, onMessageShown, modifier) {
        ScreenScaffold(
            title = if (embedded) null else item?.materialName ?: stringResource(R.string.material_title),
            onBack = if (embedded) null else onBack,
            maxContentWidth = if (twoColumns) WIDE_CONTENT else Spacing.contentMaxWidth
        ) {
            when {
                state.isLoading -> LoadingState()
                item == null -> EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = stringResource(R.string.material_not_found_title),
                    message = stringResource(R.string.material_not_found_message)
                )
                twoColumns -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                    Column(Modifier.weight(1f)) {
                        Stock(item, embedded = false)
                        Spacer(Modifier.height(Spacing.sectionGap))
                        Adjust(state, item, onAmountChange, onQuickAmount, onAdjust)
                    }
                    Column(Modifier.weight(1f)) {
                        SupplierCard(state.supplier)
                        Spacer(Modifier.height(Spacing.sectionGap))
                        ProjectCard(state.project, onOpenProject)
                    }
                }
                else -> {
                    Stock(item, embedded)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    Adjust(state, item, onAmountChange, onQuickAmount, onAdjust)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    SupplierCard(state.supplier)
                    Spacer(Modifier.height(Spacing.sectionGap))
                    ProjectCard(state.project, onOpenProject)
                }
            }
        }
    }
}

@Composable
private fun Stock(item: Inventory, embedded: Boolean) {
    if (embedded) {
        Text(item.materialName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Spacing.md))
    }
    if (item.stockLevel != StockLevel.IN_STOCK) {
        Banner(
            title = stringResource(if (item.stockLevel == StockLevel.OUT) R.string.material_out_title else R.string.material_low_title),
            message = stringResource(R.string.material_low_message, item.minimumQuantity, item.unitType),
            icon = Icons.Filled.RemoveCircleOutline,
            tone = item.stockLevel.tone
        )
        Spacer(Modifier.height(Spacing.itemGap))
    }
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.quantityAvailable.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.material_on_hand, item.unitType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(stringResource(item.stockLevel.labelRes), item.stockLevel.tone)
        }
        Spacer(Modifier.height(Spacing.md))
        // The bar is full at twice the minimum, so "just above minimum" reads as half-empty.
        val scale = (item.minimumQuantity * 2).coerceAtLeast(1)
        ProgressTrack(progress = item.quantityAvailable / scale.toFloat(), tone = item.stockLevel.tone)
        Spacer(Modifier.height(Spacing.lg))
        KeyValueRow(stringResource(R.string.material_minimum), stringResource(R.string.inventory_quantity, item.minimumQuantity, item.unitType))
        Spacer(Modifier.height(Spacing.sm))
        KeyValueRow(stringResource(R.string.material_unit_cost), item.unitCost.formatRand())
        Spacer(Modifier.height(Spacing.sm))
        KeyValueRow(
            stringResource(R.string.material_stock_value),
            item.unitCost.multiply(BigDecimal(item.quantityAvailable)).formatRand()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Adjust(
    state: MaterialDetailUiState,
    item: Inventory,
    onAmountChange: (String) -> Unit,
    onQuickAmount: (Int) -> Unit,
    onAdjust: (StockChange) -> Unit
) {
    SectionHeader(title = stringResource(R.string.material_adjust))
    Spacer(Modifier.height(Spacing.blockGap))
    AppTextField(
        value = state.amount,
        onValueChange = onAmountChange,
        label = stringResource(R.string.material_amount, item.unitType),
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.md))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        MaterialDetailViewModel.QUICK_AMOUNTS.forEach { amount ->
            ChoiceChip(
                label = amount.toString(),
                selected = state.amountValue == amount,
                onClick = { onQuickAmount(amount) }
            )
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    val amount = state.amountValue
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        AccentButton(
            text = if (amount == null) stringResource(R.string.material_record_use) else stringResource(R.string.material_record_use_amount, amount),
            onClick = { onAdjust(StockChange.USE) },
            icon = Icons.Filled.RemoveCircleOutline,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text = if (amount == null) stringResource(R.string.material_record_delivery) else stringResource(R.string.material_record_delivery_amount, amount),
            onClick = { onAdjust(StockChange.DELIVERY) },
            icon = Icons.Filled.LocalShipping,
            loading = state.isSaving,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SupplierCard(supplier: Supplier?) {
    SectionHeader(title = stringResource(R.string.material_supplier))
    Spacer(Modifier.height(Spacing.blockGap))
    AppCard(Modifier.fillMaxWidth()) {
        if (supplier == null) {
            Text(stringResource(R.string.material_supplier_unknown), style = MaterialTheme.typography.bodyMedium)
            return@AppCard
        }
        InfoRow(Icons.Filled.Business, stringResource(R.string.material_supplier_company), supplier.companyName)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Person, stringResource(R.string.material_supplier_contact), supplier.contactPerson)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Phone, stringResource(R.string.profile_phone), supplier.phone)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Email, stringResource(R.string.profile_email), supplier.email)
    }
}

@Composable
private fun ProjectCard(project: Project?, onOpenProject: (Int) -> Unit) {
    if (project == null) return
    SectionHeader(title = stringResource(R.string.material_project))
    Spacer(Modifier.height(Spacing.blockGap))
    AppCard(Modifier.fillMaxWidth(), onClick = { onOpenProject(project.projectId) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(project.projectName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(project.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(stringResource(project.status.labelRes), project.status.tone)
        }
    }
}

private val WIDE_CONTENT: Dp = 1120.dp

private fun previewState(): MaterialDetailUiState {
    val today = LocalDate.now()
    return MaterialDetailUiState(
        isLoading = false,
        item = Inventory(2, 1, 2, "Rebar Y12 (6 m lengths)", 84, 90, "bundles", BigDecimal("742.00")),
        supplier = Supplier(2, "Macsteel", "Pieter du Toit", "+27 11 871 0000", "orders@macsteel.co.za"),
        project = Project(1, 2, "Riverside Estate Phase 2", "Centurion, Gauteng", BigDecimal.ONE, today, today, ProjectStatus.ACTIVE, 68.0),
        amount = "10"
    )
}

@DevicePreviews
@Composable
private fun MaterialDetailPreview() {
    PreviewSurface {
        MaterialDetailScreen(previewState(), embedded = false, onBack = {}, onOpenProject = {}, onAmountChange = {}, onQuickAmount = {}, onAdjust = {}, onMessageShown = {})
    }
}
