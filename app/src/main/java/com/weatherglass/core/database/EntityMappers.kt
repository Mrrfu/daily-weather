package com.weatherglass.core.database

import com.weatherglass.core.model.City

fun CityEntity.toDomain(): City {
    return City(
        id = id,
        name = name,
        countryCode = countryCode,
        latitude = latitude,
        longitude = longitude,
        isCurrentLocation = isCurrentLocation,
        sortOrder = sortOrder
    )
}

fun City.toEntity(): CityEntity {
    return CityEntity(
        id = id,
        name = name,
        countryCode = countryCode,
        latitude = latitude,
        longitude = longitude,
        isCurrentLocation = isCurrentLocation,
        sortOrder = sortOrder
    )
}
