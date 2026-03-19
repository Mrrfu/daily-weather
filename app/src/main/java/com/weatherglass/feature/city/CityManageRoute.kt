package com.weatherglass.feature.city

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weatherglass.core.model.City

@Composable
fun CityManageRoute(
    onBack: () -> Unit,
    viewModel: CityManageViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    CityManageScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onSaveCity = viewModel::saveCity,
        onDeleteCity = viewModel::removeCity,
        onReorder = viewModel::reorder
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityManageScreen(
    state: CityManageState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSaveCity: (City) -> Unit,
    onDeleteCity: (City) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF03060D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "back", tint = Color.White)
                    }
                    Text(
                        text = "城市管理",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                SearchInput(
                    value = state.query,
                    onValueChange = onQueryChange
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    if (state.query.isNotBlank() && state.result.isNotEmpty()) {
                        item {
                            Text("搜索结果", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }
                        items(state.result, key = { it.id }) { city ->
                            CitySearchItem(city = city, onSave = { onSaveCity(city) })
                        }
                    }

                    val currentCity = state.saved.firstOrNull { it.isCurrentLocation }
                    if (currentCity != null) {
                        item {
                            Text("当前定位", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                        }
                        item(key = "current-${currentCity.id}") {
                            CityWeatherCard(
                                city = currentCity,
                                subtitle = "定位",
                                summary = state.weatherByCityId[currentCity.id]
                            )
                        }
                    }

                    val normalCities = state.saved.filterNot { it.isCurrentLocation }
                    if (normalCities.isNotEmpty()) {
                        item {
                            Text("已添加城市", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                        }
                        itemsIndexed(normalCities, key = { _, item -> item.id }) { index, city ->
                            SavedCityItem(
                                city = city,
                                summary = state.weatherByCityId[city.id],
                                onDelete = { onDeleteCity(city) },
                                onMoveUp = { if (index > 0) onReorder(index, index - 1) },
                                onMoveDown = { if (index < normalCities.lastIndex) onReorder(index, index + 1) },
                                canMoveUp = index > 0,
                                canMoveDown = index < normalCities.lastIndex
                            )
                        }
                    }
                }

                SnackbarHost(snackbarHostState)
            }
        }
    }
}

@Composable
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(20.dp)),
        color = Color(0xFF1E232D),
        shape = RoundedCornerShape(20.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = Color.White)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "search",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                "搜索位置",
                                color = Color.White.copy(alpha = 0.55f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun CitySearchItem(city: City, onSave: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(city.name, color = Color.White)
                Text(city.countryCode, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.62f))
            }
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C72FF), contentColor = Color.White)
            ) {
                Text("添加")
            }
        }
    }
}

@Composable
private fun CityWeatherCard(city: City, subtitle: String, summary: CityWeatherSummary?) {
    val currentTemp = summary?.currentTemp?.let { "${it}°" } ?: "--°"
    val range = summary?.let { "${it.minTemp}° / ${it.maxTemp}°" } ?: "-- / --"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF111A2F), Color(0xFF15284A), Color(0xFF0A1424))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(city.name, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(currentTemp, color = Color.White, style = MaterialTheme.typography.headlineLarge)
                    Text(range, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedCityItem(
    city: City,
    summary: CityWeatherSummary?,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    var showActions by remember { mutableStateOf(false) }
    val currentTemp = summary?.currentTemp?.let { "${it}°" } ?: "--°"
    val conditionText = summary?.condition?.toCityLabel() ?: "长按管理"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = { showActions = true }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF111A2F), Color(0xFF15284A), Color(0xFF0A1424))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(city.name, color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("${city.latitude}, ${city.longitude}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.68f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(currentTemp, color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Text(conditionText, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
            }
        }

        DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
            DropdownMenuItem(
                text = { Text("上移") },
                enabled = canMoveUp,
                onClick = {
                    showActions = false
                    onMoveUp()
                }
            )
            DropdownMenuItem(
                text = { Text("下移") },
                enabled = canMoveDown,
                onClick = {
                    showActions = false
                    onMoveDown()
                }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showActions = false
                    onDelete()
                }
            )
        }
    }
}

private fun com.weatherglass.core.model.WeatherCondition.toCityLabel(): String {
    return when (this) {
        com.weatherglass.core.model.WeatherCondition.Clear -> "晴"
        com.weatherglass.core.model.WeatherCondition.Cloudy -> "多云"
        com.weatherglass.core.model.WeatherCondition.Rain -> "雨"
        com.weatherglass.core.model.WeatherCondition.Snow -> "雪"
        com.weatherglass.core.model.WeatherCondition.Thunder -> "雷暴"
        com.weatherglass.core.model.WeatherCondition.Fog -> "雾"
        com.weatherglass.core.model.WeatherCondition.Wind -> "大风"
        com.weatherglass.core.model.WeatherCondition.Haze -> "霾"
        com.weatherglass.core.model.WeatherCondition.Unknown -> "天气"
    }
}
