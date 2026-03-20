# WeatherGlass 天气应用深度研究报告

## 1. 项目概述

WeatherGlass 是一个基于 Kotlin + Jetpack Compose 的 Android 天气应用，支持多数据源、城市管理、动态天气背景和生活建议。项目采用现代化 Android 开发技术栈，实现了类似 iOS 原生天气应用的沉浸式体验。

### 1.1 核心功能
- 实时天气、24 小时预报、7 日趋势
- 定位获取当前城市天气（支持无权限时手动添加）
- 城市搜索、添加、删除、排序、左右滑动切换
- 多 Provider 适配（QWeather / OpenWeather）+ fallback
- 本地缓存（Room）与弱网兜底
- 生活建议模块（优先使用 API 返回建议）
- 应用内 API Key 设置页面（不依赖代码硬编码）

## 2. 技术栈和架构

### 2.1 技术栈
- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material 3
- **架构模式**: MVVM + Repository
- **依赖注入**: Hilt
- **网络**: Retrofit + OkHttp + Kotlinx Serialization
- **数据库**: Room + DataStore
- **定位**: Google Play Services Location
- **权限**: Accompanist Permissions
- **构建**: Gradle (Kotlin DSL)

### 2.2 架构层次
```
┌─────────────────────────────────────────┐
│                UI Layer                 │
│  (Compose UI, Theme, Navigation)        │
├─────────────────────────────────────────┤
│             ViewModel Layer             │
│  (WeatherVM, CityManageVM, SettingsVM)  │
├─────────────────────────────────────────┤
│             Repository Layer            │
│  (WeatherRepository, LocationRepository)│
├─────────────────────────────────────────┤
│              Data Sources               │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
│  │Network  │  │Database │  │Location │ │
│  │Provider │  │ Room    │  │Client   │ │
│  └─────────┘  └─────────┘  └─────────┘ │
└─────────────────────────────────────────┘
```

### 2.3 目录结构
```
app/src/main/java/com/weatherglass/
├── core/                    # 核心模块
│   ├── common/             # 通用工具 (Result, Dispatcher)
│   ├── data/               # 数据层 (Repository, ApiKeyStore)
│   ├── database/           # 数据库层 (Room)
│   ├── location/           # 定位服务
│   ├── model/              # 领域模型
│   └── network/            # 网络层 (Provider, API)
├── di/                     # 依赖注入
├── feature/                # 功能模块
│   ├── weather/            # 天气主页面
│   ├── city/               # 城市管理
│   └── settings/           # API设置
├── navigation/             # 导航
├── ui/                     # UI主题
├── MainActivity.kt         # 主Activity
└── WeatherApplication.kt   # Application类
```

## 3. 核心模块详解

### 3.1 统一领域模型 (`core/model`)

#### 3.1.1 City 模型
```kotlin
data class City(
    val id: String,
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false,
    val sortOrder: Int = 0
)
```

#### 3.1.2 天气数据模型
```kotlin
// 当前天气
data class CurrentWeather(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val description: String,
    val condition: WeatherCondition,
    val humidity: Int,
    val pressureHPa: Int,
    val windSpeedKph: Double,
    val windDirection: String,
    val observationTimeEpochSec: Long,
    val iconCode: String? = null
)

// 天气条件枚举
enum class WeatherCondition {
    Clear, Cloudy, Rain, Snow, Thunder, Fog, Wind, Haze, Unknown
}

// 天气数据包（核心数据结构）
data class WeatherBundle(
    val cityId: String,
    val providerId: String,
    val updatedAtEpochSec: Long,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val lifestyle: List<LifestyleAdvice> = emptyList()
)
```

**设计要点**:
- 所有字段使用标准单位（摄氏度、公里/小时、百帕）
- 使用 `@Serializable` 注解支持 JSON 序列化
- `WeatherCondition` 枚举统一不同 API 的天气状态

### 3.2 网络层 (`core/network`)

#### 3.2.1 Provider 接口设计
```kotlin
interface WeatherProvider {
    val providerId: String
    val isConfigured: Boolean
    
    suspend fun getWeather(lat: Double, lon: Double, cityId: String): WeatherBundle
    suspend fun searchCity(query: String): List<City>
}
```

#### 3.2.2 实现的 Provider
1. **QWeatherProvider** (和风天气)
   - API 端点: `https://devapi.qweather.com/`
   - 支持实时、24h、7d 预报 + 生活指数
   - 城市搜索使用独立的 Geo API

