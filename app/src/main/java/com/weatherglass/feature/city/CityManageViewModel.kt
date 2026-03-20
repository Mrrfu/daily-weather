package com.weatherglass.feature.city

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherglass.core.common.AppResult
import com.weatherglass.core.data.WeatherRepository
import com.weatherglass.core.model.City
import com.weatherglass.core.model.WeatherBundle
import com.weatherglass.core.model.WeatherCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CityManageState(
    val query: String = "",
    val result: List<City> = emptyList(),
    val saved: List<City> = emptyList(),
    val weatherByCityId: Map<String, CityWeatherSummary> = emptyMap(),
    val searching: Boolean = false,
    val error: String? = null
)

data class CityWeatherSummary(
    val currentTemp: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val condition: WeatherCondition
)

@HiltViewModel
class CityManageViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CityManageState())
    val state: StateFlow<CityManageState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeSavedCities().collect { list ->
                val deduped = deduplicateCities(list)
                _state.update { it.copy(saved = deduped) }
                refreshCityWeatherSummaries(deduped)
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

    private fun refreshCityWeatherSummaries(cities: List<City>) {
        if (cities.isEmpty()) {
            _state.update { it.copy(weatherByCityId = emptyMap()) }
            return
        }

        cities.forEach { city ->
            viewModelScope.launch {
                when (val result = repository.fetchWeather(city)) {
                    is AppResult.Success -> {
                        val summary = result.data.toSummary()
                        _state.update {
                            it.copy(
                                weatherByCityId = it.weatherByCityId + (city.id to summary)
                            )
                        }
                    }

                    is AppResult.Error -> Unit
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            doSearch(value)
        }
    }

    private suspend fun doSearch(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(result = emptyList(), searching = false, error = null) }
            return
        }
        _state.update { it.copy(searching = true, error = null) }
        when (val result = repository.searchCity(query)) {
            is AppResult.Success -> {
                _state.update { it.copy(result = result.data, searching = false) }
            }

            is AppResult.Error -> {
                _state.update {
                    it.copy(
                        searching = false,
                        error = result.throwable.message ?: "搜索失败"
                    )
                }
            }
        }
    }

    fun saveCity(city: City) {
        viewModelScope.launch {
            repository.cacheCity(city.copy(sortOrder = _state.value.saved.size))
        }
    }

    fun removeCity(city: City) {
        viewModelScope.launch {
            repository.deleteCity(city)
            repository.reorderCities(_state.value.saved.filterNot { it.id == city.id })
        }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        val source = _state.value.saved.toMutableList()
        if (fromIndex !in source.indices || toIndex !in source.indices) return
        val item = source.removeAt(fromIndex)
        source.add(toIndex, item)
        viewModelScope.launch {
            repository.reorderCities(source)
        }
    }
}

private fun WeatherBundle.toSummary(): CityWeatherSummary {
    val min = daily.minOfOrNull { it.minTempC }?.toInt() ?: current.temperatureC.toInt()
    val max = daily.maxOfOrNull { it.maxTempC }?.toInt() ?: current.temperatureC.toInt()
    return CityWeatherSummary(
        currentTemp = current.temperatureC.toInt(),
        minTemp = min,
        maxTemp = max,
        condition = current.condition
    )
}
