# 天气 APP 实现方案（plan）

## 0. 目标与范围

基于 `天气软件需求分析.md`，首期交付一款 Android 极简天气应用（对标 iOS 风格体验），满足：

- 实时天气 + 24 小时 + 7~15 天预报
- 定位、城市搜索、多城市管理、城市间滑动切换
- 可切换的天气数据源（统一模型 + 适配器）
- 动态背景 + 毛玻璃卡片 + 流畅交互
- 离线缓存、弱网容错、权限拒绝兜底

首期优先 Android（Kotlin），UI 技术默认 Jetpack Compose，兼顾低端机性能降级。

---

## 1. 分步骤实现思路（里程碑式）

### 阶段 1：项目初始化与基础架构（第 1 周）

1. 创建 Android 工程与基础模块
   - `app`（展示层）
   - `core/network`（网络层）
   - `core/database`（缓存层）
   - `core/location`（定位能力）
   - `feature/weather`（天气页）
   - `feature/city`（城市搜索与管理）

2. 搭建分层架构
   - 架构：MVVM + Repository + UseCase（可选）
   - 分层：UI -> ViewModel -> Domain -> Data（Remote/Local）
   - 统一状态管理：`UiState(Loading/Success/Error/Empty)`

3. 建立统一天气领域模型（关键）
   - Domain Model：`CurrentWeather`、`HourlyForecast`、`DailyForecast`、`AirQuality`、`City`、`WeatherCondition`
   - 先不绑定具体 API 字段，避免后续更换数据源时影响上层

验收标准：
- App 可启动，基础导航可运行
- 数据分层清晰，已定义统一天气模型

### 阶段 2：天气 API 适配层与数据链路打通（第 1~2 周）

1. 设计 Provider 适配器接口
   - `WeatherProvider`：`getCurrent()`、`getHourly()`、`getDaily()`、`searchCity()`
   - Provider 实现：`QWeatherProvider`、`OpenWeatherProvider`（首批）

2. 建立 Provider 选择策略
   - 策略示例：
     - 国内优先 QWeather
     - 海外优先 OpenWeather
     - 失败自动 fallback 到备用 provider
   - 配置化：支持远程开关或本地配置切换

3. 接入缓存与容错
   - Room 缓存最新成功天气数据（按城市维度）
   - 网络失败返回缓存并标记“数据可能非实时”
   - 统一错误码映射（超时、鉴权失败、配额超限、无网）

验收标准：
- 两套 API 均可成功拉取并映射到统一模型
- 弱网/无网时可展示上次缓存数据

### 阶段 3：定位与城市管理能力（第 2 周）

1. 定位能力
   - 使用 FusedLocationProvider 获取经纬度
   - 逆地理编码获取城市名（可用 Geocoder 或 API）
   - 权限拒绝时跳转到“手动添加城市”流程

2. 城市搜索与联想
   - 输入防抖（300~500ms）
   - 支持中文/拼音/英文关键词
   - 结果去重与本地历史优先

3. 城市列表管理
   - 添加多个城市
   - 长按拖拽排序
   - 左滑删除
   - 主页左右滑动切换城市详情（Pager）

验收标准：
- 首次授权可自动定位并展示天气
- 无定位权限时可完整通过搜索使用 app
- 多城市管理交互稳定可用

### 阶段 4：核心 UI/UX 还原与动画体验（第 3 周）

1. 页面结构
   - 顶部：城市名、当前温度、体感、天气概述
   - 中部：24 小时横向卡片
   - 底部：7~15 天列表卡片 + 空气质量/附加指标

2. 视觉体系
   - 毛玻璃卡片（半透明 + blur + 圆角 + 描边）
   - 背景按天气 + 昼夜变化（晴/雨/雪/雷）
   - 深色模式配色与对比度校验

3. 交互动效
   - 下拉刷新阻尼
   - 页面切换过渡动画
   - 关键操作触觉反馈（Haptic）

4. 低端机降级策略
   - 动态背景降级为静态图
   - 动效减帧或关闭粒子效果

