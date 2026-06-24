# TASK-07 · 质量与发布

**状态**：进行中（Stage A~AA 已完成）　|　**依赖**：TASK-00~06 全部　|　对应 SPEC：阶段 7、18~22 章

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
- [x] 设置页全项实现 + 默认值落地
- [x] 全量错误路径处理与可理解文案 + “查看详情”技术信息
- [ ] 性能 pass（大文件/大量文件/缩略图/内存）
- [x] 国际化与无障碍检查
- [ ] 多设备/4KB+16KB 页面/低内存/横竖屏测试
- [ ] 单元/集成/UI 测试补齐（SPEC 20）
- [x] 体积优化与发布准备（ABI/App Bundle、许可、隐私说明）—— 见 Stage M，签名配置仍需用户提供发布密钥
- [x] README 与 GitHub Releases 自动化准备—— 见 Stage V，当前先发布按 ABI 拆分的测试 APK，无需签名密钥

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

### Stage L（已完成，已验证）—— 国际化与无障碍验收

- **系统化扫描**：用 Explore 子代理对 `app/src/main/java` 全树与两份 `strings.xml` 做硬编码文案/缺失翻译排查，
  发现并修复 2 处：`ProgressComponents.kt`（复制失败详情按钮的 Toast 文案与按钮文案）从硬编码中文/英文改为
  `R.string.action_copy`/`R.string.error_details_copied`。
- **无障碍人工复核**（逐个判断装饰性 vs 功能性图标，未委托子代理）：`ConversionProgressScreen.kt` 的 TopAppBar
  返回按钮原来 `contentDescription = null`，是独立 `IconButton` 且旁边无文字标签，修正为
  `stringResource(R.string.nav_back)`；其余 `contentDescription = null` 的用法（`AccentSwatch`、文件行缩略图、
  FAB、勾选/箭头图标等）均确认旁边已有文字标签提供可读名称，保留 `null` 是正确做法。
- **完整性核对**：`grep -c '<string name=' values/strings.xml values-zh-rCN/strings.xml` 均为 151，中英文 key
  一一对应，无缺失。
- **范围内的“合理假设”而非逐项实测**：确认无 `screenOrientation` 锁定、文本均用 `sp`（无 `dp` 硬编码反模式），
  作为旋转支持/大字体缩放的间接证据；未做逐屏幕真机 TalkBack/超大字体/横竖屏穷举测试，留给用户后续真机验证
  （按用户既有反馈，常规走查已足够，不为这一项单独安排专门测试设备）。
- 验证：`gradlew.bat lintDebug` 在当时（Stage K 状态）报告 0 issue（后续 Stage M 复测发现该结论不完整，
  见下方说明）；`gradlew.bat testDebugUnitTest assembleDebug` 通过。

### Stage M（已完成，已验证）—— 体积与发布收尾评估（SPEC 18.3）

- **lint 复测纠偏**：重新执行 `gradlew.bat lintDebug` 发现实际是 **0 error / 56 warning**（Stage L 记录的
  “0 issue”是当时未仔细核对汇总行的误判，在此纠正）。逐条分类处理：
  - **已修复**：删除 `app/src/main/res/values/colors.xml` 中 7 个未被任何代码/资源引用的默认模板色
    （`purple_200/500/700`、`teal_200/700`、`black`、`white`），确认零引用后整文件移除；warning 数降到 49。
  - **本轮判定为不处理**（记录理由，避免后续重复评估）：
    - `ExifInterface`×8：建议把 `android.media.ExifInterface` 换成 `androidx.exifinterface.media.ExifInterface`。
      命中位置包含 `ImageDecodeCompat.kt` 的“保留图片元数据”EXIF 复制路径（Stage J，已经过用户真机验证）；
      迁移本身预期是同 API 的平替，但当前环境无法重新做真机验证，为一条安全提示性 lint 警告冒险动一段
      已验证过的发布关键路径不划算，暂缓，留到下次有真机测试窗口时再做。
    - `IconLauncherShape`/`MonochromeLauncherIcon`/`IconDuplicates`（共 10 条，图标相关）：用户最近 5 个提交
      正在手动迭代应用图标素材，本轮不插手这块归用户负责的区域。
    - `GradleDependency`/`NewerVersionAvailable`（约 11 条）：仅是“有更新版本”的常规提示，主动升级依赖本身
      引入新的兼容性风险，收益不确定，本轮不做。
    - `PluralsCandidate`×6：命中的都是“数字 + completed/failed/cancelled/files/task(s)”类技术统计文案，
      经判断不是真正需要语法复数的场景，是该检查的已知启发式误报，不改为 `<plurals>`。
    - `ObsoleteSdkInt`（`mipmap-anydpi-v26` 文件夹名冗余）、`UsableSpace`×2、`UseKtx`×5：纯风格/收尾提示，
      收益低于改动成本，本轮不做无关重构。
- **ABI 拆分 / App Bundle**：核实 AGP 的 `bundle { abi/density/language { enableSplit } }`
  默认均为 `true`，无需在 `app/build.gradle.kts` 新增任何配置；生产发行应使用
  `./gradlew bundleRelease` 产出 `.aab` 而不是通用 `assembleRelease` APK，Play 商店会按用户设备 ABI 自动分发拆分包，
  避免每个用户都下载全部四个 ABI 的 FFmpeg 原生库。**结论：此项已天然满足，不需要代码改动。**
