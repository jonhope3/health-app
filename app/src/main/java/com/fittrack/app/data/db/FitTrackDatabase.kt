package com.fittrack.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database definition.
 *
 * - v1 → v2: Adds `user_settings` table replacing DataStore for user goals/preferences.
 *
 * The database instance is provided as a singleton by Hilt ([com.fittrack.app.di.AppModule]).
 * Do not instantiate this class directly — always inject [FitTrackDatabase],
 * or preferably the individual DAOs, into your dependencies.
 */
@Database(
    entities = [
        DiaryItemEntity::class,
        FoodItemEntity::class,
        StepsRecordEntity::class,
        UserSettingsEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FitTrackDatabase : RoomDatabase() {
    abstract fun diaryItemDao(): DiaryItemDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun stepsRecordDao(): StepsRecordDao
    abstract fun userSettingsDao(): UserSettingsDao
}

/** v1 → v2: Adds the user_settings singleton table. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_settings (
                id              INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                calorieGoal     INTEGER NOT NULL DEFAULT 2000,
                stepGoal        INTEGER NOT NULL DEFAULT 10000,
                weightLbs       REAL    NOT NULL DEFAULT 160.0,
                heightIn        REAL    NOT NULL DEFAULT 68.0,
                age             INTEGER NOT NULL DEFAULT 30,
                proteinGoalG    INTEGER NOT NULL DEFAULT 100,
                carbsGoalG      INTEGER NOT NULL DEFAULT 250,
                fatGoalG        INTEGER NOT NULL DEFAULT 65,
                sugarGoalG      INTEGER NOT NULL DEFAULT 50,
                nickname        TEXT    NOT NULL DEFAULT '',
                onboardingDone  INTEGER NOT NULL DEFAULT 0,
                themeMode       TEXT    NOT NULL DEFAULT 'LIGHT'
            )
            """.trimIndent()
        )
    }
}
