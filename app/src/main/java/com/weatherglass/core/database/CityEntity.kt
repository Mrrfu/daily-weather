package com.weatherglass.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean,
    val sortOrder: Int
)
