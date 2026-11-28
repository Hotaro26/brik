package com.hotaro.brik.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.util.EmergencyManager
import com.hotaro.brik.util.QrUtils
import java.util.UUID

@Composable
fun SetEmergencyKeyDialog(
    savedKeys: List<SavedKey> = emptyList(),
    title: String = "Set Emergency Key",
    subtitle: String = "Configure a secure key to guard Emergency Mode. This key will be required whenever you trigger or activate Emergency Mode.",
    onDismiss: () -> Unit,
    onSaveKey: ((KeyType, String) -> Unit)? = null,
    onKeySet: () -> Unit
) {
    val context = LocalContext.current
    var selectedKeyType by remember { mutableStateOf(KeyType.PASSWORD) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var qrMode by remember { mutableIntStateOf(0) } // 0: Random, 1: Scan External, 2: Saved Key
    val generatedQrToken = remember { UUID.randomUUID().toString() }
    var scannedQrToken by remember { mutableStateOf<String?>(null) }
    var selectedSavedKey by remember { mutableStateOf<SavedKey?>(savedKeys.firstOrNull()) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = subtitle,
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Key Type Segmented Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceContainerLowest)
                        .padding(4.dp)
                ) {
                    Surface(
                        onClick = {
                            selectedKeyType = KeyType.PASSWORD
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Password",
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            selectedKeyType = KeyType.QR_CODE
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "QR Code",
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (selectedKeyType == KeyType.PASSWORD) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Emergency Password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    painter = painterResource(if (isPasswordVisible) R.drawable.visibility_off_fill0 else R.drawable.visibility_fill0),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // QR Mode Segmented
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.surfaceContainerLowest)
                            .padding(2.dp)
                    ) {
                        Surface(
                            onClick = { qrMode = 0 },
                            shape = RoundedCornerShape(8.dp),
                            color = if (qrMode == 0) colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Random",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (qrMode == 0) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { qrMode = 1 },
                            shape = RoundedCornerShape(8.dp),
                            color = if (qrMode == 1) colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Scan Any",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (qrMode == 1) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (savedKeys.isNotEmpty()) {
                            Surface(
                                onClick = { qrMode = 2 },
                                shape = RoundedCornerShape(8.dp),
                                color = if (qrMode == 2) colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Saved Key",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (qrMode == 2) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (qrMode == 0) {
                        val qrBitmap = remember(generatedQrToken) {
                            try {
                                QrUtils.encodeToBitmap(generatedQrToken, 300)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Generated QR",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(6.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = generatedQrToken,
                                style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Brik Emergency QR", generatedQrToken))
                                    Toast.makeText(context, "QR token copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Copy QR Token", style = typography.labelSmall)
                            }
                        }
                    } else if (qrMode == 1) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (scannedQrToken == null) {
                                Button(
                                    onClick = { showScannerDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.qr_code_scanner_fill0),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Scan Any QR Code", style = typography.labelMedium)
                                }
                            } else {
                                Text(
                                    text = "Scanned Key Ready",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = scannedQrToken!!,
                                    style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showScannerDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Scan Again", style = typography.labelSmall)
                                }
                            }
                        }
                    } else if (qrMode == 2) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            savedKeys.forEach { sk ->
                                val isSelected = selectedSavedKey?.id == sk.id
                                Surface(
                                    onClick = { selectedSavedKey = sk },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sk.name,
                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(R.drawable.check_fill0),
                                                contentDescription = null,
                                                tint = colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        style = typography.bodySmall,
                        color = colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val secret: String
                    if (selectedKeyType == KeyType.PASSWORD) {
                        if (password.isBlank()) {
                            errorMessage = "Please enter a password"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }
                        secret = password.trim()
                    } else {
                        val s = when (qrMode) {
                            0 -> generatedQrToken
                            1 -> scannedQrToken
                            else -> selectedSavedKey?.token
                        }
                        if (s.isNullOrBlank()) {
                            errorMessage = "Please scan or select a QR key"
                            return@Button
                        }
                        secret = s
                    }

                    if (onSaveKey != null) {
                        onSaveKey(selectedKeyType, secret)
                    } else {
                        EmergencyManager.setEmergencyKey(context, selectedKeyType, secret)
                        EmergencyManager.setEmergencyModeActive(context, true)
                        Toast.makeText(context, "Emergency key set & Emergency Mode activated!", Toast.LENGTH_SHORT).show()
                    }
                    onKeySet()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Key & Activate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )

    if (showScannerDialog) {
        AlertDialog(
            onDismissRequest = { showScannerDialog = false },
            title = {
                Text("Scan External QR", fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    QrScannerView(
                        onQrDetected = { detected ->
                            scannedQrToken = detected
                            showScannerDialog = false
                            Toast.makeText(context, "QR scanned successfully!", Toast.LENGTH_SHORT).show()
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

@Composable
fun VerifyEmergencyKeyDialog(
    title: String = "Verify Emergency Key",
    subtitle: String? = null,
    keyTypeOverride: KeyType? = null,
    onVerifyCustom: ((String) -> Boolean)? = null,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val keyType = keyTypeOverride ?: remember(context) { EmergencyManager.getKeyType(context) }
    var inputSecret by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            showCameraScanner = false
            onDismiss()
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subtitle ?: if (keyType == KeyType.QR_CODE) {
                        "Scan your emergency QR code or paste its token to unlock."
                    } else {
                        "Enter your emergency password to unlock."
                    },
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                if (keyType == KeyType.QR_CODE) {
                    if (showCameraScanner) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            QrScannerView(
                                onQrDetected = { scannedToken ->
                                    val valid = if (onVerifyCustom != null) {
                                        onVerifyCustom(scannedToken)
                                    } else {
                                        val ok = EmergencyManager.verifyEmergencyKey(context, scannedToken)
                                        if (ok) EmergencyManager.setEmergencyModeActive(context, true)
                                        ok
                                    }
                                    if (valid) {
                                        showCameraScanner = false
                                        onVerified()
                                    } else {
                                        errorMessage = "Invalid QR key token"
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Button(
                            onClick = { showCameraScanner = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.qr_code_scanner_fill0),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Scan with Camera")
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("— or paste token text —", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputSecret,
                            onValueChange = {
                                inputSecret = it
                                errorMessage = null
                            },
                            placeholder = { Text("Paste QR token") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = inputSecret,
                        onValueChange = {
                            inputSecret = it
                            errorMessage = null
                        },
                        label = { Text("Security Password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    painter = painterResource(if (isPasswordVisible) R.drawable.visibility_off_fill0 else R.drawable.visibility_fill0),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        style = typography.bodySmall,
                        color = colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (!showCameraScanner) {
                Button(
                    onClick = {
                        val valid = if (onVerifyCustom != null) {
                            onVerifyCustom(inputSecret.trim())
                        } else {
                            val ok = EmergencyManager.verifyEmergencyKey(context, inputSecret.trim())
                            if (ok) EmergencyManager.setEmergencyModeActive(context, true)
                            ok
                        }
                        if (valid) {
                            onVerified()
                        } else {
                            errorMessage = "Incorrect key. Please try again."
                        }
                    },
                    enabled = inputSecret.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify & Continue")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    showCameraScanner = false
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = colorScheme.surfaceContainerHigh
    )
}
