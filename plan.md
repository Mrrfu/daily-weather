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
