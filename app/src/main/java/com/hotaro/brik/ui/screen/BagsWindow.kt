package com.hotaro.brik.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import com.hotaro.brik.util.QrUtils
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BagsWindow(
    onBack: () -> Unit,
    viewModel: ToolsViewModel = viewModel(),
    keyViewModel: KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val bags by viewModel.bags.collectAsState()
    val activeLocks by keyViewModel.activeLocks.collectAsState()

    var bagToLock by remember { mutableStateOf<AppBag?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                viewModel.loadInstalledApps()
                kotlinx.coroutines.delay(650)
                isRefreshing = false
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bags",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${bags.size} configured bundles • Tap switch to lock",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(10.dp))

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
                            text = "No Bags Configured",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Go to App Manager, select your apps, and pack them into a bundle bag.",
                            style = typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 40.dp),
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

    // --- Lock Bag Dialog ---
    if (bagToLock != null) {
        val bag = bagToLock!!
        val savedKeys by keyViewModel.savedKeys.collectAsState()
        var selectedKeyType by remember { mutableStateOf(KeyType.PASSWORD) }
        var password by remember { mutableStateOf("") }
        var qrMode by remember { mutableStateOf(0) } // 0 = Random QR, 1 = Scan External QR, 2 = Saved Key
        val generatedQrToken = remember { UUID.randomUUID().toString() }
        var scannedQrToken by remember { mutableStateOf<String?>(null) }
        var selectedSavedKey by remember { mutableStateOf<SavedKey?>(null) }
        var showScannerDialog by remember { mutableStateOf(false) }

        // Auto-select first saved key if mode is switched to Saved Key and none selected
        if (qrMode == 2 && selectedSavedKey == null && savedKeys.isNotEmpty()) {
            selectedSavedKey = savedKeys.first()
        }

        val activeQrToken = if (qrMode == 0) generatedQrToken else if (qrMode == 1) scannedQrToken else selectedSavedKey?.token
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

                            Surface(
                                onClick = { qrMode = 2 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (qrMode == 2) colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Saved Key",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (qrMode == 2) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
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
                                    textAlign = TextAlign.Center
                                )

                                TextButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", generatedQrToken))
                                    Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Copy Token Text", style = typography.labelSmall)
                                }
                            }
                        } else if (qrMode == 1) {
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
                                                textAlign = TextAlign.Center,
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
                        } else {
                            // qrMode == 2: Saved Key
                            if (savedKeys.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No Saved Keys",
                                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Go to Keys & Analytics and tap '+' to scan and save your physical keys first.",
                                            style = typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Select a saved key for quick locking:",
                                        style = typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant
                                    )

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(savedKeys, key = { it.id }) { key ->
                                            val isSelected = selectedSavedKey?.id == key.id
                                            Surface(
                                                onClick = { selectedSavedKey = key },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.qr_code_fill0),
                                                        contentDescription = null,
                                                        tint = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = key.name,
                                                            style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (key.description.isNotBlank()) {
                                                            Text(
                                                                text = key.description,
                                                                style = typography.bodySmall.copy(fontSize = 11.sp),
                                                                color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.check_fill0),
                                                            contentDescription = "Selected",
                                                            tint = colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(18.dp)
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
                }
            },
            confirmButton = {
                val isFormValid = if (selectedKeyType == KeyType.PASSWORD) {
                    password.isNotBlank()
                } else {
                    if (qrMode == 0) true else if (qrMode == 1) scannedQrToken != null else selectedSavedKey != null
                }

                Button(
                    onClick = {
                        val secret = if (selectedKeyType == KeyType.PASSWORD) {
                            password
                        } else {
                            if (qrMode == 0) generatedQrToken else if (qrMode == 1) scannedQrToken!! else selectedSavedKey!!.token
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
}
