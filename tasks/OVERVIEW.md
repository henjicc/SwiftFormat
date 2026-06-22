# 转个格式 · 项目总览（任务管理主文件）

> 本文件是项目进度与下一步工作的**唯一主依据**。
> 每次开始工作前先读本文件，完成任何任务后立即回写「任务状态」与「当前进度」。
> 任务详情见同目录下 `TASK-0X.md`。

---

## 1. 项目目标

“转个格式”是一款**本地 Android 文件格式转换工具**，处理图片、视频、音频。
核心体验：用户**无需先分类、无需理解编码参数**，选择文件后应用自动识别并按类型分组，
只需设置「输出格式 / 质量 / 尺寸」三个统一概念即可一键批量转换，全部本地完成。

完整需求见 `ref/转个格式_Android开发SPEC.md`（规格说明书 v1.0，权威来源）。

---

## 2. 全局约束（所有任务必须遵守）

### 产品原则
- 不要求用户理解专业术语（CRF/码率/采样率/Profile 等一律不暴露给 UI）。
- 自动识别媒体类型，按 视频 / 图片 / 音频 三组展示，无该类则不显示该组。
- 默认不覆盖、不删除原文件；删除原文件需明确二次确认且默认关闭。
- 所有文件操作围绕 Android `Uri` 设计，不以绝对路径为核心数据。
- 默认全部本地处理，不上传、不登录、不申请“管理所有文件”权限。

### 架构约束
- **引擎与 UI/业务完全隔离**：UI/业务层禁止拼接 FFmpeg 命令或直接调用编码器。
- **多引擎**：原生图片能力 + Media3 Transformer + 隔离的 FFmpeg 模块，由调度器按
  「输入格式 / 输出格式 / 设备能力」自动选择，不用单一引擎处理所有文件。
- 统一参数模型：UI 仅使用 `OutputFormat / QualityPreset / SizePreset`，
  底层负责映射为各媒体类型真实参数（见 SPEC 第 5 章）。
- 分层：UI → 业务/任务编排 → 转换能力抽象 → 引擎实现 → Android 文件/媒体系统。
- 依赖方向单向，底层不反向依赖上层。
- 第一版用**单 app 模块 + 清晰包结构**，稳定后再拆 Gradle 模块，初期不过度模块化。

### 多语言 / 主题
- 所有可见文本来自字符串资源，禁止 Compose 内硬编码文案。
- 中文 `values-zh-rCN/strings.xml`、英文 `values/strings.xml`；语言/主题存 DataStore。
- 主题：浅色/深色/跟随系统；默认强调色蓝色；动态配色默认关闭、可选开启。

### 错误与隐私
- 错误必须显式传播并向用户展示可理解原因，不向普通用户暴露堆栈；技术细节藏在“查看详情”。
- 批量任务单个失败不终止其他任务，最终汇总成功/失败数。
- 日志禁止记录文件正文、完整私密路径、用户媒体数据。

---

## 3. 技术方案

| 模块 | 选型 |
|---|---|
| 语言 / UI | Kotlin · Jetpack Compose · Material 3 · Edge-to-edge |
| 状态 / 导航 | ViewModel + StateFlow · Navigation Compose |
| 持久化 | DataStore（设置） · Room（历史） |
| 文件输入 | SAF / Photo Picker / 分享 Intent + ContentResolver |
| 文件输出 | MediaStore / ACTION_CREATE_DOCUMENT |
| 图片引擎 | ImageDecoder / Bitmap.compress（+ HeifWriter 设备支持时） |
| 音视频引擎 | Jetpack Media3 Transformer |
| 扩展格式 | 隔离的 FFmpeg 模块（仅此模块生成命令） |
| 后台 | Foreground Service（转换） + WorkManager（重试/清理等辅助） |
| 依赖注入 | Hilt（推荐） |

### 设计语言（来自 `ref/stitch_pure_media_converter/`）
- 风格：MD3 + 极简，大量留白，文件/任务状态优先，原生 Android 观感。
- 参考 `fluid_exchange/DESIGN.md` 的色板/字体/圆角/间距 token；
  **注意**：SPEC 规定默认强调色为**蓝色**，design token 中的薄荷绿/teal 仅作版式与组件
  形态参考，最终配色以 SPEC（蓝色默认 + 可选预设色）为准。
