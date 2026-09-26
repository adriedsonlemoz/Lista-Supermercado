package com.listamercado.app.model

data class CatalogProduct(
    val id: Long,
    var name: String,
    var normalizedName: String,
    var category: String,
    var unit: String,
    var lastUnitPrice: Double = 0.0,
    var barcode: String? = null,
    var favorite: Boolean = false,
    var recurringFrequency: String? = null,
    var lastQuantity: Double = 1.0,
    var lastRecurringAddedAt: Long = 0L,
    var updatedAt: Long = System.currentTimeMillis()
)
