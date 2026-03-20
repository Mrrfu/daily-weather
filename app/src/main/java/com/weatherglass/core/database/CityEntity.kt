package com.weatherglass.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cities",
    indices = [
        Index(value = ["name"]),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["sortOrder"])
    ]
)
data class CityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean,
    val sortOrder: Int
)
