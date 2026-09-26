package com.listamercado.app.util

import com.listamercado.app.model.ShoppingItem
import java.text.NumberFormat

object PriceUnitHelper {
    fun editorAmount(internalPrice: Double, unit: String): Double = when (unit) {
        "g", "mL" -> internalPrice * 1000.0
        else -> internalPrice
    }

    fun internalAmount(displayPrice: Double, unit: String): Double = when (unit) {
        "g", "mL" -> displayPrice / 1000.0
        else -> displayPrice
    }

    fun priceUnit(unit: String): String = when (unit) {
        "g" -> "kg"
        "mL" -> "L"
        else -> unit
    }

    fun inputHint(unit: String): String = when (unit) {
        "g" -> "Preço por kg"
        "mL" -> "Preço por L"
        else -> "Preço unitário"
    }

    fun normalizedPrice(item: ShoppingItem): Double = editorAmount(item.unitPrice, item.unit)

    fun normalizedUnit(item: ShoppingItem): String = priceUnit(item.unit)

    fun formattedUnitPrice(item: ShoppingItem, currency: NumberFormat): String {
        if (item.unitPrice <= 0.0) return "Sem preço"
        return "${currency.format(normalizedPrice(item))}/${normalizedUnit(item)}"
    }
}
