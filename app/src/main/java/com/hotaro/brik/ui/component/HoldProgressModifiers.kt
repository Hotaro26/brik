package com.hotaro.brik.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun Modifier.holdToProgress(
    durationMillis: Long = 5000L,
    enabled: Boolean = true,
    onProgressUpdate: (Float) -> Unit,
    onCompleted: () -> Unit,
    onTap: () -> Unit
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    var animJob by remember { mutableStateOf<Job?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(progress.value) {
        onProgressUpdate(progress.value)
    }

    return this.pointerInput(enabled) {
        if (!enabled) {
            detectTapGestures(onTap = { onTap() })
            return@pointerInput
        }
        detectTapGestures(
            onPress = {
                var completed = false
                val startTime = System.currentTimeMillis()
                animJob = coroutineScope.launch {
                    progress.snapTo(0f)
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = durationMillis.toInt(), easing = LinearEasing)
                    )
                    completed = true
                    onCompleted()
                }
                val released = tryAwaitRelease()
                val pressDuration = System.currentTimeMillis() - startTime
                if (!completed) {
                    animJob?.cancel()
                    coroutineScope.launch {
                        progress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        )
                    }
                    if (released && pressDuration < 400) {
                        onTap()
                    }
                }
            }
        )
    }
}

fun Modifier.drawBorderProgress(
    progress: Float,
    shape: Shape,
    color: Color,
    strokeWidth: Dp = 3.5.dp
): Modifier = this.drawWithContent {
    drawContent()
    if (progress > 0.005f) {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
        }
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, true)
        val length = pathMeasure.length
        val segmentPath = Path()
        pathMeasure.getSegment(0f, length * progress.coerceIn(0f, 1f), segmentPath, true)
        drawPath(
            path = segmentPath,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
