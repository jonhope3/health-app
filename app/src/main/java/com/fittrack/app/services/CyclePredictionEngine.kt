package com.fittrack.app.services

import com.fittrack.app.data.CervicalMucusType
import com.fittrack.app.data.CyclePhase
import com.fittrack.app.data.PredictionConfidence
import com.fittrack.app.data.db.CycleRecordEntity
import com.fittrack.app.data.db.DailyCycleLogEntity
import com.fittrack.app.data.db.TemperatureReadingEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hybrid cycle prediction engine with three signal sources:
 *
 *  Calendar: Baseline rhythm method — avg cycle length − 14.
 *  Temperature: Source-aware biphasic shift detection with smoothing.
 *  Symptoms: Cross-reference cervical mucus + sex drive + physical symptoms.
 *
 * Each additional signal source refines and can upgrade the confidence level.
 */
@Singleton
class CyclePredictionEngine @Inject constructor() {

    companion object {
        private const val DEFAULT_CYCLE_LENGTH = 28
        private const val LUTEAL_PHASE_LENGTH = 14
        private const val FERTILE_WINDOW_BEFORE_OVULATION = 5
        private const val TYPICAL_PERIOD_LENGTH = 5
        private const val TEMP_SHIFT_THRESHOLD_F = 0.2f
        private const val TEMP_SHIFT_SUSTAINED_DAYS = 3
    }

    data class CyclePrediction(
        val nextPeriodDate: LocalDate,
        val ovulationDate: LocalDate,
        val fertileWindowStart: LocalDate,
        val fertileWindowEnd: LocalDate,
        val confidence: PredictionConfidence,
        val averageCycleLength: Int,
        // Temperature outputs
        val temperatureShiftDetected: Boolean = false,
        val confirmedOvulationDate: LocalDate? = null,
        val coverlineTemp: Float? = null,
        val tempDataQuality: TempDataQuality = TempDataQuality.NONE,
        // Symptom outputs
        val symptomSignals: List<String> = emptyList(),
        // Conception probability (TTC mode)
        val conceptionProbability: Float = 0f,
    )

    // ── Calendar method ─────────────────────────────────────────────────────

    fun predict(
        completedCycles: List<CycleRecordEntity>,
        currentCycleStart: LocalDate,
        temperatureReadings: List<TemperatureReadingEntity> = emptyList(),
        dailyLogs: List<DailyCycleLogEntity> = emptyList(),
    ): CyclePrediction {
        val knownLengths = completedCycles.mapNotNull { it.cycleLength }
        val avgLength = if (knownLengths.isNotEmpty()) {
            knownLengths.average().toInt()
        } else {
            DEFAULT_CYCLE_LENGTH
        }

        // Base calendar prediction
        var confidence = when {
            knownLengths.size >= 6 -> PredictionConfidence.MEDIUM
            knownLengths.size >= 3 -> PredictionConfidence.MEDIUM
            knownLengths.isNotEmpty() -> PredictionConfidence.LOW
            else -> PredictionConfidence.LOW
        }

        val ovulationDay = avgLength - LUTEAL_PHASE_LENGTH
        var ovulationDate = currentCycleStart.plusDays(ovulationDay.toLong())
        val fertileStart = ovulationDate.minusDays(FERTILE_WINDOW_BEFORE_OVULATION.toLong())
        val fertileEnd = ovulationDate.plusDays(1)
        val nextPeriod = currentCycleStart.plusDays(avgLength.toLong())

        // ── Temperature shift detection ─────────────────────────────
        val tempResult = detectTemperatureShift(temperatureReadings, currentCycleStart)
        var confirmedOvulation: LocalDate? = null

        if (tempResult.shiftDetected && tempResult.shiftDate != null) {
            confirmedOvulation = tempResult.shiftDate.minusDays(1)
            ovulationDate = confirmedOvulation
            // High-quality BBT data gets full confidence boost;
            // noisy/daytime data gets a half boost (still useful).
            if (tempResult.dataQuality == TempDataQuality.HIGH ||
                tempResult.dataQuality == TempDataQuality.MODERATE
            ) {
                confidence = upgradeConfidence(confidence)
            }
        }

        // ── Symptom cross-reference ─────────────────────────────────────
        val symptomSignals = mutableListOf<String>()
        val symptomResult = analyzeSymptoms(dailyLogs, currentCycleStart, ovulationDate)

        if (symptomResult.eggWhiteMucusNearOvulation) {
            symptomSignals.add("🥚 Egg-white CM detected near predicted ovulation")
            if (!tempResult.shiftDetected) confidence = upgradeConfidence(confidence)
        }
        if (symptomResult.highSexDriveNearOvulation) {
            symptomSignals.add("🔥 High sex drive correlates with fertile window")
        }
        if (symptomResult.breastTendernessInLuteal) {
            symptomSignals.add("🫶 Breast tenderness suggests progesterone rise (luteal phase)")
        }
        if (symptomResult.fatigueInLuteal) {
            symptomSignals.add("😴 Fatigue in luteal phase — progesterone dominant")
        }

        // ── Conception probability (for TTC mode) ────────────────────────────
        val today = LocalDate.now()
        val conceptionProb = calculateConceptionProbability(
            today, ovulationDate, fertileStart, fertileEnd,
            tempResult.shiftDetected, symptomResult.eggWhiteMucusNearOvulation,
        )

        return CyclePrediction(
            nextPeriodDate = nextPeriod,
            ovulationDate = ovulationDate,
            fertileWindowStart = fertileStart,
            fertileWindowEnd = fertileEnd,
            confidence = confidence,
            averageCycleLength = avgLength,
            temperatureShiftDetected = tempResult.shiftDetected,
            confirmedOvulationDate = confirmedOvulation,
            coverlineTemp = tempResult.coverline,
            tempDataQuality = tempResult.dataQuality,
            symptomSignals = symptomSignals,
            conceptionProbability = conceptionProb,
        )
    }

