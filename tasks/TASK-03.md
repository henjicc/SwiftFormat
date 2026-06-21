# TASK-03 · 图片引擎

**状态**：已完成　|　**依赖**：TASK-00, TASK-02　|　对应 SPEC：阶段 3、5.3/5.6、10、11 章

## 目标
实现原生图片转换引擎，支持 JPG/PNG/WebP 输出，落实质量与尺寸映射，支持批量。

## 实现要求
- 实现 `ConversionEngine` 接口（`supports/convert/cancel`）的 `NativeImageEngine`。
- 输入：JPG/PNG/WebP/BMP/GIF(首帧)/HEIC(设备支持)；输出：JPG/PNG/WebP。
- 质量映射（SPEC 5.3）：最佳95/高85/标准75/省空间60；PNG 无损不显示质量；无损 WebP 不显示画质档。
- 尺寸（SPEC 5.6）：按最长边缩放，默认保持宽高比；不放大低质源；保持原始为默认。
- 解码按目标尺寸采样（inSampleSize / ImageDecoder targetSize），避免无意义多次解码与 OOM。
- 元数据策略（是否保留 EXIF）按设置；不修改源文件。
- 引擎与 UI 隔离，仅通过 `ConversionRequest` 通信；纳入 `ConversionEngineSelector` 调度。

## 执行步骤
- [x] 定义 engine-api（`engine/api/ConversionEngine` + `ConversionProgress`/`ConversionResult`
  + `ConversionEngineSelector`；`ConversionRequest`/`OutputDestination` 放在 `core/model`，与 SPEC 11.6 一致）
- [x] 实现 NativeImageEngine 解码→（按 EXIF 旋正后的目标尺寸）采样解码→旋正→缩放→编码→写出 Uri
- [x] 实现质量/尺寸映射与动态规则（`ImageQualityMapper`、`OutputFormatCatalog.isQualityApplicable`
  对 PNG 隐藏质量；WebP 有损/无损区分见下方简化说明）
- [x] 采样解码（`ImageSizeMapper.sampleSizeFor` 2 的幂采样）与 Bitmap 及时 `recycle()`
- [x] EXIF 旋转旋正像素（见下方说明，替代“保留元数据”设置项，该设置项本身留给 TASK-07）
- [x] 接入调度器（`ConversionEngineSelector`）+ 取消（按 `request.id` 跟踪 `Job`，`cancel(taskId)` 触发协程取消）

## 验收标准
- [ ] JPG/PNG/WebP 互转输出可正常打开 —— 逻辑已实现，**未做**实机/模拟器验证（当前环境无设备）
- [x] 各质量档产出符合映射（`ImageQualityMapper` 单元测试：95/85/75/60）；PNG 不出现质量项
  （`OutputFormatCatalog.isQualityApplicable`，TASK-02 已验证）
- [x] 尺寸缩放保持比例且不放大（`ImageSizeMapper` 单元测试覆盖）；大图按采样解码避免一次性全尺寸解码
- [x] 原文件保持不变（引擎只读 `input.uri`，从不写入源）；可取消单任务（`cancel(taskId)` 取消协程 Job）
- [x] 覆盖单元测试：质量映射（1）、尺寸映射（9，含 EXIF 旋转后宽高互换辅助函数）、
  输出命名（5）、引擎选择（2）

## 完成情况

### 已完成
- `core/model/ConversionRequest`：`ConversionRequest` + `OutputDestination`（密封接口，当前只有
  `ResolvedUri` 一种——输出位置解析本身是 TASK-06 的职责，引擎只管写入已解析好的 Uri，保持解耦）。
- `engine/api/ConversionEngine.kt`：接口 + `ConversionProgress`(fraction) + `ConversionResult`
  （`Success(outputUri, outputSizeBytes)` / `Failure(error)`）。
- `engine/api/ConversionEngineSelector.kt`：按注册顺序选第一个 `supports()` 为真的引擎。
- `engine/image/ImageQualityMapper.kt`：质量档位 → JPEG/WebP 压缩质量值，纯函数。
- `engine/image/ImageSizeMapper.kt`：尺寸档位 → 目标像素（含 `Custom`/`VideoResolution` 兜底）、
  采样率计算、`Dimensions.swapped()`（配合 EXIF 90°/270° 旋转）。
- `engine/image/NativeImageEngine.kt`：解码 bounds → 读 EXIF 方向 → 按显示方向计算目标尺寸 →
  反推原始像素方向的采样目标 → 采样解码 → 旋正 → 精确缩放 → 按格式压缩 → 写出 → 用
  `CountingOutputStream` 统计字节数，全程 `ensureActive()` 协作式取消检查；`cancel(taskId)` 取消对应协程。
- `core/file/OutputNaming.kt`：扩展名替换、重名自动加序号（SPEC 12.4），纯函数。
- 新增 `mockito-kotlin` 测试依赖（仅 `testImplementation`），用于在不引入 Robolectric 的前提下
  构造含 `android.net.Uri` 字段的测试夹具（Android stub jar 对真实 Uri 方法调用会抛 `Stub!`）。
- **关键修正**：最初实现里 EXIF 90°/270° 旋转会交换宽高，但目标尺寸是按旋转前的原始像素方向计算的，
  旋正后会与目标宽高比不一致而被强行拉伸变形——已修正为先读方向、按显示方向算目标尺寸、
  再反推解码阶段应使用的原始方向采样目标，避免该问题（`ImageSizeMapperTest.dimensions_swapped_...` 覆盖辅助函数）。
- 验证：`gradlew :app:assembleDebug` 通过、无警告；`testDebugUnitTest` 26/26 通过（7 个测试类）。

### 已知简化（明确记录）
- WebP 仍未区分有损/无损（沿用 TASK-02 的简化），统一按有损 `WEBP_LOSSY`（API 30+）/`WEBP`（更低版本）处理。
- “是否保留元数据”设置项（SPEC 15.2）尚未接入——当前固定行为是旋正像素方向（保证视觉正确），
  不复制完整 EXIF 标签集（GPS、相机信息等）；该设置项本身待 TASK-07 设置页落地后再决定具体策略。
- GIF/HEIC 输入解码依赖系统 `BitmapFactory` 原生支持，未做设备能力探测或针对性兜底，
  失败会统一归类为 `CORRUPT_INPUT`。
- 未做实机/模拟器运行验证（互转产物可正常打开、大图不 OOM 的真实表现），当前环境无 Android 设备。
- 输出位置解析（MediaStore/SAF、Download/转个格式 默认目录）未实现，留给 TASK-06；本任务的
  `ConversionRequest.destination` 需由未来的任务编排层先解析好 Uri 再传入。
- 引擎未接入 `AppContainer`/UI（没有调用方）：`NativeImageEngine` 目前是独立、可单元测试的模块，
  实际接到「开始转换」按钮与任务流程是 TASK-06 的工作，本任务有意不做这层接线以避免无意义占位代码。
