package com.fittrack.app.di

import android.content.Context
import androidx.room.Room
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.StepsRepository
import com.fittrack.app.data.db.DiaryItemDao
import com.fittrack.app.data.db.FitTrackDatabase
import com.fittrack.app.data.db.FoodItemDao
import com.fittrack.app.data.db.StepsRecordDao
import com.fittrack.app.services.GeminiNanoService
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
    fun provideFitTrackDatabase(@ApplicationContext context: Context): FitTrackDatabase =
        Room.databaseBuilder(context, FitTrackDatabase::class.java, "fittrack.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    @Singleton
    fun provideDiaryItemDao(db: FitTrackDatabase): DiaryItemDao = db.diaryItemDao()

    @Provides
    @Singleton
    fun provideFoodItemDao(db: FitTrackDatabase): FoodItemDao = db.foodItemDao()

    @Provides
    @Singleton
    fun provideStepsRecordDao(db: FitTrackDatabase): StepsRecordDao = db.stepsRecordDao()

    @Provides
    @Singleton
    fun provideFoodRepository(
        diaryItemDao: DiaryItemDao,
        foodItemDao: FoodItemDao,
    ): FoodRepository = FoodRepository(diaryItemDao, foodItemDao)

    @Provides
    @Singleton
    fun provideStepsRepository(stepsRecordDao: StepsRecordDao): StepsRepository =
        StepsRepository(stepsRecordDao)

    @Provides
    @Singleton
    fun provideGoalsRepository(@ApplicationContext context: Context): GoalsRepository =
        GoalsRepository(context)

    /** Singleton so the underlying ML Kit model is initialised only once. */
    @Provides
    @Singleton
    fun provideGeminiNanoService(): GeminiNanoService = GeminiNanoService()
}
