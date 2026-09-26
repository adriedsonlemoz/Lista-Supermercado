package com.listamercado.app.model

data class ListTemplate(
    val id: Long,
    var name: String,
    val builtIn: Boolean = false,
    var budget: Double = 0.0,
    val items: MutableList<ShoppingItem> = mutableListOf(),
    var updatedAt: Long = System.currentTimeMillis()
)
