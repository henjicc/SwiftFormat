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

## 待决策（开工前向用户确认）
- **minSdk**：现为 24，SPEC 建议 26。确认采用 26 还是保留 24。

## 执行步骤
- [ ] 确认 minSdk 决策
- [ ] 改造 Gradle：引入 Compose / Material3 / Navigation / DataStore / Room / Hilt（version catalog）
- [ ] 用 Compose 替换现有入口 Activity，配置 MD3 主题与 edge-to-edge
- [ ] 实现强调色 + 浅/深/跟随系统主题切换骨架（暂可用临时状态，后接 DataStore）
- [ ] 建立中/英 strings 资源与语言切换骨架
- [ ] 搭三页底部导航与空白页
- [ ] 初始化 DataStore 设置仓库 + Room 数据库骨架
- [ ] 建立 Logger 与错误模型、核心数据 enum/data class（MediaType/QualityPreset/SizePreset 等）
- [ ] 接入 Hilt，按包结构组织

## 验收标准
- 应用可启动，三页底部导航可切换。
- 切换浅色/深色/跟随系统、切换强调色立即生效；重启后设置保留（DataStore）。
- 中文系统显示中文、其他显示英文，可手动切换并立即刷新。
- 无硬编码可见文案；Room/DataStore 可读写最小样例。
- 工程编译通过，`./gradlew assembleDebug` 成功。

## 完成情况
（待填写：实际改动文件、验证命令与结果、遗留项）
