package com.listamercado.app.data

import android.content.Context
import com.listamercado.app.model.ListTemplate
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import org.json.JSONArray
import org.json.JSONObject

class TemplateRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadTemplates(): List<ListTemplate> = builtInTemplates() + loadUserTemplates()

    fun loadUserTemplates(): MutableList<ListTemplate> {
        val raw = prefs.getString(KEY_USER_TEMPLATES, null) ?: return mutableListOf()
        return runCatching {
            val array = JSONArray(raw)
            MutableList(array.length()) { index -> array.getJSONObject(index).toTemplate() }
        }.getOrElse { mutableListOf() }
    }

    fun saveFromList(source: ShoppingList, templateName: String) {
        val name = templateName.trim()
        if (name.isBlank()) return
        val templates = loadUserTemplates()
        val now = System.currentTimeMillis()
        val existing = templates.firstOrNull { it.name.equals(name, ignoreCase = true) }
        val copiedItems = source.items.mapIndexed { index, item ->
            item.copy(id = now + index + 1, purchased = false)
        }.toMutableList()
        if (existing == null) {
            templates += ListTemplate(
                id = now,
                name = name,
                builtIn = false,
                budget = source.budget,
                items = copiedItems,
                updatedAt = now
            )
        } else {
            existing.name = name
            existing.budget = source.budget
            existing.items.clear()
            existing.items += copiedItems
            existing.updatedAt = now
        }
        saveUserTemplates(templates)
    }

    fun deleteUserTemplate(id: Long) {
        val templates = loadUserTemplates()
        if (templates.removeAll { it.id == id }) saveUserTemplates(templates)
    }

    fun createListFromTemplate(template: ListTemplate, listName: String): ShoppingList {
        val now = System.currentTimeMillis()
        val copiedItems = template.items.mapIndexed { index, item ->
            item.copy(id = now + index + 1, purchased = false)
        }.toMutableList()
        return ShoppingList(
            id = now,
            name = listName.trim().ifBlank { template.name },
            createdAt = now,
            updatedAt = now,
            budget = template.budget,
            items = copiedItems
        )
    }

    fun replaceUserTemplates(templates: List<ListTemplate>) {
        saveUserTemplates(templates.filter { !it.builtIn }.map { template ->
            template.copy(items = template.items.map { it.copy() }.toMutableList(), builtIn = false)
        })
    }

    fun mergeUserTemplates(imported: List<ListTemplate>) {
        val current = loadUserTemplates()
        imported.filter { !it.builtIn }.forEach { incoming ->
            val existing = current.firstOrNull { it.id == incoming.id }
                ?: current.firstOrNull { it.name.equals(incoming.name, ignoreCase = true) }
            if (existing == null) {
                current += incoming.copy(items = incoming.items.map { it.copy() }.toMutableList(), builtIn = false)
            } else if (incoming.updatedAt >= existing.updatedAt) {
                existing.name = incoming.name
                existing.budget = incoming.budget
                existing.items.clear()
                existing.items += incoming.items.map { it.copy() }
                existing.updatedAt = incoming.updatedAt
            }
        }
        saveUserTemplates(current)
    }

    private fun saveUserTemplates(templates: List<ListTemplate>) {
        val array = JSONArray().apply { templates.filter { !it.builtIn }.forEach { put(it.toJson()) } }
        prefs.edit().putString(KEY_USER_TEMPLATES, array.toString()).apply()
    }

    private fun builtInTemplates(): List<ListTemplate> {
        return listOf(
            ListTemplate(
                id = BUILTIN_CICLOVIAGEM_ID,
                name = "Cicloviagem",
                builtIn = true,
                items = ShoppingRepository.cyclingTripTemplateItems(BUILTIN_ITEM_BASE)
            ),
            ListTemplate(BUILTIN_MONTHLY_ID, "Compra do mês", builtIn = true),
            ListTemplate(BUILTIN_BARBECUE_ID, "Churrasco", builtIn = true),
            ListTemplate(BUILTIN_CAMPING_ID, "Camping", builtIn = true),
            ListTemplate(BUILTIN_CLEANING_ID, "Limpeza", builtIn = true)
        )
    }

    private fun ListTemplate.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("budget", budget)
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
        put("purchased", false)
    }

    private fun JSONObject.toTemplate(): ListTemplate {
        val itemArray = optJSONArray("items") ?: JSONArray()
        return ListTemplate(
            id = optLong("id", System.currentTimeMillis()),
            name = optString("name", "Modelo").ifBlank { "Modelo" },
            builtIn = false,
            budget = optDouble("budget", 0.0).coerceAtLeast(0.0),
            items = MutableList(itemArray.length()) { index -> itemArray.getJSONObject(index).toItem() },
            updatedAt = optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun JSONObject.toItem() = ShoppingItem(
        id = optLong("id", System.currentTimeMillis()),
        name = optString("name", "Item"),
        quantity = optDouble("quantity", 1.0).coerceAtLeast(0.01),
        unit = optString("unit", "un").ifBlank { "un" },
        unitPrice = optDouble("unitPrice", 0.0).coerceAtLeast(0.0),
        category = optString("category", "Outros").ifBlank { "Outros" },
        note = optString("note", ""),
        purchased = false
    )

    companion object {
        private const val PREFS_NAME = "lista_mercado_templates"
        private const val KEY_USER_TEMPLATES = "user_templates_v1"
        private const val BUILTIN_CICLOVIAGEM_ID = -1L
        private const val BUILTIN_MONTHLY_ID = -2L
        private const val BUILTIN_BARBECUE_ID = -3L
        private const val BUILTIN_CAMPING_ID = -4L
        private const val BUILTIN_CLEANING_ID = -5L
        private const val BUILTIN_ITEM_BASE = 10_000L
    }
}
