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
- [x] 改造 Gradle：引入 Compose / Material3 / Navigation（version catalog）；DataStore/Room/Hilt 待后续阶段
- [x] 用 Compose 替换入口（新建 MainActivity），配置 MD3 主题与 edge-to-edge
- [~] 实现主题骨架：蓝色默认配色 + 浅/深/跟随系统 + 动态配色 hook（强调色预设与持久化待接 DataStore）
- [x] 建立中/英 strings 资源（语言切换骨架待接 DataStore）
- [x] 搭三页底部导航与占位页
- [ ] 初始化 DataStore 设置仓库 + Room 数据库骨架
- [ ] 建立 Logger 与错误模型、核心数据 enum/data class（MediaType/QualityPreset/SizePreset 等）
- [ ] 接入 Hilt，按包结构组织

> 进度分阶段：Stage A（Compose/主题/导航底座）✅ 已构建通过；
> Stage B（DataStore + 设置/主题持久化）、Stage C（Room）、Stage D（Hilt + 核心模型/Logger）待续。

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

### 待续
- Stage B/C/D：DataStore 设置与主题/语言持久化、Room 骨架、Hilt、Logger 与核心数据模型。
- 验收标准中「强调色/主题重启保留」「语言手动切换即时刷新」「Room/DataStore 读写样例」依赖后续阶段。
