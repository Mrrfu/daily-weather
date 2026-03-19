package com.weatherglass.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeather(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val description: String,
    val condition: WeatherCondition,
    val humidity: Int,
    val pressureHPa: Int,
    val windSpeedKph: Double,
    val windDirection: String,
    val observationTimeEpochSec: Long,
    val iconCode: String? = null
)

@Serializable
data class HourlyForecast(
    val timestampEpochSec: Long,
    val temperatureC: Double,
    val condition: WeatherCondition,
    val precipitationProbability: Int
)

@Serializable
data class DailyForecast(
    val dateEpochSec: Long,
    val minTempC: Double,
    val maxTempC: Double,
    val condition: WeatherCondition,
    val windSpeedKph: Double? = null,
    val windScaleText: String? = null,
    val sunrise: String? = null,
    val sunset: String? = null
)

@Serializable
enum class WeatherCondition {
    Clear,
    Cloudy,
    Rain,
    Snow,
    Thunder,
    Fog,
    Wind,
    Haze,
    Unknown
}

@Serializable
data class WeatherBundle(
    val cityId: String,
    val providerId: String,
    val updatedAtEpochSec: Long,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val lifestyle: List<LifestyleAdvice> = emptyList()
)

@Serializable
data class LifestyleAdvice(
    val category: String,
    val brief: String,
    val detail: String = "",
    val source: String = "local"
)