2. **OpenWeatherProvider** (OpenWeatherMap)
   - API 端点: `https://api.openweathermap.org/`
   - 支持当前天气 + 5日/3小时间隔预报
   - 生活建议由本地逻辑生成

#### 3.2.3 Provider 选择策略
```kotlin
class RegionAwareProviderSelector : ProviderSelector {
    override fun orderedProviders(lat: Double, lon: Double): List<WeatherProvider> {
        val inChinaLikeBounds = lat in 18.0..54.0 && lon in 73.0..136.0
        return if (inChinaLikeBounds) listOfNotNull(q, o) else listOfNotNull(o, q)
    }
}
```

**策略逻辑**:
- 中国境内（经纬度范围判断）优先使用 QWeather
- 海外优先使用 OpenWeather
- 第一个 Provider 失败后自动尝试下一个

#### 3.2.4 天气条件映射
```kotlin
fun mapQWeatherIcon(icon: String): WeatherCondition {
    return when (icon.toIntOrNull()) {
        in 100..103 -> WeatherCondition.Clear
        in 104..199 -> WeatherCondition.Cloudy
        in 300..399 -> WeatherCondition.Rain
        // ...
    }
}
```

### 3.3 数据层 (`core/data`)

#### 3.3.1 WeatherRepository 实现
**核心方法 `fetchWeather` 逻辑**:
1. 根据地理位置获取 Provider 优先级列表
2. 依次尝试每个 Provider
3. 成功则缓存到 Room 数据库
4. 所有 Provider 失败则尝试加载缓存
5. 缓存数据标记 `fromCache = true`

**缓存策略**:
```kotlin
// 成功时更新缓存
weatherDao.upsert(WeatherCacheEntity(
    cityId = city.id,
    providerId = weather.providerId,
    updatedAtEpochSec = weather.updatedAtEpochSec,
    payloadJson = json.encodeToString(WeatherBundle.serializer(), weather)
))

// 失败时尝试加载缓存
val cache = weatherDao.getByCityId(city.id)
if (cache != null) {
    return AppResult.Success(cached, fromCache = true)
}
```

#### 3.3.2 ApiKeyStore (API Key 管理)
使用 Jetpack DataStore Preferences 存储 API Key:
```kotlin
class ApiKeyStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("api_keys.preferences_pb") }
    )
    
    val keysFlow: Flow<ApiKeys> = dataStore.data.map { pref ->
        ApiKeys(
            qWeatherKey = pref[QWEATHER_KEY].orEmpty(),
            openWeatherKey = pref[OPENWEATHER_KEY].orEmpty()
        )
    }
}
```

**安全特性**:
- API Key 不硬编码在代码中
- 存储在应用私有目录
- 支持运行时动态更新

### 3.4 数据库层 (`core/database`)

#### 3.4.1 Room 数据库设计
```kotlin
@Database(entities = [WeatherCacheEntity::class, CityEntity::class], version = 1)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun cityDao(): CityDao
}
```

#### 3.4.2 表结构
1. **weather_cache** 表
   - 主键: cityId
   - 字段: providerId, updatedAtEpochSec, payloadJson
   - 用途: 缓存天气数据（JSON 格式存储整个 WeatherBundle）

2. **cities** 表
   - 主键: id
   - 字段: name, countryCode, latitude, longitude, isCurrentLocation, sortOrder
   - 用途: 存储用户添加的城市

### 3.5 定位服务 (`core/location`)

#### 3.5.1 位置获取流程
```kotlin
class FusedLocationClient : LocationClient {
    override suspend fun getCurrentLocation(): LocationPoint? {
        val provider = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            provider.lastLocation
                .addOnSuccessListener { location ->
                    cont.resume(LocationPoint(location.latitude, location.longitude))
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }
}
```

#### 3.5.2 逆地理编码
```kotlin
class GeocoderService {
    fun cityNameOf(lat: Double, lon: Double): String {
        val list = geocoder.getFromLocation(lat, lon, 1)
        return list?.firstOrNull()?.locality ?: "Current Location"
    }
}
```

## 4. 功能模块实现

### 4.1 天气主页面 (`feature/weather`)

#### 4.1.1 ViewModel 状态管理
```kotlin
data class WeatherScreenState(
    val weatherState: UiState<WeatherBundle> = UiState.Loading,
    val cities: List<City> = emptyList(),
    val selectedCityId: String? = null,
    val weatherByCityId: Map<String, WeatherBundle> = emptyMap(),
    val hasLocationPermission: Boolean = false,
    val networkWarning: String? = null
)
```

