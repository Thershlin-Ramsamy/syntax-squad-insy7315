package com.mcc.construction.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.StockLevel
import com.mcc.construction.data.repository.InventoryRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.ui.common.appContainer
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** A material with the names its row shows. */
data class InventoryItem(val item: Inventory, val projectName: String, val supplierName: String) {
    val stockValue: BigDecimal get() = item.unitCost.multiply(BigDecimal(item.quantityAvailable))
}

data class InventoryUiState(
    /** Null until the first load. */
    val items: List<InventoryItem>? = null,
    /** Projects that hold stock, for the filter. */
    val projects: List<Project> = emptyList(),
    /** Null shows every project. */
    val projectFilter: Int? = null,
    val lowStockOnly: Boolean = false,
    val selectedMaterialId: Int? = null
) {
    private val inProject: List<InventoryItem>
        get() = items.orEmpty().filter { projectFilter == null || it.item.projectId == projectFilter }

    val visible: List<InventoryItem>
        get() = inProject.filter { !lowStockOnly || it.item.stockLevel != StockLevel.IN_STOCK }

    val lowStockCount: Int get() = inProject.count { it.item.stockLevel != StockLevel.IN_STOCK }

    val totalValue: BigDecimal get() = inProject.fold(BigDecimal.ZERO) { sum, it -> sum + it.stockValue }

    fun countFor(projectId: Int?): Int = items.orEmpty().count { projectId == null || it.item.projectId == projectId }

    val paneMaterialId: Int?
        get() = selectedMaterialId?.takeIf { id -> visible.any { it.item.materialId == id } }
            ?: visible.firstOrNull()?.item?.materialId
}

/** Stock on every site: quantity, unit and supplier, with low stock called out. */
class InventoryViewModel(
    inventoryRepository: InventoryRepository,
    projectRepository: ProjectRepository
) : ViewModel() {

    private val filters = MutableStateFlow(Filters())

    private val items = combine(
        inventoryRepository.observeInventory(),
        inventoryRepository.observeSuppliers(),
        projectRepository.observeProjects()
    ) { inventory, suppliers, projects ->
        val projectNames = projects.associate { it.projectId to it.projectName }
        val supplierNames = suppliers.associate { it.supplierId to it.companyName }
        val list = inventory
            .sortedWith(compareByDescending<Inventory> { it.stockLevel }.thenBy { it.materialName })
            .map { InventoryItem(it, projectNames[it.projectId].orEmpty(), supplierNames[it.supplierId].orEmpty()) }
        val holding = inventory.map { it.projectId }.toSet()
        list to projects.filter { it.projectId in holding }
    }

    val uiState: StateFlow<InventoryUiState> = combine(items, filters) { (list, projects), f ->
        InventoryUiState(
            items = list,
            projects = projects,
            projectFilter = f.projectId,
            lowStockOnly = f.lowStockOnly,
            selectedMaterialId = f.selected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), InventoryUiState())

    fun onProjectFilterChange(projectId: Int?) = filters.update { it.copy(projectId = projectId) }

    fun onLowStockOnlyChange(value: Boolean) = filters.update { it.copy(lowStockOnly = value) }

    fun onSelectMaterial(materialId: Int) = filters.update { it.copy(selected = materialId) }

    private data class Filters(val projectId: Int? = null, val lowStockOnly: Boolean = false, val selected: Int? = null)

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                InventoryViewModel(c.inventoryRepository, c.projectRepository)
            }
        }
    }
}
