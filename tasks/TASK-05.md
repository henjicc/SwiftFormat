# TASK-05 · FFmpeg 兼容层

**状态**：已完成　|　**依赖**：TASK-03, TASK-04　|　对应 SPEC：阶段 5、10.5 章

## 目标
为 Media3/原生能力无法覆盖的格式（MP3/FLAC/特殊容器等）提供隔离的 FFmpeg 引擎，
满足许可证、16KB 页面与体积要求，并支持失败降级与输出验证。

## 待决策 / 前置确认（已决策，见下方完成情况）
- FFmpeg 来源与构建：选用可独立替换的库，**不**以未经维护验证的社区 AAR 作为不可替换核心。
- 许可证审查（LGPL/GPL）与合规结论。
- 仅编译/引入必要编解码器，记录实际构建配置与版本。

## 实现要求
- 独立 `engine-ffmpeg` 模块/包，实现 `FfmpegEngine`（`ConversionEngine`）。
- **所有 FFmpeg 命令只能在本模块内部生成**；UI/业务层禁止拼接命令。
- 仅处理 Media3 不擅长的链路（MP3/FLAC/WAV/Ogg/Opus、视频→MP3 等），纳入调度器。
- 临时文件策略（SPEC 12.2）：Uri→缓存→转换→写出→验证→删除；不在主线程复制；转换前检查空间。
- 失败降级与输出验证；FFmpeg native 调用结束后清理资源。
- 依赖必须可独立替换；FFmpeg 模块尽量裁剪、按 ABI 打包。

## 执行步骤
- [x] 完成 FFmpeg 选型/许可证/16KB 决策并记录（见下方「选型决策」）
- [x] 建立隔离 `engine/ffmpeg` 包与命令生成内聚封装（`FfmpegCommandBuilder`，纯函数）
- [x] 实现 FfmpegEngine：临时文件流程 + 进度 + 取消 + 资源清理
- [x] 实现输出验证（空文件检测）与失败降级（错误码映射到 `ConversionError.Kind`）
- [x] 扩展一期补齐 `VIDEO -> WEBM / MKV / MP3`：新增 `targetMediaType`、结构化输出选项、
  `FFprobeKit` 流结构校验、视频提取音频与友好错误文案
- [x] 扩展二期第一阶段补齐 `IMAGE -> BMP / TIFF`、`AUDIO -> OGG`、`VIDEO -> MOV`、
  `VIDEO -> M4A / WAV / FLAC`：新增 `FfmpegStillImageEngine`、图片输出校验、`M4A` 容器别名与排序规则
- [ ] 接入调度器（`ConversionEngineSelector`/`AppContainer`）—— 沿用 TASK-03/04 的既定安排，
  统一移交 TASK-06（引擎已满足 `supports()` 协议，可被直接注册）
- [ ] 验证 16KB 页面设备与体积/ABI 打包 —— **未做实机验证**（无 16KB 页面设备），
  已确认 AAR 内置 arm64-v8a/armeabi-v7a/x86/x86_64 四 ABI 原生库

## 验收标准
- [ ] FLAC→MP3、音频→OGG、图片→BMP/TIFF、视频→MP3/M4A/WAV/FLAC、视频→WEBM/MKV/MOV 等链路成功，输出可正常播放 —— 命令构建、路由、输出校验
  与错误处理已实现并通过 JVM 单元测试，**未做**实机/模拟器运行验证（无设备）
- [x] UI/业务层无任何 FFmpeg 命令拼接（命令构建收敛在 `FfmpegCommandBuilder`，`FfmpegEngine` 是唯一调用方）
- [x] 临时文件在成功/失败/取消后均被清理（`finally` 块统一 `delete()` 输入/输出临时文件）
- [ ] 16KB 页面设备可运行 —— 依赖声明与构建产物已确认含 16KB 对齐的 so（fork 名称与发布说明如此），
  **未做**真实 16KB 页面设备验证；许可证与构建配置已记录（见下方）
- [x] 覆盖单元测试：MP3/Opus 码率映射、命令构建、格式目录、引擎路由、MIME、失败原因编解码，
  均为纯函数或 JVM 测试，无需 Android 运行时

## 完成情况

