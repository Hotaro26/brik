package com.hotaro.brik.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.hotaro.brik.ui.theme.PullRefreshAnimStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Universal dispatcher for all pull-to-refresh animation styles.
 */
@Composable
fun PullRefreshIndicatorDispatcher(
    style: PullRefreshAnimStyle,
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // When RANDOM is active, select a random animation style per pull session
    var activeRandomStyle by remember { mutableStateOf(PullRefreshAnimStyle.SMILEY) }
    val resolvedStyle = if (style == PullRefreshAnimStyle.RANDOM) {
        remember(isRefreshing, progress > 0.1f) {
            val concrete = listOf(
                PullRefreshAnimStyle.SMILEY,
                PullRefreshAnimStyle.CAT,
                PullRefreshAnimStyle.BUTTERFLY,
                PullRefreshAnimStyle.BOOKS,
                PullRefreshAnimStyle.LOTUS
            )
            if (!isRefreshing && progress <= 0.1f) {
                activeRandomStyle = concrete.random()
            }
            activeRandomStyle
        }
    } else {
        style
    }

    when (resolvedStyle) {
        PullRefreshAnimStyle.SMILEY -> SmileyPullRefreshIndicator(progress, isRefreshing, modifier, color)
        PullRefreshAnimStyle.CAT -> CatPullRefreshIndicator(progress, isRefreshing, modifier, color)
        PullRefreshAnimStyle.BUTTERFLY -> ButterflyPullRefreshIndicator(progress, isRefreshing, modifier, color)
        PullRefreshAnimStyle.BOOKS -> BooksPullRefreshIndicator(progress, isRefreshing, modifier, color)
        PullRefreshAnimStyle.LOTUS -> LotusPullRefreshIndicator(progress, isRefreshing, modifier, color)
        else -> SmileyPullRefreshIndicator(progress, isRefreshing, modifier, color)
    }
}

/**
 * 1. Smiley: Circular ring with playful eyes, winking and smiling.
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

        // Outer ring
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

        // Eyes
        val eyeProgress = if (isRefreshing) 1f else ((progress - 0.25f) / 0.4f).coerceIn(0f, 1f)
        if (eyeProgress > 0f) {
            val eyeOffsetX = radius * 0.36f
            val eyeOffsetY = radius * 0.22f
            val eyeRadius = 2.4.dp.toPx() * eyeProgress

            val isWinking = isRefreshing && winkProgress > 0.6f
            if (isWinking) {
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

            drawCircle(
                color = color,
                radius = eyeRadius,
                center = Offset(center.x + eyeOffsetX, center.y - eyeOffsetY)
            )
        }

        // Smile
        val smileProgress = if (isRefreshing) 1f else ((progress - 0.45f) / 0.55f).coerceIn(0f, 1f)
        if (smileProgress > 0f) {
            val mouthWidth = radius * 0.95f
            val mouthHeight = radius * 0.72f
            val mouthTop = center.y - radius * 0.04f
            val mouthLeft = center.x - mouthWidth / 2f

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

/**
 * 2. Cat: Cute feline outline with twitching ears, whiskers, and playful cat smile.
 */
