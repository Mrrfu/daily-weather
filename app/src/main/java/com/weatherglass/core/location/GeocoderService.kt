package com.weatherglass.core.location

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class GeocoderService @Inject constructor(
    @ApplicationContext context: Context
) {
    private val geocoder = Geocoder(context, Locale.getDefault())

    fun cityNameOf(lat: Double, lon: Double): String {
        return runCatching {
            val list = geocoder.getFromLocation(lat, lon, 1)
            list?.firstOrNull()?.locality
                ?: list?.firstOrNull()?.subAdminArea
                ?: "Current Location"
        }.getOrDefault("Current Location")
    }
}
