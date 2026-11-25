package com.hotaro.brik.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotaro.brik.R
import com.hotaro.brik.data.model.FocusMode
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.TimeBlockType
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.viewmodel.TimeBlockViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModesWindow(
    onBack: () -> Unit,
    viewModel: TimeBlockViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusModes by viewModel.focusModes.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val bags by viewModel.bags.collectAsState()
    val savedKeys by viewModel.savedKeys.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var showCreateEditDialog by remember { mutableStateOf<FocusMode?>(null) }
    var modeToStart by remember { mutableStateOf<FocusMode?>(null) }
    var modeToStop by remember { mutableStateOf<FocusMode?>(null) }
    var modeToDelete by remember { mutableStateOf<FocusMode?>(null) }

    // Live countdown timer state
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeSession) {
        while (activeSession != null) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                viewModel.syncActiveBlocks()
                delay(650)
                isRefreshing = false
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showCreateEditDialog = FocusMode(
                        name = "",
                        defaultDurationMinutes = 45
                    )
                },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_fill0),
                    contentDescription = "Create Focus Mode"
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Focus Modes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Dedicated restriction profiles and study sessions",
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Active Focus Session Banner
            if (activeSession != null && activeSession!!.type == TimeBlockType.FOCUS_MODE) {
                val session = activeSession!!
                val endsAt = session.endsAt ?: (currentTime + 3600000L)
                val remainingSeconds = ((endsAt - currentTime) / 1000L).coerceAtLeast(0)
                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                val seconds = remainingSeconds % 60
                val formattedTime = if (hours > 0) {
                    String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.US, "%02d:%02d", minutes, seconds)
                }

                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.error,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "ACTIVE SESSION",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onErrorContainer
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.surface.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${session.packagesSuspended.size} apps blocked",
                                        style = typography.labelSmall,
                                        color = colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = session.title,
                                style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onErrorContainer
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = formattedTime,
                                style = typography.displaySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colorScheme.error
                            )

                            val endsTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(endsAt))
                            Text(
                                text = "Ends at $endsTimeStr • Strict Key required to unlock early",
                                style = typography.bodySmall,
                                color = colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val mode = focusModes.find { it.id == session.id }
                                    if (mode != null) {
                                        modeToStop = mode
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Stop / Unlock with Key", color = colorScheme.onError)
                            }
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Profiles (${focusModes.size})",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                }
            }

            // List of Focus Modes
            if (focusModes.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.mode_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No Focus Modes",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Create customizable focus environments like Study, Sleep, or Deep Work with key protection.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    showCreateEditDialog = FocusMode(
                                        name = "",
                                        defaultDurationMinutes = 45
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Create First Mode")
                            }
                        }
                    }
                }
            } else {
                items(focusModes, key = { it.id }) { mode ->
                    val isCurrentRunning = mode.isRunning
                    val iconRes = when (mode.iconName) {
                        "schedule_fill0" -> R.drawable.schedule_fill0
                        "dark_mode_fill0" -> R.drawable.dark_mode_fill0
                        "backpack_fill0" -> R.drawable.backpack_fill0
                        "shield_fill0" -> R.drawable.shield_fill0
                        else -> R.drawable.mode_fill0
                    }

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isCurrentRunning) colorScheme.errorContainer.copy(alpha = 0.4f) else colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colorScheme.secondaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(iconRes),
                                            contentDescription = null,
                                            tint = colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.name,
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val targetCount = mode.targetBagIds.size + mode.targetPackageNames.size
                                    Text(
                                        text = "${mode.defaultDurationMinutes}m session • $targetCount targets",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isCurrentRunning) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.error
                                    ) {
                                        Text(
                                            text = "RUNNING",
                                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onError,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Badges (DNS, Key Method)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.surfaceContainerLowest
                                ) {
                                    Text(
                                        text = if (mode.keyType == KeyType.QR_CODE) "QR Key" else "Password Key",
                                        style = typography.labelSmall,
                                        color = colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                if (mode.enableDnsBlocking) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "DNS Filtered",
                                            style = typography.labelSmall,
                                            color = colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showCreateEditDialog = mode },
                                    enabled = !isCurrentRunning
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.tune_fill0),
                                        contentDescription = "Edit Mode",
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { modeToDelete = mode }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.delete_fill0),
                                        contentDescription = "Delete Mode",
                                        tint = colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(Modifier.width(6.dp))

                                if (isCurrentRunning) {
                                    Button(
                                        onClick = { modeToStop = mode },
                                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Stop")
                                    }
                                } else {
                                    Button(
                                        onClick = { modeToStart = mode },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Start Session")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Start Focus Mode Session Dialog (Duration Picker) ---
    if (modeToStart != null) {
        val mode = modeToStart!!
        var chosenMinutes by remember { mutableStateOf(mode.defaultDurationMinutes) }

        AlertDialog(
            onDismissRequest = { modeToStart = null },
            title = {
                Text(
                    text = "Start '${mode.name}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose duration for this session. A key will be required to exit early.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 45, 60, 90).forEach { mins ->
                            val isSelected = chosenMinutes == mins
                            Surface(
                                onClick = { chosenMinutes = mins },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = chosenMinutes.toString(),
                        onValueChange = { chosenMinutes = it.toIntOrNull() ?: 1 },
                        label = { Text("Custom Minutes") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startFocusMode(mode.id, chosenMinutes) { success, err ->
                            if (success) {
                                Toast.makeText(context, "'${mode.name}' started!", Toast.LENGTH_SHORT).show()
                                modeToStart = null
                            } else {
                                Toast.makeText(context, err ?: "Failed to start", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { modeToStart = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Stop Active Session Dialog (Key Verification) ---
    if (modeToStop != null) {
        val mode = modeToStop!!
        var enteredSecret by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }

        if (showScanner) {
            AlertDialog(
                onDismissRequest = { showScanner = false },
                title = { Text("Scan Key QR Code", fontWeight = FontWeight.Bold) },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        QrScannerView(
                            onQrDetected = { detected ->
                                showScanner = false
                                enteredSecret = detected
                                viewModel.stopFocusMode(mode.id, detected) { ok, err ->
                                    if (ok) {
                                        Toast.makeText(context, "'${mode.name}' session ended!", Toast.LENGTH_SHORT).show()
                                        modeToStop = null
                                    } else {
                                        Toast.makeText(context, err ?: "Invalid QR Key", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScanner = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        } else {
            AlertDialog(
                onDismissRequest = { modeToStop = null },
                title = {
                    Text(
                        text = "Unlock '${mode.name}'?",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Provide the configured ${if (mode.keyType == KeyType.QR_CODE) "QR Key" else "Password"} to stop this focus mode early.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        if (mode.keyType == KeyType.QR_CODE) {
                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.qr_code_scanner_fill0),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Scan QR Code Key")
                            }

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = enteredSecret,
                                onValueChange = { enteredSecret = it },
                                label = { Text("Or Paste QR Token") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = enteredSecret,
                                onValueChange = { enteredSecret = it },
                                label = { Text("Secret Password") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.stopFocusMode(mode.id, enteredSecret) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "'${mode.name}' unlocked!", Toast.LENGTH_SHORT).show()
                                    modeToStop = null
                                } else {
                                    Toast.makeText(context, err ?: "Incorrect key", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = enteredSecret.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { modeToStop = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh
            )
        }
    }

    // --- Delete Focus Mode Dialog ---
    if (modeToDelete != null) {
        val mode = modeToDelete!!
        var enteredSecret by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { modeToDelete = null },
            title = {
                Text(
                    text = "Delete '${mode.name}'?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (mode.isRunning) "This mode is currently running. Key is required to abort and delete." else "Are you sure you want to delete this focus profile?",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )

                    if (mode.isRunning) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enteredSecret,
                            onValueChange = { enteredSecret = it },
                            label = { Text("Mode Key") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFocusMode(mode.id, enteredSecret) { ok, err ->
                            if (ok) {
                                Toast.makeText(context, "Deleted '${mode.name}'", Toast.LENGTH_SHORT).show()
                                modeToDelete = null
                            } else {
                                Toast.makeText(context, err ?: "Failed to delete", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { modeToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Create / Edit Focus Mode Dialog ---
    if (showCreateEditDialog != null) {
        val editing = showCreateEditDialog!!
        var nameInput by remember { mutableStateOf(editing.name) }
        var selectedIcon by remember { mutableStateOf(editing.iconName) }
        var durationMinutes by remember { mutableStateOf(editing.defaultDurationMinutes) }
        var selectedBags by remember { mutableStateOf(editing.targetBagIds.toSet()) }
        var selectedPackages by remember { mutableStateOf(editing.targetPackageNames.toSet()) }
        var enableDns by remember { mutableStateOf(editing.enableDnsBlocking) }
        var selectedDnsHost by remember { mutableStateOf(editing.dnsHost) }

        var selectedKeyType by remember { mutableStateOf(editing.keyType) }
        var passwordSecret by remember { mutableStateOf(if (editing.keyType == KeyType.PASSWORD) editing.keySecret ?: "" else "") }
        var qrSecret by remember { mutableStateOf(if (editing.keyType == KeyType.QR_CODE) editing.keySecret ?: "" else "") }
        var selectedSavedKeyId by remember { mutableStateOf(editing.savedKeyId) }

        val icons = listOf(
            "mode_fill0" to R.drawable.mode_fill0,
            "schedule_fill0" to R.drawable.schedule_fill0,
            "dark_mode_fill0" to R.drawable.dark_mode_fill0,
            "backpack_fill0" to R.drawable.backpack_fill0,
            "shield_fill0" to R.drawable.shield_fill0
        )

        AlertDialog(
            onDismissRequest = { showCreateEditDialog = null },
            title = {
                Text(
                    text = if (editing.name.isBlank()) "Create Focus Mode" else "Edit '${editing.name}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Profile Name *") },
                            placeholder = { Text("e.g., Study, Sleeping, Family Time") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Icon Picker
                    item {
                        Text("Profile Icon:", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            icons.forEach { (name, res) ->
                                val isSelected = selectedIcon == name
                                Surface(
                                    onClick = { selectedIcon = name },
                                    shape = CircleShape,
                                    color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(res),
                                            contentDescription = null,
                                            tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Target Bags Selector
                    item {
                        Text("Target Bags to Suspend:", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        if (bags.isEmpty()) {
                            Text("No bags found in Tools > Bags.", style = typography.bodySmall, color = colorScheme.outline)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                bags.forEach { bag ->
                                    val isSelected = selectedBags.contains(bag.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedBags = if (isSelected) selectedBags - bag.id else selectedBags + bag.id
                                        },
                                        label = { Text(bag.name) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(painter = painterResource(R.drawable.check_fill0), contentDescription = null, Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // DNS Blocking Option
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Private DNS Blocking", style = typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Force clean DNS during session", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = enableDns, onCheckedChange = { enableDns = it })
                        }
                    }

                    if (enableDns) {
                        item {
                            Text("Select DNS Resolver:", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            DnsManager.PRESETS.take(4).forEach { preset ->
                                val isSelected = selectedDnsHost == preset.host
                                Surface(
                                    onClick = { selectedDnsHost = preset.host },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(preset.name, style = typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(preset.host, style = typography.bodySmall.copy(fontSize = 11.sp), color = colorScheme.outline)
                                        }
                                        if (isSelected) {
                                            Icon(painter = painterResource(R.drawable.check_fill0), contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Key Protection Configuration
                    item {
                        Text("Protection Key (Required to stop early):", style = typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { selectedKeyType = KeyType.PASSWORD },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Password", style = typography.labelMedium)
                                }
                            }
                            Surface(
                                onClick = { selectedKeyType = KeyType.QR_CODE },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("QR Key", style = typography.labelMedium)
                                }
                            }
                        }
                    }

                    if (selectedKeyType == KeyType.PASSWORD) {
                        item {
                            OutlinedTextField(
                                value = passwordSecret,
                                onValueChange = { passwordSecret = it },
                                label = { Text("Password *") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // QR Key selection (Saved Key or new Random Token)
                        item {
                            if (savedKeys.isNotEmpty()) {
                                Text("Pick from Saved Keys:", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                savedKeys.forEach { sk ->
                                    val isSelected = selectedSavedKeyId == sk.id || qrSecret == sk.token
                                    Surface(
                                        onClick = {
                                            selectedSavedKeyId = sk.id
                                            qrSecret = sk.token
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(painter = painterResource(R.drawable.qr_code_fill0), contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(sk.name, style = typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                            if (isSelected) {
                                                Icon(painter = painterResource(R.drawable.check_fill0), contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = qrSecret,
                                onValueChange = { qrSecret = it },
                                label = { Text("QR Token String *") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            TextButton(onClick = { qrSecret = UUID.randomUUID().toString() }) {
                                Text("Generate Random Token")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val finalSecret = if (selectedKeyType == KeyType.PASSWORD) passwordSecret.trim() else qrSecret.trim()
                val isFormValid = nameInput.isNotBlank() && finalSecret.isNotBlank()

                Button(
                    onClick = {
                        val updatedMode = editing.copy(
                            name = nameInput.trim(),
                            iconName = selectedIcon,
                            defaultDurationMinutes = durationMinutes,
                            targetBagIds = selectedBags.toList(),
                            targetPackageNames = selectedPackages.toList(),
                            enableDnsBlocking = enableDns,
                            dnsHost = selectedDnsHost,
                            keyType = selectedKeyType,
                            keySecret = finalSecret,
                            savedKeyId = selectedSavedKeyId
                        )
                        viewModel.saveFocusMode(updatedMode)
                        Toast.makeText(context, "Saved '${nameInput.trim()}'", Toast.LENGTH_SHORT).show()
                        showCreateEditDialog = null
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateEditDialog = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }
}
