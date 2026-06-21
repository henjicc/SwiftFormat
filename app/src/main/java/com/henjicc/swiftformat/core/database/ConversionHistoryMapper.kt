package com.henjicc.swiftformat.core.database

import android.net.Uri
import com.henjicc.swiftformat.core.model.ConversionHistoryRecord
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePresetCodec

/** [ConversionHistoryEntity] ↔ [ConversionHistoryRecord] 互转，依赖 [Uri] 故不在 JVM 单元测试覆盖范围。 */
object ConversionHistoryMapper {

    fun toEntity(record: ConversionHistoryRecord): ConversionHistoryEntity = ConversionHistoryEntity(
        id = record.id,
        originalDisplayName = record.originalDisplayName,
        originalFormat = record.originalFormat,
        outputFormat = record.outputFormat,
        mediaType = record.mediaType.name,
        inputUri = record.inputUri.toString(),
        startTime = record.startTime,
        endTime = record.endTime,
        status = record.status.name,
        outputUri = record.outputUri?.toString(),
        outputSizeBytes = record.outputSizeBytes,
        failureReason = record.failureReason,
        quality = record.quality?.name,
        size = SizePresetCodec.encode(record.size),
    )

    fun toRecord(entity: ConversionHistoryEntity): ConversionHistoryRecord = ConversionHistoryRecord(
        id = entity.id,
        originalDisplayName = entity.originalDisplayName,
        originalFormat = entity.originalFormat,
        outputFormat = entity.outputFormat,
        mediaType = MediaType.valueOf(entity.mediaType),
        inputUri = Uri.parse(entity.inputUri),
        startTime = entity.startTime,
        endTime = entity.endTime,
        status = ConversionStatus.valueOf(entity.status),
        outputUri = entity.outputUri?.let(Uri::parse),
        outputSizeBytes = entity.outputSizeBytes,
        failureReason = entity.failureReason,
        quality = entity.quality?.let(QualityPreset::valueOf),
        size = SizePresetCodec.decode(entity.size),
    )
}
