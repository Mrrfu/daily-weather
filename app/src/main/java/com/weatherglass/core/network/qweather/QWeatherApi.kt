package com.weatherglass.core.network.qweather

import retrofit2.http.GET
import retrofit2.http.Query

interface QWeatherApi {
    @GET("v7/weather/now")
    suspend fun current(
        @Query("location") location: String,
        @Query("key") key: String
    ): QWeatherNowResponse

    @GET("v7/weather/24h")
    suspend fun hourly(
        @Query("location") location: String,
        @Query("key") key: String
    ): QWeatherHourlyResponse

    @GET("v7/weather/7d")
    suspend fun daily(
        @Query("location") location: String,
        @Query("key") key: String
    ): QWeatherDailyResponse

    @GET("v7/indices/1d")
    suspend fun indices(
        @Query("type") type: String = "0",
        @Query("location") location: String,
        @Query("key") key: String
    ): QWeatherIndicesResponse

    @GET("https://geoapi.qweather.com/v2/city/lookup")
    suspend fun cityLookup(
        @Query("location") query: String,
        @Query("key") key: String
    ): QWeatherCityLookupResponse
}
