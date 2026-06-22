package com.henjicc.swiftformat.di

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import coil3.ImageLoader
import com.henjicc.swiftformat.core.common.AndroidLogger
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.conversion.ConversionRecoveryManager
import com.henjicc.swiftformat.conversion.ConversionOrchestrator
import com.henjicc.swiftformat.conversion.OutputLocationResolver
import com.henjicc.swiftformat.core.database.ConversionHistoryRepository
import com.henjicc.swiftformat.core.database.SwiftFormatDatabase
import com.henjicc.swiftformat.core.datastore.SettingsRepository
import com.henjicc.swiftformat.core.file.CacheMaintenance
import com.henjicc.swiftformat.core.file.FileMetadataReader
import com.henjicc.swiftformat.core.file.ResultFileActions
import com.henjicc.swiftformat.core.file.ThumbnailImageLoader
import com.henjicc.swiftformat.engine.api.ConversionEngineSelector
import com.henjicc.swiftformat.engine.ffmpeg.FfmpegEngine
import com.henjicc.swiftformat.engine.ffmpeg.FfmpegStillImageEngine
import com.henjicc.swiftformat.engine.image.HeifAvifImageEngine
import com.henjicc.swiftformat.engine.image.NativeImageEngine
import com.henjicc.swiftformat.engine.media.Media3Engine
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 第一版的轻量手动依赖容器（见 SPEC 9.4「初期不过度模块化」）。
 * 后续可整体替换为 Hilt，替换点局限在 Application 与各 ViewModel 工厂。
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val logger: Logger = AndroidLogger
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val cacheMaintenance: CacheMaintenance by lazy { CacheMaintenance(appContext) }
    val fileMetadataReader: FileMetadataReader by lazy { FileMetadataReader(appContext, logger) }
    val resultFileActions: ResultFileActions by lazy { ResultFileActions(appContext, logger) }
    val thumbnailImageLoader: ImageLoader by lazy { ThumbnailImageLoader.build(appContext) }

    private val database: SwiftFormatDatabase by lazy {
        Room.databaseBuilder(appContext, SwiftFormatDatabase::class.java, "swiftformat.db").build()
    }
    val conversionHistoryRepository: ConversionHistoryRepository by lazy {
        ConversionHistoryRepository(database.conversionHistoryDao())
    }

    /** 引擎注册顺序即优先级：原生图片 → HEIC/AVIF → FFmpeg 图片扩展 → Media3 → FFmpeg。 */
    @get:UnstableApi
    val conversionEngineSelector: ConversionEngineSelector by lazy {
        buildConversionEngineSelector()
    }
    private val outputLocationResolver: OutputLocationResolver by lazy { OutputLocationResolver(appContext) }
    val conversionOrchestrator: ConversionOrchestrator by lazy {
        ConversionOrchestrator(conversionEngineSelector, outputLocationResolver, conversionHistoryRepository, logger)
    }
    val conversionRecoveryManager: ConversionRecoveryManager by lazy {
        ConversionRecoveryManager(
            appContext = appContext,
            historyRepository = conversionHistoryRepository,
            metadataReader = fileMetadataReader,
            orchestrator = conversionOrchestrator,
            settingsRepository = settingsRepository,
            logger = logger,
        )
    }

    /** 来自系统分享菜单的文件 Uri；replay=1 让稍后创建的 HomeViewModel 仍能收到。 */
    val incomingShareFiles = MutableSharedFlow<List<Uri>>(replay = 1)

    @UnstableApi
    private fun buildConversionEngineSelector(): ConversionEngineSelector =
        ConversionEngineSelector(
            listOf(
                NativeImageEngine(appContext, logger),
                HeifAvifImageEngine(appContext, logger),
                FfmpegStillImageEngine(appContext, logger),
                Media3Engine(appContext, logger),
                FfmpegEngine(appContext, logger),
            ),
        )
}
