package com.listamercado.app.model

data class BarcodeLookupResult(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val quantityDescription: String? = null,
    val category: String? = null,
    val source: String = "Open Food Facts"
)