验收标准：
- 主界面完整呈现设计风格
- 中高端机 60fps 接近稳定；低端机可降级后流畅

### 阶段 5：性能优化、测试与发布准备（第 4 周）

1. 性能优化
   - 启动优化：延迟初始化非关键组件
   - 网络优化：并发请求合并、缓存策略、超时重试
   - Compose 重组优化：稳定参数、列表 key、避免无效重绘

2. 质量保障
   - 单元测试：模型映射、Repository、Provider fallback
   - UI 测试：核心流程（定位、搜索、城市切换）
   - 异常场景回归：断网、权限拒绝、API 限流

3. 发布准备
   - 隐私权限说明
   - Crash/性能监控（Firebase Crashlytics + Performance）
   - Beta 版本灰度

验收标准：
- 冷启动目标 <= 2s（主流设备）
- 核心流程测试通过，具备首发上线条件

---

## 2. 关键技术栈

### 2.1 Android 基础

- 语言：Kotlin
- 最低版本建议：Android 8.0+（API 26）
- 架构：MVVM + Repository
- DI：Hilt（降低模块耦合）

### 2.2 UI 与交互

- Jetpack Compose（主推）
  - Material 3 + 自定义主题系统
  - Navigation Compose
  - Pager + LazyRow/LazyColumn
- 动画：Compose Animation + Lottie（可选，做天气特效）
- 图片：Coil

### 2.3 网络与数据

- Retrofit2 + OkHttp
- Kotlinx Serialization 或 Moshi（二选一）
- 协程：Kotlin Coroutines + Flow
- 缓存：Room + DataStore（轻配置）

### 2.4 定位与系统能力

- Google Play Services Location（FusedLocationProvider）
- 权限处理：Accompanist Permissions（可选）
- Haptic 反馈：Android Vibrator / Compose 接口

### 2.5 工程与质量

- 日志：Timber
- 崩溃监控：Firebase Crashlytics
- 测试：JUnit + MockK + Turbine + Compose UI Test
- CI：GitHub Actions（构建 + 单测 + lint）

---

## 3. 不同方案的取舍对比

### 3.1 UI 技术：Jetpack Compose vs XML(View)

1. Jetpack Compose
   - 优点：声明式开发效率高；动画和状态管理更自然；卡片化、动态 UI 更容易实现
   - 缺点：复杂场景重组优化有学习成本；部分组件生态仍在演进

2. XML + View
   - 优点：成熟稳定；历史项目兼容好
   - 缺点：复杂动画和响应式布局成本高；样板代码更多

结论：首选 Compose。该项目强调动态视觉与流畅交互，Compose 更匹配。

### 3.2 架构：MVVM vs MVI

1. MVVM
   - 优点：团队普及度高，上手快；与 Android 生态（ViewModel/LiveData/Flow）契合
   - 缺点：状态边界若管理不严，后期容易复杂化

2. MVI
   - 优点：单向数据流，状态可预测，调试友好
   - 缺点：模板代码偏多，小团队初期迭代速度可能下降

结论：首期采用 MVVM，并在 ViewModel 层引入单一 `UiState`，兼顾效率与可维护性。

### 3.3 API 策略：单一 Provider vs 多 Provider 适配器

1. 单一 Provider
   - 优点：接入快，维护成本低
   - 缺点：受制于供应商稳定性、地区覆盖和配额价格

2. 多 Provider + Adapter（推荐）
   - 优点：可按地区/成本/稳定性切换；容灾能力强
   - 缺点：映射层与测试成本更高

结论：采用多 Provider 适配器，是本项目的核心竞争力之一。

### 3.4 本地缓存：仅内存缓存 vs Room 持久化

1. 仅内存缓存
   - 优点：实现简单
   - 缺点：应用重启后丢失，无法满足离线可用

2. Room 持久化（推荐）
   - 优点：支持无网展示上次数据；可记录多城市历史
   - 缺点：需要 schema 维护

结论：使用 Room，满足非功能需求中的离线容错。

