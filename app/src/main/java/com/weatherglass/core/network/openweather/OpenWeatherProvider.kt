package com.weatherglass.core.network.openweather

import com.weatherglass.core.data.ApiKeyStore
import com.weatherglass.core.model.City
import com.weatherglass.core.model.CurrentWeather
import com.weatherglass.core.model.DailyForecast
import com.weatherglass.core.model.HourlyForecast
import com.weatherglass.core.model.LifestyleAdvice
import com.weatherglass.core.model.WeatherBundle
import com.weatherglass.core.network.WeatherProvider
import com.weatherglass.core.network.mapOpenWeatherId
import com.weatherglass.core.network.windDirectionFromDegrees
import javax.inject.Inject

class OpenWeatherProvider @Inject constructor(
    private val api: OpenWeatherApi,
    private val apiKeyStore: ApiKeyStore
) : WeatherProvider {

    override val providerId: String = "openweather"
    override val isConfigured: Boolean
        get() = true

    override suspend fun getWeather(lat: Double, lon: Double, cityId: String): WeatherBundle {
        val key = apiKeyStore.openWeatherKey()
        check(key.isNotBlank()) { "OpenWeather API key missing" }

        val current = api.current(lat, lon, appId = key)
        val forecast = api.forecast(lat, lon, appId = key)

        val hourly = forecast.list.take(24).map {
            val weatherItem = it.weather.firstOrNull()
            HourlyForecast(
                timestampEpochSec = it.dt,
                temperatureC = it.main.temp,
                condition = mapOpenWeatherId(weatherItem?.id ?: 0),
                description = weatherItem?.description ?: "",
                precipitationProbability = ((it.pop ?: 0.0) * 100).toInt(),
                humidity = it.main.humidity,
                windDirection = windDirectionFromDegrees(it.wind?.deg ?: 0),
                windSpeedKph = (it.wind?.speed ?: 0.0) * 3.6
            )
        }

        val daily = forecast.list
            .groupBy { it.dt / (24 * 3600) }
            .values
            .take(7)
            .map { entries ->
                val min = entries.minOf { it.main.tempMin }
                val max = entries.maxOf { it.main.tempMax }
                val pick = entries.first()
                DailyForecast(
                    dateEpochSec = pick.dt,
                    minTempC = min,
                    maxTempC = max,
                    condition = mapOpenWeatherId(pick.weather.firstOrNull()?.id ?: 0),
                    windSpeedKph = current.wind.speed * 3.6
                )
            }

        val weatherHead = current.weather.firstOrNull()
        val localLifestyle = buildOpenWeatherLifestyle(
            temp = current.main.temp,
            humidity = current.main.humidity,
            windKph = current.wind.speed * 3.6,
            conditionId = weatherHead?.id ?: 0
        )

        return WeatherBundle(
            cityId = cityId,
            providerId = providerId,
            updatedAtEpochSec = System.currentTimeMillis() / 1000,
            current = CurrentWeather(
                temperatureC = current.main.temp,
                feelsLikeC = current.main.feelsLike,
                description = weatherHead?.description ?: "",
                condition = mapOpenWeatherId(weatherHead?.id ?: 0),
                humidity = current.main.humidity,
                pressureHPa = current.main.pressure,
                windSpeedKph = current.wind.speed * 3.6,
                windDirection = windDirectionFromDegrees(current.wind.deg),
                observationTimeEpochSec = current.dt,
                iconCode = weatherHead?.icon
            ),
            hourly = hourly,
            daily = daily,
            lifestyle = localLifestyle
        )
    }

    override suspend fun searchCity(query: String): List<City> {
        val key = apiKeyStore.openWeatherKey()
        check(key.isNotBlank()) { "OpenWeather API key missing" }
        return api.directGeocoding(query = query, appId = key)
            .mapIndexed { index, item ->
                City(
                    id = "${item.name}-${item.lat}-${item.lon}",
                    name = item.name,
                    countryCode = item.country,
                    latitude = item.lat,
                    longitude = item.lon,
                    sortOrder = index
                )
            }
    }
}

private fun buildOpenWeatherLifestyle(
    temp: Double,
    humidity: Int,
    windKph: Double,
    conditionId: Int
): List<LifestyleAdvice> {
    val rainLike = conditionId in 200..599
    val uvBrief = if (conditionId == 800 && temp >= 30) "高" else if (conditionId == 800) "中" else "弱"
    val clothing = when {
        temp >= 30 -> "短袖短裤"
        temp >= 22 -> "短袖或薄外套"
        temp >= 15 -> "长袖加薄外套"
        temp >= 8 -> "卫衣或外套"
        else -> "厚外套或羽绒服"
    }
    val activity = when {
        rainLike -> "建议室内活动"
        windKph >= 28 -> "风大，减少长时户外"
        else -> "适合轻度户外活动"
    }

    return listOf(
        LifestyleAdvice("紫外线", uvBrief, "当前紫外线$uvBrief", "openweather-local"),
        LifestyleAdvice("穿衣", "建议", "建议穿衣：$clothing", "openweather-local"),
        LifestyleAdvice("运动", "建议", activity, "openweather-local"),
        LifestyleAdvice("晾晒", if (humidity > 75) "不宜" else "较适宜", "湿度${humidity}%", "openweather-local"),
        LifestyleAdvice("出行", if (rainLike) "带伞" else "正常", if (rainLike) "有降雨概率，建议带伞" else "天气稳定，可正常出行", "openweather-local")
    )
}
