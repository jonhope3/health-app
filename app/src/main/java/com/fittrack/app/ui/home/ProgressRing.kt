package com.fittrack.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
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

fun progressColor(ratio: Float, lowGood: Boolean): Color {
    val pct = if (lowGood) ratio else (1f - ratio)
    return when {
        pct < 0.5f -> Color(0xFF34A853)
        pct < 0.85f -> Color(0xFFFBBC04)
        else -> Color(0xFFEA4335)
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

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val displayValue = if (goalValue > 0) {
        (goalValue * animatedProgress.value).toInt().coerceAtMost(currentValue)
    } else 0

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val rawGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val glowAlpha = if (progress >= 0.95f) rawGlowAlpha else 0f

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

                if (glowAlpha > 0f) {
                    val glowStroke = strokeWidthPx * 2.5f
                    drawArc(
                        color = ringColor.copy(alpha = glowAlpha),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - radius - (glowStroke - strokeWidthPx) / 2,
                            center.y - radius - (glowStroke - strokeWidthPx) / 2
                        ),
                        size = Size(
                            radius * 2 + (glowStroke - strokeWidthPx),
                            radius * 2 + (glowStroke - strokeWidthPx)
                        ),
                        style = Stroke(width = glowStroke, cap = StrokeCap.Round)
                    )
                }

                drawArc(
                    color = Color(0xFFDADCE0),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                if (animatedProgress.value > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress.value,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fmtNum(displayValue),
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
