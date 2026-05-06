package com.hopehealth.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hopehealth.app.AppRoute
import com.hopehealth.app.data.MealType
import com.hopehealth.app.theme.AppColors
import com.hopehealth.app.theme.interFamily
import com.hopehealth.app.ui.common.ScreenScaffold
import com.hopehealth.app.util.fmtNum
import com.hopehealth.app.util.getGreeting
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val color: Color,
        val size: Float
)

@Composable
private fun ConfettiBurst(trigger: Boolean, modifier: Modifier = Modifier) {
        if (!trigger) return

        val colors =
                listOf(
                        AppColors.primary,
                        AppColors.success,
                        AppColors.warning,
                        AppColors.calorie,
                        AppColors.protein,
                        AppColors.fat
                )
        val particles = remember {
                List(40) {
                        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
                        val speed = 200f + Random.nextFloat() * 400f
                        Particle(
                                x = 0.5f,
                                y = 0.5f,
                                vx = cos(angle) * speed,
                                vy = sin(angle) * speed - 300f,
                                color = colors[Random.nextInt(colors.size)],
                                size = 4f + Random.nextFloat() * 6f
                        )
                }
        }

        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
                progress.animateTo(1f, animationSpec = tween(1500, easing = LinearEasing))
        }

        Canvas(modifier = modifier.fillMaxSize()) {
                val t = progress.value
                val alpha = (1f - t).coerceIn(0f, 1f)
                if (alpha <= 0f) return@Canvas

                particles.forEach { p ->
                        val gravity = 600f
                        val px = size.width * p.x + p.vx * t
                        val py = size.height * p.y + p.vy * t + 0.5f * gravity * t * t
                        drawCircle(
                                color = p.color.copy(alpha = alpha * 0.9f),
                                radius = p.size * (1f - t * 0.3f),
                                center = Offset(px, py)
                        )
                }
        }
}

