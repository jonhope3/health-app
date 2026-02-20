package com.fittrack.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.util.fmtNum
import com.fittrack.app.util.getGreeting
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.fittrack.app.ui.common.ScreenScaffold
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val calorieGoal by viewModel.calorieGoal.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val caloriesEaten by viewModel.caloriesEaten.collectAsState()
    val protein by viewModel.protein.collectAsState()
    val carbs by viewModel.carbs.collectAsState()
    val fat by viewModel.fat.collectAsState()
    val steps by viewModel.steps.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val navigateTo = { route: String ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val dateFormatted = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    )

    ScreenScaffold {
        Text(
            text = getGreeting(),
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

        val caloriesProgress = (caloriesEaten.toFloat() / calorieGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
        val stepsProgress = (steps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)

        // Progress rings — color picked from green/yellow/red based on percentage
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
            val context = androidx.compose.ui.platform.LocalContext.current
            ProgressRing(
                progress = stepsProgress,
                currentValue = steps,
                goalValue = stepGoal,
                label = "Steps",
                ringColor = progressColor(stepsProgress, lowGood = false),
                onClick = { navigateTo("steps") },
                onLongClick = {
                    val intents = com.fittrack.app.services.HealthConnectService().getSettingsIntents(context)
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

        // Summary strip card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Goal",
                        fontFamily = interFamily,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary
                    )
                    Text(
                        text = fmtNum(calorieGoal),
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppColors.textPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Calories Consumed",
                        fontFamily = interFamily,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary
                    )
                    Text(
                        text = fmtNum(caloriesEaten),
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppColors.textPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Remaining",
                        fontFamily = interFamily,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary
                    )
                    Text(
                        text = fmtNum((calorieGoal - caloriesEaten).coerceAtLeast(0)),
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppColors.textPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Macros card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

                val totalCalFromMacros = protein * 4 + carbs * 4 + fat * 9
                val proteinPct = if (totalCalFromMacros > 0) (protein * 4 / totalCalFromMacros) else 0f
                val carbsPct = if (totalCalFromMacros > 0) (carbs * 4 / totalCalFromMacros) else 0f
                val fatPct = if (totalCalFromMacros > 0) (fat * 9 / totalCalFromMacros) else 0f

                MacroRow("Protein", protein, AppColors.protein, proteinPct)
                Spacer(modifier = Modifier.height(8.dp))
                MacroRow("Carbs", carbs, AppColors.carbs, carbsPct)
                Spacer(modifier = Modifier.height(8.dp))
                MacroRow("Fat", fat, AppColors.fat, fatPct)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(0.dp))

        // Quick Actions
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

@Composable
private fun MacroRow(
    name: String,
    grams: Float,
    color: Color,
    progress: Float
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
                text = "${grams.toInt()}g",
                fontFamily = interFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = AppColors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = AppColors.border,
            drawStopIndicator = {}
        )
    }
}
