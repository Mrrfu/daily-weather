package com.weatherglass.core.network.qweather

import com.weatherglass.core.data.ApiKeyStore
import com.weatherglass.core.model.City
import com.weatherglass.core.model.CurrentWeather
import com.weatherglass.core.model.DailyForecast
import com.weatherglass.core.model.HourlyForecast
import com.weatherglass.core.model.LifestyleAdvice
import com.weatherglass.core.model.WeatherBundle
import com.weatherglass.core.network.WeatherProvider
import com.weatherglass.core.network.mapQWeatherIcon
import com.weatherglass.core.network.parseDateToEpochSec
import com.weatherglass.core.network.parseIsoTimeToEpochSec
import javax.inject.Inject

class QWeatherProvider @Inject constructor(
    private val api: QWeatherApi,
    private val apiKeyStore: ApiKeyStore
) : WeatherProvider {

    override val providerId: String = "qweather"
    override val isConfigured: Boolean
        get() = true

    override suspend fun getWeather(lat: Double, lon: Double, cityId: String): WeatherBundle {
        val key = apiKeyStore.qWeatherKey()
        check(key.isNotBlank()) { "QWeather API key missing" }
        val location = "$lon,$lat"
        val now = api.current(location, key).now
        val hourly = api.hourly(location, key).hourly.take(24)
        val daily = api.daily(location, key).daily.take(10)
        val indices = runCatching {
            api.indices(location = location, key = key).daily
        }.getOrDefault(emptyList())

        val lifestyle = indices.map {
            LifestyleAdvice(
                category = it.name,
                brief = it.category,
                detail = it.text,
                source = "qweather"
            )
        }

        return WeatherBundle(
            cityId = cityId,
            providerId = providerId,
            updatedAtEpochSec = System.currentTimeMillis() / 1000,
            current = CurrentWeather(
                temperatureC = now.temp.toDoubleOrNull() ?: 0.0,
                feelsLikeC = now.feelsLike.toDoubleOrNull() ?: 0.0,
                description = now.text,
                condition = mapQWeatherIcon(now.icon),
                humidity = now.humidity.toIntOrNull() ?: 0,
                pressureHPa = now.pressure.toIntOrNull() ?: 0,
                windSpeedKph = now.windSpeed.toDoubleOrNull() ?: 0.0,
                windDirection = now.windDir,
                observationTimeEpochSec = parseIsoTimeToEpochSec(now.obsTime),
                iconCode = now.icon
            ),
            hourly = hourly.map {
                HourlyForecast(
                    timestampEpochSec = parseIsoTimeToEpochSec(it.fxTime),
                    temperatureC = it.temp.toDoubleOrNull() ?: 0.0,
                    condition = mapQWeatherIcon(it.icon),
                    precipitationProbability = it.pop.toIntOrNull() ?: 0
                )
            },
            daily = daily.map {
                DailyForecast(
                    dateEpochSec = parseDateToEpochSec(it.fxDate),
                    minTempC = it.tempMin.toDoubleOrNull() ?: 0.0,
                    maxTempC = it.tempMax.toDoubleOrNull() ?: 0.0,
                    condition = mapQWeatherIcon(it.iconDay),
                    windSpeedKph = it.windSpeedDay?.toDoubleOrNull(),
                    windScaleText = it.windScaleDay,
                    sunrise = it.sunrise,
                    sunset = it.sunset
                )
            },
            lifestyle = lifestyle
        )
    }

    override suspend fun searchCity(query: String): List<City> {
        val key = apiKeyStore.qWeatherKey()
        check(key.isNotBlank()) { "QWeather API key missing" }
        return api.cityLookup(query, key).location.mapIndexed { index, item ->
            City(
                id = item.id,
                name = item.name,
                countryCode = item.country,
                latitude = item.lat.toDoubleOrNull() ?: 0.0,
                longitude = item.lon.toDoubleOrNull() ?: 0.0,
                sortOrder = index
            )
        }
    }
}
