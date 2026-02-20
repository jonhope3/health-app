package com.fittrack.app.services

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import android.util.Log
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthConnectService {

    private var client: HealthConnectClient? = null

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    fun isAvailable(context: Context): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        Log.d("FitTrack_HC", "isAvailable check: status=$status, SDK_AVAILABLE=${HealthConnectClient.SDK_AVAILABLE}")
        return status == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("FitTrack_HC", "initialize() called")
            if (!isAvailable(context)) {
                Log.d("FitTrack_HC", "initialize failed: not available")
                return@withContext false
            }

            val healthClient = HealthConnectClient.getOrCreate(context)
            client = healthClient
            Log.d("FitTrack_HC", "healthClient created")

            val granted = healthClient.permissionController.getGrantedPermissions()
            Log.d("FitTrack_HC", "granted permissions: $granted")
            Log.d("FitTrack_HC", "required permissions: $requiredPermissions")
            
            val isAllGranted = granted.containsAll(requiredPermissions)
            Log.d("FitTrack_HC", "isAllGranted: $isAllGranted")
            
            isAllGranted
        } catch (e: Exception) {
            Log.e("FitTrack_HC", "initialize exception: ${e.message}", e)
            false
        }
    }

    suspend fun readStepsToday(): Int = withContext(Dispatchers.IO) {
        try {
            val healthClient = client ?: return@withContext 0
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val endTime = Instant.now()

            val response = healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endTime)
                )
            )
            (response[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun readStepsHistory(days: Int = 7): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val healthClient = client ?: return@withContext emptyList()
            val zone = ZoneId.systemDefault()
            val results = mutableListOf<Pair<String, Int>>()

            for (i in 0 until days) {
                val date = LocalDate.now(zone).minusDays(i.toLong())
                val startOfDay = date.atStartOfDay(zone).toInstant()
                val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant()

                val response = healthClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
                val steps = (response[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
                results.add(date.toString() to steps)
            }

            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun readCaloriesBurnedToday(): Int = withContext(Dispatchers.IO) {
        try {
            val healthClient = client ?: return@withContext 0
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val endTime = Instant.now()

            val response = healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endTime)
                )
            )
            val energy = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            energy?.inKilocalories?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getSettingsIntents(context: Context): List<android.content.Intent> {
        val intents = mutableListOf<android.content.Intent>()
        
        // 1. Specific manager (often restricted but worth a try)
        intents.add(android.content.Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS").apply {
            putExtra(android.content.Intent.EXTRA_PACKAGE_NAME, context.packageName)
        })
        
        // 2. Generic platform settings
        intents.add(android.content.Intent("android.settings.HEALTH_CONNECT_SETTINGS"))
        
        // 3. Library action
        intents.add(android.content.Intent("androidx.health.connect.action.HEALTH_CONNECT_SETTINGS"))
        
        // 4. Guaranteed fallback: App Info
        intents.add(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        })
        
        return intents
    }
}