- **FFmpeg 裁剪**：复核 TASK-05.md 中已记录的选型决策——当前依赖 `com.moizhassan.ffmpeg:ffmpeg-kit-16kb` fork
  只发布一个预编译 full 包，没有 min/audio/video 等更小变体；自建裁剪版本需要自建 NDK 交叉编译流水线，是
  TASK-05 选型时就已经评估过并主动放弃的方向（换来不用自建编译流水线）。**结论：维持现状，体积优化依赖
  上面的 App Bundle 按 ABI 拆分，而不是裁剪 FFmpeg 本体**；如未来确有必要，需要单独立项评估自建构建流水线的成本。
- **R8 代码压缩（`optimization.enable`）**：确认该开关从项目最初的脚手架提交起就是 `false`，从未被启用过，
  且全项目没有任何 `proguard-rules.pro` 文件。评估后**建议暂不开启**：FFmpegKit（反射桥接 native 库）、
  Room（生成代码）等依赖在开启代码压缩/混淆后行为可能与已通过真机验证的当前状态不一致，而当前环境没有
  真机验证条件，盲目开启有不可控的发布期风险；同时 dex 代码体积相对于四 ABI 全量 FFmpeg 原生库（体积大头）
  边际收益很小。留到下次有真机测试预算时再单独评估开启并配套编写 proguard 规则。
- **许可证/隐私说明复核**：对照中英文 `settings_privacy_content`/`settings_open_source_content`
  ——本地处理声明、AndroidX/Material3/Coil/Media3/Room/DataStore 框架性说明、FFmpegKit fork 的 GPL-3.0
  整体声明（上游仓库 + 协议原文链接）均已覆盖，中英文版本逐句对应。**结论：未发现需要更新之处。**
- **遗留缺口（需要用户介入，AI 无法代为决定/获取）**：`app/build.gradle.kts` 目前没有
  `signingConfigs`/release 签名配置，`bundleRelease` 现在只能产出未签名的 `.aab`；真正上架发布前需要用户提供
  发布密钥库（或决定使用 Play App Signing 上传密钥）并补上签名配置，这一步涉及密钥/密码等敏感信息，
  不在本轮自动化范围内。
- 验证：`gradlew.bat lintDebug`（49 warning / 0 error，较改动前 56 条下降）、
  `gradlew.bat testDebugUnitTest assembleDebug` 均通过。

### Stage N（已完成，已验证）—— 质量档位默认值纠正与调参集中化
- **默认质量档位由"高"改为"标准"**：用户反馈希望默认是体积与质量的折中档；核查后发现 SPEC 5.3/5.4/19.2
  与第22节默认值表当时明确写的是"默认质量为'高'"，代码（`AppSettings.kt`）也确实按此实现，并非 bug。
  与用户确认后**作为一次产品决策变更**同步处理：SPEC 对应 5 处"高"改为"标准"；代码层面发现真正决定首次启动
  默认值的不是 `AppSettings.kt` 的 data class 默认参数（那只在没经过 DataStore 时才有意义），而是
  `SettingsRepository.kt` 里 `prefs[Keys.xxx] ?: QualityPreset.HIGH` 这三处独立硬编码的 DataStore 缺省值回退，
  两边并不同步——已一并修正，否则只改 `AppSettings.kt` 不会在真机上生效。同时为保持"无路径都收敛到同一产品
  默认值"，把分散在 11 处 `request.quality ?: QualityPreset.HIGH`（FfmpegEngine/FfmpegCommandBuilder/
  Media3ConversionConfigFactory/HeifAvifImageEngine/NativeImageEngine/FfmpegStillImageEngine/HomeViewModel/
  GroupSettingsCard 等引擎与 UI 层的防御性兜底——按架构这些路径正常不会触发，是"quality 意外为 null"时的兜底）
  统一改为 `QualityPreset.STANDARD`，并更新 `OutputFormatCatalogTest` 中对应断言。
- **质量档位数值集中化**：新增 `engine/tuning/QualityPresetTuning.kt` 作为唯一调参入口，把原本分散在
  `ImageQualityMapper`/`AvifCrfMapper`/`VideoBitrateMapper`/`AudioBitrateMapper`/`FfmpegAudioBitrateMapper`/
  `OpusBitrateMapper` 六个文件里的质量档位数值表全部迁移过去，六个 mapper 改为纯委托调用，函数签名与原有
  行为（含数值）完全不变；新增 `forPresetOrStandard` 扩展函数统一"枚举未来增档时兜底落在 STANDARD"的防御逻辑。
- **视频转码新增编码速度参数**：核查发现 `FfmpegCommandBuilder` 的视频转码命令此前完全没有设置编码速度/线程
  参数。新增 `QualityPresetTuning.vp9EncodeSpeed` 表，给 WEBM（libvpx-vp9）按质量档位设置 `-cpu-used`
  （最佳=1 最慢最精细 ... 省空间=5 最快），解决 VP9 软件编码默认速度档位（0）过慢的问题；WEBM/MOV/MKV 三种
  视频输出格式统一加 `-threads 0` 让编码器自动用满多核。**已知限制**：MOV/MKV 用的 `libopenh264` 在这个
  FFmpeg 构建里没有类似 x264 `-preset`/VP9 `-cpu-used` 的复杂度旋钮，无法做到"按质量档位调节编码速度"，
  只能加通用的 `-threads 0`，这是该编码器本身的限制，不是实现疏漏。
- **音频转换慢的排查**：已确认无误用视频滤镜/无错误重采样；发现 `FfmpegEngine` 对纯音频转码也无条件跑了一次
  `ffprobe`（结果未被使用）的浪费，但用户要求先看完集中化后的参数表再决定是否继续做性能优化，本阶段未动
  `FfmpegEngine`/音频管道的性能代码，留作下一步。用户还问到"开发者模式日志窗口"的需求——已确认设置页"查看
  日志"+"分享反馈信息"（`InMemoryLogStore`/`SettingsViewModel`）已经是功能等价实现，无需新增；并向用户说明了
  Android Studio Logcat 配合 USB/无线调试是更专业的替代方案。