### 选型决策（待决策项的结论）
官方 `arthenica/ffmpeg-kit` 已于 2025-04 退役，二进制不再发布到 Maven Central，仓库归档无人维护。
评估后选择社区维护的 16KB 重构 fork **`JamaisMagic/ffmpeg-kit-16KB`**（Maven 坐标
`io.github.jamaismagic.ffmpeg:ffmpeg-kit-main-full-16kb:6.1.4`），理由：
- 维护活跃度最高（同类 fork 中最近一次提交最新），用 NDK r27d 构建，明确支持 16KB 页面。
- 选用 **`main-full`（非 GPL）变体**：`.pom` 标注 `LGPL-3.0`，已含 LAME（MP3）/Opus/libvorbis/libwebp 等
  外部库，不含 x264/x265 等 GPL 组件，符合本项目"不强制 GPL 传染"的合规取向。
- 已实测验证（非凭空采信用户提供的信息）：
  - `./gradlew :app:dependencies` 确认坐标在 Maven Central 真实可解析（非虚构包名）。
  - 解压已下载的 `.aar` 确认内置 `arm64-v8a/armeabi-v7a/x86/x86_64` 四 ABI 原生库
    （`libavcodec`/`libavformat`/`libavfilter`/`libavdevice`/`libavutil`/`libswresample`/`libswscale`/
    `libffmpegkit*`），`.pom` 许可证字段确认为 `LGPL-3.0`。
  - 用 `javap` 反编译 `classes.jar` 中的 `com.arthenica.ffmpegkit.*` 类，确认其完整保留了原版
    FFmpegKit 的 Java API（`FFmpegKit`/`FFmpegSession`/`FFmpegKitConfig`/`ReturnCode`/`Statistics`/
    `Session` 等签名与原版一致），核实后才编码，未凭空假设 API。
  - `:app:assembleDebug` 实际打包验证依赖可正常链接、原生库被打入 APK。
- 该依赖**可替换**：仅通过 `ConversionEngine` 接口与 `FfmpegCommandBuilder` 暴露能力，业务层不直接
  依赖 `com.arthenica.ffmpegkit.*` 类型，未来切换到 `moizhassankh` fork、自建 AAR 或其他 FFmpeg JNI
  封装时只需替换 `FfmpegEngine` 内部实现。
- **已知风险（明确记录，非隐藏）**：该 fork 为个人维护、未经长期生产验证，与已退役的官方项目相比
  缺乏大规模使用track record；后续若发现兼容性/稳定性问题或维护停滞，需重新评估替换。

### 选型决策更新（2026-06-22，真机暴露问题后重新评估）

真机测试发现添加文件转换后报 `FFmpegKit failed to start on brand: vivo ...`，根因排查如下：

- **`JamaisMagic/ffmpeg-kit-main-full-16kb:6.1.4` 的 `libavdevice.so` 在所有变体上都无法加载**：
  用 `readelf --dyn-syms` 核实，`libavdevice.so` 引用了 `PLATFORM_hid_init/write/read/...` 等
  一整套符号，但该 AAR 内没有任何库提供这些符号（NEEDED 列表里没有对应依赖）；同时用 `javap` 反编译
  `NativeLoader.class` 确认 `loadFFmpeg()` 对 `FFMPEG_LIBRARIES = [avutil, swscale, swresample,
  avcodec, avformat, avfilter, avdevice]` 是无条件顺序加载、无 try/catch 降级（除 armv7a NEON 特例），
  一旦某个库 `dlopen` 失败就直接 `throw new Error(...)`，导致 `FFmpegKitConfig` 静态初始化永久失败。
  同时验证了 `lts-full-16kb:6.1.4`（同 fork 的另一分支）有**完全相同**的缺失符号——**这是该 fork 全系列
  的打包 bug，不是设备/变体问题**，意味着此依赖接入以来在任何设备上都从未真正跑通过 FFmpeg 转换链路。
- **换源为 `com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.1.1`**：下载 AAR 解压核实 `libavdevice.so`
  无上述缺失符号；用 `javap` 反编译 `FFmpegKit`/`FFmpegKitConfig`/`FFprobeKit`/`FFmpegSession`/
  `AbstractSession`/`MediaInformationSession`/`StreamInformation`/`MediaInformation`/`Statistics`/
  `ReturnCode` 等类核实 API 签名与原依赖（`com.arthenica.ffmpegkit.*`）完全一致，`FfmpegEngine`/
  `FfmpegStillImageEngine`/`FfmpegProbe`/`FfmpegRuntimeSupport` **零代码改动**；4 ABI 齐全
  （arm64-v8a/armeabi-v7a/x86/x86_64）；`testDebugUnitTest`/`assembleDebug` 通过，并解压打包后的
  APK 二次核实 `libavdevice.so` 确实是新版本、符号干净。