- 4 张 UI 稿（`_1`~`_4`）对应：首页上传 / 转换设置列表 / 处理进度+已完成 / 设置页。
  UI 稿用英文占位（FlexConvert/Download 等），实现需按 SPEC 文案规范本地化
  （如完成页用「打开/分享/查看位置」而非「下载」）。

### 与现状的偏差（需在 TASK-00 处理）
- 现有骨架为 **appcompat + Material Views**，需改造为 **Compose + Material 3**。
- 现 `minSdk = 24`，SPEC 建议 **`minSdk = 26`**：见 TASK-00「待决策」。
- 包名 `com.henjicc.swiftformat`，应用名 `转个格式`，保持不变。

---

## 4. 任务列表

| 编号 | 任务 | 依赖 | 状态 |
|---|---|---|---|
| [TASK-00](./TASK-00.md) | 工程基础（Compose/主题/导航/DataStore/日志） | — | 已完成* |
| [TASK-01](./TASK-01.md) | 文件选择与识别（SAF/分享/元数据/分组/缩略图） | 00 | 已完成* |
| [TASK-02](./TASK-02.md) | 参数页面（分组卡片 + 统一参数组件 + 动态显示） | 01 | 已完成* |
| [TASK-03](./TASK-03.md) | 图片引擎（JPG/PNG/WebP + 质量/尺寸映射） | 00,02 | 已完成* |
| [TASK-04](./TASK-04.md) | 音视频引擎（Media3 + 进度/取消/能力检查） | 00,02 | 已完成* |
| [TASK-05](./TASK-05.md) | FFmpeg 兼容层（隔离模块/许可证/16KB/降级） | 03,04 | 已完成* |
| [TASK-06](./TASK-06.md) | 后台任务与历史（前台服务/通知/Room/完成页） | 03,04 | 已完成* |
| [TASK-07](./TASK-07.md) | 质量与发布（多设备/性能/i18n/无障碍/发布准备） | 全部 | 进行中 |

状态取值：`未开始` / `进行中` / `阻塞` / `已完成`。
> *TASK-00 功能性完成（Compose 底座、DataStore 设置、构建通过）；当时 Room/Hilt 因 KSP 与 AGP9 工具链
> 冲突显式移交 TASK-06，该冲突已在 TASK-06 Stage A 解决（升级 KSP 至 2.3.9）。详见 TASK-00「Stage C」。
> *TASK-01 功能性完成（构建/单元测试通过）；50+ 文件、分享菜单、缩略图视觉效果待实机验证，不阻塞 TASK-02。
> *TASK-02 功能性完成（构建/单元测试通过）；自定义尺寸输入框、显隐过渡动画、WebP 有损/无损区分、
> 设备编码能力检查为已知简化，留给 TASK-03/04 引擎接入时处理，不阻塞后续。
> *TASK-03 功能性完成（构建/单元测试 26/26 通过）；引擎尚未接入 AppContainer/UI（无调用方，
> 接线是 TASK-06 的工作）；输出位置解析（MediaStore/SAF）留给 TASK-06；实机运行未验证。
> *TASK-04 功能性完成（构建/单元测试 39/39 通过，Media3 真实 API 已用 javap 反编译核实而非凭空记忆）；
> 重要发现：①视频"短边"缩放与旋转无关，比图片引擎更简单；②Android MediaMuxer 不支持 WAV 容器，
> 已修正任务描述，WAV 移交 TASK-05。设备能力→UI 动态显隐、静音/提取音频、WebM、并发策略均明确推迟。
> *TASK-05 功能性完成（构建/单元测试 45/45 通过）；官方 ffmpeg-kit 已于 2025-04 退役下架，改用社区
> 维护的 16KB fork（`JamaisMagic/ffmpeg-kit-16KB` non-GPL full 变体，LGPL-3.0，已用 javap 核实 API、
> 解压 aar 确认四 ABI 原生库与许可证字段，非凭空采信）；后续扩展已补齐
> `IMAGE -> BMP/TIFF`、`AUDIO -> OGG`、`VIDEO -> MOV`、`VIDEO -> M4A/WAV/FLAC/MP3/WEBM/MKV`，
> 并新增 `FfmpegStillImageEngine` 与 `FFprobeKit` / 图片 bounds 输出校验；图片链路后续也已补齐
> `HEIC / AVIF` 输出（基于 AndroidX `HeifWriter` / `AvifWriter`，非 FFmpeg 路径）；
> 当前仍未做 16KB 真机验证、APK 体积评估、硬件加速验证与 HEIC/AVIF 真机覆盖，详见 TASK-05「已知简化」。

