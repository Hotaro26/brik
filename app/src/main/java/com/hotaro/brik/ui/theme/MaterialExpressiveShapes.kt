package com.hotaro.brik.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Android 16 / Material 3 Expressive shapes catalog.
 * Represents diverse organic and geometric shapes used across the M3 Expressive design system.
 */
enum class ExpressiveShapeType(val displayName: String) {
    SUNNY("Sunny"),
    VERY_SUNNY("Very Sunny"),
    COOKIE_4("4-Sided Cookie"),
    COOKIE_6("6-Sided Cookie"),
    COOKIE_9("9-Sided Cookie"),
    COOKIE_12("12-Sided Cookie"),
    CLOVER_4("4-Leaf Clover"),
    CLOVER_8("8-Leaf Clover"),
    FLOWER("Flower"),
    SOFT_BURST("Soft Burst"),
    BURST("Burst"),
    BOOM("Boom"),
    PUFFY("Puffy"),
    PUFFY_DIAMOND("Puffy Diamond"),
    HEART("Heart"),
    DIAMOND("Diamond"),
    GEM("Gem"),
    PENTAGON("Pentagon"),
    ARCH("Arch"),
    SEMICIRCLE("Semicircle"),
    OVAL("Oval"),
    PILL("Pill"),
    TRIANGLE("Triangle"),
    FAN("Fan"),
    SLANTED("Slanted"),
    BUN("Bun"),
    CIRCLE("Circle");

    companion object {
        fun fromKey(key: String): ExpressiveShapeType {
            return entries.find { it.name.equals(key, ignoreCase = true) } ?: SUNNY
        }
    }
}

/**
 * Jetpack Compose Shape rendering any Material 3 Expressive shape with smooth Bezier/path curves.
 */
class MaterialExpressiveShape(val type: ExpressiveShapeType) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = createExpressivePath(type, size)
        return Outline.Generic(path)
    }
}

object MaterialExpressiveShapes {
    val allShapes: List<ExpressiveShapeType> = ExpressiveShapeType.entries

    // Curated dynamic expressive shapes for settings icon backgrounds
    val dynamicShapes: List<ExpressiveShapeType> = listOf(
        ExpressiveShapeType.SUNNY,
        ExpressiveShapeType.VERY_SUNNY,
        ExpressiveShapeType.COOKIE_4,
        ExpressiveShapeType.COOKIE_6,
        ExpressiveShapeType.COOKIE_9,
        ExpressiveShapeType.CLOVER_4,
        ExpressiveShapeType.CLOVER_8,
        ExpressiveShapeType.FLOWER,
        ExpressiveShapeType.SOFT_BURST,
        ExpressiveShapeType.BURST,
        ExpressiveShapeType.PUFFY,
        ExpressiveShapeType.PUFFY_DIAMOND,
        ExpressiveShapeType.HEART,
        ExpressiveShapeType.DIAMOND,
        ExpressiveShapeType.GEM,
        ExpressiveShapeType.ARCH,
        ExpressiveShapeType.PENTAGON,
        ExpressiveShapeType.TRIANGLE,
        ExpressiveShapeType.SLANTED,
        ExpressiveShapeType.BUN
    )

    fun getShapeFor(seed: String, windowSeed: Int, configuredType: String = "random"): MaterialExpressiveShape {
        if (configuredType != "random") {
            val matched = ExpressiveShapeType.entries.find { it.name.equals(configuredType, ignoreCase = true) }
            if (matched != null) return MaterialExpressiveShape(matched)
        }
        val pool = dynamicShapes
        val hash = abs(seed.hashCode() + (windowSeed * 31))
        val selected = pool[hash % pool.size]
        return MaterialExpressiveShape(selected)
    }
}

/**
 * Builds the exact Path for a given Material 3 Expressive shape within the supplied bounds.
 */
