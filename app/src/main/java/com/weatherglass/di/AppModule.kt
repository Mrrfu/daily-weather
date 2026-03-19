package com.weatherglass.di

import com.weatherglass.core.data.LocationRepository
import com.weatherglass.core.data.LocationRepositoryImpl
import com.weatherglass.core.data.WeatherRepository
import com.weatherglass.core.data.WeatherRepositoryImpl
import com.weatherglass.core.location.FusedLocationClient
import com.weatherglass.core.location.LocationClient
import com.weatherglass.core.network.ProviderSelector
import com.weatherglass.core.network.RegionAwareProviderSelector
import com.weatherglass.core.network.WeatherProvider
import com.weatherglass.core.network.openweather.OpenWeatherProvider
import com.weatherglass.core.network.qweather.QWeatherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {

    @Binds
    abstract fun bindLocationClient(impl: FusedLocationClient): LocationClient

    @Binds
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {

    @Provides
    @IntoSet
    fun provideQWeatherProvider(provider: QWeatherProvider): WeatherProvider = provider

    @Provides
    @IntoSet
    fun provideOpenWeatherProvider(provider: OpenWeatherProvider): WeatherProvider = provider

    @Provides
    @Singleton
    fun provideProviderSelector(providers: Set<@JvmSuppressWildcards WeatherProvider>): ProviderSelector {
        return RegionAwareProviderSelector(providers.sortedBy { it.providerId })
    }
}
