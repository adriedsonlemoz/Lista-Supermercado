package com.listamercado.app.model

data class ShoppingList(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var budget: Double = 0.0,
    var marketKey: String? = null,
    var marketName: String? = null,
    val items: MutableList<ShoppingItem> = mutableListOf()
) {
    /** Estimativa usa a quantidade planejada. */
    val estimatedTotal: Double
        get() = items.sumOf { it.subtotal }

    /** Carrinho usa somente a quantidade realmente comprada dos itens concluídos. */
    val purchasedTotal: Double
        get() = items.filter { it.purchased }.sumOf { it.purchasedSubtotal }

    val purchasedCount: Int
        get() = items.count { it.purchased }

    val budgetRemaining: Double
        get() = budget - estimatedTotal
}
