# TASK-06 · 后台任务与历史

**状态**：进行中（Stage A 已完成）　|　**依赖**：TASK-03, TASK-04　|　对应 SPEC：阶段 6、4.5/4.6、13、14 章

## 目标
实现转换的后台执行、进度通知、转换/完成页面、Room 历史与进程恢复、临时文件清理。

## ⚠ 前置工具链风险（来自 [[TASK-00]] Stage C 探测，**已解决**）
本任务的 Room 依赖 **KSP**，曾在 AGP 9.2.1 + 内置 Kotlin 2.3.0 下不可用。开工前复测：升级
`com.google.devtools.ksp` 到 `2.3.9` 后 KSP 与 AGP9 内置 Kotlin 已兼容（上游已发布支持版本），
实测 Room `@Database`/`@Dao` 注解处理正常生成代码。**不需要**降级 AGP 或放弃 Room。详见下方 Stage A
与 TASK-00「Stage C」更新。Hilt 同理已无 KSP 阻塞，但本任务仍沿用手动 `AppContainer`（无新增必要性，
不顺带引入）。

## 实现要求
- 转换执行用 **Foreground Service**；通知含：正在转换数量、当前文件、总体进度、点击返回、取消。
- WorkManager 仅用于延迟重试、清理缓存/残留临时文件等非实时辅助。
- 转换页面（SPEC 4.5）：总体进度、已完成/总数、当前文件与阶段；
  文件行含 原格式→目标格式、状态、单文件进度、取消、失败原因、重试。
- 任务状态机：等待中/准备中/转换中/正在保存/已完成/已取消/失败（`ConversionStatus`）。
- 完成页面（SPEC 4.6）：主操作 打开/分享/查看位置/再次转换；次操作 删除结果/删除原文件
  （删除原文件默认关闭、需二次确认、不与完成自动绑定）。
- 输出位置（SPEC 12.3）：默认 `Download/转个格式` 或对应 MediaStore 分类；重名默认加序号。
- Room 历史（SPEC 14）：记录原名/原格式/输出格式/类型/起止时间/状态/输出Uri/大小/失败原因/质量/尺寸档；
  历史操作：打开/分享/查看位置/再次转换/删除记录/删除输出（删记录默认不删文件）。
- 进程被回收/旋转后可恢复任务状态；缓存清理不删已完成输出。

## 执行步骤
- [x] 解决 KSP/AGP9 工具链阻塞（升级 KSP 到 2.3.9，验证 Room 注解处理可用）
- [x] Room 历史数据层：`ConversionHistoryEntity`/`Dao`/`Database`/`Repository`，接入 `AppContainer`
- [ ] 实现 Foreground Service + 通知（进度/取消/返回）
- [ ] 任务编排层：队列、并发策略、状态机、批量汇总
- [ ] 转换进度页面 UI（含取消/重试/失败原因）
- [ ] 输出写入 MediaStore + 重名处理
- [ ] 完成页面与操作（打开/分享/查看位置/再次转换/删除）
- [ ] 历史页面（UI，读 `ConversionHistoryRepository`）
- [ ] 进程恢复与残留临时文件清理（WorkManager）

## 验收标准
- 退后台仍继续转换，通知显示进度并可取消。
- 可取消单个/全部任务；部分失败不阻断其他。
- 输出可正常打开；原文件默认不变；重名自动加序号。
- 旋转/进程回收后任务状态可恢复；缓存清理不删已完成输出；取消不留无效输出。
- 历史正确记录并可执行各项操作；删记录不默认删文件。

## 完成情况

