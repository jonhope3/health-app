package com.fittrack.app.ui.log

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.DiaryItem
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.util.fmtNum
import com.fittrack.app.util.formatTime

@Composable
fun DiaryItemCard(entry: DiaryItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(8.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier =
                            Modifier.width(4.dp)
                                    .height(56.dp)
                                    .background(AppColors.primary, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = entry.name,
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.textPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "${fmtNum(entry.calories)} cal",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!entry.quantity.isNullOrBlank()) {
                        Text(
                                text = entry.quantity,
                                fontFamily = interFamily,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.textPrimary
                        )
                        Text(
                                text = "·",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.textSecondary
                        )
                    }
                    Text(
                            text = formatTime(entry.timestamp),
                            fontFamily = interFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MacroPill("Protein", "${fmt(entry.protein)}g", AppColors.protein)
                    MacroPill("Carbs", "${fmt(entry.carbs)}g", AppColors.carbs)
                    MacroPill("Fat", "${fmt(entry.fat)}g", AppColors.fat)
                    MacroPill("Sugar", "${fmt(entry.sugar)}g", AppColors.sugar)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = AppColors.textSecondary
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                            text = { Text("Edit Entry", fontFamily = interFamily) },
                            onClick = {
                                expanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                            text = {
                                Text("Delete", fontFamily = interFamily, color = AppColors.error)
                            },
                            onClick = {
                                expanded = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = AppColors.error
                                )
                            }
                    )
                }
            }
        }
    }
}

private fun fmt(v: Float): String {
    val rounded = kotlin.math.round(v * 10) / 10f
    return if (rounded == rounded.toLong().toFloat()) rounded.toLong().toString()
    else "%.1f".format(rounded)
}

@Composable
private fun MacroPill(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Box(
            modifier =
                    Modifier.background(color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
                text = "$label: $value",
                fontFamily = interFamily,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.textPrimary
        )
    }
}
