package com.listamercado.app.model

data class ShoppingItem(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    var quantity: Double = 1.0,
    var purchasedQuantity: Double = 0.0,
    var unit: String = "un",
    var unitPrice: Double = 0.0,
    var category: String = "Outros",
    var note: String = "",
    var purchased: Boolean = false
) {
    /** Quantidade planejada × preço unitário. Usado na estimativa da lista. */
    val subtotal: Double
        get() = quantity * unitPrice

    /** Quantidade realmente comprada × preço unitário. */
    val purchasedSubtotal: Double
        get() = purchasedQuantity.coerceAtLeast(0.0) * unitPrice

    /** Garante uma quantidade real útil ao concluir uma compra antiga/sem preenchimento. */
    fun ensurePurchasedQuantity() {
        if (purchasedQuantity <= 0.0) purchasedQuantity = quantity.coerceAtLeast(0.01)
    }
}
