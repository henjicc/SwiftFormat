# TASK-07 · 质量与发布

**状态**：进行中（Stage A/B/C/D/E/F/G/H/I 已完成）　|　**依赖**：TASK-00~06 全部　|　对应 SPEC：阶段 7、18~22 章

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

### Stage H（已完成，已验证）—— 转换闪退止血与恢复链路加固
- **修复“开始转换后闪退，重启后因活跃任务恢复继续闪退”的致命链路**：
  - `ConversionOrchestrator` 之前只兜住了“提交前”异常；引擎转换、历史回写、完成/失败收尾阶段一旦抛出未捕获异常，
    会直接把整个进程带崩，同时把 Room 记录留在 `PENDING/PREPARING/CONVERTING/SAVING`。
  - 现在 `runTask()` 对非取消异常统一兜底，转换为 `ENGINE_CRASH` 失败并尽力回写历史；即使历史回写本身再失败，
    也至少会把内存任务状态改成失败/取消，避免继续以“进行中”悬挂。
- **恢复链路改为逐条保护，避免旧脏任务造成启动死循环**：
  - `ConversionRecoveryManager` 现在对每条活跃历史记录单独 `runCatching` 恢复；若恢复入队本身失败，会把该条记录直接标记为
    `FAILED`，而不是让启动协程异常冒泡导致整应用闪退。
  - `SwiftFormatApplication` 启动时的“残留清理 + 活跃任务恢复”也新增总兜底日志，不再允许恢复异常把进程带走。
- **前台服务启动改为“尽力而为，不再致命”**：
  - `ConversionForegroundService.start(context)` 现在捕获启动异常并只记日志；如果系统因为时机或策略拒绝启动前台服务，
    当前批次最多表现为“没有通知”，不会再因此闪退。
- **回归测试补充**：
  - 新增 `ConversionCrashSafetyTest`，覆盖两条关键回归：①引擎内部抛异常时任务应转为 `FAILED/ENGINE_CRASH`；
    ②恢复入队失败时历史记录应被标记为失败而不是继续保持活跃。
- 验证：`gradlew.bat testDebugUnitTest`、`gradlew.bat lintDebug`、`gradlew.bat assembleDebug` 通过
  （使用本机 JDK 17 设置 `JAVA_HOME` 执行）。

### Stage I（已完成，已验证）—— FFmpegKit 启动失败诊断增强
- **确认问题层级在 FFmpegKit native 启动，而不是 MKV 命令参数**：
  - 结合本地反查的 `NativeLoader.java` 可确认：`FFmpegKit failed to start on brand ...` 这句并不是业务层报错，
    而是 FFmpegKit 在 `System.loadLibrary(...)` 失败时，把底层 `UnsatisfiedLinkError` 包成 `Error` 后抛出的摘要。
  - 这意味着 `VIDEO -> MKV` 场景里如果连库都没起起来，真正有价值的信息通常在它的 `cause` 里，而不是顶层那句机型摘要。
- **补充 FFmpeg 运行时启动探测**：
  - 新增 `engine/ffmpeg/FfmpegRuntimeSupport`，首次进入 FFmpeg 引擎前会先调用 `FFmpegKitConfig.getVersion()`
    做一次启动探测；若 native 初始化失败，直接返回 `ENGINE_CRASH` 失败并附带完整调试信息，不再等到命令执行阶段才暴露。
  - `FfmpegEngine` 与 `FfmpegStillImageEngine` 现在都会在真正执行命令前检查这层可用性。
- **补充异常详情格式化**：
  - 新增 `core/common/ThrowableDebugFormatter`，把顶层异常、cause 链和前几行关键栈帧整理成单段文本。
  - `FfmpegEngine` / `FfmpegStillImageEngine` / `Media3Engine` / `NativeImageEngine` / `HeifAvifImageEngine`
    以及 `ConversionOrchestrator` 现在统一使用该格式化文本写入 `ConversionError.debugMessage`，让“查看详情”能看到
    类似 `UnsatisfiedLinkError: dlopen failed ...` 这类真正的底层原因，而不再只有一句“failed to start on brand ...”。
- **回归测试补充**：
  - 新增 `ThrowableDebugFormatterTest`，验证嵌套 `Error -> UnsatisfiedLinkError` 时详情文本会保留两层信息。
- 验证：`gradlew.bat testDebugUnitTest`、`gradlew.bat assembleDebug` 通过（使用本机 JDK 17 设置 `JAVA_HOME` 执行）。

