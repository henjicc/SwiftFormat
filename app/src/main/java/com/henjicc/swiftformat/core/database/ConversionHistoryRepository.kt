package com.henjicc.swiftformat.core.database

import com.henjicc.swiftformat.core.model.ConversionHistoryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** UI/业务层只通过本类访问转换历史，不直接接触 Room 的 Entity/Dao（见 SPEC 9.4 分层约束）。 */
class ConversionHistoryRepository(private val dao: ConversionHistoryDao) {

    fun observeAll(): Flow<List<ConversionHistoryRecord>> =
        dao.observeAll().map { entities -> entities.map(ConversionHistoryMapper::toRecord) }

    suspend fun insert(record: ConversionHistoryRecord): Long = dao.insert(ConversionHistoryMapper.toEntity(record))

    suspend fun update(record: ConversionHistoryRecord) = dao.update(ConversionHistoryMapper.toEntity(record))

    suspend fun getById(id: Long): ConversionHistoryRecord? = dao.getById(id)?.let(ConversionHistoryMapper::toRecord)

    suspend fun getActiveRecords(): List<ConversionHistoryRecord> =
        dao.getActiveRecords().map(ConversionHistoryMapper::toRecord)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
