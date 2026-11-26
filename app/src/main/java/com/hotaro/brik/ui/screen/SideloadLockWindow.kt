package com.hotaro.brik.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.policy.PolicyBridgeManager
import com.hotaro.brik.ui.component.SetEmergencyKeyDialog
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.TomatoSettingsItem
import com.hotaro.brik.ui.component.VerifyEmergencyKeyDialog
import com.hotaro.brik.ui.component.segmentedItemShape
import com.hotaro.brik.util.SideloadLockManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideloadLockWindow(onBack: () -> Unit) {
    val context = LocalContext.current
    val keyRepository = remember(context) { KeyRepository(context) }
    val savedKeys by keyRepository.savedKeys.collectAsState()
    val bridgeStatus = remember { PolicyBridgeManager.getBridgeStatus(context) }
    val isPrivileged = bridgeStatus.isDeviceOwner || bridgeStatus.isDhizukuGranted

    val isRestrictionActive by SideloadLockManager.isRestrictionActive.collectAsState()
    val restrictUnknownSources by SideloadLockManager.restrictUnknownSources.collectAsState()
    val restrictPlayStore by SideloadLockManager.restrictPlayStore.collectAsState()
    val restrictDeveloperOptions by SideloadLockManager.restrictDeveloperOptions.collectAsState()
    val hasKey by SideloadLockManager.hasSecurityKey.collectAsState()

    var showSetKeyDialog by remember { mutableStateOf(false) }
    var showVerifyKeyDialog by remember { mutableStateOf(false) }
    var showVerifyBeforeReconfigureDialog by remember { mutableStateOf(false) }
    var pendingToggleActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Sideload & ADB Lock",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Prohibit APK sideloading, Play Store & debugging",
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
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Main Status Banner Card (M3 Expressive)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isRestrictionActive) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (isRestrictionActive) colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isRestrictionActive) colorScheme.primary else colorScheme.secondaryContainer,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(
                                    if (isRestrictionActive) R.drawable.shield_fill0 else R.drawable.block_fill0
                                ),
                                contentDescription = null,
                                tint = if (isRestrictionActive) colorScheme.onPrimary else colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRestrictionActive) "Lockdown Active" else "Restrictions Inactive",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isRestrictionActive) colorScheme.onPrimaryContainer else colorScheme.onSurface
                        )
                        Text(
                            text = if (isRestrictionActive) {
                                "Installation and ADB features are strictly restricted by device policy"
                            } else {
                                "Turn on to enforce device policy restrictions"
                            },
                            style = typography.bodySmall,
                            color = if (isRestrictionActive) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isRestrictionActive,
                        onCheckedChange = { targetState ->
                            if (!isPrivileged) {
                                Toast.makeText(
                                    context,
                                    "Device Owner or Dhizuku privilege is required",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Switch
                            }

                            if (targetState) {
                                // Turning on
                                if (!hasKey) {
                                    pendingToggleActive = true
                                    showSetKeyDialog = true
                                } else {
                                    val res = SideloadLockManager.setRestrictionActive(context, true)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Sideload & ADB lock activated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to apply restriction", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Turning off -> Must verify password/key
                                if (hasKey) {
                                    showVerifyKeyDialog = true
                                } else {
                                    val res = SideloadLockManager.setRestrictionActive(context, false)
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Restrictions lifted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.onPrimary,
                            checkedTrackColor = colorScheme.primary
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // SECTION 1: RESTRICTION RULES
            TomatoSectionHeader(title = "POLICY RESTRICTIONS")

            // 1. Prohibit Unknown Sources / Sideloading
            TomatoSettingsItem(
                title = "Block Unknown Sources",
                subtitle = "Prohibit installing APKs from browsers, file managers & downloads",
                leadingIcon = R.drawable.block_fill0,
                shape = segmentedItemShape(0, 3),
                trailingContent = {
                    Switch(
                        checked = restrictUnknownSources,
                        onCheckedChange = { checked ->
                            if (isRestrictionActive && hasKey) {
                                Toast.makeText(context, "Deactivate lock to alter individual rules", Toast.LENGTH_SHORT).show()
                            } else {
                                SideloadLockManager.setRestrictUnknownSources(context, checked)
                            }
                        }
                    )
                }
            )

            // 2. Prohibit Play Store App Installations
            TomatoSettingsItem(
                title = "Block Play Store Installations",
                subtitle = "Prevent installing new applications from Google Play or app stores",
                leadingIcon = R.drawable.apps_fill0,
                shape = segmentedItemShape(1, 3),
                trailingContent = {
                    Switch(
                        checked = restrictPlayStore,
                        onCheckedChange = { checked ->
                            if (isRestrictionActive && hasKey) {
                                Toast.makeText(context, "Deactivate lock to alter individual rules", Toast.LENGTH_SHORT).show()
                            } else {
                                SideloadLockManager.setRestrictPlayStore(context, checked)
                            }
                        }
                    )
                }
            )

            // 3. Prohibit Developer Options & ADB
            TomatoSettingsItem(
                title = "Disable Developer Options & ADB",
                subtitle = "Revoke USB debugging, wireless ADB & hide developer menu",
                leadingIcon = R.drawable.tools_fill0,
                shape = segmentedItemShape(2, 3),
                trailingContent = {
                    Switch(
                        checked = restrictDeveloperOptions,
                        onCheckedChange = { checked ->
                            if (isRestrictionActive && hasKey) {
                                Toast.makeText(context, "Deactivate lock to alter individual rules", Toast.LENGTH_SHORT).show()
                            } else {
                                SideloadLockManager.setRestrictDeveloperOptions(context, checked)
                            }
                        }
                    )
                }
            )

            Spacer(Modifier.height(18.dp))

            // SECTION 2: SECURITY & AUTHENTICATION
            TomatoSectionHeader(title = "SECURITY KEY")

            TomatoSettingsItem(
                title = if (hasKey) "Security Key Configured" else "Configure Security Key",
                subtitle = if (hasKey) {
                    "Guards toggles and prevents unauthorized deactivation • Tap to reconfigure"
                } else {
                    "Set a password or QR key required to lift lockdown"
                },
                leadingIcon = R.drawable.key_fill0,
                shape = segmentedItemShape(0, 1),
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasKey) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hasKey) "Active" else "Not Set",
                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasKey) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                },
                onClick = {
                    if (isRestrictionActive && hasKey) {
                        Toast.makeText(context, "Lift restrictions first to modify the key", Toast.LENGTH_SHORT).show()
                    } else if (hasKey) {
                        showVerifyBeforeReconfigureDialog = true
                    } else {
                        showSetKeyDialog = true
                    }
                }
            )

            Spacer(Modifier.height(18.dp))

            // Informational Notice Card (Greyscale M3 style)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shield_fill0),
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(top = 2.dp),
                        tint = colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Device Policy Protection",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "When active, Android's Device Policy Manager restricts debugging interfaces immediately. Ensure you keep your password or emergency QR key accessible so you can unlock anytime.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    if (showSetKeyDialog) {
        SetEmergencyKeyDialog(
            savedKeys = savedKeys,
            title = "Set Sideload Lock Key",
            subtitle = "Set a secure password or QR key. This key will be required whenever you wish to disable Sideload & ADB restrictions.",
            onDismiss = {
                showSetKeyDialog = false
                pendingToggleActive = false
            },
            onSaveKey = { keyType, secret ->
                SideloadLockManager.setSecurityKey(context, keyType, secret)
                Toast.makeText(context, "Security key saved", Toast.LENGTH_SHORT).show()
                if (pendingToggleActive) {
                    val res = SideloadLockManager.setRestrictionActive(context, true)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Sideload & ADB lock activated", Toast.LENGTH_SHORT).show()
                    }
                    pendingToggleActive = false
                }
            },
            onKeySet = {
                showSetKeyDialog = false
            }
        )
    }

    if (showVerifyBeforeReconfigureDialog) {
        VerifyEmergencyKeyDialog(
            title = "Verify Current Key",
            subtitle = if (SideloadLockManager.getKeyType(context) == KeyType.QR_CODE) {
                "Scan or enter your existing QR key to modify Sideload & ADB security."
            } else {
                "Enter your existing password to modify Sideload & ADB security."
            },
            keyTypeOverride = SideloadLockManager.getKeyType(context),
            onVerifyCustom = { input ->
                SideloadLockManager.verifySecurityKey(context, input)
            },
            onDismiss = { showVerifyBeforeReconfigureDialog = false },
            onVerified = {
                showVerifyBeforeReconfigureDialog = false
                showSetKeyDialog = true
            }
        )
    }

    if (showVerifyKeyDialog) {
        VerifyEmergencyKeyDialog(
            title = "Lift Restrictions",
            subtitle = "Enter your security password or scan your key to disable Sideload & ADB lockdown.",
            keyTypeOverride = SideloadLockManager.getKeyType(context),
            onVerifyCustom = { input ->
                SideloadLockManager.verifySecurityKey(context, input)
            },
            onDismiss = { showVerifyKeyDialog = false },
            onVerified = {
                showVerifyKeyDialog = false
                val res = SideloadLockManager.setRestrictionActive(context, false)
                if (res.isSuccess) {
                    Toast.makeText(context, "Restrictions lifted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to lift restrictions", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
