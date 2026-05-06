package com.hopehealth.app.ui.family

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hopehealth.app.data.CyclePhase
import com.hopehealth.app.data.FamilyRepository
import com.hopehealth.app.data.FlowIntensity
import com.hopehealth.app.data.GoalsRepository
import com.hopehealth.app.data.PredictionConfidence
import com.hopehealth.app.data.db.CycleRecordEntity
import com.hopehealth.app.data.db.DailyCycleLogEntity
import com.hopehealth.app.data.db.TemperatureReadingEntity
import com.hopehealth.app.services.CyclePredictionEngine
import com.hopehealth.app.services.GeminiNanoService
import com.hopehealth.app.services.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** A projected future cycle window based on average cycle length. */
data class ProjectedCycle(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val fertileStart: LocalDate,
    val fertileEnd: LocalDate,
    val ovulationDate: LocalDate,
)

data class FamilyUiState(
    val currentCycleDay: Int = 0,
    val currentPhase: CyclePhase = CyclePhase.FOLLICULAR,
    val daysUntilPeriod: Int = 0,
    val fertilityLevel: String = "Unknown",
    val fertilityScore: Int = 0,
    val fertilityAccuracy: String = "Low",
    val prediction: CyclePredictionEngine.CyclePrediction? = null,
    val confidence: PredictionConfidence = PredictionConfidence.LOW,
    val viewingMonth: YearMonth = YearMonth.now(),
    val monthLogs: List<DailyCycleLogEntity> = emptyList(),
    val todayLog: DailyCycleLogEntity? = null,
    val currentCycle: CycleRecordEntity? = null,
    val allCycles: List<CycleRecordEntity> = emptyList(),
    val isLoading: Boolean = true,
    // Phase 2 & 3 additions
    val temperatureReadings: List<TemperatureReadingEntity> = emptyList(),
    val aiInsight: String? = null,
    val isLoadingInsight: Boolean = false,
    val cycleStats: CyclePredictionEngine.CycleStats? = null,
    val conceptionProbability: Float = 0f,
    val isTtcMode: Boolean = false,
    val symptomSignals: List<String> = emptyList(),
    val futurePredictions: List<ProjectedCycle> = emptyList(),
) {
    companion object {
        /**
         * Compute a fertility score (0–10) for a given date based on
         * proximity to the nearest ovulation date across projected cycles.
         * Score peaks at 10 on ovulation day and drops off within the fertile window.
         */
        fun fertilityScoreForDate(
            date: LocalDate,
            projectedCycles: List<ProjectedCycle>,
        ): Int {
            if (projectedCycles.isEmpty()) return 0
            var bestScore = 0
            for (p in projectedCycles) {
                val daysFromOv = kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(date, p.ovulationDate)).toInt()
                val score = when {
                    daysFromOv == 0 -> 10                   // Ovulation day
                    daysFromOv == 1 -> 9                    // Day before/after ovulation
                    date in p.fertileStart..p.fertileEnd -> {
                        // Within fertile window: 5–8 depending on proximity
                        val windowDays = java.time.temporal.ChronoUnit.DAYS.between(p.fertileStart, p.fertileEnd).toInt()
                        val daysIn = java.time.temporal.ChronoUnit.DAYS.between(p.fertileStart, date).toInt()
                        val midpoint = windowDays / 2
                        val distFromMid = kotlin.math.abs(daysIn - midpoint)
                        (8 - distFromMid).coerceIn(5, 8)
                    }
                    date in p.periodStart..p.periodEnd -> 1 // Menstrual
                    else -> 0
                }
                if (score > bestScore) bestScore = score
            }
            return bestScore
        }

        /** Accuracy label based on prediction confidence. */
        fun accuracyLabel(confidence: PredictionConfidence): String = when (confidence) {
            PredictionConfidence.VERY_HIGH -> "Very high accuracy"
            PredictionConfidence.HIGH -> "High accuracy"
            PredictionConfidence.MEDIUM -> "Moderate accuracy"
            PredictionConfidence.LOW -> "Low accuracy"
        }
    }
}