**关键功能**:
- 监听城市列表变化
- 自动去重城市（名称+经纬度相似度）
- 预加载相邻城市数据（提升滑动体验）

#### 4.1.2 UI 实现特色

**动态背景系统**:
```kotlin
private fun weatherVisualStyle(condition: WeatherCondition): WeatherVisualStyle {
    return when (condition) {
        WeatherCondition.Clear -> WeatherVisualStyle(
            top = SunnyStart,      // #8FC9FF
            mid = SunnyEnd,        // #E8F4FF
            bottom = Color(0xFFF2B287)
        )
        WeatherCondition.Rain -> WeatherVisualStyle(
            top = RainyStart,      // #486685
            mid = Color(0xFF3E5877),
            bottom = Color(0xFF2A3547)
        )
        // ...
    }
}
```

**毛玻璃卡片**:
```kotlin
@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x3309111E))
    ) { /* ... */ }
}
```

**城市滑动切换**:
- 使用 `pointerInput` + `detectHorizontalDragGestures`
- 拖拽超过 22% 容器宽度触发切换
- 支持预加载左右城市数据

**7日预报趋势图**:
- 使用 Compose Canvas 绘制折线图
- 支持夜间/白天配色切换
- 自定义 Paint 绘制温度标签

### 4.2 城市管理 (`feature/city`)

#### 4.2.1 搜索功能
```kotlin
fun onQueryChange(value: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        delay(350)  // 防抖 350ms
        doSearch(value)
    }
}
```

#### 4.2.2 城市去重逻辑
```kotlin
private fun deduplicateCities(input: List<City>): List<City> {
    return input.groupBy { city ->
        // 按名称+经纬度相似度分组
        input.firstOrNull { existing ->
            existing.name.equals(city.name, ignoreCase = true) &&
            abs(existing.latitude - city.latitude) <= 0.12 &&
            abs(existing.longitude - city.longitude) <= 0.12
        }
    }.map { (_, cities) ->
        // 保留 isCurrentLocation 的版本
        cities.firstOrNull { it.isCurrentLocation } ?: cities.first()
    }
}
```

#### 4.2.3 城市排序
- 支持上移/下移操作
- 排序后更新 sortOrder 字段
- 实时同步到数据库

### 4.3 API 设置 (`feature/settings`)

**设置界面功能**:
- 输入 QWeather API Key
- 输入 OpenWeather API Key（可选）
- 保存到 DataStore
- 运行时生效（无需重启应用）

## 5. 依赖注入配置

### 5.1 Hilt 模块

**AppModule**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {
    @Provides @IntoSet
    fun provideQWeatherProvider(provider: QWeatherProvider): WeatherProvider = provider
    
    @Provides @IntoSet
    fun provideOpenWeatherProvider(provider: OpenWeatherProvider): WeatherProvider = provider
    
    @Provides @Singleton
    fun provideProviderSelector(providers: Set<WeatherProvider>): ProviderSelector {
        return RegionAwareProviderSelector(providers.sortedBy { it.providerId })
    }
}
```

**NetworkModule**:
- 提供 Json 实例（忽略未知字段）
- 提供 OkHttpClient（带日志拦截器）
- 提供两个 Retrofit 实例（QWeather/OpenWeather）

**DatabaseModule**:
- 提供 Room 数据库实例
- 提供 DAO 实例

## 6. 数据流分析

### 6.1 天气数据获取流程
```
用户操作 → ViewModel
    ↓
WeatherRepository.fetchWeather(city)
    ↓
ProviderSelector.orderedProviders(lat, lon)
    ↓
尝试 Provider 1 (QWeather)
    ↓ (失败)
尝试 Provider 2 (OpenWeather)
    ↓ (成功)
缓存到 Room → 返回 AppResult.Success
    ↓
ViewModel 更新 StateFlow
    ↓
Compose UI 重组显示
```

### 6.2 城市管理流程
```
搜索输入 → CityManageViewModel
    ↓ (防抖 350ms)
WeatherRepository.searchCity(query)
    ↓
尝试 Provider.searchCity()
    ↓
返回城市列表 → 更新 UI
    ↓
用户点击添加
    ↓
WeatherRepository.cacheCity(city)
    ↓
CityDao.upsert() → 保存到数据库
    ↓