### 3.5 动态背景：实时粒子渲染 vs 预渲染资源

1. 实时粒子渲染
   - 优点：沉浸感强，可做高质量天气效果
   - 缺点：耗电与性能压力大

2. 预渲染视频/GIF/Lottie
   - 优点：开发快，效果稳定
   - 缺点：资源体积可能较大，定制灵活度有限

3. 混合策略（推荐）
   - 中高端机：实时或高质量动画
   - 低端机：静态图或轻量动画

结论：采用混合策略，确保体验与性能平衡。

---

## 4. 关键技术决策（最终建议）

- UI：Jetpack Compose
- 架构：MVVM + Repository + 统一 `UiState`
- 数据源：多 API Provider（QWeather + OpenWeather 起步）
- 容错：Room 持久化缓存 + fallback provider
- 性能：动态背景可降级 + 启动与重组优化

---

## 5. 风险与预案

- API 配额/稳定性风险：引入多 provider + 熔断/降级
- 定位权限拒绝：引导手动搜索并保存默认城市
- 低端机掉帧：动态特效分级、关闭高成本动画
- 数据口径差异：在 Domain 层定义统一枚举与单位换算策略

---

## 6. 交付节奏建议（4 周）

- 第 1 周：架构 + 统一模型 + 首个 provider
- 第 2 周：双 provider + 定位 + 城市管理
- 第 3 周：完整 UI/UX + 动效 + 深色模式
- 第 4 周：性能优化 + 测试回归 + Beta 发布

该计划可支持后续快速扩展：空气质量、分钟级降雨、天气预警、桌面小组件、Wear OS 等。

---

## 7. 性能与UI优化详细计划

### 7.1 性能优化方案

#### 7.1.1 启动优化（目标：冷启动 ≤ 1.5s）

**当前问题分析**：
- Hilt 依赖注入在 Application 阶段初始化所有模块
- Retrofit 和 Room 数据库在启动时同步创建
- 首屏天气数据需要等待网络请求完成

**优化方案**：

1. **延迟初始化（Deferred Initialization）**
```kotlin
// 在 AppModule 中使用 @LazySingleton
@Provides
@LazySingleton
fun provideQWeatherApi(@QWeatherRetrofit retrofit: Lazy<Retrofit>): QWeatherApi {
    return retrofit.get().create(QWeatherApi::class.java)
}
```

2. **异步数据库初始化**
```kotlin
// DatabaseModule 中使用 Room.databaseBuilder 的 allowMainThreadQueries() 替代方案
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): WeatherDatabase {
    return Room.databaseBuilder(context, WeatherDatabase::class.java, "weather_glass.db")
        .setQueryExecutor(Dispatchers.IO.asExecutor())
        .setTransactionExecutor(Dispatchers.IO.asExecutor())
        .build()
}
```

3. **启动画面优化**
```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 设置启动主题
        setTheme(R.style.Theme_WeatherGlass_Splash)
        super.onCreate(savedInstanceState)
        // 预加载首屏数据
        lifecycleScope.launch {
            viewModel.preloadInitialWeather()
        }
    }
}
```

**验收标准**：
- Pixel 6 冷启动 ≤ 1.2s
- 小米 10 冷启动 ≤ 1.5s
- 低端机（Redmi Note 9）冷启动 ≤ 2.0s

#### 7.1.2 网络优化

**当前问题分析**：
- 多个 API 请求串行执行（当前天气、小时预报、日预报、生活指数）
- 没有请求合并和去重机制
- 缓存过期策略不明确

**优化方案**：

1. **并发请求合并**
```kotlin
// WeatherProvider.kt
override suspend fun getWeather(lat: Double, lon: Double, cityId: String): WeatherBundle {
    return coroutineScope {
        val currentDeferred = async { api.current(location, key) }
        val hourlyDeferred = async { api.hourly(location, key) }
        val dailyDeferred = async { api.daily(location, key) }
        val indicesDeferred = async { 
            runCatching { api.indices(location = location, key = key) }.getOrNull() 
        }
        
        // 等待所有请求完成
        val current = currentDeferred.await()
        val hourly = hourlyDeferred.await()
        val daily = dailyDeferred.await()
        val indices = indicesDeferred.await()
        
        // 合并结果
        WeatherBundle(...)
    }
}
```

