package com.hotaro.brik.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.policy.BridgeStatus
import com.hotaro.brik.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeSetupSheet(
    initialTab: Int = 0,
    bridgeStatus: BridgeStatus,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var isRunningShizukuDpm by remember { mutableStateOf(false) }
    var shizukuDpmResult by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dpmCommand = "adb shell dpm set-device-owner com.hotaro.brik/.admin.BrikAdminReceiver"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Privileged Setup",
                        style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Choose your setup method for app blocking",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Method Selector Pills
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("ADB", "Shizuku", "Dhizuku")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = { selectedTab = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLowest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            when (selectedTab) {
                // --- TAB 0: ADB ---
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Device Owner (ADB)",
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (bridgeStatus.isDeviceOwner) colorScheme.primaryContainer else colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = if (bridgeStatus.isDeviceOwner) "Active" else "Not Provisioned",
                                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (bridgeStatus.isDeviceOwner) colorScheme.onPrimaryContainer else colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Device Owner gives Brik highest privilege to suspend and conceal apps without root. Requires connecting to a PC once.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Text(
                            text = "Setup Instructions:",
                            style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )

                        val steps = listOf(
                            "1. Enable Developer Options on your phone (tap Build Number 7 times).",
                            "2. Enable USB Debugging in Developer Options.",
                            "3. Connect your phone to your PC via USB cable.",
                            "4. If Android gives 'accounts exist' error, temporarily remove accounts in Settings > Accounts.",
                            "5. Run the following command in terminal or PowerShell:"
                        )

                        steps.forEach { step ->
                            Text(
                                text = step,
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        // Command Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceContainerLowest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = dpmCommand,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = colorScheme.primary,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", dpmCommand))
                                    Toast.makeText(context, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy_fill0),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Copy Command", fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { settingsViewModel.refreshStatus() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Verify")
                            }
                        }
                    }
                }

                // --- TAB 1: SHIZUKU ---
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Shizuku Bridge",
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (bridgeStatus.isShizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                                    ) {
                                        Text(
                                            text = if (bridgeStatus.isShizukuGranted) "Granted" else if (bridgeStatus.isShizukuRunning) "Permission Needed" else "Stopped",
                                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (bridgeStatus.isShizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Shizuku provides elevated shell capabilities without root. You can also use Shizuku to provision Device Owner directly on your device without a PC!",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Shizuku Status Actions
                        if (!bridgeStatus.isShizukuRunning) {
                            Text(
                                text = "Shizuku service is not running. Start Shizuku via Wireless Debugging or root first.",
                                style = typography.bodySmall,
                                color = colorScheme.error
                            )

                            Button(
                                onClick = {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    } else {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
                                        context.startActivity(browserIntent)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Shizuku App", fontWeight = FontWeight.Bold)
                            }
                        } else if (!bridgeStatus.isShizukuGranted) {
                            Text(
                                text = "Shizuku service is running. Tap below to grant authorization to Brik.",
                                style = typography.bodySmall,
                                color = colorScheme.primary
                            )

                            Button(
                                onClick = { settingsViewModel.requestShizukuPermission() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Shizuku Permission", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Shizuku is granted!
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Shizuku is Connected",
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Brik can execute shell-level suspension directly.",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }

                            if (!bridgeStatus.isDeviceOwner) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "One-Tap Device Owner Activation:",
                                    style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Use Shizuku shell to set Brik as Device Owner without touching a computer.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        isRunningShizukuDpm = true
                                        shizukuDpmResult = null
                                        settingsViewModel.setDeviceOwnerViaShizuku { success, msg ->
                                            isRunningShizukuDpm = false
                                            shizukuDpmResult = msg
                                            if (success) {
                                                Toast.makeText(context, "Device Owner activated!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed: $msg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    enabled = !isRunningShizukuDpm,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isRunningShizukuDpm) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("Activate Device Owner via Shizuku", fontWeight = FontWeight.Bold)
                                }

                                if (shizukuDpmResult != null) {
                                    Text(
                                        text = shizukuDpmResult!!,
                                        style = typography.bodySmall,
                                        color = if (bridgeStatus.isDeviceOwner) colorScheme.primary else colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // --- TAB 2: DHIZUKU ---
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Dhizuku Bridge",
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (bridgeStatus.isDhizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                                    ) {
                                        Text(
                                            text = if (bridgeStatus.isDhizukuGranted) "Granted" else if (bridgeStatus.isDhizukuInstalled) "Permission Needed" else "Not Installed",
                                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (bridgeStatus.isDhizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Dhizuku shares Device Owner APIs with other apps. Brik uses Dhizuku to suspend packages, block uninstallation, and conceal apps.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        if (!bridgeStatus.isDhizukuInstalled) {
                            Text(
                                text = "Dhizuku is not installed on this device.",
                                style = typography.bodySmall,
                                color = colorScheme.error
                            )

                            Button(
                                onClick = {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/iamr0s/Dhizuku"))
                                    context.startActivity(browserIntent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Get Dhizuku", fontWeight = FontWeight.Bold)
                            }
                        } else if (!bridgeStatus.isDhizukuGranted) {
                            Text(
                                text = "Dhizuku is installed. Tap below to request permission for Brik.",
                                style = typography.bodySmall,
                                color = colorScheme.primary
                            )

                            Button(
                                onClick = { settingsViewModel.requestDhizukuPermission() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Dhizuku Permission", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Dhizuku Bridge Operational",
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "App suspension, package concealment, and anti-uninstall enforcement are ready.",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
