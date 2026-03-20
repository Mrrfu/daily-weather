package com.weatherglass.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weather_cache",
    indices = [Index(value = ["updatedAtEpochSec"])]
)
data class WeatherCacheEntity(
    @PrimaryKey val cityId: String,
    val providerId: String,
    val updatedAtEpochSec: Long,
    val payloadJson: String
)