    // ── Temperature analysis ─────────────────────────────────────────────────

    data class TempShiftResult(
        val shiftDetected: Boolean,
        val shiftDate: LocalDate?,
        val coverline: Float?,
        val dataQuality: TempDataQuality = TempDataQuality.NONE,
    )

    enum class TempDataQuality { NONE, LOW, MODERATE, HIGH }

    /**
     * Source-aware temperature shift detection.
     *
     * 1. Normalizes readings by source — skin/watch temps get a
     *    +1.0°F correction to approximate oral BBT equivalence.
     * 2. Groups by date, resolving multiple readings per day by
     *    preferring manual BBT > sleep BBT > daytime skin.
     * 3. Rejects outliers (IQR fence) to handle noisy daytime data.
     * 4. Applies a 3-day moving average to smooth remaining noise.
     * 5. Detects biphasic shift: smoothed temp rises ≥ threshold
     *    above coverline and stays elevated for 3+ days.
     *
     * For non-BBT sources the shift threshold is relaxed (0.15°F
     * instead of 0.2°F) and confidence boost is reduced.
     */
    private fun detectTemperatureShift(
        readings: List<TemperatureReadingEntity>,
        cycleStart: LocalDate,
    ): TempShiftResult {
        if (readings.isEmpty()) return TempShiftResult(false, null, null)

        // ── 1. Normalize by source ──────────────────────────────
        val SOURCE_PRIORITY = mapOf(
            "MANUAL" to 3,
            "HEALTH_CONNECT_BBT" to 2,
            "HEALTH_CONNECT_SKIN" to 1,
        )
        val SKIN_TO_BBT_OFFSET = 1.0f  // wrist skin → oral BBT approx

        data class NormalizedReading(
            val date: String,
            val normalizedTempF: Float,
            val source: String,
            val priority: Int,
        )

        val normalized = readings.map { r ->
            val offset = when (r.source) {
                "HEALTH_CONNECT_SKIN" -> SKIN_TO_BBT_OFFSET
                "HEALTH_CONNECT_BBT" -> 0.3f  // slight offset for wrist vs oral
                else -> 0f
            }
            NormalizedReading(
                date = r.date,
                normalizedTempF = r.temperatureF + offset,
                source = r.source,
                priority = SOURCE_PRIORITY[r.source] ?: 0,
            )
        }

        // ── 2. Best reading per day (highest priority source) ───
        val dailyTemps = normalized
            .groupBy { it.date }
            .mapValues { (_, dayReadings) ->
                dayReadings.maxByOrNull { it.priority }!!
            }
            .toSortedMap()

        if (dailyTemps.size < 5) {
            // Not enough data for shift, but still return a coverline
            val avg = dailyTemps.values.map { it.normalizedTempF }.average().toFloat()
            return TempShiftResult(false, null, avg, TempDataQuality.LOW)
        }

        // ── 3. Outlier rejection (IQR method) ───────────────────
        val allTemps = dailyTemps.values.map { it.normalizedTempF }.sorted()
        val q1 = allTemps[allTemps.size / 4]
        val q3 = allTemps[allTemps.size * 3 / 4]
        val iqr = q3 - q1
        val lowerFence = q1 - 1.5f * iqr
        val upperFence = q3 + 1.5f * iqr

        val filtered = dailyTemps.filter { (_, r) ->
            r.normalizedTempF in lowerFence..upperFence
        }.entries.toList()

        if (filtered.size < 5) {
            return TempShiftResult(false, null, null, TempDataQuality.LOW)
        }

        // ── 4. Moving average smoothing (3-day window) ──────────
        val smoothed = filtered.mapIndexed { i, entry ->
            val window = filtered.subList(
                maxOf(0, i - 1), minOf(filtered.size, i + 2)
            )
            val avg = window.map { it.value.normalizedTempF }.average().toFloat()
            entry.key to avg
        }

        // ── 5. Determine data quality ───────────────────────────
        val hasBbt = dailyTemps.values.any {
            it.source == "MANUAL" || it.source == "HEALTH_CONNECT_BBT"
        }
        val quality = when {
            hasBbt && filtered.size >= 8 -> TempDataQuality.HIGH
            hasBbt || filtered.size >= 8 -> TempDataQuality.MODERATE
            else -> TempDataQuality.LOW
        }

        // ── 6. Biphasic shift detection ─────────────────────────
        // Relax threshold for lower-quality data
        val threshold = if (quality == TempDataQuality.HIGH) {
            TEMP_SHIFT_THRESHOLD_F
        } else {
            0.15f
        }

        if (smoothed.size < 8) {
            val avg = smoothed.map { it.second }.average().toFloat()
            return TempShiftResult(false, null, avg, quality)
        }

        for (i in 6 until smoothed.size - 2) {
            val coverline = smoothed.subList(0, i)
                .map { it.second }.average().toFloat()
            val day1 = smoothed[i].second
            val day2 = smoothed[i + 1].second
            val day3 = smoothed[i + 2].second

            if (day1 >= coverline + threshold &&
                day2 >= coverline + threshold &&
                day3 >= coverline + threshold
            ) {
                return TempShiftResult(
                    shiftDetected = true,
                    shiftDate = LocalDate.parse(smoothed[i].first),
                    coverline = coverline,
                    dataQuality = quality,
                )
            }
        }

        return TempShiftResult(
            shiftDetected = false,
            shiftDate = null,
            coverline = smoothed.map { it.second }.average().toFloat(),
            dataQuality = quality,
        )
    }