- 验证：`gradlew.bat testDebugUnitTest`（91 测试通过，含新增 `webmVideoTranscode_smallSizeUsesFasterVp9EncodeSpeed`
  等回归用例）、`gradlew.bat lintDebug`、`gradlew.bat assembleDebug` 均通过。

### Stage O（已完成，已验证）—— 历史/完成页操作区 UI 精简
- **问题**：用户反馈历史记录卡片与转换完成页的操作按钮区域"很乱"且占用空间过大——`打开/分享/查看位置/
  删除结果` 四个 `TextButton` 挤在一个 `FlowRow` 里经常换行到两行，`删除原文件` 还单独占一整行；卡片摘要部分
  文件名/格式/状态/时间/体积/质量标签各占一行，纵向也偏长。
- **核查可用图标集**：项目 `app/build.gradle.kts` 没有显式声明 `material-icons-extended`，但通过
  `gradlew app:dependencies` 核实它被其他依赖间接拉入并已在 `debugRuntimeClasspath` 上（`HomeFileRows.kt`/
  `GroupSettingsCard.kt` 等已在用 `Icons.Filled.Image/Movie/ChevronRight` 等扩展集图标可印证），因此可以直接用
  `Icons.AutoMirrored.Filled.OpenInNew`/`Icons.Filled.Share`/`Icons.Filled.FolderOpen`，不需要新增 Gradle 依赖。
- **`HistoryComponents.kt`（历史记录卡片）**：
  - `HistoryRecordSummary` 把文件名+状态合并到同一行（状态靠右、`labelMedium` 着色），格式转换/时间/体积合并成
    一行 `FORMAT → FORMAT · 时间 · 体积`（单行省略号），质量/尺寸标签行 `padding(top)` 从 8dp 收紧到 4dp；
    整体从 6 行摘要信息收紧到 2-3 行。
  - `HistoryRecordActions` 拆成两行：`打开/分享/查看位置` 改成 `IconButton`（保留原有字符串资源作
    `contentDescription`，满足无障碍朗读），`删除结果/再次转换/删除记录` 保留为 `TextButton` 文字按钮——
    按用户要求，删除类操作维持清晰文字而不是纯图标。
  - `Card` 内边距 12dp（原 16dp）、分割线竖向间距 8dp（原 12dp）。
- **`ProgressComponents.kt`（转换完成页任务行）**：`TaskActionRow` 同样把 `打开/分享/查看位置` 改成 `IconButton`
  行；原来"删除结果"和"删除原文件"分两个独立 `FlowRow`（后者总是单独占一整行）合并成一个 `FlowRow`，按各自条件
  显隐，减少一行。
- **未改动**：取消/重试/再次转换（顶部行）、删除原文件前的二次确认对话框、所有字符串资源——均按原有行为保留，
  没有新增/删改文案。
- 验证：`gradlew.bat compileDebugKotlin`（确认图标引用可解析）、`gradlew.bat testDebugUnitTest lintDebug
  assembleDebug` 均通过；UI 改动未跑真机截图复核（当前环境没有连接设备/模拟器），下次真机测试时建议确认
  图标可点击区域与文字密度的实际视觉效果。

### Stage P（已完成，已验证）—— 历史记录卡片改为单行列表项 + 溢出菜单
- **问题**：Stage O 收紧后用户真机实测反馈一个屏幕仍只能完整看到 2 条历史记录，且"已完成"状态文字独占一行
  右侧，下方却没有任何内容跟它对齐，视觉上显得突兀空旷。
- **参考的同类 UI 模式**：Google Files / Google Drive / Telegram 媒体下载列表等"历史记录/下载列表"类场景的
  通行做法——单行列表项（左侧类型图标 + 中间两行文字 + 行尾 1-2 个常用操作 + "更多"溢出菜单），且这些应用
  对"已完成"这种默认/期望状态通常**不展示专门的状态文字**，只有失败/进行中等需要用户注意的例外状态才显示
  状态标签。本次改造直接借用了这个思路，而不是继续在原有"卡片+多行文字+多行按钮"结构上挤间距。
  本项目自己的 `feature/home/HomeFileRows.kt` 的 `FileRow`（40dp 图标 + 两行文字 + 单个尾随 `IconButton`）
  恰好就是同一种列表项模式，因此直接复用了它的 `mediaIcon(MediaType)` 图标映射，保持视觉语言一致。
- **`HistoryComponents.kt` 改动**：
  - 整条记录从"卡片内三段纵向堆叠（摘要/分割线/按钮行）"改为单个 `Row`：40dp 媒体类型图标
    （复用 `feature.home.mediaIcon`）+ 文字列（标题行 + 一行合并详情：格式转换 · 时间 · 体积 · 质量 · 尺寸）
    + 行尾操作，divider 和单独的按钮行整段删除。
  - **状态文字仅在非 `COMPLETED` 时显示**（失败/取消/进行中），完成是历史记录里的默认期望结果，不再用
    "已完成"占一块地方；失败时的错误原因/"查看详情"仍保留在文字列里，不受影响。
  - 行尾操作改为"打开+分享"两个常驻 `IconButton`，`查看位置/删除结果/再次转换/删除记录` 收进一个
    `Icons.Filled.MoreVert` 触发的 `DropdownMenu`（`DropdownMenuItem`），不再用占地方的 `TextButton` 平铺。
    新增字符串资源 `action_more`（中"更多操作"/英"More options"）作为该图标的 `contentDescription`，
    中英 `strings.xml` 保持 152 key 同步。
  - 进行中（`isActive`）记录的行尾不再用独立 `Button`，改为同一 `Row` 末尾的 `TextButton`「查看进度」，
    与已完成/失败记录共享同一种单行结构。
