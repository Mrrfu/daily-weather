package com.weatherglass.core.data

import com.weatherglass.core.model.City

interface LocationRepository {
    suspend fun currentLocationCity(): City?
}
