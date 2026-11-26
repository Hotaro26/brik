package com.hotaro.brik.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotaro.brik.R
import com.hotaro.brik.data.model.BagLock
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.data.repository.TimeBlockRepository
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.component.SetEmergencyKeyDialog
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.VerifyEmergencyKeyDialog
import com.hotaro.brik.ui.component.segmentedItemShape
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import com.hotaro.brik.util.EmergencyManager
import com.hotaro.brik.util.GrayscaleManager
import com.hotaro.brik.util.QrUtils
import com.hotaro.brik.util.SideloadLockManager
import com.hotaro.brik.util.ZenManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysWindow(
    onBack: () -> Unit,
    keyViewModel: KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeLocks by keyViewModel.activeLocks.collectAsState()
    val stats by keyViewModel.stats.collectAsState()
    val savedKeys by keyViewModel.savedKeys.collectAsState()

    var viewQrLock by remember { mutableStateOf<BagLock?>(null) }
    var emergencyUnlockLock by remember { mutableStateOf<BagLock?>(null) }
    var viewSavedKey by remember { mutableStateOf<SavedKey?>(null) }
    var deleteSavedKeyTarget by remember { mutableStateOf<SavedKey?>(null) }
    var showAddSavedKeyDialog by remember { mutableStateOf(false) }
    var prefilledTokenForAdd by remember { mutableStateOf<String?>(null) }

    val isEmergencyActive by EmergencyManager.isEmergencyModeActive.collectAsState()
    val pullTimestamps = remember { mutableStateListOf<Long>() }
    var showSetEmergencyKeyDialog by remember { mutableStateOf(false) }
    var showVerifyEmergencyKeyDialog by remember { mutableStateOf(false) }

    // Unified System Key editing states
    var systemLockToEdit by remember { mutableStateOf<BagLock?>(null) }
    var showVerifyBeforeEditSystemKeyDialog by remember { mutableStateOf(false) }
    var showEditSystemKeyDialog by remember { mutableStateOf(false) }

    var qrOptionMode by remember { mutableStateOf(0) } // 0: Random, 1: Scan External
    var randomQrKey by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var scannedCustomQr by remember { mutableStateOf<String?>(null) }
    var showCustomQrScanner by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isActiveKeysExpanded by remember { mutableStateOf(true) }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                val now = System.currentTimeMillis()
                pullTimestamps.removeAll { now - it > 5000L }
                pullTimestamps.add(now)

                if (pullTimestamps.size >= 3) {
                    pullTimestamps.clear()
                    if (isEmergencyActive) {
                        Toast.makeText(context, "Emergency Mode is already active", Toast.LENGTH_SHORT).show()
                    } else if (!EmergencyManager.hasKey(context)) {
                        showSetEmergencyKeyDialog = true
                    } else {
                        showVerifyEmergencyKeyDialog = true
                    }
                }

                kotlinx.coroutines.delay(650)
                isRefreshing = false
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    prefilledTokenForAdd = null
                    showAddSavedKeyDialog = true
                },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_fill0),
                    contentDescription = "Add Saved Key"
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Keys & Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Active locks, physical vault & tokens",
                            style = typography.bodySmall.copy(fontSize = 11.sp),
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
                actions = {
                    if (isEmergencyActive) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.errorContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shield_fill0),
                                    contentDescription = null,
                                    tint = colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Emergency Active",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ==========================================
            // SECTION 1: OVERVIEW & ANALYTICS
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TomatoSectionHeader(title = "OVERVIEW & ANALYTICS")

                    // 3-Column Metric Overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Active",
                            value = "${activeLocks.size}",
                            subtitle = "Guarded Keys",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total",
                            value = "${stats.totalLocks}",
                            subtitle = "Locked Bags",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Unlocked",
                            value = "${stats.totalUnlocks}",
                            subtitle = "Successful",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Emergency Status Banner
                    if (isEmergencyActive) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shield_fill0),
                                    contentDescription = null,
                                    tint = colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Emergency Vault Unlocked",
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Saved tokens, QR visualizers, and manual unlock controls are accessible.",
                                        style = typography.bodySmall,
                                        color = colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        EmergencyManager.setEmergencyModeActive(context, false)
                                        Toast.makeText(context, "Emergency Mode deactivated", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Lock Vault", color = colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.shield_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Security Key Vault Enforced",
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Pull down 3 times rapidly on this screen to trigger Emergency Override verification.",
                                        style = typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: ACTIVE PROTECTION KEYS
            // ==========================================
            item {
                Surface(
                    onClick = { isActiveKeysExpanded = !isActiveKeysExpanded },
                    shape = if (isActiveKeysExpanded && activeLocks.isNotEmpty()) {
                        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    } else {
                        RoundedCornerShape(18.dp)
                    },
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.primaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.key_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Active Protection Keys",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (activeLocks.isEmpty()) "No keys configured" else "${activeLocks.size} key${if (activeLocks.size == 1) "" else "s"} active",
                                    style = typography.bodySmall.copy(fontSize = 11.sp),
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeLocks.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primary
                                ) {
                                    Text(
                                        text = "${activeLocks.size}",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward_big),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        rotationZ = if (isActiveKeysExpanded) 270f else 90f
                                    }
                            )
                        }
                    }
                }
            }

            if (isActiveKeysExpanded) {
                if (activeLocks.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.key_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "No Active Keys Configured",
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Keys set for Device Owner, Sideload/ADB, DNS, Grayscale, Zen Mode, Focus Modes, and App Bags will appear here.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(activeLocks, key = { _, lock -> lock.id }) { index, lock ->
                    val shape = segmentedItemShape(index, activeLocks.size)
                    val leadingIconRes = when {
                        lock.id == "dpm_uninstall_lock" -> R.drawable.shield_fill0
                        lock.id == "sideload_adb_lock" -> R.drawable.screen_lock_portrait_fill0
                        lock.id == "dns_lock" -> R.drawable.dns_fill0
                        lock.id == "grayscale_security_key" -> R.drawable.contrast_fill0
                        lock.id == "zen_security_key" -> R.drawable.self_improvement_fill0
                        lock.id.startsWith("focus_mode_") || lock.id.startsWith("routine_") -> R.drawable.schedule_fill0
                        else -> R.drawable.key_fill0
                    }

                    val protectionDesc = when (lock.id) {
                        "dpm_uninstall_lock" -> "Device Owner Uninstall Key • Required to revoke Device Owner"
                        "sideload_adb_lock" -> "Sideload & ADB Lockdown • Required to modify installation policies"
                        "dns_lock" -> "Private DNS Guard • Required to alter DNS protection rules"
                        "grayscale_security_key" -> "Screen Grayscale Guard • Required to disable monochrome filter"
                        "zen_security_key" -> "Zen Mode Security Key • Required to abort running zen session"
                        else -> {
                            if (lock.id.startsWith("focus_mode_")) {
                                "Focus Mode Protection • Key required to abort focus session"
                            } else if (lock.id.startsWith("routine_")) {
                                "Routine Protection • Key required to modify or toggle routine"
                            } else {
                                "${lock.packageNames.size} packages currently suspended in '${lock.bagName}'"
                            }
                        }
                    }

                    ElevatedCard(
                        onClick = {
                            if (lock.isSystemKey) {
                                systemLockToEdit = lock
                                showVerifyBeforeEditSystemKeyDialog = true
                            }
                        },
                        shape = shape,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(leadingIconRes),
                                            contentDescription = null,
                                            tint = if (lock.isSystemKey) colorScheme.primary else colorScheme.tertiary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = lock.bagName,
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val dateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(lock.lockedAt))
                                    Text(
                                        text = dateStr,
                                        style = typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colorScheme.outline
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = if (lock.isSystemKey) colorScheme.primaryContainer else if (lock.keyType == KeyType.QR_CODE) colorScheme.tertiaryContainer else colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = if (lock.keyType == KeyType.QR_CODE) "QR Key" else "Password",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (lock.isSystemKey) colorScheme.onPrimaryContainer else if (lock.keyType == KeyType.QR_CODE) colorScheme.onTertiaryContainer else colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = protectionDesc,
                                style = typography.bodySmall,
                                color = if (lock.isSystemKey) colorScheme.onSurfaceVariant else colorScheme.error
                            )

                            Spacer(Modifier.height(12.dp))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (lock.isSystemKey) {
                                    FilledTonalButton(
                                        onClick = {
                                            systemLockToEdit = lock
                                            showVerifyBeforeEditSystemKeyDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.edit_fill0),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit Key", style = typography.labelMedium)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                if (lock.keyType == KeyType.QR_CODE && lock.qrToken != null) {
                                    OutlinedButton(
                                        onClick = { viewQrLock = lock },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.qr_code_fill0),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("View QR", style = typography.labelMedium)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                // Emergency Unlock or Clear Key is ONLY displayed in Emergency Mode!
                                if (isEmergencyActive) {
                                    Button(
                                        onClick = { emergencyUnlockLock = lock },
                                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.errorContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.delete_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.onErrorContainer,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (lock.isSystemKey) "Clear Key" else "Emergency Unlock",
                                            color = colorScheme.onErrorContainer,
                                            style = typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                } // end else activeLocks.isNotEmpty
            } // end if isActiveKeysExpanded

            // ==========================================
            // SECTION 3: SAVED KEYS VAULT
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TomatoSectionHeader(title = "SAVED KEYS VAULT")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (savedKeys.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.secondaryContainer,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${savedKeys.size}",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                prefilledTokenForAdd = null
                                showAddSavedKeyDialog = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add_fill0),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Add Key")
                        }
                    }
                }
            }

            if (savedKeys.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
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
                                color = colorScheme.surfaceContainerLowest,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.qr_code_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Vault Is Empty",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Scan and save external QR codes (e.g. books, cards, household items) to quickly lock bags and features.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    prefilledTokenForAdd = null
                                    showAddSavedKeyDialog = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add_fill0),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Add Saved Key")
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(savedKeys, key = { _, key -> key.id }) { index, key ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                deleteSavedKeyTarget = key
                                false
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(segmentedItemShape(index, savedKeys.size))
                                    .background(colorScheme.error)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = alignment
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete_fill0),
                                    contentDescription = "Delete",
                                    tint = colorScheme.onError,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    ) {
                        Surface(
                            onClick = { viewSavedKey = key },
                            shape = segmentedItemShape(index, savedKeys.size),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.qr_code_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = key.name,
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (key.description.isNotBlank()) {
                                        Text(
                                            text = key.description,
                                            style = typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(key.createdAt))
                                    Text(
                                        text = "Saved $dateStr",
                                        style = typography.bodySmall.copy(fontSize = 10.sp),
                                        color = colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (!isEmergencyActive) {
                                                Toast.makeText(context, "Activate Emergency Mode to view or copy saved key tokens", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Brik Saved Key", key.token))
                                                Toast.makeText(context, "Key token copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(if (isEmergencyActive) R.drawable.content_copy_fill0 else R.drawable.shield_fill0),
                                            contentDescription = if (isEmergencyActive) "Copy Token" else "Key Protected",
                                            tint = if (isEmergencyActive) colorScheme.primary else colorScheme.outline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Icon(
                                        painter = painterResource(R.drawable.arrow_forward_big),
                                        contentDescription = "View QR Details",
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 4: QR CODE UTILITIES
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TomatoSectionHeader(title = "QR CODE UTILITIES")

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Token Generator & Physical Scanner",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Generate secure random tokens or bind real-world barcodes & QR codes.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(14.dp))

                            // Segmented Option Switcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceContainerLowest)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = { qrOptionMode = 0 },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (qrOptionMode == 0) colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Random Generator",
                                            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (qrOptionMode == 0) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { qrOptionMode = 1 },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (qrOptionMode == 1) colorScheme.primaryContainer else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Scan Any Code",
                                            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (qrOptionMode == 1) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            if (qrOptionMode == 0) {
                                val qrBmp = remember(randomQrKey) {
                                    try {
                                        QrUtils.encodeToBitmap(randomQrKey, 320)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (qrBmp != null) {
                                        Image(
                                            bitmap = qrBmp.asImageBitmap(),
                                            contentDescription = "Random QR Key",
                                            modifier = Modifier
                                                .size(150.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White)
                                                .padding(6.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Text(
                                        text = randomQrKey,
                                        style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                randomQrKey = UUID.randomUUID().toString()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Regenerate", style = typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", randomQrKey))
                                                Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Copy", style = typography.labelSmall)
                                        }

                                        Button(
                                            onClick = {
                                                prefilledTokenForAdd = randomQrKey
                                                showAddSavedKeyDialog = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Save", style = typography.labelSmall)
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (scannedCustomQr == null) {
                                        Text(
                                            text = "Scan any physical item (e.g. food box barcode, book ISBN, bottle QR) to turn it into an unlock key.",
                                            style = typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        Button(
                                            onClick = { showCustomQrScanner = true },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.qr_code_scanner_fill0),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Open Camera Scanner")
                                        }
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Scanned Key Detected",
                                                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = scannedCustomQr!!,
                                            style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(Modifier.height(12.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Brik Scanned QR Key", scannedCustomQr))
                                                    Toast.makeText(context, "Scanned key copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Copy", style = typography.labelSmall)
                                            }

                                            FilledTonalButton(
                                                onClick = {
                                                    prefilledTokenForAdd = scannedCustomQr
                                                    showAddSavedKeyDialog = true
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Save", style = typography.labelSmall)
                                            }

                                            OutlinedButton(
                                                onClick = { showCustomQrScanner = true },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Rescan", style = typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGS & BOTTOM SHEETS
    // ==========================================

    // --- View QR Code Dialog for Active Lock ---
    if (viewQrLock != null && viewQrLock!!.qrToken != null) {
        val lock = viewQrLock!!
        val qrBitmap = remember(lock.qrToken) {
            try {
                QrUtils.encodeToBitmap(lock.qrToken!!, 400)
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { viewQrLock = null },
            title = {
                Text(
                    text = "QR Key for '${lock.bagName}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code Key",
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Scan this QR code from Key window to unlock.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", lock.qrToken))
                            Toast.makeText(context, "QR Token copied!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Copy Token Text")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewQrLock = null }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Emergency Unlock / Clear Confirmation Dialog ---
    if (emergencyUnlockLock != null) {
        val lock = emergencyUnlockLock!!
        AlertDialog(
            onDismissRequest = { emergencyUnlockLock = null },
            title = {
                Text(
                    text = if (lock.isSystemKey) "Clear Security Key?" else "Emergency Unlock?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Text(
                    text = if (lock.isSystemKey) {
                        "This will remove the security key configured for '${lock.bagName}'."
                    } else {
                        "This will immediately unsuspend all ${lock.packageNames.size} apps in '${lock.bagName}' and remove the lock key."
                    },
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyViewModel.emergencyUnlock(context, lock) { ok, err ->
                            if (ok) {
                                Toast.makeText(context, if (lock.isSystemKey) "Key cleared!" else "'${lock.bagName}' emergency unlocked!", Toast.LENGTH_SHORT).show()
                                emergencyUnlockLock = null
                            } else {
                                Toast.makeText(context, err ?: "Failed to unlock", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (lock.isSystemKey) "Yes, Clear Key" else "Yes, Unlock Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { emergencyUnlockLock = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Custom QR Scanner Dialog for Utilities ---
    if (showCustomQrScanner) {
        AlertDialog(
            onDismissRequest = { showCustomQrScanner = false },
            title = {
                Text("Scan Any Barcode or QR", fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    QrScannerView(
                        onQrDetected = { detected ->
                            scannedCustomQr = detected
                            showCustomQrScanner = false
                            Toast.makeText(context, "Code captured!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomQrScanner = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- Add Saved Key Dialog ---
    if (showAddSavedKeyDialog) {
        var nameInput by remember { mutableStateOf("") }
        var descriptionInput by remember { mutableStateOf("") }
        var tokenInput by remember(prefilledTokenForAdd) { mutableStateOf(prefilledTokenForAdd ?: "") }
        var showScannerForAdd by remember { mutableStateOf(false) }

        if (showScannerForAdd) {
            AlertDialog(
                onDismissRequest = { showScannerForAdd = false },
                title = { Text("Scan QR Code to Save", fontWeight = FontWeight.Bold) },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        QrScannerView(
                            onQrDetected = { detected ->
                                tokenInput = detected
                                showScannerForAdd = false
                                Toast.makeText(context, "QR code captured!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScannerForAdd = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        } else {
            val previewBmp = remember(tokenInput) {
                if (tokenInput.isNotBlank()) {
                    try {
                        QrUtils.encodeToBitmap(tokenInput, 240)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

            AlertDialog(
                onDismissRequest = {
                    showAddSavedKeyDialog = false
                    prefilledTokenForAdd = null
                },
                title = {
                    Text(
                        text = "Add Saved Key",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Scan or type a QR key with a name and description to save it in your vault.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Key Name *") },
                                placeholder = { Text("e.g., Desk QR, Blue Notebook") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = descriptionInput,
                                onValueChange = { descriptionInput = it },
                                label = { Text("Description (Optional)") },
                                placeholder = { Text("e.g., Barcode on shelf item") },
                                maxLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = { Text("Key Token / QR Content *") },
                                placeholder = { Text("Paste token or scan QR") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showScannerForAdd = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.qr_code_scanner_fill0),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Scan QR", style = typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        tokenInput = UUID.randomUUID().toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Random Key", style = typography.labelSmall)
                                }
                            }
                        }

                        if (previewBmp != null) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        bitmap = previewBmp.asImageBitmap(),
                                        contentDescription = "QR Preview",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .padding(4.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "QR Preview",
                                        style = typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            keyViewModel.saveKey(
                                name = nameInput.trim(),
                                description = descriptionInput.trim(),
                                token = tokenInput.trim()
                            )
                            Toast.makeText(context, "Saved key '${nameInput.trim()}'!", Toast.LENGTH_SHORT).show()
                            showAddSavedKeyDialog = false
                            prefilledTokenForAdd = null
                        },
                        enabled = nameInput.isNotBlank() && tokenInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Key")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddSavedKeyDialog = false
                        prefilledTokenForAdd = null
                    }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh
            )
        }
    }

    // --- View / Edit Saved Key Bottom Sheet ---
    if (viewSavedKey != null) {
        val key = viewSavedKey!!
        val qrBitmap = remember(key.token) {
            try {
                QrUtils.encodeToBitmap(key.token, 400)
            } catch (e: Exception) {
                null
            }
        }
        var isEditingKey by remember(key.id) { mutableStateOf(false) }
        var editedName by remember(key.id, key.name) { mutableStateOf(key.name) }
        var editedDesc by remember(key.id, key.description) { mutableStateOf(key.description) }

        ModalBottomSheet(
            onDismissRequest = {
                viewSavedKey = null
                isEditingKey = false
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isEditingKey) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = key.name,
                                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            if (key.description.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = key.description,
                                    style = typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { isEditingKey = true }) {
                            Icon(
                                painter = painterResource(R.drawable.edit_fill0),
                                contentDescription = "Edit Key Details",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Edit Key Details",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editedDesc,
                            onValueChange = { editedDesc = it },
                            label = { Text("Description") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isEditingKey = false }) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val trimmedName = editedName.trim()
                                    if (trimmedName.isNotEmpty()) {
                                        keyViewModel.updateSavedKey(key.id, trimmedName, editedDesc.trim())
                                        viewSavedKey = key.copy(name = trimmedName, description = editedDesc.trim())
                                        isEditingKey = false
                                        Toast.makeText(context, "Key details updated!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (isEmergencyActive) {
                    if (qrBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            modifier = Modifier.size(240.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code Key",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = key.token,
                        style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", key.token))
                            Toast.makeText(context, "QR Token copied!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.content_copy_fill0),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Token Text")
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colorScheme.surfaceContainerLowest,
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
                                color = colorScheme.secondaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.shield_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Key Content Vault Protected",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "QR visualizer and token contents are hidden to prevent impulsive unlocking.\n\nPull down 3 times rapidly on Keys & Analytics to activate Emergency Mode.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Delete Saved Key Dialog ---
    if (deleteSavedKeyTarget != null) {
        val target = deleteSavedKeyTarget!!
        AlertDialog(
            onDismissRequest = { deleteSavedKeyTarget = null },
            title = {
                Text(
                    text = "Delete Saved Key?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${target.name}'? This key will be permanently removed from your vault.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyViewModel.deleteSavedKey(target.id)
                        Toast.makeText(context, "Deleted '${target.name}'", Toast.LENGTH_SHORT).show()
                        deleteSavedKeyTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSavedKeyTarget = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Emergency Mode Setup & Verification Dialogs ---
    if (showSetEmergencyKeyDialog) {
        SetEmergencyKeyDialog(
            savedKeys = savedKeys,
            onDismiss = { showSetEmergencyKeyDialog = false },
            onKeySet = { showSetEmergencyKeyDialog = false }
        )
    }

    if (showVerifyEmergencyKeyDialog) {
        val keyType = remember(context) { EmergencyManager.getKeyType(context) }
        VerifyEmergencyKeyDialog(
            title = "Emergency Override",
            subtitle = if (keyType == KeyType.QR_CODE) {
                "Scan your Device Owner / Emergency QR code to activate Emergency Mode."
            } else {
                "Enter your Device Owner password or Emergency Key to activate Emergency Mode."
            },
            keyTypeOverride = keyType,
            onDismiss = { showVerifyEmergencyKeyDialog = false },
            onVerified = {
                showVerifyEmergencyKeyDialog = false
                Toast.makeText(context, "Emergency Mode activated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- Unified System Key Verification Before Edit Dialog ---
    if (showVerifyBeforeEditSystemKeyDialog && systemLockToEdit != null) {
        val lock = systemLockToEdit!!
        val currentKeyType = when (lock.id) {
            "dpm_uninstall_lock" -> keyViewModel.getDpmUninstallLock()?.keyType ?: KeyType.PASSWORD
            "sideload_adb_lock" -> SideloadLockManager.getKeyType(context)
            "dns_lock" -> DnsManager.getKeyType(context)
            "grayscale_security_key" -> GrayscaleManager.getKeyType(context)
            "zen_security_key" -> ZenManager.getKeyType(context)
            else -> lock.keyType
        }

        VerifyEmergencyKeyDialog(
            title = "Verify Current Key",
            subtitle = if (currentKeyType == KeyType.QR_CODE) {
                "Scan or enter your existing QR key to authorize editing '${lock.bagName}'."
            } else {
                "Enter your existing password to authorize editing '${lock.bagName}'."
            },
            keyTypeOverride = currentKeyType,
            onVerifyCustom = { input ->
                when (lock.id) {
                    "dpm_uninstall_lock" -> keyViewModel.verifyDpmUninstallKey(input)
                    "sideload_adb_lock" -> SideloadLockManager.verifySecurityKey(context, input)
                    "dns_lock" -> DnsManager.verifySecurityKey(context, input)
                    "grayscale_security_key" -> GrayscaleManager.verifySecurityKey(context, input)
                    "zen_security_key" -> ZenManager.verifySecurityKey(context, input)
                    else -> keyViewModel.verifySystemSecurityKey(lock.id, input)
                }
            },
            onDismiss = {
                showVerifyBeforeEditSystemKeyDialog = false
                systemLockToEdit = null
            },
            onVerified = {
                showVerifyBeforeEditSystemKeyDialog = false
                showEditSystemKeyDialog = true
            }
        )
    }

    // --- Unified System Key Edit Dialog ---
    if (showEditSystemKeyDialog && systemLockToEdit != null) {
        val lock = systemLockToEdit!!
        SetEmergencyKeyDialog(
            savedKeys = savedKeys,
            title = "Edit ${lock.bagName}",
            subtitle = "Set a new password or QR key for ${lock.bagName}.",
            onDismiss = {
                showEditSystemKeyDialog = false
                systemLockToEdit = null
            },
            onSaveKey = { keyType, secret ->
                when {
                    lock.id == "dpm_uninstall_lock" -> {
                        keyViewModel.setDpmUninstallLock(keyType, secret)
                    }
                    lock.id == "sideload_adb_lock" -> {
                        SideloadLockManager.setSecurityKey(context, keyType, secret)
                    }
                    lock.id == "dns_lock" -> {
                        DnsManager.setSecurityKey(context, keyType, secret)
                    }
                    lock.id == "grayscale_security_key" -> {
                        GrayscaleManager.setSecurityKey(context, keyType, secret)
                    }
                    lock.id == "zen_security_key" -> {
                        ZenManager.setSecurityKey(context, keyType, secret)
                    }
                    lock.id.startsWith("focus_mode_") -> {
                        val modeId = lock.id.removePrefix("focus_mode_")
                        TimeBlockRepository(context).updateFocusModeKey(modeId, keyType, secret)
                    }
                    lock.id.startsWith("routine_") -> {
                        val routineId = lock.id.removePrefix("routine_")
                        TimeBlockRepository(context).updateRoutineKey(routineId, keyType, secret)
                    }
                    else -> {
                        keyViewModel.setSystemSecurityKey(lock.id, lock.bagName, keyType, secret)
                    }
                }
                Toast.makeText(context, "${lock.bagName} updated successfully!", Toast.LENGTH_SHORT).show()
            },
            onKeySet = {
                showEditSystemKeyDialog = false
                systemLockToEdit = null
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.primary
            )
            Text(
                text = title,
                style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = typography.bodySmall.copy(fontSize = 10.sp),
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
