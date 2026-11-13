package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AsyncUiState
import com.example.permissions.PermissionManager
import com.example.ui.components.AiRadarLoadingCard
import com.example.ui.components.GlassCard
import com.example.ui.components.SourceAttributionBanner
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoScannerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val selectedUri by viewModel.selectedPhotoUri.collectAsStateWithLifecycle()
    val photoState by viewModel.photoState.collectAsStateWithLifecycle()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.scanPhotoUri(uri)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val uri = pendingCameraUri
            if (success && uri != null) {
                viewModel.scanPhotoUri(uri)
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val uri = createTempCameraUri(context)
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                viewModel.emitToast("Camera permission denied. You can still use Gallery Photo Picker.")
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 920.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_back_photo")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Photo & OCR Scanner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "On-Device OCR + Multimodal Vision (Zero Facial ID)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_pick_gallery")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Gallery")
                        }

                        OutlinedButton(
                            onClick = {
                                if (!prefs.cameraScanFeatureEnabled) {
                                    viewModel.emitToast("Camera feature is disabled in Security Center.")
                                } else if (PermissionManager.isGranted(context, Manifest.permission.CAMERA)) {
                                    val uri = createTempCameraUri(context)
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_capture_camera")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Camera")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Photos are analyzed only when you select them. No background uploads, no facial identification, and no private photo search.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedUri != null) {
                item {
                    GlassCard(contentPadding = PaddingValues(10.dp)) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Selected photo for AI OCR analysis",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }

            when (val state = photoState) {
                is AsyncUiState.Idle -> {
                    item {
                        GlassCard {
                            Text(
                                text = "Select a business card, storefront sign, invoice, or screenshot to extract phone numbers, read visible text via ML Kit OCR, and inspect objects safely.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is AsyncUiState.Loading -> {
                    item { AiRadarLoadingCard(message = state.stepMessage) }
                }

                is AsyncUiState.Error -> {
                    item {
                        GlassCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                is AsyncUiState.Success -> {
                    val report = state.data
                    item {
                        GlassCard(elevated = true) {
                            Text(
                                text = "Extracted Phone Numbers (${report.extractedPhoneNumbers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            if (report.extractedPhoneNumbers.isEmpty()) {
                                Text(
                                    text = "No valid phone numbers detected in this image.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    report.extractedPhoneNumbers.forEach { phone ->
                                        AssistChip(
                                            onClick = { viewModel.jumpToNumberLookup(phone) },
                                            label = { Text("Lookup $phone") },
                                            leadingIcon = {
                                                Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "AI Image Description",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(report.imageDescription, style = MaterialTheme.typography.bodyMedium)

                            if (report.detectedObjects.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Detected Objects & Elements",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    report.detectedObjects.forEach { obj ->
                                        AssistChip(onClick = {}, label = { Text(obj) })
                                    }
                                }
                            }

                            if (report.visibleTextSummary.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Recognized OCR Text",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(report.visibleTextSummary, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Authorized Public Source & Safety",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(report.publicSourceGuidance, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    item {
                        SourceAttributionBanner(
                            confidenceLevel = report.confidenceLevel,
                            sourceAttribution = report.sourceAttribution
                        )
                    }
                }
            }
        }
    }
}

private fun createTempCameraUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
