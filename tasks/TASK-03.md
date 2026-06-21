# TASK-03 · 图片引擎

**状态**：未开始　|　**依赖**：TASK-00, TASK-02　|　对应 SPEC：阶段 3、5.3/5.6、10、11 章

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
- [ ] 定义 engine-api（ConversionEngine/Request/Result/Progress/Error，若 TASK-00 未建则补）
- [ ] 实现 NativeImageEngine 解码→缩放→编码→写出 Uri
- [ ] 实现质量/尺寸映射与动态规则（PNG/无损 WebP）
- [ ] 采样解码与 Bitmap 及时释放
- [ ] 元数据保留策略
- [ ] 接入调度器并支持批量 + 取消

## 验收标准
- JPG/PNG/WebP 互转输出可正常打开。
- 各质量档产出符合映射；PNG 不出现质量项。
- 尺寸缩放保持比例且不放大；大图不 OOM。
- 原文件保持不变；可取消单任务。
- 覆盖单元测试：质量映射、尺寸映射、输出命名、引擎选择。

## 完成情况
（待填写）