- **`ProgressComponents.kt` 同步调整**：`TaskActionRow` 完成态的操作区也改成同样的"打开+分享图标 + 更多
  溢出菜单（查看位置/删除结果/删除原文件）"结构，保持与历史页一致的交互模式；删除原文件的二次确认弹窗
  行为不变（仍由 `onRequestDeleteOriginal` 触发同一个 `AlertDialog`）。
- **未改动**：取消/重试/再次转换的顶部行、删除确认对话框文案、`qualityLabel`/`sizeLabel`/`errorKindLabelRes`
  等既有标签函数。
- 验证：`gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` 均通过；
  英/中 `strings.xml` key 集合核对完全一致（152 个）。**仍未做真机视觉复核**（环境无设备/模拟器），
  下次真机测试请重点确认：单行高度下拉框点击是否顺手、溢出菜单展开位置在长列表滚动时是否被裁切。

### Stage Q（已完成，已验证）—— 历史记录行内三图标进一步收成"整行点击 + 单一更多菜单"
- **问题**：Stage P 上线后用户真机实测反馈，三个图标（打开/分享/更多）占了行宽很大一截，文件名被压得只剩
  开头几个字（如"《赵鹏-···"），可用信息太少；用户自己提出"三个图标合并成一个，点击后再展开"的方向，但也
  担心这样不够方便，要求按设计规范判断更合理的做法。
- **参考结论**：Google Files / Google Drive / WhatsApp 文档列表 / Telegram 媒体列表这类"历史/下载列表"场景
  的通行模式是——**整行可点击触发最高频操作（通常是"打开"），行尾只留一个"更多"溢出图标**，不会同时摆
  打开+分享+更多三个独立图标；"打开"作为最高频操作直接绑定到整行点击后，不需要再单独占一个图标位，因为
  全行可点击的目标区域远大于一个 48dp 图标，反而比小图标更好点。
- **`HistoryComponents.kt` 改动**：
  - `HistoryRecordCard` 把 `Card(modifier=...)` 换成 Material3 的可点击 `Card(onClick = { onOpen(outputUri) }, ...)`
    重载（仅当 `outputUri != null && !isActive` 时使用该重载，否则退回普通不可点击 `Card`），整行点击直接
    打开转换结果；原先单独的"打开"`IconButton` 随之删除。
  - `HistoryQuickActions` 从"打开+分享+更多三图标"精简成**只有一个 `MoreVert` 溢出图标**，菜单项变为
    `分享/查看位置/删除结果/再次转换/删除记录`（"打开"已经由整行点击覆盖，不再重复出现在菜单里）。
  - `HistoryRecordSummary` 的文件名从强制单行省略改为 **最多 2 行**（`maxLines = 2`），并把"状态文字"从
    跟文件名同一行的右侧挪到单独一行（不再跟文件名抢同一行的横向空间），两者共同让文件名在恢复出来的
    宽度里能显示更多内容。
  - **关于"超出显示区域自动滚动（跑马灯）"**：评估后没有采用。跑马灯一般只用在"当前播放"这类单一焦点文本
    （如音乐播放器的播放条标题），而不是用在一个会同时渲染多条记录的可滚动列表里——多条记录同时做持续滚动
    动画在视觉上会显得杂乱、分散注意力，也违反"动画只负责解释变化"的原则，对依赖辅助功能或对动效敏感的用户
    也不友好。改用 2 行换行 + 必要时省略号，是同类列表更常见也更稳妥的处理方式。
- **未改动**：`ProgressComponents.kt`（转换完成页）本轮未动——这次用户反馈与截图都明确指向"历史"页，
  完成页在 Stage O 已经做过一轮压缩，为避免无依据的范围扩大，本轮没有顺带改它；如果之后也希望完成页做到
  同样的"整行点击+单一更多菜单"，可以照搬本次思路单独跟进。
- 验证：`gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` 均通过。**仍未做真机视觉
  复核**，下次真机测试建议重点确认：可点击 `Card` 的点击反馈（ripple）范围是否符合预期、2 行文件名在长列表
  里整体高度是否确实比 Stage P 更紧凑。

### Stage R（已完成，已验证）—— 配色系统补全中性表面角色 + 强调色预览取色修正 + 导航转场动画
- **问题 1：底部导航栏/列表卡片背景偏粉，且不随强调色变化**。排查后定位根因：`core/designsystem/Color.kt`
  的 `BaseLightColorScheme`/`BaseDarkColorScheme` 只显式覆盖了 `background/surface/surfaceVariant/outline/
  error` 等角色，**没有覆盖 `surfaceContainer` 系列（`surfaceContainerLowest/Low/.../Highest`、`surfaceDim`、
  `surfaceBright`、`inverseSurface`、`inverseOnSurface`）**——这些角色没指定时会落回 Compose Material3
  `lightColorScheme()`/`darkColorScheme()` 工厂函数自带的基线配色（Material baseline 默认偏紫粉调），
  与项目自己的强调色逻辑（`accentColorScheme`）完全无关，因此切换强调色时这块背景纹丝不动——
  跟用户反馈完全对应。`NavigationBar` 默认 `containerColor` 和 `Card` 默认 `containerColor` 都读的是这组
  `surfaceContainer*` 角色，因此底栏和历史卡片同时受影响。
  **修复**：在 `BaseLightColorScheme`/`BaseDarkColorScheme` 里用中性灰阶显式补全这一整组角色（深浅两套各
  9 个值，延续既有 background/surfaceVariant 的灰阶序列），不再继承 Material3 的紫粉基线默认值；
  `tertiary` 系列因全项目无任何调用，本次未处理（避免无依据的范围扩大）。
