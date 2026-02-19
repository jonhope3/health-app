package com.fittrack.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val calorieGoal by viewModel.calorieGoal.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val weightLbs by viewModel.weightLbs.collectAsState()
    val heightFt by viewModel.heightFt.collectAsState()
    val heightIn by viewModel.heightIn.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text = "Settings",
            fontFamily = interFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = AppColors.textPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Goals section
        SectionHeader("Daily Goals")
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = calorieGoal,
                    onValueChange = { viewModel.setCalorieGoal(it) },
                    label = { Text("Calorie Goal", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Recommended: 1,500 – 3,000", fontFamily = interFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = stepGoal,
                    onValueChange = { viewModel.setStepGoal(it) },
                    label = { Text("Step Goal", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Recommended: 8,000 – 12,000", fontFamily = interFamily) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(24.dp))

        // Body section
        SectionHeader("Body Measurements")
        Text(
            text = "Used to estimate calories burned from steps",
            fontFamily = interFamily,
            fontSize = 13.sp,
            color = AppColors.textSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = weightLbs,
                    onValueChange = { viewModel.setWeightLbs(it) },
                    label = { Text("Weight", fontFamily = interFamily) },
                    suffix = { Text("lbs", fontFamily = interFamily, color = AppColors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Height",
                    fontFamily = interFamily,
                    fontSize = 14.sp,
                    color = AppColors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = heightFt,
                        onValueChange = { viewModel.setHeightFt(it) },
                        label = { Text("Feet", fontFamily = interFamily) },
                        suffix = { Text("ft", fontFamily = interFamily, color = AppColors.textSecondary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = heightIn,
                        onValueChange = { viewModel.setHeightIn(it) },
                        label = { Text("Inches", fontFamily = interFamily) },
                        suffix = { Text("in", fontFamily = interFamily, color = AppColors.textSecondary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.save()
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.primary,
                contentColor = AppColors.textOnPrimary
            )
        ) {
            Text(
                text = "Save",
                fontFamily = interFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = interFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = AppColors.textPrimary
    )
}
