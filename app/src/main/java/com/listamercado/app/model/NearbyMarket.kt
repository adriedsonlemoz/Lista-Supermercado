package com.listamercado.app.model

data class NearbyMarket(
    val key: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Double,
    val address: String = "",
    val source: String = "OpenStreetMap",
    val fromCache: Boolean = false,
    val favorite: Boolean = false,
    val listId: Long? = null
)
