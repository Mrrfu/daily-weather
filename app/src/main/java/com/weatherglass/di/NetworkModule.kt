package com.weatherglass.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.weatherglass.core.network.ApiConstants
import com.weatherglass.core.network.openweather.OpenWeatherApi
import com.weatherglass.core.network.qweather.QWeatherApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Qualifier
annotation class QWeatherRetrofit

@Qualifier
annotation class OpenWeatherRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
    }

    @Provides
    @Singleton
    @QWeatherRetrofit
    fun provideQWeatherRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.QWEATHER_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @OpenWeatherRetrofit
    fun provideOpenWeatherRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.OPENWEATHER_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideQWeatherApi(@QWeatherRetrofit retrofit: Retrofit): QWeatherApi {
        return retrofit.create(QWeatherApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenWeatherApi(@OpenWeatherRetrofit retrofit: Retrofit): OpenWeatherApi {
        return retrofit.create(OpenWeatherApi::class.java)
    }
}
