package com.fittrack.app.services

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import android.util.Log
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
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

    /** Additional permissions requested lazily when the Family feature is first accessed. */
    val familyPermissions = setOf(
        HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
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

    /** Check if family (temperature) permissions are already granted. */
    suspend fun hasFamilyPermissions(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val healthClient = client ?: HealthConnectClient.getOrCreate(context).also { client = it }
            val granted = healthClient.permissionController.getGrantedPermissions()
            granted.containsAll(familyPermissions)
        } catch (e: Exception) {
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

    /**
     * Read skin/body temperature from Health Connect for the given date range.
     * Returns list of (date ISO string, temperature in °F, source label).
     * Tries BasalBodyTemperatureRecord first, falls back to BodyTemperatureRecord.
     */
    suspend fun readTemperatureHistory(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Triple<String, Float, String>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Triple<String, Float, String>>()
        val healthClient = client ?: return@withContext results
        val zone = ZoneId.systemDefault()
        val startInstant = startDate.atStartOfDay(zone).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(zone).toInstant()

        try {
            // BBT records (from Pixel Watch sleep tracking)
            val bbtResponse = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = BasalBodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                )
            )
            bbtResponse.records.forEach { record ->
                val date = record.time.atZone(zone).toLocalDate().toString()
                val tempC = record.temperature.inCelsius
                val tempF = (tempC * 9.0 / 5.0 + 32.0).toFloat()
                results.add(Triple(date, tempF, "HEALTH_CONNECT_BBT"))
            }
        } catch (e: Exception) {
            Log.d("FitTrack_HC", "BBT read failed: ${e.message}")
        }

        try {
            // Body temperature records (general)
            val bodyResponse = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = BodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant),
                )
            )
            bodyResponse.records.forEach { record ->
                val date = record.time.atZone(zone).toLocalDate().toString()
                val tempC = record.temperature.inCelsius
                val tempF = (tempC * 9.0 / 5.0 + 32.0).toFloat()
                results.add(Triple(date, tempF, "HEALTH_CONNECT_SKIN"))
            }
        } catch (e: Exception) {
            Log.d("FitTrack_HC", "Body temp read failed: ${e.message}")
        }

        results
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

