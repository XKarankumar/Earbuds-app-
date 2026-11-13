package com.example.ui.components

import android.Manifest
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.permissions.PermissionManager
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

data class ScannedBillInfo(
    val detectedLabel: String,
    val extractedAmount: String?,
    val rawText: String
)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScannerSheet(
    cameraFeatureEnabled: Boolean,
    onDismiss: () -> Unit,
    onResultExtracted: (ScannedBillInfo) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(PermissionManager.isGranted(context, Manifest.permission.CAMERA))
    }
    var latestScan by remember { mutableStateOf<ScannedBillInfo?>(null) }
    var scanStatus by remember { mutableStateOf("Point camera at a UPI QR code or paper bill, or pick a photo.") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scanStatus = "Analyzing bill image..."
            analyzePickedImage(
                context = context,
                uri = uri,
                onResult = { info ->
                    latestScan = info
                    scanStatus = info.detectedLabel
                },
                onError = { msg -> scanStatus = msg }
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Bill & UPI QR Scanner",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (cameraFeatureEnabled && hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    CameraScannerPreview(
                        onDetected = { info ->
                            latestScan = info
                            scanStatus = info.detectedLabel
                        }
                    )
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (!cameraFeatureEnabled) {
                            "Camera scanner is disabled in Permission Manager. You can re-enable it or pick a bill image via Android Photo Picker."
                        } else {
                            "Camera permission is requested only when scanning live bills or QR codes."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cameraFeatureEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.testTag("grant_camera_btn")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Enable Camera Scanner")
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = scanStatus,
                    style = MaterialTheme.typography.bodyMedium
                )
                latestScan?.let { info ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = info.rawText.take(160),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    info.extractedAmount?.let { amt ->
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onResultExtracted(info)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Amount ₹$amt in Calculator")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_bill_photo_btn")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Bill Photo (Zero-Permission Photo Picker)")
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScannerPreview(
    onDetected: (ScannedBillInfo) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            barcodeScanner.close()
            textRecognizer.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var lastAnalyzedMs = 0L
                analysis.setAnalyzer(executor) { imageProxy ->
                    val now = System.currentTimeMillis()
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && now - lastAnalyzedMs > 900L) {
                        lastAnalyzedMs = now
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val rawQr = barcodes.firstOrNull()?.rawValue
                                if (!rawQr.isNullOrBlank()) {
                                    onDetected(parseScannedQrOrText(rawQr, isQr = true))
                                } else {
                                    textRecognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            if (visionText.text.isNotBlank()) {
                                                val parsed = parseScannedQrOrText(visionText.text, isQr = false)
                                                if (parsed.extractedAmount != null) {
                                                    onDetected(parsed)
                                                }
                                            }
                                        }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun analyzePickedImage(
    context: android.content.Context,
    uri: Uri,
    onResult: (ScannedBillInfo) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val barcodeScanner = BarcodeScanning.getClient()
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val qr = barcodes.firstOrNull()?.rawValue
                if (!qr.isNullOrBlank()) {
                    onResult(parseScannedQrOrText(qr, isQr = true))
                } else {
                    textRecognizer.process(image)
                        .addOnSuccessListener { text ->
                            if (text.text.isBlank()) {
                                onError("No readable QR code or bill amount found in image.")
                            } else {
                                onResult(parseScannedQrOrText(text.text, isQr = false))
                            }
                        }
                        .addOnFailureListener {
                            onError("Could not read text from photo.")
                        }
                }
            }
            .addOnFailureListener {
                onError("Could not analyze selected image.")
            }
    } catch (e: Exception) {
        onError("Unable to open image: ${e.localizedMessage ?: "error"}")
    }
}

private fun parseScannedQrOrText(raw: String, isQr: Boolean): ScannedBillInfo {
    if (isQr && raw.startsWith("upi://", ignoreCase = true)) {
        val uri = Uri.parse(raw)
        val payee = uri.getQueryParameter("pn") ?: uri.getQueryParameter("pa") ?: "UPI Payee"
        val amount = uri.getQueryParameter("am")
        return ScannedBillInfo(
            detectedLabel = "UPI QR Detected: $payee" + (if (amount != null) " (₹$amount)" else ""),
            extractedAmount = amount,
            rawText = raw
        )
    }
    // Extract largest currency-like number from bill OCR text
    val regex = Regex("(?:₹|Rs\\.?|INR|Total|TOTAL|Amount|AMOUNT)?\\s*([0-9]{1,7}(?:\\.[0-9]{1,2})?)")
    val numbers = regex.findAll(raw)
        .mapNotNull { it.groupValues.getOrNull(1)?.toDoubleOrNull() }
        .filter { it > 0.0 }
        .toList()
    val best = numbers.maxOrNull()
    val bestStr = best?.let {
        if (it % 1.0 == 0.0) it.toLong().toString() else String.format(java.util.Locale.US, "%.2f", it)
    }
    return ScannedBillInfo(
        detectedLabel = if (bestStr != null) "Bill Total Detected: ₹$bestStr" else if (isQr) "QR Scanned" else "Bill Text Scanned",
        extractedAmount = bestStr,
        rawText = raw
    )
}