### Stage A（已完成，已验证）—— KSP 解锁 + Room 历史数据层
- **KSP/AGP9 阻塞解决**：升级 `gradle/libs.versions.toml` 的 `ksp` 到 `2.3.9`
  （`com.google.devtools.ksp` 插件），同时给根 `build.gradle.kts`/`app/build.gradle.kts` 加上
  `alias(libs.plugins.ksp)`。实测验证（非仅"插件加载不报错"）：写了真实的 `@Database`/`@Dao`，
  `gradlew :app:kspDebugKotlin` 生成了 `app/build/generated/ksp/debug/kotlin/.../
  SwiftFormatDatabase_Impl.kt`/`ConversionHistoryDao_Impl.kt`，`assembleDebug`/`testDebugUnitTest`
  均通过。TASK-00 当时探测到的"内置 Kotlin 与 KSP 冲突 / 外置 Kotlin 插件与 AGP9 不兼容"已不存在
  （KSP 上游后续发布了支持 AGP9 内置 Kotlin 的版本）。已同步更新 `TASK-00.md`「Stage C」。
- 新增 `core/model/ConversionStatus`：任务状态机枚举（PENDING/PREPARING/CONVERTING/SAVING/
  COMPLETED/CANCELLED/FAILED，对应 SPEC 4.5 中文状态名）。
- 新增 `core/model/SizePresetCodec`：`SizePreset` ↔ 紧凑字符串互转的**纯函数**（如
  `VIDEO:1080`/`CUSTOM:1920x`），供历史记录持久化使用；不依赖 Room/Android，单元测试覆盖
  全部四种 `SizePreset` 变体 + null + 非法字符串。
- 新增 `core/model/ConversionHistoryRecord`：历史记录的领域模型（字段对应 SPEC 14 列出的全部
  字段：原文件名/原格式/输出格式/类型/起止时间/状态/输出Uri/大小/失败原因/质量/尺寸档），
  不依赖 Room，ViewModel/UI 只接触这个类型。
- 新增 `core/database` 包（对应 SPEC 9.4 推荐结构）：
  - `ConversionHistoryEntity`：Room 实体，字段全部是 DB 友好的基础类型（枚举/Uri/SizePreset
    存成 String，转换收敛在 Mapper，不用 Room `@TypeConverter`，避免转换逻辑散落且不可单测）。
  - `ConversionHistoryMapper`：Entity ↔ Record 互转（依赖 `Uri.parse`，故不在 JVM 单元测试范围，
    与项目内其他 Android 依赖类一致的已知测试gap）。
  - `ConversionHistoryDao`：`insert`/`update`/`getById`/`deleteById`/`observeAll`（Flow）/
    `getActiveRecords`（查询状态为进行中的记录，供后续「进程恢复」步骤使用）。
  - `SwiftFormatDatabase`：`@Database(version = 1, exportSchema = true)`，schema 导出到
    `app/schemas/`（已纳入版本控制，为后续 migration 提供基线）。
  - `ConversionHistoryRepository`：包装 Dao，对外只暴露 `ConversionHistoryRecord`，是
    `feature/history` 等上层代码的唯一访问入口。
- 接入 `di/AppContainer`：`Room.databaseBuilder(...)` 构建单例 `SwiftFormatDatabase`，暴露
  `conversionHistoryRepository`。
- 验证：`gradlew :app:assembleDebug` 通过；`testDebugUnitTest` 53/53 通过（新增 8 个：
  `SizePresetCodecTest`）。

### 已知简化（Stage A 范围内）
- **历史页面 UI 未实现**：`feature/history/HistoryScreen.kt` 仍是占位，尚未读取
  `ConversionHistoryRepository`；留给本任务后续 Stage（历史页面 UI 步骤）。
- **`ConversionHistoryMapper` 未做单元测试**：依赖 `android.net.Uri.parse`，Android SDK 桩
  会抛 `Stub!`，与项目其余 Android 依赖类一致，未引入 Robolectric。`SizePresetCodec`（最易出错的
  字符串编解码逻辑）已单独抽出并测试，缓解该 gap 的影响面。
- **未做迁移测试**：当前 `version = 1`，无历史版本，`app/schemas/` 只有一份基线，迁移测试
  留到首次 schema 变更时补充。
- 尚未实现：Foreground Service、任务编排、转换进度/完成页面 UI、MediaStore 输出写入、历史页面 UI、
  进程恢复与残留清理——见上方执行步骤未勾选项，留给本任务后续 Stage。
