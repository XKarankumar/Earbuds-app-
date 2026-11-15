package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hometheatre.core.AudioTelemetrySnapshot
import com.example.hometheatre.dj.DecodedAudioTrack
import com.example.hometheatre.dj.DjConsoleState
import com.example.hometheatre.dj.DjDeckUiState
import com.example.hometheatre.dj.PerformancePadType
import com.example.ui.components.GlassConsoleCard
import com.example.ui.components.InteractiveJogWheel
import com.example.ui.components.SpectrumVisualizerCanvas
import com.example.ui.components.StatusPill
import com.example.ui.components.WaveformSeekBar
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.TheatreViewModel

@Composable
fun DjConsoleScreen(
    viewModel: TheatreViewModel,
    djState: DjConsoleState,
    telemetry: AudioTelemetrySnapshot
) {
    val palette = LocalStudioPalette.current

    val pickDeckA = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.loadDjAudioFile("A", it) }
    }
    val pickDeckB = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.loadDjAudioFile("B", it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DUAL DECK DJ STUDIO",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Live 44.1kHz stereo mixer • Vinyl scratch • 16 Pads",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                    StatusPill(
                        text = if (djState.engineRunning) "Mixer Live" else "Standby",
                        active = djState.engineRunning
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                SpectrumVisualizerCanvas(telemetry = telemetry)
            }
        }

        // Responsive Dual Decks (Side-by-side on wide screens >= 720dp, stacked on phones)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 720.dp
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DjDeckPanel(
                            deck = djState.deckA,
                            builtInLibrary = djState.builtInLibrary,
                            accentColor = palette.primaryAccent,
                            viewModel = viewModel,
                            onPickFile = { pickDeckA.launch(arrayOf("audio/*")) },
                            modifier = Modifier.weight(1f)
                        )
                        DjDeckPanel(
                            deck = djState.deckB,
                            builtInLibrary = djState.builtInLibrary,
                            accentColor = palette.secondaryAccent,
                            viewModel = viewModel,
                            onPickFile = { pickDeckB.launch(arrayOf("audio/*")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DjDeckPanel(
                            deck = djState.deckA,
                            builtInLibrary = djState.builtInLibrary,
                            accentColor = palette.primaryAccent,
                            viewModel = viewModel,
                            onPickFile = { pickDeckA.launch(arrayOf("audio/*")) }
                        )
                        DjDeckPanel(
                            deck = djState.deckB,
                            builtInLibrary = djState.builtInLibrary,
                            accentColor = palette.secondaryAccent,
                            viewModel = viewModel,
                            onPickFile = { pickDeckB.launch(arrayOf("audio/*")) }
                        )
                    }
                }
            }
        }

        // Master DJ Mixer: Crossfader, Echo, Reverb
        item {
            GlassConsoleCard(highlighted = true) {
                Text(
                    text = "DJ MASTER MIXER & FX",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primaryAccent
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DECK A", style = MaterialTheme.typography.labelMedium, color = palette.primaryAccent)
                    Text(
                        "CROSSFADER (${"%.0f".format(djState.crossfader * 100)}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary
                    )
                    Text("DECK B", style = MaterialTheme.typography.labelMedium, color = palette.secondaryAccent)
                }
                Slider(
                    value = djState.crossfader,
                    onValueChange = viewModel::setDjCrossfader,
                    valueRange = -1f..1f,
                    modifier = Modifier.testTag("dj_crossfader_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = palette.textPrimary,
                        activeTrackColor = palette.primaryAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Echo Delay (${(djState.echoMix * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                        Slider(
                            value = djState.echoMix,
                            onValueChange = viewModel::setDjEcho,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.primaryAccent,
                                activeTrackColor = palette.primaryAccent
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Space Reverb (${(djState.reverbMix * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                        Slider(
                            value = djState.reverbMix,
                            onValueChange = viewModel::setDjReverb,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.secondaryAccent,
                                activeTrackColor = palette.secondaryAccent
                            )
                        )
                    }
                }
            }
        }

        // 16 Performance Pads (4x4 Grid)
        item {
            GlassConsoleCard {
                Text(
                    text = "16 LIVE PERFORMANCE PADS",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "Tap any pad to synthesize & trigger real-time drums, bass drops, and FX into the mix",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val pads = PerformancePadType.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 4) {
                                val pad = pads[row * 4 + col]
                                val padColor = when (pad.category) {
                                    "Drums" -> palette.primaryAccent
                                    "Bass" -> palette.secondaryAccent
                                    "Synth" -> palette.successEmerald
                                    else -> palette.warningAmber
                                }
                                val flashed = djState.activePadFlashId == pad.padNumber
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (flashed) padColor.copy(alpha = 0.42f)
                                            else palette.surfaceElevated
                                        )
                                        .border(
                                            1.dp,
                                            padColor.copy(alpha = if (flashed) 0.95f else 0.45f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.triggerDjPad(pad) }
                                        .testTag("dj_pad_${pad.padNumber}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = pad.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = pad.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = padColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DjDeckPanel(
    deck: DjDeckUiState,
    builtInLibrary: List<DecodedAudioTrack>,
    accentColor: Color,
    viewModel: TheatreViewModel,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var stemMenuExpanded by remember { mutableStateOf(false) }

    GlassConsoleCard(modifier = modifier, highlighted = deck.isPlaying) {
        // Header + Load Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DECK ${deck.deckId} • ${"%.1f".format(deck.effectiveBpm)} BPM",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = deck.trackTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                OutlinedButton(
                    onClick = { stemMenuExpanded = true },
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stems")
                }
                DropdownMenu(
                    expanded = stemMenuExpanded,
                    onDismissRequest = { stemMenuExpanded = false }
                ) {
                    builtInLibrary.forEach { stem ->
                        DropdownMenuItem(
                            text = { Text("${stem.title} (${stem.detectedBpm.toInt()} BPM)") },
                            onClick = {
                                stemMenuExpanded = false
                                viewModel.loadDjBuiltInTrack(deck.deckId, stem.id)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = onPickFile,
                border = BorderStroke(1.dp, palette.borderStroke)
            ) {
                Icon(Icons.Filled.AudioFile, contentDescription = "Pick audio file", modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        WaveformSeekBar(
            peaks = deck.waveformPeaks,
            progress = deck.progress,
            cueNormalized = deck.cuePointNormalized,
            hotCues = deck.hotCuesNormalized,
            accentColor = accentColor,
            onSeekNormalized = { viewModel.seekDjDeck(deck.deckId, it) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InteractiveJogWheel(
                deckLabel = deck.deckId,
                isPlaying = deck.isPlaying,
                progress = deck.progress,
                accentColor = accentColor,
                onScratchDelta = { delta -> viewModel.scratchDjDeck(deck.deckId, delta) }
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleDjPlayPause(deck.deckId) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dj_play_${deck.deckId}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (deck.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play or pause Deck ${deck.deckId}"
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.triggerDjCue(deck.deckId) },
                        modifier = Modifier.testTag("dj_cue_${deck.deckId}")
                    ) {
                        Text("CUE")
                    }
                    OutlinedButton(
                        onClick = { viewModel.syncDjDeck(deck.deckId) },
                        modifier = Modifier.testTag("dj_sync_${deck.deckId}")
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = "Beat sync Deck ${deck.deckId}", modifier = Modifier.size(16.dp))
                    }
                }

                // Loop 1 / 2 / 4 / 8 beats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 4, 8).forEach { beats ->
                        FilterChip(
                            selected = deck.loopBeats == beats,
                            onClick = { viewModel.toggleDjLoop(deck.deckId, beats) },
                            label = { Text("${beats}B") }
                        )
                    }
                }

                // 4 Hot Cues
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (slot in 0..3) {
                        val isSet = deck.hotCuesNormalized.getOrNull(slot) != null
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSet) accentColor.copy(alpha = 0.28f)
                                    else palette.surfaceElevated
                                )
                                .border(
                                    1.dp,
                                    if (isSet) accentColor else palette.borderStroke,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.triggerDjHotCue(deck.deckId, slot) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "HC${slot + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSet) accentColor else palette.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        // Tempo & Filter + 3-Band EQ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Tempo ${"%.0f".format((deck.tempoMultiplier - 1f) * 100)}%",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )
            Text(
                "Filter ${if (deck.filterSweep < -0.05f) "LPF" else if (deck.filterSweep > 0.05f) "HPF" else "OFF"}",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Slider(
                value = deck.tempoMultiplier,
                onValueChange = { viewModel.setDjTempo(deck.deckId, it) },
                valueRange = 0.75f..1.25f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
            )
            Slider(
                value = deck.filterSweep,
                onValueChange = { viewModel.setDjDeckEq(deck.deckId, filter = it) },
                valueRange = -1f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeckMiniEqSlider("LOW", deck.lowEqDb, accentColor, Modifier.weight(1f)) {
                viewModel.setDjDeckEq(deck.deckId, low = it)
            }
            DeckMiniEqSlider("MID", deck.midEqDb, accentColor, Modifier.weight(1f)) {
                viewModel.setDjDeckEq(deck.deckId, mid = it)
            }
            DeckMiniEqSlider("HIGH", deck.highEqDb, accentColor, Modifier.weight(1f)) {
                viewModel.setDjDeckEq(deck.deckId, high = it)
            }
        }
    }
}

@Composable
private fun DeckMiniEqSlider(
    label: String,
    valueDb: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$label ${if (valueDb >= 0f) "+" else ""}${valueDb.toInt()}dB",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
        Slider(
            value = valueDb,
            onValueChange = onChange,
            valueRange = -12f..12f,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
        )
    }
}