@Composable
private fun OnboardingDialog(
    onSave: (name: String, calorieGoal: Int, stepGoal: Int, weightLbs: Float, heightFt: Int, heightIn: Int, age: Int) -> Unit,
    onComplete: () -> Unit,
) {
        var step by remember { mutableStateOf(0) }
        var nameText by remember { mutableStateOf("") }
        var calorieText by remember { mutableStateOf("2000") }
        var stepText by remember { mutableStateOf("10000") }
        var weightText by remember { mutableStateOf("") }
        var heightFtText by remember { mutableStateOf("") }
        var heightInText by remember { mutableStateOf("") }
        var ageText by remember { mutableStateOf("") }

        fun capitalizeInput(input: String): String =
                if (input.isNotEmpty()) input.replaceFirstChar { it.uppercase() } else input

        fun saveAndFinish() {
                onSave(
                    capitalizeInput(nameText.trim()),
                    calorieText.toIntOrNull()?.takeIf { it in 500..10000 } ?: 2000,
                    stepText.toIntOrNull()?.takeIf { it in 100..200000 } ?: 10000,
                    weightText.toFloatOrNull() ?: 0f,
                    heightFtText.toIntOrNull() ?: 0,
                    heightInText.toIntOrNull() ?: 0,
                    ageText.toIntOrNull() ?: 0,
                )
                onComplete()
        }

        AlertDialog(
                onDismissRequest = {},
                title = {
                        Text(
                                text = if (step == 0) "Welcome to HopeHealth" else "Your Profile",
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                },
                text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                                if (step == 0) {
                                        Text(
                                                text = "What should we call you?",
                                                fontFamily = interFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text = "We'll use this to personalize your experience.",
                                                fontFamily = interFamily,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedTextField(
                                                value = nameText,
                                                onValueChange = { nameText = it },
                                                label = { Text("Your name", fontFamily = interFamily) },
                                                placeholder = { Text("e.g. Alex", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                                        )
                                } else {
                                        Text(
                                                text = "Fill in what you know — you can always update these in Settings.",
                                                fontFamily = interFamily,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                        value = calorieText,
                                                        onValueChange = { calorieText = it.filter { c -> c.isDigit() } },
                                                        label = { Text("Calorie goal", fontFamily = interFamily) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                                OutlinedTextField(
                                                        value = stepText,
                                                        onValueChange = { stepText = it.filter { c -> c.isDigit() } },
                                                        label = { Text("Step goal", fontFamily = interFamily) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                        value = weightText,
                                                        onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                                                        label = { Text("Weight", fontFamily = interFamily) },
                                                        suffix = { Text("lbs", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        placeholder = { Text("160", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                                )
                                                OutlinedTextField(
                                                        value = ageText,
                                                        onValueChange = { ageText = it.filter { c -> c.isDigit() } },
                                                        label = { Text("Age", fontFamily = interFamily) },
                                                        suffix = { Text("yrs", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Height", fontFamily = interFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                        value = heightFtText,
                                                        onValueChange = { heightFtText = it.filter { c -> c.isDigit() } },
                                                        suffix = { Text("ft", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        placeholder = { Text("5", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                                OutlinedTextField(
                                                        value = heightInText,
                                                        onValueChange = { heightInText = it.filter { c -> c.isDigit() } },
                                                        suffix = { Text("in", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        placeholder = { Text("10", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                        }
                                }
                        }
                },
                confirmButton = {
                        Button(
                                onClick = { if (step == 0) step = 1 else saveAndFinish() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                                Text(text = if (step == 0) "Next" else "Let's Go", fontFamily = interFamily)
                        }
                },
                dismissButton = {
                        if (step == 1) {
                                TextButton(onClick = { saveAndFinish() }) {
                                        Text("Use Defaults", fontFamily = interFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                        }
                }
        )
}

// ── Meal Visualization ────────────────────────────────────────────────────────

private data class MealSlot(
    val type: MealType,
    val label: String,
    val icon: ImageVector,
    val color: Color,
)

private val MEAL_SLOTS = listOf(
    MealSlot(MealType.BREAKFAST, "Breakfast", Icons.Filled.WbSunny,     Color(0xFFFF9800)),
    MealSlot(MealType.LUNCH,     "Lunch",     Icons.Filled.LunchDining,  Color(0xFF4CAF50)),
    MealSlot(MealType.DINNER,    "Dinner",    Icons.Filled.Bedtime,      Color(0xFF7C5CBF)),
    MealSlot(MealType.SNACK,     "Snacks",    Icons.Filled.Coffee,       Color(0xFF2196F3)),
)

@Composable
private fun MealVisualizationCard(
    breakdown: Map<MealType, Int>,
    selectedMeal: MealType,
    calorieGoal: Int,
    onMealSelected: (MealType) -> Unit,
    onMealDetailClick: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalLogged = breakdown.values.sum()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Meals Today",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = AppColors.textPrimary,
                )
                if (totalLogged > 0) {
                    Text(
                        text = "${fmtNum(totalLogged)} cal logged",
                        fontFamily = interFamily,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Meal Pill Tabs ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MEAL_SLOTS.forEach { slot ->
                    val isSelected = slot.type == selectedMeal
                    val slotCals = breakdown[slot.type] ?: 0
                    val hasFood = slotCals > 0

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) slot.color.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) slot.color
                                        else AppColors.border,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onMealSelected(slot.type) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = slot.icon,
                                contentDescription = slot.label,
                                tint = if (isSelected) slot.color else AppColors.textSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = slot.label,
                                fontFamily = interFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 10.sp,
                                color = if (isSelected) slot.color else AppColors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                            // Small dot if food logged for this meal
                            if (hasFood) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(slot.color),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Stacked calorie bar ──────────────────────────────────────────
            if (totalLogged > 0 || calorieGoal > 0) {
                val barTotal = calorieGoal.coerceAtLeast(totalLogged).coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(AppColors.border.copy(alpha = 0.4f)),
                ) {
                    MEAL_SLOTS.forEach { slot ->
                        val rawCals = breakdown[slot.type] ?: 0
                        val frac by animateFloatAsState(
                            targetValue = rawCals.toFloat() / barTotal,
                            animationSpec = tween(700, easing = FastOutSlowInEasing),
                            label = "bar_${slot.type}",
                        )
                        if (frac > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(frac)
                                    .fillMaxHeight()
                                    .background(slot.color),
                            )
                        }
                    }
                    // Remaining unfilled space
                    val filledFrac = MEAL_SLOTS.sumOf {
                        (breakdown[it.type] ?: 0).toFloat().toDouble()
                    }.toFloat() / barTotal
                    val remainFrac = (1f - filledFrac).coerceAtLeast(0f)
                    if (remainFrac > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(remainFrac)
                                .fillMaxHeight()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MEAL_SLOTS.forEach { slot ->
                        if ((breakdown[slot.type] ?: 0) > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(slot.color),
                                )
                                Text(
                                    text = slot.label,
                                    fontFamily = interFamily,
                                    fontSize = 10.sp,
                                    color = AppColors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Selected meal detail ─────────────────────────────────────────
            val currentSlot = MEAL_SLOTS.first { it.type == selectedMeal }
            val currentCals = breakdown[selectedMeal] ?: 0
            val animatedCals by animateIntAsState(
                targetValue = currentCals,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "meal_cals",
            )
            val mealPct = if (calorieGoal > 0) currentCals.toFloat() / calorieGoal else 0f
            val animatedMealPct by animateFloatAsState(
                targetValue = mealPct.coerceIn(0f, 1f),
                animationSpec = tween(700, easing = FastOutSlowInEasing),
                label = "meal_pct",
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentSlot.color.copy(alpha = 0.08f))
                    .clickable { onMealDetailClick(selectedMeal) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = currentSlot.icon,
                            contentDescription = currentSlot.label,
                            tint = currentSlot.color,
                            modifier = Modifier.size(32.dp),
                        )
                        Column {
                            AnimatedContent(
                                targetState = animatedCals,
                                transitionSpec = {
                                    fadeIn(tween(300)) togetherWith
                                        androidx.compose.animation.fadeOut(tween(150))
                                },
                                label = "cals_anim",
                            ) { cals ->
                                Text(
                                    text = "${fmtNum(cals)} cal",
                                    fontFamily = interFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = currentSlot.color,
                                )
                            }
                            Text(
                                text = if (currentCals == 0) "Nothing logged yet · tap to open Diary"
                                       else "${currentSlot.label} · ${(mealPct * 100).toInt()}% of goal · tap for details",
                                fontFamily = interFamily,
                                fontSize = 12.sp,
                                color = AppColors.textSecondary,
                            )
                        }
                    }
                    // Mini ring indicator
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(currentSlot.color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(40.dp)) {
                            val stroke = 5.dp.toPx()
                            drawArc(
                                color = currentSlot.color.copy(alpha = 0.18f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = stroke,
                                ),
                            )
                            drawArc(
                                color = currentSlot.color,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedMealPct,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── HomeScreen ────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
        val context = LocalContext.current
        val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
        val showOnboarding = !onboardingDone

        val calorieGoal by viewModel.calorieGoal.collectAsStateWithLifecycle()
        val stepGoal by viewModel.stepGoal.collectAsStateWithLifecycle()
        val caloriesEaten by viewModel.caloriesEaten.collectAsStateWithLifecycle()
        val protein by viewModel.protein.collectAsStateWithLifecycle()
        val carbs by viewModel.carbs.collectAsStateWithLifecycle()
        val fat by viewModel.fat.collectAsStateWithLifecycle()
        val sugar by viewModel.sugar.collectAsStateWithLifecycle()
        val steps by viewModel.steps.collectAsStateWithLifecycle()
        val caloriesBurned by viewModel.caloriesBurned.collectAsStateWithLifecycle()
        val proteinGoalG by viewModel.proteinGoalG.collectAsStateWithLifecycle()
        val carbsGoalG by viewModel.carbsGoalG.collectAsStateWithLifecycle()
        val fatGoalG by viewModel.fatGoalG.collectAsStateWithLifecycle()
        val sugarGoalG by viewModel.sugarGoalG.collectAsStateWithLifecycle()
        val nickname by viewModel.nickname.collectAsStateWithLifecycle()
        val coachTip by viewModel.coachTip.collectAsStateWithLifecycle()
        val mealBreakdown by viewModel.mealBreakdown.collectAsStateWithLifecycle()
        val selectedMealType by viewModel.selectedMealType.collectAsStateWithLifecycle()
        var showManualBurnDialog by remember { mutableStateOf(false) }
        var burnDialogMode by remember { mutableStateOf("add") }

        if (showOnboarding) {
                OnboardingDialog(
                        onSave = { name, cals, stepGoal, weightLbs, heightFt, heightIn, age ->
                            viewModel.onboardingSave(name, cals, stepGoal, weightLbs, heightFt, heightIn, age)
                        },
                        onComplete = { viewModel.loadData() },
                )
        }

        LaunchedEffect(Unit) { viewModel.loadData() }

        fun navigateTo(route: AppRoute) {
                navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                }
        }

        fun navigateToLog(mealFilter: String? = null) {
                navController.navigate(AppRoute.Log(mealFilter = mealFilter)) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = false  // always re-create so filter is applied fresh
                }
        }

        val dateFormatted =
                LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))

        val stepsProgress = (steps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
        var showConfetti by remember { mutableStateOf(false) }
        var confettiFired by remember { mutableStateOf(false) }
        LaunchedEffect(stepsProgress) {
                if (stepsProgress >= 1f && !confettiFired) {
                        confettiFired = true
                        showConfetti = true
                }
        }

        val cardCount = 6
        val cardVisible = remember { List(cardCount) { mutableStateOf(false) } }
        LaunchedEffect(Unit) {
                for (i in 0 until cardCount) {
                        delay(80L * i)
                        cardVisible[i].value = true
                }
        }

        Box {
                ScreenScaffold {
                        Text(
                                text = getGreeting(nickname),
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = AppColors.textPrimary
                        )
                        Text(
                                text = dateFormatted,
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val caloriesProgress =
                                (caloriesEaten.toFloat() / calorieGoal.coerceAtLeast(1)).coerceIn(
                                        0f,
                                        1f
                                )

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                                ProgressRing(
                                        progress = caloriesProgress,
                                        currentValue = caloriesEaten,
                                        goalValue = calorieGoal,
                                        label = "Calories Consumed",
                                        ringColor = progressColor(caloriesProgress, lowGood = true),
                                onClick = { navigateToLog() }
                                )
                                ProgressRing(
                                        progress = stepsProgress,
                                        currentValue = steps,
                                        goalValue = stepGoal,
                                        label = "Steps",
                                        ringColor = progressColor(stepsProgress, lowGood = false),
                                        onClick = { navigateTo(AppRoute.Steps) },
                                        onLongClick = {
                                                val intents =
                                                        com.hopehealth.app.services
                                                                .HealthConnectService()
                                                                .getSettingsIntents(context)
                                                for (intent in intents) {
                                                        try {
                                                                context.startActivity(intent)
                                                                break
                                                        } catch (_: Exception) {}
                                                }
                                        }
                                )
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // AI Coach card
                        AnimatedVisibility(
                                visible = cardVisible[0].value && coachTip.isNotBlank(),
                                enter =
                                        fadeIn(tween(400)) +
                                                slideInVertically(tween(400)) { it / 3 }
                        ) {
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor = Color(0xFFF0F4FF)
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        shape = RoundedCornerShape(16.dp)
                                ) {
                                        Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.Top
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Outlined.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = AppColors.primary,
                                                        modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                        Text(
                                                                text = "Coach",
                                                                fontFamily = interFamily,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 13.sp,
                                                                color = AppColors.primary
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                                text = boldCoachTip(coachTip),
                                                                fontFamily = interFamily,
                                                                fontSize = 14.sp,
                                                                lineHeight = 20.sp,
                                                                color = AppColors.textPrimary
                                                        )
                                                }
                                        }
                                }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedVisibility(
                                visible = cardVisible[1].value,
                                enter =
                                        fadeIn(tween(300)) +
                                                slideInVertically(tween(300)) { it / 3 }
                        ) {
                                val total = (caloriesEaten + caloriesBurned).coerceAtLeast(1)
                                val targetRatio =
                                        (caloriesEaten.toFloat() / total).coerceIn(0.05f, 0.95f)
                                val animatedRatio by
                                        animateFloatAsState(
                                                targetValue = targetRatio,
                                                animationSpec =
                                                        tween(800, easing = FastOutSlowInEasing),
                                                label = "calorieBar"
                                        )

                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor = AppColors.surface
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                                var burnMenuExpanded by remember { mutableStateOf(false) }
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                        Text(
                                                                text = "Calorie Balance",
                                                                fontFamily = interFamily,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 16.sp,
                                                                color = AppColors.textPrimary
                                                        )
                                                        Box {
                                                                Icon(
                                                                        imageVector = Icons.Default.MoreVert,
                                                                        contentDescription = "Edit burned calories",
                                                                        modifier = Modifier
                                                                                .size(22.dp)
                                                                                .clip(CircleShape)
                                                                                .clickable { burnMenuExpanded = true },
                                                                        tint = AppColors.textSecondary,
                                                                )
                                                                DropdownMenu(
                                                                        expanded = burnMenuExpanded,
                                                                        onDismissRequest = { burnMenuExpanded = false },
                                                                ) {
                                                                        DropdownMenuItem(
                                                                                text = { Text("Add burned calories", fontFamily = interFamily, fontSize = 14.sp) },
                                                                                onClick = {
                                                                                        burnMenuExpanded = false
                                                                                        burnDialogMode = "add"
                                                                                        showManualBurnDialog = true
                                                                                },
                                                                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                                                        )
                                                                        DropdownMenuItem(
                                                                                text = { Text("Set burned calories", fontFamily = interFamily, fontSize = 14.sp) },
                                                                                onClick = {
                                                                                        burnMenuExpanded = false
                                                                                        burnDialogMode = "set"
                                                                                        showManualBurnDialog = true
                                                                                },
                                                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                                                        )
                                                                }
                                                        }
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Hero: net calories centered
                                                val net = caloriesEaten - caloriesBurned
                                                val netColor =
                                                        if (net <= 0) AppColors.success
                                                        else AppColors.calorie
                                                Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {
                                                        Text(
                                                                text =
                                                                        if (net > 0)
                                                                                "+${fmtNum(net)}"
                                                                        else fmtNum(net),
                                                                fontFamily = interFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 36.sp,
                                                                color = netColor
                                                        )
                                                        Text(
                                                                text =
                                                                        if (net <= 0)
                                                                                "calorie deficit"
                                                                        else "calorie surplus",
                                                                fontFamily = interFamily,
                                                                fontSize = 12.sp,
                                                                color = AppColors.textSecondary
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Consumed | Burned flanking stats
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceEvenly
                                                ) {
                                                        Column(
                                                                horizontalAlignment =
                                                                        Alignment
                                                                                .CenterHorizontally,
                                                                modifier = Modifier.weight(1f)
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                fmtNum(
                                                                                        caloriesEaten
                                                                                ),
                                                                        fontFamily = interFamily,
                                                                        fontWeight =
                                                                                FontWeight.SemiBold,
                                                                        fontSize = 18.sp,
                                                                        color = AppColors.calorie
                                                                )
                                                                Text(
                                                                        text = "consumed",
                                                                        fontFamily = interFamily,
                                                                        fontSize = 11.sp,
                                                                        color =
                                                                                AppColors
                                                                                        .textSecondary
                                                                )
                                                        }
                                                        // Vertical divider
                                                        Box(
                                                                modifier =
                                                                        Modifier.width(1.dp)
                                                                                .height(36.dp)
                                                                                .align(
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                                )
                                                                                .background(
                                                                                        AppColors
                                                                                                .border
                                                                                )
                                                        )
                                                        Column(
                                                                horizontalAlignment =
                                                                        Alignment
                                                                                .CenterHorizontally,
                                                                modifier = Modifier.weight(1f)
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                fmtNum(
                                                                                        caloriesBurned
                                                                                ),
                                                                        fontFamily = interFamily,
                                                                        fontWeight =
                                                                                FontWeight.SemiBold,
                                                                        fontSize = 18.sp,
                                                                        color = AppColors.success
                                                                )
                                                                Text(
                                                                        text = "burned",
                                                                        fontFamily = interFamily,
                                                                        fontSize = 11.sp,
                                                                        color = AppColors.textSecondary
                                                                )
                                                        }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Consumed vs Burned bar
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .height(8.dp)
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        4.dp
                                                                                )
                                                                        ),
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(2.dp)
                                                ) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.weight(
                                                                                        animatedRatio
                                                                                )
                                                                                .fillMaxHeight()
                                                                                .background(
                                                                                        AppColors
                                                                                                .calorie
                                                                                )
                                                        )
                                                        Box(
                                                                modifier =
                                                                        Modifier.weight(
                                                                                        1f -
                                                                                                animatedRatio
                                                                                )
                                                                                .fillMaxHeight()
                                                                                .background(
                                                                                        AppColors
                                                                                                .success
                                                                                )
                                                        )
                                                }
                                        }
                                }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Meal Visualization card
                        AnimatedVisibility(
                                visible = cardVisible[2].value,
                                enter =
                                        fadeIn(tween(300)) +
                                                slideInVertically(tween(300)) { it / 3 }
                        ) {
                                MealVisualizationCard(
                                        breakdown = mealBreakdown,
                                        selectedMeal = selectedMealType,
                                        calorieGoal = calorieGoal,
                                        onMealSelected = { viewModel.selectMealType(it) },
                                        onMealDetailClick = { meal ->
                                                navigateToLog(mealFilter = meal.name)
                                        },
                                )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedVisibility(
                                visible = cardVisible[3].value,
                                enter =
                                        fadeIn(tween(300)) +
                                                slideInVertically(tween(300)) { it / 3 }
                        ) {
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor = AppColors.surface
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                        text = "Macros",
                                                        fontFamily = interFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 18.sp,
                                                        color = AppColors.textPrimary
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))

                                                val proteinPct =
                                                        if (proteinGoalG > 0)
                                                                (protein / proteinGoalG).coerceIn(
                                                                        0f,
                                                                        1f
                                                                )
                                                        else 0f
                                                val carbsPct =
                                                        if (carbsGoalG > 0)
                                                                (carbs / carbsGoalG).coerceIn(
                                                                        0f,
                                                                        1f
                                                                )
                                                        else 0f
                                                val fatPct =
                                                        if (fatGoalG > 0)
                                                                (fat / fatGoalG).coerceIn(0f, 1f)
                                                        else 0f
                                                val sugarPct =
                                                        if (sugarGoalG > 0)
                                                                (sugar / sugarGoalG).coerceIn(
                                                                        0f,
                                                                        1f
                                                                )
                                                        else 0f

                                                MacroRow(
                                                        "Protein",
                                                        protein,
                                                        AppColors.protein,
                                                        proteinPct,
                                                        goalG = proteinGoalG
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                MacroRow(
                                                        "Carbs",
                                                        carbs,
                                                        AppColors.carbs,
                                                        carbsPct,
                                                        goalG = carbsGoalG
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                MacroRow(
                                                        "Fat",
                                                        fat,
                                                        AppColors.fat,
                                                        fatPct,
                                                        goalG = fatGoalG
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                MacroRow(
                                                        "Sugar",
                                                        sugar,
                                                        AppColors.sugar,
                                                        sugarPct,
                                                        goalG = sugarGoalG
                                                )
                                        }
                                }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedVisibility(
                                visible = cardVisible[4].value,
                                enter =
                                        fadeIn(tween(300)) +
                                                slideInVertically(tween(300)) { it / 3 }
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        OutlinedButton(
                                                onClick = { navigateToLog() },
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                Text(
                                                        text = "Diary",
                                                        fontFamily = interFamily,
                                                        fontWeight = FontWeight.Medium
                                                )
                                        }
                                        OutlinedButton(
                                                onClick = { navigateTo(AppRoute.Steps) },
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                Text(
                                                        text = "View Steps",
                                                        fontFamily = interFamily,
                                                        fontWeight = FontWeight.Medium
                                                )
                                        }
                                }
                        }
                }

                if (showConfetti) {
                        ConfettiBurst(trigger = true)
                }

                // Manual burned calories dialog
                if (showManualBurnDialog) {
                        val isSetMode = burnDialogMode == "set"
                        var burnInput by remember { mutableStateOf(if (isSetMode) caloriesBurned.toString() else "") }
                        AlertDialog(
                                onDismissRequest = { showManualBurnDialog = false },
                                title = {
                                        Text(
                                                if (isSetMode) "Set Burned Calories" else "Add Burned Calories",
                                                fontFamily = interFamily, fontWeight = FontWeight.Bold,
                                        )
                                },
                                text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                        if (isSetMode) "Override today's total burned calories."
                                                        else "Add exercise calories on top of your current total.",
                                                        fontFamily = interFamily, fontSize = 13.sp, color = AppColors.textSecondary,
                                                )
                                                OutlinedTextField(
                                                        value = burnInput,
                                                        onValueChange = { burnInput = it.filter { c -> c.isDigit() } },
                                                        label = { Text(if (isSetMode) "Total calories" else "Calories to add", fontFamily = interFamily) },
                                                        placeholder = { Text(if (isSetMode) caloriesBurned.toString() else "e.g. 250", fontFamily = interFamily) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                )
                                        }
                                },
                                confirmButton = {
                                        Button(
                                                onClick = {
                                                        burnInput.toIntOrNull()?.takeIf { it >= 0 }?.let { cal ->
                                                                if (isSetMode) viewModel.setTotalBurnedCal(cal)
                                                                else viewModel.addManualBurnedCal(cal)
                                                        }
                                                        showManualBurnDialog = false
                                                },
                                                enabled = (burnInput.toIntOrNull() ?: -1) >= 0,
                                        ) { Text(if (isSetMode) "Set" else "Add", fontFamily = interFamily) }
                                },
                                dismissButton = {
                                        TextButton(onClick = { showManualBurnDialog = false }) {
                                                Text("Cancel", fontFamily = interFamily)
                                        }
                                },
                        )
                }
        }
}

@Composable
private fun MacroRow(
        name: String,
        grams: Float,
        color: Color,
        progress: Float,
        showBar: Boolean = true,
        goalG: Int = 0
) {
        Column {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Text(
                                text = name,
                                fontFamily = interFamily,
                                fontSize = 13.sp,
                                color = AppColors.textSecondary
                        )
                        Text(
                                text =
                                        if (goalG > 0) "${grams.toInt()}g / ${goalG}g"
                                        else "${grams.toInt()}g",
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = AppColors.textPrimary
                        )
                }
                if (showBar) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                color = color,
                                trackColor = color.copy(alpha = 0.15f),
                                drawStopIndicator = {}
                        )
                }
        }
}

private fun boldCoachTip(text: String) = buildAnnotatedString {
        if (text.contains("**")) {
                val regex = Regex("""\*\*(.+?)\*\*""")
                var cursor = 0
                for (match in regex.findAll(text)) {
                        append(text.substring(cursor, match.range.first))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(match.groupValues[1])
                        }
                        cursor = match.range.last + 1
                }
                append(text.substring(cursor))
        } else {
                val patterns =
                        Regex(
                                """(\d[\d,]*\s*(?:cal|steps?|%|g)\b)""" +
                                        """|(no food[a-z ]*logged)""" +
                                        """|(no steps[a-z ]*yet)""" +
                                        """|(no food or steps)""" +
                                        """|(step goal crushed)""" +
                                        """|(right on track)""" +
                                        """|(under half)""" +
                                        """|(under \d+%)""" +
                                        """|(protein'?s low)""" +
                                        """|(fat intake is high)""" +
                                        """|(great step count)""" +
                                        """|(well managed)""" +
                                        """|(making progress)""" +
                                        """|(eating enough)""",
                                RegexOption.IGNORE_CASE
                        )
                var cursor = 0
                for (match in patterns.findAll(text)) {
                        if (match.range.first < cursor) continue
                        append(text.substring(cursor, match.range.first))
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.value) }
                        cursor = match.range.last + 1
                }
                append(text.substring(cursor))
        }
}
