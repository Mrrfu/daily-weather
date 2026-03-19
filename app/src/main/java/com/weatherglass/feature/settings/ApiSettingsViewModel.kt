package com.weatherglass.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherglass.core.data.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApiSettingsState(
    val qWeatherKey: String = "",
    val openWeatherKey: String = "",
    val savedToast: String? = null
)

@HiltViewModel
class ApiSettingsViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _state = MutableStateFlow(ApiSettingsState())
    val state: StateFlow<ApiSettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            apiKeyStore.keysFlow.collect { keys ->
                _state.update {
                    it.copy(
                        qWeatherKey = keys.qWeatherKey,
                        openWeatherKey = keys.openWeatherKey
                    )
                }
            }
        }
    }

    fun updateQWeather(value: String) {
        _state.update { it.copy(qWeatherKey = value) }
    }

    fun updateOpenWeather(value: String) {
        _state.update { it.copy(openWeatherKey = value) }
    }

    fun save() {
        viewModelScope.launch {
            apiKeyStore.save(
                qWeatherKey = _state.value.qWeatherKey,
                openWeatherKey = _state.value.openWeatherKey
            )
            _state.update { it.copy(savedToast = "已保存，天气请求将使用新 key") }
        }
    }

    fun consumeToast() {
        _state.update { it.copy(savedToast = null) }
    }
}