    // ── Symptom analysis ─────────────────────────────────────────────────────

    data class SymptomAnalysis(
        val eggWhiteMucusNearOvulation: Boolean,
        val highSexDriveNearOvulation: Boolean,
        val breastTendernessInLuteal: Boolean,
        val fatigueInLuteal: Boolean,
    )

    private fun analyzeSymptoms(
        logs: List<DailyCycleLogEntity>,
        cycleStart: LocalDate,
        ovulationDate: LocalDate,
    ): SymptomAnalysis {
        val fertileWindow = ovulationDate.minusDays(5)..ovulationDate.plusDays(1)
        val lutealWindow = ovulationDate.plusDays(2)..cycleStart.plusDays(40) // generous range

        var eggWhiteNearOvulation = false
        var highSexDriveNearOvulation = false
        var breastTendernessInLuteal = false
        var fatigueInLuteal = false

        logs.forEach { log ->
            val date = LocalDate.parse(log.date)

            if (date in fertileWindow) {
                if (log.cervicalMucus == CervicalMucusType.EGG_WHITE.name ||
                    log.cervicalMucus == CervicalMucusType.WATERY.name
                ) {
                    eggWhiteNearOvulation = true
                }
                if (log.sexDrive == "HIGH") {
                    highSexDriveNearOvulation = true
                }
            }

            if (date in lutealWindow) {
                if ("BREAST_TENDERNESS" in log.symptoms) {
                    breastTendernessInLuteal = true
                }
                if ("FATIGUE" in log.symptoms) {
                    fatigueInLuteal = true
                }
            }
        }

        return SymptomAnalysis(
            eggWhiteMucusNearOvulation = eggWhiteNearOvulation,
            highSexDriveNearOvulation = highSexDriveNearOvulation,
            breastTendernessInLuteal = breastTendernessInLuteal,
            fatigueInLuteal = fatigueInLuteal,
        )
    }

    // ── Conception probability (TTC) ─────────────────────────────────────────

