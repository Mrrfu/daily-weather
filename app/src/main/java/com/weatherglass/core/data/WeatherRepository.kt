package com.weatherglass.core.data

import com.weatherglass.core.common.AppResult
import com.weatherglass.core.model.City
import com.weatherglass.core.model.WeatherBundle
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun fetchWeather(city: City): AppResult<WeatherBundle>

    suspend fun searchCity(query: String): AppResult<List<City>>

    suspend fun cacheCity(city: City): City

    suspend fun deleteCity(city: City)

    suspend fun reorderCities(cities: List<City>)

    fun observeSavedCities(): Flow<List<City>>
}
