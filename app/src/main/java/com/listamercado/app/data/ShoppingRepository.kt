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
        val withStarter = ensureCyclingTripStarterList(lists)
        return applyCyclingTripReferencePrices(withStarter)
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
        return ShoppingList(
            id = now,
            name = CICLOVIAGEM_NAME,
            createdAt = now,
            updatedAt = now,
            items = cyclingTripTemplateItems(now)
        )
    }




    private fun applyCyclingTripReferencePrices(lists: MutableList<ShoppingList>): MutableList<ShoppingList> {
        if (prefs.getBoolean(KEY_CICLOVIAGEM_PRICES_V1, false)) return lists
        val list = lists.firstOrNull { it.name.equals(CICLOVIAGEM_NAME, ignoreCase = true) }
        if (list != null) {
            val referencePrices = mapOf(
                "arroz branco" to ("kg" to 18.0 / 5.0),
                "óleo de soja" to ("mL" to 8.0 / 900.0),
                "sal" to ("kg" to 1.89),
                "tempero pronto" to ("g" to 2.50 / 300.0),
                "macarrão instantâneo" to ("pacote" to 2.0),
                "ovos" to ("un" to 18.0 / 30.0),
                "farinha de mandioca" to ("kg" to 4.39),
                "cenoura" to ("kg" to 3.00),
                "pepino" to ("kg" to 1.57),
                "repolho" to ("un" to 1.00),
                "cebola" to ("kg" to 5.50),
                "tomate" to ("kg" to 4.54),
                "abobrinha" to ("g" to 1.94 / 1000.0),
                "beterraba" to ("g" to 3.15 / 1000.0),
                "pimentão" to ("g" to 2.50 / 1000.0),
                "sardinha" to ("lata" to 6.29),
                "suco em pó" to ("un" to 0.95)
            )
            var changed = false
            list.items.forEach { item ->
                val suggested = referencePrices[item.name.trim().lowercase()]
                if (suggested != null && item.unit.equals(suggested.first, ignoreCase = true) && item.unitPrice <= 0.0) {
                    item.unitPrice = suggested.second
                    changed = true
                }
            }
            if (changed) {
                list.updatedAt = System.currentTimeMillis()
                saveLists(lists)
            }
        }
        prefs.edit().putBoolean(KEY_CICLOVIAGEM_PRICES_V1, true).apply()
        return lists
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
        put("budget", budget)
        put("marketKey", marketKey ?: JSONObject.NULL)
        put("marketName", marketName ?: JSONObject.NULL)
        put("items", JSONArray().apply { items.forEach { put(it.toJson()) } })
    }

    private fun ShoppingItem.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("quantity", quantity)
        put("purchasedQuantity", purchasedQuantity)
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
            budget = optDouble("budget", 0.0),
            marketKey = if (isNull("marketKey")) null else optString("marketKey").takeIf { it.isNotBlank() },
            marketName = if (isNull("marketName")) null else optString("marketName").takeIf { it.isNotBlank() },
            items = MutableList(itemArray.length()) { index -> itemArray.getJSONObject(index).toItem() }
        )
    }

    private fun JSONObject.toItem(): ShoppingItem {
        val plannedQuantity = optDouble("quantity", 1.0).coerceAtLeast(0.01)
        val purchased = optBoolean("purchased", false)
        val actualQuantity = if (has("purchasedQuantity")) {
            optDouble("purchasedQuantity", 0.0).coerceAtLeast(0.0)
        } else if (purchased) {
            // Migração automática: nas versões antigas, comprado significava comprar toda a quantidade planejada.
            plannedQuantity
        } else {
            0.0
        }
        return ShoppingItem(
            id = optLong("id", System.currentTimeMillis()),
            name = optString("name", "Item"),
            quantity = plannedQuantity,
            purchasedQuantity = actualQuantity,
            unit = optString("unit", "un"),
            unitPrice = optDouble("unitPrice", 0.0),
            category = optString("category", "Outros"),
            note = optString("note", ""),
            purchased = purchased
        )
    }

    companion object {
        private const val PREFS_NAME = "lista_mercado"
        private const val KEY_LISTS = "shopping_lists_v2"
        private const val KEY_LEGACY_ITEMS = "shopping_items"
        private const val KEY_CICLOVIAGEM_CREATED = "starter_cicloviagem_created"
        private const val KEY_CICLOVIAGEM_PRICES_V1 = "starter_cicloviagem_prices_v1"
        private const val CICLOVIAGEM_NAME = "Cicloviagem"

        fun cyclingTripTemplateItems(baseId: Long = System.currentTimeMillis()): MutableList<ShoppingItem> = listOf(
            templateItem(baseId, 1, "Arroz branco", 5.0, "kg", "Mercearia", 18.0 / 5.0),
            templateItem(baseId, 2, "Óleo de soja", 900.0, "mL", "Mercearia", 8.0 / 900.0),
            templateItem(baseId, 3, "Sal", 1.0, "kg", "Mercearia", 1.89),
            templateItem(baseId, 4, "Tempero pronto", 300.0, "g", "Mercearia", 2.50 / 300.0),
            templateItem(baseId, 5, "Macarrão instantâneo", 20.0, "pacote", "Mercearia", 2.0),
            templateItem(baseId, 6, "Ovos", 30.0, "un", "Mercearia", 18.0 / 30.0),
            templateItem(baseId, 7, "Farinha de mandioca", 1.0, "kg", "Mercearia", 4.39),
            templateItem(baseId, 8, "Batata", 2.0, "kg", "Hortifruti", 0.0),
            templateItem(baseId, 9, "Cenoura", 1.0, "kg", "Hortifruti", 3.00),
            templateItem(baseId, 10, "Pepino", 1.0, "kg", "Hortifruti", 1.57),
            templateItem(baseId, 11, "Repolho", 1.0, "un", "Hortifruti", 1.00),
            templateItem(baseId, 12, "Cebola", 1.0, "kg", "Hortifruti", 5.50),
            templateItem(baseId, 13, "Tomate", 1.0, "kg", "Hortifruti", 4.54),
            templateItem(baseId, 14, "Abobrinha", 500.0, "g", "Hortifruti", 1.94 / 1000.0),
            templateItem(baseId, 15, "Beterraba", 500.0, "g", "Hortifruti", 3.15 / 1000.0),
            templateItem(baseId, 16, "Pimentão", 500.0, "g", "Hortifruti", 2.50 / 1000.0),
            templateItem(baseId, 17, "Sardinha", 5.0, "lata", "Mercearia", 6.29),
            templateItem(baseId, 18, "Suco em pó", 20.0, "un", "Bebidas", 0.95)
        ).toMutableList()

        private fun templateItem(
            baseId: Long,
            offset: Int,
            name: String,
            quantity: Double,
            unit: String,
            category: String,
            unitPrice: Double
        ) = ShoppingItem(
            id = baseId + offset,
            name = name,
            quantity = quantity,
            purchasedQuantity = 0.0,
            unit = unit,
            unitPrice = unitPrice,
            category = category,
            note = "",
            purchased = false
        )
    }
}
