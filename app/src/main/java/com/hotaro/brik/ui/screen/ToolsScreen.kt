package com.hotaro.brik.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.policy.DnsMode
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import com.hotaro.brik.util.QrUtils
import com.hotaro.brik.util.StealthUtils
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    viewModel: ToolsViewModel = viewModel(),
    keyViewModel: KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAppManagerSheet by remember { mutableStateOf(false) }
    var showBagsSheet by remember { mutableStateOf(false) }
    var showCreateBagDialog by remember { mutableStateOf(false) }
    var bagToLock by remember { mutableStateOf<AppBag?>(null) }
    var showDnsSheet by remember { mutableStateOf(false) }
    var showStealthSheet by remember { mutableStateOf(false) }

    var dnsStatus by remember { mutableStateOf(DnsManager.getCurrentDns(context)) }
    var isAppHidden by remember { mutableStateOf(StealthUtils.isAppHidden(context)) }
    var dialerCode by remember { mutableStateOf(StealthUtils.getDialerCode(context)) }

    val bags by viewModel.bags.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeLocks by keyViewModel.activeLocks.collectAsState()

    val appManagerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bagsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dnsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stealthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tools",
            style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.onSurface
        )

        // Section 1: App Manager Card
        ElevatedCard(
            onClick = { showAppManagerSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.apps_fill0),
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "App Manager",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (installedApps.isNotEmpty()) "${installedApps.size} apps available" else "Manage and inspect applications",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 2: Bags Card
        ElevatedCard(
            onClick = { showBagsSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.backpack_fill0),
                            contentDescription = null,
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Bags",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (bags.isNotEmpty()) "${bags.size} bags (${activeLocks.size} active)" else "Pack apps into custom bundles",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 3: Private DNS (DoT) Card
        ElevatedCard(
            onClick = {
                dnsStatus = DnsManager.getCurrentDns(context)
                showDnsSheet = true
            },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.tertiaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.dns_fill0),
                            contentDescription = null,
                            tint = colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Private DNS",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    val dnsSub = when (dnsStatus.mode) {
                        DnsMode.OFF -> "Disabled (Standard ISP DNS)"
                        DnsMode.OPPORTUNISTIC -> "Automatic (Opportunistic)"
                        DnsMode.SPECIFIED_HOST -> dnsStatus.host ?: "Custom Host"
                    }
                    Text(
                        text = dnsSub,
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (dnsStatus.mode == DnsMode.SPECIFIED_HOST) colorScheme.primaryContainer else if (dnsStatus.mode == DnsMode.OPPORTUNISTIC) colorScheme.secondaryContainer else colorScheme.surfaceContainerLowest
                ) {
                    Text(
                        text = when (dnsStatus.mode) {
                            DnsMode.OFF -> "Off"
                            DnsMode.OPPORTUNISTIC -> "Auto"
                            DnsMode.SPECIFIED_HOST -> "Custom"
                        },
                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (dnsStatus.mode == DnsMode.SPECIFIED_HOST) colorScheme.onPrimaryContainer else if (dnsStatus.mode == DnsMode.OPPORTUNISTIC) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Section 4: Stealth Mode (Hide Brik) Card
        ElevatedCard(
            onClick = {
                isAppHidden = StealthUtils.isAppHidden(context)
                dialerCode = StealthUtils.getDialerCode(context)
                showStealthSheet = true
            },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isAppHidden) colorScheme.errorContainer else colorScheme.surfaceContainerLowest,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(if (isAppHidden) R.drawable.visibility_off_fill0 else R.drawable.call_fill0),
                            contentDescription = null,
                            tint = if (isAppHidden) colorScheme.onErrorContainer else colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Stealth Mode (Hide Brik)",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (isAppHidden) "Hidden • Dial *#*#$dialerCode#*#*" else "Visible • Dial code: *#*#$dialerCode#*#*",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (isAppHidden) colorScheme.errorContainer else colorScheme.surfaceContainerLowest
                ) {
                    Text(
                        text = if (isAppHidden) "Hidden" else "Visible",
                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isAppHidden) colorScheme.onErrorContainer else colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(130.dp))
    }

    // --- App Manager Bottom Sheet ---
    if (showAppManagerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppManagerSheet = false },
            sheetState = appManagerSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Box(modifier = Modifier.fillMaxHeight(0.85f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "App Manager",
                                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedPackages.isNotEmpty()) "${selectedPackages.size} selected" else "${installedApps.size} apps installed",
                                style = typography.bodyMedium,
                                color = colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedPackages.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearSelection() }) {
                                    Text("Clear", color = colorScheme.error)
                                }
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    appManagerSheetState.hide()
                                    showAppManagerSheet = false
                                }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_fill0),
                                    contentDescription = "Close",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps...") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.search_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        painter = painterResource(R.drawable.close_fill0),
                                        contentDescription = "Clear search",
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = colorScheme.surfaceContainerLow,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Apps List or Loading State
                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colorScheme.primary)
                        }
                    } else if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No apps found",
                                style = typography.bodyLarge,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppListItem(
                                    app = app,
                                    isSelected = selectedPackages.contains(app.packageName),
                                    onToggle = { viewModel.toggleAppSelection(app.packageName) }
                                )
                            }
                        }
                    }
                }

                // Floating Action Button with Plus Icon to make a Bag
                FloatingActionButton(
                    onClick = {
                        if (selectedPackages.isEmpty()) {
                            Toast.makeText(context, "Select at least 1 app to create a bag", Toast.LENGTH_SHORT).show()
                        } else {
                            showCreateBagDialog = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add_fill0),
                            contentDescription = "Create Bag"
                        )
                        AnimatedVisibility(visible = selectedPackages.isNotEmpty()) {
                            Text(
                                text = "Pack Bag (${selectedPackages.size})",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Bags Bottom Sheet ---
    if (showBagsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBagsSheet = false },
            sheetState = bagsSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Bags",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${bags.size} configured bundles • Tap switch to lock",
                            style = typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {
                        coroutineScope.launch {
                            bagsSheetState.hide()
                            showBagsSheet = false
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (bags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.backpack_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No Bags Yet",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Open App Manager, select your favorite apps, and tap '+' to pack them into a bag.",
                                style = typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bags, key = { it.id }) { bag ->
                            val isLocked = keyViewModel.isBagLocked(bag.id)
                            BagItemCard(
                                bag = bag,
                                isLocked = isLocked,
                                viewModel = viewModel,
                                onToggleLock = { shouldLock ->
                                    if (shouldLock) {
                                        bagToLock = bag
                                    } else {
                                        Toast.makeText(context, "Go to Key window to unlock '${bag.name}'", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = { viewModel.deleteBag(bag.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Create Bag Name Dialog ---
    if (showCreateBagDialog) {
        var bagNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateBagDialog = false },
            title = {
                Text(
                    text = "Name your Bag",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Pack ${selectedPackages.size} selected app${if (selectedPackages.size > 1) "s" else ""} into a custom bundle.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = bagNameInput,
                        onValueChange = { bagNameInput = it },
                        placeholder = { Text("e.g. Socials, Study, Work") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bagNameInput.isNotBlank()) {
                            val success = viewModel.createBag(bagNameInput)
                            if (success) {
                                Toast.makeText(context, "Bag '$bagNameInput' created!", Toast.LENGTH_SHORT).show()
                                showCreateBagDialog = false
                            }
                        }
                    },
                    enabled = bagNameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Bag")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBagDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Lock Bag: Set Key (Password or QR) Dialog ---
    if (bagToLock != null) {
        val bag = bagToLock!!
        var selectedKeyType by remember { mutableStateOf(KeyType.PASSWORD) }
        var password by remember { mutableStateOf("") }
        var qrMode by remember { mutableStateOf(0) } // 0 = Random QR, 1 = Scan External QR
        val generatedQrToken = remember { UUID.randomUUID().toString() }
        var scannedQrToken by remember { mutableStateOf<String?>(null) }
        var showScannerDialog by remember { mutableStateOf(false) }

        val activeQrToken = if (qrMode == 0) generatedQrToken else scannedQrToken
        val qrBitmap = remember(activeQrToken) {
            if (activeQrToken != null) {
                try {
                    QrUtils.encodeToBitmap(activeQrToken, 320)
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        AlertDialog(
            onDismissRequest = { bagToLock = null },
            title = {
                Text(
                    text = "Lock '${bag.name}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose a lock key to suspend apps in this bag:",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    // Key Type Picker
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
                                .height(56.dp)
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
                                .height(56.dp)
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
                            placeholder = { Text("Enter secret password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // QR Key Sub-mode Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surfaceContainerLowest)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { qrMode = 0 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (qrMode == 0) colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Random QR",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (qrMode == 0) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                onClick = { qrMode = 1 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (qrMode == 1) colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Scan Any QR",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (qrMode == 1) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (qrMode == 0) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR Code Key",
                                        modifier = Modifier
                                            .size(160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(android.graphics.Color.WHITE.let { Color(it) })
                                            .padding(6.dp)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Save or copy this random QR key to unlock later.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                TextButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", generatedQrToken))
                                    Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Copy Token Text", style = typography.labelSmall)
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (scannedQrToken == null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = colorScheme.surfaceContainerLowest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.qr_code_fill0),
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "Scan Physical QR Code",
                                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Scan any QR code from a book, card, or packaging to use as the lock key.",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                            )
                                            Button(
                                                onClick = { showScannerDialog = true },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Start Camera Scanner")
                                            }
                                        }
                                    }
                                } else {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = "Scanned Physical QR",
                                            modifier = Modifier
                                                .size(150.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(android.graphics.Color.WHITE.let { Color(it) })
                                                .padding(6.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Physical QR Captured",
                                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = scannedQrToken!!,
                                        style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { showScannerDialog = true },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Scan Again", style = typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", scannedQrToken))
                                                Toast.makeText(context, "Key copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Copy Text", style = typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val isFormValid = if (selectedKeyType == KeyType.PASSWORD) {
                    password.isNotBlank()
                } else {
                    if (qrMode == 0) true else scannedQrToken != null
                }

                Button(
                    onClick = {
                        val secret = if (selectedKeyType == KeyType.PASSWORD) {
                            password
                        } else {
                            if (qrMode == 0) generatedQrToken else scannedQrToken!!
                        }
                        keyViewModel.lockBag(context, bag, selectedKeyType, secret) { success, err ->
                            if (success) {
                                Toast.makeText(context, "'${bag.name}' locked and suspended!", Toast.LENGTH_SHORT).show()
                                bagToLock = null
                            } else {
                                Toast.makeText(context, err ?: "Failed to lock", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (selectedKeyType == KeyType.QR_CODE) "Lock Bag with QR" else "Lock Bag")
                }
            },
            dismissButton = {
                TextButton(onClick = { bagToLock = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )

        // Nested Scanner Dialog for Scanning Any External QR
        if (showScannerDialog) {
            AlertDialog(
                onDismissRequest = { showScannerDialog = false },
                title = {
                    Text("Scan Any QR Code", fontWeight = FontWeight.Bold)
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
                                scannedQrToken = detected
                                showScannerDialog = false
                                Toast.makeText(context, "QR code scanned!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScannerDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }

    // --- Private DNS Bottom Sheet ---
    if (showDnsSheet) {
        var selectedMode by remember { mutableStateOf(dnsStatus.mode) }
        var hostInput by remember { mutableStateOf(dnsStatus.host ?: "dns.adguard-dns.com") }

        ModalBottomSheet(
            onDismissRequest = { showDnsSheet = false },
            sheetState = dnsSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Private DNS",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Route device traffic through secure encrypted DNS",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            dnsSheetState.hide()
                            showDnsSheet = false
                        }
                    }) {
                        Icon(painterResource(R.drawable.close_fill0), contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Mode Selector Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        DnsMode.OFF to "Off",
                        DnsMode.OPPORTUNISTIC to "Automatic",
                        DnsMode.SPECIFIED_HOST to "Custom DNS"
                    ).forEach { (m, label) ->
                        Surface(
                            onClick = { selectedMode = m },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMode == m) colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selectedMode == m) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (selectedMode == DnsMode.SPECIFIED_HOST) {
                    Text(
                        text = "Quick Presets",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(DnsManager.PRESETS) { preset ->
                            val isSelected = hostInput.trim().equals(preset.host, ignoreCase = true)
                            Surface(
                                onClick = { hostInput = preset.host },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("DNS Provider Hostname") },
                        placeholder = { Text("e.g. dns.adguard-dns.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Android will establish an encrypted TLS connection directly to this DNS provider host.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                } else if (selectedMode == DnsMode.OPPORTUNISTIC) {
                    Text(
                        text = "Automatic mode will attempt to use encrypted DNS with your existing network's servers when available.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Private DNS is turned off. Your ISP's standard unencrypted DNS will be used.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        val (ok, msg) = DnsManager.setDns(context, selectedMode, hostInput)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (ok) {
                            dnsStatus = DnsManager.getCurrentDns(context)
                            coroutineScope.launch {
                                dnsSheetState.hide()
                                showDnsSheet = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Apply DNS Settings")
                }
            }
        }
    }

    // --- Stealth Mode Bottom Sheet ---
    if (showStealthSheet) {
        var tempCode by remember { mutableStateOf(dialerCode) }

        ModalBottomSheet(
            onDismissRequest = { showStealthSheet = false },
            sheetState = stealthSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Stealth Mode",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Hide Brik icon & reopen via Phone dialer",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            stealthSheetState.hide()
                            showStealthSheet = false
                        }
                    }) {
                        Icon(painterResource(R.drawable.close_fill0), contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Toggle Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Hide Brik from Launcher",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (isAppHidden) "App icon is currently hidden" else "App icon is visible in launcher drawer",
                                style = typography.bodySmall,
                                color = if (isAppHidden) colorScheme.error else colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAppHidden,
                            onCheckedChange = { hide ->
                                StealthUtils.setAppHidden(context, hide)
                                isAppHidden = StealthUtils.isAppHidden(context)
                                Toast.makeText(
                                    context,
                                    if (hide) "Brik icon hidden! Dial *#*#$dialerCode#*#* to open." else "Brik icon restored to launcher.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Dialer Code Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Dialer Secret Code",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "To open Brik while hidden, open your Phone dialer and type:",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "*#*#$dialerCode#*#*",
                                style = typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                ),
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = tempCode,
                            onValueChange = { input ->
                                tempCode = input.filter { it.isDigit() }
                            },
                            label = { Text("Change Code (digits only)") },
                            placeholder = { Text("e.g. 2745") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (tempCode.isNotBlank()) {
                                        StealthUtils.setDialerCode(context, tempCode)
                                        dialerCode = StealthUtils.getDialerCode(context)
                                        Toast.makeText(context, "Secret code saved to *#*#$dialerCode#*#*", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Code")
                            }

                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:*#*#$dialerCode#*#*")
                                    }
                                    try {
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Dial *#*#$dialerCode#*#* in Phone app", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test in Dialer")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.35f) else colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val painter = rememberDrawablePainter(drawable = app.icon)
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = typography.bodySmall.copy(fontSize = 11.sp),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    uncheckedColor = colorScheme.outline
                )
            )
        }
    }
}

@Composable
fun BagItemCard(
    bag: AppBag,
    isLocked: Boolean,
    viewModel: ToolsViewModel,
    onToggleLock: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLocked) colorScheme.errorContainer else colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(if (isLocked) R.drawable.block_fill0 else R.drawable.backpack_fill0),
                                contentDescription = null,
                                tint = if (isLocked) colorScheme.onErrorContainer else colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bag.name,
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            if (isLocked) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.error
                                ) {
                                    Text(
                                        text = "LOCKED",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                                        color = colorScheme.onError,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${bag.packageNames.size} apps packed",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { onToggleLock(it) }
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(R.drawable.delete_fill0),
                            contentDescription = "Delete Bag",
                            tint = colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mini App Icons Preview in the Bag
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(bag.packageNames.toList()) { pkg ->
                    val appInfo = viewModel.getAppInfo(pkg)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (appInfo?.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = appInfo.icon),
                                    contentDescription = appInfo.label,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.apps_fill0),
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
