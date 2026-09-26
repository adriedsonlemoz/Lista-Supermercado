package com.listamercado.app.data

import android.content.Context
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import com.listamercado.app.util.PriceUnitHelper
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

        val quantityMigration = !prefs.getBoolean(KEY_LAST_QUANTITY_MIGRATED_V1, false)
        if (quantityMigration) {
            lists.sortedBy { it.updatedAt }.forEach { list ->
                list.items.forEach { item ->
                    val existing = findByName(products, item.name)
                    if (existing != null && item.quantity > 0.0 && existing.lastQuantity != item.quantity) {
                        existing.lastQuantity = item.quantity
                        existing.updatedAt = maxOf(existing.updatedAt, list.updatedAt)
                        changed = true
                    }
                }
            }
        }

        if (changed || firstSeed || quantityMigration) saveProducts(products)
        val editor = prefs.edit()
        if (firstSeed) editor.putBoolean(KEY_INITIAL_SEED_DONE, true)
        if (quantityMigration) editor.putBoolean(KEY_LAST_QUANTITY_MIGRATED_V1, true)
        editor.apply()
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
            .sortedWith(compareByDescending<CatalogProduct> { it.favorite }.thenBy { it.name.lowercase(Locale("pt", "BR")) })
            .toList()
    }

    fun setFavorite(productId: Long, favorite: Boolean) {
        val products = loadProducts()
        val product = products.firstOrNull { it.id == productId } ?: return
        product.favorite = favorite
        product.updatedAt = System.currentTimeMillis()
        saveProducts(products)
    }

    fun setRecurringFrequency(productId: Long, frequency: String?) {
        val products = loadProducts()
        val product = products.firstOrNull { it.id == productId } ?: return
        product.recurringFrequency = frequency
        product.updatedAt = System.currentTimeMillis()
        saveProducts(products)
    }

    fun setPriceTarget(productId: Long, displayPrice: Double?, unit: String) {
        val products = loadProducts()
        val product = products.firstOrNull { it.id == productId } ?: return
        applyPriceTarget(product, displayPrice, unit)
        saveProducts(products)
    }

    fun setPriceTargetByName(name: String, displayPrice: Double?, unit: String) {
        val products = loadProducts()
        val product = findByName(products, name) ?: return
        applyPriceTarget(product, displayPrice, unit)
        saveProducts(products)
    }

    private fun applyPriceTarget(product: CatalogProduct, displayPrice: Double?, unit: String) {
        val target = displayPrice?.takeIf { it.isFinite() && it > 0.0 }
        product.priceTarget = target
        product.priceTargetUnit = target?.let { PriceUnitHelper.priceUnit(unit) }
        product.updatedAt = System.currentTimeMillis()
    }

    fun recurringProducts(): List<CatalogProduct> = loadProducts()
        .filter { !it.recurringFrequency.isNullOrBlank() }
        .sortedWith(compareByDescending<CatalogProduct> { it.favorite }.thenBy { it.name.lowercase(Locale("pt", "BR")) })

    fun markRecurringAdded(productIds: Collection<Long>) {
        if (productIds.isEmpty()) return
        val products = loadProducts()
        val now = System.currentTimeMillis()
        var changed = false
        products.filter { it.id in productIds }.forEach {
            it.lastRecurringAddedAt = now
            it.updatedAt = now
            changed = true
        }
        if (changed) saveProducts(products)
    }

    fun replaceProducts(products: List<CatalogProduct>) {
        saveProducts(products.map { it.copy() })
    }

    fun mergeProducts(imported: List<CatalogProduct>) {
        val merged = loadProducts()
        imported.forEach { incoming ->
            val normalized = normalizeName(incoming.name)
            if (normalized.isBlank()) return@forEach
            val existing = merged.firstOrNull { it.normalizedName == normalized }
            if (existing == null) {
                merged += incoming.copy(normalizedName = normalized)
            } else if (incoming.updatedAt >= existing.updatedAt) {
                existing.name = incoming.name.trim()
                existing.normalizedName = normalized
                existing.category = incoming.category
                existing.unit = incoming.unit
                existing.lastUnitPrice = incoming.lastUnitPrice.coerceAtLeast(0.0)
                existing.priceTarget = incoming.priceTarget?.takeIf { it.isFinite() && it > 0.0 }
                existing.priceTargetUnit = existing.priceTarget?.let { incoming.priceTargetUnit?.takeIf { unit -> unit.isNotBlank() } }
                existing.barcode = sanitizeBarcode(incoming.barcode) ?: existing.barcode
                existing.favorite = incoming.favorite
                existing.recurringFrequency = incoming.recurringFrequency
                existing.lastQuantity = incoming.lastQuantity.coerceAtLeast(0.01)
                existing.lastRecurringAddedAt = incoming.lastRecurringAddedAt.coerceAtLeast(0L)
                existing.updatedAt = incoming.updatedAt
            } else if (existing.barcode.isNullOrBlank()) {
                existing.barcode = sanitizeBarcode(incoming.barcode)
            }
        }
        saveProducts(merged)
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
                if (item.quantity > 0.0 && existing.lastQuantity != item.quantity) {
                    existing.lastQuantity = item.quantity
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
            lastQuantity = item.quantity.coerceAtLeast(0.01),
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
        put("priceTarget", priceTarget ?: JSONObject.NULL)
        put("priceTargetUnit", priceTargetUnit ?: JSONObject.NULL)
        put("barcode", barcode ?: JSONObject.NULL)
        put("favorite", favorite)
        put("recurringFrequency", recurringFrequency ?: JSONObject.NULL)
        put("lastQuantity", lastQuantity)
        put("lastRecurringAddedAt", lastRecurringAddedAt)
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
            priceTarget = if (isNull("priceTarget")) null else optDouble("priceTarget").takeIf { it.isFinite() && it > 0.0 },
            priceTargetUnit = if (isNull("priceTargetUnit")) null else optString("priceTargetUnit").takeIf { it.isNotBlank() },
            barcode = sanitizeBarcode(rawBarcode),
            favorite = optBoolean("favorite", false),
            recurringFrequency = if (isNull("recurringFrequency")) null else optString("recurringFrequency").takeIf { it.isNotBlank() },
            lastQuantity = optDouble("lastQuantity", 1.0).coerceAtLeast(0.01),
            lastRecurringAddedAt = optLong("lastRecurringAddedAt", 0L).coerceAtLeast(0L),
            updatedAt = optLong("updatedAt", System.currentTimeMillis())
        )
    }

    companion object {
        private const val PREFS_NAME = "lista_mercado_catalog"
        private const val KEY_PRODUCTS = "catalog_products_v1"
        private const val KEY_INITIAL_SEED_DONE = "catalog_seed_from_lists_v1"
        private const val KEY_LAST_QUANTITY_MIGRATED_V1 = "catalog_last_quantity_migrated_v1"

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