@Composable
fun CatPullRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "catPullRefresh")
    val earTwitch by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "catEarTwitch"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 2.8.dp.toPx()
        val diameter = minOf(size.width, size.height)
        val radius = (diameter - strokeWidth) / 2.2f
        val center = Offset(size.width / 2f, size.height / 2f + radius * 0.12f)

        val p = if (isRefreshing) 1f else progress.coerceIn(0f, 1f)

        // Head outline with cat ears
        val headPath = Path().apply {
            val earAngleL = if (isRefreshing) earTwitch * 5f else 0f
            val earAngleR = if (isRefreshing) -earTwitch * 5f else 0f

            // Start near bottom
            moveTo(center.x, center.y + radius)
            // Left cheek arc
            cubicTo(
                center.x - radius * 1.05f, center.y + radius * 0.8f,
                center.x - radius * 1.05f, center.y - radius * 0.2f,
                center.x - radius * 0.7f, center.y - radius * 0.65f
            )
            // Left ear tip
            lineTo(center.x - radius * 0.9f + earAngleL, center.y - radius * 1.25f)
            lineTo(center.x - radius * 0.25f, center.y - radius * 0.85f)
            // Crown between ears
            lineTo(center.x + radius * 0.25f, center.y - radius * 0.85f)
            // Right ear tip
            lineTo(center.x + radius * 0.9f + earAngleR, center.y - radius * 1.25f)
            lineTo(center.x + radius * 0.7f, center.y - radius * 0.65f)
            // Right cheek arc
            cubicTo(
                center.x + radius * 1.05f, center.y - radius * 0.2f,
                center.x + radius * 1.05f, center.y + radius * 0.8f,
                center.x, center.y + radius
            )
            close()
        }

        drawPath(
            path = headPath,
            color = color.copy(alpha = (p * 0.95f).coerceIn(0.2f, 1f)),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Cat Eyes (^ ^ happy eyes)
        if (p > 0.35f) {
            val eyeAlpha = ((p - 0.35f) / 0.3f).coerceIn(0f, 1f)
            val eyeSpacing = radius * 0.42f
            val eyeY = center.y - radius * 0.15f
            val eyeW = radius * 0.22f

            // Left eye arc (happy curve ^)
            val leftEye = Path().apply {
                moveTo(center.x - eyeSpacing - eyeW, eyeY + 2f)
                quadraticTo(center.x - eyeSpacing, eyeY - eyeW * 1.2f, center.x - eyeSpacing + eyeW, eyeY + 2f)
            }
            // Right eye arc
            val rightEye = Path().apply {
                moveTo(center.x + eyeSpacing - eyeW, eyeY + 2f)
                quadraticTo(center.x + eyeSpacing, eyeY - eyeW * 1.2f, center.x + eyeSpacing + eyeW, eyeY + 2f)
            }
            drawPath(leftEye, color.copy(alpha = eyeAlpha), style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Round))
            drawPath(rightEye, color.copy(alpha = eyeAlpha), style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Round))
        }

        // Cat Nose & Mouth (3)
        if (p > 0.55f) {
            val mouthAlpha = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
            val noseY = center.y + radius * 0.08f
            drawCircle(color.copy(alpha = mouthAlpha), radius = 2.dp.toPx(), center = Offset(center.x, noseY))

            val mouthPath = Path().apply {
                val mouthW = radius * 0.22f
                val mouthH = radius * 0.18f
                // Left curve
                moveTo(center.x, noseY + 2f)
                quadraticTo(center.x - mouthW * 0.6f, noseY + mouthH, center.x - mouthW, noseY + mouthH * 0.6f)
                // Right curve
                moveTo(center.x, noseY + 2f)
                quadraticTo(center.x + mouthW * 0.6f, noseY + mouthH, center.x + mouthW, noseY + mouthH * 0.6f)
            }
            drawPath(mouthPath, color.copy(alpha = mouthAlpha), style = Stroke(strokeWidth * 0.8f, cap = StrokeCap.Round))
        }

        // Whiskers (3 on left, 3 on right)
        if (p > 0.65f) {
            val whiskerP = ((p - 0.65f) / 0.35f).coerceIn(0f, 1f)
            val wLen = radius * 0.48f * whiskerP
            val wy = center.y + radius * 0.12f

            listOf(-1f, 1f).forEach { side ->
                val startX = center.x + (radius * 0.65f * side)
                val endX = startX + (wLen * side)
                // Top whisker
                drawLine(color.copy(alpha = whiskerP * 0.85f), Offset(startX, wy - 4f), Offset(endX, wy - 8f), strokeWidth * 0.7f, StrokeCap.Round)
                // Middle whisker
                drawLine(color.copy(alpha = whiskerP * 0.85f), Offset(startX, wy), Offset(endX, wy), strokeWidth * 0.7f, StrokeCap.Round)
                // Bottom whisker
                drawLine(color.copy(alpha = whiskerP * 0.85f), Offset(startX, wy + 4f), Offset(endX, wy + 8f), strokeWidth * 0.7f, StrokeCap.Round)
            }
        }
    }
}

