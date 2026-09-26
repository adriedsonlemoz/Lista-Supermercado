package com.listamercado.app.model

data class ShoppingList(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var budget: Double = 0.0,
    val items: MutableList<ShoppingItem> = mutableListOf()
) {
    val estimatedTotal: Double
        get() = items.sumOf { it.subtotal }

    val purchasedTotal: Double
        get() = items.filter { it.purchased }.sumOf { it.subtotal }

    val purchasedCount: Int
        get() = items.count { it.purchased }

    val budgetRemaining: Double
        get() = budget - estimatedTotal
}
