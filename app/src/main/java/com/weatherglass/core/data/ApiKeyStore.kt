package com.weatherglass.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ApiKeys(
    val qWeatherKey: String = "",
    val openWeatherKey: String = ""
)

@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("api_keys.preferences_pb") }
    )

    val keysFlow: Flow<ApiKeys> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { pref ->
            ApiKeys(
                qWeatherKey = pref[QWEATHER_KEY].orEmpty(),
                openWeatherKey = pref[OPENWEATHER_KEY].orEmpty()
            )
        }

    suspend fun save(qWeatherKey: String, openWeatherKey: String) {
        dataStore.edit { pref ->
            pref[QWEATHER_KEY] = qWeatherKey.trim()
            pref[OPENWEATHER_KEY] = openWeatherKey.trim()
        }
    }

    suspend fun qWeatherKey(): String {
        return keysFlow.first().qWeatherKey
    }

    suspend fun openWeatherKey(): String {
        return keysFlow.first().openWeatherKey
    }

    companion object {
        private val QWEATHER_KEY = stringPreferencesKey("qweather_api_key")
        private val OPENWEATHER_KEY = stringPreferencesKey("openweather_api_key")
    }
}
