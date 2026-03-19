package com.weatherglass.core.model

data class City(
    val id: String,
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false,
    val sortOrder: Int = 0
)
