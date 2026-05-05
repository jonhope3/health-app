package com.fittrack.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database definition.
 *
 * The database instance is provided as a singleton by Hilt ([com.fittrack.app.di.AppModule]).
 * Do not instantiate this class directly — always inject [FitTrackDatabase],
 * or preferably the individual DAOs, into your dependencies.
 */
@Database(
    entities = [DiaryItemEntity::class, FoodItemEntity::class, StepsRecordEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun diaryItemDao(): DiaryItemDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun stepsRecordDao(): StepsRecordDao
}
