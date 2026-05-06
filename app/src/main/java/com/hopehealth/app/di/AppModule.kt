package com.hopehealth.app.di

import android.content.Context
import androidx.room.Room
import com.hopehealth.app.data.FoodRepository
import com.hopehealth.app.data.GoalsRepository
import com.hopehealth.app.data.StepsRepository
import com.hopehealth.app.data.db.CycleRecordDao
import com.hopehealth.app.data.db.DailyCycleLogDao
import com.hopehealth.app.data.db.DiaryItemDao
import com.hopehealth.app.data.db.HopeHealthDatabase
import com.hopehealth.app.data.db.FoodItemDao
import com.hopehealth.app.data.db.MIGRATION_1_2
import com.hopehealth.app.data.db.MIGRATION_2_3
import com.hopehealth.app.data.db.MIGRATION_3_4
import com.hopehealth.app.data.db.StepsRecordDao
import com.hopehealth.app.data.db.TemperatureReadingDao
import com.hopehealth.app.data.db.UserSettingsDao
import com.hopehealth.app.services.GeminiNanoService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHopeHealthDatabase(@ApplicationContext context: Context): HopeHealthDatabase =
        Room.databaseBuilder(context, HopeHealthDatabase::class.java, "hopehealth.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides @Singleton
    fun provideDiaryItemDao(db: HopeHealthDatabase): DiaryItemDao = db.diaryItemDao()

    @Provides @Singleton
    fun provideFoodItemDao(db: HopeHealthDatabase): FoodItemDao = db.foodItemDao()

    @Provides @Singleton
    fun provideStepsRecordDao(db: HopeHealthDatabase): StepsRecordDao = db.stepsRecordDao()

    @Provides @Singleton
    fun provideUserSettingsDao(db: HopeHealthDatabase): UserSettingsDao = db.userSettingsDao()

    @Provides @Singleton
    fun provideFoodRepository(
        diaryItemDao: DiaryItemDao,
        foodItemDao: FoodItemDao,
    ): FoodRepository = FoodRepository(diaryItemDao, foodItemDao)

    @Provides @Singleton
    fun provideStepsRepository(stepsRecordDao: StepsRecordDao): StepsRepository =
        StepsRepository(stepsRecordDao)

    // GoalsRepository uses @Inject constructor — Hilt resolves it automatically.
    // No explicit @Provides needed.

    /** Singleton so the underlying ML Kit model is initialised only once. */
    @Provides @Singleton
    fun provideGeminiNanoService(@ApplicationContext context: Context): GeminiNanoService =
        GeminiNanoService(context)

    @Provides @Singleton
    fun provideCycleRecordDao(db: HopeHealthDatabase): CycleRecordDao = db.cycleRecordDao()

    @Provides @Singleton
    fun provideDailyCycleLogDao(db: HopeHealthDatabase): DailyCycleLogDao = db.dailyCycleLogDao()

    @Provides @Singleton
    fun provideTemperatureReadingDao(db: HopeHealthDatabase): TemperatureReadingDao = db.temperatureReadingDao()
}
