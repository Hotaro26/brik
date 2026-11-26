package com.hotaro.brik.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.ui.component.VerifyEmergencyKeyDialog
import com.hotaro.brik.util.WindDownManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WindDownDesktop() {
    val context = LocalContext.current
    val activity = context as? Activity
    val pm = context.packageManager

    val wakeHour by WindDownManager.wakeHour.collectAsState()
    val wakeMinute by WindDownManager.wakeMinute.collectAsState()
    val isNoAppsMode by WindDownManager.isNoAppsMode.collectAsState()
    val allowedPackages by WindDownManager.allowedPackages.collectAsState()
    val isPreviewMode by WindDownManager.isPreviewMode.collectAsState()
    val allowEmergencyUnlock by WindDownManager.allowEmergencyUnlock.collectAsState()
    var showEmergencyUnlockDialog by remember { mutableStateOf(false) }

    val wakeTimeFormatted = remember(wakeHour, wakeMinute) {
        String.format(Locale.getDefault(), "%02d:%02d", wakeHour, wakeMinute)
    }

    // Intercept back key: offers emergency key unlock if allowed, or exits preview mode
    BackHandler(enabled = true) {
        if (isPreviewMode) {
            WindDownManager.stopPreviewMode(activity)
            Toast.makeText(context, "Exited Wind Down preview", Toast.LENGTH_SHORT).show()
        } else if (allowEmergencyUnlock) {
            showEmergencyUnlockDialog = true
        } else {
            Toast.makeText(context, "Strict bedtime mode active • Emergency unlock is disabled", Toast.LENGTH_SHORT).show()
        }
    }

    var currentTime by remember { mutableStateOf(Date()) }
    var remainingMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(wakeHour, wakeMinute) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = Date(now)
            remainingMillis = WindDownManager.getMillisUntilWake(now, wakeHour, wakeMinute)
            WindDownManager.checkActive(context, activity)
            delay(1000)
        }
    }

    val use24Hour by com.hotaro.brik.ui.theme.ThemePreferences.use24HourClock.collectAsState()
    val timeDigitsFormat = remember(use24Hour) { SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm", Locale.getDefault()) }
    val amPmFormat = remember(use24Hour) { if (use24Hour) null else SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val launcherApps = remember(context) { loadLauncherApps(pm, context.packageName) }
    val permittedApps = remember(allowedPackages, launcherApps) {
        launcherApps.filter { allowedPackages.contains(it.packageName) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Absorb blank desktop clicks */ }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Minimalist Clock & Bedtime Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeDigitsFormat.format(currentTime),
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )
                    if (!use24Hour) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = amPmFormat?.format(currentTime)?.uppercase() ?: "",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                }

                Text(
                    text = dateFormat.format(currentTime),
                    style = typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    color = Color(0xFF8E8E93)
                )

                Spacer(Modifier.height(18.dp))

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF16171B),
                    border = BorderStroke(1.dp, Color(0xFF24252B))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dark_mode_fill0),
                            contentDescription = null,
                            tint = Color(0xFFB0B3C0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val totalMinutes = (remainingMillis / 60000L).coerceAtLeast(0L)
                        val h = totalMinutes / 60
                        val m = totalMinutes % 60
                        val countdownStr = if (h > 0) "${h}h ${m}m remaining" else "${m}m remaining"
                        Text(
                            text = "Wind Down Active • $countdownStr",
                            style = typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFFE2E2E6)
                        )
                    }
                }
            }

            // Center: Minimalist Apps or Pure Rest Message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (isNoAppsMode || permittedApps.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF141519),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.dark_mode_fill0),
                                    contentDescription = null,
                                    tint = Color(0xFF7A7D8A),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No apps allowed",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFDDDDDD)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Time to wind down and rest.",
                            style = typography.bodySmall,
                            color = Color(0xFF6B6E7B)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(permittedApps, key = { it.packageName }) { app ->
                            Text(
                                text = app.label,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFFE8E8EC),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        } else {
                                            Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 28.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Bottom: Lock Notice or Exit Preview Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPreviewMode) {
                    Button(
                        onClick = {
                            WindDownManager.stopPreviewMode(activity)
                            Toast.makeText(context, "Exited Wind Down preview", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF252730),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Exit Preview Mode", fontWeight = FontWeight.Bold)
                    }
                } else if (allowEmergencyUnlock) {
                    Surface(
                        onClick = { showEmergencyUnlockDialog = true },
                        shape = CircleShape,
                        color = Color(0xFF141518),
                        border = BorderStroke(1.dp, Color(0xFF23242A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.key_fill0),
                                contentDescription = null,
                                tint = Color(0xFF8E90A0),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Emergency Unlock",
                                style = typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFFB0B3C0)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shield_fill0),
                            contentDescription = null,
                            tint = Color(0xFF5E606E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Strict lockdown active until $wakeTimeFormatted",
                            style = typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFF5E606E)
                        )
                    }
                }
            }
        }

        val floatingControlsEnabled by WindDownManager.floatingControlsEnabled.collectAsState()
        val floatingAutoHideSeconds by WindDownManager.floatingControlsAutoHideSeconds.collectAsState()
        if (floatingControlsEnabled) {
            com.hotaro.brik.ui.component.FloatingQuickControls(
                autoHideDelaySeconds = floatingAutoHideSeconds,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showEmergencyUnlockDialog) {
        VerifyEmergencyKeyDialog(
            title = "Exit Wind Down",
            subtitle = "Verify your security key to unlock and turn off Wind Down mode.",
            keyTypeOverride = WindDownManager.getKeyType(context),
            onVerifyCustom = { input ->
                WindDownManager.verifySecurityKey(context, input)
            },
            onDismiss = { showEmergencyUnlockDialog = false },
            onVerified = {
                showEmergencyUnlockDialog = false
                WindDownManager.setEnabled(context, false, activity)
                WindDownManager.stopPreviewMode(activity)
                Toast.makeText(context, "Wind Down mode unlocked and turned off", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

private fun loadLauncherApps(pm: PackageManager, currentPackageName: String): List<AppInfo> {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    val seenPackages = mutableSetOf<String>()
    val result = mutableListOf<AppInfo>()

    for (info in resolveInfos) {
        val pkg = info.activityInfo.packageName
        if (pkg != currentPackageName && seenPackages.add(pkg)) {
            val label = info.loadLabel(pm).toString().ifBlank { pkg }
            val icon: Drawable? = try {
                info.loadIcon(pm)
            } catch (_: Exception) {
                null
            }
            result.add(
                AppInfo(
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    isSystemApp = false
                )
            )
        }
    }

    return result.sortedBy { it.label.lowercase(Locale.getDefault()) }
}