2. **请求去重（Request Deduplication）**
```kotlin
// WeatherRepositoryImpl.kt
private val pendingRequests = ConcurrentHashMap<String, Deferred<AppResult<WeatherBundle>>>()

override suspend fun fetchWeather(city: City): AppResult<WeatherBundle> {
    val key = "${city.id}-${System.currentTimeMillis() / 60000}" // 1分钟粒度去重
    
    return pendingRequests.getOrPut(key) {
        viewModelScope.async(Dispatchers.IO) {
            try {
                fetchWeatherInternal(city)
            } finally {
                pendingRequests.remove(key)
            }
        }
    }.await()
}
```

3. **缓存过期策略**
```kotlin
// WeatherDao.kt
@Query("""
    SELECT * FROM weather_cache 
    WHERE cityId = :cityId 
    AND updatedAtEpochSec > :minTimestamp
""")
suspend fun getValidCache(cityId: String, minTimestamp: Long): WeatherCacheEntity?

// WeatherRepositoryImpl.kt
private fun isCacheValid(cache: WeatherCacheEntity): Boolean {
    val maxAge = when {
        // 5分钟内的缓存认为有效
        System.currentTimeMillis() / 1000 - cache.updatedAtEpochSec < 300 -> true
        else -> false
    }
    return maxAge
}
```

4. **OkHttp 缓存配置**
```kotlin
// NetworkModule.kt
@Provides
@Singleton
fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
    val cache = Cache(File(context.cacheDir, "http_cache"), 10 * 1024 * 1024) // 10MB
    
    return OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Cache-Control", "public, max-age=300") // 5分钟缓存
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}
```

**验收标准**：
- 首次加载时间 ≤ 2s（4G 网络）
- 重复请求响应时间 ≤ 200ms（缓存命中）
- 离线模式启动时间 ≤ 500ms

#### 7.1.3 Compose 重组优化

**当前问题分析**：
- WeatherRoute.kt 中存在大量状态提升，可能导致过度重组
- LazyColumn 中没有使用 key 参数
- 部分 Composable 函数参数不稳定

**优化方案**：

1. **稳定参数优化**
```kotlin
// 使用 @Stable 注解标记数据类
@Stable
data class WeatherScreenState(
    val weatherState: UiState<WeatherBundle> = UiState.Loading,
    val cities: ImmutableList<City> = persistentListOf(), // 使用不可变列表
    val selectedCityId: String? = null,
    val weatherByCityId: PersistentMap<String, WeatherBundle> = persistentMapOf(),
    val hasLocationPermission: Boolean = false,
    val networkWarning: String? = null
)

// 使用 derivedStateOf 计算派生状态
val cityName by remember {
    derivedStateOf {
        state.cities.firstOrNull { it.id == state.selectedCityId }?.name ?: "请选择城市"
    }
}
```

2. **LazyColumn key 优化**
```kotlin
// WeatherRoute.kt
LazyColumn {
    items(
        items = state.cities,
        key = { it.id } // 明确指定 key
    ) { city ->
        CityWeatherCard(city = city)
    }
}
```

3. **避免不必要重组**
```kotlin
// 使用 remember 缓存计算结果
val visual = remember(weatherCondition) {
    weatherVisualStyle(weatherCondition)
}

// 使用 LaunchedEffect 替代副作用
LaunchedEffect(bundle?.current?.condition) {
    bundle?.let { stable ->
        lastStableBundle = stable
        lastStableCondition = stable.current.condition
    }
}
```

4. **重组性能监控**
```kotlin
// 添加重组计数器（仅 Debug 模式）
if (BuildConfig.DEBUG) {
    SideEffect {
        recomposeCounter++
        Log.d("Recompose", "WeatherScreen recomposed: $recomposeCounter")
    }
}
```

