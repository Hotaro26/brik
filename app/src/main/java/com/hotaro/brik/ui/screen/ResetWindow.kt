package com.hotaro.brik.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.theme.ThemePreferences
import com.hotaro.brik.util.ResetManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetWindow(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resetState by ResetManager.resetState.collectAsState()
    val use24Hour by ThemePreferences.use24HourClock.collectAsState()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showStartConfirmDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(resetState.isScheduled, resetState.targetAt) {
        if (resetState.isScheduled) {
            while (resetState.isScheduled) {
                val current = System.currentTimeMillis()
                nowMs = current
                if (current >= resetState.targetAt) {
                    ResetManager.checkAndExecuteIfDue(context)
                    break
                }
                delay(1000L)
            }
        }
    }

    val remainingMs = if (resetState.isScheduled) {
        (resetState.targetAt - nowMs).coerceAtLeast(0L)
    } else 0L

    val totalSeconds = remainingMs / 1000L
    val days = totalSeconds / 86400L
    val hours = (totalSeconds % 86400L) / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    val progress = if (resetState.isScheduled) {
        val elapsed = (nowMs - resetState.startedAt).coerceIn(0L, ResetManager.RESET_DURATION_MS)
        (elapsed.toFloat() / ResetManager.RESET_DURATION_MS.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Reset & Revoke",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "5-day cooldown safety reset",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Hero / Status Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = when {
                    resetState.isCompleted -> colorScheme.primaryContainer
                    resetState.isScheduled -> colorScheme.errorContainer.copy(alpha = 0.7f)
                    else -> colorScheme.surfaceContainerHigh
                },
                border = BorderStroke(
                    1.dp,
                    if (resetState.isScheduled) colorScheme.error.copy(alpha = 0.4f) else colorScheme.outlineVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            resetState.isCompleted -> colorScheme.primary
                            resetState.isScheduled -> colorScheme.error
                            else -> colorScheme.errorContainer
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (resetState.isResetting) {
                                CircularProgressIndicator(
                                    color = colorScheme.onError,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        if (resetState.isCompleted) R.drawable.check_fill0 else R.drawable.delete_fill0
                                    ),
                                    contentDescription = null,
                                    tint = when {
                                        resetState.isCompleted -> colorScheme.onPrimary
                                        resetState.isScheduled -> colorScheme.onError
                                        else -> colorScheme.onErrorContainer
                                    },
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = when {
                            resetState.isResetting -> "Resetting Brik..."
                            resetState.isCompleted -> "Device & App Reset Complete"
                            resetState.isScheduled -> "Reset Countdown Active"
                            else -> "5-Day Safe Reset"
                        },
                        style = typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when {
                            resetState.isResetting -> "Releasing all restrictions, clearing app data, and revoking Device Owner..."
                            resetState.isCompleted -> "All app blocks, limits, keys, and Device Owner permissions have been cleared."
                            resetState.isScheduled -> "When the countdown reaches zero, all restrictions are lifted, data is wiped, and Device Owner is revoked."
                            else -> "Starts a 5-day countdown before removing all distraction blocks, clearing Brik data, and revoking Device Owner permissions."
                        },
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (resetState.isScheduled && !resetState.isResetting) {
                TomatoSectionHeader(title = "TIME REMAINING")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CountdownUnitBox(value = days, label = "DAYS", modifier = Modifier.weight(1f))
                    CountdownUnitBox(value = hours, label = "HOURS", modifier = Modifier.weight(1f))
                    CountdownUnitBox(value = minutes, label = "MINS", modifier = Modifier.weight(1f))
                    CountdownUnitBox(value = seconds, label = "SECS", modifier = Modifier.weight(1f))
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = colorScheme.error,
                            trackColor = colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                        val pattern = if (use24Hour) "EEE, MMM dd • HH:mm" else "EEE, MMM dd • hh:mm a"
                        val targetFormatted = remember(resetState.targetAt, use24Hour) {
                            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(resetState.targetAt))
                        }
                        Text(
                            text = "Scheduled completion: $targetFormatted",
                            style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showCancelConfirmDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Cancel Countdown", fontWeight = FontWeight.Bold)
                }
            } else if (!resetState.isCompleted && !resetState.isResetting) {
                TomatoSectionHeader(title = "WHAT HAPPENS AFTER 5 DAYS")

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ResetEffectRow("All suspended & hidden apps are restored")
                        ResetEffectRow("Bags, Routines, Focus Modes, Limits, and Keys are wiped")
                        ResetEffectRow("Private DNS, Grayscale, Zen, Bedtime & Sideload locks reset")
                        ResetEffectRow("Device Owner (DPM) permission is revoked cleanly")
                    }
                }

                Button(
                    onClick = { showStartConfirmDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Start 5-Day Reset Countdown", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    if (showStartConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStartConfirmDialog = false },
            title = {
                Text(
                    text = "Start 5-Day Reset?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Text(
                    "Brik will begin a 5-day countdown. Once the timer finishes, all distractions will be removed, app data will be wiped, and Device Owner permissions will be revoked."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        ResetManager.startCountdown(context)
                        showStartConfirmDialog = false
                        Toast.makeText(context, "5-day reset countdown started", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    )
                ) {
                    Text("Start Countdown", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = "Cancel Reset Countdown?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to cancel the scheduled 5-day reset? Your current restrictions and Device Owner setup will remain active."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        ResetManager.cancelCountdown(context)
                        showCancelConfirmDialog = false
                        Toast.makeText(context, "Reset countdown cancelled", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Confirm Cancel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Keep Running")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun CountdownUnitBox(
    value: Long,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%02d", value),
                style = typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = colorScheme.error
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResetEffectRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = colorScheme.errorContainer,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.check_fill0),
                    contentDescription = null,
                    tint = colorScheme.onErrorContainer,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Text(
            text = text,
            style = typography.bodySmall,
            color = colorScheme.onSurface
        )
    }
}