@HiltViewModel
class FamilyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val familyRepository: FamilyRepository,
    private val predictionEngine: CyclePredictionEngine,
    private val goalsRepository: GoalsRepository,
    private val geminiNanoService: GeminiNanoService,
) : ViewModel() {

    private val healthConnectService = HealthConnectService()
    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val mode = goalsRepository.getFamilyMode()
            _uiState.value = _uiState.value.copy(isTtcMode = mode == "TTC")
        }
        loadData()
        syncHealthConnectTemperatures()
    }

    fun loadData() {
        viewModelScope.launch {
            val allCycles = familyRepository.getAllCycles()
            val currentCycle = familyRepository.getCurrentCycle()
            val today = LocalDate.now()
            val viewingMonth = _uiState.value.viewingMonth

            // Load month logs
            val monthStart = viewingMonth.atDay(1)
            val monthEnd = viewingMonth.atEndOfMonth()
            val monthLogs = familyRepository.getLogsForDateRange(monthStart, monthEnd)
            val todayLog = familyRepository.getLogForDate(today)

            // Cycle stats
            val cycleStats = predictionEngine.analyzeCycleHistory(allCycles)

            if (currentCycle != null) {
                val cycleStart = LocalDate.parse(currentCycle.startDate)
                val completedCycles = allCycles.filter { it.cycleLength != null }

                // Load temperature readings for the current cycle
                val tempReadings = familyRepository.getTemperaturesForRange(
                    cycleStart, today
                )

                // Load daily logs for the cycle (for symptom analysis)
                val cycleLogs = familyRepository.getLogsForDateRange(cycleStart, today)

                // Full 3-layer prediction
                val prediction = predictionEngine.predict(
                    completedCycles, cycleStart,
                    temperatureReadings = tempReadings,
                    dailyLogs = cycleLogs,
                )
                val cycleDay = predictionEngine.getCycleDay(cycleStart)
                val phase = predictionEngine.getPhaseForDate(
                    today, cycleStart,
                    periodLength = currentCycle.periodLength ?: 5,
                    prediction = prediction,
                )
                val daysUntil = predictionEngine.daysUntilNextPeriod(prediction)

                val fertility = when (phase) {
                    CyclePhase.OVULATORY -> "High"
                    CyclePhase.FOLLICULAR -> {
                        if (today >= prediction.fertileWindowStart.minusDays(2)) "Medium" else "Low"
                    }
                    else -> "Low"
                }

                // Project future cycles (12 months forward)
                val avgLen = prediction.averageCycleLength
                val periodLen = currentCycle.periodLength ?: 5
                val projected = mutableListOf<ProjectedCycle>()
                // Include current cycle as first projection
                projected.add(ProjectedCycle(
                    periodStart = cycleStart,
                    periodEnd = cycleStart.plusDays(periodLen.toLong() - 1),
                    fertileStart = prediction.fertileWindowStart,
                    fertileEnd = prediction.fertileWindowEnd,
                    ovulationDate = prediction.ovulationDate,
                ))
                // Project 12 future cycles
                var nextStart = prediction.nextPeriodDate
                for (i in 0 until 12) {
                    val ovDay = nextStart.plusDays((avgLen.toLong() - 14))
                    val fertStart = ovDay.minusDays(5)
                    val fertEnd = ovDay.plusDays(1)
                    projected.add(ProjectedCycle(
                        periodStart = nextStart,
                        periodEnd = nextStart.plusDays(periodLen.toLong() - 1),
                        fertileStart = fertStart,
                        fertileEnd = fertEnd,
                        ovulationDate = ovDay,
                    ))
                    nextStart = nextStart.plusDays(avgLen.toLong())
                }

                _uiState.value = _uiState.value.copy(
                    currentCycleDay = cycleDay,
                    currentPhase = phase,
                    daysUntilPeriod = daysUntil,
                    fertilityLevel = fertility,
                    fertilityScore = FamilyUiState.fertilityScoreForDate(today, projected),
                    fertilityAccuracy = FamilyUiState.accuracyLabel(prediction.confidence),
                    prediction = prediction,
                    confidence = prediction.confidence,
                    viewingMonth = viewingMonth,
                    monthLogs = monthLogs,
                    todayLog = todayLog,
                    currentCycle = currentCycle,
                    allCycles = allCycles,
                    isLoading = false,
                    temperatureReadings = tempReadings,
                    cycleStats = cycleStats,
                    conceptionProbability = prediction.conceptionProbability,
                    symptomSignals = prediction.symptomSignals,
                    futurePredictions = projected,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    currentCycleDay = 0,
                    currentPhase = CyclePhase.FOLLICULAR,
                    daysUntilPeriod = 0,
                    fertilityLevel = "Unknown",
                    prediction = null,
                    confidence = PredictionConfidence.LOW,
                    monthLogs = monthLogs,
                    todayLog = todayLog,
                    currentCycle = null,
                    allCycles = allCycles,
                    isLoading = false,
                    temperatureReadings = emptyList(),
                    cycleStats = cycleStats,
                    conceptionProbability = 0f,
                    symptomSignals = emptyList(),
                    futurePredictions = emptyList(),
                )
            }
        }
    }

    /** Pull temperature data from Health Connect and persist to our Room DB. */
    private fun syncHealthConnectTemperatures() {
        viewModelScope.launch {
            try {
                healthConnectService.initialize(context)
                val currentCycle = familyRepository.getCurrentCycle() ?: return@launch
                val cycleStart = LocalDate.parse(currentCycle.startDate)
                val today = LocalDate.now()

                val hcTemps = healthConnectService.readTemperatureHistory(cycleStart, today)
                hcTemps.forEach { (date, tempF, source) ->
                    familyRepository.saveTemperature(
                        TemperatureReadingEntity(
                            date = date,
                            temperatureF = tempF,
                            source = source,
                            timestamp = System.currentTimeMillis(),
                        )
                    )
                }
                if (hcTemps.isNotEmpty()) loadData() // refresh with new temp data
            } catch (_: Exception) {
                // Silently fail — HC may not be available
            }
        }
    }

    fun navigateMonth(delta: Int) {
        _uiState.value = _uiState.value.copy(
            viewingMonth = _uiState.value.viewingMonth.plusMonths(delta.toLong())
        )
        loadData()
    }

    /** Load the existing log for a specific date, then invoke a callback on the main thread. */
    fun loadLogForDate(date: LocalDate, onLoaded: (com.hopehealth.app.data.db.DailyCycleLogEntity?) -> Unit) {
        viewModelScope.launch {
            val log = familyRepository.getLogForDate(date)
            onLoaded(log)
        }
    }

    fun logPeriodStart(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            familyRepository.startNewCycle(date)
            saveDailyLog(
                date = date,
                flowIntensity = FlowIntensity.MEDIUM.name,
            )
            loadData()
        }
    }

    fun logPeriodEnd(date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            familyRepository.endPeriod(date)
            loadData()
        }
    }

    /** Delete a daily log for a specific date (undo a wrong entry). */
    fun deleteLogForDate(date: LocalDate) {
        viewModelScope.launch {
            familyRepository.deleteLogForDate(date)
            // If this date is the start of any cycle, delete that cycle too
            val dateStr = date.toString()
            val allCycles = familyRepository.getAllCycles()
            allCycles.filter { it.startDate == dateStr }.forEach { cycle ->
                familyRepository.deleteCycleById(cycle.id)
            }
            loadData()
        }
    }

    fun saveDailyLog(
        date: LocalDate = LocalDate.now(),
        flowIntensity: String? = null,
        cervicalMucus: String? = null,
        symptoms: String = "",
        mood: String? = null,
        sexDrive: String? = null,
        sexualActivity: String? = null,
        temperature: Float? = null,
        temperatureSource: String? = null,
    ) {
        viewModelScope.launch {
            val currentCycle = familyRepository.getCurrentCycle()
            val cycleDay = if (currentCycle != null) {
                predictionEngine.getCycleDay(LocalDate.parse(currentCycle.startDate), date)
            } else null

            val phase = if (currentCycle != null) {
                val cycleStart = LocalDate.parse(currentCycle.startDate)
                val completedCycles = familyRepository.getAllCycles().filter { it.cycleLength != null }
                val prediction = predictionEngine.predict(completedCycles, cycleStart)
                predictionEngine.getPhaseForDate(
                    date, cycleStart,
                    periodLength = currentCycle.periodLength ?: 5,
                    prediction = prediction,
                ).name
            } else null

            familyRepository.saveLog(
                DailyCycleLogEntity(
                    date = date.toString(),
                    cycleRecordId = currentCycle?.id,
                    cycleDay = cycleDay,
                    phase = phase,
                    flowIntensity = flowIntensity,
                    cervicalMucus = cervicalMucus,
                    symptoms = symptoms,
                    mood = mood,
                    sexDrive = sexDrive,
                    sexualActivity = sexualActivity,
                    temperature = temperature,
                    temperatureSource = temperatureSource,
                )
            )

            // Also save as a TemperatureReadingEntity if temp was provided
            if (temperature != null) {
                familyRepository.saveTemperature(
                    TemperatureReadingEntity(
                        date = date.toString(),
                        temperatureF = temperature,
                        source = temperatureSource ?: "MANUAL",
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }

            loadData()
        }
    }

    fun toggleTtcMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isTtcMode = enabled)
        viewModelScope.launch {
            goalsRepository.setFamilyMode(if (enabled) "TTC" else "TRACKING")
            loadData() // Refresh predictions with TTC context
        }
    }

    /** Generate AI-powered cycle insight using Gemini Nano (with fallback). */
    fun generateAiInsight() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInsight = true)

            val state = _uiState.value
            val prompt = buildCycleInsightPrompt(state)

            val aiResponse = try {
                if (geminiNanoService.initIfNeeded()) {
                    val raw = geminiNanoService.generateContent(prompt)
                    raw.ifBlank { null }
                } else null
            } catch (_: Exception) {
                null
            }

            _uiState.value = _uiState.value.copy(
                aiInsight = aiResponse ?: getFallbackInsight(state),
                isLoadingInsight = false,
            )
        }
    }

    private fun buildCycleInsightPrompt(state: FamilyUiState): String {
        val mode = if (state.isTtcMode) "trying to conceive" else "tracking menstrual health"
        val stats = state.cycleStats
        return """
            <instruction>
            You are a compassionate fertility health assistant. Give a brief, personalized daily insight
            (2-3 sentences max) based on the user's cycle data. Be warm, supportive, and evidence-based.
            Bold the 1-3 most important short phrases by wrapping them in **double asterisks**.
            Use only one space after periods. Do NOT use emojis. Do NOT include medical disclaimers.
            User is ${mode}.
            </instruction>

            <cycle_data>
            Cycle day: ${state.currentCycleDay}
            Phase: ${state.currentPhase.name}
            Days until period: ${state.daysUntilPeriod}
            Fertility level: ${state.fertilityLevel}
            Confidence: ${state.confidence.name}
            Average cycle length: ${stats?.averageLength ?: 28} days
            Cycle regularity: ${stats?.cycleRegularity ?: "Unknown"}
            Temperature shift detected: ${state.prediction?.temperatureShiftDetected ?: false}
            Symptom signals: ${state.symptomSignals.joinToString("; ").ifBlank { "None" }}
            ${if (state.isTtcMode) "Conception probability today: ${(state.conceptionProbability * 100).toInt()}%" else ""}
            </cycle_data>
        """.trimIndent()
    }

    private fun getFallbackInsight(state: FamilyUiState): String {
        return when {
            state.isTtcMode && state.currentPhase == CyclePhase.OVULATORY ->
                "You're in your fertile window! Today's estimated conception chance is ${(state.conceptionProbability * 100).toInt()}%. Timing intercourse every 1-2 days during this window maximizes your chances."
            state.isTtcMode && state.currentPhase == CyclePhase.LUTEAL ->
                "You're in the two-week wait. Try to stay relaxed — stress can affect implantation. Your next period is expected in ~${state.daysUntilPeriod} days."
            state.prediction?.temperatureShiftDetected == true ->
                "Your temperature shift has been confirmed! This suggests ovulation occurred around ${state.prediction?.confirmedOvulationDate}. Your prediction confidence has been upgraded."
            state.symptomSignals.isNotEmpty() ->
                "Your logged symptoms are providing additional signals: ${state.symptomSignals.first()}"
            else -> when (state.currentPhase) {
                CyclePhase.MENSTRUAL -> "You're on day ${state.currentCycleDay} of your cycle. Rest well, and remember that iron-rich foods can help replenish what's lost during your period."
                CyclePhase.FOLLICULAR -> "Rising estrogen is giving you a natural energy boost! This is a great time for higher-intensity workouts and creative projects."
                CyclePhase.OVULATORY -> "You're near peak fertility. Energy and confidence tend to be highest now. Your next period is expected in ~${state.daysUntilPeriod} days."
                CyclePhase.LUTEAL -> "Progesterone is dominant now. Cravings, bloating, and mood shifts are completely normal. Prioritize sleep and gentle movement."
            }
        }
    }
}
