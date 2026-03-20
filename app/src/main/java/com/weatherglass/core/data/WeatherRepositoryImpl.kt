package com.weatherglass.core.data

import com.weatherglass.core.common.AppResult
import com.weatherglass.core.database.CityDao
import com.weatherglass.core.database.WeatherCacheEntity
import com.weatherglass.core.database.WeatherDao
import com.weatherglass.core.database.toDomain
import com.weatherglass.core.database.toEntity
import com.weatherglass.core.model.City
import com.weatherglass.core.model.WeatherBundle
import kotlin.math.abs
import com.weatherglass.core.network.ProviderSelector
import com.weatherglass.core.network.WeatherProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards WeatherProvider>,
    private val selector: ProviderSelector,
    private val weatherDao: WeatherDao,
    private val cityDao: CityDao,
    private val json: Json
) : WeatherRepository {

    private val configuredProviders: List<WeatherProvider>
        get() = providers.filter { it.isConfigured }.sortedBy { it.providerId }

    override suspend fun fetchWeather(city: City): AppResult<WeatherBundle> {
        val ordered = selector.orderedProviders(city.latitude, city.longitude)
            .ifEmpty { configuredProviders }

        if (ordered.isEmpty()) {
            return AppResult.Error(IllegalStateException("未检测到可用天气服务，请在应用内输入设置配置 API Key"))
        }

        var lastError: Throwable? = null
        for (provider in ordered) {
            val result = runCatching {
                provider.getWeather(city.latitude, city.longitude, city.id)
            }
            if (result.isSuccess) {
                val weather = result.getOrThrow()
                weatherDao.upsert(
                    WeatherCacheEntity(
                        cityId = city.id,
                        providerId = weather.providerId,
                        updatedAtEpochSec = weather.updatedAtEpochSec,
                        payloadJson = json.encodeToString(WeatherBundle.serializer(), weather)
                    )
                )
                return AppResult.Success(weather)
            }
            lastError = result.exceptionOrNull()
        }

        // 尝试加载缓存（5分钟内的缓存有效）
        val cache = weatherDao.getByCityId(city.id)
        if (cache != null && isCacheValid(cache)) {
            val cached = json.decodeFromString(WeatherBundle.serializer(), cache.payloadJson)
            return AppResult.Success(cached, fromCache = true)
        }

        // 即使缓存过期，在网络失败时也返回缓存数据
        if (cache != null) {
            val cached = json.decodeFromString(WeatherBundle.serializer(), cache.payloadJson)
            return AppResult.Success(cached, fromCache = true)
        }

        return AppResult.Error(lastError ?: IllegalStateException("No provider available"))
    }

    private fun isCacheValid(cache: WeatherCacheEntity): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        val cacheAge = currentTime - cache.updatedAtEpochSec
        return cacheAge < CACHE_MAX_AGE_SECONDS
    }

    override suspend fun searchCity(query: String): AppResult<List<City>> {
        val clean = query.trim()
        if (clean.isBlank()) return AppResult.Success(emptyList())

        var lastError: Throwable? = null
        val activeProviders = configuredProviders
        if (activeProviders.isEmpty()) {
            return AppResult.Error(IllegalStateException("未检测到可用天气服务，请先配置 API Key"))
        }

        activeProviders.forEach { provider ->
            val attempt = runCatching { provider.searchCity(clean) }
            if (attempt.isSuccess) {
                return AppResult.Success(attempt.getOrThrow())
            }
            lastError = attempt.exceptionOrNull()
        }
        val normalized = when {
            lastError?.message?.contains("404") == true -> {
                IllegalStateException("城市搜索接口不可用（404），请检查 API Key 权限或服务域名配置")
            }

            lastError?.message?.contains("401") == true -> {
                IllegalStateException("城市搜索鉴权失败（401），请检查 API Key 是否正确")
            }

            else -> lastError ?: IllegalStateException("No provider")
        }
        return AppResult.Error(normalized)
    }

    override suspend fun cacheCity(city: City): City {
        val existing = cityDao.observeAll().firstOrNull().orEmpty()
        val duplicate = existing.firstOrNull { entity ->
            val sameName = entity.name.equals(city.name, ignoreCase = true)
            val closeEnough = abs(entity.latitude - city.latitude) <= 0.12 && abs(entity.longitude - city.longitude) <= 0.12
            sameName && closeEnough
        }

        val finalCity = if (duplicate != null) {
            city.copy(
                id = duplicate.id,
                sortOrder = duplicate.sortOrder,
                isCurrentLocation = city.isCurrentLocation || duplicate.isCurrentLocation
            )
        } else {
            city
        }

        cityDao.upsert(finalCity.toEntity())
        return finalCity
    }

    override suspend fun deleteCity(city: City) {
        cityDao.delete(city.toEntity())
    }

    override suspend fun reorderCities(cities: List<City>) {
        cityDao.upsertAll(cities.mapIndexed { index, city -> city.copy(sortOrder = index).toEntity() })
    }

    override fun observeSavedCities(): Flow<List<City>> {
        return cityDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    companion object {
        private const val CACHE_MAX_AGE_SECONDS = 300L // 5分钟
    }
}
