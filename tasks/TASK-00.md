# TASK-00 · 工程基础

**状态**：未开始　|　**依赖**：无　|　对应 SPEC：阶段 0、第 6/8/9 章

## 目标
把现有 appcompat/Views 骨架改造为 Compose + Material 3 工程底座，建立主题、导航、
持久化与统一日志/错误模型，为后续功能提供稳定地基。

## 实现要求
- Kotlin + Jetpack Compose + Material 3，Edge-to-edge。
- Material 3 主题：浅色/深色/跟随系统；强调色系统，**默认蓝色**，预设色（蓝/青/绿/紫/粉/橙/红），
  动态配色默认关闭、可在设置开启，低版本回退预设色。
- 中/英字符串资源体系（`values/` 英文、`values-zh-rCN/` 中文），文案不硬编码。
- 底部导航三项：转换 / 历史 / 设置（Navigation Compose）。
- DataStore（设置）与 Room（历史）初始化骨架。
- 统一 `Logger` 抽象与统一错误模型（`ConversionError` 等，见 SPEC 11、17 章）。
- 依赖注入（Hilt）接入。
- 包结构按 SPEC 9.4 建立：`core/`(common,model,file,database,datastore,localization,designsystem)、
  `engine/`(engine-api...)、`feature/`(home,file-picker,conversion-editor,conversion-progress,history,settings)、
  `service/`，第一版为单模块内的包。

## 决策记录
- **minSdk = 26**（采用 SPEC 建议）。理由：覆盖现代文件访问/通知/媒体能力，减少版本分支，仍覆盖绝大多数设备。用户授权自主决策。

## 构建环境（已探测）
- Gradle 9.4.1（wrapper），AGP 9.2.1，Kotlin 2.3.0；launcher JDK 17，daemon 需 JDK 21（由 foojay 自动下载）。
- Android SDK：`D:\ProgramData\Android_SDK`，platforms android-36.1/37.0，build-tools 36/37。
- 运行命令需设 `JAVA_HOME=C:\Program Files\Java\jdk-17.0.5` 作为 launcher。

## 执行步骤
- [x] 确认 minSdk 决策（=26）
- [x] 改造 Gradle：引入 Compose / Material3 / Navigation / DataStore（version catalog）；Room/Hilt 待后续阶段
- [x] 用 Compose 替换入口（新建 MainActivity），配置 MD3 主题与 edge-to-edge
- [x] 实现主题：蓝色默认 + 7 种预设强调色 + 浅/深/跟随系统 + 动态配色，全部由 DataStore 驱动
- [x] 建立中/英 strings 资源（语言**选择已持久化**；运行时 locale 应用待后续，见遗留）
- [x] 搭三页底部导航与占位页
- [x] 初始化 DataStore 设置仓库（Room 数据库骨架待 Stage C）
- [x] 建立 Logger 与错误模型、核心数据 enum/data class（MediaType/QualityPreset/SizePreset/AppSettings 等）
- [~] 依赖注入：第一版用手动 AppContainer（Hilt 待 Stage D，替换点局限于 Application/ViewModel 工厂）

> 进度分阶段：Stage A（Compose/主题/导航底座）✅；Stage B（DataStore 设置 + 主题/强调色持久化 + 核心模型 + Logger）✅ 已构建通过；
> Stage C（Room 骨架）、Stage D（Hilt，需先验证 KSP/AGP9 兼容）待续。

## 验收标准
- 应用可启动，三页底部导航可切换。
- 切换浅色/深色/跟随系统、切换强调色立即生效；重启后设置保留（DataStore）。
- 中文系统显示中文、其他显示英文，可手动切换并立即刷新。
- 无硬编码可见文案；Room/DataStore 可读写最小样例。
- 工程编译通过，`./gradlew assembleDebug` 成功。

## 完成情况

### Stage A（已完成，已验证）
- 改动：`build.gradle.kts`/`app/build.gradle.kts`/`libs.versions.toml`（Compose/Material3/Navigation 依赖，minSdk 26）；
  `AndroidManifest.xml`（MainActivity + LAUNCHER）；`themes.xml`/`values-night/themes.xml`（Material3 NoActionBar 基底）；
  `values/strings.xml`(英) + 新增 `values-zh-rCN/strings.xml`(中)；
  新增 Kotlin：`MainActivity`、`core/designsystem/{Color,Theme,Type}`、`ui/navigation/{TopLevelDestination,SwiftFormatApp}`、
  `feature/{home,history,settings}/*Screen`。
- 关键修正：AGP 9 内置 Kotlin，需移除 `org.jetbrains.kotlin.android` 插件；XML Material3 主题依赖 `com.google.android.material`，需保留。
- 验证：`gradlew :app:assembleDebug` BUILD SUCCESSFUL，产出 `app-debug.apk`。**未做**实机/模拟器运行验证（环境无设备）。

### Stage B（已完成，已验证）
- 新增依赖：`datastore-preferences`、`lifecycle-runtime-compose`。
- 新增核心模型：`core/model/{MediaType,QualityPreset,SizePreset,ConversionError,AppSettings(含 ThemeMode/AccentColor/AppLanguage)}`。
- 新增 `core/common/Logger`（接口 + AndroidLogger）。
- 新增 `core/datastore/SettingsRepository`（DataStore Preferences，settings 冷流 + setter）。
- 设计系统重构：`Color`(中性基底) + `Accent`(7 强调色映射/swatch) + `Theme`(由 ThemeMode/AccentColor/dynamic 驱动)。
- 新增 `di/AppContainer` + `SwiftFormatApplication`（手动 DI），manifest 注册 application。
- 设置页落地：主题模式 / 强调色 / 动态配色 / 语言 选择，经 `SettingsViewModel` 写回 DataStore；
  `MainActivity` 观察 settings 实时应用主题。
- 验证：`gradlew :app:assembleDebug` BUILD SUCCESSFUL；无编译警告。**未做**实机运行验证。

### 待续
- Stage C：Room 数据库骨架（历史表，需 KSP，先验证 KSP/AGP9 兼容）。
- Stage D：Hilt 替换手动容器（需 KSP）。
- 语言运行时切换：当前仅持久化选择，实际 locale 应用（不重启刷新）待实现（候选：AppCompatDelegate.setApplicationLocales 或 API33 LocaleManager）。
- 验收标准中「语言手动切换即时刷新」「Room 读写样例」仍待上述项；其余（主题/强调色即时生效与重启保留、构建通过、无硬编码文案）已满足（构建层面，未实机）。