/**
 * 3. Butterfly: Graceful insect with fluttering wings and delicate antennae.
 */
@Composable
fun ButterflyPullRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "butterflyPullRefresh")
    val flapScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "butterflyFlap"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 2.6.dp.toPx()
        val diameter = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val wingSpan = (diameter - strokeWidth) / 2.1f

        val p = if (isRefreshing) 1f else progress.coerceIn(0f, 1f)
        val currentFlap = if (isRefreshing) flapScale else (0.4f + p * 0.6f)

        // Draw Left and Right Wings
        listOf(-1f, 1f).forEach { side ->
            scale(scaleX = currentFlap * side, scaleY = 1f, pivot = center) {
                // Upper Large Wing
                val upperWing = Path().apply {
                    moveTo(center.x, center.y - wingSpan * 0.15f)
                    cubicTo(
                        center.x + wingSpan * 0.5f, center.y - wingSpan * 1.15f * p,
                        center.x + wingSpan * 1.15f * p, center.y - wingSpan * 0.65f * p,
                        center.x + wingSpan * 0.85f * p, center.y + wingSpan * 0.05f
                    )
                    cubicTo(
                        center.x + wingSpan * 0.55f, center.y + wingSpan * 0.12f,
                        center.x + wingSpan * 0.2f, center.y + wingSpan * 0.08f,
                        center.x, center.y + wingSpan * 0.1f
                    )
                    close()
                }
                drawPath(
                    path = upperWing,
                    color = color.copy(alpha = (p * 0.9f).coerceIn(0.2f, 1f)),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Lower Soft Wing
                val lowerWing = Path().apply {
                    moveTo(center.x, center.y + wingSpan * 0.1f)
                    cubicTo(
                        center.x + wingSpan * 0.8f * p, center.y + wingSpan * 0.15f,
                        center.x + wingSpan * 0.85f * p, center.y + wingSpan * 0.85f * p,
                        center.x + wingSpan * 0.35f * p, center.y + wingSpan * 0.95f * p
                    )
                    cubicTo(
                        center.x + wingSpan * 0.15f, center.y + wingSpan * 0.65f,
                        center.x + wingSpan * 0.05f, center.y + wingSpan * 0.35f,
                        center.x, center.y + wingSpan * 0.25f
                    )
                    close()
                }
                drawPath(
                    path = lowerWing,
                    color = color.copy(alpha = (p * 0.85f).coerceIn(0.2f, 1f)),
                    style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Butterfly Center Body
        drawLine(
            color = color,
            start = Offset(center.x, center.y - wingSpan * 0.55f),
            end = Offset(center.x, center.y + wingSpan * 0.65f),
            strokeWidth = strokeWidth * 1.3f,
            cap = StrokeCap.Round
        )

        // Antennae
        val antPath = Path().apply {
            // Left antenna
            moveTo(center.x, center.y - wingSpan * 0.5f)
            quadraticTo(center.x - wingSpan * 0.28f, center.y - wingSpan * 0.85f, center.x - wingSpan * 0.35f, center.y - wingSpan * 0.95f)
            // Right antenna
            moveTo(center.x, center.y - wingSpan * 0.5f)
            quadraticTo(center.x + wingSpan * 0.28f, center.y - wingSpan * 0.85f, center.x + wingSpan * 0.35f, center.y - wingSpan * 0.95f)
        }
        drawPath(antPath, color = color, style = Stroke(width = strokeWidth * 0.75f, cap = StrokeCap.Round))
        drawCircle(color, radius = 1.8.dp.toPx(), center = Offset(center.x - wingSpan * 0.35f, center.y - wingSpan * 0.95f))
        drawCircle(color, radius = 1.8.dp.toPx(), center = Offset(center.x + wingSpan * 0.35f, center.y - wingSpan * 0.95f))
    }
}

/**
 * 4. Books: Open tome of wisdom with fluttering, turning pages.
 */
@Composable
fun BooksPullRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "booksPullRefresh")
    val pageFlip by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pageFlip"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 2.8.dp.toPx()
        val diameter = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f + 2.dp.toPx())
        val bookWidth = (diameter - strokeWidth) / 2.1f
        val bookHeight = bookWidth * 0.72f

        val p = if (isRefreshing) 1f else progress.coerceIn(0f, 1f)

        // Spine Center
        val spineTop = center.y - bookHeight * 0.6f
        val spineBottom = center.y + bookHeight * 0.55f

        // Left Book Page Outline
        val leftPage = Path().apply {
            moveTo(center.x, spineTop)
            cubicTo(
                center.x - bookWidth * 0.4f, spineTop - bookHeight * 0.25f * p,
                center.x - bookWidth * 0.8f, spineTop - bookHeight * 0.05f * p,
                center.x - bookWidth * p, spineTop + bookHeight * 0.1f
            )
            lineTo(center.x - bookWidth * p, spineBottom + bookHeight * 0.1f)
            cubicTo(
                center.x - bookWidth * 0.8f, spineBottom - bookHeight * 0.05f * p,
                center.x - bookWidth * 0.4f, spineBottom - bookHeight * 0.25f * p,
                center.x, spineBottom
            )
            close()
        }

        // Right Book Page Outline
        val rightPage = Path().apply {
            moveTo(center.x, spineTop)
            cubicTo(
                center.x + bookWidth * 0.4f, spineTop - bookHeight * 0.25f * p,
                center.x + bookWidth * 0.8f, spineTop - bookHeight * 0.05f * p,
                center.x + bookWidth * p, spineTop + bookHeight * 0.1f
            )
            lineTo(center.x + bookWidth * p, spineBottom + bookHeight * 0.1f)
            cubicTo(
                center.x + bookWidth * 0.8f, spineBottom - bookHeight * 0.05f * p,
                center.x + bookWidth * 0.4f, spineBottom - bookHeight * 0.25f * p,
                center.x, spineBottom
            )
            close()
        }

        drawPath(leftPage, color = color.copy(alpha = (p * 0.95f).coerceIn(0.2f, 1f)), style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(rightPage, color = color.copy(alpha = (p * 0.95f).coerceIn(0.2f, 1f)), style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Center spine line
        drawLine(color = color, start = Offset(center.x, spineTop - 2f), end = Offset(center.x, spineBottom + 4f), strokeWidth = strokeWidth * 1.2f, cap = StrokeCap.Round)

        // Animated Page Turning Flip Arc
        if (isRefreshing || p > 0.6f) {
            val flipFactor = if (isRefreshing) pageFlip else (p - 0.6f) / 0.4f
            val flipX = center.x + (bookWidth * 0.7f * flipFactor)
            val flippingPage = Path().apply {
                moveTo(center.x, spineTop)
                quadraticTo(flipX, spineTop - bookHeight * 0.4f, flipX, spineBottom - bookHeight * 0.15f)
                lineTo(center.x, spineBottom)
            }
            drawPath(flippingPage, color = color.copy(alpha = 0.75f), style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round))
        }

        // Bookmark ribbon hanging down
        val bookmark = Path().apply {
            moveTo(center.x, spineBottom)
            lineTo(center.x - 3.dp.toPx(), spineBottom + 6.dp.toPx())
            lineTo(center.x, spineBottom + 4.dp.toPx())
            lineTo(center.x + 3.dp.toPx(), spineBottom + 6.dp.toPx())
            close()
        }
        drawPath(bookmark, color = color)
    }
}

