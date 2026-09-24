package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Supplier
import kotlinx.coroutines.flow.Flow

/** Materials on hand (INVENTORY) and who supplies them (SUPPLIERS). */
interface InventoryRepository {

    fun observeInventory(): Flow<List<Inventory>>

    fun observeInventoryForProject(projectId: Int): Flow<List<Inventory>>

    fun observeItem(materialId: Int): Flow<Inventory?>

    fun observeSuppliers(): Flow<List<Supplier>>

    fun observeSupplier(supplierId: Int): Flow<Supplier?>

    /**
     * Mirrors the class diagram's `Inventory.updateStock()`: [change] is added
     * to the quantity on hand (negative to book stock out). The quantity never
     * drops below zero.
     */
    suspend fun adjustStock(materialId: Int, change: Int)
}