**验收标准**：
- 主页面滑动时重组次数 ≤ 5次/秒
- 列表滚动帧率 ≥ 55fps（中端设备）
- 内存占用 ≤ 150MB（正常使用场景）

#### 7.1.4 数据库优化

**当前问题分析**：
- 城市去重算法使用内存计算，大量城市时性能差
- 没有数据库索引
- JSON 存储整个 WeatherBundle，查询效率低

**优化方案**：

1. **数据库索引**
```kotlin
// CityEntity.kt
@Entity(
    tableName = "cities",
    indices = [
        Index(value = ["name"]),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["sortOrder"])
    ]
)
data class CityEntity(...)

// WeatherCacheEntity.kt
@Entity(
    tableName = "weather_cache",
    indices = [Index(value = ["updatedAtEpochSec"])]
)
data class WeatherCacheEntity(...)
```

2. **城市去重优化（数据库层）**
```kotlin
// CityDao.kt
@Query("""
    SELECT * FROM cities 
    WHERE LOWER(name) = LOWER(:name) 
    AND ABS(latitude - :lat) <= 0.12 
    AND ABS(longitude - :lon) <= 0.12
    LIMIT 1
""")
suspend fun findDuplicate(name: String, lat: Double, lon: Double): CityEntity?

// 在 Repository 中使用数据库查询替代内存计算
override suspend fun cacheCity(city: City): City {
    val duplicate = cityDao.findDuplicate(city.name, city.latitude, city.longitude)
    // ... 处理逻辑
}
```

3. **分页加载**
```kotlin
// CityDao.kt
@Query("SELECT * FROM cities ORDER BY sortOrder ASC LIMIT :limit OFFSET :offset")
suspend fun getCities(limit: Int, offset: Int): List<CityEntity>

// 支持 Flow 分页
@Query("SELECT * FROM cities ORDER BY sortOrder ASC")
fun observeCitiesPaging(): PagingSource<Int, CityEntity>
```

**验收标准**：
- 100个城市加载时间 ≤ 100ms
- 城市搜索响应时间 ≤ 50ms
- 数据库文件大小 ≤ 5MB（100个城市 + 30天缓存）

### 7.2 UI/UX 优化方案

#### 7.2.1 动态天气背景增强

**当前实现**：
- 静态渐变背景，根据天气条件切换颜色
- 简单的呼吸动画（alpha 变化）

**优化方案**：

1. **天气粒子效果（高端设备）**
```kotlin
// 新增 WeatherParticles.kt
@Composable
fun RainParticles(intensity: Float) {
    val particles = remember { List(50) { RainParticle.random() } }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(particle.x, particle.y),
                end = Offset(particle.x + particle.length, particle.y + particle.length * 2),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
    
    // 动画更新
    LaunchedEffect(Unit) {
        while (isActive) {
            particles.forEach { it.update() }
            delay(16) // 60fps
        }
    }
}

data class RainParticle(
    var x: Float,
    var y: Float,
    val speed: Float,
    val length: Float
) {
    fun update() {
        y += speed
        if (y > maxHeight) {
            y = -length
            x = Random.nextFloat() * maxWidth
        }
    }
}
```

2. **云层动画**
```kotlin
@Composable
fun CloudLayer(condition: WeatherCondition) {
    val clouds = remember { List(3) { Cloud.random() } }
    val infiniteTransition = rememberInfiniteTransition()
    
    clouds.forEachIndexed { index, cloud ->
        val offsetX by infiniteTransition.animateFloat(
            initialValue = cloud.startX,
            targetValue = cloud.endX,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = cloud.duration, easing = LinearEasing)
            )
        )
        
        Image(
            painter = painterResource(id = cloud.resource),
            contentDescription = null,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), cloud.y.roundToInt()) }
                .alpha(cloud.alpha)
        )
    }
}
```

