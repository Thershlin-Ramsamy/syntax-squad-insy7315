package com.mcc.construction.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.StockLevel
import com.mcc.construction.data.model.Supplier
import com.mcc.construction.data.repository.InventoryRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.runBackendMutation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class StockChange {
    USE, DELIVERY
}

data class MaterialDetailUiState(
    val isLoading: Boolean = true,
    val item: Inventory? = null,
    val supplier: Supplier? = null,
    val project: Project? = null,
    val amount: String = "",
    val isSaving: Boolean = false,
    val message: UiMessage? = null
) {
    val amountValue: Int?
        get() = amount.toIntOrNull()?.takeIf { it > 0 }
}

class MaterialDetailViewModel(
    materialId: Int,
    private val inventoryRepository: InventoryRepository,
    projectRepository: ProjectRepository
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())
    private val itemFlow = inventoryRepository.observeItem(materialId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val related = itemFlow.flatMapLatest { item ->
        if (item == null) {
            flowOf(null to null)
        } else {
            combine(
                inventoryRepository.observeSupplier(item.supplierId),
                projectRepository.observeProject(item.projectId)
            ) { supplier, project ->
                supplier to project
            }
        }
    }

    val uiState: StateFlow<MaterialDetailUiState> =
        combine(itemFlow, related, transient) {
                item, (supplier, project), t ->
            MaterialDetailUiState(
                isLoading = false,
                item = item,
                supplier = supplier,
                project = project,
                amount = t.amount,
                isSaving = t.isSaving,
                message = t.message
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            MaterialDetailUiState()
        )

    fun onAmountChange(value: String) {
        transient.update {
            it.copy(
                amount = value.filter(Char::isDigit).take(MAX_DIGITS)
            )
        }
    }

    fun onQuickAmount(value: Int) {
        transient.update { it.copy(amount = value.toString()) }
    }

    fun onAdjust(change: StockChange) {
        val state = uiState.value
        val item = state.item ?: return

        if (state.isSaving) return

        val amount = state.amountValue

        if (amount == null) {
            transient.update {
                it.copy(
                    message = UiMessage(
                        R.string.material_amount_required,
                        isError = true
                    )
                )
            }
            return
        }

        if (
            change == StockChange.USE &&
            amount > item.quantityAvailable
        ) {
            transient.update {
                it.copy(
                    message = UiMessage(
                        R.string.material_not_enough,
                        listOf(
                            item.quantityAvailable,
                            item.unitType
                        ),
                        isError = true
                    )
                )
            }
            return
        }

        transient.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            runBackendMutation(
                onFinally = {
                    transient.update { it.copy(isSaving = false) }
                }
            ) {
                val delta = if (change == StockChange.USE) {
                    -amount
                } else {
                    amount
                }

                inventoryRepository.adjustStock(item.materialId, delta)

                val left = item.quantityAvailable + delta
                val nowLow = item.copy(
                    quantityAvailable = left
                ).stockLevel != StockLevel.IN_STOCK

                val text = when {
                    change == StockChange.DELIVERY ->
                        R.string.material_delivery_recorded
                    nowLow ->
                        R.string.material_use_recorded_low
                    else ->
                        R.string.material_use_recorded
                }

                transient.update {
                    Transient(
                        message = UiMessage(
                            text,
                            listOf(amount, item.unitType, left)
                        )
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        transient.update { it.copy(message = null) }
    }

    private data class Transient(
        val amount: String = "",
        val isSaving: Boolean = false,
        val message: UiMessage? = null
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val MAX_DIGITS = 6

        val QUICK_AMOUNTS = listOf(1, 5, 10, 25, 50)

        fun factory(materialId: Int) = viewModelFactory {
            initializer {
                val c = appContainer()
                MaterialDetailViewModel(
                    materialId,
                    c.inventoryRepository,
                    c.projectRepository
                )
            }
        }
    }
}