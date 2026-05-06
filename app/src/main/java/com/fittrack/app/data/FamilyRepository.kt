package com.fittrack.app.data

import com.fittrack.app.data.db.CycleRecordDao
import com.fittrack.app.data.db.CycleRecordEntity
import com.fittrack.app.data.db.DailyCycleLogDao
import com.fittrack.app.data.db.DailyCycleLogEntity
import com.fittrack.app.data.db.TemperatureReadingDao
import com.fittrack.app.data.db.TemperatureReadingEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for all Family / cycle tracking data.
 *
 * Owns [CycleRecordDao], [DailyCycleLogDao], and [TemperatureReadingDao].
 * Business logic for *predictions* lives in [com.fittrack.app.services.CyclePredictionEngine].
 */
@Singleton
class FamilyRepository @Inject constructor(
    private val cycleDao: CycleRecordDao,
    private val logDao: DailyCycleLogDao,
    private val tempDao: TemperatureReadingDao,
) {

    // ── Cycle records ────────────────────────────────────────────────────────

    fun observeAllCycles(): Flow<List<CycleRecordEntity>> = cycleDao.getAllFlow()

    suspend fun getAllCycles(): List<CycleRecordEntity> = cycleDao.getAll()

    suspend fun getCurrentCycle(): CycleRecordEntity? = cycleDao.getCurrentCycle()

    /**
     * Start a new cycle. If there's a current unclosed cycle, close it first.
     * Returns the new cycle ID.
     */
    suspend fun startNewCycle(periodStartDate: LocalDate): Long {
        // Close any unclosed cycle
        val current = cycleDao.getCurrentCycle()
        if (current != null) {
            val length = ChronoUnit.DAYS.between(
                LocalDate.parse(current.startDate), periodStartDate
            ).toInt()
            cycleDao.update(current.copy(cycleLength = length))
        }

        return cycleDao.upsert(
            CycleRecordEntity(
                startDate = periodStartDate.toString(),
                endDate = null,
                cycleLength = null,
                periodLength = null,
                ovulationDate = null,
            )
        )
    }

    /** Mark period end for the current cycle. */
    suspend fun endPeriod(endDate: LocalDate) {
        val current = cycleDao.getCurrentCycle() ?: return
        val periodLength = ChronoUnit.DAYS.between(
            LocalDate.parse(current.startDate), endDate
        ).toInt() + 1 // inclusive
        cycleDao.update(current.copy(endDate = endDate.toString(), periodLength = periodLength))
    }

    suspend fun updateCycle(cycle: CycleRecordEntity) = cycleDao.update(cycle)

    // ── Daily logs ───────────────────────────────────────────────────────────

    suspend fun getLogForDate(date: LocalDate): DailyCycleLogEntity? =
        logDao.getForDate(date.toString())

    fun observeLogForDate(date: LocalDate): Flow<DailyCycleLogEntity?> =
        logDao.observeForDate(date.toString())

    fun observeLogsForMonth(year: Int, month: Int): Flow<List<DailyCycleLogEntity>> {
        val start = LocalDate.of(year, month, 1)
        val end = start.plusMonths(1).minusDays(1)
        return logDao.observeForDateRange(start.toString(), end.toString())
    }

    suspend fun getLogsForDateRange(start: LocalDate, end: LocalDate): List<DailyCycleLogEntity> =
        logDao.getForDateRange(start.toString(), end.toString())

    suspend fun saveLog(log: DailyCycleLogEntity) = logDao.upsert(log)

    // ── Temperature ──────────────────────────────────────────────────────────

    suspend fun saveTemperature(reading: TemperatureReadingEntity) = tempDao.upsert(reading)

    suspend fun getTemperaturesForRange(start: LocalDate, end: LocalDate) =
        tempDao.getForDateRange(start.toString(), end.toString())

    // ── Cleanup ──────────────────────────────────────────────────────────────

    suspend fun deleteAllFamilyData() {
        cycleDao.deleteAll()
        logDao.deleteAll()
        tempDao.deleteAll()
    }
}
