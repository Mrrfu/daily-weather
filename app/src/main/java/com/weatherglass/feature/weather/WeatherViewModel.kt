package com.weatherglass.feature.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherglass.core.common.AppResult
import com.weatherglass.core.common.UiState
import com.weatherglass.core.data.LocationRepository
import com.weatherglass.core.data.WeatherRepository
import com.weatherglass.core.model.City
import com.weatherglass.core.model.WeatherBundle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherScreenState(
    val weatherState: UiState<WeatherBundle> = UiState.Loading,
    val cities: List<City> = emptyList(),
    val selectedCityId: String? = null,
    val weatherByCityId: Map<String, WeatherBundle> = emptyMap(),
    val hasLocationPermission: Boolean = false,
    val networkWarning: String? = null
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherScreenState())
    val state: StateFlow<WeatherScreenState> = _state.asStateFlow()

    init {
        observeCities()
    }

    private fun observeCities() {
        viewModelScope.launch {
            repository.observeSavedCities().collect { list ->
                val deduped = deduplicateCities(list)
                _state.update {
                    it.copy(
                        cities = deduped,
                        selectedCityId = it.selectedCityId?.takeIf { id -> deduped.any { c -> c.id == id } }
                            ?: deduped.firstOrNull()?.id
                    )
                }
                if (deduped.isNotEmpty()) {
                    refreshForCity(_state.value.selectedCityId ?: deduped.first().id)
                }
            }
        }
    }

    private fun deduplicateCities(input: List<City>): List<City> {
        val result = mutableListOf<City>()
        input.forEach { city ->
            val hitIndex = result.indexOfFirst { existing ->
                existing.name.equals(city.name, ignoreCase = true) &&
                    abs(existing.latitude - city.latitude) <= 0.12 &&
                    abs(existing.longitude - city.longitude) <= 0.12
            }
            if (hitIndex == -1) {
                result += city
            } else {
                val keep = if (city.isCurrentLocation) city else result[hitIndex]
                result[hitIndex] = keep
            }
        }
        return result.sortedBy { it.sortOrder }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        _state.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            loadCurrentLocationCity()
        }
    }

    fun loadCurrentLocationCity() {
        viewModelScope.launch {
            val city = locationRepository.currentLocationCity() ?: return@launch
            val merged = repository.cacheCity(city)
            _state.update { it.copy(selectedCityId = merged.id) }
            refreshForCity(merged.id)
        }
    }

    fun refreshCurrent() {
        val cityId = _state.value.selectedCityId ?: return
        refreshForCity(cityId)
    }

    fun selectCity(cityId: String) {
        _state.update { it.copy(selectedCityId = cityId) }
        refreshForCity(cityId)
    }

    private fun refreshForCity(cityId: String) {
        val city = _state.value.cities.firstOrNull { it.id == cityId } ?: return
        viewModelScope.launch {
            _state.update { it.copy(weatherState = UiState.Loading, networkWarning = null) }
            when (val result = repository.fetchWeather(city)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            weatherState = UiState.Success(result.data, fromCache = result.fromCache),
                            weatherByCityId = it.weatherByCityId + (city.id to result.data),
                            networkWarning = if (result.fromCache) "网络异常，当前展示缓存数据" else null
                        )
                    }
                    prefetchAdjacent(cityId)
                }

                is AppResult.Error -> {
                    _state.update {
                        it.copy(
                            weatherState = UiState.Error(result.throwable.message ?: "天气获取失败")
                        )
                    }
                }
            }
        }
    }

    private fun prefetchAdjacent(cityId: String) {
        val cities = _state.value.cities
        val index = cities.indexOfFirst { it.id == cityId }
        if (index == -1) return
        val neighbors = listOfNotNull(cities.getOrNull(index - 1), cities.getOrNull(index + 1))

        neighbors.forEach { neighbor ->
            viewModelScope.launch {
                when (val result = repository.fetchWeather(neighbor)) {
                    is AppResult.Success -> {
                        _state.update {
                            it.copy(weatherByCityId = it.weatherByCityId + (neighbor.id to result.data))
                        }
                    }

                    is AppResult.Error -> Unit
                }
            }
        }
    }
}
