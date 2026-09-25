package com.listamercado.app.data

import android.content.Context
import com.listamercado.app.model.ShoppingItem
import org.json.JSONArray
import org.json.JSONObject

class ShoppingRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): MutableList<ShoppingItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return mutableListOf()
        return runCatching {
            val array = JSONArray(raw)
            MutableList(array.length()) { index -> array.getJSONObject(index).toItem() }
        }.getOrElse { mutableListOf() }
    }

    fun save(items: List<ShoppingItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("unitPrice", item.unitPrice)
                    put("category", item.category)
                    put("note", item.note)
                    put("purchased", item.purchased)
                }
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun JSONObject.toItem() = ShoppingItem(
        id = optLong("id", System.currentTimeMillis()),
        name = optString("name", "Item"),
        quantity = optDouble("quantity", 1.0),
        unit = optString("unit", "un"),
        unitPrice = optDouble("unitPrice", 0.0),
        category = optString("category", "Outros"),
        note = optString("note", ""),
        purchased = optBoolean("purchased", false)
    )

    companion object {
        private const val PREFS_NAME = "lista_mercado"
        private const val KEY_ITEMS = "shopping_items"
    }
}