- **问题 2：设置页强调色预览圆圈颜色和实际生效颜色有明显色差**。定位到 `core/designsystem/Accent.kt` 的
  `accentSwatchColor()` 此前**永远只取 `lightAccent(accent).primary`，不感知当前深浅主题**，而实际生效的
  `ColorScheme.primary` 在深色模式下来自 `darkAccent()`——深浅两张表数值差异很大（如 BLUE 浅色 `#1565C0`
  深色 `#9FCBFF`），跟用户描述的"预览深、实际浅"完全对应。**修复**：`accentSwatchColor` 改为接收
  `dark: Boolean` 参数，和 `accentColorScheme` 用同一套 `lightAccent`/`darkAccent` 表，调用处
  （`SettingsSections.kt`）按 `settings.themeMode` 解析出实际深浅再传入，新增 `resolveDarkTheme()`
  辅助函数（`Theme.kt`）让 `SwiftFormatTheme` 和设置页复用同一份"主题模式→是否深色"判定逻辑，避免后续
  再出现两处独立判断不一致。顺带给 `accentColorScheme` 补了 `inversePrimary`（之前也会落到 Material3
  基线默认值，同样会带紫粉色），取对侧明暗模式的 accent primary，保证在反色表面（如 Snackbar 按钮）上
  仍随强调色变化。
- **问题 3：导航切换动效**。用户提供了图片/视频/音频这类"同级 Tab"该用 `HorizontalPager` 平移的资料，
  但本项目"转换/历史/设置"三个目的地是**底部 `NavigationBar`**（`SwiftFormatApp.kt`），不是并列的 Tab——
  用户给的资料原文末尾也明确给出了这个区分：底部导航的多个顶级目的地之间没有固定左右顺序，不适合整页
  平移，更适合"淡入淡出或轻微缩放"，因此没有引入 `HorizontalPager`，而是按 Material motion 规范里
  "Fade through"（先淡出再淡入+轻微放大）模式实现：退出 90ms 纯淡出，进入延迟 90ms 后 210ms
  淡入+从 0.92 缩放到 1.0，三个目的地之间切换共用同一套转场。另外"转换进度"页是推入式全屏页（比底部导航
  深一层，语义上更接近"列表进入详情"），单独给它配了从右侧滑入/滑出 300ms 的前进式转场，跟底部 Tab 之间的
  fade-through 区分开。全部用 Navigation Compose 的 `enterTransition`/`exitTransition`/`popEnterTransition`/
  `popExitTransition`（`NavHost` 级默认 + `composable()` 单独覆盖进度页），未引入手势驱动的 Pager，
  风险和改动面都比较小。
- 验证：`gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` 均通过。**仍未做真机
  视觉复核**，下次真机测试请重点确认：①底栏/卡片背景在浅色/深色模式下是否确实不再偏粉、且随强调色切换；
  ②强调色预览圆圈在深色模式下是否跟实际按钮颜色一致；③底部三个 Tab 切换与"转换进度"推入页的转场是否
  顺滑、有没有意外的闪烁或方向感不对的情况。

### Stage S（已完成，已验证）—— 首页"已选文件"页头部精简 + 参数行改为下拉框样式
- **问题 1：选中文件后的头部三件套（"已选 N 个 · 体积"文字 / "清空全部" / "添加更多文件"两个按钮）占地方且拥挤**，
  窄屏下两个按钮经常挤到换行。按用户要求改成标准 `TopAppBar`（`HomeContent.kt` 的 `FileList`）：
  - `navigationIcon` 用返回箭头，点击直接调用原来的 `onClear`——这个页面本身没有"上一级"可以真正返回，
    用户的意图是"点返回=清空选择回到初始引导态"，跟原来的"清空全部"按钮是同一个回调，只是换了个更省地方
    的图标入口，`contentDescription` 沿用既有的 `home_clear` 字符串。
  - `actions` 放一个"+"`IconButton` 对应"添加更多文件"，沿用既有的 `home_add_more` 字符串作
    `contentDescription`，不再是占一整块的实心按钮。
  - "已选 N 个 · 体积" 文字保留在 `TopAppBar` 的 `title` 里（单行省略号），信息没有丢，只是从一个独立的
    `LazyColumn` item 挪进了固定不滚动的头部。
  - 空状态（未选文件时的引导页）本轮未动，仍是原来没有 `TopAppBar` 的居中引导布局——这次反馈明确针对
    "选完文件之后"的页面，空状态不在范围内。
- **问题 2：每个分组卡片里"输出格式/质量/尺寸"三行纵向铺开太占空间**。参考用户给的截图（类似电商筛选栏的
  三个并排下拉框样式），把原来纵向堆叠的 3 个 `SettingRow`（每个一整行）改成横向一排 3 个等宽
  `DropdownSettingChip`（`GroupSettingsCard.kt`）：每个芯片上方是 2 字小标签（尺寸/质量/格式），下方是带
  边框圆角的"当前值 + ▾"，点击仍然打开原有的 `ModalBottomSheet` 选择器——**只换外观，没有改交互机制**，
  不是新做一套 `ExposedDropdownMenu`。顺序按用户要求改成尺寸→质量→格式（原来是格式→质量→尺寸）；
  "输出格式"标签统一缩成"格式"（中英文 `row_output_format` 字符串资源同步改短，英文 "Output format"→
  "Format"），跟"质量"/"尺寸"保持两字风格一致。条件显隐逻辑（无质量/尺寸档位的格式不显示对应芯片）
  完全保留，仍用 `weight(1f)` 让剩余芯片自动占满整行。
