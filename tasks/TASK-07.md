# TASK-07 · 质量与发布

**状态**：进行中（Stage A/B/C/D/E/F/G 已完成）　|　**依赖**：TASK-00~06 全部　|　对应 SPEC：阶段 7、18~22 章

## 目标
完成多设备/性能/国际化/无障碍验证、设置页收尾、错误与日志完善，准备发布。

## 实现要求
- 设置页（SPEC 15）完整：外观（主题/强调色/动态配色/语言）、转换默认值（图/视/音质量、默认尺寸、
  保留元数据、保留音轨）、文件（默认目录、重名处理、完成通知、临时清理、清理缓存）、关于（版本/开源许可/隐私/反馈/查看日志）。
- 默认值按 SPEC 22 落地。
- 性能（SPEC 18）：分析不阻塞主线程、列表懒加载、缩略图异步缓存、采样解码、及时释放资源、优先硬件编码与降级。
- 国际化检查：无硬编码未翻译文本，档位语义跨语言一致（SPEC 7.3 对照表）。
- 无障碍（SPEC 8.4）：浅/深对比度、状态不只靠颜色、字体缩放、最小点击区。
- 错误处理（SPEC 17）全覆盖：文件不存在/权限失效/格式不识别/损坏/无编码器/空间不足/目录不可写/引擎崩溃/取消/进程回收 等。
- 体积（SPEC 18.3）：FFmpeg 裁剪、按 ABI/App Bundle、无重复媒体库、依赖统一版本。

## 执行步骤
- [ ] 设置页全项实现 + 默认值落地
- [ ] 全量错误路径处理与可理解文案 + “查看详情”技术信息
- [ ] 性能 pass（大文件/大量文件/缩略图/内存）
- [ ] 国际化与无障碍检查
- [ ] 多设备/4KB+16KB 页面/低内存/横竖屏测试
- [ ] 单元/集成/UI 测试补齐（SPEC 20）
- [ ] 体积优化与发布准备（ABI/App Bundle、许可、隐私说明）

## 验收标准
- SPEC 19 全部可测试验收项通过（文件选择/参数/转换/多语言/主题/稳定性）。
- 旋转不丢任务、进程回收可恢复、空间不足不产生伪成功损坏文件、取消不留无效输出。
- 关键流程在多设备实机通过；无明显未翻译文案；无障碍基本达标。
- 可产出可发布的 App Bundle，许可证/隐私说明齐备。

## 完成情况

### Stage A（已完成，已验证）—— 真机测试前的设置页/语言基础收尾
- **运行时语言切换真正生效**：
  - 新增 `core/localization/AppLocaleManager`，使用 `AppCompatDelegate.setApplicationLocales(...)`
    驱动应用级语言切换；`MainActivity` 在观察到 `AppSettings.language` 变化后立即应用，不再只是把语言存进
    DataStore 却不真正切换界面。
  - `app/build.gradle.kts` 开启 `androidResources.generateLocaleConfig = true`，并新增
    `app/src/main/res/resources.properties`（`unqualifiedResLocale=en`），让 Android 13+ 的系统
    “应用语言”入口可以根据资源自动生成 locale config（对应官方推荐方案）。
- **设置页从“只有外观”扩展到更适合真机测试**：
  - 新增“转换默认值”分区：图片/视频/音频默认质量均可设置（`BEST/HIGH/STANDARD/SMALL_SIZE`）。
  - 新增“文件”分区：自动清理临时文件开关、缓存占用展示、手动清理缓存按钮。
  - 新增“关于”分区：展示应用版本号，方便真机反馈时确认版本。
  - `ChipRow` 与强调色选择从单行 `Row` 改为 `FlowRow`，强调色触点提升到 48dp，并补上可读的
    `contentDescription`，对小屏、大字体和基础无障碍更友好。
- **默认值真正接入首页行为**：
  - `HomeViewModel` 改为读取 `SettingsRepository` 当前设置，新加入文件分组时会使用设置页里选好的
    图片/视频/音频默认质量，而不再永远固定 `HIGH`。
- **缓存维护能力收口**：
  - 新增 `core/file/CacheMaintenance`，供设置页“清理缓存”和 `ResidualTempCleanupWorker`
    共用；“清理缓存”会清理应用缓存目录，不影响 `Download/转个格式` 正式输出。
  - 若当前仍有活跃转换任务，设置页会阻止“清理缓存”，避免误删进行中的中间文件。
  - 应用启动时是否自动排队残留临时文件清理，现在受 `AppSettings.autoCleanupTempFiles` 控制。
