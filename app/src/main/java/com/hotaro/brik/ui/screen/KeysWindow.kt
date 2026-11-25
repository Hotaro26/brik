package com.hotaro.brik.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotaro.brik.R
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import com.hotaro.brik.data.model.BagLock
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import com.hotaro.brik.util.QrUtils
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

    var qrOptionMode by remember { mutableStateOf(0) } // 0: Random, 1: Scan External
    var randomQrKey by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var scannedCustomQr by remember { mutableStateOf<String?>(null) }
    var showCustomQrScanner by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                kotlinx.coroutines.delay(650)
                isRefreshing = false
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSavedKeyDialog = true },
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
                    Text(
                        text = "Keys & Analytics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colorScheme.onSurface
                    )
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Active",
                        value = "${activeLocks.size}",
                        subtitle = "Bags Locked",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total",
                        value = "${stats.totalLocks}",
                        subtitle = "Locked Sessions",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Unlocked",
                        value = "${stats.totalUnlocks}",
                        subtitle = "Success Unlocks",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Saved Keys Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved Keys",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    if (savedKeys.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${savedKeys.size}",
                                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
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
                            Icon(
                                painter = painterResource(R.drawable.qr_code_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No Saved Keys",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Scan and save physical QR codes or tokens here for quick access when locking bags.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            Button(
                                onClick = { showAddSavedKeyDialog = true },
                                shape = RoundedCornerShape(10.dp)
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
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            savedKeys.forEachIndexed { index, key ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colorScheme.surfaceContainerLowest,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.qr_code_fill0),
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(26.dp)
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
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Brik Saved Key", key.token))
                                                Toast.makeText(context, "Key token copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.content_copy_fill0),
                                                contentDescription = "Copy Token",
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewSavedKey = key }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.qr_code_scanner_fill0),
                                                contentDescription = "View QR",
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { deleteSavedKeyTarget = key }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.delete_fill0),
                                                contentDescription = "Delete Key",
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

            // QR Key Options (Random Generated QR vs Scan Any Other QR)
            item {
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
                            text = "QR Key Options",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Generate random QR key or scan any physical QR code",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        // Segmented Option Selector
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
                                        text = "Random QR",
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
                                        text = "Scan Any QR",
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
                                            .size(160.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(android.graphics.Color.WHITE.let { Color(it) })
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

                                Spacer(Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Brik Random QR Key", randomQrKey))
                                            Toast.makeText(context, "Random QR Key copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Copy Key", style = typography.labelSmall)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            randomQrKey = UUID.randomUUID().toString()
                                            Toast.makeText(context, "New random key generated!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Generate New", style = typography.labelSmall)
                                    }
                                }
                            }
                        } else {
                            val customBmp = remember(scannedCustomQr) {
                                if (scannedCustomQr != null) {
                                    try {
                                        QrUtils.encodeToBitmap(scannedCustomQr!!, 320)
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else null
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (scannedCustomQr == null) {
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
                                                text = "Scan any QR code from a book, card, receipt, or packaging to use or verify it as an external key.",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                            )
                                            Button(
                                                onClick = { showCustomQrScanner = true },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Start Camera Scanner")
                                            }
                                        }
                                    }
                                } else {
                                    if (customBmp != null) {
                                        Image(
                                            bitmap = customBmp.asImageBitmap(),
                                            contentDescription = "Scanned Physical QR",
                                            modifier = Modifier
                                                .size(160.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(android.graphics.Color.WHITE.let { Color(it) })
                                                .padding(6.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Physical QR Key Captured",
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

                                    Spacer(Modifier.height(10.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Brik Scanned QR Key", scannedCustomQr))
                                                Toast.makeText(context, "Scanned key copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Copy Scanned Key", style = typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = { showCustomQrScanner = true },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Scan Another", style = typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Active Keys",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary
                )
            }

            if (activeLocks.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.key_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No Active Keys",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Lock any bag in Tools > Bags with Password or QR Code to track its key here.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(activeLocks, key = { it.id }) { lock ->
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = lock.bagName,
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    val dateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(lock.lockedAt))
                                    Text(
                                        text = if (lock.isSystemKey) "System Protection Key • $dateStr" else "Locked $dateStr",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (lock.isSystemKey) colorScheme.errorContainer else if (lock.keyType == KeyType.QR_CODE) colorScheme.tertiaryContainer else colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (lock.isSystemKey) "System Key" else if (lock.keyType == KeyType.QR_CODE) "QR Key" else "Password Key",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (lock.isSystemKey) colorScheme.onErrorContainer else if (lock.keyType == KeyType.QR_CODE) colorScheme.onTertiaryContainer else colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            if (lock.isSystemKey) {
                                Text(
                                    text = "Device Owner Uninstall Key (${if (lock.keyType == KeyType.QR_CODE) "QR Key" else "Password"}) • Required to revoke DPM",
                                    style = typography.bodySmall,
                                    color = colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "${lock.packageNames.size} packages currently suspended",
                                    style = typography.bodySmall,
                                    color = colorScheme.error
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                        Text("View QR")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }

                                Button(
                                    onClick = { emergencyUnlockLock = lock },
                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.errorContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (lock.isSystemKey) "Clear Key" else "Emergency Unlock", color = colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- View QR Code Dialog ---
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
                                .background(android.graphics.Color.WHITE.let { androidx.compose.ui.graphics.Color(it) })
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

    // --- Emergency Unlock Confirmation Dialog ---
    if (emergencyUnlockLock != null) {
        val lock = emergencyUnlockLock!!
        AlertDialog(
            onDismissRequest = { emergencyUnlockLock = null },
            title = {
                Text(
                    text = if (lock.isSystemKey) "Clear Uninstall Key?" else "Emergency Unlock?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Text(
                    text = if (lock.isSystemKey) {
                        "This will remove the saved uninstall security key for Device Owner."
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
                                Toast.makeText(context, if (lock.isSystemKey) "Uninstall key cleared!" else "'${lock.bagName}' emergency unlocked!", Toast.LENGTH_SHORT).show()
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

    // --- Custom QR Scanner Dialog ---
    if (showCustomQrScanner) {
        AlertDialog(
            onDismissRequest = { showCustomQrScanner = false },
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
                            scannedCustomQr = detected
                            showCustomQrScanner = false
                            Toast.makeText(context, "QR code captured!", Toast.LENGTH_SHORT).show()
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
        var tokenInput by remember { mutableStateOf("") }
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
                onDismissRequest = { showAddSavedKeyDialog = false },
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
                                text = "Scan or type a QR key with a name and description to save it for quick use.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Key Name *") },
                                placeholder = { Text("e.g., Desk QR, Journal Book") },
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
                                placeholder = { Text("e.g., Barcode on notebook cover") },
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
                                            .background(android.graphics.Color.WHITE.let { Color(it) })
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
                        },
                        enabled = nameInput.isNotBlank() && tokenInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Key")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSavedKeyDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh
            )
        }
    }

    // --- View Saved Key Dialog ---
    if (viewSavedKey != null) {
        val key = viewSavedKey!!
        val qrBitmap = remember(key.token) {
            try {
                QrUtils.encodeToBitmap(key.token, 400)
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { viewSavedKey = null },
            title = {
                Column {
                    Text(
                        text = key.name,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    if (key.description.isNotBlank()) {
                        Text(
                            text = key.description,
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                                .background(android.graphics.Color.WHITE.let { Color(it) })
                                .padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = key.token,
                        style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", key.token))
                            Toast.makeText(context, "QR Token copied!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Copy Token Text")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewSavedKey = null }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
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
                    text = "Are you sure you want to delete '${target.name}'? This key will no longer appear in quick use.",
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