- **新发现的合规风险（未解决，需后续单独评估）**：排查替代依赖时用 `strings` 检查
  `libavcodec.so`，发现 `com.mrljdx:ffmpeg-kit-full`、`com.moizhassan.ffmpeg:ffmpeg-kit-16kb`
  **以及原来的 `JamaisMagic/ffmpeg-kit-main-full-16kb`** 都含有 `x264_build`/`x264 - core` 等
  字符串，说明都静态链接了 x264（GPL 许可），但三者的 `.pom` 均只声明 LGPL-3.0——**许可证标注与
  实际二进制内容不符**。也就是说 TASK-05 当初"已实测验证"只核实了 `.pom` 字段和 Java API，没有
  反编译检查 native 二进制是否混入 GPL 代码，存在遗漏。当前为恢复转换功能，临时接受这一现状切换
  依赖，**尚未做 GPL 合规处理**（GPL 要求随应用提供对应版本的完整源码或书面提供源码的渠道等义务）。
  后续需要：①确认应用分发方式是否触发 GPL 义务、②寻找真正不含 x264 的干净构建或自建 NDK 交叉编译、
  ③或正式接受 GPL 并补齐合规要求（许可证文件、源码提供渠道等）。
  **2026-06-22 决策**：用户明确表示当前优先级是"先能用"，许可证合规问题暂不处理，留待后续单独评估。

### GPL 合规处理（2026-06-22，真机测试通过后回头处理）

用户决策：正式接受 GPL，按 GPL-3.0 要求做公开义务，而不是回避或寄望"非链接"豁免。

- **纠正许可证标注**：设置页「关于 → 开源组件」原文案只说"社区维护的 16KB 兼容 FFmpegKit fork"，
  未提及 GPL，与上一节核实的事实（`libavcodec.so` 含 x264 符号）不符。已改为明确说明：该组件除
  LGPL-3.0 部分外还静态链接了 x264（GPL-3.0），随本应用分发的 FFmpeg 原生库整体按 GPL-3.0 条款提供。
- **履行 GPL §6 对应源代码义务**：网络分发（如应用商店）适用 GPL-3.0 §6(d)——需从提供二进制的同一
  渠道，以同样方式、无额外费用提供对应源代码的访问方式，且需持续到停止提供二进制为止。当前依赖坐标
  `com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.1.1` 的源码与构建脚本已由上游公开维护在
  `github.com/moizhassankh/ffmpeg-kit-android-16KB`（已用 WebSearch 核实仓库真实存在，非凭空采信/编造
  URL），设置页文案中已附该仓库地址（含版本号）与 GPL-3.0 完整许可证文本链接（`gnu.org/licenses/gpl-3.0.txt`），
  作为满足"对应源代码访问渠道"的公开声明。
- **残留风险（如实记录，非法律意见）**：本应用自身代码并未随附 GPL 许可证文本一并发布为 GPL，目前采用的是
  "FFmpeg 原生库作为独立 GPL 组件随附分发 + 公开声明 + 指向上游源码"的常见行业做法（类似多数捆绑
  GPL 版 FFmpeg/x264 的安卓应用），未对"是否构成与本应用代码的单一合成作品从而需要本应用代码本身也
  开源"做法律层面的最终判定；若后续需要更严格合规，可考虑：自行裁剪/重新构建不含 x264 的 FFmpeg
  （仅用 LGPL 组件，舍弃 x264 编码能力换用其它编码器）、或将本应用整体开源。
- 验证：仅改字符串资源，`gradlew.bat assembleDebug testDebugUnitTest` 通过。

### AVIF 输出改用 FFmpeg（2026-06-22）

真机反馈 `HEIC -> AVIF` 报 `encoded image cannot be decoded`，而 `PNG`/`WEBP` 等输出正常。
排查确认是 `HeifAvifImageEngine` 用的 AndroidX `AvifWriter` 依赖设备硬件 AV1 编码器，
该 vivo 机型编出的文件连系统自带解码器（`BitmapFactory`/`ImageDecoder`）都读不回来——是
设备硬件编码器兼容性问题，不是本项目代码逻辑问题。

