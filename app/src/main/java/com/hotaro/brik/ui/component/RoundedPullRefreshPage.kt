package com.hotaro.brik.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundedPullRefreshPage(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    pageColor: Color = Color.Unspecified,
    pageShape: Shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    content: @Composable (PaddingValues) -> Unit
) {
    val state = rememberPullToRefreshState()
    val view = LocalView.current
    val density = LocalDensity.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val actualContainerColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else if (isDark) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val actualPageColor = if (pageColor != Color.Unspecified) {
        pageColor
    } else if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }

    val thresholdDp = 80.dp
    val thresholdPx = with(density) { thresholdDp.toPx() }
    val maxPullPx = with(density) { 140.dp.toPx() }

    // Haptic feedback tick when passing threshold
    var hasVibratedThreshold by remember { mutableStateOf(false) }
    LaunchedEffect(state.distanceFraction) {
        if (state.distanceFraction >= 1.0f && !hasVibratedThreshold && !isRefreshing) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            hasVibratedThreshold = true
        } else if (state.distanceFraction < 0.2f) {
            hasVibratedThreshold = false
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                state = state,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            ),
        containerColor = actualContainerColor,
        topBar = topBar,
        floatingActionButton = floatingActionButton
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Loading indicator revealed behind the descending page: morphs from circle into smiley
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .graphicsLayer {
                        val progress = state.distanceFraction.coerceIn(0f, 1f)
                        alpha = if (isRefreshing) 1f else progress
                        val scale = if (isRefreshing) 1f else 0.4f + (progress * 0.6f)
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                SmileyPullRefreshIndicator(
                    progress = state.distanceFraction.coerceIn(0f, 1f),
                    isRefreshing = isRefreshing,
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Rounded Page Surface that detaches and slides downwards when pulled
            val pullDistance = (state.distanceFraction * thresholdPx).coerceIn(0f, maxPullPx)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = pullDistance
                        clip = true
                        shape = pageShape
                    },
                shape = pageShape,
                color = actualPageColor
            ) {
                val subPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    top = 0.dp,
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = innerPadding.calculateBottomPadding()
                )
                content(subPadding)
            }
        }
    }
}

/**
 * Animated Pull-to-Refresh indicator that morphs from a circle outline into a smiling face
 * as the user pulls down, and playfully winks/spins when refreshing.
 */
@Composable
fun SmileyPullRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "smileyPullRefresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringSpin"
    )

    val winkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "winkAnim"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 3.2.dp.toPx()
        val diameter = minOf(size.width, size.height)
        val radius = (diameter - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Outer circle (draws as pull progress increases, spins when refreshing)
        if (isRefreshing) {
            drawArc(
                color = color,
                startAngle = rotation,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        } else {
            val sweep = (progress * 360f).coerceIn(0f, 360f)
            if (sweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // 2. Eyes morphing in (from 25% pull onward)
        val eyeProgress = if (isRefreshing) 1f else ((progress - 0.25f) / 0.4f).coerceIn(0f, 1f)
        if (eyeProgress > 0f) {
            val eyeOffsetX = radius * 0.36f
            val eyeOffsetY = radius * 0.22f
            val eyeRadius = 2.4.dp.toPx() * eyeProgress

            val isWinking = isRefreshing && winkProgress > 0.6f
            if (isWinking) {
                // Left eye winks to a playful curved arc
                drawArc(
                    color = color,
                    startAngle = 190f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(center.x - eyeOffsetX - eyeRadius, center.y - eyeOffsetY - eyeRadius),
                    size = Size(eyeRadius * 2f, eyeRadius * 2f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                drawCircle(
                    color = color,
                    radius = eyeRadius,
                    center = Offset(center.x - eyeOffsetX, center.y - eyeOffsetY)
                )
            }

            // Right eye
            drawCircle(
                color = color,
                radius = eyeRadius,
                center = Offset(center.x + eyeOffsetX, center.y - eyeOffsetY)
            )
        }

        // 3. Smile morphing in (from 45% pull onward)
        val smileProgress = if (isRefreshing) 1f else ((progress - 0.45f) / 0.55f).coerceIn(0f, 1f)
        if (smileProgress > 0f) {
            val mouthWidth = radius * 0.95f
            val mouthHeight = radius * 0.72f
            val mouthTop = center.y - radius * 0.04f
            val mouthLeft = center.x - mouthWidth / 2f

            // Start angle moves inward to outward, expanding smile curve from center
            val totalSweep = 120f * smileProgress
            val startAngle = 90f - (totalSweep / 2f)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = Offset(mouthLeft, mouthTop),
                size = Size(mouthWidth, mouthHeight),
                style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
            )
        }
    }
}