3. **性能分级策略**
```kotlin
// DevicePerformanceTier.kt
enum class PerformanceTier {
    HIGH, MEDIUM, LOW
}

object DevicePerformanceEvaluator {
    fun evaluate(context: Context): PerformanceTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return when {
            memoryInfo.totalMem >= 6 * 1024 * 1024 * 1024 -> PerformanceTier.HIGH  // 6GB+
            memoryInfo.totalMem >= 4 * 1024 * 1024 * 1024 -> PerformanceTier.MEDIUM // 4GB+
            else -> PerformanceTier.LOW
        }
    }
}

// 在 WeatherScreen 中使用
val performanceTier = remember { DevicePerformanceEvaluator.evaluate(context) }
when (performanceTier) {
    PerformanceTier.HIGH -> {
        RainParticles(intensity = 0.8f)
        CloudLayer(condition = weatherCondition)
    }
    PerformanceTier.MEDIUM -> {
        CloudLayer(condition = weatherCondition)
    }
    PerformanceTier.LOW -> {
        // 只显示静态背景
    }
}
```

**验收标准**：
- 高端设备（Pixel 7）：60fps 粒子效果
- 中端设备（小米 12）：60fps 云层动画
- 低端设备（Redmi Note 9）：静态背景无卡顿

#### 7.2.2 交互动效优化

**当前实现**：
- 城市滑动切换（基础实现）
- 7日预报页面淡入淡出

**优化方案**：

1. **下拉刷新动效**
```kotlin
// PullRefresh 实现
@Composable
fun WeatherScreenWithPullRefresh(
    state: WeatherScreenState,
    onRefresh: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = onRefresh
    )
    
    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        WeatherScreen(state = state)
        
        PullRefreshIndicator(
            refreshing = state.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Color.White,
            backgroundColor = Color(0x3309111E)
        )
    }
}
```

2. **城市切换动画增强**
```kotlin
// 使用 AnimatedContent 替代手动偏移
@Composable
fun CityWeatherTransition(
    selectedCity: City,
    weatherBundle: WeatherBundle?
) {
    AnimatedContent(
        targetState = selectedCity.id,
        transitionSpec = {
            val direction = if (targetState > initialState) {
                // 向左滑动
                slideInHorizontally { it } with slideOutHorizontally { -it }
            } else {
                // 向右滑动
                slideInHorizontally { -it } with slideOutHorizontally { it }
            }
            direction.using(SizeTransform(clip = false))
        }
    ) { cityId ->
        weatherBundle?.let { HeroTemperature(it) }
    }
}
```

3. **卡片展开动画**
```kotlin
@Composable
fun ExpandableWeatherCard(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize() // 自动动画
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            
            AnimatedVisibility(visible = expanded) {
                content()
            }
        }
    }
}
```

4. **触觉反馈**
```kotlin
// 在关键操作时添加触觉反馈
val hapticFeedback = LocalHapticFeedback.current

IconButton(
    onClick = {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        onOpenCityManager()
    }
) {
    Icon(Icons.Default.Add, contentDescription = "添加城市")
}
```

**验收标准**：
- 下拉刷新动画流畅度 ≥ 55fps
- 城市切换动画时长 300ms，无卡顿
- 卡片展开/折叠动画时长 250ms

#### 7.2.3 信息展示优化

**当前问题**：
- 7日预报图表使用自定义 Canvas，复杂度高
- 生活建议卡片布局固定，不够灵活
- 温度趋势图缺少交互

**优化方案**：