修复：AVIF 改由 `FfmpegStillImageEngine` 处理（复用其已有的解码/EXIF 旋正/缩放管线），
用 `libaom-av1` 软件编码写出（不依赖设备硬件，新增 `AvifCrfMapper` 做质量→CRF 映射）；
`HeifAvifImageEngine` 收窄为只负责 `HEIC`。API 门控不变（仍要求 API 31+）。

### 范围收敛
- 本阶段保持 **Media3 负责主流稳定链路 `VIDEO -> MP4(H.264/AAC)`**，FFmpeg 只承接扩展格式，
  当前范围收敛为 **IMAGE -> BMP/TIFF**、**AUDIO -> MP3/OGG/FLAC/WAV**、
  **VIDEO -> MOV/WEBM/MKV**、**VIDEO -> MP3/M4A/WAV/FLAC**（提取首条音轨）。
- 本阶段**不**扩出 `HEVC/AV1`、`VIDEO -> OGG/Opus`、FFmpeg 硬件加速，
  也不做设备能力驱动的 UI 动态隐藏，避免把 FFmpeg 变成默认视频引擎。

### 已完成
- 新增 `engine/ffmpeg/FfmpegCommandBuilder`：纯函数按输出格式（MP3/FLAC/WAV）构建 ffmpeg 参数数组
  （`-y -i <in> -vn <codec参数> <out>`），MP3 用 `libmp3lame` + 码率、FLAC 用内置 `flac` 编码器、
  WAV 用 `pcm_s16le`；不依赖 Android 框架，可在 JVM 单元测试中直接验证生成的参数列表。
- 扩展一期将 `FfmpegCommandBuilder` 拆为 `buildAudioTranscodeArgs()`、`buildVideoTranscodeArgs()`、
  `buildVideoExtractAudioArgs()` 三类纯函数；其中 `WEBM` 固定 `VP9 + Opus`，`MKV` 固定
  `OpenH264 + AAC`，`VIDEO -> MP3` 固定 `libmp3lame` 且仅映射首条音轨。
- 新增 `engine/ffmpeg/FfmpegAudioBitrateMapper`：MP3 质量档位 → 目标码率（320/256/192/128 kbps），
  与 `engine/media/AudioBitrateMapper`（AAC 档位）分离，因 MP3 编码效率低于 AAC 需要更高码率才能
  达到相近听感。
- 新增 `engine/ffmpeg/OpusBitrateMapper`：WebM 的 Opus 音频码率映射（192/160/128/96 kbps）。
- 新增 `engine/ffmpeg/FfmpegStillImageEngine`：负责 `BMP / TIFF` 输出；先复用原生位图链路做解码、
  EXIF 旋正与尺寸缩放，再交给 FFmpeg 进行静态图容器写出，并用“非空文件 + 可读 bounds”做结果校验。
- 新增 `engine/ffmpeg/FfmpegEngine`（`ConversionEngine`）：
  - `supports()`：覆盖 AUDIO + 输出格式在 {MP3, OGG, FLAC, WAV}，以及 VIDEO + 输出格式在
    {MOV, WEBM, MKV}、VIDEO 提取 {MP3, M4A, WAV, FLAC}。
  - 转换前检查 `cacheDir.usableSpace` 是否足够（源文件大小 ×2 + 16MB 余量），不足则返回
    `INSUFFICIENT_STORAGE` 而不是转换中途失败（落实 SPEC 12.2"转换前检查空间"）。
  - 临时文件流程：源 `Uri` → `ContentResolver.openInputStream` 拷贝到 `cacheDir` 临时文件 →
    `FFmpegKit.executeWithArgumentsAsync` 转换到另一个 `cacheDir` 临时文件 → 校验输出文件存在且非空
    （输出验证）→ 拷贝字节到目标 `Uri` → `finally` 块删除两个临时文件（成功/失败/取消路径都会执行）。
  - 进度：`StatisticsCallback` 回调中用 `statistics.time / request.input.durationMs` 估算进度
    （复用 TASK-01 `FileMetadataReader` 已读取的 `durationMs`，没有另起探测逻辑），转换阶段占 0~90%，
    拷贝到目标 `Uri` 占剩余 10%（与 `Media3Engine` 的进度划分约定一致）。
  - 取消：按 `request.id` 跟踪 `FFmpegSession`（`ConcurrentHashMap`，与 `Media3Engine` 的
    `activeTransformers` 同构），`cancel(taskId)` 调用 `session.cancel()`。
  - 失败降级：用 `ReturnCode.isSuccess/isCancel` 区分成功/取消/失败，失败时取
    `session.failStackTrace`（为空则取 `session.allLogsAsString`）作为 `debugMessage`，
    不向上层暴露 FFmpeg 原始堆栈作为用户可见文案（与 `ConversionError.debugMessage` 的既定用途一致）。
  - 扩展阶段新增 `FFprobeKit` 探测：输入侧先检查视频/音频流是否存在，输出侧再验证
    `MOV/WEBM/MKV` 至少含 1 条视频流、`VIDEO -> AUDIO` 仅含音频流，避免“执行成功但产物流结构错误”的假成功。
  - 与 `Media3Engine` 不同：FFmpegKit 的 `execute*Async`/`cancel`/回调没有 Looper 线程绑定要求，
    `convert()`/`cancel()` 固定跑在 `Dispatchers.IO`（FFmpegKit 内部自行管理原生执行线程）。
