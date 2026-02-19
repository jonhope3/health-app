package com.fittrack.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.util.fmtNum

@Composable
fun ProgressRing(
    progress: Float,
    currentValue: Int,
    goalValue: Int,
    label: String,
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val ringSize = 140.dp
    val strokeWidth = 12.dp
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .size(ringSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokeWidthPx = with(density) { strokeWidth.toPx() }
            val canvasSize = this.size.minDimension
            val radius = (canvasSize / 2) - strokeWidthPx / 2
            val center = Offset(canvasSize / 2, canvasSize / 2)

            // Gray background arc (270 degrees)
            drawArc(
                color = AppColors.border,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Colored progress arc with sweep gradient
            if (progress > 0f) {
                val sweepAngle = 270f * progress.coerceIn(0f, 1f)
                val sweepGradient = Brush.sweepGradient(
                    colors = listOf(gradientStart, gradientEnd),
                    center = center
                )
                drawArc(
                    brush = sweepGradient,
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        // Center content
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
        ) {
            Text(
                text = fmtNum(currentValue),
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = AppColors.textPrimary
            )
            Text(
                text = "/ ${fmtNum(goalValue)}",
                fontFamily = interFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = AppColors.textSecondary
            )
            Text(
                text = label,
                fontFamily = interFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = AppColors.textSecondary
            )
        }
    }
}
