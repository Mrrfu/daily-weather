package com.weatherglass.core.network

import com.weatherglass.core.model.City
import com.weatherglass.core.model.WeatherBundle

interface WeatherProvider {
    val providerId: String
    val isConfigured: Boolean

    suspend fun getWeather(lat: Double, lon: Double, cityId: String): WeatherBundle

    suspend fun searchCity(query: String): List<City>
}
