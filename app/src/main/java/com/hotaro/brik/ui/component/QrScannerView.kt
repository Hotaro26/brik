package com.hotaro.brik.ui.component

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hotaro.brik.R
import com.hotaro.brik.util.QrUtils
import com.hotaro.brik.util.ScannerPreferences
import java.util.concurrent.Executors

@SuppressLint("ClickableViewAccessibility")
@Composable
fun QrScannerView(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val engine by ScannerPreferences.engine.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.qr_code_scanner_fill0),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Camera permission required to scan key or ISBN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Camera")
                }
            }
        }
    } else {
        val executor = remember { Executors.newSingleThreadExecutor() }
        val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }
        var cameraInstance by remember { mutableStateOf<Camera?>(null) }
        var isScanned by remember { mutableStateOf(false) }

        var isTorchOn by remember { mutableStateOf(false) }
        var currentZoomRatio by remember { mutableFloatStateOf(1f) }
        var maxZoomRatio by remember { mutableFloatStateOf(5f) }

        DisposableEffect(Unit) {
            onDispose {
                cameraProviderState.value?.unbindAll()
                executor.shutdown()
            }
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(colorScheme.surfaceContainerLowest)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val targetZoom = (currentZoomRatio * zoom).coerceIn(1f, maxZoomRatio)
                        currentZoomRatio = targetZoom
                        cameraInstance?.cameraControl?.setZoomRatio(targetZoom)
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        try {
                            val provider = future.get()
                            cameraProviderState.value = provider
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(executor) { proxy ->
                                proxy.use {
                                    if (!isScanned) {
                                        val detectedText = QrUtils.decodeImageProxy(it, engine)
                                        if (detectedText != null) {
                                            isScanned = true
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onQrDetected(detectedText)
                                            }
                                        }
                                    }
                                }
                            }
                            provider.unbindAll()
                            val cam = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                            cameraInstance = cam

                            cam.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                                if (state != null) {
                                    maxZoomRatio = state.maxZoomRatio.coerceAtMost(8f)
                                }
                            }

                            // Tap to focus
                            previewView.setOnTouchListener { v, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    val factory = previewView.meteringPointFactory
                                    val point = factory.createPoint(event.x, event.y)
                                    val action = FocusMeteringAction.Builder(point).build()
                                    cam.cameraControl.startFocusAndMetering(action)
                                    v.performClick()
                                    true
                                } else {
                                    false
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Central scanning viewfinder box overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .border(
                            width = 2.dp,
                            color = colorScheme.primary.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(18.dp)
                        )
                )
            }

            // Top Bar: Torch Toggle and Engine Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Engine pill
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (engine == com.hotaro.brik.util.QrEngine.GOOGLE_ML_KIT) Color(0xFF4CAF50) else Color(0xFF2196F3),
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Text(
                            text = if (engine == com.hotaro.brik.util.QrEngine.GOOGLE_ML_KIT) "ML Kit" else "ZXing",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Flashlight / Torch toggle button
                Surface(
                    shape = CircleShape,
                    color = if (isTorchOn) colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                isTorchOn = !isTorchOn
                                cameraInstance?.cameraControl?.enableTorch(isTorchOn)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flashlight),
                            contentDescription = "Toggle Torch",
                            tint = if (isTorchOn) colorScheme.onPrimary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom Bar: Zoom Controls & Scan Hint
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Zoom Selection Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1f, 2f, 3f).forEach { zoomLevel ->
                        val isSelected = kotlin.math.abs(currentZoomRatio - zoomLevel) < 0.25f
                        Surface(
                            onClick = {
                                currentZoomRatio = zoomLevel
                                cameraInstance?.cameraControl?.setZoomRatio(zoomLevel)
                            },
                            shape = CircleShape,
                            color = if (isSelected) colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${zoomLevel.toInt()}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colorScheme.onPrimary else Color.White
                                )
                            }
                        }
                    }
                }

                // Friendly format indicator badge
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "Scan QR Code or Book ISBN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
