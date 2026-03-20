package com.weatherglass.core.network.openweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenCurrentResponse(
    @SerialName("dt") val dt: Long,
    @SerialName("main") val main: OpenMain,
    @SerialName("weather") val weather: List<OpenWeatherItem>,
    @SerialName("wind") val wind: OpenWind
)

@Serializable
data class OpenForecastResponse(
    @SerialName("list") val list: List<OpenForecastItem>
)

@Serializable
data class OpenForecastItem(
    @SerialName("dt") val dt: Long,
    @SerialName("main") val main: OpenMain,
    @SerialName("weather") val weather: List<OpenWeatherItem>,
    @SerialName("wind") val wind: OpenWind? = null,
    @SerialName("pop") val pop: Double? = null
)

@Serializable
data class OpenMain(
    @SerialName("temp") val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("pressure") val pressure: Int,
    @SerialName("humidity") val humidity: Int,
    @SerialName("temp_min") val tempMin: Double = temp,
    @SerialName("temp_max") val tempMax: Double = temp
)

@Serializable
data class OpenWeatherItem(
    @SerialName("id") val id: Int,
    @SerialName("main") val main: String,
    @SerialName("description") val description: String,
    @SerialName("icon") val icon: String
)

@Serializable
data class OpenWind(
    @SerialName("speed") val speed: Double,
    @SerialName("deg") val deg: Int? = null
)

@Serializable
data class OpenGeoItem(
    @SerialName("name") val name: String,
    @SerialName("lat") val lat: Double,
    @SerialName("lon") val lon: Double,
    @SerialName("country") val country: String,
    @SerialName("state") val state: String? = null
)
