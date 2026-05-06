package com.fittrack.app.ui.family

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.CyclePhase
import com.fittrack.app.data.FamilyRepository
import com.fittrack.app.data.FlowIntensity
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.PredictionConfidence
import com.fittrack.app.data.db.CycleRecordEntity
import com.fittrack.app.data.db.DailyCycleLogEntity
import com.fittrack.app.data.db.TemperatureReadingEntity
import com.fittrack.app.services.CyclePredictionEngine
import com.fittrack.app.services.GeminiNanoService
import com.fittrack.app.services.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class FamilyUiState(
    val currentCycleDay: Int = 0,
    val currentPhase: CyclePhase = CyclePhase.FOLLICULAR,
    val daysUntilPeriod: Int = 0,
    val fertilityLevel: String = "Unknown",
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
)

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

                _uiState.value = _uiState.value.copy(
                    currentCycleDay = cycleDay,
                    currentPhase = phase,
                    daysUntilPeriod = daysUntil,
                    fertilityLevel = fertility,
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
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    monthLogs = monthLogs,
                    todayLog = todayLog,
                    allCycles = allCycles,
                    cycleStats = cycleStats,
                    isLoading = false,
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
            Do NOT include medical disclaimers. User is ${mode}.
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
