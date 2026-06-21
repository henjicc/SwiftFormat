package com.henjicc.swiftformat.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionHistoryDao {

    @Insert
    suspend fun insert(entity: ConversionHistoryEntity): Long

    @Update
    suspend fun update(entity: ConversionHistoryEntity)

    @Query("SELECT * FROM conversion_history ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ConversionHistoryEntity>>

    @Query("SELECT * FROM conversion_history WHERE id = :id")
    suspend fun getById(id: Long): ConversionHistoryEntity?

    /** 进程被回收/旋转后用于恢复未完结任务（见 SPEC 13.1）。 */
    @Query("SELECT * FROM conversion_history WHERE status IN ('PENDING', 'PREPARING', 'CONVERTING', 'SAVING')")
    suspend fun getActiveRecords(): List<ConversionHistoryEntity>

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