- **未改动**：`OptionsBottomSheet` 的选择器本身、`onFormatChange`/`onQualityChange`/`onSizeChange` 等业务
  回调签名、`FileRow`（每个文件的缩略图行）。
- 验证：`gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` 均通过；中英 `strings.xml`
  仍保持 152 key 同步（只改了 `row_output_format` 的文案，没新增字符串）。**仍未做真机视觉复核**，下次
  真机测试请重点确认：① TopAppBar 返回箭头在状态栏边缘的点击区域是否舒适；②三个下拉框芯片在小屏/长格式名
  （如 "WEBM"）下是否会挤得太紧或文字被截断。

### Stage T（已完成，已验证）—— 修复嵌套 Scaffold 重复留白 + 文案/排序微调
- **问题："开始转换"按钮下方有一大片空白，且上下不对称**。排查后定位到真实根因：Stage S 新增的
  `HomeContent.kt` `FileList` 内部 `Scaffold` 是**嵌套在外层 `SwiftFormatApp.kt` 的 `Scaffold`（带
  底部 `NavigationBar`）+ `NavHost` 里面的第二层 `Scaffold`**。外层 `Scaffold` 已经把底部系统导航条/手势区
  计算进 `innerPadding` 并应用给 `NavHost`，但内层 `Scaffold` 默认的 `contentWindowInsets` 不知道这件事，
  会**再算一遍**底部系统栏 inset 加到自己的 `innerPadding` 上——两层叠加正好就是按钮下方那块多出来的空白，
  且只有底部会叠加（顶部状态栏只由 `TopAppBar` 自己处理一次），所以看起来"上下不对称"。`HistoryScreen.kt`
  原本也是同样的嵌套 `Scaffold` 写法，存在一样的潜在重复留白（只是列表场景不像固定按钮那样直观可见）。
  **修复**：两个页面都去掉了内层 `Scaffold`，改成直接 `Column { TopAppBar(...); 内容（weight(1f)）}`——
  `TopAppBar` 本身就会处理顶部状态栏 inset，不需要外面再包一层 `Scaffold` 来重复保留底部 inset 空间。
- **文案："保持原始"统一简化为"原始"**。`size_original` 中文字符串资源（英文已经是简洁的 "Original"，
  未改）；同步检查并修正了 SPEC 里所有"作为 UI 标签字面引用"的"保持原始"出处（选项列表项 / 带引号的默认值
  描述 / 中英对照表 / 第22节默认值表，共 8 处），**保留了 2 处是描述行为的散文用法**（"竖屏视频保持原始
  方向和宽高比"、"默认保持原始尺寸"）未动，因为那两处不是 UI 按钮文案，改了反而读不通。
- **视频输出格式排序：WEBM 移到 MKV 后面**。`OutputFormatCatalog.kt` 的 `videoOptions` 表交换了 MKV(30)/
  WEBM(40) 的 `sortOrder`，不常用格式不再排在比较常用的 MKV 前面；同步更新了
  `OutputFormatCatalogTest.outputOptions_followPlannedStableOrder` 里断言的期望顺序。
- **关于"WEBP 能完整显示但 WEBM 会被截断"**：两个值都是 4 个字符，芯片宽度也相同，差异来自 **"M" 在大多数
  字体里比 "P" 宽**，临界情况下足够让 WEBM 多出几个像素触发省略号——不是某处宽度设置错了。已经把
  `DropdownSettingChip` 内部水平留白从两侧各 12dp 收紧到"左 10dp / 右 4dp"（图标本身有内边距，右侧不需要
  留太多），给文字腾出一点余量，但这类长度临界的格式名仍有可能在更窄的设备上触发省略号，省略号本身是
  正常的兜底显示而不是 bug。
- 验证：`gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` 均通过；中英 `strings.xml`
  仍保持 152 key 同步。**仍未做真机视觉复核**，下次真机测试请重点确认"开始转换"按钮下方的空白是否已经消失、
  上下间距是否对称。

### Stage U（已完成，已验证）—— 转换进度页对齐历史记录卡片的列表项样式
- **问题**：用户反馈转换完成后的进度页（`ConversionTaskRow`）还是改造前的旧样式（纵向堆叠的卡片），跟
  Stage P/Q 已经改过的历史记录列表项风格不一致。
- **`ConversionProgressViewModel.kt`**：给 `ConversionTaskUiItem` 新增 `mediaType: MediaType` 字段（取自
  `request.input.mediaType`），用于跟历史记录一样显示左侧媒体类型图标；同步更新了
  `ConversionProgressUiStateTest.kt` 里直接构造 `ConversionTaskUiItem` 的测试夹具。
- **`ProgressComponents.kt` 的 `ConversionTaskRow` 改造**：
  - 整行重排成"40dp 媒体类型图标 + 两行文字 + 行尾操作"，跟 `HistoryRecordCard` 同款结构（复用
    `feature.home.mediaIcon`）；文件名最多 2 行，状态文字只在非 `COMPLETED` 时显示。
  - 进度条 (`LinearProgressIndicator`) 和失败原因/"查看详情"挪进文字列内部，跟随文件名一起换行，
    不再额外占整张卡片宽度的一整行。
  - 已完成且结果未被删除时，**整行点击直接打开**（用 Material3 `Card(onClick = ...)` 重载），不再单独放
    "打开"图标，这点跟历史记录的处理完全一样。
  - 行尾操作按状态收紧：进行中只显示"取消"，失败只显示"重试"，已完成才有"更多"溢出菜单
    （分享/查看位置/删除结果/再次转换/删除原文件）——跟历史记录一样，"打开"被整行点击取代，不再重复出现
    在菜单里。
  - **行为未变**：取消/重试/再次转换/删除结果/删除原文件的具体回调和二次确认弹窗逻辑完全没动，只是
    重新排布了视觉结构和触发入口。
