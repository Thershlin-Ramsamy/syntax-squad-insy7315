package com.mcc.construction.data.model

/** A row of the SUPPLIERS table. */
data class Supplier(
    val supplierId: Int,
    val companyName: String,
    val contactPerson: String,
    val phone: String,
    val email: String
)
