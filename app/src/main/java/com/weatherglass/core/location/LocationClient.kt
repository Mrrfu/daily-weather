package com.weatherglass.core.location

import com.weatherglass.core.model.LocationPoint

interface LocationClient {
    suspend fun getCurrentLocation(): LocationPoint?
}
