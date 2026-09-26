package com.listamercado.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.net.NetworkCapabilities
import com.listamercado.app.model.BarcodeLookupResult
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class BarcodeLookupRepository(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    fun lookup(barcode: String, callback: (Result<BarcodeLookupResult?>) -> Unit) {
        val sanitized = ProductCatalogRepository.sanitizeBarcode(barcode)
        if (sanitized == null) {
            callback(Result.success(null))
            return
        }
        if (!isNetworkAvailable()) {
            callback(Result.success(null))
            return
        }
        Thread {
            val result = runCatching {
                queryOpenFoodFacts(sanitized)
            }
            mainHandler.post { callback(result) }
        }.start()
    }

    private fun queryOpenFoodFacts(barcode: String): BarcodeLookupResult? {
        val fields = listOf(
            "product_name",
            "product_name_pt",
            "generic_name",
            "generic_name_pt",
            "brands",
            "quantity",
            "categories",
            "categories_tags"
        ).joinToString(",")
        val encodedFields = URLEncoder.encode(fields, StandardCharsets.UTF_8.name())
        val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=$encodedFields")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 5000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.7")
            setRequestProperty("User-Agent", "MeuSupermercado/1.0 (Android)")
            instanceFollowRedirects = true
        }
        return try {
            val status = connection.responseCode
            if (status !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            if (root.optInt("status", 0) != 1) return null
            val product = root.optJSONObject("product") ?: return null
            val brand = product.optString("brands").trim().takeIf { it.isNotBlank() }
            val quantity = product.optString("quantity").trim().takeIf { it.isNotBlank() }
            val rawName = sequenceOf(
                product.optString("product_name_pt"),
                product.optString("product_name"),
                product.optString("generic_name_pt"),
                product.optString("generic_name")
            ).map { it.trim() }.firstOrNull { it.isNotBlank() }
                ?: listOfNotNull(brand, quantity).joinToString(" ").trim().takeIf { it.isNotBlank() }
                ?: return null
            val mappedCategory = mapCategory(
                product.optJSONArray("categories_tags")?.let { array ->
                    List(array.length()) { index -> array.optString(index) }
                }.orEmpty(),
                product.optString("categories")
            )
            BarcodeLookupResult(
                barcode = barcode,
                name = rawName,
                brand = brand,
                quantityDescription = quantity,
                category = mappedCategory,
                source = "Open Food Facts"
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun mapCategory(tags: List<String>, categories: String?): String? {
        val values = buildList {
            addAll(tags.map { it.lowercase() })
            if (!categories.isNullOrBlank()) add(categories.lowercase())
        }
        fun containsAny(vararg needles: String): Boolean =
            values.any { value -> needles.any { needle -> value.contains(needle) } }

        return when {
            containsAny("beverage", "drink", "juice", "water", "soda", "coffee", "tea") -> "Bebidas"
            containsAny("dairy", "milk", "yogurt", "cheese", "butter") -> "Laticínios"
            containsAny("bread", "bakery", "cake", "biscuit", "cookie") -> "Padaria"
            containsAny("meat", "beef", "pork", "chicken", "sausage", "ham") -> "Açougue"
            containsAny("frozen", "ice-cream") -> "Congelados"
            containsAny("fruit", "vegetable", "hortifruti", "legume") -> "Hortifruti"
            containsAny("clean", "detergent", "soap", "bleach") -> "Limpeza"
            containsAny("hygiene", "shampoo", "toothpaste", "deodorant", "personal-care") -> "Higiene"
            containsAny("pet") -> "Pet"
            containsAny("grocery", "cereal", "pasta", "rice", "bean", "sauce", "snack", "seasoning") -> "Mercearia"
            else -> null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
