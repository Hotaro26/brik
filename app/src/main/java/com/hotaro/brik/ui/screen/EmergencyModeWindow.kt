package com.hotaro.brik.ui.screen

import android.view.HapticFeedbackConstants
import android.widget.Toast
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
import com.hotaro.brik.ui.component.BrikSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotaro.brik.R
import com.hotaro.brik.ui.component.SetEmergencyKeyDialog
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.VerifyEmergencyKeyDialog
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import com.hotaro.brik.util.EmergencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyModeWindow(
    onBack: () -> Unit,
    keyViewModel: KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else colorScheme.surface

    val isEmergencyActive by EmergencyManager.isEmergencyModeActive.collectAsState()
    val hasEmergencyKey by EmergencyManager.hasEmergencyKey.collectAsState()
    val autoDeactivateOnLock by EmergencyManager.autoDeactivateOnScreenLock.collectAsState()
    val allowCommonKeyDpm by EmergencyManager.allowCommonKeyDpm.collectAsState()
    val savedKeys by keyViewModel.savedKeys.collectAsState()

    var showSetKeyDialog by remember { mutableStateOf(false) }
    var showVerifyKeyDialog by remember { mutableStateOf(false) }
    var isChangingKey by remember { mutableStateOf(false) }

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

                        BrikSwitch(
                            checked = isEmergencyActive,
                            onCheckedChange = { active ->
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                if (active) {
                                    if (!hasEmergencyKey) {
                                        showSetKeyDialog = true
                                    } else {
                                        showVerifyKeyDialog = true
                                    }
                                } else {
                                    EmergencyManager.setEmergencyModeActive(context, false)
                                    Toast.makeText(context, "Emergency Mode deactivated", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorScheme.onError,
                                checkedTrackColor = colorScheme.error
                            )
                        )
                    }
                }
            }

            // --- Section: Security Key ---
            TomatoSectionHeader(title = "SECURITY KEY")
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isEmergencyCommonKey = com.hotaro.brik.util.CommonKeyManager.isFollowingAndReady(context, "emergency_key")
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
                                color = if (hasEmergencyKey || isEmergencyCommonKey) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.password_fill0),
                                        contentDescription = null,
                                        tint = if (hasEmergencyKey || isEmergencyCommonKey) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isEmergencyCommonKey) "Common Key Configured" else if (hasEmergencyKey) "Emergency Key Configured" else "No Emergency Key Set",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEmergencyCommonKey) "Common Key configured for this" else if (hasEmergencyKey) "Required to activate emergency mode" else "Must configure a key before activating",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!isEmergencyCommonKey) {
                            Button(
                                onClick = {
                                    if (!hasEmergencyKey) {
                                        showSetKeyDialog = true
                                    } else {
                                        isChangingKey = true
                                        showVerifyKeyDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (hasEmergencyKey) "Change" else "Set Key", style = typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // --- Section: Self-Deactivation on Screen Lock ---
            TomatoSectionHeader(title = "SELF-DEACTIVATION")
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
                                color = colorScheme.secondaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.screen_lock_portrait_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Deactivate on Screen Lock",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Disable emergency mode when screen locks",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        BrikSwitch(
                            checked = autoDeactivateOnLock,
                            onCheckedChange = { enabled ->
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                EmergencyManager.setAutoDeactivateOnScreenLock(context, enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "Self-deactivation enabled" else "Self-deactivation disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    Text(
                        text = "When enabled, Emergency Mode automatically turns off whenever you lock your phone or the screen turns off. Entering your emergency key will be required again on the next activation.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Section: Common Key & DPM Override ---
            TomatoSectionHeader(title = "COMMON KEY & DPM OVERRIDE")
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
                                color = colorScheme.secondaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.shield_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Common Key & Disable DPM",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Unlock emergency mode & disable DPM with common key",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        BrikSwitch(
                            checked = allowCommonKeyDpm,
                            onCheckedChange = { enabled ->
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                EmergencyManager.setAllowCommonKeyDpm(context, enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "Common key & DPM override enabled" else "Common key & DPM override disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    Text(
                        text = "When enabled, any configured protection key can unlock emergency mode, and Device Owner (DPM) can be deactivated directly during an active emergency session.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    if (isEmergencyActive && allowCommonKeyDpm) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                val res = com.hotaro.brik.policy.PolicyBridgeManager.clearDeviceOwner(context)
                                keyViewModel.removeDpmUninstallLock()
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Device Owner (DPM) disabled successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "DPM status updated", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.delete_fill0),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Disable Device Owner (DPM) Now",
                                color = colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                        text = "In the Keys & Analytics window, pull down the page 3 times within 5 seconds to instantly trigger Emergency Mode. You will be prompted to verify your emergency key.",
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
                        if (!hasEmergencyKey) {
                            showSetKeyDialog = true
                        } else {
                            showVerifyKeyDialog = true
                        }
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

    if (showSetKeyDialog) {
        SetEmergencyKeyDialog(
            savedKeys = savedKeys,
            onDismiss = {
                showSetKeyDialog = false
                isChangingKey = false
            },
            onKeySet = {
                showSetKeyDialog = false
                isChangingKey = false
            }
        )
    }

    if (showVerifyKeyDialog) {
        VerifyEmergencyKeyDialog(
            onDismiss = {
                showVerifyKeyDialog = false
                isChangingKey = false
            },
            onVerified = {
                showVerifyKeyDialog = false
                if (isChangingKey) {
                    isChangingKey = false
                    showSetKeyDialog = true
                }
            }
        )
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
