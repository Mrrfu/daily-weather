package com.weatherglass.core.data

import com.weatherglass.core.location.GeocoderService
import com.weatherglass.core.location.LocationClient
import com.weatherglass.core.model.City
import java.util.Locale
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val locationClient: LocationClient,
    private val geocoderService: GeocoderService
) : LocationRepository {

    override suspend fun currentLocationCity(): City? {
        val point = locationClient.getCurrentLocation() ?: return null
        val name = geocoderService.cityNameOf(point.latitude, point.longitude)
        return City(
            id = "current-${point.latitude}-${point.longitude}",
            name = name,
            countryCode = Locale.getDefault().country,
            latitude = point.latitude,
            longitude = point.longitude,
            isCurrentLocation = true,
            sortOrder = 0
        )
    }
}