### Stage J（已完成，已验证）—— 默认保留图片元数据（SPEC 15.2）
- **结论先行**：核查 SPEC 15.2「转换默认值」四项后，确认只有「默认保留图片元数据」是真正缺失的代码项；
  「系统动态配色」已在 Stage A 实现；「默认保持原始尺寸」已满足（`SizePreset.Original` 本就是各媒体类型
  尺寸档位列表里的第一个/默认选项）；「默认保留视频音频轨道」已满足（当前转换链路没有静音/抽轨开关，
  音轨始终保留，对应的“保留音频/视频静音”需求在 SPEC 里实际归类在第一版之后的附加能力，非本页待办）。
- **新增设置项**：`AppSettings.preserveImageMetadata`（默认 `true`，DataStore key `preserve_image_metadata`），
  `SettingsRepository.setPreserveImageMetadata`，`SettingsViewModel.setPreserveImageMetadata`，UI 落在
  `ConversionDefaultsSection`（`SettingsSections.kt`）里的一个 `ToggleRow`，字符串
  `settings_preserve_metadata` / `_desc`（中英双语）。
- **数据通路**：`ConversionRequest.preserveMetadata`（默认 `false`，引擎只认这个字段，不感知 `AppSettings`）→
  `ConversionRequestFactory.createResolvedRequest` 新增同名参数透传 → `ConversionOrchestrator.submit`/
  `submitAll`/`recover`/`convertAgain` 依次透传；`HomeViewModel.startConversion` 提交时传入
  `currentAppSettings.preserveImageMetadata`；`ConversionRecoveryManager` 新增 `settingsRepository` 依赖，
  恢复时读取当前设置值传给 `orchestrator.recover`（进程重启后的恢复语义用“当前设置”而非“提交时设置”，
  与本页其它默认值的语义一致）。
- **实现细节**（`core/file/ImageDecodeCompat.kt` 新增 `copyExifMetadata`，`NativeImageEngine` 转 JPG 成功后调用）：
  - 用 `android.media.ExifInterface`（平台类，非 androidx），构造参数与 `TAG_*` 常量名先用本机 Android SDK
    `android.jar` 反查 `javap` 确认过真实签名，避免凑名字编译失败。
  - 只复制拍摄相关标签（厂商/型号/软件、拍摄时间三种、曝光/光圈/ISO/焦距/闪光灯/白平衡、GPS 经纬度/海拔/
    时间戳/处理方式），不复制宽高等几何标签（输出尺寸可能已经变化）。
  - 方向标签固定写回 `ORIENTATION_NORMAL`，不是直接复制源文件的值：输出位图在写出前已经按 EXIF 方向旋正过，
    如果原样复制方向标签会变成“图已经转正 + 标签还说要再转一次”的二次旋转错误。
  - 只在输出格式为 `JPG` 时生效：`PNG`/`WEBP` 用 `Bitmap.compress` 直接落盘，且 `ExifInterface` 对这两种容器
    的标签支持本就有限；其余引擎（`HeifAvifImageEngine`、`FfmpegStillImageEngine`）不产出 JPG 输出，未受影响。
  - 整个复制过程包一层 `runCatching` 仅记录警告日志，不影响转换主流程成功与否（元数据是锦上添花，不是
    转换是否成功的判定条件）。
- **回归测试**：`ConversionCrashSafetyTest` 中 `ConversionRecoveryManager` 的构造与 `mock<ConversionOrchestrator>()`
  的 `recover` 调用因新增参数同步补了 `settingsRepository` mock 与 `preserveMetadata` 期望值。
- 验证：`gradlew.bat testDebugUnitTest assembleDebug` 通过（使用本机 JDK 17 设置 `JAVA_HOME` 执行）。

### Stage K（已完成，已验证）—— 可配置默认输出目录 + 可配置重名策略（SPEC 12.3/12.4）
- **新增设置项**：`AppSettings.customOutputDirectoryUri`（默认 `null`，存 SAF 树 Uri 字符串）、
  `AppSettings.nameCollisionStrategy`（新枚举 `NameCollisionStrategy`：`AUTO_NUMBER`/`OVERWRITE`，默认
  `AUTO_NUMBER`）；`SettingsRepository` 新增对应 DataStore key 与
  `setCustomOutputDirectory(uri: Uri?)`/`setNameCollisionStrategy(strategy)`。
- **范围裁剪**：SPEC 12.4 列了三种重名策略（自动加序号/覆盖/每次询问），本次只实现前两种；“每次询问”需要
  在批量提交转换的过程中暂停等待用户输入，与当前 `ConversionOrchestrator` 的异步入队编排模型不兼容，
  留作后续如需要再单独评估编排模型改造成本。
