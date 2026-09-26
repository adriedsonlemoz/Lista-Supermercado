package com.listamercado.app.model

data class CatalogProduct(
    val id: Long,
    var name: String,
    var normalizedName: String,
    var category: String,
    var unit: String,
    var lastUnitPrice: Double = 0.0,
    var barcode: String? = null,
    var updatedAt: Long = System.currentTimeMillis()
)