- **API 核实方法**：延续 TASK-04 的 `javap` 反编译验证方法（见上方「选型决策」），核实
  `FFmpegKit.executeWithArgumentsAsync`/`FFmpegSession`/`FFmpegSessionCompleteCallback`/
  `StatisticsCallback`/`Statistics`/`ReturnCode`/`Session` 以及 `FFprobeKit` /
  `MediaInformation` / `StreamInformation` 的真实签名后再编码，一次编译即通过。
- 扩展一期同时更新了 UI/模型路由：`GroupConversionSettings` / `ConversionRequest` 新增
  `targetMediaType`，`OutputFormatCatalog` 改为结构化 `OutputOption`，视频组选项现为
  `MP4 / WEBM / MKV / MP3`，其中 `MP3` 会隐藏尺寸但保留质量选择。
- 扩展二期第一阶段继续更新了 UI/模型路由：图片组选项现为 `JPG / PNG / WEBP / BMP / TIFF`，
  音频组选项现为 `MP3 / M4A / AAC / WAV / FLAC / OGG`，视频组选项现为
  `MP4 / MOV / WEBM / MKV / MP3 / M4A / WAV / FLAC`；其中真正输出视频的格式才显示尺寸，
  `BMP / TIFF / WAV / FLAC` 隐藏质量。
- 验证：`gradlew.bat testDebugUnitTest`、`gradlew.bat lintDebug`、`gradlew.bat assembleDebug`
  均通过；当前 JVM 单元测试总数 85。

### 已知简化（明确记录）
- **未接入调度器/AppContainer**：与 TASK-03/04 的既定安排一致，引擎已满足 `ConversionEngine` 协议
  可被 `ConversionEngineSelector` 直接注册，实际接线已在 TASK-06 完成。
- **更高风险格式仍后置**：未实现 `HEVC/AV1`、`VIDEO -> OGG/Opus`、动图 GIF 输出、
  `HEIC/AVIF` 输出，也未验证 FFmpeg MediaCodec 硬件加速能力。
- **16KB 页面设备与体积验证未做实机测试**：当前环境无 16KB 页面真机；APK 体积影响（四 ABI 全量
  FFmpeg 原生库会显著增加包体积）未测量，正式发布前应评估按 ABI 拆分 APK/AAB 或裁剪未用编解码器
  （SPEC 要求"仅编译必要编解码器"，当前用的是 fork 提供的预编译 full 包，并非按需自建最小化构建，
  这是相对官方建议方案的一处妥协，换来的是不需要自建 NDK 交叉编译流水线）。
- **未做真实解码级可播放性校验**：当前输出验证已提升为 `FFprobeKit` 的流结构探测
  （而非仅检查非空文件），足以拦截“执行成功但流类型不符合预期”的明显失败场景；但仍未做
  二次实际解码验证，无法发现“写出了但内容损坏”的更深层边界情况。
- **未做实机/模拟器运行验证**：MP3/FLAC/WAV 真实转换效果、进度回调实际触发频率、取消的真实表现均
  未验证；FFmpegKit 16KB fork 本身是个人维护、缺乏长期生产验证记录（见上方「已知风险」）。