- **架构决策**：没有像 `preserveMetadata` 那样把这两个新参数一路透传过
  `ConversionOrchestrator`/`ConversionRequestFactory`/`HomeViewModel`/`ConversionRecoveryManager`，而是给
  `OutputLocationResolver` 直接注入 `SettingsRepository` 依赖，`resolve()` 改成 `suspend fun` 内部读取当前设置。
  原因：`resolve()` 只有 `ConversionRequestFactory.createResolvedRequest` 一处调用，且已经在
  `outputResolutionMutex.withLock` 的 suspend 上下文里，没有必要为两个新参数改动一整条调用链的公开签名。
- **`OutputLocationResolver` 双路径**：
  - 未设置自定义目录时沿用原 MediaStore 路径（`Download/转个格式`）；`OVERWRITE` 策略新增
    `deleteExistingByName`，按 `DISPLAY_NAME` + `RELATIVE_PATH`（或旧版 `DATA` 路径）先删后插。
  - 设置了自定义目录时改用 `android.provider.DocumentsContract` 直接操作 SAF 树（不引入
    `androidx.documentfile` 依赖，按项目“新增依赖前确认现有依赖或标准库无法合理解决”的约束）：
    `buildChildDocumentsUriUsingTree` 查询现有子项名称/`documentId`，`AUTO_NUMBER` 复用
    `OutputNaming.resolveCollision`，`OVERWRITE` 先 `deleteDocument` 再 `createDocument`。
  - 选目录后立即 `takePersistableUriPermission`（读写）持久化授权，切换/清空旧目录时
    `releasePersistableUriPermission` 释放，避免授权列表无限增长（`SettingsRepository.setCustomOutputDirectory`）。
- **设置页 UI**（`SettingsSections.kt`/`SettingsScreen.kt`/`SettingsViewModel.kt`）：保存位置行新增“选择目录”/
  “恢复默认”按钮，用 `ActivityResultContracts.OpenDocumentTree()` 启动系统目录选择器；自定义目录的展示名称
  通过 `SettingsViewModel.customOutputDirectoryLabel`（查询 SAF 树根文档的 `COLUMN_DISPLAY_NAME`，跑在
  `Dispatchers.IO`）派生展示，查询失败时兜底显示“自定义目录”。重名处理从静态文案改成
  `ChipRow<NameCollisionStrategy>`。
- 验证：`gradlew.bat testDebugUnitTest assembleDebug` 通过；`OutputLocationResolver` 重度依赖
  `MediaStore`/`DocumentsContract` 等 Android 框架行为，沿用既有项目惯例未补 Robolectric 测试，
  本机真机测试由用户后续验证。

### 已知简化 / 下一步
- **设置页仍未完整覆盖 SPEC 15 的极少数项**：默认目录/重名策略已可配置（见 Stage K），重名策略仍只支持
  `自动加序号`/`覆盖`两种，未做“每次询问”（见 Stage K 范围裁剪说明）。
- **失败详情仍是基础版本**：目前已经做到了“结构化友好原因 + 查看详情分层”，但还没补复制详情、
  更完整的错误恢复建议、以及更多边界错误的主动注入测试。
- **日志查看是进程内临时日志**：重启应用后会丢失，不是持久日志系统；足够支撑真机测试排错，但还不是最终发布态方案。
- **当前这次修复的重点是“避免崩溃与恢复死循环”**：即便某个具体格式/引擎后续仍存在单任务异常，现在也应表现为
  某条任务失败而不是整应用闪退；后续仍需继续定位最初触发这次实机崩溃的具体引擎/输入组合。
- **FFmpegKit 在个别 Android 15/16 设备上仍可能存在 native 兼容性问题**：当前已把这类问题从“黑盒闪退”提升为
  “可见的 startup probe/Throwable 详情”，但库本身是否与特定厂商 linker 完整兼容，仍取决于所用 fork 的二进制质量。
- **国际化与无障碍主要做了基础修正**：运行时语言、FlowRow、防溢出、强调色可读性已补，但还没做系统级
  TalkBack、超大字体、对比度、横竖屏逐项验收。
- **多设备/真机验证仍待执行**：这一步现在已经更适合上手验证，建议优先测：
  1. 设置页切换中文/英文是否立即生效。
  2. 默认质量设置后，重新选择文件时首页分组默认值是否跟着变化。
  3. 清理缓存按钮在空闲/转换中两种状态下的行为。
  4. Android 13+ 系统设置中的“应用语言”是否能看到本应用语言项。
