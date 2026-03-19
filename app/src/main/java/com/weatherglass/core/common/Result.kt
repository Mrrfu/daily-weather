package com.weatherglass.core.common

sealed class AppResult<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean = false) : AppResult<T>()
    data class Error(val throwable: Throwable, val fallbackUsed: Boolean = false) : AppResult<Nothing>()
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val value: T, val fromCache: Boolean = false) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
