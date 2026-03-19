package com.weatherglass.core.network.qweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QWeatherNowResponse(
    @SerialName("now") val now: QWeatherNow
)

@Serializable
data class QWeatherNow(
    @SerialName("temp") val temp: String,
    @SerialName("feelsLike") val feelsLike: String,
    @SerialName("text") val text: String,
    @SerialName("icon") val icon: String,
    @SerialName("windSpeed") val windSpeed: String,
    @SerialName("windDir") val windDir: String,
    @SerialName("humidity") val humidity: String,
    @SerialName("pressure") val pressure: String,
    @SerialName("obsTime") val obsTime: String
)

@Serializable
data class QWeatherHourlyResponse(
    @SerialName("hourly") val hourly: List<QWeatherHourly>
)

@Serializable
data class QWeatherHourly(
    @SerialName("fxTime") val fxTime: String,
    @SerialName("temp") val temp: String,
    @SerialName("icon") val icon: String,
    @SerialName("pop") val pop: String
)

@Serializable
data class QWeatherDailyResponse(
    @SerialName("daily") val daily: List<QWeatherDaily>
)

@Serializable
data class QWeatherDaily(
    @SerialName("fxDate") val fxDate: String,
    @SerialName("tempMax") val tempMax: String,
    @SerialName("tempMin") val tempMin: String,
    @SerialName("iconDay") val iconDay: String,
    @SerialName("windSpeedDay") val windSpeedDay: String? = null,
    @SerialName("windScaleDay") val windScaleDay: String? = null,
    @SerialName("sunrise") val sunrise: String,
    @SerialName("sunset") val sunset: String
)

@Serializable
data class QWeatherCityLookupResponse(
    @SerialName("location") val location: List<QWeatherCityItem> = emptyList()
)

@Serializable
data class QWeatherCityItem(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("country") val country: String,
    @SerialName("lat") val lat: String,
    @SerialName("lon") val lon: String
)

@Serializable
data class QWeatherIndicesResponse(
    @SerialName("daily") val daily: List<QWeatherIndexItem> = emptyList()
)

@Serializable
data class QWeatherIndexItem(
    @SerialName("type") val type: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("text") val text: String
)