1. **7日预报图表重构**
```kotlin
// 使用 Vico 图表库（更高效）
@Composable
fun SevenDayChartV2(days: List<DailyForecast>) {
    val chartEntryModelProducer = remember {
        ChartEntryModelProducer(
            days.mapIndexed { index, day ->
                entryOf(index.toFloat(), day.maxTempC.toFloat())
            }
        )
    }
    
    Chart(
        chart = lineChart(),
        model = chartEntryModelProducer.getModel(),
        startAxis = startAxis(),
        bottomAxis = bottomAxis(),
        marker = rememberMarker()
    )
}

// 或者优化现有 Canvas 实现
@Composable
fun OptimizedSevenDayChart(days: List<DailyForecast>) {
    // 使用 remember 缓存计算结果
    val chartData = remember(days) {
        val maxTemp = days.maxOf { it.maxTempC }
        val minTemp = days.minOf { it.minTempC }
        val span = (maxTemp - minTemp).takeIf { it > 0 } ?: 1.0
        
        ChartData(
            highPoints = days.mapIndexed { index, d ->
                Offset(index.toFloat(), d.maxTempC.toFloat())
            },
            lowPoints = days.mapIndexed { index, d ->
                Offset(index.toFloat(), d.minTempC.toFloat())
            },
            maxTemp = maxTemp,
            minTemp = minTemp,
            span = span
        )
    }
    
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        // 使用缓存的数据绘制，避免重复计算
        drawChart(chartData)
    }
}
```

2. **生活建议卡片网格优化**
```kotlin
@Composable
fun LifestyleAdviceGrid(
    advices: List<LifestyleAdvice>,
    columns: Int = 3
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = advices,
            key = { it.category + it.brief } // 唯一 key
        ) { advice ->
            AdviceCard(advice = advice)
        }
    }
}
```

3. **温度趋势交互**
```kotlin
@Composable
fun InteractiveTemperatureChart(
    days: List<DailyForecast>,
    onDaySelected: (Int) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val index = (offset.x / (size.width / days.size)).toInt()
                    selectedIndex = index.coerceIn(0, days.lastIndex)
                    onDaySelected(selectedIndex)
                }
            }
    ) {
        // 绘制图表
        drawChart(days, selectedIndex)
    }
}
```

**验收标准**：
- 7日预报图表渲染时间 ≤ 16ms（单帧）
- 生活建议卡片滚动流畅度 ≥ 60fps
- 温度趋势图交互响应时间 ≤ 100ms

#### 7.2.4 深色模式优化

**当前实现**：
- 基础深色模式支持
- 7日预报页面有夜间/白天切换

**优化方案**：

1. **动态颜色适配**
```kotlin
// Theme.kt
@Composable
fun WeatherGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Android 12+ 动态颜色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

2. **对比度优化**
```kotlin
// 确保文本对比度符合 WCAG 2.1 AA 标准
val primaryText = if (night) {
    Color.White // 对比度 21:1
} else {
    Color.Black // 对比度 21:1
}

