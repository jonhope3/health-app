package com.hopehealth.app.data

import com.hopehealth.app.data.db.StepsRecordDao
import com.hopehealth.app.data.db.StepsRecordEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for step-count history.
 *
 * Each day is stored as a single row keyed by ISO-8601 date string.
 * Upserting a new count for a date replaces the previous value.
 */
class StepsRepository(private val dao: StepsRecordDao) {

    /** Reactive stream of the most recent [limit] days of step records. */
    fun getRecentFlow(limit: Int = 7): Flow<List<Pair<String, Int>>> =
        dao.getRecentFlow(limit).map { records -> records.map { it.date to it.steps } }

    suspend fun getSteps(date: String): Int = dao.getForDate(date)?.steps ?: 0

    suspend fun saveSteps(count: Int, date: String) {
        val existing = dao.getForDate(date)
        dao.upsert(StepsRecordEntity(
            date = date, steps = count,
            activeBurnedCal = existing?.activeBurnedCal ?: 0,
            manualBurnedCal = existing?.manualBurnedCal ?: 0,
        ))
    }

    /** Save Health Connect active calories for a given day. */
    suspend fun saveActiveBurnedCal(cal: Int, date: String) {
        val existing = dao.getForDate(date)
        dao.upsert(StepsRecordEntity(
            date = date,
            steps = existing?.steps ?: 0,
            activeBurnedCal = cal,
            manualBurnedCal = existing?.manualBurnedCal ?: 0,
        ))
    }

    /** Add manually entered exercise calories for a given day (additive). */
    suspend fun addManualBurnedCal(cal: Int, date: String) {
        val existing = dao.getForDate(date)
        val prev = existing?.manualBurnedCal ?: 0
        dao.upsert(StepsRecordEntity(
            date = date,
            steps = existing?.steps ?: 0,
            activeBurnedCal = existing?.activeBurnedCal ?: 0,
            manualBurnedCal = prev + cal,
        ))
    }

    /** Total burned (active + manual) for a given day. */
    suspend fun getTotalBurned(date: String): Int {
        val record = dao.getForDate(date) ?: return 0
        return record.activeBurnedCal + record.manualBurnedCal
    }

    /** Override total burned to an exact value (zeroes active, sets manual). */
    suspend fun setTotalBurned(cal: Int, date: String) {
        val existing = dao.getForDate(date)
        dao.upsert(StepsRecordEntity(
            date = date,
            steps = existing?.steps ?: 0,
            activeBurnedCal = 0,
            manualBurnedCal = cal,
        ))
    }

    /**
     * Returns (date, stepCount) pairs for the last [days] days, newest first.
     * Days with no record produce a 0 count.
     */
    suspend fun getStepsHistory(days: Int = 7): List<Pair<String, Int>> {
        val today = LocalDate.now()
        val dates = List(days) { i -> today.minusDays(i.toLong()).toString() }
        val rowMap = dao.getForDates(dates).associate { it.date to it.steps }
        return dates.map { it to (rowMap[it] ?: 0) }
    }

    suspend fun cleanupOldData(retainDays: Int = 90) {
        val cutoff = LocalDate.now().minusDays(retainDays.toLong()).toString()
        dao.deleteOlderThan(cutoff)
    }
}
