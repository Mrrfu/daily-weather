package com.weatherglass.core.network

interface ProviderSelector {
    fun orderedProviders(lat: Double, lon: Double): List<WeatherProvider>
}

class RegionAwareProviderSelector(
    private val providers: List<WeatherProvider>
) : ProviderSelector {

    override fun orderedProviders(lat: Double, lon: Double): List<WeatherProvider> {
        val configured = providers.filter { it.isConfigured }
        if (configured.isEmpty()) return emptyList()
        val inChinaLikeBounds = lat in 18.0..54.0 && lon in 73.0..136.0
        val q = configured.firstOrNull { it.providerId == "qweather" }
        val o = configured.firstOrNull { it.providerId == "openweather" }
        val ordered = if (inChinaLikeBounds) listOfNotNull(q, o) else listOfNotNull(o, q)
        return ordered + configured.filterNot { ordered.contains(it) }
    }
}
