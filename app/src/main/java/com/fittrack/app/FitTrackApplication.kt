package com.fittrack.app

import android.app.Application
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.StepsRepository

class FitTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FoodRepository(this).cleanupOldData()
        StepsRepository(this).cleanupOldData()
    }
}
