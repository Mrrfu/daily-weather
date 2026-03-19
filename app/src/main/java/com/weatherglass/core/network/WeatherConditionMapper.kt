package com.weatherglass.core.network

import com.weatherglass.core.model.WeatherCondition

fun mapQWeatherIcon(icon: String): WeatherCondition {
    val code = icon.toIntOrNull() ?: return WeatherCondition.Unknown
    return when (code) {
        in 100..103 -> WeatherCondition.Clear
        in 104..199 -> WeatherCondition.Cloudy
        in 300..399 -> WeatherCondition.Rain
        in 400..499 -> WeatherCondition.Snow
        in 500..504 -> WeatherCondition.Fog
        in 507..515 -> WeatherCondition.Haze
        else -> WeatherCondition.Unknown
    }
}

fun mapOpenWeatherId(id: Int): WeatherCondition {
    return when (id) {
        in 200..299 -> WeatherCondition.Thunder
        in 300..599 -> WeatherCondition.Rain
        in 600..699 -> WeatherCondition.Snow
        in 700..799 -> WeatherCondition.Fog
        800 -> WeatherCondition.Clear
        in 801..899 -> WeatherCondition.Cloudy
        else -> WeatherCondition.Unknown
    }
}

fun windDirectionFromDegrees(deg: Int?): String {
    if (deg == null) return "N/A"
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return dirs[((deg % 360) / 45) % dirs.size]
}
