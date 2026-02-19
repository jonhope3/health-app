package com.fittrack.app.services

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
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
        return try {
            HealthConnectClient.getOrCreate(context)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isAvailable(context)) return@withContext false

            val healthClient = HealthConnectClient.getOrCreate(context)
            client = healthClient

            val granted = healthClient.permissionController.getGrantedPermissions()
            if (!granted.containsAll(requiredPermissions)) {
                return@withContext false
            }

            true
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
}