    private fun calculateConceptionProbability(
        today: LocalDate,
        ovulationDate: LocalDate,
        fertileStart: LocalDate,
        fertileEnd: LocalDate,
        tempShiftConfirmed: Boolean,
        eggWhiteMucus: Boolean,
    ): Float {
        if (today !in fertileStart..fertileEnd) return 0f

        val daysFromOvulation = ChronoUnit.DAYS.between(today, ovulationDate).toInt()

        // Base probability by day relative to ovulation (clinical data)
        val baseProb = when (daysFromOvulation) {
            -5 -> 0.04f
            -4 -> 0.10f
            -3 -> 0.16f
            -2 -> 0.27f
            -1 -> 0.31f
             0 -> 0.33f // ovulation day
             1 -> 0.08f
            else -> 0f
        }

        // Boost if confirmed by temperature or mucus signals
        var multiplier = 1.0f
        if (tempShiftConfirmed) multiplier += 0.1f
        if (eggWhiteMucus) multiplier += 0.1f

        return (baseProb * multiplier).coerceIn(0f, 0.45f)
    }

    // ── Phase detection ──────────────────────────────────────────────────────

    fun getPhaseForDate(
        date: LocalDate,
        cycleStart: LocalDate,
        periodLength: Int = TYPICAL_PERIOD_LENGTH,
        prediction: CyclePrediction,
    ): CyclePhase {
        val dayInCycle = ChronoUnit.DAYS.between(cycleStart, date).toInt() + 1

        return when {
            dayInCycle <= periodLength -> CyclePhase.MENSTRUAL
            date in prediction.fertileWindowStart..prediction.fertileWindowEnd -> CyclePhase.OVULATORY
            date < prediction.fertileWindowStart -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }
    }

    fun getCycleDay(cycleStart: LocalDate, date: LocalDate = LocalDate.now()): Int =
        ChronoUnit.DAYS.between(cycleStart, date).toInt() + 1

    fun daysUntilNextPeriod(prediction: CyclePrediction): Int =
        ChronoUnit.DAYS.between(LocalDate.now(), prediction.nextPeriodDate).toInt()
            .coerceAtLeast(0)

    // ── Cycle History Analysis ────────────────────────────────────────────────

    data class CycleStats(
        val averageLength: Int,
        val shortestCycle: Int,
        val longestCycle: Int,
        val averagePeriodLength: Int,
        val cycleRegularity: String, // "Regular", "Somewhat Irregular", "Irregular"
        val anomalies: List<String>,
    )

    fun analyzeCycleHistory(cycles: List<CycleRecordEntity>): CycleStats? {
        val completed = cycles.filter { it.cycleLength != null }
        if (completed.isEmpty()) return null

        val lengths = completed.mapNotNull { it.cycleLength }
        val periodLengths = completed.mapNotNull { it.periodLength }
        val avg = lengths.average().toInt()
        val shortest = lengths.min()
        val longest = lengths.max()
        val avgPeriod = if (periodLengths.isNotEmpty()) periodLengths.average().toInt() else 5
        val stdDev = if (lengths.size >= 2) {
            kotlin.math.sqrt(lengths.map { (it - avg).toDouble() * (it - avg) }.average())
        } else 0.0

        val regularity = when {
            stdDev <= 2.0 -> "Regular"
            stdDev <= 4.0 -> "Somewhat Irregular"
            else -> "Irregular"
        }

        val anomalies = mutableListOf<String>()
        if (shortest < 21) anomalies.add("⚠️ Short cycle detected (${shortest}d) — under 21 days")
        if (longest > 35) anomalies.add("⚠️ Long cycle detected (${longest}d) — over 35 days")
        val periodOutliers = periodLengths.filter { it > 7 }
        if (periodOutliers.isNotEmpty()) anomalies.add("⚠️ Heavy/long periods (${periodOutliers.max()}d)")

        return CycleStats(
            averageLength = avg,
            shortestCycle = shortest,
            longestCycle = longest,
            averagePeriodLength = avgPeriod,
            cycleRegularity = regularity,
            anomalies = anomalies,
        )
    }

    private fun upgradeConfidence(current: PredictionConfidence): PredictionConfidence =
        when (current) {
            PredictionConfidence.LOW -> PredictionConfidence.MEDIUM
            PredictionConfidence.MEDIUM -> PredictionConfidence.HIGH
            PredictionConfidence.HIGH -> PredictionConfidence.VERY_HIGH
            PredictionConfidence.VERY_HIGH -> PredictionConfidence.VERY_HIGH
        }
}
