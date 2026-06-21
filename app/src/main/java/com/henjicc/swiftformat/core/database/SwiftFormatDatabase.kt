package com.henjicc.swiftformat.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ConversionHistoryEntity::class], version = 1, exportSchema = true)
abstract class SwiftFormatDatabase : RoomDatabase() {
    abstract fun conversionHistoryDao(): ConversionHistoryDao
}
