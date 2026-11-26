package com.hotaro.brik.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RealFillBatteryIcon(
    batteryLevel: Int,
    isCharging: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 22.dp, height = 11.dp)) {
        val strokeWidth = 1.3.dp.toPx()
        val cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
        val bodyWidth = size.width - 2.5.dp.toPx()
        val bodyHeight = size.height

        // Outer battery shell
        drawRoundRect(
            color = tint,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(bodyWidth - strokeWidth, bodyHeight - strokeWidth),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        // Positive terminal cap on the right
        val capWidth = 1.5.dp.toPx()
        val capHeight = bodyHeight * 0.44f
        val capTop = (bodyHeight - capHeight) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyWidth, capTop),
            size = Size(capWidth, capHeight),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )

        // Inner battery fill
        val innerPadding = strokeWidth + 1.2.dp.toPx()
        val maxFillWidth = (bodyWidth - innerPadding * 2f).coerceAtLeast(0f)
        val fillHeight = (bodyHeight - innerPadding * 2f).coerceAtLeast(0f)
        val levelRatio = batteryLevel.coerceIn(0, 100) / 100f
        val fillWidth = (maxFillWidth * levelRatio).coerceAtLeast(0f)

        if (fillWidth > 0f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(innerPadding, innerPadding),
                size = Size(fillWidth, fillHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }

        // Charging lightning bolt indicator
        if (isCharging) {
            val cx = bodyWidth / 2f
            val cy = bodyHeight / 2f
            val boltPath = Path().apply {
                moveTo(cx + 0.8.dp.toPx(), cy - 4.dp.toPx())
                lineTo(cx - 2.8.dp.toPx(), cy + 0.3.dp.toPx())
                lineTo(cx - 0.5.dp.toPx(), cy + 0.3.dp.toPx())
                lineTo(cx - 0.8.dp.toPx(), cy + 4.dp.toPx())
                lineTo(cx + 2.8.dp.toPx(), cy - 0.3.dp.toPx())
                lineTo(cx + 0.5.dp.toPx(), cy - 0.3.dp.toPx())
                close()
            }
            val boltColor = if (fillWidth > cx) Color.Black else tint
            drawPath(boltPath, color = boltColor)
        }
    }
}