/**
 * 5. Man in Lotus: Serene meditator seated atop an unfurling lotus bloom with breathing aura.
 */
@Composable
fun LotusPullRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lotusPullRefresh")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val auraSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auraSpin"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 2.6.dp.toPx()
        val diameter = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val scaleRadius = (diameter - strokeWidth) / 2.1f

        val p = if (isRefreshing) 1f else progress.coerceIn(0f, 1f)
        val currentBreath = if (isRefreshing) breathScale else 1f

        scale(scale = currentBreath, pivot = center) {
            // 1. Serene Aura Halo
            if (isRefreshing) {
                drawArc(
                    color = color.copy(alpha = 0.45f),
                    startAngle = auraSpin,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(center.x - scaleRadius * 0.92f, center.y - scaleRadius * 0.92f),
                    size = Size(scaleRadius * 1.84f, scaleRadius * 1.84f),
                    style = Stroke(width = strokeWidth * 0.65f, cap = StrokeCap.Round)
                )
            } else if (p > 0.4f) {
                val auraSweep = ((p - 0.4f) / 0.6f) * 360f
                drawArc(
                    color = color.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = auraSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - scaleRadius * 0.92f, center.y - scaleRadius * 0.92f),
                    size = Size(scaleRadius * 1.84f, scaleRadius * 1.84f),
                    style = Stroke(width = strokeWidth * 0.65f, cap = StrokeCap.Round)
                )
            }

            // 2. Lotus Petals Beneath
            val petalBaseY = center.y + scaleRadius * 0.42f
            val petalBloom = p * scaleRadius * 0.48f

            // Center Petal
            val centerPetal = Path().apply {
                moveTo(center.x, petalBaseY + scaleRadius * 0.45f)
                quadraticTo(center.x - scaleRadius * 0.22f, petalBaseY + scaleRadius * 0.15f, center.x, petalBaseY - petalBloom * 0.4f)
                quadraticTo(center.x + scaleRadius * 0.22f, petalBaseY + scaleRadius * 0.15f, center.x, petalBaseY + scaleRadius * 0.45f)
            }
            drawPath(centerPetal, color = color.copy(alpha = 0.85f), style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Round))

            // Left & Right Petals
            listOf(-1f, 1f).forEach { side ->
                val sidePetal = Path().apply {
                    moveTo(center.x, petalBaseY + scaleRadius * 0.45f)
                    cubicTo(
                        center.x + (side * scaleRadius * 0.55f * p), petalBaseY + scaleRadius * 0.35f,
                        center.x + (side * scaleRadius * 0.72f * p), petalBaseY + scaleRadius * 0.05f,
                        center.x + (side * scaleRadius * 0.62f * p), petalBaseY - petalBloom * 0.25f
                    )
                    quadraticTo(center.x + (side * scaleRadius * 0.22f), petalBaseY + scaleRadius * 0.2f, center.x, petalBaseY + scaleRadius * 0.45f)
                }
                drawPath(sidePetal, color = color.copy(alpha = 0.75f), style = Stroke(strokeWidth * 0.8f, cap = StrokeCap.Round))
            }

            // 3. Meditating Figure (Head & Shoulders)
            val headRadius = scaleRadius * 0.22f
            val headCenter = Offset(center.x, center.y - scaleRadius * 0.42f)
            drawCircle(color, radius = headRadius, center = headCenter)

            // Torso, Arms in Meditation Mudra, and Crossed Legs
            val bodyPath = Path().apply {
                val neckY = headCenter.y + headRadius + 2f
                val shoulderY = neckY + scaleRadius * 0.12f
                val lapY = center.y + scaleRadius * 0.28f
                val kneeSpan = scaleRadius * 0.68f

                // Neck to Left Shoulder
                moveTo(center.x, neckY)
                lineTo(center.x - scaleRadius * 0.38f, shoulderY)
                // Left arm to left knee resting in mudra
                lineTo(center.x - kneeSpan, lapY)
                // Left knee to crossed lap center
                lineTo(center.x, lapY + scaleRadius * 0.08f)
                // Right knee
                lineTo(center.x + kneeSpan, lapY)
                // Right arm to right shoulder
                lineTo(center.x + scaleRadius * 0.38f, shoulderY)
                lineTo(center.x, neckY)
                close()
            }
            drawPath(bodyPath, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Chin Mudra hand points
            drawCircle(color, radius = 2.dp.toPx(), center = Offset(center.x - scaleRadius * 0.55f, center.y + scaleRadius * 0.24f))
            drawCircle(color, radius = 2.dp.toPx(), center = Offset(center.x + scaleRadius * 0.55f, center.y + scaleRadius * 0.24f))
        }
    }
}
