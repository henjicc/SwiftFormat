# TASK-01 · 文件选择与识别

**状态**：已完成　|　**依赖**：TASK-00　|　对应 SPEC：阶段 1、3.1、4.2、6.4 章

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
- [x] 接入 SAF 多选（`OpenMultipleDocuments`）与 Uri 权限持久化（`takePersistableUriPermission`）
- [x] 接入分享 Intent 入口（manifest `SEND`/`SEND_MULTIPLE` intent-filter + `MainActivity` 处理 + `onNewIntent`）
- [x] 实现媒体类型识别（MIME 优先 + 扩展名兜底）与不支持文件标记（`MediaTypeResolver`，单元测试覆盖）
- [x] 后台读取元数据（`FileMetadataReader`：displayName/size 走 ContentResolver；图片用 `BitmapFactory` 仅解边界；
  音视频用 `MediaMetadataRetriever`；全部跑在 `Dispatchers.IO`，失败降级不抛出）
- [x] 实现分组数据结构（`HomeUiState.groups` 按 视频/图片/音频 固定顺序，空组不显示；`unsupported` 单独列出）
- [x] 首页空状态 + 文件列表 UI（汇总栏/分组头/文件行/移除/添加更多/清空）
- [x] 缩略图异步加载与缓存（Coil3：图片直接解码，视频取首帧；音频/不支持文件用统一图标）

## 验收标准
- [ ] 一次可选 ≥ 50 个混合文件并正确分组 —— 逻辑已支持（无数量上限），未做大批量实机验证
- [x] 不支持文件被明确标记，含原因（`MediaType.UNKNOWN` → 「不支持」分组 + 固定原因文案）
- [x] 元数据读取不阻塞主线程（`Dispatchers.IO` + `viewModelScope`），大量文件下列表为 `LazyColumn`
- [x] 从分享菜单可将文件送入应用（`ACTION_SEND`/`ACTION_SEND_MULTIPLE` → `incomingShareFiles` → `HomeViewModel`）
- [x] 缩略图异步出现且有缓存（Coil 内存/磁盘缓存自动管理）

## 完成情况

### 已完成
- 新增 `core/model/InputFile`（Uri 为核心，不依赖绝对路径）。
- 新增 `core/file/MediaTypeResolver`（纯函数，4 个单元测试通过：mime 优先、扩展名兜底、未知归类、扩展名边界）。
- 新增 `core/file/FileMetadataReader`（IO 线程读取，图片仅解边界不全量解码避免 OOM，
  `MediaMetadataRetriever` 用 try/finally 手动 release 兼容 minSdk 26~28 无 AutoCloseable）。
- 新增 `feature/home/HomeViewModel` + `HomeUiState`（去重追加、移除、清空、按 id 增量元数据读取）。
- 新增 `feature/home/HomeScreen`（空状态 + 文件列表，`OpenMultipleDocuments` 选择器 + 权限持久化）。
- `AppContainer` 新增 `fileMetadataReader`、`incomingShareFiles`（`MutableSharedFlow<List<Uri>>` replay=1）。
- `MainActivity` 接入分享 Intent（`onCreate` + `onNewIntent`），转发到 `incomingShareFiles`。
- Manifest 新增 `SEND`/`SEND_MULTIPLE` intent-filter（image/video/audio）。
- 中英文新增首页/分组相关字符串资源。
- 缩略图：接入 Coil3（`coil-compose` + `coil-video`，自带内存/磁盘缓存与异步解码，避免手写缓存逻辑）。
  新增 `core/file/ThumbnailImageLoader`（注册 `VideoFrameDecoder` 取视频首帧），`AppContainer.thumbnailImageLoader` 单例；
  `HomeScreen.FileRow` 对 IMAGE/VIDEO 用 `AsyncImage` 渲染缩略图（裁剪填充、失败回退类型图标），AUDIO/UNKNOWN 仍用类型图标。
- 验证：`gradlew :app:assembleDebug` 通过、无警告；`testDebugUnitTest` 4/4 通过。**未做**实机/大批量/分享菜单/缩略图视觉的真机验证。

### 遗留（移交后续，不阻塞 TASK-02）
- 50+ 文件实机性能验证、分享菜单真机验证、缩略图实际渲染效果：需设备/模拟器，当前环境未执行。
