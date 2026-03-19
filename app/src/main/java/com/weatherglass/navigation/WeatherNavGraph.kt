package com.weatherglass.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.weatherglass.feature.city.CityManageRoute
import com.weatherglass.feature.settings.ApiSettingsRoute
import com.weatherglass.feature.weather.WeatherRoute

private const val WEATHER_ROUTE = "weather"
private const val CITY_ROUTE = "city"
private const val SETTINGS_ROUTE = "settings"

@Composable
fun WeatherNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = WEATHER_ROUTE) {
        composable(WEATHER_ROUTE) {
            WeatherRoute(
                onOpenCityManager = { navController.navigate(CITY_ROUTE) },
                onOpenApiSettings = { navController.navigate(SETTINGS_ROUTE) }
            )
        }
        composable(CITY_ROUTE) {
            CityManageRoute(onBack = { navController.popBackStack() })
        }
        composable(SETTINGS_ROUTE) {
            ApiSettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
