package com.henjicc.swiftformat.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 转换历史的持久化形状（见 SPEC 14）。字段全部为 DB 友好的基础类型，
 * 枚举/Uri/[com.henjicc.swiftformat.core.model.SizePreset] 等领域类型的转换收敛在 [ConversionHistoryMapper]。
 */
@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalDisplayName: String,
    val originalFormat: String?,
    val outputFormat: String,
    val mediaType: String,
    val inputUri: String,
    val startTime: Long,
    val endTime: Long?,
    val status: String,
    val outputUri: String?,
    val outputSizeBytes: Long?,
    val failureReason: String?,
    val quality: String?,
    val size: String?,
)
