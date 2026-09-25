package com.listamercado.app.data

import android.content.Context
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import org.json.JSONArray
import org.json.JSONObject

class ShoppingRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadLists(): MutableList<ShoppingList> {
        val raw = prefs.getString(KEY_LISTS, null)
        if (!raw.isNullOrBlank()) {
            return runCatching {
                val array = JSONArray(raw)
                MutableList(array.length()) { index -> array.getJSONObject(index).toList() }
            }.getOrElse { mutableListOf() }
        }
        return migrateLegacyItems()
    }

    fun saveLists(lists: List<ShoppingList>) {
        val array = JSONArray()
        lists.forEach { list -> array.put(list.toJson()) }
        prefs.edit().putString(KEY_LISTS, array.toString()).apply()
    }

    private fun migrateLegacyItems(): MutableList<ShoppingList> {
        val raw = prefs.getString(KEY_LEGACY_ITEMS, null) ?: return mutableListOf()
        val items = runCatching {
            val array = JSONArray(raw)
            MutableList(array.length()) { index -> array.getJSONObject(index).toItem() }
        }.getOrElse { mutableListOf() }

        if (items.isEmpty()) return mutableListOf()

        val now = System.currentTimeMillis()
        val migrated = mutableListOf(
            ShoppingList(
                id = now,
                name = "Minha lista",
                createdAt = now,
                updatedAt = now,
                items = items
            )
        )
        saveLists(migrated)
        prefs.edit().remove(KEY_LEGACY_ITEMS).apply()
        return migrated
    }

    private fun ShoppingList.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("items", JSONArray().apply { items.forEach { put(it.toJson()) } })
    }

    private fun ShoppingItem.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("quantity", quantity)
        put("unit", unit)
        put("unitPrice", unitPrice)
        put("category", category)
        put("note", note)
        put("purchased", purchased)
    }

    private fun JSONObject.toList(): ShoppingList {
        val itemArray = optJSONArray("items") ?: JSONArray()
        return ShoppingList(
            id = optLong("id", System.currentTimeMillis()),
            name = optString("name", "Lista de compras"),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            updatedAt = optLong("updatedAt", System.currentTimeMillis()),
            items = MutableList(itemArray.length()) { index -> itemArray.getJSONObject(index).toItem() }
        )
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
        private const val KEY_LISTS = "shopping_lists_v2"
        private const val KEY_LEGACY_ITEMS = "shopping_items"
    }
}
