package com.hopehealth.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database definition.
 *
 * - v1 → v2: Adds `user_settings` table replacing DataStore for user goals/preferences.
 * - v2 → v3: Adds Family feature tables (`cycle_record`, `daily_cycle_log`,
 *   `temperature_reading`) and three new columns on `user_settings`.
 *
 * The database instance is provided as a singleton by Hilt ([com.hopehealth.app.di.AppModule]).
 * Do not instantiate this class directly — always inject [HopeHealthDatabase],
 * or preferably the individual DAOs, into your dependencies.
 */
@Database(
    entities = [
        DiaryItemEntity::class,
        FoodItemEntity::class,
        StepsRecordEntity::class,
        UserSettingsEntity::class,
        CycleRecordEntity::class,
        DailyCycleLogEntity::class,
        TemperatureReadingEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HopeHealthDatabase : RoomDatabase() {
    abstract fun diaryItemDao(): DiaryItemDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun stepsRecordDao(): StepsRecordDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun cycleRecordDao(): CycleRecordDao
    abstract fun dailyCycleLogDao(): DailyCycleLogDao
    abstract fun temperatureReadingDao(): TemperatureReadingDao
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

/** v2 → v3: Adds Family feature tables and columns on user_settings. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── New columns on user_settings ──
        db.execSQL("ALTER TABLE user_settings ADD COLUMN familyEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN familyMode TEXT NOT NULL DEFAULT 'TRACKING'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN lastPeriodStart TEXT DEFAULT NULL")

        // ── cycle_record ──
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS cycle_record (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                startDate            TEXT    NOT NULL,
                endDate              TEXT,
                cycleLength          INTEGER,
                periodLength         INTEGER,
                ovulationDate        TEXT,
                ovulationConfidence  TEXT    NOT NULL DEFAULT 'LOW'
            )
        """.trimIndent())

        // ── daily_cycle_log ──
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS daily_cycle_log (
                date              TEXT PRIMARY KEY NOT NULL,
                cycleRecordId     INTEGER,
                cycleDay          INTEGER,
                phase             TEXT,
                flowIntensity     TEXT,
                cervicalMucus     TEXT,
                symptoms          TEXT    NOT NULL DEFAULT '',
                mood              TEXT,
                sexDrive          TEXT,
                sexualActivity    TEXT,
                temperature       REAL,
                temperatureSource TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_cycle_log_date ON daily_cycle_log(date)")

        // ── temperature_reading ──
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS temperature_reading (
                id             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date           TEXT    NOT NULL,
                temperatureF   REAL    NOT NULL,
                source         TEXT    NOT NULL,
                timestamp      INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_temperature_reading_date_source ON temperature_reading(date, source)")
    }
}

/** v3 → v4: Adds burned-calorie columns to steps_record. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE steps_record ADD COLUMN activeBurnedCal INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE steps_record ADD COLUMN manualBurnedCal INTEGER NOT NULL DEFAULT 0")
    }
}
