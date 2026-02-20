package com.fittrack.app.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.NutritionResult
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily

enum class MeasureUnit(val label: String, val gramsPerUnit: Float, val presets: List<Float>) {
    GRAMS("g", 1f, listOf(50f, 100f, 150f, 200f)),
    OUNCES("oz", 28.3495f, listOf(1f, 2f, 4f, 8f)),
    CUPS("cup", 240f, listOf(0.25f, 0.5f, 1f, 2f)),
    ML("ml", 1f, listOf(50f, 100f, 250f, 500f)),
    TBSP("tbsp", 15f, listOf(1f, 2f, 3f, 4f)),
    TSP("tsp", 5f, listOf(1f, 2f, 3f, 4f));

    fun toGrams(amount: Float): Float = amount * gramsPerUnit

    fun formatPreset(value: Float): String {
        return if (value == value.toLong().toFloat()) "${value.toLong()} $label"
        else "$value $label"
    }
}

@Composable
fun NutritionCard(
    result: NutritionResult,
    onAdd: (String, NutritionResult, Float, String) -> Unit
) {
    val parsedInfo = remember(result) { com.fittrack.app.util.parseServingSize(result.servingDescription) }
    
    var name by remember(result) { mutableStateOf(result.name) }
    var amount by remember(result) { 
        val fmt = if (parsedInfo.amount == parsedInfo.amount.toInt().toFloat()) 
            parsedInfo.amount.toInt().toString() 
        else "%.1f".format(parsedInfo.amount)
        mutableStateOf(fmt) 
    }
    var selectedUnit by remember(result) { mutableStateOf(parsedInfo.unit) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    val amountValue = amount.replace(",", ".").toFloatOrNull() ?: 0f
    val gramsValue = selectedUnit.toGrams(amountValue)
    val factor = if (selectedUnit == parsedInfo.unit) {
        amountValue / parsedInfo.amount.coerceAtLeast(0.1f)
    } else {
        (amountValue * selectedUnit.gramsPerUnit) / parsedInfo.baseGrams.coerceAtLeast(1f)
    }

    val scaledCalories = (result.calories * factor).toInt()
    val scaledProtein = result.protein * factor
    val scaledCarbs = result.carbs * factor
    val scaledFat = result.fat * factor

    val showInput = result.servingDescription?.contains("100g", ignoreCase = true) == true
            || result.servingDescription?.contains("serving", ignoreCase = true) == true
            || result.servingDescription != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food Name", fontFamily = interFamily) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )
            )

            if (result.servingDescription != null) {
                Text(
                    text = "Per ${result.servingDescription}",
                    fontFamily = interFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary
                )
            }

            if (showInput) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(8) },
                        label = { Text("Amount", fontFamily = interFamily) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { unitMenuExpanded = true }
                                .background(AppColors.primarySurface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedUnit.label,
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.primary
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Select unit",
                                tint = AppColors.primary
                            )
                        }

                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            MeasureUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${unit.label} (${unit.name.lowercase()})",
                                            fontFamily = interFamily
                                        )
                                    },
                                    onClick = {
                                        if (selectedUnit != unit) {
                                            val currentGrams = selectedUnit.toGrams(amountValue)
                                            val converted = currentGrams / unit.gramsPerUnit
                                            amount = if (converted == converted.toInt().toFloat())
                                                converted.toInt().toString()
                                            else "%.1f".format(java.util.Locale.US, converted)
                                        }
                                        selectedUnit = unit
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedUnit.presets.forEach { preset ->
                        Button(
                            onClick = {
                                amount = if (preset == preset.toLong().toFloat())
                                    preset.toLong().toString()
                                else preset.toString()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.primarySurface,
                                contentColor = AppColors.primary
                            )
                        ) {
                            Text(
                                selectedUnit.formatPreset(preset),
                                fontFamily = interFamily,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroChip(label = "Cal", value = "$scaledCalories", color = AppColors.calorie)
                MacroChip(label = "P", value = "${"%.1f".format(scaledProtein)}g", color = AppColors.protein)
                MacroChip(label = "C", value = "${"%.1f".format(scaledCarbs)}g", color = AppColors.carbs)
                MacroChip(label = "F", value = "${"%.1f".format(scaledFat)}g", color = AppColors.fat)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val qtyDisplay = if (selectedUnit == MeasureUnit.GRAMS) {
                        if (amountValue == amountValue.toInt().toFloat()) "${amountValue.toInt()}g"
                        else "${"%.1f".format(amountValue)}g"
                    } else {
                        if (amountValue == amountValue.toInt().toFloat()) "${amountValue.toInt()} ${selectedUnit.label}"
                        else "${"%.1f".format(amountValue)} ${selectedUnit.label}"
                    }
                    onAdd(name, result, gramsValue, qtyDisplay)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary, contentColor = AppColors.textOnPrimary)
            ) {
                Text("Add to Log", fontFamily = interFamily, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MacroChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: $value",
            fontFamily = interFamily,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textPrimary
        )
    }
}
