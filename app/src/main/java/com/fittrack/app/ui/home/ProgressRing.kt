package com.fittrack.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.util.fmtNum

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

/** Pick green / yellow / red from the Google medium palette based on progress ratio.
 *  [lowGood] = true means low usage is good (calories), false means high is good (steps). */
fun progressColor(ratio: Float, lowGood: Boolean): Color {
    val pct = if (lowGood) ratio else (1f - ratio)
    return when {
        pct < 0.5f -> Color(0xFF34A853) // green  — under / well on track
        pct < 0.85f -> Color(0xFFFBBC04) // yellow — getting there
        else -> Color(0xFFEA4335)         // red    — over / far behind
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProgressRing(
    progress: Float,
    currentValue: Int,
    goalValue: Int,
    label: String,
    ringColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val ringSize = 140.dp
    val strokeWidth = 14.dp
    val density = LocalDensity.current

    Column(
        modifier = modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val strokeWidthPx = with(density) { strokeWidth.toPx() }
                val canvasSize = this.size.minDimension
                val radius = (canvasSize / 2) - strokeWidthPx / 2
                val center = Offset(canvasSize / 2, canvasSize / 2)
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)

                // Track ring — visible medium grey
                drawArc(
                    color = Color(0xFFDADCE0),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Progress arc
                if (progress > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = 135f,
                        sweepAngle = 270f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            // Number + small goal line
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fmtNum(currentValue),
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "of ${fmtNum(goalValue)}",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = AppColors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label.uppercase(),
            fontFamily = interFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = AppColors.textSecondary,
            letterSpacing = 1.sp
        )
    }
}

