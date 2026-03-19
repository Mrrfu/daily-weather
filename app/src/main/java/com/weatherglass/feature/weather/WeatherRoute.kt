package com.weatherglass.feature.weather

import android.Manifest
import android.graphics.Paint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.weatherglass.core.common.UiState
import com.weatherglass.core.model.DailyForecast
import com.weatherglass.core.model.CurrentWeather
import com.weatherglass.core.model.LifestyleAdvice
import com.weatherglass.core.model.WeatherCondition
import com.weatherglass.core.model.WeatherBundle
import com.weatherglass.ui.theme.RainyEnd
import com.weatherglass.ui.theme.RainyStart
import com.weatherglass.ui.theme.SunnyEnd
import com.weatherglass.ui.theme.SunnyStart
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import kotlin.math.abs
import kotlin.math.roundToInt
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherRoute(
    onOpenCityManager: () -> Unit,
    onOpenApiSettings: () -> Unit,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status.isGranted) {
        viewModel.onLocationPermissionChanged(locationPermissionState.status.isGranted)
    }

    WeatherScreen(
        state = state,
        onRequestLocation = { locationPermissionState.launchPermissionRequest() },
        onRefresh = viewModel::refreshCurrent,
        onSelectCity = viewModel::selectCity,
        onOpenCityManager = onOpenCityManager,
        onOpenApiSettings = onOpenApiSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherScreen(
    state: WeatherScreenState,
    onRequestLocation: () -> Unit,
    onRefresh: () -> Unit,
    onSelectCity: (String) -> Unit,
    onOpenCityManager: () -> Unit,
    onOpenApiSettings: () -> Unit
) {
    val bundle = (state.weatherState as? UiState.Success<WeatherBundle>)?.value
    var lastStableBundle by remember { mutableStateOf<WeatherBundle?>(null) }
    var lastStableCondition by remember { mutableStateOf(WeatherCondition.Clear) }
    LaunchedEffect(bundle?.current?.condition) {
        bundle?.let { stable ->
            lastStableBundle = stable
            lastStableCondition = stable.current.condition
        }
    }
    val effectiveBundle = bundle ?: lastStableBundle
    val weatherCondition = effectiveBundle?.current?.condition ?: lastStableCondition
    val visual = weatherVisualStyle(weatherCondition)
    val animatedTop by animateColorAsState(targetValue = visual.top, animationSpec = tween(450), label = "top")
    val animatedMid by animateColorAsState(targetValue = visual.mid, animationSpec = tween(450), label = "mid")
    val animatedBottom by animateColorAsState(targetValue = visual.bottom, animationSpec = tween(450), label = "bottom")
    val cityName = state.cities.firstOrNull { it.id == state.selectedCityId }?.name ?: "请选择城市"
    val cityIndexMap = remember(state.cities) { state.cities.mapIndexed { index, city -> city.id to index }.toMap() }
    val selectedIndex = state.cities.indexOfFirst { it.id == state.selectedCityId }.coerceAtLeast(0)
    var showMenu by remember { mutableStateOf(false) }
    var show7DaysPage by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var dragContainerWidth by remember { mutableIntStateOf(1) }
    var isDragging by remember { mutableStateOf(false) }
    val followOffsetX by animateFloatAsState(
        targetValue = if (isDragging) dragOffsetPx else 0f,
        animationSpec = tween(if (isDragging) 50 else 220),
        label = "follow-drag"
    )
    val pulse = rememberInfiniteTransition(label = "sky")
    val tint by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4200), repeatMode = RepeatMode.Reverse),
        label = "tint"
    )
    fun moveCityPage(next: Boolean) {
        if (state.cities.isEmpty()) return
        val newIndex = if (next) {
            (selectedIndex + 1).coerceAtMost(state.cities.lastIndex)
        } else {
            (selectedIndex - 1).coerceAtLeast(0)
        }
        if (newIndex != selectedIndex) {
            onSelectCity(state.cities[newIndex].id)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            animatedTop.copy(alpha = tint),
                            animatedMid,
                            animatedBottom
                        )
                    )
                )
        ) {
            // Removed cloud mask layer to avoid top-half haze overlay.

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { dragContainerWidth = it.width.coerceAtLeast(1) }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .pointerInput(state.cities, selectedIndex) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                isDragging = true
                                dragOffsetPx = (dragOffsetPx + dragAmount)
                                    .coerceIn(-dragContainerWidth * 0.85f, dragContainerWidth * 0.85f)
                                change.consume()
                            },
                            onDragEnd = {
                                val drag = dragOffsetPx
                                if (abs(drag) > dragContainerWidth * 0.22f) {
                                    if (drag < 0f) moveCityPage(next = true) else moveCityPage(next = false)
                                }
                                dragOffsetPx = 0f
                                isDragging = false
                            },
                            onDragCancel = {
                                dragOffsetPx = 0f
                                isDragging = false
                            }
                        )
                    },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(cityName, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = onOpenCityManager) {
                                Icon(Icons.Default.Add, contentDescription = "city", tint = Color.White)
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "more", tint = Color.White)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("输入设置") },
                                    onClick = {
                                        showMenu = false
                                        onOpenApiSettings()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("刷新天气") },
                                    onClick = {
                                        showMenu = false
                                        onRefresh()
                                    }
                                )
                            }
                        }
                    }
                }

                if (!state.hasLocationPermission && state.cities.isEmpty()) {
                    item {
                        GlassCard {
                            Text("需要定位权限来自动获取当前位置天气")
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onRequestLocation) {
                                Text("授权定位")
                            }
                        }
                    }
                }

                item {
                    when (val weather = state.weatherState) {
                        UiState.Loading -> {
                            effectiveBundle?.let { HeroTemperature(it) }
                        }

                        is UiState.Error -> {
                            GlassCard {
                                Text(weather.message, color = Color.White)
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = onRefresh) { Text("重试") }
                            }
                        }

                        is UiState.Success -> {
                            HeroTemperature(weather.value)
                        }

                        UiState.Empty -> Unit
                    }
                }

                item {
                    Spacer(Modifier.height(220.dp))
                }

                item {
                    AnimatedContent(
                        modifier = Modifier.offset { IntOffset(followOffsetX.roundToInt(), 0) },
                        targetState = effectiveBundle,
                        transitionSpec = {
                            val from = cityIndexMap[initialState?.cityId] ?: 0
                            val to = cityIndexMap[targetState?.cityId] ?: 0
                            val slideLeft = to > from
                            (slideInHorizontally(animationSpec = tween(280)) { full -> if (slideLeft) full / 2 else -full / 2 } + fadeIn(animationSpec = tween(220))).togetherWith(
                                slideOutHorizontally(animationSpec = tween(260)) { full -> if (slideLeft) -full / 2 else full / 2 } + fadeOut(animationSpec = tween(220))
                            )
                        },
                        label = "forecast-slide"
                    ) { animatedBundle ->
                        animatedBundle?.let {
                            ForecastPanel(
                                daily = it.daily,
                                onOpen7Days = { show7DaysPage = true }
                            )
                        }
                    }
                }

                item {
                    AnimatedContent(
                        modifier = Modifier.offset { IntOffset(followOffsetX.roundToInt(), 0) },
                        targetState = effectiveBundle,
                        transitionSpec = {
                            val from = cityIndexMap[initialState?.cityId] ?: 0
                            val to = cityIndexMap[targetState?.cityId] ?: 0
                            val slideLeft = to > from
                            (slideInHorizontally(animationSpec = tween(300)) { full -> if (slideLeft) full / 2 else -full / 2 } + fadeIn(animationSpec = tween(220))).togetherWith(
                                slideOutHorizontally(animationSpec = tween(280)) { full -> if (slideLeft) -full / 2 else full / 2 } + fadeOut(animationSpec = tween(220))
                            )
                        },
                        label = "detail-slide"
                    ) { animatedBundle ->
                        animatedBundle?.let {
                            WeatherDetailPanel(current = it.current)
                        }
                    }
                }

                item {
                    AnimatedContent(
                        modifier = Modifier.offset { IntOffset(followOffsetX.roundToInt(), 0) },
                        targetState = effectiveBundle,
                        transitionSpec = {
                            val from = cityIndexMap[initialState?.cityId] ?: 0
                            val to = cityIndexMap[targetState?.cityId] ?: 0
                            val slideLeft = to > from
                            (slideInHorizontally(animationSpec = tween(320)) { full -> if (slideLeft) full / 2 else -full / 2 } + fadeIn(animationSpec = tween(220))).togetherWith(
                                slideOutHorizontally(animationSpec = tween(300)) { full -> if (slideLeft) -full / 2 else full / 2 } + fadeOut(animationSpec = tween(220))
                            )
                        },
                        label = "advice-slide"
                    ) { animatedBundle ->
                        animatedBundle?.let {
                            LifestyleAdvicePanel(
                                current = it.current,
                                apiLifestyle = it.lifestyle
                            )
                        }
                    }
                }

                if (state.networkWarning != null) {
                    item {
                        Text(
                            text = state.networkWarning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            if (show7DaysPage && effectiveBundle != null) {
                SevenDayForecastPage(
                    daily = effectiveBundle.daily,
                    onBack = { show7DaysPage = false }
                )
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x3309111E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
private fun HeroTemperature(bundle: WeatherBundle) {
    Column {
        Text(
            text = "${bundle.current.temperatureC.toInt()}°",
            color = Color.White,
            fontSize = 110.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 100.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${bundle.current.condition.toChinese()}  最高${bundle.daily.maxOfOrNull { it.maxTempC }?.toInt() ?: 0}° 最低${bundle.daily.minOfOrNull { it.minTempC }?.toInt() ?: 0}°",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0x3D09111E)) {
            Text(
                text = "空气良 ${(bundle.current.humidity + 12).coerceAtMost(100)}",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ForecastPanel(daily: List<DailyForecast>, onOpen7Days: () -> Unit) {
    GlassCard {
        val short = daily.take(3)
        short.forEachIndexed { index, day ->
            val dayLabel = dateLabel(index, day.dateEpochSec)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(dayLabel, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(day.condition.toChinese(), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${day.minTempC.toInt()}°", color = Color.White.copy(alpha = 0.86f))
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 6.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    )
                    Text("${day.maxTempC.toInt()}°", color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen7Days() }
        ) {
            Text(
                text = "查看近7日天气",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SevenDayForecastPage(
    daily: List<DailyForecast>,
    onBack: () -> Unit
) {
    val days = daily.take(7)
    val night = isNightNow()
    val pageBrush = if (night) {
        Brush.verticalGradient(listOf(Color(0xFF0B101C), Color(0xFF000000)))
    } else {
        Brush.verticalGradient(listOf(SunnyStart, SunnyEnd, Color(0xFFF2AA7D)))
    }
    val panelColor = if (night) Color(0xCC111A2E) else Color(0xFFF4F4F6)
    val itemColor = if (night) Color(0xFF1A2740) else Color(0xFFF0F0F2)
    val activeItemColor = if (night) Color(0xFF223558) else Color.White
    val primaryText = if (night) Color.White else Color(0xFF262626)
    val secondaryText = if (night) Color.White.copy(alpha = 0.78f) else Color(0xFF808080)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "back", tint = primaryText)
                    }
                    Text("7日天气预报", style = MaterialTheme.typography.headlineMedium, color = primaryText)
                }

                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = panelColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 360.dp)
                            .padding(10.dp)
                    ) {
                        SevenDayTrendBoard(
                            days = days,
                            night = night,
                            itemColor = itemColor,
                            activeItemColor = activeItemColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SevenDayTrendBoard(
    days: List<DailyForecast>,
    night: Boolean,
    itemColor: Color,
    activeItemColor: Color,
    primaryText: Color,
    secondaryText: Color
) {
    if (days.isEmpty()) return

    val highTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#E8C26E")
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }
    val lowTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#AFC3A0")
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    val maxTemp = days.maxOf { it.maxTempC }
    val minTemp = days.minOf { it.minTempC }
    val span = (maxTemp - minTemp).takeIf { it > 0 } ?: 1.0
    val itemWidth = 72.dp
    val boardWidth = itemWidth * days.size
    val boardHeight = 340.dp
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .width(boardWidth)
                .height(boardHeight)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val itemWidthPx = width / days.size

                val highBandTop = height * 0.47f
                val highBandBottom = height * 0.52f
                val lowBandTop = height * 0.56f
                val lowBandBottom = height * 0.61f

                fun yForHigh(temp: Double): Float {
                    val p = ((maxTemp - temp) / span).toFloat()
                    return highBandTop + p * (highBandBottom - highBandTop)
                }

                fun yForLow(temp: Double): Float {
                    val p = ((maxTemp - temp) / span).toFloat()
                    return lowBandTop + p * (lowBandBottom - lowBandTop)
                }

                val highPoints = days.mapIndexed { index, d ->
                    Offset(itemWidthPx * index + itemWidthPx / 2f, yForHigh(d.maxTempC))
                }
                val lowPoints = days.mapIndexed { index, d ->
                    Offset(itemWidthPx * index + itemWidthPx / 2f, yForLow(d.minTempC))
                }

                highPoints.zipWithNext().forEach { (a, b) ->
                    drawLine(color = Color(0xFFC59A4D), start = a, end = b, strokeWidth = 2.6f)
                }
                lowPoints.zipWithNext().forEach { (a, b) ->
                    drawLine(color = Color(0xFF9FB48F), start = a, end = b, strokeWidth = 2.6f)
                }

                highPoints.forEachIndexed { index, p ->
                    drawCircle(color = Color(0xFFE5B85A), radius = 3.4f, center = p)
                    drawContext.canvas.nativeCanvas.drawText("${days[index].maxTempC.toInt()}°", p.x, p.y - 10f, highTextPaint)
                }
                lowPoints.forEachIndexed { index, p ->
                    drawCircle(color = Color(0xFF9FB48F), radius = 3.4f, center = p)
                    drawContext.canvas.nativeCanvas.drawText("${days[index].minTempC.toInt()}°", p.x, p.y + 24f, lowTextPaint)
                }

                repeat(days.size + 1) { i ->
                    val x = itemWidthPx * i
                    drawLine(
                        color = if (night) Color.White.copy(alpha = 0.06f) else Color(0x22000000),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                days.forEachIndexed { index, day ->
                    val isActive = index == 0
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                            .background(if (isActive) activeItemColor.copy(alpha = if (night) 0.45f else 1f) else itemColor.copy(alpha = if (night) 0.22f else 0.85f))
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(dateLabel(index, day.dateEpochSec), color = primaryText, style = MaterialTheme.typography.bodyMedium)
                                Text(epochToMd(day.dateEpochSec), color = secondaryText, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(6.dp))
                                Text(conditionIcon(day.condition), fontSize = 14.sp)
                                Text(day.condition.toChinese(), color = secondaryText, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(Modifier.height(112.dp))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(day.condition.toChinese(), color = secondaryText, style = MaterialTheme.typography.bodySmall)
                                Text(conditionIcon(day.condition), fontSize = 12.sp)
                                Text(
                                    text = dayWindText(day),
                                    color = if (isActive) Color(0xFFFFB14A) else secondaryText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun dayWindText(day: DailyForecast): String {
    return when {
        !day.windScaleText.isNullOrBlank() -> "${day.windScaleText}级"
        day.windSpeedKph != null -> "${day.windSpeedKph.toInt()}km/h"
        else -> "--"
    }
}

@Composable
private fun WeatherDetailPanel(current: CurrentWeather) {
    GlassCard {
        Text("天气详情", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x2B1A2C52))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("降水预报", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (current.condition == WeatherCondition.Rain || current.condition == WeatherCondition.Thunder) "2小时内可能有降雨"
                        else "2小时内无降雨",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.radialGradient(listOf(Color(0x66A8FFCF), Color(0x334B72D8), Color.Transparent)),
                            RoundedCornerShape(12.dp)
                        )
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        MetricGrid(current)
    }
}

@Composable
private fun MetricGrid(current: CurrentWeather) {
    val items = listOf(
        Triple("紫外线", uvText(current), "☀"),
        Triple("湿度", "${current.humidity}%", "💧"),
        Triple("风速", "${current.windSpeedKph.toInt()} km/h", "〰"),
        Triple("风向", current.windDirection, "🧭"),
        Triple("体感", "${current.feelsLikeC.toInt()}°", "🌡"),
        Triple("气压", "${current.pressureHPa}", "↕")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value, icon) ->
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        title = label,
                        value = value,
                        icon = icon
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTile(modifier: Modifier, title: String, value: String, icon: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x2B1A2C52))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Text(icon)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyLarge)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LifestyleAdvicePanel(
    current: CurrentWeather,
    apiLifestyle: List<LifestyleAdvice>
) {
    val cards = buildAdviceCards(current, apiLifestyle)
    val clothing = apiLifestyle.firstOrNull { it.category.contains("穿", true) }?.detail ?: clothingAdvice(current)
    val activity = apiLifestyle.firstOrNull {
        it.category.contains("运动", true) || it.category.contains("出行", true)
    }?.detail ?: activityAdvice(current)

    GlassCard {
        Text("生活建议", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x2B1A2C52))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(item.icon)
                                Text(item.title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("穿衣提醒：$clothing", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
        Text("活动建议：$activity", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
    }
}

private data class AdviceCard(val title: String, val icon: String)

private fun buildAdviceCards(current: CurrentWeather, apiLifestyle: List<LifestyleAdvice>): List<AdviceCard> {
    if (apiLifestyle.isNotEmpty()) {
        return apiLifestyle.take(6).map {
            AdviceCard(
                title = apiAdviceTitle(it),
                icon = apiAdviceIcon(it)
            )
        }
    }

    return listOf(
        AdviceCard("适宜晨练", "🏃"),
        AdviceCard("注意防晒", if (uvText(current).startsWith("高")) "☀" else "🧴"),
        AdviceCard("宜户外活动", if (current.condition == WeatherCondition.Rain) "☂" else "🌿"),
        AdviceCard("适宜洗车", if (current.condition == WeatherCondition.Rain) "🚫" else "🚗"),
        AdviceCard("不宜晾晒", if (current.humidity > 75) "🚫" else "👕"),
        AdviceCard("空气较好", "🍃")
    )
}

private fun apiAdviceTitle(advice: LifestyleAdvice): String {
    return when {
        advice.category.contains("穿", true) -> advice.brief.ifBlank { "穿衣" }
        advice.category.contains("运", true) -> advice.brief.ifBlank { "运动" }
        advice.category.contains("洗车", true) -> advice.brief.ifBlank { "洗车" }
        advice.category.contains("雨", true) -> advice.brief.ifBlank { "降水" }
        advice.category.contains("紫外线", true) -> "紫外线${advice.brief}"
        else -> advice.category
    }
}

private fun apiAdviceIcon(advice: LifestyleAdvice): String {
    val key = advice.category + advice.brief + advice.detail
    return when {
        key.contains("穿", true) -> "👕"
        key.contains("运动", true) || key.contains("晨练", true) -> "🏃"
        key.contains("洗车", true) -> if (key.contains("不宜", true)) "🚫" else "🚗"
        key.contains("雨", true) || key.contains("伞", true) -> "☂"
        key.contains("紫外线", true) || key.contains("晒", true) -> "☀"
        key.contains("空气", true) -> "🍃"
        key.contains("风", true) -> "🍃"
        else -> "🌿"
    }
}

private fun uvText(current: CurrentWeather): String {
    return when {
        current.condition == WeatherCondition.Clear && current.temperatureC >= 30 -> "高"
        current.condition == WeatherCondition.Clear -> "中"
        else -> "弱"
    }
}

private fun isNightNow(): Boolean {
    val hour = LocalTime.now().hour
    return hour < 6 || hour >= 18
}

@Composable
private fun SevenDayChart(days: List<DailyForecast>) {
    if (days.isEmpty()) return
    val maxTemp = days.maxOf { it.maxTempC }
    val minTemp = days.minOf { it.minTempC }
    val span = (maxTemp - minTemp).takeIf { it > 0 } ?: 1.0
    val highTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#E8C26E")
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }
    val lowTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#AFC3A0")
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
    ) {
        val left = 12f
        val right = size.width - 12f
        val top = 20f
        val bottom = size.height - 20f
        val chartHeight = bottom - top
        val step = if (days.size == 1) 0f else (right - left) / (days.size - 1)

        fun yFor(temp: Double): Float {
            return top + ((maxTemp - temp) / span).toFloat() * chartHeight
        }

        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(left, (top + bottom) / 2f),
            end = Offset(right, (top + bottom) / 2f),
            strokeWidth = 1.2f
        )

        val highPoints = days.mapIndexed { index, d ->
            Offset(left + index * step, yFor(d.maxTempC))
        }
        val lowPoints = days.mapIndexed { index, d ->
            Offset(left + index * step, yFor(d.minTempC))
        }

        highPoints.zipWithNext().forEach { (a, b) ->
            drawLine(color = Color(0xFFC59A4D), start = a, end = b, strokeWidth = 2.6f)
        }
        lowPoints.zipWithNext().forEach { (a, b) ->
            drawLine(color = Color(0xFF9FB48F), start = a, end = b, strokeWidth = 2.6f)
        }

        highPoints.forEachIndexed { index, p ->
            drawCircle(color = Color(0xFFE5B85A), radius = 3.8f, center = p)
            drawContext.canvas.nativeCanvas.drawText("${days[index].maxTempC.toInt()}°", p.x, p.y - 10f, highTextPaint)
        }

        lowPoints.forEachIndexed { index, p ->
            drawCircle(color = Color(0xFF9FB48F), radius = 3.8f, center = p)
            drawContext.canvas.nativeCanvas.drawText("${days[index].minTempC.toInt()}°", p.x, p.y + 24f, lowTextPaint)
        }
    }
}

private fun epochToMd(epochSec: Long): String {
    val zdt = Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault())
    return "${zdt.monthValue}-${zdt.dayOfMonth}"
}

private fun conditionIcon(condition: WeatherCondition): String {
    return when (condition) {
        WeatherCondition.Clear -> "☀"
        WeatherCondition.Cloudy -> "☁"
        WeatherCondition.Rain -> "🌧"
        WeatherCondition.Snow -> "❄"
        WeatherCondition.Thunder -> "⛈"
        WeatherCondition.Fog -> "🌫"
        WeatherCondition.Wind -> "🍃"
        WeatherCondition.Haze -> "🌫"
        WeatherCondition.Unknown -> "☁"
    }
}

private fun clothingAdvice(current: CurrentWeather): String {
    val t = current.temperatureC
    return when {
        t >= 30 -> "天气较热，建议短袖短裤，注意防晒补水。"
        t >= 22 -> "体感舒适，建议短袖或薄外套。"
        t >= 15 -> "早晚偏凉，建议长袖加薄外套。"
        t >= 8 -> "气温较低，建议外套或卫衣，注意保暖。"
        else -> "天气寒冷，建议厚外套/羽绒服，注意防风保暖。"
    }
}

private fun activityAdvice(current: CurrentWeather): String {
    val rainLike = current.condition == WeatherCondition.Rain || current.condition == WeatherCondition.Thunder
    return when {
        rainLike -> "建议室内活动，外出请带伞并注意路滑。"
        current.windSpeedKph >= 28 -> "风力较大，户外活动注意防风，避免高空或水边项目。"
        current.temperatureC in 18.0..28.0 -> "适合散步、慢跑等轻量户外活动。"
        current.temperatureC > 28.0 -> "适合早晚时段活动，避免午后暴晒。"
        else -> "适合短时户外活动，注意添衣保暖。"
    }
}

private fun dateLabel(index: Int, epochSec: Long): String {
    if (index == 0) return "今天"
    if (index == 1) return "明天"
    val dayOfWeek = Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault()).dayOfWeek
    return when (dayOfWeek.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        7 -> "周日"
        else -> "未来"
    }
}

private fun WeatherCondition.toChinese(): String {
    return when (this) {
        WeatherCondition.Clear -> "晴"
        WeatherCondition.Cloudy -> "多云"
        WeatherCondition.Rain -> "雨"
        WeatherCondition.Snow -> "雪"
        WeatherCondition.Thunder -> "雷暴"
        WeatherCondition.Fog -> "雾"
        WeatherCondition.Wind -> "大风"
        WeatherCondition.Haze -> "霾"
        WeatherCondition.Unknown -> "未知"
    }
}

private data class WeatherVisualStyle(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val cloudAlpha: Float = 0.2f
)

private fun weatherVisualStyle(condition: WeatherCondition): WeatherVisualStyle {
    return when (condition) {
        WeatherCondition.Clear -> WeatherVisualStyle(
            top = SunnyStart,
            mid = SunnyEnd,
            bottom = Color(0xFFF2B287),
            cloudAlpha = 0.16f
        )

        WeatherCondition.Cloudy -> WeatherVisualStyle(
            top = Color(0xFF87A7D3),
            mid = Color(0xFF9CB3CE),
            bottom = Color(0xFFBFAFAD),
            cloudAlpha = 0.3f
        )

        WeatherCondition.Rain, WeatherCondition.Thunder -> WeatherVisualStyle(
            top = RainyStart,
            mid = Color(0xFF3E5877),
            bottom = Color(0xFF2A3547),
            cloudAlpha = 0.38f
        )

        WeatherCondition.Snow -> WeatherVisualStyle(
            top = Color(0xFFC7DAF0),
            mid = Color(0xFFEAF2FA),
            bottom = Color(0xFFDCE8F3),
            cloudAlpha = 0.24f
        )

        WeatherCondition.Fog, WeatherCondition.Haze -> WeatherVisualStyle(
            top = Color(0xFF95A2AF),
            mid = Color(0xFFAEB8C1),
            bottom = Color(0xFFC2B9AB),
            cloudAlpha = 0.35f
        )

        WeatherCondition.Wind -> WeatherVisualStyle(
            top = Color(0xFF8DB0CC),
            mid = Color(0xFFBFD1DC),
            bottom = Color(0xFFDABEA5),
            cloudAlpha = 0.2f
        )

        WeatherCondition.Unknown -> WeatherVisualStyle(
            top = SunnyStart,
            mid = SunnyEnd,
            bottom = Color(0xFFF2B287),
            cloudAlpha = 0.2f
        )
    }
}
