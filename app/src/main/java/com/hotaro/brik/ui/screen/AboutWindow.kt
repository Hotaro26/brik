package com.hotaro.brik.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.hotaro.brik.ui.theme.ExpressiveShapeType
import com.hotaro.brik.ui.theme.MaterialExpressiveShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.TomatoSettingsItem
import com.hotaro.brik.ui.component.segmentedItemShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutWindow(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else colorScheme.surface

    val appVersion = remember(context) {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val raw = pInfo.versionName ?: "1.0.3"
            if (raw.startsWith("v", ignoreCase = true)) raw else "v$raw"
        } catch (e: Exception) {
            "v1.0.3"
        }
    }

    val openUrl: (String) -> Unit = { url ->
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Throwable) {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "About",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.3).sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Brik information & developer profile",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp),
                        onClick = onBack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back_fill0),
                                contentDescription = "Back",
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = appBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // HERO MOMENT 1: APP HERO (M3 EXPRESSIVE)
            // ==========================================
            val infiniteTransition = rememberInfiniteTransition(label = "aboutCloverTransition")
            val cloverRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 20000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "aboutCloverRotation"
            )
            val cloverShape = remember { MaterialExpressiveShape(ExpressiveShapeType.CLOVER_8) }

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animating Material 3 Expressive 8-Leaf Clover Icon Container
                    Box(
                        modifier = Modifier.size(104.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationZ = cloverRotation }
                                .background(color = colorScheme.primaryContainer, shape = cloverShape)
                        )

                        Image(
                            painter = painterResource(R.drawable.ic_brik_monochrome),
                            contentDescription = "Brik Monochrome Icon",
                            colorFilter = ColorFilter.tint(colorScheme.primary),
                            modifier = Modifier.size(62.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Brik",
                        style = typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            letterSpacing = (-0.8).sp
                        ),
                        color = colorScheme.onSurface
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = appVersion,
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = CircleShape,
                        color = colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Free & Open Source Software",
                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "A zero-compromise, open-source focus & device policy controller built with modern Jetpack Compose & Material 3 Expressive.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // ==========================================
            // HERO MOMENT 2: HIGHLIGHTED DEVELOPER CARD
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "DEVELOPER")

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = colorScheme.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar from GitHub Profile
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.surfaceContainerLowest,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.avatar_hotaro),
                                    contentDescription = "Hotaro Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Hotaro",
                                        style = typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primary
                                    ) {
                                        Text(
                                            text = "CREATOR",
                                            style = typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = "@Hotaro26",
                                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.primary
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = "46 Repositories • 41 Followers",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Crafting minimalist, high-performance privacy utilities and system controllers for Android.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        // Social Icon-Only Buttons (Website, Pinterest, GitHub)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Website (aki101.in)
                            FilledTonalIconButton(
                                onClick = { openUrl("https://aki101.in") },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = colorScheme.secondaryContainer,
                                    contentColor = colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_globe),
                                    contentDescription = "Website (aki101.in)",
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // 2. Pinterest (hotaro26)
                            FilledTonalIconButton(
                                onClick = { openUrl("https://www.pinterest.com/hotaro26/") },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = colorScheme.tertiaryContainer,
                                    contentColor = colorScheme.onTertiaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pinterest),
                                    contentDescription = "Pinterest (hotaro26)",
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // 3. GitHub (Hotaro26)
                            FilledTonalIconButton(
                                onClick = { openUrl("https://github.com/Hotaro26") },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = colorScheme.primaryContainer,
                                    contentColor = colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_github),
                                    contentDescription = "GitHub (Hotaro26)",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION: REPOSITORY & CODE
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "REPOSITORY & CODE")

                TomatoSettingsItem(
                    title = "GitHub Repository",
                    subtitle = "Hotaro26/brik • Source code, issues & releases",
                    leadingIcon = R.drawable.ic_github,
                    iconContainerColor = colorScheme.primaryContainer,
                    iconTint = colorScheme.primary,
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward_big),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    shape = segmentedItemShape(0, 1),
                    onClick = { openUrl("https://github.com/Hotaro26/brik") }
                )
            }

            // ==========================================
            // SECTION: PRIVACY POLICY
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "PRIVACY POLICY")

                TomatoSettingsItem(
                    title = "100% Offline & Private",
                    subtitle = "Brik contains zero ads, analytics, tracking SDKs, or cloud telemetry. All locks, bags, and routines are stored locally on your device.",
                    leadingIcon = R.drawable.shield_fill0,
                    iconContainerColor = colorScheme.primaryContainer,
                    iconTint = colorScheme.primary,
                    shape = segmentedItemShape(0, 2)
                )

                TomatoSettingsItem(
                    title = "Device Policy Protection",
                    subtitle = "Enforcement is implemented via Android standard Device Policy Manager (DPM), Dhizuku, and Shizuku APIs.",
                    leadingIcon = R.drawable.screen_lock_portrait_fill0,
                    iconContainerColor = colorScheme.surfaceContainerLowest,
                    iconTint = colorScheme.onSurfaceVariant,
                    shape = segmentedItemShape(1, 2)
                )
            }

            // ==========================================
            // SECTION: SYSTEM & RUNTIME
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "SYSTEM & RUNTIME")

                TomatoSettingsItem(
                    title = "Operating System",
                    subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    leadingIcon = R.drawable.settings_filled,
                    iconContainerColor = colorScheme.surfaceContainerLowest,
                    iconTint = colorScheme.onSurfaceVariant,
                    shape = segmentedItemShape(0, 2)
                )

                TomatoSettingsItem(
                    title = "Device Hardware",
                    subtitle = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                    leadingIcon = R.drawable.apps_fill0,
                    iconContainerColor = colorScheme.surfaceContainerLowest,
                    iconTint = colorScheme.onSurfaceVariant,
                    shape = segmentedItemShape(1, 2)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Built with Kotlin & Jetpack Compose\nMaterial 3 Expressive UI",
                style = typography.labelSmall,
                color = colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