### 依赖关系图
```
00 ─► 01 ─► 02 ─┬─► 03 ─┐
                └─► 04 ─┼─► 05
                        └─► 06
03,04,05,06 ───────────────► 07
```

---

## 5. 当前进度

- **总体阶段**：TASK-00～TASK-06 均功能性完成；TASK-07 已开始，正在补真机测试中暴露出的稳定性与质量项。
- **已完成**：项目资料分析；任务目录创建；TASK-00（Compose 底座、DataStore 设置/主题/语言持久化、核心数据模型、
  Logger；KSP/AGP9 工具链冲突已探测并记录，Room/Hilt 移交后续）；
  TASK-01（SAF 多选 + Uri 权限持久化、分享 Intent 接收、媒体类型识别含单元测试、IO 线程元数据读取、
  按类型分组的首页文件列表、Coil3 缩略图加载与缓存）；
  TASK-02（统一参数模型 `GroupConversionSettings`/`OutputFormatCatalog`、分组卡片 + 统一设置行 +
  Modal Bottom Sheet 选择器、按格式动态显隐质量/尺寸、参数状态随分组持久化）；
  TASK-03（`engine/api` 抽象 + `ConversionEngineSelector`、`NativeImageEngine` 解码/EXIF 旋正/采样缩放/
  压缩写出、质量与尺寸纯函数映射、输出命名规则）；
  TASK-04（`Media3Engine`：视频→MP4(H.264)/音频→AAC/M4A，`Presentation` 缩放、`VideoEncoderSettings`/
  `AudioEncoderSettings` 码率控制、临时文件→目标 Uri 拷贝、进度轮询、取消、设备编码器能力检查）；
  TASK-05（`FfmpegEngine` / `FfmpegStillImageEngine`：图片→BMP/TIFF、音频→MP3/OGG/FLAC/WAV、
  视频→MOV/WEBM/MKV、视频提取→MP3/M4A/WAV/FLAC，社区维护的 16KB fork 替代已退役的
  官方 ffmpeg-kit；`OutputFormatCatalog` 升级为结构化输出选项并新增 `sortOrder`，
  `ConversionRequest` / `GroupConversionSettings` 新增 `targetMediaType`，同时补了图片 `ImageDecoder`
  回退与 `FFprobeKit`/图片 bounds 输入输出校验），`testDebugUnitTest`、`lintDebug`、`assembleDebug`
  通过，当前 JVM 单元测试总数 85；
  TASK-06 Stage A（**解决 KSP/AGP9 工具链阻塞**——升级 KSP 到 2.3.9 后与 AGP9 内置 Kotlin 兼容，
  已用真实 Room `@Database`/`@Dao` 验证注解处理可生成代码；新增 `core/database` 历史数据层
  `ConversionHistoryEntity`/`Dao`/`Database`/`Repository` 接入 `AppContainer`，新增
  `ConversionStatus`/`ConversionHistoryRecord`/`SizePresetCodec`）；
  TASK-06 Stage B（新增 `conversion` 任务编排层 `ConversionOrchestrator`：队列/按媒体类型并发限流
  `Semaphore`/状态机推导/批量汇总/取消/重试，全部回写 Room 历史；新增 `OutputLocationResolver`
  统一写入 `Download/转个格式` 并用 `Mutex` 串行化解决同批次重名竞态；**三个引擎首次接入
  `AppContainer`/`ConversionEngineSelector`**，收尾 TASK-03/04/05 长期标注的"引擎尚未接入"遗留项）；
  TASK-06 Stage C（新增 `service/ConversionForegroundService`：只做保活+通知+取消，调度逻辑仍在
  `ConversionOrchestrator`；通知含总体进度/当前文件/取消全部/点击返回；覆写 API35+ `onTimeout`；
  manifest 声明 `mediaProcessing` 前台服务类型与三个权限）；
  TASK-06 Stage D（`HomeScreen`「开始转换」按钮接线：`HomeViewModel.startConversion()` 按分组提交到
  `ConversionOrchestrator`，按钮点击同时启动 `ConversionForegroundService` 并按需请求
  `POST_NOTIFICATIONS` 运行时权限——首页 → 编排 → 历史落库 → 通知的链路首次打通）；
  TASK-06 Stage E（应用内转换进度页 `feature/progress`：总体进度、当前文件、单文件状态/进度、取消/重试，
  并在全部结束后提供 `打开 / 分享 / 查看位置 / 再次转换 / 删除结果 / 删除原文件` 完成态操作）；
  TASK-06 Stage F（历史页 `feature/history`：读取 `ConversionHistoryRepository` 展示状态/时间/输出大小/
  质量尺寸标签/失败原因，支持 `打开 / 分享 / 查看位置 / 再次转换 / 删除结果 / 删除记录`，且有活跃任务时
  提供“查看进度”入口）；
  TASK-06 Stage G（应用启动时基于 Room 活跃历史记录做任务恢复、恢复后自动拉起前台服务、WorkManager 清理
  `media3_*`/`ffmpeg_*` 缓存残留文件）；
  TASK-07 Stage A（设置页扩展：图片/视频/音频默认质量、自动清理临时文件、清理缓存、版本展示；
  运行时语言切换落地到 `AppCompatDelegate.setApplicationLocales(...)`；
  选项布局改为更适合小屏/大字体的 `FlowRow`；自动 locale config 生成已开启），
  TASK-07 Stage B（进度页/历史页失败任务增加“查看详情”弹窗；设置页“关于”分区新增隐私说明与开源组件说明）；
  TASK-07 Stage C（设置页“文件”分区新增保存位置/重名处理说明、完成通知开关；前台服务支持完成汇总通知；
  设置页新增基础日志查看入口）；
  TASK-07 Stage D（设置页新增“分享反馈信息”，可把版本/主题/语言/最近日志通过系统分享面板导出）；
  TASK-07 Stage E（失败原因结构化：进度页/历史页显示友好失败文案并保留“查看详情”；首页新增“有任务正在转换”
  卡片，可直接回到进度页；补 `FailureReasonCodecTest` 覆盖多行详情与旧数据兼容）；
  TASK-07 Stage F（补齐前台服务通知权限检查、`OutputLocationResolver` 低版本 MediaStore 兼容分支、
  Media3 `UnstableApi` 的 lint opt-in，`lintDebug` 恢复通过），
  TASK-07 Stage G（按代码体量与职责密度拆分 4 个大 Screen：`SettingsScreen`/`HomeScreen`/
  `ConversionProgressScreen`/`HistoryScreen` 改为“路由 + 组件”结构；并把 `ConversionOrchestrator`
  的请求解析/历史同步、`Media3Engine` 的编码配置/Transformer 生命周期/错误映射抽到独立文件），
  TASK-07 Stage H（根据真机反馈修复“开始转换即闪退、重启后因活跃任务恢复继续闪退”的致命链路：
  `ConversionOrchestrator` 对引擎/历史收尾异常新增兜底并统一转为 `ENGINE_CRASH` 失败，
  `ConversionRecoveryManager` 对单条恢复失败直接标记历史失败，`ConversionForegroundService.start()`
  改为捕获启动异常后降级为仅无通知不崩溃，`SwiftFormatApplication` 启动恢复新增总兜底日志，并补
  `ConversionCrashSafetyTest` 回归测试），
  TASK-07 Stage I（针对 vivo / Android 16 上 `FFmpegKit failed to start on brand ...` 这类 native 启动失败，
  新增 `FfmpegRuntimeSupport` 启动探测与 `ThrowableDebugFormatter`，让 FFmpeg native 初始化失败能提前降级为
  明确任务失败，并在“查看详情”里保留 `Error -> UnsatisfiedLinkError/dlopen failed` 的 cause 链），
  FFmpeg 全媒体扩展第一阶段（图片组选项扩为 `JPG / PNG / WEBP / BMP / TIFF`，音频组选项扩为
  `MP3 / M4A / AAC / WAV / FLAC / OGG`，视频组选项扩为 `MP4 / MOV / WEBM / MKV / MP3 / M4A / WAV / FLAC`，
  其中 `MP3 / M4A / WAV / FLAC` 走“视频提取音频”链路，新增 `FfmpegStillImageEngine`、
  `UNSUPPORTED_IMAGE_OUTPUT` / `NO_AUDIO_TRACK` / `UNSUPPORTED_VIDEO_OUTPUT` /
  `OUTPUT_VALIDATION_FAILED` 友好错误类型），
  图片输出扩展第二步（新增 `HEIC / AVIF` 输出，接入 AndroidX `heifwriter`，新增
  `HeifAvifImageEngine`，图片组选项顺序扩为 `JPG / PNG / WEBP / BMP / TIFF / HEIC / AVIF`，
  并补齐 MIME、命名、路由与不支持错误映射），
  TASK-07 Stage H（修复“点击开始转换后闪退 + 重启恢复继续闪退”的严重稳定性问题，恢复路径与前台服务启动
  均改为有兜底、可失败但不致命，并新增 `ConversionCrashSafetyTest` 回归覆盖），
  TASK-07 Stage I（FFmpeg 运行时启动失败诊断增强：新增 startup probe 与 cause 链格式化，让
  `FFmpegKit failed to start on brand ...` 能进一步看到底层 `UnsatisfiedLinkError/dlopen failed` 详情），
  `assembleDebug`、`testDebugUnitTest` 与 `lintDebug` 通过。
