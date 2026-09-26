package com.listamercado.app.data

import com.listamercado.app.BuildConfig
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.ListTemplate
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.util.Locale

class BackupRepository(
    private val shoppingRepository: ShoppingRepository,
    private val catalogRepository: ProductCatalogRepository,
    private val templateRepository: TemplateRepository,
    private val marketRepository: MarketPreferencesRepository
) {
    data class BackupSummary(
        val listCount: Int,
        val productCount: Int,
        val priceRecordCount: Int,
        val itemCount: Int,
        val templateCount: Int,
        val favoriteMarketCount: Int
    )

    data class BackupSnapshot(
        val schemaVersion: Int,
        val lists: MutableList<ShoppingList>,
        val products: MutableList<CatalogProduct>,
        val templates: MutableList<ListTemplate>,
        val favoriteMarketKeys: Set<String>,
        val marketRadiusMeters: Int,
        val summary: BackupSummary
    )

    fun createJsonBackup(): String {
        val lists = shoppingRepository.loadLists()
        catalogRepository.seedFromLists(lists)
        val products = catalogRepository.loadProducts()
        val history = buildPriceHistory(lists)
        val templates = templateRepository.loadUserTemplates()
        val favoriteMarketKeys = marketRepository.favoriteKeys()

        return JSONObject().apply {
            put("format", BACKUP_FORMAT)
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", Instant.now().toString())
            put("appVersion", "${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}")
            put("lists", JSONArray().apply { lists.forEach { put(it.toBackupJson()) } })
            put("catalog", JSONArray().apply { products.forEach { put(it.toBackupJson()) } })
            put("templates", JSONArray().apply { templates.forEach { put(it.toBackupJson()) } })
            put("marketPreferences", JSONObject().apply {
                put("radiusMeters", marketRepository.selectedRadiusMeters())
                put("favoriteKeys", JSONArray().apply { favoriteMarketKeys.sorted().forEach { put(it) } })
            })
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
            "tipo", "lista", "produto", "quantidade_planejada", "quantidade_comprada", "unidade", "preco_unitario",
            "categoria", "comprado", "orcamento_lista", "codigo_barras", "favorito", "recorrencia",
            "preco_alvo", "unidade_preco_alvo", "atualizado_em"
        ).joinToString(",", transform = ::csv)

        lists.forEach { list ->
            if (list.items.isEmpty()) {
                lines += listOf(
                    "LISTA", list.name, "", "", "", "", "", "", "", number(list.budget), "", "", "", "", "", instant(list.updatedAt)
                ).joinToString(",", transform = ::csv)
            } else {
                list.items.forEach { item ->
                    lines += listOf(
                        "ITEM",
                        list.name,
                        item.name,
                        number(item.quantity),
                        number(item.purchasedQuantity),
                        item.unit,
                        number(item.unitPrice),
                        item.category,
                        item.purchased.toString(),
                        number(list.budget),
                        catalogByName[ProductCatalogRepository.normalizeName(item.name)]?.barcode.orEmpty(),
                        "",
                        "",
                        catalogByName[ProductCatalogRepository.normalizeName(item.name)]?.priceTarget?.let(::number).orEmpty(),
                        catalogByName[ProductCatalogRepository.normalizeName(item.name)]?.priceTargetUnit.orEmpty(),
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
                "",
                product.unit,
                number(product.lastUnitPrice),
                product.category,
                "",
                "",
                product.barcode.orEmpty(),
                product.favorite.toString(),
                product.recurringFrequency.orEmpty(),
                product.priceTarget?.let(::number).orEmpty(),
                product.priceTargetUnit.orEmpty(),
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
        val templateArray = root.optJSONArray("templates") ?: JSONArray()
        val templates = MutableList(templateArray.length()) { index ->
            parseTemplate(templateArray.getJSONObject(index), index)
        }
        val marketPrefs = root.optJSONObject("marketPreferences")
        val favoriteMarketKeys = mutableSetOf<String>()
        marketPrefs?.optJSONArray("favoriteKeys")?.let { array ->
            for (i in 0 until array.length()) array.optString(i).trim().takeIf { it.isNotBlank() }?.let(favoriteMarketKeys::add)
        }
        val marketRadiusMeters = marketPrefs?.optInt("radiusMeters", MarketPreferencesRepository.DEFAULT_RADIUS_METERS)
            ?.takeIf { it in MarketPreferencesRepository.ALLOWED_RADIUS_METERS }
            ?: MarketPreferencesRepository.DEFAULT_RADIUS_METERS

        val duplicateListIds = lists.groupingBy { it.id }.eachCount().filterValues { it > 1 }
        if (duplicateListIds.isNotEmpty()) throw IllegalArgumentException("O backup contém listas duplicadas pelo identificador.")
        val duplicateProductIds = products.groupingBy { it.id }.eachCount().filterValues { it > 1 }
        if (duplicateProductIds.isNotEmpty()) throw IllegalArgumentException("O backup contém produtos duplicados pelo identificador.")
        val duplicateTemplateIds = templates.groupingBy { it.id }.eachCount().filterValues { it > 1 }
        if (duplicateTemplateIds.isNotEmpty()) throw IllegalArgumentException("O backup contém modelos duplicados pelo identificador.")

        val summary = BackupSummary(
            listCount = lists.size,
            productCount = products.size,
            priceRecordCount = lists.sumOf { list ->
                list.items.count { it.purchased && it.purchasedQuantity > 0.0 && it.unitPrice > 0.0 }
            },
            itemCount = lists.sumOf { it.items.size },
            templateCount = templates.size,
            favoriteMarketCount = favoriteMarketKeys.size
        )
        return BackupSnapshot(schema, lists, products, templates, favoriteMarketKeys, marketRadiusMeters, summary)
    }

    fun replaceWith(snapshot: BackupSnapshot) {
        shoppingRepository.saveLists(snapshot.lists)
        val products = prepareImportedProducts(snapshot.products, snapshot.schemaVersion)
        catalogRepository.replaceProducts(products)
        if (snapshot.schemaVersion >= 2) {
            templateRepository.replaceUserTemplates(snapshot.templates)
        }
        if (snapshot.schemaVersion >= 3) {
            marketRepository.replaceFavorites(snapshot.favoriteMarketKeys)
            marketRepository.setSelectedRadiusMeters(snapshot.marketRadiusMeters)
        }
    }

    fun mergeWith(snapshot: BackupSnapshot) {
        val mergedLists = mergeLists(
            shoppingRepository.loadLists(),
            snapshot.lists,
            preserveMarketAssociation = snapshot.schemaVersion < 3
        )
        shoppingRepository.saveLists(mergedLists)
        val products = prepareImportedProducts(snapshot.products, snapshot.schemaVersion)
        catalogRepository.mergeProducts(products)
        if (snapshot.schemaVersion >= 2) {
            templateRepository.mergeUserTemplates(snapshot.templates)
        }
        if (snapshot.schemaVersion >= 3) {
            marketRepository.mergeFavorites(snapshot.favoriteMarketKeys)
        }
        catalogRepository.seedFromLists(mergedLists)
    }

    private fun prepareImportedProducts(
        imported: MutableList<CatalogProduct>,
        schemaVersion: Int
    ): MutableList<CatalogProduct> {
        if (schemaVersion >= SCHEMA_VERSION) return imported.map { it.copy() }.toMutableList()
        val current = catalogRepository.loadProducts().associateBy { ProductCatalogRepository.normalizeName(it.name) }
        return imported.map { incoming ->
            val existing = current[ProductCatalogRepository.normalizeName(incoming.name)]
            if (existing == null) {
                incoming.copy()
            } else {
                incoming.copy(
                    favorite = if (schemaVersion < 2) existing.favorite else incoming.favorite,
                    recurringFrequency = if (schemaVersion < 2) existing.recurringFrequency else incoming.recurringFrequency,
                    lastQuantity = if (schemaVersion < 2) existing.lastQuantity else incoming.lastQuantity,
                    lastRecurringAddedAt = if (schemaVersion < 2) existing.lastRecurringAddedAt else incoming.lastRecurringAddedAt,
                    priceTarget = if (schemaVersion < 4) existing.priceTarget else incoming.priceTarget,
                    priceTargetUnit = if (schemaVersion < 4) existing.priceTargetUnit else incoming.priceTargetUnit
                )
            }
        }.toMutableList()
    }

    private fun mergeLists(
        current: MutableList<ShoppingList>,
        imported: MutableList<ShoppingList>,
        preserveMarketAssociation: Boolean
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
                    marketKey = if (preserveMarketAssociation) existing.marketKey else if (incomingIsNewer) incoming.marketKey else existing.marketKey,
                    marketName = if (preserveMarketAssociation) existing.marketName else if (incomingIsNewer) incoming.marketName else existing.marketName,
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
            marketKey = if (json.isNull("marketKey")) null else json.optString("marketKey").takeIf { it.isNotBlank() },
            marketName = if (json.isNull("marketName")) null else json.optString("marketName").takeIf { it.isNotBlank() },
            items = items
        )
    }

    private fun parseItem(json: JSONObject, listName: String, index: Int): ShoppingItem {
        val id = json.optLong("id", 0L)
        val name = json.optString("name", "").trim()
        val quantity = json.optDouble("quantity", Double.NaN)
        val purchased = json.optBoolean("purchased", false)
        val purchasedQuantity = if (json.has("purchasedQuantity")) {
            json.optDouble("purchasedQuantity", Double.NaN)
        } else if (purchased) {
            // Backups dos schemas 1-4 não separavam planejado/comprado.
            quantity
        } else {
            0.0
        }
        val price = json.optDouble("unitPrice", 0.0)
        if (id <= 0L) throw IllegalArgumentException("Lista '$listName', item ${index + 1}: identificador inválido.")
        if (name.isBlank()) throw IllegalArgumentException("Lista '$listName', item ${index + 1}: nome vazio.")
        if (!quantity.isFinite() || quantity <= 0.0) throw IllegalArgumentException("Item '$name': quantidade planejada inválida.")
        if (!purchasedQuantity.isFinite() || purchasedQuantity < 0.0) throw IllegalArgumentException("Item '$name': quantidade comprada inválida.")
        if (purchased && purchasedQuantity <= 0.0) throw IllegalArgumentException("Item '$name': item comprado sem quantidade comprada.")
        if (!price.isFinite() || price < 0.0) throw IllegalArgumentException("Item '$name': preço inválido.")
        return ShoppingItem(
            id = id,
            name = name,
            quantity = quantity,
            purchasedQuantity = purchasedQuantity,
            unit = json.optString("unit", "un").ifBlank { "un" },
            unitPrice = price,
            category = json.optString("category", "Outros").ifBlank { "Outros" },
            note = json.optString("note", ""),
            purchased = purchased
        )
    }

    private fun parseProduct(json: JSONObject, index: Int): CatalogProduct {
        val id = json.optLong("id", 0L)
        val name = json.optString("name", "").trim()
        val lastPrice = json.optDouble("lastUnitPrice", 0.0)
        val priceTarget = if (json.isNull("priceTarget")) null else json.optDouble("priceTarget", Double.NaN)
        if (id <= 0L) throw IllegalArgumentException("Produto ${index + 1}: identificador inválido.")
        if (name.isBlank()) throw IllegalArgumentException("Produto ${index + 1}: nome vazio.")
        if (!lastPrice.isFinite() || lastPrice < 0.0) throw IllegalArgumentException("Produto '$name': último preço inválido.")
        if (priceTarget != null && (!priceTarget.isFinite() || priceTarget <= 0.0)) throw IllegalArgumentException("Produto '$name': preço-alvo inválido.")
        return CatalogProduct(
            id = id,
            name = name,
            normalizedName = ProductCatalogRepository.normalizeName(name),
            category = json.optString("category", "Outros").ifBlank { "Outros" },
            unit = json.optString("unit", "un").ifBlank { "un" },
            lastUnitPrice = lastPrice,
            priceTarget = priceTarget,
            priceTargetUnit = if (json.isNull("priceTargetUnit")) null else json.optString("priceTargetUnit").takeIf { it.isNotBlank() },
            barcode = ProductCatalogRepository.sanitizeBarcode(
                if (json.isNull("barcode")) null else json.optString("barcode")
            ),
            favorite = json.optBoolean("favorite", false),
            recurringFrequency = if (json.isNull("recurringFrequency")) null else json.optString("recurringFrequency").takeIf { it.isNotBlank() },
            lastQuantity = json.optDouble("lastQuantity", 1.0).coerceAtLeast(0.01),
            lastRecurringAddedAt = json.optLong("lastRecurringAddedAt", 0L).coerceAtLeast(0L),
            updatedAt = json.optLong("updatedAt", id).coerceAtLeast(1L)
        )
    }

    private fun ShoppingList.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("budget", budget)
        put("marketKey", marketKey ?: JSONObject.NULL)
        put("marketName", marketName ?: JSONObject.NULL)
        put("items", JSONArray().apply { items.forEach { put(it.toBackupJson()) } })
    }

    private fun ShoppingItem.toBackupJson() = JSONObject().apply {
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

    private fun CatalogProduct.toBackupJson() = JSONObject().apply {
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

    private fun ListTemplate.toBackupJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("budget", budget)
        put("updatedAt", updatedAt)
        put("items", JSONArray().apply { items.forEach { put(it.toBackupJson()) } })
    }

    private fun parseTemplate(json: JSONObject, index: Int): ListTemplate {
        val id = json.optLong("id", 0L)
        val name = json.optString("name", "").trim()
        if (id == 0L) throw IllegalArgumentException("Modelo ${index + 1}: identificador inválido.")
        if (name.isBlank()) throw IllegalArgumentException("Modelo ${index + 1}: nome vazio.")
        val itemArray = json.optJSONArray("items") ?: JSONArray()
        val items = MutableList(itemArray.length()) { itemIndex ->
            parseItem(itemArray.getJSONObject(itemIndex), "modelo $name", itemIndex).apply {
                purchased = false
                purchasedQuantity = 0.0
            }
        }
        return ListTemplate(
            id = id,
            name = name,
            builtIn = false,
            budget = json.optDouble("budget", 0.0).validMoney("Modelo '$name': orçamento inválido."),
            items = items,
            updatedAt = json.optLong("updatedAt", kotlin.math.abs(id)).coerceAtLeast(1L)
        )
    }

    private fun buildPriceHistory(lists: List<ShoppingList>) = JSONArray().apply {
        lists.sortedBy { it.createdAt }.forEach { list ->
            list.items.filter { it.purchased && it.purchasedQuantity > 0.0 && it.unitPrice > 0.0 }.forEach { item ->
                put(JSONObject().apply {
                    put("listId", list.id)
                    put("listName", list.name)
                    put("itemId", item.id)
                    put("productName", item.name)
                    put("plannedQuantity", item.quantity)
                    put("purchasedQuantity", item.purchasedQuantity)
                    put("unitPrice", item.unitPrice)
                    put("paidTotal", item.purchasedSubtotal)
                    put("unit", item.unit)
                    put("recordedAt", list.updatedAt)
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
        const val SCHEMA_VERSION = 5
        private const val BACKUP_FORMAT = "meu-supermercado-backup"
    }
}