fun createExpressivePath(type: ExpressiveShapeType, size: Size): Path {
    val path = Path()
    val width = size.width
    val height = size.height
    val cx = width / 2f
    val cy = height / 2f
    val radius = min(width, height) / 2f * 0.96f

    when (type) {
        ExpressiveShapeType.CIRCLE -> {
            path.addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
        }

        ExpressiveShapeType.SUNNY -> {
            // 8 rounded petals
            samplePolarShape(path, cx, cy, radius, lobes = 8, depth = 0.16f, stepCount = 96)
        }

        ExpressiveShapeType.VERY_SUNNY -> {
            // 12 rounded petals
            samplePolarShape(path, cx, cy, radius, lobes = 12, depth = 0.14f, stepCount = 120)
        }

        ExpressiveShapeType.COOKIE_4 -> {
            // Squircle with 4 indented scallops
            samplePolarShape(path, cx, cy, radius, lobes = 4, depth = -0.15f, stepCount = 96)
        }

        ExpressiveShapeType.COOKIE_6 -> {
            samplePolarShape(path, cx, cy, radius, lobes = 6, depth = -0.13f, stepCount = 96)
        }

        ExpressiveShapeType.COOKIE_9 -> {
            samplePolarShape(path, cx, cy, radius, lobes = 9, depth = -0.12f, stepCount = 108)
        }

        ExpressiveShapeType.COOKIE_12 -> {
            samplePolarShape(path, cx, cy, radius, lobes = 12, depth = -0.11f, stepCount = 120)
        }

        ExpressiveShapeType.CLOVER_4 -> {
            val steps = 96
            var first = true
            for (i in 0 until steps) {
                val theta = (i * 2 * PI / steps).toFloat()
                val r = radius * (0.72f + 0.28f * abs(cos(2 * theta)))
                val px = cx + r * cos(theta)
                val py = cy + r * sin(theta)
                if (first) {
                    path.moveTo(px, py)
                    first = false
                } else {
                    path.lineTo(px, py)
                }
            }
            path.close()
        }

        ExpressiveShapeType.CLOVER_8 -> {
            val steps = 96
            var first = true
            for (i in 0 until steps) {
                val theta = (i * 2 * PI / steps).toFloat()
                val r = radius * (0.75f + 0.25f * abs(cos(4 * theta)))
                val px = cx + r * cos(theta)
                val py = cy + r * sin(theta)
                if (first) {
                    path.moveTo(px, py)
                    first = false
                } else {
                    path.lineTo(px, py)
                }
            }
            path.close()
        }

        ExpressiveShapeType.FLOWER -> {
            samplePolarShape(path, cx, cy, radius, lobes = 10, depth = 0.18f, stepCount = 120)
        }

        ExpressiveShapeType.SOFT_BURST -> {
            samplePolarShape(path, cx, cy, radius, lobes = 8, depth = 0.22f, stepCount = 96)
        }

        ExpressiveShapeType.BURST -> {
            val points = 8
            val rOuter = radius
            val rInner = radius * 0.58f
            for (i in 0 until points * 2) {
                val theta = (i * PI / points).toFloat() - (PI / 2f).toFloat()
                val r = if (i % 2 == 0) rOuter else rInner
                val px = cx + r * cos(theta)
                val py = cy + r * sin(theta)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }

        ExpressiveShapeType.BOOM -> {
            val points = 16
            val rOuter = radius
            val rInner = radius * 0.62f
            for (i in 0 until points * 2) {
                val theta = (i * PI / points).toFloat() - (PI / 2f).toFloat()
                val r = if (i % 2 == 0) rOuter else rInner
                val px = cx + r * cos(theta)
                val py = cy + r * sin(theta)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }

        ExpressiveShapeType.PUFFY -> {
            samplePolarShape(path, cx, cy, radius, lobes = 5, depth = 0.18f, stepCount = 96)
        }

        ExpressiveShapeType.PUFFY_DIAMOND -> {
            samplePolarShape(path, cx, cy, radius, lobes = 4, depth = 0.20f, stepCount = 96)
        }

        ExpressiveShapeType.HEART -> {
            val steps = 90
            var first = true
            val scale = radius / 16.5f
            for (i in 0 until steps) {
                val t = (i * 2 * PI / steps).toFloat()
                val hx = 16f * sin(t) * sin(t) * sin(t)
                val hy = -(13f * cos(t) - 5f * cos(2f * t) - 2f * cos(3f * t) - cos(4f * t))
                val px = cx + hx * scale
                val py = cy + (hy + 2f) * scale
                if (first) {
                    path.moveTo(px, py)
                    first = false
                } else {
                    path.lineTo(px, py)
                }
            }
            path.close()
        }

        ExpressiveShapeType.DIAMOND -> {
            // Rotated square with rounded corners
            val cr = radius * 0.22f
            path.moveTo(cx, cy - radius + cr)
            path.quadraticTo(cx, cy - radius, cx + cr, cy - radius + cr)
            path.lineTo(cx + radius - cr, cy - cr)
            path.quadraticTo(cx + radius, cy, cx + radius - cr, cy + cr)
            path.lineTo(cx + cr, cy + radius - cr)
            path.quadraticTo(cx, cy + radius, cx - cr, cy + radius - cr)
            path.lineTo(cx - radius + cr, cy + cr)
            path.quadraticTo(cx - radius, cy, cx - radius + cr, cy - cr)
            path.lineTo(cx - cr, cy - radius + cr)
            path.quadraticTo(cx, cy - radius, cx, cy - radius + cr)
            path.close()
        }

        ExpressiveShapeType.GEM -> {
            // Hexagon with rounded corners
            val corners = 6
            val cornerRadius = radius * 0.2f
            for (i in 0 until corners) {
                val theta = (i * 2 * PI / corners).toFloat() - (PI / 6f).toFloat()
                val px = cx + (radius - cornerRadius) * cos(theta)
                val py = cy + (radius - cornerRadius) * sin(theta)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }

        ExpressiveShapeType.PENTAGON -> {
            // 5-sided polygon with rounded feel
            val corners = 5
            for (i in 0 until corners) {
                val theta = (i * 2 * PI / corners).toFloat() - (PI / 2f).toFloat()
                val px = cx + radius * cos(theta)
                val py = cy + radius * sin(theta)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }

        ExpressiveShapeType.ARCH -> {
            // Semicircular top, flat bottom with rounded bottom corners
            val bottomCorner = radius * 0.25f
            path.moveTo(cx - radius, cy)
            path.arcTo(
                Rect(cx - radius, cy - radius, cx + radius, cy + radius),
                180f,
                180f,
                false
            )
            path.lineTo(cx + radius, cy + radius - bottomCorner)
            path.quadraticTo(cx + radius, cy + radius, cx + radius - bottomCorner, cy + radius)
            path.lineTo(cx - radius + bottomCorner, cy + radius)
            path.quadraticTo(cx - radius, cy + radius, cx - radius, cy + radius - bottomCorner)
            path.close()
        }

        ExpressiveShapeType.SEMICIRCLE -> {
            path.moveTo(cx - radius, cy + radius * 0.5f)
            path.arcTo(
                Rect(cx - radius, cy - radius * 0.8f, cx + radius, cy + radius * 1.2f),
                180f,
                180f,
                false
            )
            path.lineTo(cx + radius, cy + radius * 0.5f)
            path.close()
        }

        ExpressiveShapeType.OVAL -> {
            path.addOval(Rect(cx - radius * 0.78f, cy - radius, cx + radius * 0.78f, cy + radius))
        }

        ExpressiveShapeType.PILL -> {
            path.addRoundRect(
                RoundRect(
                    left = cx - radius * 0.72f,
                    top = cy - radius,
                    right = cx + radius * 0.72f,
                    bottom = cy + radius,
                    radiusX = radius * 0.72f,
                    radiusY = radius * 0.72f
                )
            )
        }

        ExpressiveShapeType.TRIANGLE -> {
            // Equilateral triangle
            val topX = cx
            val topY = cy - radius
            val rightX = cx + radius * 0.95f
            val rightY = cy + radius * 0.75f
            val leftX = cx - radius * 0.95f
            val leftY = cy + radius * 0.75f
            path.moveTo(topX, topY)
            path.lineTo(rightX, rightY)
            path.lineTo(leftX, leftY)
            path.close()
        }

        ExpressiveShapeType.FAN -> {
            path.moveTo(cx - radius * 0.7f, cy + radius * 0.7f)
            path.lineTo(cx - radius * 0.7f, cy - radius * 0.7f)
            path.arcTo(
                Rect(cx - radius * 0.7f, cy - radius * 0.7f, cx + radius * 1.4f, cy + radius * 1.4f),
                180f,
                90f,
                false
            )
            path.lineTo(cx - radius * 0.7f, cy + radius * 0.7f)
            path.close()
        }

        ExpressiveShapeType.SLANTED -> {
            val skew = radius * 0.35f
            val r = radius * 0.28f
            path.moveTo(cx - radius + skew + r, cy - radius)
            path.lineTo(cx + radius + r, cy - radius)
            path.lineTo(cx + radius - skew - r, cy + radius)
            path.lineTo(cx - radius - r, cy + radius)
            path.close()
        }

        ExpressiveShapeType.BUN -> {
            // Two overlapping horizontal pills (bun shape)
            val bunHeight = radius * 0.85f
            path.addRoundRect(
                RoundRect(
                    left = cx - radius,
                    top = cy - radius,
                    right = cx + radius,
                    bottom = cy - radius + bunHeight,
                    radiusX = bunHeight / 2f,
                    radiusY = bunHeight / 2f
                )
            )
            path.addRoundRect(
                RoundRect(
                    left = cx - radius,
                    top = cy + radius - bunHeight,
                    right = cx + radius,
                    bottom = cy + radius,
                    radiusX = bunHeight / 2f,
                    radiusY = bunHeight / 2f
                )
            )
        }
    }

    return path
}

private fun samplePolarShape(
    path: Path,
    cx: Float,
    cy: Float,
    baseRadius: Float,
    lobes: Int,
    depth: Float,
    stepCount: Int
) {
    var first = true
    for (i in 0 until stepCount) {
        val theta = (i * 2 * PI / stepCount).toFloat()
        val r = baseRadius * ((1f - abs(depth)) + depth * cos(lobes * theta))
        val px = cx + r * cos(theta)
        val py = cy + r * sin(theta)
        if (first) {
            path.moveTo(px, py)
            first = false
        } else {
            path.lineTo(px, py)
        }
    }
    path.close()
}
