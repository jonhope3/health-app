package com.hopehealth.app

import android.app.Application
import com.hopehealth.app.data.FoodRepository
import com.hopehealth.app.data.StepsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class HopeHealthApplication : Application() {

    @Inject lateinit var foodRepository: FoodRepository
    @Inject lateinit var stepsRepository: StepsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            foodRepository.cleanupOldData()
            stepsRepository.cleanupOldData()
        }
    }
}