- 验证：`gradlew.bat assembleDebug testDebugUnitTest` 通过。

### Stage B（已完成，已验证）—— 失败详情入口 + 关于说明补齐
- **失败任务增加“查看详情”**：
  - `feature/progress/ConversionProgressScreen.kt`：失败行不再直接把内部调试信息铺在列表里，而是显示统一的
    “转换失败”文案，并提供“查看详情”按钮；点击后用对话框展示 `ConversionError.debugMessage`。
  - `feature/history/HistoryScreen.kt`：历史页失败记录也同样支持“查看详情”，便于用户在任务结束后回看失败原因。
  - `ConversionProgressViewModel` / `HistoryViewModel` 相应拆分了失败展示字段与失败详情字段，为后续更细的
    用户文案 / 技术细节分层留出空间。
- **设置页“关于”分区补成可读说明**：
  - 新增“隐私说明”与“开源组件”两个弹窗入口，便于真机测试和发布前核对基本信息。
  - 隐私说明明确当前默认本地转换、不上传、不登录、仅访问用户主动选择的文件。
  - 开源组件说明列出 AndroidX、Material 3、Coil、Media3、Room、DataStore、FFmpegKit 16KB fork。
- 验证：`gradlew.bat assembleDebug testDebugUnitTest` 通过。

### Stage C（已完成，已验证）—— 文件设置补强 + 基础日志查看
- **设置页“文件”分区补充实际行为说明/控制**：
  - 新增“保存位置”只读展示：当前明确显示为 `Download/转个格式`。
  - 新增“重名处理”只读展示：当前明确显示为“自动追加序号”。
  - 新增“完成通知”开关：控制全部转换结束后是否弹出汇总通知。
  - 原有“自动清理临时文件”与“清理缓存”保留；现在整组文件相关设置更接近 SPEC 15 的可测试形态。
- **完成通知真正落地**：
  - `ConversionForegroundService` 在观察到本轮任务全部结束时，会根据设置页开关决定是否发出一条汇总通知，
    内容包含成功/失败/取消数量。
  - 进度通知与完成通知分离：进度通知负责前台保活，完成通知负责结束反馈。
- **基础日志查看入口**：
  - 新增 `core/common/InMemoryLogStore`，把本次应用会话中的日志缓存在内存里。
  - `AndroidLogger` 写 Logcat 时同步写入内存日志。
  - 设置页“关于”新增“查看日志”，可直接弹出最近日志文本，便于真机测试时快速自查，而不必每次都连电脑看 logcat。
- 验证：`gradlew.bat assembleDebug testDebugUnitTest` 通过。

### Stage D（已完成，已验证）—— 反馈信息分享
- **设置页新增“分享反馈信息”**：
  - 通过系统分享面板导出一段测试反馈文本，包含应用版本、当前语言、当前主题，以及最近最多 40 条进程内日志。
  - 目的不是替代正式问题反馈系统，而是让真机测试阶段遇到问题时能更快地把关键信息发给开发者或留档。
- **与日志查看协同**：
  - “分享反馈信息”会先刷新当前进程内日志，再拼装分享内容，避免把太旧的日志发出去。
- 验证：`gradlew.bat assembleDebug testDebugUnitTest` 通过。

### Stage E（已完成，已验证）—— 失败文案分层 + 首页活跃任务入口
- **失败任务改为“用户可理解原因 + 技术详情分层”**：
  - 新增 `core/model/FailureReasonCodec`，把失败种类与调试详情编码进现有 `failureReason` 单字段，
    不改 Room schema 也能兼容历史数据。
  - 进度页/历史页现在优先展示结构化的友好失败文案（如“找不到文件”“空间不足”“权限失效”），
    技术细节继续放在“查看详情”弹窗中，不再只有笼统的“转换失败”。
  - `FailureReasonCodec` 额外处理了多行调试信息，避免换行后的详情被截断；旧历史里的纯文本失败原因仍保持可读。
- **首页补上“有任务正在转换”的直接入口**：
  - `HomeViewModel` 订阅 `ConversionOrchestrator.tasks` 汇总当前活跃批次，并把“已完成/总数 + 当前文件名”投给首页。
  - 首页无文件态与文件列表态都会显示活跃任务卡片，可一键进入进度页，减少用户切回首页后“任务去哪了”的迷失感。
  - 同时修正了首页清空待转换文件时误把活跃任务卡片清掉的闪烁问题。
- **回归测试补充**：
  - 新增 `FailureReasonCodecTest`，覆盖结构化失败原因的编解码与旧数据兼容。
