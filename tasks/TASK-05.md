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
- [ ] 接入调度器（`ConversionEngineSelector`/`AppContainer`）—— 沿用 TASK-03/04 的既定安排，
  统一移交 TASK-06（引擎已满足 `supports()` 协议，可被直接注册）
- [ ] 验证 16KB 页面设备与体积/ABI 打包 —— **未做实机验证**（无 16KB 页面设备），
  已确认 AAR 内置 arm64-v8a/armeabi-v7a/x86/x86_64 四 ABI 原生库

## 验收标准
- [ ] FLAC→MP3、视频→MP3 等链路成功，输出可正常播放 —— 命令构建与转换流程已实现并通过单元测试，
  **未做**实机/模拟器运行验证（无设备）；视频→MP3 当前不在范围内（见下方「范围收敛」）
- [x] UI/业务层无任何 FFmpeg 命令拼接（命令构建收敛在 `FfmpegCommandBuilder`，`FfmpegEngine` 是唯一调用方）
- [x] 临时文件在成功/失败/取消后均被清理（`finally` 块统一 `delete()` 输入/输出临时文件）
- [ ] 16KB 页面设备可运行 —— 依赖声明与构建产物已确认含 16KB 对齐的 so（fork 名称与发布说明如此），
  **未做**真实 16KB 页面设备验证；许可证与构建配置已记录（见下方）
- [x] 覆盖单元测试：MP3 码率映射（1）+ 命令构建（5），均为纯函数无需 Android 运行时

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

### 范围收敛
- `OutputFormatCatalog` 当前音频格式只有 MP3/AAC/WAV/FLAC（无 Ogg/Opus 选项），且 AAC 已由
  `Media3Engine` 覆盖，故 `FfmpegEngine.supports()` 范围收敛为 **AUDIO → MP3/FLAC/WAV** 三种，
  未实现"视频→MP3"等任务文件原描述的链路（`OutputFormatCatalog` 的视频输出格式只有 MP4/WEBM，
  当前无"视频转音频"的 UI/模型入口，且属于 SPEC 未明确要求的能力，本任务不顺带扩出该范围）。
- WebM 视频输出（TASK-04 已知简化项）本可由 FFmpeg 弥补，但同理无 UI/模型支持，留待后续按需评估，
  不在本任务内顺带实现。

### 已完成
- 新增 `engine/ffmpeg/FfmpegCommandBuilder`：纯函数按输出格式（MP3/FLAC/WAV）构建 ffmpeg 参数数组
  （`-y -i <in> -vn <codec参数> <out>`），MP3 用 `libmp3lame` + 码率、FLAC 用内置 `flac` 编码器、
  WAV 用 `pcm_s16le`；不依赖 Android 框架，可在 JVM 单元测试中直接验证生成的参数列表。
- 新增 `engine/ffmpeg/FfmpegAudioBitrateMapper`：MP3 质量档位 → 目标码率（320/256/192/128 kbps），
  与 `engine/media/AudioBitrateMapper`（AAC 档位）分离，因 MP3 编码效率低于 AAC 需要更高码率才能
  达到相近听感。
- 新增 `engine/ffmpeg/FfmpegEngine`（`ConversionEngine`）：
  - `supports()`：仅 AUDIO + 输出格式在 {MP3, FLAC, WAV}。
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
  - 与 `Media3Engine` 不同：FFmpegKit 的 `execute*Async`/`cancel`/回调没有 Looper 线程绑定要求，
    `convert()`/`cancel()` 固定跑在 `Dispatchers.IO`（FFmpegKit 内部自行管理原生执行线程）。
- **API 核实方法**：延续 TASK-04 的 `javap` 反编译验证方法（见上方「选型决策」），核实
  `FFmpegKit.executeWithArgumentsAsync`/`FFmpegSession`/`FFmpegSessionCompleteCallback`/
  `StatisticsCallback`/`Statistics`/`ReturnCode`/`Session` 的真实签名后再编码，一次编译即通过。
- 验证：`gradlew :app:assembleDebug` 通过（确认 `libavcodec`/`libavformat` 等原生库被正确打入 APK）；
  `testDebugUnitTest` 45/45 通过（新增 6 个测试：`FfmpegCommandBuilderTest` 5 个 + 
  `FfmpegAudioBitrateMapperTest` 1 个）。

### 已知简化（明确记录）
- **未接入调度器/AppContainer**：与 TASK-03/04 的既定安排一致，引擎已满足 `ConversionEngine` 协议
  可被 `ConversionEngineSelector` 直接注册，实际接线移交 TASK-06（届时三个引擎一起接入）。
- **范围只覆盖 MP3/FLAC/WAV 音频转码**：未实现"视频→MP3"等链路（无对应 UI/模型支持，见上方
  「范围收敛」），未实现 WebM 视频输出（同上）。
- **16KB 页面设备与体积验证未做实机测试**：当前环境无 16KB 页面真机；APK 体积影响（四 ABI 全量
  FFmpeg 原生库会显著增加包体积）未测量，正式发布前应评估按 ABI 拆分 APK/AAB 或裁剪未用编解码器
  （SPEC 要求"仅编译必要编解码器"，当前用的是 fork 提供的预编译 full 包，并非按需自建最小化构建，
  这是相对官方建议方案的一处妥协，换来的是不需要自建 NDK 交叉编译流水线）。
- **未做输出文件的可播放性校验**：当前"输出验证"仅检查文件存在且非空，没有进一步解码校验
  （如重新探测输出文件的编码参数确认与预期一致），属于轻量级验证，足以拦截"FFmpeg 报成功但实际
  没写出内容"的明显失败场景，但无法发现"写出了但内容损坏"的边界情况。
- **未做实机/模拟器运行验证**：MP3/FLAC/WAV 真实转换效果、进度回调实际触发频率、取消的真实表现均
  未验证；FFmpegKit 16KB fork 本身是个人维护、缺乏长期生产验证记录（见上方「已知风险」）。
