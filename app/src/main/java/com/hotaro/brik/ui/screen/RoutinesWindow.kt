package com.hotaro.brik.ui.screen

import android.widget.Toast
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotaro.brik.R
import com.hotaro.brik.data.model.AutomatedRoutine
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.TimeBlockType
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.viewmodel.TimeBlockViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutinesWindow(
    onBack: () -> Unit,
    viewModel: TimeBlockViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val routines by viewModel.routines.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val bags by viewModel.bags.collectAsState()
    val savedKeys by viewModel.savedKeys.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var showCreateEditDialog by remember { mutableStateOf<AutomatedRoutine?>(null) }
    var routineToToggle by remember { mutableStateOf<AutomatedRoutine?>(null) }
    var routineToDelete by remember { mutableStateOf<AutomatedRoutine?>(null) }

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
                    showCreateEditDialog = AutomatedRoutine(
                        name = "",
                        startHour = 9,
                        startMinute = 0,
                        endHour = 17,
                        endMinute = 0
                    )
                },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_fill0),
                    contentDescription = "Create Routine"
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Routines",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Automated recurring schedules & bedtime blocks",
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
            // Active Routine Banner
            if (activeSession != null && activeSession!!.type == TimeBlockType.ROUTINE) {
                val session = activeSession!!
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = colorScheme.tertiaryContainer
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
                                shape = CircleShape,
                                color = colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(10.dp)
                            ) {}

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ROUTINE ACTIVE: ${session.title.uppercase()}",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "${session.packagesSuspended.size} apps suspended • Key required to toggle",
                                    style = typography.bodySmall,
                                    color = colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Routines (${routines.size})",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                }
            }

            // Routines List
            if (routines.isEmpty()) {
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
                                painter = painterResource(R.drawable.schedule_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No Scheduled Routines",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Automate your daily focus. Block distracting apps on set days and times automatically.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    showCreateEditDialog = AutomatedRoutine(
                                        name = "",
                                        startHour = 9,
                                        startMinute = 0,
                                        endHour = 17,
                                        endMinute = 0
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Create Routine")
                            }
                        }
                    }
                }
            } else {
                items(routines, key = { it.id }) { routine ->
                    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    val timeStr = formatRoutineTime(routine.startHour, routine.startMinute, routine.endHour, routine.endMinute)

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (routine.isActiveNow) colorScheme.primaryContainer.copy(alpha = 0.4f) else colorScheme.surfaceContainerHigh
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = routine.name,
                                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (routine.isActiveNow) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = timeStr,
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.primary
                                    )
                                }

                                Switch(
                                    checked = routine.isEnabled,
                                    onCheckedChange = { routineToToggle = routine }
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Day of Week Indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (1..7).forEach { dayNum ->
                                    val isDayActive = routine.daysOfWeek.contains(dayNum)
                                    val dayInitial = listOf("S", "M", "T", "W", "T", "F", "S")[dayNum - 1]
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDayActive) colorScheme.primary else colorScheme.surfaceContainerLowest,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = dayInitial,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDayActive) colorScheme.onPrimary else colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Badges & Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val targetCount = routine.targetBagIds.size + routine.targetPackageNames.size
                                    Surface(shape = CircleShape, color = colorScheme.surfaceContainerLowest) {
                                        Text(
                                            text = "$targetCount Targets",
                                            style = typography.labelSmall,
                                            color = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (routine.enableDnsBlocking) {
                                        Surface(shape = CircleShape, color = colorScheme.tertiaryContainer) {
                                            Text(
                                                text = "DNS",
                                                style = typography.labelSmall,
                                                color = colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { showCreateEditDialog = routine }) {
                                        Icon(
                                            painter = painterResource(R.drawable.tune_fill0),
                                            contentDescription = "Edit",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(onClick = { routineToDelete = routine }) {
                                        Icon(
                                            painter = painterResource(R.drawable.delete_fill0),
                                            contentDescription = "Delete",
                                            tint = colorScheme.error,
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
    }

    // --- Key Verification Dialog to Toggle Routine (Enable / Disable) ---
    if (routineToToggle != null) {
        val routine = routineToToggle!!
        var enteredKey by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }

        if (showScanner) {
            AlertDialog(
                onDismissRequest = { showScanner = false },
                title = { Text("Scan Routine Key", fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp))) {
                        QrScannerView(
                            onQrDetected = { detected ->
                                showScanner = false
                                viewModel.toggleRoutine(routine.id, detected) { ok, err ->
                                    if (ok) {
                                        Toast.makeText(context, "Routine status updated!", Toast.LENGTH_SHORT).show()
                                        routineToToggle = null
                                    } else {
                                        Toast.makeText(context, err ?: "Incorrect key", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScanner = false }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(24.dp)
            )
        } else {
            AlertDialog(
                onDismissRequest = { routineToToggle = null },
                title = {
                    Text(
                        text = if (routine.isEnabled) "Disable '${routine.name}'?" else "Enable '${routine.name}'?",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "All routines require their configured key to toggle on or off.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        if (routine.keyType == KeyType.QR_CODE) {
                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(painter = painterResource(R.drawable.qr_code_scanner_fill0), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scan Key QR Code")
                            }

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = enteredKey,
                                onValueChange = { enteredKey = it },
                                label = { Text("Or Paste QR Token") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = enteredKey,
                                onValueChange = { enteredKey = it },
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
                            viewModel.toggleRoutine(routine.id, enteredKey) { ok, err ->
                                if (ok) {
                                    Toast.makeText(context, "Routine updated!", Toast.LENGTH_SHORT).show()
                                    routineToToggle = null
                                } else {
                                    Toast.makeText(context, err ?: "Incorrect key", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = enteredKey.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { routineToToggle = null }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh
            )
        }
    }

    // --- Delete Routine Dialog ---
    if (routineToDelete != null) {
        val routine = routineToDelete!!
        var enteredSecret by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = {
                Text(
                    text = "Delete '${routine.name}'?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Are you sure you want to delete this routine? Key verification is required.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = enteredSecret,
                        onValueChange = { enteredSecret = it },
                        label = { Text("Routine Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRoutine(routine.id, enteredSecret) { ok, err ->
                            if (ok) {
                                Toast.makeText(context, "Deleted routine!", Toast.LENGTH_SHORT).show()
                                routineToDelete = null
                            } else {
                                Toast.makeText(context, err ?: "Incorrect key", Toast.LENGTH_SHORT).show()
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
                TextButton(onClick = { routineToDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Create / Edit Routine Dialog ---
    if (showCreateEditDialog != null) {
        val editing = showCreateEditDialog!!
        var nameInput by remember { mutableStateOf(editing.name) }
        var selectedDays by remember { mutableStateOf(editing.daysOfWeek.toSet()) }
        var startHour by remember { mutableStateOf(editing.startHour) }
        var startMinute by remember { mutableStateOf(editing.startMinute) }
        var endHour by remember { mutableStateOf(editing.endHour) }
        var endMinute by remember { mutableStateOf(editing.endMinute) }

        var selectedBags by remember { mutableStateOf(editing.targetBagIds.toSet()) }
        var enableDns by remember { mutableStateOf(editing.enableDnsBlocking) }
        var selectedDnsHost by remember { mutableStateOf(editing.dnsHost) }

        var selectedKeyType by remember { mutableStateOf(editing.keyType) }
        var passwordSecret by remember { mutableStateOf(if (editing.keyType == KeyType.PASSWORD) editing.keySecret ?: "" else "") }
        var qrSecret by remember { mutableStateOf(if (editing.keyType == KeyType.QR_CODE) editing.keySecret ?: "" else "") }
        var selectedSavedKeyId by remember { mutableStateOf(editing.savedKeyId) }

        AlertDialog(
            onDismissRequest = { showCreateEditDialog = null },
            title = {
                Text(
                    text = if (editing.name.isBlank()) "Create Routine" else "Edit '${editing.name}'",
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
                            label = { Text("Routine Name *") },
                            placeholder = { Text("e.g., Work Hours, Bedtime") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Days of Week Selection
                    item {
                        Text("Active Days of Week:", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            (1..7).forEach { dayNum ->
                                val isSelected = selectedDays.contains(dayNum)
                                Surface(
                                    onClick = {
                                        selectedDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = dayLabels[dayNum - 1].take(2),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Time Schedule Inputs (Start & End)
                    item {
                        Text("Time Window:", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = String.format(Locale.US, "%02d:%02d", startHour, startMinute),
                                onValueChange = { input ->
                                    val parts = input.split(":")
                                    if (parts.size == 2) {
                                        parts[0].toIntOrNull()?.let { if (it in 0..23) startHour = it }
                                        parts[1].toIntOrNull()?.let { if (it in 0..59) startMinute = it }
                                    }
                                },
                                label = { Text("Start (HH:MM)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = String.format(Locale.US, "%02d:%02d", endHour, endMinute),
                                onValueChange = { input ->
                                    val parts = input.split(":")
                                    if (parts.size == 2) {
                                        parts[0].toIntOrNull()?.let { if (it in 0..23) endHour = it }
                                        parts[1].toIntOrNull()?.let { if (it in 0..59) endMinute = it }
                                    }
                                },
                                label = { Text("End (HH:MM)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Target Bags Selector
                    item {
                        Text("Target Bags to Block:", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
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
                                        label = { Text(bag.name) }
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
                                Text("Filter domains during routine", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = enableDns, onCheckedChange = { enableDns = it })
                        }
                    }

                    // Key Protection
                    item {
                        Text("Routine Protection Key *:", style = typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                        Text("Required to toggle or modify this routine", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { selectedKeyType = KeyType.PASSWORD },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text("Password", style = typography.labelMedium) }
                            }
                            Surface(
                                onClick = { selectedKeyType = KeyType.QR_CODE },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text("QR Key", style = typography.labelMedium) }
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
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painter = painterResource(R.drawable.qr_code_fill0), contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(sk.name, style = typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
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
                val isFormValid = nameInput.isNotBlank() && selectedDays.isNotEmpty() && finalSecret.isNotBlank()

                Button(
                    onClick = {
                        val updated = editing.copy(
                            name = nameInput.trim(),
                            daysOfWeek = selectedDays.toList().sorted(),
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            targetBagIds = selectedBags.toList(),
                            enableDnsBlocking = enableDns,
                            dnsHost = selectedDnsHost,
                            keyType = selectedKeyType,
                            keySecret = finalSecret,
                            savedKeyId = selectedSavedKeyId
                        )
                        viewModel.saveRoutine(updated)
                        Toast.makeText(context, "Saved routine '${nameInput.trim()}'", Toast.LENGTH_SHORT).show()
                        showCreateEditDialog = null
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Routine")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateEditDialog = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }
}

private fun formatRoutineTime(sh: Int, sm: Int, eh: Int, em: Int): String {
    fun fmt(h: Int, m: Int): String {
        val ampm = if (h >= 12) "PM" else "AM"
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, m, ampm)
    }
    return "${fmt(sh, sm)} - ${fmt(eh, em)}"
}
