package com.fittrack.app.ui.home

import android.util.Log
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.StepsRepository
import com.fittrack.app.services.GeminiNanoService
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoachTipUseCase(
        private val foodRepository: FoodRepository,
        private val stepsRepository: StepsRepository,
        private val geminiNanoService: GeminiNanoService,
        private val random: Random = Random.Default
) {
    private fun selectTone(data: CoachPromptData): String {
        val calRatio = data.caloriesEaten.toFloat() / data.calorieGoal.coerceAtLeast(1)
        val stepRatio = data.steps.toFloat() / data.stepGoal.coerceAtLeast(1)
        val noData = data.caloriesEaten == 0 && data.steps == 0

        return when {
            noData -> "a friendly, encouraging teammate"
            calRatio > 1.15f -> "a supportive, non-judgmental nutritionist"
            stepRatio >= 1f && calRatio in 0.8f..1.05f -> "an excited cheerleader celebrating their success"
            data.now.hour >= 18 && calRatio < 0.5f -> "a caring nutritionist concerned about under-eating"
            stepRatio < 0.3f && data.now.hour >= 15 -> "a motivating sports coach"
            else -> "a knowledgeable, encouraging fitness coach"
        }
    }

    suspend fun getCoachTip(data: CoachPromptData): String =
            withContext(Dispatchers.IO) {
                val name = data.nickname.ifBlank { "friend" }
                val promptData = data.copy(nickname = name)

                val includeWeeklyData =
                        promptData.now.hour >= 18 ||
                                promptData.caloriesEaten > promptData.calorieGoal ||
                                (promptData.stepGoal > 0 &&
                                        promptData.now.hour >= 15 &&
                                        promptData.steps < promptData.stepGoal * 0.5f)

                val weeklySummary: String? =
                        if (includeWeeklyData) {
                            val calHistory =
                                    foodRepository.getCaloriesHistory(7).map { it.second }.filter {
                                        it > 0
                                    }
                            val stepsHistory =
                                    stepsRepository.getStepsHistory(7).map { it.second }.filter {
                                        it > 0
                                    }

                            val avgCal =
                                    if (calHistory.isNotEmpty()) calHistory.average().toInt()
                                    else promptData.caloriesEaten
                            val avgSteps =
                                    if (stepsHistory.isNotEmpty()) stepsHistory.average().toInt()
                                    else promptData.steps

                            "\n7-day averages: ~${avgCal} cal/day, ~${avgSteps} steps/day\n"
                        } else null

                val prompt = buildCoachPrompt(promptData, weeklySummary)

                try {
                    if (geminiNanoService.initIfNeeded()) {
                        val response = geminiNanoService.generateContent(prompt)
                        val cleaned =
                                response.trim('"')
                                        .replace(Regex("""\*{3,}"""), "**")
                                        .replace(Regex(""" {2,}"""), " ")
                                        .trim()

                        if (cleaned.length > 5 && cleaned.isNotBlank()) {
                            if (!cleaned.contains("**")) {
                                Log.w(
                                        "FitTrack_Coach",
                                        "AI response missing bold markers: $cleaned"
                                )
                            }
                            Log.d("FitTrack_Coach", "AI coach tip: $cleaned")
                            return@withContext cleaned
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("FitTrack_Coach", "AI coach failed, using fallback: ${e.message}", e)
                }

                return@withContext fallbackCoachTip(promptData)
            }

    private fun buildCoachPrompt(data: CoachPromptData, weeklySummary: String?): String {
        val timeOfDay =
                when {
                    data.now.hour < 12 -> "morning"
                    data.now.hour < 17 -> "afternoon"
                    else -> "evening"
                }
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val exactTime = data.now.format(formatter)
        val tone = selectTone(data)

        return buildString {
            append("<instruction>\n")
            append("You are $tone. ")
            append("Give a short, personalized health coach tip (2-3 sentences max). ")
            append("Be specific to their actual numbers and progress toward goals. ")
            append("Bold the 1-3 most important short phrases by wrapping them in **double asterisks**. ")
            append("Do NOT use emojis. Do NOT wrap in quotes. Use only one space after periods.\n")
            append("</instruction>\n\n")

            append("<user_data>\n")
            append("Name: ${data.nickname}\n")
            append("Time: $exactTime ($timeOfDay)\n")
            append("Calories: ${data.caloriesEaten} eaten / ${data.calorieGoal} goal\n")
            append("Protein: ${data.protein.toInt()}g / ${data.proteinGoal}g goal\n")
            append("Carbs: ${data.carbs.toInt()}g / ${data.carbsGoal}g goal\n")
            append("Fat: ${data.fat.toInt()}g / ${data.fatGoal}g goal\n")
            append("Sugar: ${data.sugar.toInt()}g / ${data.sugarGoal}g limit\n")
            append("Steps: ${data.steps} / ${data.stepGoal} goal\n")
            weeklySummary?.let { append(it) }

            if (data.caloriesEaten <= 0) {
                append("NOTE: No food logged yet today.\n")
            }
            if (data.steps == 0) {
                append("NOTE: No steps recorded yet today.\n")
            }
            append("</user_data>\n\n")

            append("<guidelines>\n")
            if (data.caloriesEaten <= 0) {
                append("- Gently suggest they log their food.\n")
            }
            if (data.caloriesEaten > data.calorieGoal) {
                append("- Be supportive, not judgmental about being over the calorie goal.\n")
            }
            val proteinRatio = if (data.proteinGoal > 0) data.protein / data.proteinGoal else 1f
            if (proteinRatio < 0.3f && data.caloriesEaten > 0) {
                append("- Protein intake is low relative to goal. Suggest protein-rich foods.\n")
            }
            if (data.sugar > data.sugarGoal && data.sugarGoal > 0) {
                append("- Sugar intake exceeds their daily limit. Mention this gently.\n")
            }
            append("</guidelines>")
        }
    }

    private fun fallbackCoachTip(data: CoachPromptData): String {
        val noFood =
                data.caloriesEaten == 0 && data.protein == 0f && data.carbs == 0f && data.fat == 0f
        val noSteps = data.steps == 0
        val name = data.nickname
        val hour = data.now.hour
        val cal = data.caloriesEaten
        val calGoal = data.calorieGoal
        val s = data.steps
        val sGoal = data.stepGoal
        val p = data.protein
        val c = data.carbs
        val f = data.fat

        return when {
            noFood && noSteps -> {
                if (hour < 12)
                        listOf(
                                        "Good morning, $name! A fresh day ahead. Log your breakfast and get moving — you've got this!",
                                        "Rise and shine, $name! Let's make today count. Start by logging breakfast to stay on track.",
                                        "Morning $name! Your day is a blank canvas. Grab a healthy breakfast and log those first steps!"
                                )
                                .random(random)
                else
                        listOf(
                                        "Hey $name, looks like today's a blank slate. No food or steps logged yet — consider adding them so I can help you stay on track!",
                                        "Checking in, $name! Have you eaten today? We'd love to see what you've had — log your food and steps when you can.",
                                        "Hi $name! I haven't seen any activity today. Getting started is the hardest part, let's track a meal or some steps!"
                                )
                                .random(random)
            }
            noFood ->
                    listOf(
                                    "You've got $s steps in already — nice work, $name! No food logged yet though. Consider adding your meals so we can track your nutrition together.",
                                    "Great job moving today, $name! But don't forget to fuel up — try logging your food so we can see your macros.",
                                    "$s steps down, $name! Now, how about logging what's on your plate today?"
                            )
                            .random(random)
            noSteps ->
                    listOf(
                                    "Food's logged but no steps yet, $name. Even a short walk counts! Try to get moving when you can — your body will thank you.",
                                    "Got your nutrition down, $name! Take a quick 10-minute walk when you can to get those steps flowing.",
                                    "Nutrition is looking tracked, $name. Stand up and stretch, then maybe pace a bit to grab some steps!"
                            )
                            .random(random)
            cal > calGoal * 1.15 -> {
                val over = cal - calGoal
                listOf(
                                "You're about $over cal over your goal, $name. No stress — it happens! Maybe go lighter on your next meal or take a walk to balance things out.",
                                "Hey $name, you're slightly above your calorie target by $over cal. Tomorrow is a new day, don't sweat it!",
                                "$over cal above the mark, $name. Keep your head up — drink some extra water and stay active!"
                        )
                        .random(random)
            }
            cal > calGoal * 0.9 && cal <= calGoal ->
                    listOf(
                                    "Almost at your calorie goal, $name! You're right on track. Be mindful with snacks from here and you'll finish the day perfectly.",
                                    "You're in the sweet spot for calories, $name! Close to the goal and doing great.",
                                    "Pacing beautifully today, $name. You're right under your calorie limit!"
                            )
                            .random(random)
            hour >= 18 && cal < calGoal * 0.5 ->
                    listOf(
                                    "It's getting late and you're under half your calorie goal, $name. Make sure you're eating enough — your body needs fuel to recover!",
                                    "Hey $name, you've barely eaten and the day is winding down. Grab a nutritious dinner!",
                                    "Don't under-fuel yourself, $name. You're below 50% of your calories. Time for a good meal!"
                            )
                            .random(random)
            s >= sGoal ->
                    listOf(
                                    "Step goal crushed! Amazing work, $name. Keep that energy going — consistency is what makes the difference.",
                                    "Goal achieved! You nailed your step targets today, $name.",
                                    "Boom! Step goal hit, $name. Give yourself a well-deserved high five."
                            )
                            .random(random)
            hour >= 15 && s < sGoal * 0.3 ->
                    listOf(
                                    "Afternoon check-in, $name: you're under 30% of your step goal. An evening walk could get you back on pace — you've still got time!",
                                    "Hey $name, still quite a few steps to go today! See if you can sneak in a walk before dinner.",
                                    "Step count needs a little love today, $name. Try pacing or strolling around the block."
                            )
                            .random(random)
            f > 0 && p > 0 && f * 9 > (p * 4 + c * 4 + f * 9) * 0.45 ->
                    listOf(
                                    "Your fat intake is high today relative to protein and carbs, $name. Try adding some lean protein or veggies to your next meal to balance things out.",
                                    "Watch the fats, $name — they are taking over your macros today. Try rounding it out with more protein!",
                                    "Hey $name, you've logged a good amount of fat. Balance it with lean protein on your next meal if you can."
                            )
                            .random(random)
            p > 0 && c > 0 && p * 4 < (p * 4 + c * 4 + f * 9) * 0.15 ->
                    listOf(
                                    "Protein's low today, $name. Consider adding something like chicken, eggs, or Greek yogurt — your muscles will appreciate it!",
                                    "Hey $name, your protein macros are trailing! A protein shake or some eggs might hit the spot.",
                                    "Don't forget protein, $name! Essential for recovery. Consider an extra serving of lean meat or beans."
                            )
                            .random(random)
            s.toFloat() / sGoal.coerceAtLeast(1) > 0.7f &&
                    cal.toFloat() / calGoal.coerceAtLeast(1) < 0.8f ->
                    listOf(
                                    "Great step count so far, $name! Your calories are well managed too. You're having an awesome day — keep it up!",
                                    "You're moving well and eating right, $name. This is the recipe for success.",
                                    "Awesome job balancing activity and nutrition today, $name!"
                            )
                            .random(random)
            else ->
                    listOf(
                                    "Looking good, $name! You're making progress on your goals today. Keep logging and moving — every bit counts.",
                                    "Keep it up, $name. Steady habits lead to big results!",
                                    "You are doing wonderfully, $name. Have a fantastic rest of your day."
                            )
                            .random(random)
        }
    }
}
