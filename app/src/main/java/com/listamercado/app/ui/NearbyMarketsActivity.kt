package com.listamercado.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.listamercado.app.R
import com.listamercado.app.data.ShoppingRepository
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
    private lateinit var locationManager: LocationManager
    private var mapReady = false
    private var pendingLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_nearby_markets)
        InsetsHelper.applyScaffold(this, findViewById(R.id.rootMarkets))

        status = findViewById(R.id.textMarketStatus)
        progress = findViewById(R.id.progressMarkets)
        webView = findViewById(R.id.webMarkets)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        findViewById<ImageButton>(R.id.buttonBackMarkets).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.buttonRefreshMarkets).setOnClickListener { requestLocation() }

        configureMap()
        requestLocation()
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureMap() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = webView.settings.userAgentString + " MeuSupermercado/${com.listamercado.app.BuildConfig.VERSION_NAME}"
        webView.addJavascriptInterface(MapBridge(), "AndroidMarket")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                mapReady = true
                pendingLocation?.let { showLocationAndLoadMarkets(it) }
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
            runCatching {
                if (hasLocationPermission()) locationManager.getLastKnownLocation(provider) else null
            }.getOrNull()
        }.maxByOrNull { it.time }

        if (last != null && System.currentTimeMillis() - last.time < 10 * 60 * 1000L) {
            onLocationChanged(last)
            return
        }

        if (providers.isEmpty()) {
            progress.visibility = View.GONE
            status.text = "Ative a localização do celular para procurar supermercados."
            return
        }

        providers.forEach { provider ->
            runCatching {
                if (hasLocationPermission()) {
                    locationManager.requestLocationUpdates(provider, 0L, 0f, this, Looper.getMainLooper())
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        locationManager.removeUpdates(this)
        pendingLocation = location
        if (mapReady) showLocationAndLoadMarkets(location)
    }

    private fun showLocationAndLoadMarkets(location: Location) {
        webView.evaluateJavascript("setUserLocation(${location.latitude},${location.longitude})", null)
        status.text = "Procurando supermercados em até ${RADIUS_METERS / 1000} km…"
        loadMarkets(location)
    }

    private fun loadMarkets(location: Location) {
        progress.visibility = View.VISIBLE
        thread(name = "overpass-markets") {
            val result = runCatching { queryMarkets(location) }
            runOnUiThread {
                progress.visibility = View.GONE
                result.onSuccess { markets ->
                    status.text = when (markets.size) {
                        0 -> "Nenhum supermercado encontrado no raio de ${RADIUS_METERS / 1000} km."
                        1 -> "1 supermercado encontrado no raio de ${RADIUS_METERS / 1000} km."
                        else -> "${markets.size} supermercados encontrados no raio de ${RADIUS_METERS / 1000} km."
                    }
                    val payload = JSONArray().apply { markets.forEach { put(it.toJson()) } }
                    val quoted = JSONObject.quote(payload.toString())
                    webView.evaluateJavascript("renderMarkets(JSON.parse($quoted))", null)
                }.onFailure {
                    status.text = "Não foi possível consultar os supermercados. Verifique sua internet."
                }
            }
        }
    }

    private fun queryMarkets(location: Location): List<NearbyMarket> {
        val query = """
            [out:json][timeout:20];
            (
              nwr["shop"="supermarket"](around:$RADIUS_METERS,${location.latitude},${location.longitude});
            );
            out center tags;
        """.trimIndent()

        val connection = (URL(OVERPASS_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 25_000
            doOutput = true
            setRequestProperty("User-Agent", "MeuSupermercado/1.0 Android")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        val body = "data=" + URLEncoder.encode(query, Charsets.UTF_8.name())
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")

        val json = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val elements = JSONObject(json).optJSONArray("elements") ?: JSONArray()
        val markets = mutableListOf<NearbyMarket>()
        for (i in 0 until elements.length()) {
            val item = elements.optJSONObject(i) ?: continue
            val tags = item.optJSONObject("tags") ?: JSONObject()
            val center = item.optJSONObject("center")
            val lat = if (item.has("lat")) item.optDouble("lat") else center?.optDouble("lat") ?: Double.NaN
            val lon = if (item.has("lon")) item.optDouble("lon") else center?.optDouble("lon") ?: Double.NaN
            if (lat.isNaN() || lon.isNaN()) continue

            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, lat, lon, distance)
            markets += NearbyMarket(
                name = tags.optString("name").ifBlank { tags.optString("brand").ifBlank { "Supermercado" } },
                lat = lat,
                lon = lon,
                distanceMeters = distance[0].toDouble(),
                address = buildAddress(tags)
            )
        }
        return markets.sortedBy { it.distanceMeters }.take(MAX_RESULTS)
    }

    private fun buildAddress(tags: JSONObject): String {
        val street = tags.optString("addr:street")
        val number = tags.optString("addr:housenumber")
        val neighborhood = tags.optString("addr:suburb")
        return listOf(
            listOf(street, number).filter { it.isNotBlank() }.joinToString(", "),
            neighborhood
        ).filter { it.isNotBlank() }.joinToString(" • ")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationLookup()
            } else {
                progress.visibility = View.GONE
                status.text = "Permissão de localização necessária para encontrar supermercados próximos."
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
        fun createList(name: String, lat: Double, lon: Double) {
            runOnUiThread { confirmCreateList(name) }
        }
    }

    private fun confirmCreateList(marketName: String) {
        val date = SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date())
        val suggestedName = "$marketName — $date"
        MaterialAlertDialogBuilder(this)
            .setTitle("Criar lista para este mercado?")
            .setMessage("Será criada uma nova lista chamada “$suggestedName”. Você poderá renomear depois.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Criar lista") { _, _ ->
                val repository = ShoppingRepository(this)
                val lists = repository.loadLists()
                val newList = ShoppingList(name = suggestedName)
                lists.add(0, newList)
                repository.saveLists(lists)
                startActivity(
                    Intent(this, ListDetailActivity::class.java)
                        .putExtra(ListDetailActivity.EXTRA_LIST_ID, newList.id)
                )
            }
            .show()
    }

    private data class NearbyMarket(
        val name: String,
        val lat: Double,
        val lon: Double,
        val distanceMeters: Double,
        val address: String
    ) {
        fun toJson() = JSONObject().apply {
            put("name", name)
            put("lat", lat)
            put("lon", lon)
            put("distanceMeters", distanceMeters)
            put("address", address)
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 7001
        private const val RADIUS_METERS = 5000
        private const val MAX_RESULTS = 60
        private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
    }
}
