package com.hotaro.brik.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.hotaro.brik.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun segmentedItemShape(index: Int, total: Int): RoundedCornerShape {
    val cornerLarge = 22.dp
    val cornerSmall = 4.dp
    return when {
        total <= 1 -> RoundedCornerShape(cornerLarge)
        index == 0 -> RoundedCornerShape(
            topStart = cornerLarge,
            topEnd = cornerLarge,
            bottomStart = cornerSmall,
            bottomEnd = cornerSmall
        )
        index == total - 1 -> RoundedCornerShape(
            topStart = cornerSmall,
            topEnd = cornerSmall,
            bottomStart = cornerLarge,
            bottomEnd = cornerLarge
        )
        else -> RoundedCornerShape(cornerSmall)
    }
}

@Composable
fun TomatoSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 6.dp, bottom = 4.dp)
    )
}

@Composable
fun TomatoSettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    iconSize: Dp = 22.dp,
    trailingContent: @Composable (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val morphProgress by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardCornerMorph"
    )

    val morphedShape = remember(shape, morphProgress) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val pillRadius = size.height / 2f
                val baseTopStart = shape.topStart.toPx(size, density)
                val baseTopEnd = shape.topEnd.toPx(size, density)
                val baseBottomEnd = shape.bottomEnd.toPx(size, density)
                val baseBottomStart = shape.bottomStart.toPx(size, density)

                val ts = baseTopStart + (pillRadius - baseTopStart) * morphProgress
                val te = baseTopEnd + (pillRadius - baseTopEnd) * morphProgress
                val be = baseBottomEnd + (pillRadius - baseBottomEnd) * morphProgress
                val bs = baseBottomStart + (pillRadius - baseBottomStart) * morphProgress

                val rrect = RoundRect(
                    rect = Rect(Offset.Zero, size),
                    topLeft = CornerRadius(ts),
                    topRight = CornerRadius(te),
                    bottomRight = CornerRadius(be),
                    bottomLeft = CornerRadius(bs)
                )
                return Outline.Rounded(rrect)
            }
        }
    }

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = morphedShape,
        color = containerColor,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconContainerColor,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(leadingIcon),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

/**
 * Material 3 Expressive Switch with animated checkmark icon on checked state,
 * compact outline track on unchecked state, and seamless primary accent theming.
 */
object BrikSwitchDefaults {
    val IconSize: Dp = SwitchDefaults.IconSize

    @Composable
    fun colors(
        checkedThumbColor: Color = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
        checkedBorderColor: Color = Color.Transparent,
        checkedIconColor: Color = checkedTrackColor,
        uncheckedThumbColor: Color = MaterialTheme.colorScheme.outline,
        uncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
        uncheckedBorderColor: Color = MaterialTheme.colorScheme.outline,
        disabledCheckedThumbColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        disabledCheckedTrackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        disabledUncheckedThumbColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledUncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.12f),
        disabledUncheckedBorderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    ): SwitchColors = SwitchDefaults.colors(
        checkedThumbColor = checkedThumbColor,
        checkedTrackColor = checkedTrackColor,
        checkedBorderColor = checkedBorderColor,
        checkedIconColor = checkedIconColor,
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedTrackColor = uncheckedTrackColor,
        uncheckedBorderColor = uncheckedBorderColor,
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor,
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor,
        disabledUncheckedBorderColor = disabledUncheckedBorderColor
    )
}

typealias TomatoSwitchDefaults = BrikSwitchDefaults

@Composable
fun BrikSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = if (checked) {
        {
            Icon(
                painter = painterResource(R.drawable.check_fill0),
                contentDescription = null,
                modifier = Modifier.size(BrikSwitchDefaults.IconSize)
            )
        }
    } else null,
    colors: SwitchColors = BrikSwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

@Composable
fun TomatoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = BrikSwitch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled
)

