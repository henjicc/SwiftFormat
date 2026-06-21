# TASK-01 · 文件选择与识别

**状态**：未开始　|　**依赖**：TASK-00　|　对应 SPEC：阶段 1、3.1、4.2、6.4 章

## 目标
让用户从系统选择器或分享菜单导入一个/多个混合文件，自动识别媒体类型并按
视频/图片/音频分组，读取元数据并生成缩略图。

## 实现要求
- 入口：`ACTION_OPEN_DOCUMENT` / Photo Picker 多选；Android 分享 Intent 接收文件。
- 以 `Uri` 为核心，持久化读权限（takePersistableUriPermission）。
- 媒体类型识别（MIME + 扩展名兜底）→ `MediaType.{IMAGE,VIDEO,AUDIO,UNKNOWN}`。
- 读取元数据：displayName、mimeType、扩展名、大小、图片宽高、音视频时长（不阻塞主线程）。
- 自动过滤/标记不支持文件并展示原因。
- 按类型分组；缺某类则不显示该组。
- 视频/图片缩略图异步加载并缓存；音频用统一图标。
- 输入目标格式范围见 SPEC 3.1（图片含 HEIC 设备支持时；视频 MP4/MOV/MKV/WebM；音频 MP3/AAC/WAV/FLAC/Ogg/Opus 等）。

## 执行步骤
- [ ] 接入 SAF/Photo Picker 多选与 Uri 权限持久化
- [ ] 接入分享 Intent 入口（manifest intent-filter + 处理）
- [ ] 实现媒体类型识别与不支持文件标记
- [ ] 后台读取元数据（ContentResolver / MediaMetadataRetriever）
- [ ] 实现分组数据结构与缩略图异步加载缓存
- [ ] 首页空状态 + 已选文件入参传递到参数页

## 验收标准
- 一次可选 ≥ 50 个混合文件并正确分组。
- 不支持文件被明确标记，含原因。
- 元数据读取不阻塞主线程，大量文件下首页仍可操作。
- 从分享菜单可将文件送入应用。
- 缩略图异步出现且有缓存，音频显示统一图标。

## 完成情况
（待填写）