- **下一步**：真机测试（用户已实测，开始转换/崩溃恢复/取消/通知/分享打开查看位置等核心链路基本无问题，
  后续若再发现问题随时反馈）与 GPL 合规均已处理完毕。SPEC 15 设置页剩余项中，「默认保留图片元数据」
  已实现（2026-06-22，新增 `AppSettings.preserveImageMetadata` 开关，默认开启；`NativeImageEngine`
  转 JPG 时通过 `ImageDecodeCompat.copyExifMetadata` 复制拍摄时间/相机型号/GPS 等标签，方向标签固定写回
  `ORIENTATION_NORMAL`；其余引擎不输出 JPG，无需改动），「动态配色开关」「默认保持原始尺寸」「默认保留
  视频音频轨道」核查后确认已有实现满足（详见 [TASK-07](./TASK-07.md)），「正式问题反馈入口」判断现有
  「分享反馈信息」机制已足够、无需新增。继续 [TASK-07](./TASK-07.md) 剩余收尾，按以下顺序推进：
  ①可配置默认输出目录（当前硬编码 `Download/转个格式`，需 SAF 目录选择）与可配置重名策略（当前硬编码
  自动追加序号）；②国际化逐项核对（无未翻译文案、档位语义跨语言一致）与无障碍验收
  （TalkBack、超大字体、对比度、横竖屏）；③体积与发布收尾（ABI 拆分/App Bundle 评估、清理 lint
  warning、许可证与隐私说明发布版核对）。
