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
        val lists = if (!raw.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(raw)
                MutableList(array.length()) { index -> array.getJSONObject(index).toList() }
            }.getOrElse { mutableListOf() }
        } else {
            migrateLegacyItems()
        }
        return ensureCyclingTripStarterList(lists)
    }

    fun saveLists(lists: List<ShoppingList>) {
        val array = JSONArray()
        lists.forEach { list -> array.put(list.toJson()) }
        prefs.edit().putString(KEY_LISTS, array.toString()).apply()
    }

    private fun ensureCyclingTripStarterList(lists: MutableList<ShoppingList>): MutableList<ShoppingList> {
        if (prefs.getBoolean(KEY_CICLOVIAGEM_CREATED, false)) return lists

        val alreadyExists = lists.any { it.name.equals(CICLOVIAGEM_NAME, ignoreCase = true) }
        if (!alreadyExists) {
            lists.add(0, createCyclingTripList())
            saveLists(lists)
        }

        prefs.edit().putBoolean(KEY_CICLOVIAGEM_CREATED, true).apply()
        return lists
    }

    private fun createCyclingTripList(): ShoppingList {
        val now = System.currentTimeMillis()
        val items = listOf(
            starterItem(now, 1, "Arroz branco", 5.0, "kg", "Mercearia"),
            starterItem(now, 2, "Óleo de soja", 900.0, "mL", "Mercearia"),
            starterItem(now, 3, "Sal", 1.0, "kg", "Mercearia"),
            starterItem(now, 4, "Tempero pronto", 300.0, "g", "Mercearia"),
            starterItem(now, 5, "Macarrão instantâneo", 20.0, "pacote", "Mercearia"),
            starterItem(now, 6, "Ovos", 30.0, "un", "Mercearia"),
            starterItem(now, 7, "Farinha de mandioca", 1.0, "kg", "Mercearia"),
            starterItem(now, 8, "Batata", 2.0, "kg", "Hortifruti"),
            starterItem(now, 9, "Cenoura", 1.0, "kg", "Hortifruti"),
            starterItem(now, 10, "Pepino", 1.0, "kg", "Hortifruti"),
            starterItem(now, 11, "Repolho", 1.0, "un", "Hortifruti"),
            starterItem(now, 12, "Cebola", 1.0, "kg", "Hortifruti"),
            starterItem(now, 13, "Tomate", 1.0, "kg", "Hortifruti"),
            starterItem(now, 14, "Abobrinha", 500.0, "g", "Hortifruti"),
            starterItem(now, 15, "Beterraba", 500.0, "g", "Hortifruti"),
            starterItem(now, 16, "Pimentão", 500.0, "g", "Hortifruti"),
            starterItem(now, 17, "Sardinha", 5.0, "lata", "Mercearia"),
            starterItem(now, 18, "Suco em pó", 20.0, "un", "Bebidas")
        ).toMutableList()

        return ShoppingList(
            id = now,
            name = CICLOVIAGEM_NAME,
            createdAt = now,
            updatedAt = now,
            items = items
        )
    }

    private fun starterItem(
        baseId: Long,
        offset: Int,
        name: String,
        quantity: Double,
        unit: String,
        category: String
    ) = ShoppingItem(
        id = baseId + offset,
        name = name,
        quantity = quantity,
        unit = unit,
        unitPrice = 0.0,
        category = category,
        note = "",
        purchased = false
    )

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
        private const val KEY_CICLOVIAGEM_CREATED = "starter_cicloviagem_created"
        private const val CICLOVIAGEM_NAME = "Cicloviagem"
    }
}