- **范围说明**：本次只改了单条任务行 `ConversionTaskRow`；页面顶部的整体进度条 `ProgressHeader`（总进度、
  取消全部、完成汇总文案）保持不变，未做调整。
- 验证：`gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` 均通过。**仍未做真机
  视觉复核**，下次真机测试请重点确认转换完成后的列表是否跟历史页视觉一致、整行点击打开是否符合预期。

### Stage V（已完成，已验证）—— 中文 README + GitHub Releases 测试包自动化
- **中文 README**：新增 `README.md`，参考用户提供的通用模板保留居中标题、徽章、目录、下载、使用指南、
  技术栈、开发指南、项目结构、路线图、FAQ 与许可证/开源组件说明；删掉官网、桌面端下载、网盘镜像等当前
  Android 项目并不存在的占位内容，链接落到 `henjicc/SwiftFormat` 仓库；下载按钮直连
  `releases/latest/download/swiftformat-arm64-v8a.apk`，方便普通安卓手机用户从 README 直接下载安装包。
- **发布 workflow**：新增 `.github/workflows/release.yml`，支持推送 `v*` tag 或手动填写 tag 触发；流程会
  设置 JDK 17、运行 `testDebugUnitTest lintDebug assembleDebug -Pswiftformat.abiSplits=true`，输出
  `arm64-v8a` / `armeabi-v7a` / `x86` / `x86_64` 四个 APK，生成 SHA256 校验文件，并通过
  `softprops/action-gh-release` 以 prerelease 形式上传到 GitHub Releases；产物文件名固定为
  `swiftformat-arm64-v8a.apk` 等，避免 README 直链随版本变化失效。
- **边界说明**：当前阶段按用户要求先发测试 APK，workflow 不需要 GitHub Secrets、release keystore 或签名密码；
  安装包适合私下测试和朋友体验，不建议作为长期正式版本。后续切换 release 签名包时，可能无法覆盖安装
  已安装的当前版本，需要先卸载。
- 验证：临时设置 `JAVA_HOME=C:\Program Files\Java\jdk-17.0.5` 后执行
  `gradlew.bat testDebugUnitTest lintDebug assembleDebug -Pswiftformat.abiSplits=true` 通过；本地产物包含
  `app-arm64-v8a-debug.apk`、`app-armeabi-v7a-debug.apk`、`app-x86-debug.apk`、`app-x86_64-debug.apk`。

### Stage W（已完成，已验证）—— 取消语义、当前进度边界与历史多选删除
- **修复“点取消后被标记为失败/重启后又恢复等待中”的根因窗口**：
  - `ConversionOrchestrator.cancel(taskId)` 现在会立即把内存任务与 Room 历史记录写成 `CANCELLED`，而不是只
    `Job.cancel()` 后等待协程稍后兜底；即使用户点完取消马上杀进程/重启，也尽量避免旧记录仍停在
    `PENDING/PREPARING/CONVERTING/SAVING` 后被恢复。
  - 新增 `cancellationRequests` 标记，若底层引擎在取消后仍返回成功/失败结果，编排层会优先以“已取消”收尾，
    不让迟到的引擎结果覆盖用户的取消意图。
- **把“当前转换进度”和“历史记录”边界切开**：
  - 新增 `progressTaskIds` 作为当前进度页任务集合。首页开始转换、历史“再次转换”、进程恢复都会显式设置当前集合；
    进度页、首页活跃任务卡片与前台服务通知都只展示/汇总这个集合，避免上一轮已结束或旧恢复任务混进当前页面。
  - 当没有显式当前集合时，进度相关 UI 只回退展示真正活跃任务，不再展示进程内所有旧任务。
- **历史页改为终态历史列表 + 长按多选删除**：
  - `HistoryViewModel` 现在过滤掉 `PENDING/PREPARING/CONVERTING/SAVING`，这些状态只属于进度页；历史列表只展示
    已完成/已取消/失败等终态记录，避免历史里出现“等待中/正在运行”的混乱感。
  - 历史页顶部“当前任务”入口改为基于编排器中的实时活跃任务计数，而不是基于 Room 历史里的旧活跃状态。
  - `HistoryRecordCard` 支持长按进入多选模式；选择模式下可点行或复选框切换选中，顶部显示已选数量并可批量删除记录。
- **回归测试**：`ConversionCrashSafetyTest` 新增取消时序测试，覆盖活跃任务取消后应立即进入 `CANCELLED` 并持久化。
- 验证：`gradlew.bat compileDebugKotlin`、`gradlew.bat testDebugUnitTest`、`gradlew.bat lintDebug`、
  `gradlew.bat assembleDebug` 通过（使用本机 JDK 17 设置 `JAVA_HOME` 执行）。**仍未做真机视觉复核**，下次真机测试
  请重点确认：点取消后状态是否稳定为“已取消”；重启后新添加文件进入进度页是否只显示本轮文件；历史页长按多选删除
  是否符合手感预期。

