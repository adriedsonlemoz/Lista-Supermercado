package com.listamercado.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.listamercado.app.R
import com.listamercado.app.data.MarketPreferencesRepository
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.model.NearbyMarket
import com.listamercado.app.model.ShoppingList
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class NearbyMarketsActivity : AppCompatActivity(), LocationListener {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var radiusGroup: ChipGroup
    private lateinit var adapter: NearbyMarketAdapter
    private lateinit var locationManager: LocationManager
    private lateinit var marketPreferences: MarketPreferencesRepository
    private lateinit var shoppingRepository: ShoppingRepository

    private var mapReady = false
    private var pendingLocation: Location? = null
    private var selectedRadiusMeters = MarketPreferencesRepository.DEFAULT_RADIUS_METERS
    private var baseMarkets: List<NearbyMarket> = emptyList()
    private var displayedMarkets: List<NearbyMarket> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_nearby_markets)

        status = findViewById(R.id.textMarketStatus)
        progress = findViewById(R.id.progressMarkets)
        radiusGroup = findViewById(R.id.groupMarketRadius)
        webView = findViewById(R.id.webMarkets)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        marketPreferences = MarketPreferencesRepository(this)
        shoppingRepository = ShoppingRepository(this)
        selectedRadiusMeters = marketPreferences.selectedRadiusMeters()

        adapter = NearbyMarketAdapter(
            onFavorite = ::toggleFavorite,
            onListAction = ::openOrCreateMarketList
        )
        val recyclerMarkets = findViewById<RecyclerView>(R.id.recyclerMarkets).apply {
            layoutManager = LinearLayoutManager(this@NearbyMarketsActivity)
            adapter = this@NearbyMarketsActivity.adapter
        }
        InsetsHelper.applyScaffold(
            activity = this,
            root = findViewById(R.id.rootMarkets),
            scrollable = recyclerMarkets
        )

        findViewById<ImageButton>(R.id.buttonBackMarkets).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.buttonRefreshMarkets).setOnClickListener { requestLocation() }
        setupRadiusSelector()
        configureMap()
        requestLocation()
    }

    override fun onResume() {
        super.onResume()
        if (baseMarkets.isNotEmpty()) renderMarkets(baseMarkets)
    }

    private fun setupRadiusSelector() {
        radiusGroup.check(chipIdForRadius(selectedRadiusMeters))
        radiusGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val radius = radiusForChipId(checkedId) ?: return@setOnCheckedStateChangeListener
            if (radius == selectedRadiusMeters) return@setOnCheckedStateChangeListener
            selectedRadiusMeters = radius
            marketPreferences.setSelectedRadiusMeters(radius)
            pendingLocation?.let { loadMarkets(it) }
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureMap() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = webView.settings.userAgentString +
            " MeuSupermercado/${com.listamercado.app.BuildConfig.VERSION_NAME}"
        webView.addJavascriptInterface(MapBridge(), "AndroidMarket")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                mapReady = true
                pendingLocation?.let(::renderUserLocation)
                renderMap(displayedMarkets)
            }
        }
        webView.loadUrl("file:///android_asset/nearby_markets_map.html")
    }

    private fun requestLocation() {
        progress.visibility = View.VISIBLE
        status.text = "Obtendo sua localização…"
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION
            )
            return
        }
        startLocationLookup()
    }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun startLocationLookup() {
        if (!hasLocationPermission()) return
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
        val last = providers.mapNotNull { provider ->
            runCatching { if (hasLocationPermission()) locationManager.getLastKnownLocation(provider) else null }.getOrNull()
        }.maxByOrNull { it.time }

        if (last != null && System.currentTimeMillis() - last.time < 10 * 60 * 1000L) {
            onLocationChanged(last)
            return
        }
        if (providers.isEmpty()) {
            progress.visibility = View.GONE
            status.text = "Ative a localização do celular para procurar mercados próximos."
            return
        }
        providers.forEach { provider ->
            runCatching {
                if (hasLocationPermission()) locationManager.requestLocationUpdates(provider, 0L, 0f, this, Looper.getMainLooper())
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        locationManager.removeUpdates(this)
        pendingLocation = location
        if (mapReady) renderUserLocation(location)
        loadMarkets(location)
    }

    private fun renderUserLocation(location: Location) {
        webView.evaluateJavascript("setUserLocation(${location.latitude},${location.longitude})", null)
    }

    private fun loadMarkets(location: Location) {
        progress.visibility = View.VISIBLE
        val cached = marketPreferences.loadCache(location, selectedRadiusMeters)
        val cachedMarkets = cached?.let { selectCachedMarkets(it.markets, selectedRadiusMeters, it.radiusMeters) }
        if (cached != null && cachedMarkets != null) {
            baseMarkets = cachedMarkets
            renderMarkets(baseMarkets)
            status.text = "Exibindo resultados recentes enquanto atualiza…"
        } else {
            status.text = "Procurando mercados próximos…"
        }

        if (!hasInternetConnection()) {
            progress.visibility = View.GONE
            if (cached != null) {
                status.text = "Sem internet. Exibindo mercados do cache recente."
            } else {
                baseMarkets = emptyList()
                renderMarkets(baseMarkets)
                status.text = "Sem internet e sem resultados recentes em cache. Sua localização foi obtida normalmente."
            }
            return
        }

        thread(name = "overpass-markets") {
            val result = runCatching { queryMarketsAdaptive(location, selectedRadiusMeters) }
            runOnUiThread {
                progress.visibility = View.GONE
                result.onSuccess { searchResult ->
                    baseMarkets = searchResult.markets
                    if (baseMarkets.isNotEmpty()) {
                        marketPreferences.saveCache(location, searchResult.radiusMeters, baseMarkets)
                    }
                    renderMarkets(baseMarkets)
                    updateSuccessStatus(searchResult)
                }.onFailure {
                    if (cached != null && cachedMarkets != null) {
                        baseMarkets = cachedMarkets
                                    renderMarkets(baseMarkets)
                        status.text = if (hasInternetConnection()) {
                            "Overpass indisponível agora. Exibindo mercados do cache recente."
                        } else {
                            "Sem internet. Exibindo mercados do cache recente."
                        }
                    } else {
                        baseMarkets = emptyList()
                        renderMarkets(baseMarkets)
                        status.text = if (hasInternetConnection()) {
                            "O serviço de mercados está temporariamente indisponível. Sua localização não foi considerada inválida."
                        } else {
                            "Sem internet e sem resultados recentes em cache. Sua localização foi obtida normalmente."
                        }
                    }
                }
            }
        }
    }

    private fun updateSuccessStatus(result: MarketSearchResult) {
        val radiusKm = result.radiusMeters / 1000
        status.text = when (result.markets.size) {
            0 -> "Nenhum mercado cadastrado no OpenStreetMap em até $radiusKm km. Sua localização foi obtida normalmente."
            1 -> "1 mercado encontrado em até $radiusKm km."
            else -> "${result.markets.size} mercados encontrados em até $radiusKm km."
        }
    }


    private fun selectCachedMarkets(markets: List<NearbyMarket>, requestedRadius: Int, cachedRadius: Int): List<NearbyMarket> {
        val requested = markets.filter { it.distanceMeters <= requestedRadius }
        return if (requested.size >= MIN_RESULTS_BEFORE_STOP || cachedRadius <= requestedRadius) {
            requested
        } else {
            markets.filter { it.distanceMeters <= cachedRadius }
        }
    }

    private fun queryMarketsAdaptive(location: Location, requestedRadius: Int): MarketSearchResult {
        val steps = RADIUS_STEPS_METERS.filter { it >= requestedRadius }.ifEmpty { listOf(requestedRadius) }
        var lastResult = emptyList<NearbyMarket>()
        var lastRadius = steps.first()
        for (radius in steps) {
            lastRadius = radius
            runOnUiThread { status.text = "Procurando em até ${radius / 1000} km…" }
            lastResult = queryMarkets(location, radius)
            if (lastResult.size >= MIN_RESULTS_BEFORE_STOP || radius == RADIUS_STEPS_METERS.last()) break
        }
        return MarketSearchResult(lastResult, lastRadius)
    }

    private fun queryMarkets(location: Location, radiusMeters: Int): List<NearbyMarket> {
        val query = """
            [out:json][timeout:30];
            (
              nwr["shop"="supermarket"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["shop"="convenience"](around:$radiusMeters,${location.latitude},${location.longitude});
            );
            out center tags;
        """.trimIndent()

        val elements = JSONObject(executeOverpass(query)).optJSONArray("elements") ?: JSONArray()
        val markets = mutableListOf<NearbyMarket>()
        for (i in 0 until elements.length()) {
            val item = elements.optJSONObject(i) ?: continue
            val tags = item.optJSONObject("tags") ?: JSONObject()
            val center = item.optJSONObject("center")
            val lat = if (item.has("lat")) item.optDouble("lat") else center?.optDouble("lat") ?: Double.NaN
            val lon = if (item.has("lon")) item.optDouble("lon") else center?.optDouble("lon") ?: Double.NaN
            if (lat.isNaN() || lon.isNaN()) continue

            val name = tags.optString("name").ifBlank { tags.optString("brand").ifBlank { "Mercado" } }
            val type = item.optString("type")
            val id = item.optLong("id", 0L)
            val key = if (type.isNotBlank() && id > 0L) "osm:$type:$id" else MarketPreferencesRepository.fallbackKey(name, lat, lon)
            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, lat, lon, distance)
            markets += NearbyMarket(
                key = key,
                name = name,
                lat = lat,
                lon = lon,
                distanceMeters = distance[0].toDouble(),
                address = buildAddress(tags)
            )
        }
        return markets.distinctBy { it.key }.sortedBy { it.distanceMeters }.take(MAX_RESULTS)
    }

    private fun renderMarkets(markets: List<NearbyMarket>) {
        val favorites = marketPreferences.favoriteKeys()
        val associatedLists = shoppingRepository.loadLists()
            .mapNotNull { list -> list.marketKey?.takeIf { it.isNotBlank() }?.let { it to list.id } }
            .toMap()
        displayedMarkets = markets.map { market ->
            market.copy(
                favorite = market.key in favorites,
                listId = associatedLists[market.key]
            )
        }.sortedWith(compareByDescending<NearbyMarket> { it.favorite }.thenBy { it.distanceMeters })
        adapter.submitList(displayedMarkets)
        renderMap(displayedMarkets)
    }

    private fun renderMap(markets: List<NearbyMarket>) {
        if (!mapReady) return
        val payload = JSONArray().apply {
            markets.forEach { market ->
                put(JSONObject().apply {
                    put("key", market.key)
                    put("name", market.name)
                    put("lat", market.lat)
                    put("lon", market.lon)
                    put("distanceMeters", market.distanceMeters)
                    put("address", market.address)
                    put("source", market.source)
                    put("fromCache", market.fromCache)
                    put("favorite", market.favorite)
                    put("hasList", market.listId != null)
                })
            }
        }
        val quoted = JSONObject.quote(payload.toString())
        webView.evaluateJavascript("renderMarkets(JSON.parse($quoted))", null)
    }

    private fun toggleFavorite(market: NearbyMarket) {
        marketPreferences.toggleFavorite(market.key)
        renderMarkets(baseMarkets)
    }

    private fun openOrCreateMarketList(market: NearbyMarket) {
        val currentList = shoppingRepository.loadLists().firstOrNull { it.marketKey == market.key }
        if (currentList != null) {
            openList(currentList.id)
        } else {
            confirmCreateList(market)
        }
    }

    private fun confirmCreateList(market: NearbyMarket) {
        val date = SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date())
        val suggestedName = "${market.name} — $date"
        MaterialAlertDialogBuilder(this)
            .setTitle("Criar lista para este mercado?")
            .setMessage("Será criada uma lista associada a ${market.name}. Depois, este mercado mostrará a opção Abrir lista deste mercado.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Criar lista") { _, _ ->
                val lists = shoppingRepository.loadLists()
                val existing = lists.firstOrNull { it.marketKey == market.key }
                if (existing != null) {
                    openList(existing.id)
                    return@setPositiveButton
                }
                val newList = ShoppingList(
                    name = suggestedName,
                    marketKey = market.key,
                    marketName = market.name
                )
                lists.add(0, newList)
                shoppingRepository.saveLists(lists)
                renderMarkets(baseMarkets)
                openList(newList.id)
            }
            .show()
    }

    private fun openList(listId: Long) {
        startActivity(Intent(this, ListDetailActivity::class.java).putExtra(ListDetailActivity.EXTRA_LIST_ID, listId))
    }

    private fun executeOverpass(query: String): String {
        var lastError: Throwable? = null
        for (endpoint in OVERPASS_URLS) {
            val result = runCatching {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("User-Agent", "MeuSupermercado/${com.listamercado.app.BuildConfig.VERSION_NAME} Android")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                }
                try {
                    val body = "data=" + URLEncoder.encode(query, Charsets.UTF_8.name())
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
            }
            if (result.isSuccess) return result.getOrThrow()
            lastError = result.exceptionOrNull()
        }
        throw lastError ?: IllegalStateException("Falha ao consultar Overpass")
    }

    private fun buildAddress(tags: JSONObject): String {
        val street = tags.optString("addr:street")
        val number = tags.optString("addr:housenumber")
        val neighborhood = tags.optString("addr:suburb")
        val city = tags.optString("addr:city")
        return listOf(
            listOf(street, number).filter { it.isNotBlank() }.joinToString(", "),
            neighborhood,
            city
        ).filter { it.isNotBlank() }.distinct().joinToString(" • ")
    }

    private fun hasInternetConnection(): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) startLocationLookup()
            else {
                progress.visibility = View.GONE
                status.text = "Permissão de localização necessária para encontrar mercados próximos."
            }
        }
    }

    override fun onDestroy() {
        locationManager.removeUpdates(this)
        webView.removeJavascriptInterface("AndroidMarket")
        webView.destroy()
        super.onDestroy()
    }

    inner class MapBridge {
        @JavascriptInterface
        fun listAction(key: String) {
            runOnUiThread { displayedMarkets.firstOrNull { it.key == key }?.let(::openOrCreateMarketList) }
        }

        @JavascriptInterface
        fun toggleFavorite(key: String) {
            runOnUiThread { displayedMarkets.firstOrNull { it.key == key }?.let(::toggleFavorite) }
        }
    }

    private fun chipIdForRadius(radius: Int): Int = when (radius) {
        10_000 -> R.id.chipRadius10
        20_000 -> R.id.chipRadius20
        30_000 -> R.id.chipRadius30
        50_000 -> R.id.chipRadius50
        else -> R.id.chipRadius5
    }

    private fun radiusForChipId(id: Int): Int? = when (id) {
        R.id.chipRadius5 -> 5_000
        R.id.chipRadius10 -> 10_000
        R.id.chipRadius20 -> 20_000
        R.id.chipRadius30 -> 30_000
        R.id.chipRadius50 -> 50_000
        else -> null
    }

    private data class MarketSearchResult(val markets: List<NearbyMarket>, val radiusMeters: Int)

    companion object {
        private const val REQUEST_LOCATION = 7001
        private const val MIN_RESULTS_BEFORE_STOP = 5
        private const val MAX_RESULTS = 100
        private val RADIUS_STEPS_METERS = listOf(5_000, 10_000, 20_000, 30_000, 50_000)
        private val OVERPASS_URLS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
        )
    }
}