- 验证：`gradlew.bat testDebugUnitTest assembleDebug` 通过。

### Stage F（已完成，已验证）—— 静态质量门禁补强
- **`lintDebug` 从硬失败恢复到可通过**：
  - `ConversionForegroundService` 对 `POST_NOTIFICATIONS` 增加显式权限检查，避免 Android 13+ 在通知被拒后
    仍直接 `notify()` 的 Lint 硬错误。
  - `OutputLocationResolver` 增加 API 29 以下的 MediaStore 兼容分支：Android 10+ 继续用 `Downloads` +
    `RELATIVE_PATH`，更低版本改走 `MediaStore.Files` + 目标文件路径，消除 `NewApi` 静态错误。
  - `Media3Engine` 明确标注 `@UnstableApi`，并新增项目级 `lint.xml` opt-in，避免 Media3 Transformer 的
    不稳定 API 在 Lint 中持续报错。
- **顺手清理一项无效资源**：
  - 旧的 `error_conversion_failed` 字符串已不再使用，已从中英文资源中移除。
- **当前结果**：
  - `gradlew.bat lintDebug` 已通过。
  - 仍有 35 条 warning，主要是模板残留未使用资源、依赖可升级提示、以及 `android.media.ExifInterface`
    的替换建议；这些不阻塞当前真机测试，但仍属于后续发布前收尾项。

### Stage G（已完成，已验证）—— 大文件拆分与代码结构整理
- **优先拆分的 4 个大 Screen 已拆为“路由 + 组件”结构**：
  - `feature/settings/SettingsScreen.kt` 仅保留状态订阅、事件收集与反馈分享入口，具体 section/dialog 拆到
    `SettingsSections.kt`、`SettingsDialogs.kt`。
  - `feature/home/HomeScreen.kt` 仅保留文件选择器、通知权限与启动转换链路，列表/空态/文件行拆到
    `HomeContent.kt`、`HomeFileRows.kt`。
  - `feature/progress/ConversionProgressScreen.kt` 与 `feature/history/HistoryScreen.kt` 仅保留页面级 scaffold，
    明细卡片、头部、动作区、详情弹窗拆到独立组件文件，减少单文件状态和分支密度。
- **第二梯队的编排/引擎文件已抽辅助职责**：
  - `conversion/ConversionOrchestrator.kt` 抽出 `ConversionRequestFactory` 与 `ConversionHistoryTracker`，
    把“请求解析”和“历史记录同步”从主编排器里拿出去，保留原有外部接口不变。
  - `engine/media/Media3Engine.kt` 抽出 `Media3ConversionConfigFactory`、`Media3TransformerRunner`、
    `Media3ErrorMapper`，把编码配置、Transformer 生命周期和错误映射与页面/UI 彻底解耦。
- **本次调整的目标**：
  - 不改用户可见行为，只降低单文件体积和职责混杂度，为后续真机测试、错误路径补齐和继续扩展设置项提供更稳的落点。
- 验证：`gradlew.bat compileDebugKotlin`、`gradlew.bat testDebugUnitTest` 通过（使用本机 JDK 17 设置 `JAVA_HOME` 执行）。

### 已知简化 / 下一步
- **设置页仍未完整覆盖 SPEC 15**：目前已补到“外观 + 部分转换默认值 + 文件行为说明 + 完成通知开关 +
  临时清理 + 关于版本/隐私/开源说明 + 基础日志查看 + 反馈信息分享”；还缺可自定义默认目录、可配置重名策略、
  保留元数据/音轨、反馈等。
- **失败详情仍是基础版本**：目前已经做到了“结构化友好原因 + 查看详情分层”，但还没补复制详情、
  更完整的错误恢复建议、以及更多边界错误的主动注入测试。
- **日志查看是进程内临时日志**：重启应用后会丢失，不是持久日志系统；足够支撑真机测试排错，但还不是最终发布态方案。
- **国际化与无障碍主要做了基础修正**：运行时语言、FlowRow、防溢出、强调色可读性已补，但还没做系统级
  TalkBack、超大字体、对比度、横竖屏逐项验收。
- **多设备/真机验证仍待执行**：这一步现在已经更适合上手验证，建议优先测：
  1. 设置页切换中文/英文是否立即生效。
  2. 默认质量设置后，重新选择文件时首页分组默认值是否跟着变化。
  3. 清理缓存按钮在空闲/转换中两种状态下的行为。
  4. Android 13+ 系统设置中的“应用语言”是否能看到本应用语言项。
