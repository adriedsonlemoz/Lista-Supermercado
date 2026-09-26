package com.listamercado.app.data

import com.listamercado.app.BuildConfig
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.util.Locale

class BackupRepository(
    private val shoppingRepository: ShoppingRepository,
    private val catalogRepository: ProductCatalogRepository
) {
    data class BackupSummary(
        val listCount: Int,
        val productCount: Int,
        val priceRecordCount: Int,
        val itemCount: Int
    )

    data class BackupSnapshot(
        val schemaVersion: Int,
        val lists: MutableList<ShoppingList>,
        val products: MutableList<CatalogProduct>,
        val summary: BackupSummary
    )

    fun createJsonBackup(): String {
        val lists = shoppingRepository.loadLists()
        catalogRepository.seedFromLists(lists)
        val products = catalogRepository.loadProducts()
        val history = buildPriceHistory(lists)

        return JSONObject().apply {
            put("format", BACKUP_FORMAT)
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", Instant.now().toString())
            put("appVersion", "${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}")
            put("lists", JSONArray().apply { lists.forEach { put(it.toBackupJson()) } })
            put("catalog", JSONArray().apply { products.forEach { put(it.toBackupJson()) } })
            put("priceHistory", history)
        }.toString(2)
    }

    fun createCsvExport(): String {
        val lists = shoppingRepository.loadLists()
        catalogRepository.seedFromLists(lists)
        val products = catalogRepository.loadProducts()
        val catalogByName = products.associateBy { ProductCatalogRepository.normalizeName(it.name) }
        val lines = mutableListOf<String>()
        lines += listOf(
            "tipo", "lista", "produto", "quantidade", "unidade", "preco_unitario",
            "categoria", "comprado", "orcamento_lista", "codigo_barras", "atualizado_em"
        ).joinToString(",", transform = ::csv)

        lists.forEach { list ->
            if (list.items.isEmpty()) {
                lines += listOf(
                    "LISTA", list.name, "", "", "", "", "", "", number(list.budget), "", instant(list.updatedAt)
                ).joinToString(",", transform = ::csv)
            } else {
                list.items.forEach { item ->
                    lines += listOf(
                        "ITEM",
                        list.name,
                        item.name,
                        number(item.quantity),
                        item.unit,
                        number(item.unitPrice),
                        item.category,
                        item.purchased.toString(),
                        number(list.budget),
                        catalogByName[ProductCatalogRepository.normalizeName(item.name)]?.barcode.orEmpty(),
                        instant(list.updatedAt)
                    ).joinToString(",", transform = ::csv)
                }
            }
        }

        products.forEach { product ->
            lines += listOf(
                "CATALOGO",
                "",
                product.name,
                "",
                product.unit,
                number(product.lastUnitPrice),
                product.category,
                "",
                "",
                product.barcode.orEmpty(),
                instant(product.updatedAt)
            ).joinToString(",", transform = ::csv)
        }
        return "\uFEFF" + lines.joinToString("\r\n")
    }

    @Throws(IllegalArgumentException::class)
    fun parseAndValidate(raw: String): BackupSnapshot {
        if (raw.isBlank()) throw IllegalArgumentException("O arquivo está vazio.")
        val root = try {
            JSONObject(raw)
        } catch (_: JSONException) {
            throw IllegalArgumentException("O arquivo não contém um JSON válido.")
        }

        if (root.optString("format") != BACKUP_FORMAT) {
            throw IllegalArgumentException("Este arquivo não é um backup do Meu Supermercado.")
        }
        val schema = root.optInt("schemaVersion", -1)
        if (schema <= 0) throw IllegalArgumentException("O backup não informa uma versão de schema válida.")
        if (schema > SCHEMA_VERSION) {
            throw IllegalArgumentException("Este backup usa o schema $schema, mais novo que o suportado ($SCHEMA_VERSION).")
        }

        val listArray = root.optJSONArray("lists")
            ?: throw IllegalArgumentException("O backup não contém a coleção de listas.")
        val catalogArray = root.optJSONArray("catalog")
            ?: throw IllegalArgumentException("O backup não contém o catálogo de produtos.")

        val lists = MutableList(listArray.length()) { index ->
            parseList(listArray.getJSONObject(index), index)
        }
        val products = MutableList(catalogArray.length()) { index ->
            parseProduct(catalogArray.getJSONObject(index), index)
        }

        val duplicateListIds = lists.groupingBy { it.id }.eachCount().filterValues { it > 1 }
        if (duplicateListIds.isNotEmpty()) throw IllegalArgumentException("O backup contém listas duplicadas pelo identificador.")
        val duplicateProductIds = products.groupingBy { it.id }.eachCount().filterValues { it > 1 }
        if (duplicateProductIds.isNotEmpty()) throw IllegalArgumentException("O backup contém produtos duplicados pelo identificador.")

        val summary = BackupSummary(
            listCount = lists.size,
            productCount = products.size,
            priceRecordCount = lists.sumOf { list -> list.items.count { it.unitPrice > 0.0 } },
            itemCount = lists.sumOf { it.items.size }
        )
        return BackupSnapshot(schema, lists, products, summary)
    }

    fun replaceWith(snapshot: BackupSnapshot) {
        shoppingRepository.saveLists(snapshot.lists)
        catalogRepository.replaceProducts(snapshot.products)
    }

    fun mergeWith(snapshot: BackupSnapshot) {
        val mergedLists = mergeLists(shoppingRepository.loadLists(), snapshot.lists)
        shoppingRepository.saveLists(mergedLists)
        catalogRepository.mergeProducts(snapshot.products)
        catalogRepository.seedFromLists(mergedLists)
    }

    private fun mergeLists(
        current: MutableList<ShoppingList>,
        imported: MutableList<ShoppingList>
    ): MutableList<ShoppingList> {
        val byId = current.associateBy { it.id }.toMutableMap()
        imported.forEach { incoming ->
            val existing = byId[incoming.id]
            if (existing == null) {
                byId[incoming.id] = incoming.copy(items = incoming.items.map { it.copy() }.toMutableList())
            } else {
                val incomingIsNewer = incoming.updatedAt >= existing.updatedAt
                val items = existing.items.associateBy { it.id }.toMutableMap()
                incoming.items.forEach { importedItem ->
                    val currentItem = items[importedItem.id]
                    if (currentItem == null || incomingIsNewer) items[importedItem.id] = importedItem.copy()
                }
                byId[incoming.id] = ShoppingList(
                    id = existing.id,
                    name = if (incomingIsNewer) incoming.name else existing.name,
                    createdAt = minOf(existing.createdAt, incoming.createdAt),
                    updatedAt = maxOf(existing.updatedAt, incoming.updatedAt),
                    budget = if (incomingIsNewer) incoming.budget else existing.budget,
                    items = items.values.toMutableList()
                )
            }
        }
        return byId.values.sortedByDescending { it.updatedAt }.toMutableList()
    }

    private fun parseList(json: JSONObject, index: Int): ShoppingList {
        val id = json.optLong("id", 0L)
        val name = json.optString("name", "").trim()
        if (id <= 0L) throw IllegalArgumentException("Lista ${index + 1}: identificador inválido.")
        if (name.isBlank()) throw IllegalArgumentException("Lista ${index + 1}: nome vazio.")
        val itemArray = json.optJSONArray("items")
            ?: throw IllegalArgumentException("Lista '$name': itens ausentes.")
        val items = MutableList(itemArray.length()) { itemIndex ->
            parseItem(itemArray.getJSONObject(itemIndex), name, itemIndex)
        }
        if (items.groupingBy { it.id }.eachCount().any { it.value > 1 }) {
            throw IllegalArgumentException("Lista '$name': há itens duplicados pelo identificador.")
        }
        return ShoppingList(
            id = id,
            name = name,
            createdAt = json.optLong("createdAt", id).coerceAtLeast(1L),
            updatedAt = json.optLong("updatedAt", id).coerceAtLeast(1L),
            budget = json.optDouble("budget", 0.0).validMoney("Lista '$name': orçamento inválido."),
            items = items
        )
    }

    private fun parseItem(json: JSONObject, listName: String, index: Int): ShoppingItem {
        val id = json.optLong("id", 0L)
        val name = json.optString("name", "").trim()
        val quantity = json.optDouble("quantity", Double.NaN)
        val price = json.optDouble("unitPrice", 0.0)
        if (id <= 0L) throw IllegalArgumentException("Lista '$listName', item ${index + 1}: identificador inválido.")
        if (name.isBlank()) throw IllegalArgumentException("Lista '$listName', item ${index + 1}: nome vazio.")
        if (!quantity.isFinite() || quantity <= 0.0) throw IllegalArgumentException("Item '$name': quantidade inválida.")
        if (!price.isFinite() || price < 0.0) throw IllegalArgumentException("Item '$name': preço inválido.")
        return ShoppingItem(
            id = id,
            name = name,
            quantity = quantity,
            unit = json.optString("unit", "un").ifBlank { "un" },
            unitPrice = price,
            category = json.optString("category", "Outros").ifBlank { "Outros" },
            note = json.optString("note", ""),
            purchased = json.optBoolean("purchased", false)
        )
    }

    private fun parseProduct(json: JSONObject, index: Int): CatalogProduct {
        val id = json.optLong("id", 0L)
        val name = json.optString("name", "").trim()
        val lastPrice = json.optDouble("lastUnitPrice", 0.0)
        if (id <= 0L) throw IllegalArgumentException("Produto ${index + 1}: identificador inválido.")
        if (name.isBlank()) throw IllegalArgumentException("Produto ${index + 1}: nome vazio.")
        if (!lastPrice.isFinite() || lastPrice < 0.0) throw IllegalArgumentException("Produto '$name': último preço inválido.")
        return CatalogProduct(
            id = id,
            name = name,
            normalizedName = ProductCatalogRepository.normalizeName(name),
            category = json.optString("category", "Outros").ifBlank { "Outros" },
            unit = json.optString("unit", "un").ifBlank { "un" },
            lastUnitPrice = lastPrice,
            barcode = ProductCatalogRepository.sanitizeBarcode(
                if (json.isNull("barcode")) null else json.optString("barcode")
            ),
            updatedAt = json.optLong("updatedAt", id).coerceAtLeast(1L)
        )
    }

    private fun ShoppingList.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("budget", budget)
        put("items", JSONArray().apply { items.forEach { put(it.toBackupJson()) } })
    }

    private fun ShoppingItem.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("quantity", quantity)
        put("unit", unit)
        put("unitPrice", unitPrice)
        put("category", category)
        put("note", note)
        put("purchased", purchased)
    }

    private fun CatalogProduct.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("normalizedName", normalizedName)
        put("category", category)
        put("unit", unit)
        put("lastUnitPrice", lastUnitPrice)
        put("barcode", barcode ?: JSONObject.NULL)
        put("updatedAt", updatedAt)
    }

    private fun buildPriceHistory(lists: List<ShoppingList>) = JSONArray().apply {
        lists.sortedBy { it.createdAt }.forEach { list ->
            list.items.filter { it.unitPrice > 0.0 }.forEach { item ->
                put(JSONObject().apply {
                    put("listId", list.id)
                    put("listName", list.name)
                    put("itemId", item.id)
                    put("productName", item.name)
                    put("unitPrice", item.unitPrice)
                    put("unit", item.unit)
                    put("recordedAt", list.createdAt)
                })
            }
        }
    }

    private fun Double.validMoney(message: String): Double {
        if (!isFinite() || this < 0.0) throw IllegalArgumentException(message)
        return this
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
    private fun number(value: Double): String = String.format(Locale.ROOT, "%.6f", value).trimEnd('0').trimEnd('.')
    private fun instant(epochMillis: Long): String = runCatching { Instant.ofEpochMilli(epochMillis).toString() }.getOrDefault("")

    companion object {
        const val SCHEMA_VERSION = 1
        private const val BACKUP_FORMAT = "meu-supermercado-backup"
    }
}
