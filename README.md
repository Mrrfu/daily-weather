
#### 在OpenCode使用Vibe Coding创建的天气APP，整体UI风格仿照小米自带天气APP的风格。刚开始使用的是GPT-5.3-Codex，整体体验是后端逻辑比较强，但是在前端UI上比较弱一点，需要多次给指令进行调整（也可能是我给的指令太不专业了）。后续用小米 MiMo V2 Pro模型进行性能、UI优化。相较于GPT-5.3-Codex，MiMo V2 Pro在UI上表现更好，但对于后端逻辑代码，一次性生成的代码有BUG，需要微调。

#### 以下说明由MiMO V2 Pro生成。


# WeatherGlass - 每日天气

一款基于 Kotlin + Jetpack Compose 的精美 Android 天气应用，提供沉浸式的天气体验。

## ✨ 功能特性

### 🌤️ 天气信息
- **实时天气**：当前温度、体感温度、湿度、气压、风速风向
- **逐时预报**：24小时天气趋势，包含温度、天气状况、降水概率
- **7日预报**：一周天气趋势，支持展开查看详情
- **生活建议**：穿衣、运动、洗车、出行等生活指数

### 📍 城市管理
- **智能定位**：自动获取当前位置天气（支持无权限时手动添加）
- **城市搜索**：支持中英文城市名搜索
- **滑动切换**：左右滑动快速切换城市
- **城市排序**：自定义城市显示顺序
- **去重机制**：自动识别并合并重复城市

### 🎨 界面设计
- **动态背景**：根据天气状况自动切换渐变背景
  - ☀️ 晴天：金黄渐变
  - ☁️ 多云：灰蓝渐变
  - 🌧️ 雨天：深蓝渐变
  - ❄️ 雪天：银白渐变
  - 🌫️ 雾霾：灰白渐变
- **毛玻璃卡片**：半透明卡片效果，透出动态背景
- **精美图标**：8种天气状态矢量图标
- **流畅动画**：城市切换、卡片展开等交互动效

### ⚙️ 设置功能
- **API Key 管理**：应用内配置天气数据源密钥
- **安全显示**：API Key 星号遮罩，支持切换查看
- **使用说明**：内置数据源说明

### 🌙 主题支持
- **深色模式**：跟随系统深色模式
- **动态颜色**：Android 12+ 支持 Material You 动态主题

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | MVVM + Repository |
| 依赖注入 | Hilt |
| 网络请求 | Retrofit + OkHttp |
| 本地存储 | Room + DataStore |
| 定位服务 | Google Play Services Location |
| 权限管理 | Accompanist Permissions |

## 📁 项目结构

```
app/src/main/java/com/weatherglass/
├── core/                      # 核心模块
│   ├── common/               # 通用工具 (Result, UiState, PerformanceMonitor)
│   ├── data/                 # 数据层 (Repository, ApiKeyStore)
│   ├── database/             # 数据库层 (Room Entities, DAOs)
│   ├── location/             # 定位服务
│   ├── model/                # 领域模型
│   └── network/              # 网络层 (Provider, API)
│       ├── qweather/         # 和风天气适配
│       └── openweather/      # OpenWeather 适配
├── di/                       # 依赖注入模块
├── feature/                  # 功能模块
│   ├── weather/              # 天气主页面
│   ├── city/                 # 城市管理
│   └── settings/             # API 设置
├── navigation/               # 导航配置
├── ui/theme/                 # 主题配置
├── MainActivity.kt           # 主 Activity
└── WeatherApplication.kt     # Application 类
```

## 🚀 本地运行

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK 35
- Min SDK 26 (Android 8.0)

### 运行步骤
1. 克隆项目到本地
2. 使用 Android Studio 打开项目根目录
3. 等待 Gradle 同步完成
4. 连接真机或启动模拟器
5. 点击运行按钮

## 🔑 API Key 配置

本应用支持两个天气数据源，至少需要配置其中一个：

### 和风天气 (推荐国内用户)
1. 访问 [和风天气开发平台](https://devapi.qweather.com/)
2. 注册账号并创建应用
3. 获取 API Key
4. 在应用内 `设置` 页面输入 Key

### OpenWeather (推荐海外用户)
1. 访问 [OpenWeather](https://openweathermap.org/api)
2. 注册账号并获取 API Key
3. 在应用内 `设置` 页面输入 Key

### 数据源选择策略
- 中国境内：优先使用和风天气
- 海外地区：优先使用 OpenWeather
- 请求失败：自动切换到备用数据源

## 📊 数据缓存策略

- **缓存有效期**：5分钟
- **离线支持**：网络失败时自动加载缓存数据
- **缓存标识**：使用缓存数据时显示提示信息
- **数据库索引**：优化城市查询和缓存查询性能

## 🔧 性能优化

- **并行请求**：天气数据多接口并发获取
- **请求缓存**：OkHttp 磁盘缓存减少重复请求
- **预加载**：预加载相邻城市数据提升滑动体验
- **Compose 优化**：使用 derivedStateOf 和 remember 减少重组

## 📝 已知限制

- 天气预警功能暂未实现
- 分钟级降雨预报暂未支持
- 桌面小组件暂未开发

## 🤝 后续计划

- [ ] 天气预警通知
- [ ] 分钟级降雨预报
- [ ] 空气质量指数
- [ ] 桌面小组件
- [ ] Wear OS 支持
- [ ] 多语言国际化

## 📄 License

本项目仅供学习和个人使用。
