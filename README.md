# 每日天气 (WeatherGlass)

一个基于 Kotlin + Jetpack Compose 的 Android 天气应用，支持多数据源、城市管理、动态天气背景和生活建议。

## 功能概览

- 实时天气、24 小时预报、7 日趋势
- 定位获取当前城市天气（支持无权限时手动添加）
- 城市搜索、添加、删除、排序、左右滑动切换
- 多 Provider 适配（QWeather / OpenWeather）+ fallback
- 本地缓存（Room）与弱网兜底
- 生活建议模块（优先使用 API 返回建议）
- 应用内 API Key 设置页面（不依赖代码硬编码）

## 技术栈

- Kotlin, Coroutines, Flow
- Jetpack Compose, Navigation Compose
- Hilt
- Retrofit + OkHttp
- Room + DataStore

## 项目结构

- `app/src/main/java/com/weatherglass/core/model` 统一领域模型
- `app/src/main/java/com/weatherglass/core/network` Provider 与 API 适配
- `app/src/main/java/com/weatherglass/core/data` 仓储、Key 存储
- `app/src/main/java/com/weatherglass/core/database` 缓存层
- `app/src/main/java/com/weatherglass/feature/weather` 天气主页面
- `app/src/main/java/com/weatherglass/feature/city` 城市管理页面
- `app/src/main/java/com/weatherglass/feature/settings` API Key 设置页面
- `app/src/main/java/com/weatherglass/di` 依赖注入

## 本地运行

1. 使用 Android Studio 打开项目根目录
2. 确保环境：
   - JDK 17
   - Android SDK 35
   - Min SDK 26
3. 连接真机或启动模拟器运行

## API Key 配置（应用内）

   特别说明，因为是个人项目，所以需要接入自己的天气API Key。本项目使用的是和风天气的API。

1. 打开主页面右上角 `...`
2. 进入 `输入设置`
3. 输入：
   - QWeather API Key
   - OpenWeather API Key（可选）
4. 点击 `保存并应用`

说明：
- 当前版本仅使用应用内保存的 Key 请求天气
- 未配置对应 Key 时，该数据源请求会失败并给出提示

## 安全与隐私注意事项

- 不要把 API Key 写入代码、`README`、截图或日志
- `local.properties` 已在 `.gitignore` 中，避免误提交
- 建议提交前执行：
  - 检查是否包含 `.env`、`*.jks`、`*.keystore`、私钥文件
  - 全局搜索 `api_key` / `token` / `secret` 关键字

## 已知限制

- 7 日页风格仍在持续精修（布局和折线层级可继续优化）
- 目前建议模块图标以轻量符号为主，可后续升级矢量图标

## 后续建议

- 增加 API Key 可用性检测按钮（保存前校验）
- 增加更细颗粒度天气特效与性能分级
- 增加 UI 自动化测试与核心仓储单测
