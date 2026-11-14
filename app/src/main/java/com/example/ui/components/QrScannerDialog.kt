package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.QrCodeGenerator
import com.example.ui.theme.LocalGlassPalette
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onMemberCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalGlassPalette.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var manualCode by remember { mutableStateOf("") }
    var scannedOnce by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (palette.isDark) Color(0xFF0A0D12) else Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.glassBorderTop)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = palette.accentCyan)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("QR Check-In Scanner", style = MaterialTheme.typography.titleLarge, color = palette.primaryText)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close scanner", tint = palette.primaryText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (hasCameraPerm) {
                    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                    val scanner = remember { BarcodeScanning.getClient() }

                    DisposableEffect(Unit) {
                        onDispose {
                            cameraExecutor.shutdown()
                            scanner.close()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(2.dp, palette.accentCyan, RoundedCornerShape(20.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { imgAnalysis ->
                                            imgAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                                processCameraFrame(imageProxy, scanner, scannedOnce) { raw ->
                                                    scannedOnce = true
                                                    onMemberCodeScanned(QrCodeGenerator.parseMemberCodeFromQr(raw))
                                                }
                                            }
                                        }
                                    runCatching {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis
                                        )
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.glassFill)
                            .border(1.dp, palette.glassBorderTop, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Camera permission is requested only when scanning member QR passes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GlassPillButton(
                            text = "Allow Camera for QR Scan",
                            onClick = { permLauncher.launch(Manifest.permission.CAMERA) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Or enter Member ID / QR payload directly:",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    placeholder = { Text("e.g. FH-MEM-1001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassPillButton(
                    text = "Check In Member",
                    onClick = {
                        if (manualCode.isNotBlank()) {
                            onMemberCodeScanned(QrCodeGenerator.parseMemberCodeFromQr(manualCode))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processCameraFrame(
    imageProxy: androidx.camera.core.ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    alreadyScanned: Boolean,
    onFound: (String) -> Unit
) {
    val mediaImg = imageProxy.image
    if (mediaImg != null && !alreadyScanned) {
        val inputImg = InputImage.fromMediaImage(mediaImg, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImg)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstOrNull()?.rawValue
                if (!raw.isNullOrBlank()) {
                    onFound(raw)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}