Flow 触发更新 → 所有监听者收到通知
```

## 7. UI/UX 设计特色

### 7.1 动态天气背景
- 根据天气条件（晴/雨/雪/雾）切换渐变色
- 支持呼吸动画效果（`infiniteRepeatable`）
- 顶部状态栏颜色随背景变化

### 7.2 毛玻璃效果
- 半透明卡片背景 (`Color(0x3309111E)`)
- 圆角设计 (`RoundedCornerShape(24.dp)`)
- 透出底部动态背景

### 7.3 流畅交互
- 城市滑动切换（支持预加载）
- 7日预报全屏页面（带返回手势）
- 搜索防抖（350ms）
- 下拉刷新（计划中）

### 7.4 信息层次
```
主页面信息布局:
┌─────────────────────────┐
│ 城市名        [+] [⋮]   │
├─────────────────────────┤
│     23°                 │ ← 主温度
│  晴 最高28° 最低18°     │
│  [空气良 85]            │
├─────────────────────────┤
│   [预报面板]            │ ← 3日预报
│   查看近7日天气         │
├─────────────────────────┤
│   [天气详情]            │ ← 详细指标
│   降水/紫外线/湿度/...  │
├─────────────────────────┤
│   [生活建议]            │ ← 生活指数
│   穿衣/运动/洗车/...    │
└─────────────────────────┘
```

## 8. 关键技术亮点

### 8.1 多 Provider 容灾
- 地理位置感知的 Provider 选择
- 自动 fallback 机制
- 统一的错误处理和用户提示

### 8.2 离线缓存策略
- Room 持久化存储
- 网络失败时自动加载缓存
- 明确标记缓存数据（`fromCache = true`）

### 8.3 城市去重算法
- 基于名称相似度 + 经纬度距离
- 优先保留定位城市
- 避免重复添加

### 8.4 预加载优化
- 预加载左右相邻城市数据
- 滑动切换时无缝衔接
- 减少用户等待时间

## 9. 潜在问题和改进建议

### 9.1 当前限制

1. **7日预报图表**
   - 使用自定义 Canvas 绘制，复杂度较高
   - 考虑使用现成的图表库（如 Vico）

2. **生活建议模块**
   - OpenWeather 的生活建议由本地逻辑生成，准确性有限
   - QWeather 的生活指数更丰富但仅限国内

3. **错误处理**
   - 网络错误提示较为简单
   - 可以增加重试机制和更详细的错误分类

4. **性能优化**
   - 大量城市时可能影响性能
   - 建议增加分页加载

### 9.2 改进建议

1. **增加 API Key 验证**
   ```kotlin
   // 保存前验证 Key 有效性
   suspend fun validateKey(provider: String, key: String): Boolean {
       return try {
           when (provider) {
               "qweather" -> api.testKey(key).isSuccessful
               "openweather" -> api.testKey(key).isSuccessful
               else -> false
           }
       } catch (e: Exception) { false }
   }
   ```

2. **增加天气预警**
   - QWeather 支持天气预警 API
   - 可以在首页显著位置展示

3. **增加分钟级降雨**
   - QWeather 支持分钟级降雨预报
   - 对出行建议很有价值

4. **优化缓存策略**
   ```kotlin
   // 增加缓存过期时间判断
   val isCacheValid = System.currentTimeMillis() / 1000 - cache.updatedAtEpochSec < 3600
   ```

5. **增加桌面小组件**
   - 使用 Glance 框架
   - 显示当前城市天气概况

6. **增加 Wear OS 支持**
   - 共享核心业务逻辑
   - 适配手表 UI

7. **国际化支持**
   - 当前界面为中文硬编码
   - 建议使用字符串资源支持多语言

8. **单元测试覆盖**
   - Provider 映射逻辑
   - 城市去重算法
   - Repository 缓存逻辑

## 10. 总结

WeatherGlass 是一个架构清晰、功能完整的 Android 天气应用。它成功实现了：

1. **统一的领域模型** - 解耦 UI 和具体 API 实现
2. **多 Provider 容灾** - 提供更好的可用性
3. **优雅的 UI 设计** - 动态背景 + 毛玻璃效果
4. **完善的离线支持** - Room 缓存 + 弱网兜底
5. **灵活的配置管理** - 应用内 API Key 设置

项目代码质量较高，遵循了现代 Android 开发最佳实践，是一个很好的学习和参考项目。后续可以通过增加更多天气数据源、优化性能、扩展平台支持等方式进一步提升。