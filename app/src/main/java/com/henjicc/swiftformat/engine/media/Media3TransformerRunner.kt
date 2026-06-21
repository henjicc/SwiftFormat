package com.henjicc.swiftformat.engine.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.engine.api.ConversionProgress
import com.henjicc.swiftformat.engine.api.ConversionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
internal class Media3TransformerRunner(
    private val appContext: Context,
) {
    private val activeTransformers = ConcurrentHashMap<String, Transformer>()

    suspend fun convert(
        request: ConversionRequest,
        config: Media3ConversionConfig,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        val destinationUri = (request.destination as? OutputDestination.ResolvedUri)?.uri
            ?: return ConversionResult.Failure(ConversionError(ConversionError.Kind.OUTPUT_NOT_WRITABLE, "destination not resolved"))

        onProgress(ConversionProgress(0f))

        val tempFile = File(appContext.cacheDir, "media3_${request.id}.tmp")
        return try {
            when (val outcome = export(request, config, tempFile, onProgress)) {
                is TransformOutcome.Error -> ConversionResult.Failure(
                    ConversionError(
                        kind = mapMedia3Error(outcome.exception.errorCode),
                        debugMessage = outcome.exception.message ?: "export failed",
                    ),
                )

                is TransformOutcome.Success -> {
                    val sizeBytes = withContext(Dispatchers.IO) {
                        copyToDestination(tempFile, destinationUri)
                    }
                    onProgress(ConversionProgress(1f))
                    ConversionResult.Success(destinationUri, sizeBytes)
                }
            }
        } finally {
            tempFile.delete()
            activeTransformers.remove(request.id)
        }
    }

    fun cancel(taskId: String) {
        activeTransformers[taskId]?.cancel()
    }

    private suspend fun export(
        request: ConversionRequest,
        config: Media3ConversionConfig,
        tempFile: File,
        onProgress: (ConversionProgress) -> Unit,
    ): TransformOutcome = coroutineScope {
        val resultDeferred = CompletableDeferred<TransformOutcome>()
        val transformer = Transformer.Builder(appContext)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(config.encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    resultDeferred.complete(TransformOutcome.Success(exportResult))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    resultDeferred.complete(TransformOutcome.Error(exportException))
                }
            })
            .build()

        activeTransformers[request.id] = transformer
        transformer.start(config.editedItem, tempFile.absolutePath)

        val progressJob = launch {
            val holder = ProgressHolder()
            while (isActive) {
                val state = transformer.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    // 导出阶段占总进度 0~90%，剩余 10% 留给临时文件拷贝到目标 Uri。
                    onProgress(ConversionProgress(holder.progress / 100f * 0.9f))
                }
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }

        val result = resultDeferred.await()
        progressJob.cancel()
        transformer.cancel()
        result
    }

    private fun copyToDestination(tempFile: File, destinationUri: Uri): Long {
        val outputStream = appContext.contentResolver.openOutputStream(destinationUri)
            ?: error("cannot open output stream")
        outputStream.use { out ->
            tempFile.inputStream().use { input -> input.copyTo(out) }
        }
        return tempFile.length()
    }

    private sealed interface TransformOutcome {
        data class Success(val exportResult: ExportResult) : TransformOutcome
        data class Error(val exception: ExportException) : TransformOutcome
    }

    private companion object {
        const val PROGRESS_POLL_INTERVAL_MS = 300L
    }
}
