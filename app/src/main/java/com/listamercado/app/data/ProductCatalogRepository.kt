package com.listamercado.app.data

import android.content.Context
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

class ProductCatalogRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadProducts(): MutableList<CatalogProduct> {
        val raw = prefs.getString(KEY_PRODUCTS, null) ?: return mutableListOf()
        return runCatching {
            val array = JSONArray(raw)
            val parsed = MutableList(array.length()) { index -> array.getJSONObject(index).toProduct() }
            deduplicate(parsed)
        }.getOrElse { mutableListOf() }
    }

    fun seedFromLists(lists: List<ShoppingList>) {
        val products = loadProducts()
        val firstSeed = !prefs.getBoolean(KEY_INITIAL_SEED_DONE, false)
        var changed = false

        if (firstSeed) {
            lists.sortedBy { it.updatedAt }.forEach { list ->
                list.items.forEach { item ->
                    changed = mergeItem(products, item, barcode = null, overwriteExisting = true) || changed
                }
            }
        } else {
            lists.forEach { list ->
                list.items.forEach { item ->
                    if (findByName(products, item.name) == null) {
                        changed = mergeItem(products, item, barcode = null, overwriteExisting = false) || changed
                    }
                }
            }
        }

        if (changed || firstSeed) saveProducts(products)
        if (firstSeed) prefs.edit().putBoolean(KEY_INITIAL_SEED_DONE, true).apply()
    }

    fun recordItem(item: ShoppingItem, barcode: String? = null) {
        val products = loadProducts()
        mergeItem(products, item, sanitizeBarcode(barcode), overwriteExisting = true)
        saveProducts(products)
    }

    fun findByName(name: String): CatalogProduct? = findByName(loadProducts(), name)

    fun findByBarcode(barcode: String): CatalogProduct? {
        val normalizedBarcode = sanitizeBarcode(barcode) ?: return null
        return loadProducts().firstOrNull { it.barcode == normalizedBarcode }
    }

    fun suggestions(query: String = ""): List<CatalogProduct> {
        val normalizedQuery = normalizeName(query)
        return loadProducts()
            .asSequence()
            .filter { normalizedQuery.isBlank() || it.normalizedName.contains(normalizedQuery) }
            .sortedBy { it.name.lowercase(Locale("pt", "BR")) }
            .toList()
    }

    private fun mergeItem(
        products: MutableList<CatalogProduct>,
        item: ShoppingItem,
        barcode: String?,
        overwriteExisting: Boolean
    ): Boolean {
        val normalized = normalizeName(item.name)
        if (normalized.isBlank()) return false

        val now = System.currentTimeMillis()
        val existing = products.firstOrNull { it.normalizedName == normalized }
        if (existing != null) {
            if (!overwriteExisting && barcode.isNullOrBlank()) return false
            var changed = false
            if (overwriteExisting) {
                if (existing.name != item.name.trim()) {
                    existing.name = item.name.trim()
                    changed = true
                }
                if (existing.category != item.category) {
                    existing.category = item.category
                    changed = true
                }
                if (existing.unit != item.unit) {
                    existing.unit = item.unit
                    changed = true
                }
                if (item.unitPrice > 0.0 && existing.lastUnitPrice != item.unitPrice) {
                    existing.lastUnitPrice = item.unitPrice
                    changed = true
                }
            }
            if (!barcode.isNullOrBlank() && existing.barcode != barcode) {
                products.filter { it.id != existing.id && it.barcode == barcode }
                    .forEach { it.barcode = null }
                existing.barcode = barcode
                changed = true
            }
            if (changed) existing.updatedAt = now
            return changed
        }

        val nextId = (products.maxOfOrNull { it.id } ?: 0L) + 1L
        if (!barcode.isNullOrBlank()) {
            products.filter { it.barcode == barcode }.forEach { it.barcode = null }
        }
        products += CatalogProduct(
            id = nextId,
            name = item.name.trim(),
            normalizedName = normalized,
            category = item.category,
            unit = item.unit,
            lastUnitPrice = item.unitPrice.coerceAtLeast(0.0),
            barcode = barcode,
            updatedAt = now
        )
        return true
    }

    private fun findByName(products: List<CatalogProduct>, name: String): CatalogProduct? {
        val normalized = normalizeName(name)
        if (normalized.isBlank()) return null
        return products.firstOrNull { it.normalizedName == normalized }
    }

    private fun saveProducts(products: List<CatalogProduct>) {
        val deduplicated = deduplicate(products.toMutableList())
        val array = JSONArray().apply { deduplicated.forEach { put(it.toJson()) } }
        prefs.edit().putString(KEY_PRODUCTS, array.toString()).apply()
    }

    private fun deduplicate(products: MutableList<CatalogProduct>): MutableList<CatalogProduct> {
        val byName = linkedMapOf<String, CatalogProduct>()
        products.sortedBy { it.updatedAt }.forEach { product ->
            val normalized = normalizeName(product.name.ifBlank { product.normalizedName })
            if (normalized.isBlank()) return@forEach
            product.normalizedName = normalized
            byName[normalized] = product
        }

        val claimedBarcodes = mutableSetOf<String>()
        byName.values.sortedByDescending { it.updatedAt }.forEach { product ->
            val barcode = sanitizeBarcode(product.barcode)
            product.barcode = if (barcode != null && claimedBarcodes.add(barcode)) barcode else null
        }
        return byName.values.sortedBy { it.name.lowercase(Locale("pt", "BR")) }.toMutableList()
    }

    private fun CatalogProduct.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("normalizedName", normalizedName)
        put("category", category)
        put("unit", unit)
        put("lastUnitPrice", lastUnitPrice)
        put("barcode", barcode ?: JSONObject.NULL)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toProduct(): CatalogProduct {
        val name = optString("name", "").trim()
        val rawBarcode = if (isNull("barcode")) null else optString("barcode")
        return CatalogProduct(
            id = optLong("id", System.currentTimeMillis()),
            name = name,
            normalizedName = normalizeName(optString("normalizedName", name)),
            category = optString("category", "Outros"),
            unit = optString("unit", "un"),
            lastUnitPrice = optDouble("lastUnitPrice", 0.0).coerceAtLeast(0.0),
            barcode = sanitizeBarcode(rawBarcode),
            updatedAt = optLong("updatedAt", System.currentTimeMillis())
        )
    }

    companion object {
        private const val PREFS_NAME = "lista_mercado_catalog"
        private const val KEY_PRODUCTS = "catalog_products_v1"
        private const val KEY_INITIAL_SEED_DONE = "catalog_seed_from_lists_v1"

        fun normalizeName(value: String): String {
            val withoutAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
            return withoutAccents
                .lowercase(Locale.ROOT)
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun sanitizeBarcode(value: String?): String? = value
            ?.trim()
            ?.replace(Regex("\\s+"), "")
            ?.takeIf { it.isNotBlank() }
    }
}
