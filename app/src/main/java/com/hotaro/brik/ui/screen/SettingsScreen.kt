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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title & Active Bridge Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Settings",
                    style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
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
        Text(
            text = "Privileged Bridges",
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.primary
        )

        // Device Owner Card
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (bridgeStatus.isDeviceOwner) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.shield_fill0),
                                contentDescription = null,
                                tint = if (bridgeStatus.isDeviceOwner) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Device Owner (DPM)",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (bridgeStatus.isDeviceOwner) "Active & Full Control" else "Not Provisioned",
                            style = typography.bodySmall,
                            color = if (bridgeStatus.isDeviceOwner) colorScheme.primary else colorScheme.error
                        )
                    }
                }

                if (bridgeStatus.isDeviceOwner) {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val existing = keyViewModel.getDpmUninstallLock()
                            if (existing == null) {
                                showSetUninstallKeyDialog = true
                            } else {
                                showVerifyUninstallKeyDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Deactivate Device Owner (Allow Uninstall)", color = colorScheme.onErrorContainer)
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    val dpmCmd = "adb shell dpm set-device-owner com.hotaro.brik/.admin.BrikAdminReceiver"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dpmCmd,
                                style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", dpmCmd))
                                Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy_fill0),
                                    contentDescription = "Copy",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!bridgeStatus.isDeviceOwner) {
            // Dhizuku Card
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (bridgeStatus.isDhizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.key_fill0),
                                contentDescription = null,
                                tint = if (bridgeStatus.isDhizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Dhizuku Bridge",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (bridgeStatus.isDhizukuGranted) "Granted & Operational" else if (bridgeStatus.isDhizukuInstalled) "Permission Needed" else "Not Installed",
                            style = typography.bodySmall,
                            color = if (bridgeStatus.isDhizukuGranted) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }

                    if (!bridgeStatus.isDhizukuGranted && bridgeStatus.isDhizukuInstalled) {
                        FilledTonalButton(onClick = { settingsViewModel.requestDhizukuPermission() }) {
                            Text("Grant")
                        }
                    }
                }
            }

            // Shizuku Card
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (bridgeStatus.isShizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.backpack_fill0),
                                contentDescription = null,
                                tint = if (bridgeStatus.isShizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Shizuku Bridge",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (bridgeStatus.isShizukuGranted) "Granted & Running" else if (bridgeStatus.isShizukuRunning) "Permission Needed" else "Service Stopped",
                            style = typography.bodySmall,
                            color = if (bridgeStatus.isShizukuGranted) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }

                    if (bridgeStatus.isShizukuRunning && !bridgeStatus.isShizukuGranted) {
                        FilledTonalButton(onClick = { settingsViewModel.requestShizukuPermission() }) {
                            Text("Grant")
                        }
                    }
                }
            }
        }

        // --- SECTION 2: KEYS & ANALYTICS ---
        Text(
            text = "Keys & Security",
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.primary
        )

        ElevatedCard(
            onClick = onNavigateToKeys,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.key_fill0),
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Keys & Lock Analytics",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "${activeLocks.size} active bag keys • ${stats.totalLocks} sessions",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.arrow_back_fill0),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }

        // --- SECTION 3: NON-NEGOTIABLES ENFORCEMENT ---
        Text(
            text = "Policy Enforcement (Core)",
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.primary
        )

        // 1. App Blocking (Suspension)
        ElevatedCard(
            onClick = { showBlockSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.errorContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.block_fill0),
                            contentDescription = null,
                            tint = colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "App Blocking (Suspension)",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (suspendedPackages.isNotEmpty()) "${suspendedPackages.size} packages suspended" else "Suspend distracting apps or entire bags",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. App Hiding
        ElevatedCard(
            onClick = { showHideSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.tertiaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.visibility_off_fill0),
                            contentDescription = null,
                            tint = colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "App Hiding",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (hiddenPackages.isNotEmpty()) "${hiddenPackages.size} apps hidden" else "Completely hide packages from system launcher",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Lock / Kiosk Mode
        ElevatedCard(
            onClick = { showKioskSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.screen_lock_portrait_fill0),
                            contentDescription = null,
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Lock / Kiosk Mode",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (isKioskActive) "Kiosk Active" else "Pin app into single-task kiosk mode",
                        style = typography.bodySmall,
                        color = if (isKioskActive) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                }
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
