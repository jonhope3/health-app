package com.fittrack.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.ui.common.ScreenScaffold
import com.fittrack.app.util.fmtNum
import com.fittrack.app.util.getGreeting
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

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
private fun OnboardingDialog(goalsRepository: GoalsRepository, onComplete: () -> Unit) {
        var step by remember { mutableStateOf(0) }
        var nameText by remember { mutableStateOf("") }
        var calorieText by remember { mutableStateOf("2000") }
        var stepText by remember { mutableStateOf("10000") }

        val titles = listOf("What should we call you?", "Daily calorie goal?", "Daily step goal?")
        val subtitles =
                listOf(
                        "We'll use this to personalize your experience.",
                        "How many calories are you aiming for each day?",
                        "How many steps do you want to hit daily?"
                )

        fun saveAndFinish() {
                val name = nameText.trim()
                if (name.isNotBlank()) goalsRepository.setNickname(name)
                calorieText.toIntOrNull()?.let {
                        if (it in 500..10000) goalsRepository.setCalorieGoal(it)
                }
                stepText.toIntOrNull()?.let {
                        if (it in 100..200000) goalsRepository.setStepGoal(it)
                }
                goalsRepository.setOnboardingCompleted()
                onComplete()
        }

        fun skipStep() {
                if (step < 2) step++ else saveAndFinish()
        }

        AlertDialog(
                onDismissRequest = {},
                title = {
                        Column {
                                Text(
                                        text = "Welcome to FitTrack",
                                        fontFamily = interFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = AppColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = "Step ${step + 1} of 3",
                                        fontFamily = interFamily,
                                        fontSize = 12.sp,
                                        color = AppColors.textSecondary
                                )
                        }
                },
                text = {
                        Column {
                                Text(
                                        text = titles[step],
                                        fontFamily = interFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = AppColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = subtitles[step],
                                        fontFamily = interFamily,
                                        fontSize = 13.sp,
                                        color = AppColors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                when (step) {
                                        0 ->
                                                OutlinedTextField(
                                                        value = nameText,
                                                        onValueChange = { nameText = it },
                                                        label = {
                                                                Text(
                                                                        "Your name",
                                                                        fontFamily = interFamily
                                                                )
                                                        },
                                                        placeholder = {
                                                                Text(
                                                                        "e.g. Alex",
                                                                        fontFamily = interFamily,
                                                                        color =
                                                                                AppColors
                                                                                        .textSecondary
                                                                )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true
                                                )
                                        1 ->
                                                OutlinedTextField(
                                                        value = calorieText,
                                                        onValueChange = {
                                                                calorieText =
                                                                        it.filter { c ->
                                                                                c.isDigit()
                                                                        }
                                                        },
                                                        label = {
                                                                Text(
                                                                        "Calorie goal",
                                                                        fontFamily = interFamily
                                                                )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        keyboardOptions =
                                                                KeyboardOptions(
                                                                        keyboardType =
                                                                                KeyboardType.Number
                                                                )
                                                )
                                        2 ->
                                                OutlinedTextField(
                                                        value = stepText,
                                                        onValueChange = {
                                                                stepText =
                                                                        it.filter { c ->
                                                                                c.isDigit()
                                                                        }
                                                        },
                                                        label = {
                                                                Text(
                                                                        "Step goal",
                                                                        fontFamily = interFamily
                                                                )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        keyboardOptions =
                                                                KeyboardOptions(
                                                                        keyboardType =
                                                                                KeyboardType.Number
                                                                )
                                                )
                                }
                        }
                },
                confirmButton = {
                        Button(
                                onClick = { if (step < 2) step++ else saveAndFinish() },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = AppColors.primary
                                        )
                        ) {
                                Text(
                                        text = if (step < 2) "Next" else "Done",
                                        fontFamily = interFamily
                                )
                        }
                },
                dismissButton = {
                        TextButton(onClick = { skipStep() }) {
                                Text(
                                        "Skip",
                                        fontFamily = interFamily,
                                        color = AppColors.textSecondary
                                )
                        }
                }
        )
}

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
        val context = LocalContext.current
        val goalsRepository = remember { GoalsRepository(context) }
        var showOnboarding by remember { mutableStateOf(!goalsRepository.hasCompletedOnboarding()) }

        val calorieGoal by viewModel.calorieGoal.collectAsState()
        val stepGoal by viewModel.stepGoal.collectAsState()
        val caloriesEaten by viewModel.caloriesEaten.collectAsState()
        val protein by viewModel.protein.collectAsState()
        val carbs by viewModel.carbs.collectAsState()
        val fat by viewModel.fat.collectAsState()
        val sugar by viewModel.sugar.collectAsState()
        val steps by viewModel.steps.collectAsState()
        val caloriesBurned by viewModel.caloriesBurned.collectAsState()
        val proteinGoalG by viewModel.proteinGoalG.collectAsState()
        val carbsGoalG by viewModel.carbsGoalG.collectAsState()
        val fatGoalG by viewModel.fatGoalG.collectAsState()
        val sugarGoalG by viewModel.sugarGoalG.collectAsState()
        val nickname by viewModel.nickname.collectAsState()
        val coachTip by viewModel.coachTip.collectAsState()
        val downloadState by viewModel.downloadState.collectAsState()
        val downloadProgress by viewModel.downloadProgress.collectAsState()

        if (showOnboarding) {
                OnboardingDialog(
                        goalsRepository = goalsRepository,
                        onComplete = {
                                showOnboarding = false
                                viewModel.loadData()
                        }
                )
        }

        LaunchedEffect(Unit) { viewModel.loadData() }

        val navigateTo = { route: String ->
                navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
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

        val cardCount = 5
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
                                        onClick = { navigateTo("log") }
                                )
                                ProgressRing(
                                        progress = stepsProgress,
                                        currentValue = steps,
                                        goalValue = stepGoal,
                                        label = "Steps",
                                        ringColor = progressColor(stepsProgress, lowGood = false),
                                        onClick = { navigateTo("steps") },
                                        onLongClick = {
                                                val intents =
                                                        com.fittrack.app.services
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
                        val isDownloading =
                                downloadState is com.fittrack.app.data.DownloadState.Downloading
                        val isError = downloadState is com.fittrack.app.data.DownloadState.Error

                        AnimatedVisibility(
                                visible =
                                        cardVisible[0].value &&
                                                (coachTip.isNotBlank() || isDownloading || isError),
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

                                                        if (isDownloading) {
                                                                Text(
                                                                        text =
                                                                                "Setting up AI Coach... ${(downloadProgress * 100).toInt()}%",
                                                                        fontFamily = interFamily,
                                                                        fontSize = 14.sp,
                                                                        color =
                                                                                AppColors
                                                                                        .textPrimary
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        8.dp
                                                                                )
                                                                )
                                                                LinearProgressIndicator(
                                                                        progress = {
                                                                                downloadProgress
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .height(
                                                                                                4.dp
                                                                                        )
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        2.dp
                                                                                                )
                                                                                        ),
                                                                        color = AppColors.primary,
                                                                        trackColor =
                                                                                AppColors.primary
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.2f
                                                                                        )
                                                                )
                                                                Text(
                                                                        text =
                                                                                "This only happens once. It's a large file (1.5GB) to enable offline privacy.",
                                                                        fontFamily = interFamily,
                                                                        fontSize = 11.sp,
                                                                        color =
                                                                                AppColors
                                                                                        .textSecondary,
                                                                        lineHeight = 14.sp,
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        top = 4.dp
                                                                                )
                                                                )
                                                        } else if (isError) {
                                                                Text(
                                                                        text =
                                                                                "AI Coach unavailable: ${(downloadState as com.fittrack.app.data.DownloadState.Error).message}",
                                                                        fontFamily = interFamily,
                                                                        fontSize = 14.sp,
                                                                        color = AppColors.calorie
                                                                )
                                                        } else {
                                                                Text(
                                                                        text =
                                                                                boldCoachTip(
                                                                                        coachTip
                                                                                ),
                                                                        fontFamily = interFamily,
                                                                        fontSize = 14.sp,
                                                                        lineHeight = 20.sp,
                                                                        color =
                                                                                AppColors
                                                                                        .textPrimary
                                                                )
                                                        }
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
                                                Text(
                                                        text = "Calorie Balance",
                                                        fontFamily = interFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 16.sp,
                                                        color = AppColors.textPrimary
                                                )
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
                                                                        color =
                                                                                AppColors
                                                                                        .textSecondary
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

                        AnimatedVisibility(
                                visible = cardVisible[2].value,
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
                                visible = cardVisible[3].value,
                                enter =
                                        fadeIn(tween(300)) +
                                                slideInVertically(tween(300)) { it / 3 }
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        OutlinedButton(
                                                onClick = { navigateTo("log") },
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                Text(
                                                        text = "Log Food",
                                                        fontFamily = interFamily,
                                                        fontWeight = FontWeight.Medium
                                                )
                                        }
                                        OutlinedButton(
                                                onClick = { navigateTo("steps") },
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
                                color = color.copy(alpha = 0.2f),
                                trackColor = Color.Transparent,
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
