package com.weatherglass.core.network.openweather

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApi {
    @GET("data/2.5/weather")
    suspend fun current(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") appId: String
    ): OpenCurrentResponse

    @GET("data/2.5/forecast")
    suspend fun forecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") appId: String
    ): OpenForecastResponse

    @GET("https://api.openweathermap.org/geo/1.0/direct")
    suspend fun directGeocoding(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("appid") appId: String
    ): List<OpenGeoItem>
}