### Stage X（已完成，已验证）—— 修复无活跃任务时的假前台通知
- **问题**：用户实测安装/打开后，明明只有之前处理过的历史文件，系统通知却显示“正在转换 0 个文件 · 100%”。
- **根因**：
  - Stage W 把进度集合收窄到 `progressTaskIds` 后，`ConversionForegroundService.onStartCommand()` 仍然无条件
    `startForeground(...)`。
  - 如果当前集合里只有旧的已完成任务，通知构造会得到 `activeTasks.size == 0`，但总体进度按终态任务计算为
    100%，于是出现“正在转换 0 个文件 · 100%”。
  - 原本用于避免“服务第一帧空任务就自停”的 `!hasObservedActiveTask` 保护，又会在这种“从来没有活跃任务”的场景
    阻止服务自停，导致假通知残留。
- **修复**：
  - 新增 `NotificationTaskSnapshot` / `notificationSnapshot(...)`，明确区分“已有活跃任务”“刚提交但任务对象尚未入队的
    占位任务”和“只有终态历史任务”。
  - `onStartCommand()` 若发现没有真实活跃工作，直接 `stopSelf()`，不再进入前台通知；若刚提交任务但运行态尚未创建，
    用 `progressTaskIds` 的缺失项作为短暂占位，避免新转换刚开始时服务过早自停。
  - `onTasksChanged()` 在已进入前台但从未观察到活跃任务时，会移除前台通知并停止服务，不再保留“0 个文件”通知。
- **回归测试**：新增 `NotificationTaskSnapshotTest`，覆盖旧完成任务不算活跃、缺失当前任务 id 算占位、无显式当前集合时只取
  真正活跃任务三种场景。
- 验证：串行执行 `gradlew.bat compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` 通过（使用本机 JDK 17 设置
  `JAVA_HOME`）。曾并行执行 Gradle 时触发 Kotlin incremental cache 的 `Storage ... already registered` 噪声，改串行后
  验证干净通过。

### Stage Y（已完成，已验证）—— 去除进度条末端 stop indicator 圆点
- **问题**：进度页顶部总进度条与任务卡片内进度条右侧会显示一个蓝色小圆点，用户真机截图中看起来像多余的脏点。
- **根因**：当前 Material3 `LinearProgressIndicator` 默认会绘制 `drawStopIndicator`，在本项目这种转换进度条里没有
  明确语义，且与轨道断点一起出现时容易被误解为 UI 错误。
- **修复**：
  - 新增 `ConversionLinearProgressIndicator` 小封装，内部统一调用 `LinearProgressIndicator(..., drawStopIndicator = {})`。
  - 顶部总进度条和单任务行进度条都改用该封装，保持原进度值、颜色、尺寸与布局不变，只去掉末端圆点。
- 验证：`gradlew.bat compileDebugKotlin`、`gradlew.bat testDebugUnitTest lintDebug assembleDebug` 通过（使用本机
  JDK 17 设置 `JAVA_HOME` 执行）。仍未做真机截图复核，下次安装包请重点确认两处进度条右侧圆点已消失。

### Stage Z（已完成，已验证）—— 修复设置页多语言切换不生效
- **问题**：设置页选择语言后，`AppSettings.language` 已写入 DataStore，但界面语言没有按预期切换。
- **根因**：应用调用的是 AppCompat per-app language API（`AppCompatDelegate.setApplicationLocales`），但
  `MainActivity` 继承自 `ComponentActivity`，没有 AppCompat activity delegate 参与资源上下文与重建流程；
  同时 Compose 初始收集值使用占位 `AppSettings()`，可能在真实 DataStore 值到达前短暂把应用语言清回“跟随系统”。
- **修复**：
  - `MainActivity` 改为继承 `AppCompatActivity`，让 AppCompat locale delegate 正常接管应用级语言切换。
  - 设置流在 Compose 中先以 `AppSettings?` 收集，只有真实 DataStore 设置到达后才调用 `AppLocaleManager.apply(...)`，
    避免初始占位值覆盖用户已选语言。
  - `AppLocaleManager.apply(...)` 增加当前 locale 对比，语言未变化时直接返回，减少无意义的重复重建。
- 验证：`gradlew.bat compileDebugKotlin`、`gradlew.bat testDebugUnitTest lintDebug assembleDebug` 通过（使用本机
  JDK 17 设置 `JAVA_HOME` 执行）。仍需下一次真机安装后确认：设置页在中文/英文/跟随系统之间切换时主界面立即刷新。

### Stage AA（已完成，已验证）—— 去除语言切换时的 Activity 重建黑屏
- **问题**：Stage Z 改为 AppCompat per-app language 后，语言切换虽然生效，但真机上会短暂黑屏一下。
- **根因**：`AppCompatDelegate.setApplicationLocales(...)` 会触发 Activity 重建；对纯 Compose 页面来说，
  这属于过重的刷新方式，用户能看到启动窗口/空白帧。
- **修复**：
  - 新增 `AppLocaleProvider`，在 Compose 树内提供本地化 `LocalContext` 与 `LocalConfiguration`，让
    `stringResource(...)` 读取目标语言资源并随 DataStore 设置重组。
  - `AppLocaleManager` 改为创建仅覆盖 `Resources`/`Assets` 的 `ContextWrapper`：资源读取走本地化 context，
    `startActivity`、`contentResolver`、`applicationContext` 等能力仍委托给原 Activity，避免影响文件选择、
    分享反馈、打开结果等依赖 `LocalContext` 的流程。
  - `MainActivity` 退回 `ComponentActivity`，不再调用 AppCompat 语言 API，因此切换语言不再触发 Activity 重建。
- 验证：`gradlew.bat compileDebugKotlin`、`gradlew.bat testDebugUnitTest lintDebug assembleDebug` 通过（使用本机
  JDK 17 设置 `JAVA_HOME` 执行）。仍需真机复核：设置页切换中/英时应只更新文案，不再出现黑屏。

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
