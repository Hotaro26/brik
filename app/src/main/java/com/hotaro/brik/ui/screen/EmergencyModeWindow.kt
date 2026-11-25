package com.hotaro.brik.ui.screen

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.util.EmergencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyModeWindow(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else colorScheme.surface
    val isEmergencyActive by EmergencyManager.isEmergencyModeActive.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Emergency Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.3).sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Override controls & lockdown bypass",
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
                    containerColor = appBackground
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = appBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Live Status Card ---
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isEmergencyActive) colorScheme.errorContainer.copy(alpha = 0.45f) else colorScheme.surfaceContainerHigh)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isEmergencyActive) colorScheme.error else colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.shield_fill0),
                                        contentDescription = null,
                                        tint = if (isEmergencyActive) colorScheme.onError else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isEmergencyActive) "Emergency Mode ACTIVE" else "Emergency Mode Inactive",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isEmergencyActive) colorScheme.error else colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEmergencyActive) "Override active - saved keys revealed" else "Standard lockdown rules enforced",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isEmergencyActive,
                            onCheckedChange = { active ->
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                EmergencyManager.setEmergencyModeActive(context, active)
                                val msg = if (active) "Emergency Mode activated" else "Emergency Mode deactivated"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorScheme.onError,
                                checkedTrackColor = colorScheme.error
                            )
                        )
                    }
                }
            }

            // --- Section: Quick Trigger Gesture ---
            TomatoSectionHeader(title = "ACTIVATION TRIGGER")
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.key_fill0),
                                    contentDescription = null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "3x Pull-Down in Keys & Analytics",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "In the Keys & Analytics window, pull down the page 3 times within 5 seconds to instantly trigger Emergency Mode. This gesture works even during strict lockdowns.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Section: What Emergency Mode Does ---
            TomatoSectionHeader(title = "EMERGENCY PRIVILEGES")
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    EmergencyFeatureRow(
                        title = "Reveal Saved QR Codes",
                        description = "Saved key QR codes and token strings become visible and copyable in Keys & Analytics.",
                        iconRes = R.drawable.qr_code_fill0
                    )
                    EmergencyFeatureRow(
                        title = "Emergency Pill on Bags",
                        description = "All running locked bags display an EMERGENCY badge so you can easily identify them.",
                        iconRes = R.drawable.backpack_fill0
                    )
                    EmergencyFeatureRow(
                        title = "Fail-Safe Recovery",
                        description = "Allows you to inspect or copy your backup key if you urgently need access to blocked apps.",
                        iconRes = R.drawable.shield_fill0
                    )
                }
            }

            // --- Action Button ---
            Spacer(Modifier.height(8.dp))
            if (isEmergencyActive) {
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        EmergencyManager.setEmergencyModeActive(context, false)
                        Toast.makeText(context, "Emergency Mode deactivated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Deactivate Emergency Mode",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onError
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        EmergencyManager.setEmergencyModeActive(context, true)
                        Toast.makeText(context, "Emergency Mode activated", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Activate Emergency Mode Now",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyFeatureRow(
    title: String,
    description: String,
    iconRes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = colorScheme.surfaceContainerLowest,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface
            )
            Text(
                text = description,
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
