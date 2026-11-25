package com.hotaro.brik.ui.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hotaro.brik.R
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.policy.BridgeType
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.viewmodel.SettingsViewModel
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import com.hotaro.brik.util.QrUtils
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToKeys: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
    toolsViewModel: ToolsViewModel = viewModel(),
    keyViewModel: com.hotaro.brik.ui.viewmodel.KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    val bridgeStatus by settingsViewModel.bridgeStatus.collectAsState()
    val suspendedPackages by settingsViewModel.suspendedPackages.collectAsState()
    val hiddenPackages by settingsViewModel.hiddenPackages.collectAsState()
    val isKioskActive by settingsViewModel.isKioskActive.collectAsState()
    val installedApps by toolsViewModel.installedApps.collectAsState()
    val bags by toolsViewModel.bags.collectAsState()
    val activeLocks by keyViewModel.activeLocks.collectAsState()
    val stats by keyViewModel.stats.collectAsState()

    var showBlockSheet by remember { mutableStateOf(false) }
    var showHideSheet by remember { mutableStateOf(false) }
    var showKioskSheet by remember { mutableStateOf(false) }
    var showSetUninstallKeyDialog by remember { mutableStateOf(false) }
    var showVerifyUninstallKeyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.refreshStatus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Tomato-style Header & Active Bridge Banner ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Settings",
                    style = typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Privileged Bridges & Enforcement",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = if (bridgeStatus.activeBridge != BridgeType.NONE) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = when (bridgeStatus.activeBridge) {
                        BridgeType.DEVICE_OWNER -> "Device Owner"
                        BridgeType.DHIZUKU -> "Dhizuku"
                        BridgeType.SHIZUKU -> "Shizuku"
                        BridgeType.ROOT -> "Root"
                        BridgeType.NONE -> "Unprivileged"
                    },
                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (bridgeStatus.activeBridge != BridgeType.NONE) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // --- SECTION 1: PRIVILEGED BRIDGES ---
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TomatoSectionHeader(title = "PRIVILEGED BRIDGES")

            val totalBridges = if (bridgeStatus.isDeviceOwner) 1 else 3

            // 1. Device Owner (DPM)
            TomatoSettingsItem(
                title = "Device Owner (DPM)",
                subtitle = if (bridgeStatus.isDeviceOwner) "Active & Full Control" else "Not Provisioned (Tap to copy ADB command)",
                leadingIcon = R.drawable.shield_fill0,
                iconContainerColor = if (bridgeStatus.isDeviceOwner) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                iconTint = if (bridgeStatus.isDeviceOwner) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                trailingContent = {
                    Surface(
                        shape = CircleShape,
                        color = if (bridgeStatus.isDeviceOwner) colorScheme.primaryContainer else colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (bridgeStatus.isDeviceOwner) "Active" else "Not Set",
                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (bridgeStatus.isDeviceOwner) colorScheme.onPrimaryContainer else colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                },
                shape = segmentedItemShape(0, totalBridges),
                onClick = {
                    if (!bridgeStatus.isDeviceOwner) {
                        val dpmCmd = "adb shell dpm set-device-owner com.hotaro.brik/.admin.BrikAdminReceiver"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", dpmCmd))
                        Toast.makeText(context, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // If DPM is not active, display Dhizuku and Shizuku
            if (!bridgeStatus.isDeviceOwner) {
                // 2. Dhizuku Bridge
                TomatoSettingsItem(
                    title = "Dhizuku Bridge",
                    subtitle = if (bridgeStatus.isDhizukuGranted) "Granted & Operational" else if (bridgeStatus.isDhizukuInstalled) "Permission Needed" else "Not Installed",
                    leadingIcon = R.drawable.key_fill0,
                    iconContainerColor = if (bridgeStatus.isDhizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                    iconTint = if (bridgeStatus.isDhizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    trailingContent = {
                        if (!bridgeStatus.isDhizukuGranted && bridgeStatus.isDhizukuInstalled) {
                            FilledTonalButton(
                                onClick = { settingsViewModel.requestDhizukuPermission() },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Grant", style = typography.labelSmall)
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (bridgeStatus.isDhizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                            ) {
                                Text(
                                    text = if (bridgeStatus.isDhizukuGranted) "Granted" else "Unavailable",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (bridgeStatus.isDhizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    shape = segmentedItemShape(1, totalBridges)
                )

                // 3. Shizuku Bridge
                TomatoSettingsItem(
                    title = "Shizuku Bridge",
                    subtitle = if (bridgeStatus.isShizukuGranted) "Granted & Running" else if (bridgeStatus.isShizukuRunning) "Permission Needed" else "Service Stopped",
                    leadingIcon = R.drawable.backpack_fill0,
                    iconContainerColor = if (bridgeStatus.isShizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                    iconTint = if (bridgeStatus.isShizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    trailingContent = {
                        if (bridgeStatus.isShizukuRunning && !bridgeStatus.isShizukuGranted) {
                            FilledTonalButton(
                                onClick = { settingsViewModel.requestShizukuPermission() },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Grant", style = typography.labelSmall)
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (bridgeStatus.isShizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                            ) {
                                Text(
                                    text = if (bridgeStatus.isShizukuGranted) "Running" else "Stopped",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (bridgeStatus.isShizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    shape = segmentedItemShape(2, totalBridges)
                )
            }
        }

        // --- SECTION 2: KEYS & SECURITY ---
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TomatoSectionHeader(title = "KEYS & SECURITY")

            TomatoSettingsItem(
                title = "Keys & Lock Analytics",
                subtitle = "${activeLocks.size} active bag keys • ${stats.totalLocks} sessions",
                leadingIcon = R.drawable.key_fill0,
                iconContainerColor = colorScheme.primaryContainer,
                iconTint = colorScheme.onPrimaryContainer,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = "Open Keys",
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(0, 1),
                onClick = onNavigateToKeys
            )
        }

        // --- SECTION 3: POLICY ENFORCEMENT ---
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TomatoSectionHeader(title = "POLICY ENFORCEMENT")

            // 1. App Blocking
            TomatoSettingsItem(
                title = "App Blocking (Suspension)",
                subtitle = if (suspendedPackages.isNotEmpty()) "${suspendedPackages.size} packages currently suspended" else "Suspend distracting apps or entire bags",
                leadingIcon = R.drawable.block_fill0,
                iconContainerColor = colorScheme.errorContainer,
                iconTint = colorScheme.onErrorContainer,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(0, 3),
                onClick = { showBlockSheet = true }
            )

            // 2. App Hiding
            TomatoSettingsItem(
                title = "App Hiding (Invisible)",
                subtitle = if (hiddenPackages.isNotEmpty()) "${hiddenPackages.size} apps hidden from launcher" else "Completely hide packages from system launcher",
                leadingIcon = R.drawable.visibility_off_fill0,
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
                shape = segmentedItemShape(1, 3),
                onClick = { showHideSheet = true }
            )

            // 3. Lock / Kiosk Mode
            TomatoSettingsItem(
                title = "Lock / Kiosk Mode",
                subtitle = if (isKioskActive) "Kiosk Active (App is pinned)" else "Pin app into single-task kiosk mode",
                leadingIcon = R.drawable.screen_lock_portrait_fill0,
                iconContainerColor = colorScheme.tertiaryContainer,
                iconTint = colorScheme.onTertiaryContainer,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(2, 3),
                onClick = { showKioskSheet = true }
            )
        }

        // --- SECTION 4: ADMINISTRATION ---
        if (bridgeStatus.isDeviceOwner) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "ADMINISTRATION")

                TomatoSettingsItem(
                    title = "Deactivate Device Owner",
                    subtitle = "Requires security key verification to allow uninstalling Brik",
                    leadingIcon = R.drawable.delete_fill0,
                    iconContainerColor = colorScheme.errorContainer,
                    iconTint = colorScheme.onErrorContainer,
                    trailingContent = {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Revoke",
                                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    },
                    shape = segmentedItemShape(0, 1),
                    onClick = {
                        val existing = keyViewModel.getDpmUninstallLock()
                        if (existing == null) {
                            showSetUninstallKeyDialog = true
                        } else {
                            showVerifyUninstallKeyDialog = true
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(130.dp))
    }

    // --- Step 1: Set Uninstall Key Dialog ---
    if (showSetUninstallKeyDialog) {
        var selectedKeyType by remember { mutableStateOf(KeyType.PASSWORD) }
        var password by remember { mutableStateOf("") }
        val generatedQrToken = remember { UUID.randomUUID().toString() }
        val qrBitmap = remember(generatedQrToken) {
            try {
                QrUtils.encodeToBitmap(generatedQrToken, 320)
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { showSetUninstallKeyDialog = false },
            title = {
                Text(
                    text = "Set Uninstall Security Key",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "To revoke Device Owner and uninstall Brik, you must set either a Password or QR Code key. This key will be tracked in your Keys section.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Key Type Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { selectedKeyType = KeyType.PASSWORD },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.password_fill0),
                                    contentDescription = null,
                                    tint = if (selectedKeyType == KeyType.PASSWORD) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Password",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedKeyType = KeyType.QR_CODE },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.qr_code_fill0),
                                    contentDescription = null,
                                    tint = if (selectedKeyType == KeyType.QR_CODE) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "QR Key",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (selectedKeyType == KeyType.PASSWORD) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Enter security password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Uninstall QR Key",
                                    modifier = Modifier
                                        .size(190.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(android.graphics.Color.WHITE.let { androidx.compose.ui.graphics.Color(it) })
                                        .padding(8.dp)
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Save or copy this QR token. You will need to scan or paste it to revoke Device Owner and uninstall Brik.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", generatedQrToken))
                                    Toast.makeText(context, "QR Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Copy Token Text", style = typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val secret = if (selectedKeyType == KeyType.PASSWORD) password.trim() else generatedQrToken
                        keyViewModel.setDpmUninstallLock(selectedKeyType, secret)
                        Toast.makeText(context, "Security key saved in Keys section!", Toast.LENGTH_SHORT).show()
                        showSetUninstallKeyDialog = false
                        showVerifyUninstallKeyDialog = true
                    },
                    enabled = selectedKeyType == KeyType.QR_CODE || password.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Key & Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetUninstallKeyDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Step 2: Verify Uninstall Key Dialog ---
    if (showVerifyUninstallKeyDialog) {
        val uninstallLock = keyViewModel.getDpmUninstallLock()
        var inputSecret by remember { mutableStateOf("") }
        var showCameraScanner by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showVerifyUninstallKeyDialog = false
                showCameraScanner = false
            },
            title = {
                Text(
                    text = "Verify Key to Deactivate",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uninstallLock?.keyType == KeyType.QR_CODE) {
                            "Scan your QR code key or enter the token to revoke Device Owner and uninstall Brik."
                        } else {
                            "Enter the password you set to revoke Device Owner and uninstall Brik."
                        },
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    if (uninstallLock?.keyType == KeyType.QR_CODE) {
                        if (showCameraScanner) {
                            QrScannerView(
                                onQrDetected = { scannedToken ->
                                    keyViewModel.verifyAndDeactivateDeviceOwner(context, scannedToken) { ok, err ->
                                        if (ok) {
                                            settingsViewModel.refreshStatus()
                                            Toast.makeText(context, "Device Owner deactivated! You can now uninstall Brik.", Toast.LENGTH_LONG).show()
                                            showVerifyUninstallKeyDialog = false
                                            showCameraScanner = false
                                        } else {
                                            errorMessage = err ?: "Invalid QR key"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        } else {
                            Button(
                                onClick = { showCameraScanner = true },
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.qr_code_fill0),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Scan with Camera")
                            }

                            Spacer(Modifier.height(12.dp))

                            Text("— or paste token text —", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputSecret,
                                onValueChange = {
                                    inputSecret = it
                                    errorMessage = null
                                },
                                placeholder = { Text("Paste QR token") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = inputSecret,
                            onValueChange = {
                                inputSecret = it
                                errorMessage = null
                            },
                            placeholder = { Text("Enter password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = typography.bodySmall,
                            color = colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (!showCameraScanner) {
                    Button(
                        onClick = {
                            keyViewModel.verifyAndDeactivateDeviceOwner(context, inputSecret.trim()) { ok, err ->
                                if (ok) {
                                    settingsViewModel.refreshStatus()
                                    Toast.makeText(context, "Device Owner deactivated! You can now uninstall Brik.", Toast.LENGTH_LONG).show()
                                    showVerifyUninstallKeyDialog = false
                                } else {
                                    errorMessage = err ?: "Incorrect password or key"
                                }
                            }
                        },
                        enabled = inputSecret.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Deactivate")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVerifyUninstallKeyDialog = false
                    showCameraScanner = false
                }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- APP BLOCKING SHEET ---
    if (showBlockSheet) {
        val blockSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBlockSheet = false },
            sheetState = blockSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "App Blocking",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Suspend bags or individual apps",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showBlockSheet = false }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bags Quick-Action
                if (bags.isNotEmpty()) {
                    Text(
                        text = "Suspend by Bag",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    bags.forEach { bag ->
                        val isBagSuspended = bag.packageNames.isNotEmpty() && bag.packageNames.all { suspendedPackages.contains(it) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = bag.name,
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${bag.packageNames.size} apps",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = isBagSuspended,
                                    onCheckedChange = { suspend ->
                                        settingsViewModel.suspendPackages(
                                            bag.packageNames.toList(),
                                            suspend
                                        ) { ok, err ->
                                            if (!ok) {
                                                Toast.makeText(context, err ?: "Operation failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    text = "Individual Apps",
                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        val isSuspended = suspendedPackages.contains(app.packageName)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberDrawablePainter(app.icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isSuspended,
                                    onCheckedChange = { suspend ->
                                        settingsViewModel.suspendPackages(
                                            listOf(app.packageName),
                                            suspend
                                        ) { ok, err ->
                                            if (!ok) {
                                                Toast.makeText(context, err ?: "Operation failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- APP HIDING SHEET ---
    if (showHideSheet) {
        val hideSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHideSheet = false },
            sheetState = hideSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "App Hiding",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Hide apps completely from the system",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showHideSheet = false }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        val isHidden = hiddenPackages.contains(app.packageName)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberDrawablePainter(app.icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isHidden,
                                    onCheckedChange = { hide ->
                                        settingsViewModel.toggleAppHidden(
                                            app.packageName,
                                            hide
                                        ) { ok, err ->
                                            if (!ok) {
                                                Toast.makeText(context, err ?: "Operation failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- LOCK / KIOSK SHEET ---
    if (showKioskSheet) {
        val kioskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showKioskSheet = false },
            sheetState = kioskSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Lock / Kiosk Mode",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Single-task kiosk mode controls",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showKioskSheet = false }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "When Kiosk Mode is activated, the device is locked strictly into Brik or allowed apps, preventing home/back button exit without authorization.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                if (isKioskActive) {
                    Button(
                        onClick = {
                            if (activity != null) {
                                val success = settingsViewModel.stopKioskMode(activity)
                                if (success) {
                                    Toast.makeText(context, "Kiosk Mode Stopped", Toast.LENGTH_SHORT).show()
                                    showKioskSheet = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Exit Kiosk Mode")
                    }
                } else {
                    Button(
                        onClick = {
                            if (activity != null) {
                                val success = settingsViewModel.startKioskMode(activity)
                                if (success) {
                                    Toast.makeText(context, "Kiosk Mode Started", Toast.LENGTH_SHORT).show()
                                    showKioskSheet = false
                                } else {
                                    Toast.makeText(context, "Could not start kiosk. Ensure Device Owner or Dhizuku is active.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Start Kiosk Mode")
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

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
    trailingContent: @Composable (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = shape,
        color = containerColor,
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
                            modifier = Modifier.size(22.dp)
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
