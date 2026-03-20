package com.weatherglass.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WeatherCacheEntity::class, CityEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun cityDao(): CityDao
}
