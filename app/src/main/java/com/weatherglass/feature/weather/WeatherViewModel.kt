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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherScreenState(
    val weatherState: UiState<WeatherBundle> = UiState.Loading,
    val cities: List<City> = emptyList(),
    val selectedCityId: String? = null,
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
                _state.update {
                    it.copy(
                        cities = list,
                        selectedCityId = it.selectedCityId ?: list.firstOrNull()?.id
                    )
                }
                if (list.isNotEmpty()) {
                    refreshForCity(_state.value.selectedCityId ?: list.first().id)
                }
            }
        }
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
            repository.cacheCity(city)
            _state.update { it.copy(selectedCityId = city.id) }
            refreshForCity(city.id)
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
                            networkWarning = if (result.fromCache) "网络异常，当前展示缓存数据" else null
                        )
                    }
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
}