val secondaryText = if (night) {
    Color.White.copy(alpha = 0.78f) // 对比度 7.5:1
} else {
    Color.Black.copy(alpha = 0.78f) // 对比度 7.5:1
}
```

3. **深色模式背景优化**
```kotlin
// 为深色模式设计专门的渐变
private fun darkModeBackground(condition: WeatherCondition): Brush {
    return when (condition) {
        WeatherCondition.Clear -> Brush.verticalGradient(
            listOf(Color(0xFF0B101C), Color(0xFF1A2740), Color(0xFF0B101C))
        )
        WeatherCondition.Rain -> Brush.verticalGradient(
            listOf(Color(0xFF0A0F1A), Color(0xFF1A2740), Color(0xFF0A0F1A))
        )
        // ...
    }
}
```

**验收标准**：
- 所有文本对比度 ≥ 4.5:1（WCAG AA 标准）
- 深色模式下无刺眼高亮
- 动态颜色支持 Android 12+ 设备

### 7.3 优化实施计划

#### 第一阶段：性能基准测试（第 1 周）

1. **建立性能监控**
   - 集成 Firebase Performance Monitoring
   - 添加自定义 Trace（启动时间、API 响应时间、UI 渲染时间）
   - 建立性能基准数据

2. **性能分析工具使用**
   - Android Studio Profiler 分析 CPU/内存/网络
   - Compose Layout Inspector 分析重组次数
   - Macrobenchmark 测试启动性能

3. **确定优化优先级**
   - 根据基准数据确定最需要优化的环节
   - 制定详细的优化任务清单

#### 第二阶段：核心性能优化（第 2 周）

1. **启动优化**
   - 实现延迟初始化
   - 优化 Application.onCreate()
   - 添加启动 Trace 监控

2. **网络优化**
   - 实现并发请求
   - 添加请求去重
   - 配置 OkHttp 缓存

3. **数据库优化**
   - 添加数据库索引
   - 优化城市去重算法
   - 实现分页加载

#### 第三阶段：UI/UX 优化（第 3 周）

1. **动效优化**
   - 实现下拉刷新动效
   - 优化城市切换动画
   - 添加触觉反馈

2. **信息展示优化**
   - 重构 7日预报图表
   - 优化生活建议布局
   - 添加温度趋势交互

3. **深色模式优化**
   - 实现动态颜色支持
   - 优化对比度
   - 测试不同设备表现

#### 第四阶段：测试与调优（第 4 周）

1. **性能测试**
   - 在不同设备上测试性能
   - 使用 Macrobenchmark 进行自动化测试
   - 收集真实用户数据

2. **兼容性测试**
   - 测试不同 Android 版本（8.0-14）
   - 测试不同屏幕尺寸
   - 测试不同性能等级设备

3. **最终调优**
   - 根据测试结果进行微调
   - 优化内存占用
   - 确保所有优化目标达成

### 7.4 性能监控指标

#### 关键性能指标（KPI）

1. **启动性能**
   - 冷启动时间 ≤ 1.5s（中端设备）
   - 热启动时间 ≤ 0.5s
   - 首屏渲染时间 ≤ 1.0s

2. **运行时性能**
   - 主线程阻塞时间 ≤ 16ms/帧
   - 内存占用 ≤ 150MB（正常使用）
   - 电池消耗 ≤ 5%/小时（持续使用）

3. **网络性能**
   - API 响应时间 ≤ 2s（4G 网络）
   - 缓存命中率 ≥ 80%
   - 请求失败率 ≤ 1%

4. **UI 性能**
   - 列表滚动帧率 ≥ 55fps
   - 动画流畅度 ≥ 55fps
   - 重组频率 ≤ 5次/秒

#### 监控工具

1. **Firebase Performance Monitoring**
   - 自定义 Trace 监控关键操作
   - 网络请求监控
   - 屏幕渲染监控

2. **Android Vitals**
   - 崩溃率监控
   - ANR 监控
   - 启动性能监控

3. **自定义监控**
```kotlin
// PerformanceMonitor.kt
object PerformanceMonitor {
    private val traces = mutableMapOf<String, Trace>()
    
    fun startTrace(name: String) {
        val trace = Firebase.performance.newTrace(name)
        trace.start()
        traces[name] = trace
    }
    
    fun stopTrace(name: String) {
        traces[name]?.stop()
        traces.remove(name)
    }
    
    fun recordMetric(name: String, value: Long) {
        traces[name]?.putMetric(name, value)
    }
}

// 使用示例
PerformanceMonitor.startTrace("weather_fetch")
try {
    val weather = repository.fetchWeather(city)
} finally {
    PerformanceMonitor.stopTrace("weather_fetch")
}
```

### 7.5 风险与应对

#### 性能优化风险

1. **过度优化风险**
   - 风险：过度优化可能导致代码复杂度增加，维护成本上升
   - 应对：遵循 80/20 原则，优先优化最关键的 20% 代码

2. **兼容性风险**
   - 风险：某些优化可能在特定设备或系统版本上不兼容
   - 应对：添加设备/版本检查，提供降级方案

3. **测试覆盖风险**
   - 风险：性能优化可能引入新的 bug
   - 应对：增加性能测试用例，进行充分的回归测试

#### UI 优化风险

1. **设计一致性风险**
   - 风险：优化后的 UI 可能与原有设计风格不一致
   - 应对：建立设计规范，确保所有优化符合整体风格

2. **用户习惯风险**
   - 风险：大幅改变 UI 可能影响用户习惯
   - 应对：渐进式优化，收集用户反馈，提供设置选项

3. **性能与效果平衡风险**
   - 风险：追求炫酷效果可能导致性能下降
   - 应对：实施性能分级策略，确保在所有设备上流畅运行
