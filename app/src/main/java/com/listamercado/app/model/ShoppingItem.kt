package com.listamercado.app.model

data class ShoppingItem(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    var quantity: Double = 1.0,
    var unit: String = "un",
    var unitPrice: Double = 0.0,
    var category: String = "Outros",
    var note: String = "",
    var purchased: Boolean = false
) {
    val subtotal: Double
        get() = quantity * unitPrice
}
