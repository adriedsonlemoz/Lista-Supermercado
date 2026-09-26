package com.listamercado.app.data

import android.content.Context
import android.location.Location
import com.listamercado.app.model.NearbyMarket
import org.json.JSONArray
import org.json.JSONObject

class MarketPreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class CacheSnapshot(
        val savedAt: Long,
        val centerLat: Double,
        val centerLon: Double,
        val radiusMeters: Int,
        val markets: List<NearbyMarket>
    )

    fun selectedRadiusMeters(): Int {
        val stored = prefs.getInt(KEY_SELECTED_RADIUS, DEFAULT_RADIUS_METERS)
        return if (stored in ALLOWED_RADIUS_METERS) stored else DEFAULT_RADIUS_METERS
    }

    fun setSelectedRadiusMeters(radiusMeters: Int) {
        if (radiusMeters in ALLOWED_RADIUS_METERS) {
            prefs.edit().putInt(KEY_SELECTED_RADIUS, radiusMeters).apply()
        }
    }

    fun favoriteKeys(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toSet()

    fun isFavorite(key: String): Boolean = favoriteKeys().contains(key)

    fun toggleFavorite(key: String): Boolean {
        val favorites = favoriteKeys().toMutableSet()
        val nowFavorite = if (favorites.contains(key)) {
            favorites.remove(key)
            false
        } else {
            favorites.add(key)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        return nowFavorite
    }

    fun replaceFavorites(keys: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, keys.filter { it.isNotBlank() }.toSet()).apply()
    }

    fun mergeFavorites(keys: Set<String>) {
        replaceFavorites(favoriteKeys() + keys.filter { it.isNotBlank() })
    }

    fun saveCache(location: Location, radiusMeters: Int, markets: List<NearbyMarket>) {
        if (markets.isEmpty()) return
        val payload = JSONObject().apply {
            put("savedAt", System.currentTimeMillis())
            put("centerLat", location.latitude)
            put("centerLon", location.longitude)
            put("radiusMeters", radiusMeters)
            put("markets", JSONArray().apply {
                markets.forEach { market ->
                    put(JSONObject().apply {
                        put("key", market.key)
                        put("name", market.name)
                        put("lat", market.lat)
                        put("lon", market.lon)
                        put("distanceMeters", market.distanceMeters)
                        put("address", market.address)
                        put("source", market.source)
                    })
                }
            })
        }
        prefs.edit().putString(KEY_CACHE, payload.toString()).apply()
    }

    fun loadCache(location: Location, minimumRadiusMeters: Int): CacheSnapshot? {
        val raw = prefs.getString(KEY_CACHE, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val savedAt = root.optLong("savedAt", 0L)
            val centerLat = root.optDouble("centerLat", Double.NaN)
            val centerLon = root.optDouble("centerLon", Double.NaN)
            val radius = root.optInt("radiusMeters", 0)
            if (savedAt <= 0L || centerLat.isNaN() || centerLon.isNaN() || radius < minimumRadiusMeters) return null
            if (System.currentTimeMillis() - savedAt > CACHE_MAX_AGE_MS) return null

            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, centerLat, centerLon, distance)
            if (distance[0] > CACHE_MAX_CENTER_DISTANCE_METERS) return null

            val array = root.optJSONArray("markets") ?: return null
            val markets = MutableList(array.length()) { index ->
                val item = array.getJSONObject(index)
                val lat = item.optDouble("lat", Double.NaN)
                val lon = item.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) error("Cache de mercado inválido")
                val currentDistance = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, lat, lon, currentDistance)
                NearbyMarket(
                    key = item.optString("key").ifBlank { fallbackKey(item.optString("name", "Mercado"), lat, lon) },
                    name = item.optString("name", "Mercado").ifBlank { "Mercado" },
                    lat = lat,
                    lon = lon,
                    distanceMeters = currentDistance[0].toDouble(),
                    address = item.optString("address", ""),
                    source = item.optString("source", "OpenStreetMap"),
                    fromCache = true
                )
            }
            CacheSnapshot(savedAt, centerLat, centerLon, radius, markets)
        }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "meu_supermercado_markets"
        private const val KEY_SELECTED_RADIUS = "selected_radius_meters_v1"
        private const val KEY_FAVORITES = "favorite_market_keys_v1"
        private const val KEY_CACHE = "nearby_market_cache_v1"
        private const val CACHE_MAX_AGE_MS = 6 * 60 * 60 * 1000L
        private const val CACHE_MAX_CENTER_DISTANCE_METERS = 3_000f
        const val DEFAULT_RADIUS_METERS = 5_000
        val ALLOWED_RADIUS_METERS = setOf(5_000, 10_000, 20_000, 30_000, 50_000)

        fun fallbackKey(name: String, lat: Double, lon: Double): String =
            "geo:${name.trim().lowercase()}:${"%.5f".format(java.util.Locale.ROOT, lat)}:${"%.5f".format(java.util.Locale.ROOT, lon)}"
    }
}
