package com.hotaro.brik.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val isDark = colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else colorScheme.surface

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "About",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Application info & open source details",
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color.Black else colorScheme.surfaceContainerLowest,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.ic_brik),
                                contentDescription = "Brik Logo",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Brik",
                        style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )

                    Text(
                        text = "Version 1.0.3 (Build 4)",
                        style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Free & Open Source Software",
                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "A zero-compromise, open-source focus & device policy controller built for Android.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // Section 1: REPOSITORY & PROJECT
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "REPOSITORY & CODE")

                TomatoSettingsItem(
                    title = "GitHub Repository",
                    subtitle = "Hotaro26/brik • Source code, issues & releases",
                    leadingIcon = R.drawable.tools_fill0,
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
                    shape = segmentedItemShape(0, 2),
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hotaro26/brik"))
                        context.startActivity(browserIntent)
                    }
                )

                TomatoSettingsItem(
                    title = "Copy Repository URL",
                    subtitle = "https://github.com/Hotaro26/brik",
                    leadingIcon = R.drawable.content_copy_fill0,
                    iconContainerColor = colorScheme.surfaceContainerLowest,
                    iconTint = colorScheme.onSurfaceVariant,
                    shape = segmentedItemShape(1, 2),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Brik Repo", "https://github.com/Hotaro26/brik"))
                        Toast.makeText(context, "Repository URL copied!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Section 2: PRIVACY POLICY
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "PRIVACY POLICY")

                TomatoSettingsItem(
                    title = "100% Offline & Private",
                    subtitle = "Brik contains no ads, analytics, tracking SDKs, or cloud telemetry. All locks, bags, and routines are stored locally on your device.",
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

            // Section 3: DEVELOPER
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "DEVELOPER")

                TomatoSettingsItem(
                    title = "Hotaro",
                    subtitle = "github.com/Hotaro26 • Tap to view profile",
                    leadingIcon = R.drawable.mode_fill0,
                    iconContainerColor = colorScheme.secondaryContainer,
                    iconTint = colorScheme.onSecondaryContainer,
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward_big),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    shape = segmentedItemShape(0, 1),
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hotaro26"))
                        context.startActivity(browserIntent)
                    }
                )
            }

            // Section 4: ENVIRONMENT & DEVICE
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
                text = "Built with Kotlin & Jetpack Compose\nMaterial You Expressive Design",
                style = typography.labelSmall,
                color = colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
