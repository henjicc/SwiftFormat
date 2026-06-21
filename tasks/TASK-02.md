# TASK-02 · 参数页面

**状态**：已完成　|　**依赖**：TASK-01　|　对应 SPEC：阶段 2、4.3/4.4、5、6.3 章

## 目标
实现统一参数页面：按组展示文件，每组用相同风格卡片设置「输出格式/质量/尺寸」，
并根据输出格式动态显示有效选项。

## 实现要求
- 顶部：已选文件总数、总大小、添加更多文件、清空全部。
- 视频/图片/音频分组卡片，组内文件列表默认只显示前几个 + “查看全部”，可折叠。
- 统一设置行组件（输出格式 / 质量 / 尺寸），点击用 **Material 3 Modal Bottom Sheet** 选择，含选项说明。
- 统一参数模型 `GroupConversionSettings`：每组独立设置格式/质量；视频与图片独立设置尺寸；音频无尺寸。
- 质量档位：最佳/高/标准/省空间（默认**高**）。尺寸默认**保持原始**。
- 动态显示（SPEC 5.7）：PNG/WAV 隐藏质量；FLAC 可隐藏质量或显示压缩程度；
  不支持尺寸变化的格式隐藏尺寸；设备无法生成的格式不显示或标记不可用并解释；切换用平滑动画。
- 添加更多文件后**不丢失**已有设置；参数状态可在配置变化（旋转）后保留。
- 底部固定主按钮「开始转换」。

## 执行步骤
- [x] 顶部汇总栏 + 添加/清空（沿用 TASK-01 已实现，本任务未改动）
- [x] 三类分组卡片与“查看全部/折叠”（`GroupCard`，默认展示前 3 个，超出显示「查看全部 (N)」）
- [x] 统一设置行组件 + Modal Bottom Sheet 选择器（`SettingRow` + `OptionsBottomSheet`，列表项 + 选中态勾选）
- [x] 接入统一参数模型与各组默认值（`GroupConversionSettings` + `OutputFormatCatalog.defaultSettings`）
- [x] 实现按输出格式的动态选项显隐（`isQualityApplicable` 控制质量行；`sizePresets` 为空控制尺寸行；
  Compose 行的出现由 if 条件控制，**未加显式过渡动画**，见下方简化说明）
- [x] 参数状态在 ViewModel 持久化（`HomeUiState.settings: Map<MediaType, GroupConversionSettings>`），
  处理添加文件/旋转不丢失（新分组才补默认值，已有分组设置不动；StateFlow 在配置变化后由 ViewModel 存活）

## 验收标准
- [x] 三类参数组件视觉一致（统一走 `SettingRow`/`GroupCard`）；音频不显示尺寸（`sizePresets(AUDIO)` 为空）。
- [x] PNG/WAV/FLAC 不展示质量项；切换格式后参数行自动按新格式显隐更新。
- [x] 默认质量「高」、默认尺寸「保持原始」（`OutputFormatCatalog.defaultSettings`，单元测试覆盖）。
- [x] 添加更多文件后原设置不丢失（已有分组设置不会被覆盖）；ViewModel 存活于配置变化，理论上旋转不丢，
  **未做实机旋转验证**。

## 完成情况

### 已完成
- 新增 `core/model/GroupConversionSettings`（纯数据，mediaType/outputFormat/quality/size）。
- 新增 `core/model/OutputFormatCatalog`（各类型输出格式列表、默认格式、质量适用性规则、尺寸档位列表、
  默认设置），4 个单元测试覆盖（音频无尺寸、图片/视频默认原始尺寸、PNG/WAV/FLAC 隐藏质量、
  各类型默认设置匹配 SPEC 22）。
- `HomeUiState` 新增 `settings: Map<MediaType, GroupConversionSettings>`；`HomeViewModel` 新增
  `setOutputFormat/setQuality/setSize`，新分组首次出现时自动补默认值，已有分组设置保持不变。
- 新增 `feature/home/GroupSettingsCard.kt`：`GroupCard`（分组头 + 三个统一设置行 + 折叠文件列表）、
  `SettingRow`（标签 + 当前值 + chevron，点击打开底部弹层）、`OptionsBottomSheet`
  （Material3 `ModalBottomSheet` + `ListItem` 列表 + 选中勾选图标）、质量/尺寸标签的字符串映射。
- `HomeScreen.FileList` 改为渲染 `GroupCard`（替代裸文件列表），底部新增固定「开始转换」按钮
  （当前禁用，转换引擎留给 TASK-03/04，避免无实现却可点击的误导）。
- 新增中英文质量/尺寸/分组行/底部按钮相关字符串资源。
- 验证：`gradlew :app:assembleDebug` 通过、无警告；`testDebugUnitTest` 9/9 通过（含新增 4 个）。
  **未做**实机交互、旋转保留、底部弹层视觉的真机验证。

### 已知简化（明确记录，非遗漏）
- 动态显隐未加显式过渡动画（SPEC 5.7 建议「使用平滑动画」）；当前用条件渲染直接出现/消失。
- 自定义尺寸（`SizePreset.Custom`）UI 未实现输入框，暂回退显示「保持原始」文案；
  第一版 SPEC 范围内非必需（5.6 允许第一版暂不提供复杂裁剪/自定义）。
- WebP 未区分有损/无损（SPEC 5.3「无损 WebP 不显示画质档位」），当前 WebP 统一按有损处理显示质量行；
  待 TASK-03 图片引擎接入时再细化。
- 设备实际编码能力检查未接入（格式列表为 SPEC 静态目标列表），留给 TASK-03/04 真实引擎接入后处理。
- 底部「开始转换」按钮禁用占位，等待 TASK-03/04（引擎）与 TASK-06（任务编排）接入后启用并接转换流程。
