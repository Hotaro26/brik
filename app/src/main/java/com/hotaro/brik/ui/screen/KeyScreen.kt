package com.hotaro.brik.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotaro.brik.R
import com.hotaro.brik.data.model.BagLock
import com.hotaro.brik.data.model.KeyType
import androidx.compose.runtime.rememberCoroutineScope
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyScreen(
    modifier: Modifier = Modifier,
    keyViewModel: KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUnlockPillVisible by remember { mutableStateOf(false) }
    var showUnlockSheet by remember { mutableStateOf(false) }
    var selectedLockForUnlock by remember { mutableStateOf<BagLock?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val activeLocks by keyViewModel.activeLocks.collectAsState()

    val scale by animateFloatAsState(
        targetValue = if (isUnlockPillVisible) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "keyScale"
    )

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                kotlinx.coroutines.delay(650)
                isRefreshing = false
            }
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key",
                    style = typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(bottom = 96.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                // Tappable Key Icon Surface
            Surface(
                shape = CircleShape,
                color = if (isUnlockPillVisible) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .scale(scale)
                    .size(96.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isUnlockPillVisible = !isUnlockPillVisible
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.key_fill0),
                        contentDescription = "Key Icon",
                        tint = if (isUnlockPillVisible) colorScheme.onPrimaryContainer else colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Key",
                style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface
            )

            Text(
                text = if (activeLocks.isNotEmpty()) "${activeLocks.size} active locked bag${if (activeLocks.size > 1) "s" else ""}" else "Tap key to reveal unlock",
                style = typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Animated Unlock Pill
            AnimatedVisibility(
                visible = isUnlockPillVisible,
                enter = slideInVertically(
                    initialOffsetY = { -20 },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    onClick = { showUnlockSheet = true },
                    shape = CircleShape,
                    color = colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.password_fill0),
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Unlock",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
}

    // --- Unlock Drawer (Bottom Sheet) ---
    if (showUnlockSheet) {
        val unlockSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showUnlockSheet = false },
            sheetState = unlockSheetState,
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
                            text = "Unlock Bags",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (activeLocks.isNotEmpty()) "${activeLocks.size} locked bundles" else "No locked bags",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showUnlockSheet = false }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (activeLocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check_fill0),
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "All Bags Unlocked",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "There are no currently active locked bags to unlock.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeLocks, key = { it.id }) { lock ->
                            ElevatedCard(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = colorScheme.surfaceContainerHigh
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = lock.bagName,
                                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (lock.keyType == KeyType.QR_CODE) colorScheme.tertiaryContainer else colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = if (lock.keyType == KeyType.QR_CODE) "QR Key" else "Password Key",
                                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (lock.keyType == KeyType.QR_CODE) colorScheme.onTertiaryContainer else colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "${lock.packageNames.size} apps",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            selectedLockForUnlock = lock
                                            if (lock.keyType == KeyType.PASSWORD) {
                                                passwordInput = ""
                                                showPasswordDialog = true
                                            } else {
                                                showQrScannerDialog = true
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Unlock")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Password Input Dialog ---
    if (showPasswordDialog && selectedLockForUnlock != null) {
        val lock = selectedLockForUnlock!!
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = {
                Text(
                    text = "Unlock '${lock.bagName}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the password you created when locking this bag.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyViewModel.unlockBag(context, lock, passwordInput) { success, err ->
                            if (success) {
                                Toast.makeText(context, "'${lock.bagName}' unlocked successfully!", Toast.LENGTH_SHORT).show()
                                showPasswordDialog = false
                                selectedLockForUnlock = null
                            } else {
                                Toast.makeText(context, err ?: "Incorrect password", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = passwordInput.isNotBlank()
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- QR Scanner Dialog ---
    if (showQrScannerDialog && selectedLockForUnlock != null) {
        val lock = selectedLockForUnlock!!
        AlertDialog(
            onDismissRequest = { showQrScannerDialog = false },
            title = {
                Text(
                    text = "Scan QR Key for '${lock.bagName}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Point camera at the QR code generated when locking.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    QrScannerView(
                        onQrDetected = { scannedToken ->
                            keyViewModel.unlockBag(context, lock, scannedToken) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "'${lock.bagName}' unlocked via QR Key!", Toast.LENGTH_SHORT).show()
                                    showQrScannerDialog = false
                                    selectedLockForUnlock = null
                                } else {
                                    Toast.makeText(context, err ?: "Invalid QR Code", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQrScannerDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }
}