- **遗留**：TASK-02/03/04/05 的已知简化项（自定义尺寸输入框、显隐动画、WebP 有损/无损、设备编码能力检查未接入
  UI 动态显隐、EXIF 完整标签保留、动图 GIF、HEVC/AV1、视频→OGG/Opus、APK 体积/ABI
  拆分评估）见各任务完成情况；`ConversionOrchestrator` 未做单元测试（依赖 Room/协程时序，已把可抽出的
  判定逻辑拆成纯函数单测，编排本身运行时行为已经真机验证通过）；输出统一写入
  `Download/转个格式`未按媒体类型分相册/视频/音乐 MediaStore 分类；取消为"双重保证"非事务性保证；
  16KB 页面设备、WorkManager 清理等边缘场景仍缺逐项专门验证，但不阻塞当前迭代。
- **未解决问题 / 风险**：
  - minSdk 已定为 26（已决策）。
  - **KSP/AGP9 工具链阻塞已解决**（TASK-06 Stage A）：升级 `com.google.devtools.ksp` 到 `2.3.9`
    后与 AGP 9.2.1 内置 Kotlin 兼容，Room 已接入并验证可用；Hilt 同理不再受阻，但暂无新增必要性，
    继续用手动 `AppContainer`。详见 TASK-00「Stage C」更新与 TASK-06「Stage A」。
  - **FFmpeg 依赖为社区维护 fork，非官方项目**：官方 `arthenica/ffmpeg-kit` 已退役下架。原选型
    `JamaisMagic/ffmpeg-kit-16KB`（main-full-16kb）已确认 **`libavdevice.so` 在其所有变体上都
    引用了不存在的 `PLATFORM_hid_*` 符号**，`NativeLoader` 无条件加载 avdevice 且无降级路径，
    导致 `FFmpegKitConfig` 在任何设备上都必然初始化失败——已换用 `com.moizhassan.ffmpeg:ffmpeg-kit-16kb`
    （API 与 `com.arthenica.ffmpegkit.*` 完全一致，零代码改动）。详见 TASK-05「选型决策」更新。
  - **新换上的 FFmpeg 依赖同样静态链接了 x264（GPL）**：排查替代依赖时发现，包括原依赖和新依赖在内的
    多个社区 16KB fork 的 `libavcodec.so` 都含 x264 符号，但 `.pom` 仅声明 LGPL-3.0——许可证标注与
    实际二进制内容不符。**GPL 合规已处理**（2026-06-22）：设置页「关于 → 开源组件」已纠正标注为
    GPL-3.0，并附上游源码仓库（`github.com/moizhassankh/ffmpeg-kit-android-16KB`，含版本号）与
    GPL-3.0 完整许可证文本链接，履行 GPL §6(d) 网络分发场景下的对应源代码访问义务；本应用自身代码
    未随之开源（行业常见做法，非最终法律判定），详见 TASK-05「GPL 合规处理」。
  - **HEIC/AVIF 输出依赖系统编码能力**：当前实现通过 AndroidX `heifwriter` 接入，`HEIC` 需 API 28+、
    `AVIF` 需 API 31+；同时因库本身声明 `minSdk 28`，工程侧使用了 manifest `tools:overrideLibrary`
    并在运行时门控，构建已验证通过，但仍需真机确认设备互操作性与失败表现。
  - **本次闪退已先从架构层止血**：未捕获的引擎/恢复/前台服务启动异常现在不应再直接导致整应用崩溃；
    但最初触发真机闪退的“具体输入样本/具体引擎路径”还需要继续复测定位。
  - **`FFmpegKit failed to start on brand ...` 根因已确认并修复**：不是设备兼容性问题，而是原 fork
    `libavdevice.so` 本身缺符号、在所有设备上必然失败；已通过更换依赖坐标解决（见上方风险条目）。
  - **任务恢复不是断点续转**：当前“进程恢复”实现是应用重启后重新提交未完成任务并沿用原历史记录/目标 Uri，
    不是从原编码进度继续；对第一版用户体验足够，但耗时会比真正断点续转更长。
  - design token 为 teal，与 SPEC 蓝色默认强调色冲突，已约定以 SPEC 为准。

---

## 6. 维护约定

- 每完成一个可验证步骤，更新对应 `TASK-0X.md` 的「执行步骤」勾选与「完成情况」。
- 任务状态变化时，同步更新本文件第 4 节状态列与第 5 节当前进度。
- 出现新风险/决策，记入第 5 节并在相关任务文件展开。
- 不在本文件堆积实现细节，细节进各任务文件；本文件只保留全局视图。
