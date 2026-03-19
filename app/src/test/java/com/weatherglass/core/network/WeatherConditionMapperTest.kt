package com.weatherglass.core.network

import com.weatherglass.core.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionMapperTest {

    @Test
    fun `mapQWeatherIcon returns clear for 100`() {
        assertEquals(WeatherCondition.Clear, mapQWeatherIcon("100"))
    }

    @Test
    fun `mapOpenWeatherId returns rain for 501`() {
        assertEquals(WeatherCondition.Rain, mapOpenWeatherId(501))
    }

    @Test
    fun `windDirectionFromDegrees handles null`() {
        assertEquals("N/A", windDirectionFromDegrees(null))
    }
}
