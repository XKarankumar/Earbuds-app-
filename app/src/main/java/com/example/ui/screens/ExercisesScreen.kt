package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExerciseCatalog
import com.example.domain.FitnessCalculators
import com.example.model.AppDestination
import com.example.model.DifficultyLevel
import com.example.model.ExerciseItem
import com.example.model.MuscleGroup
import com.example.ui.components.Exercise3DCanvas
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.GlassSearchBar
import com.example.ui.components.GlassTopHeader
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun ExercisesScreen(
    onOpenExercise: (String) -> Unit
) {
    val palette = LocalGlassPalette.current
    var selectedGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var search by remember { mutableStateOf("") }

    val filtered = remember(selectedGroup, search) {
        ExerciseCatalog.allExercises.filter { ex ->
            val matchGroup = selectedGroup == null || ex.muscleGroup == selectedGroup
            val q = search.trim()
            val matchQuery = q.isBlank() ||
                ex.name.contains(q, ignoreCase = true) ||
                ex.equipment.contains(q, ignoreCase = true) ||
                ex.primaryMuscles.any { it.contains(q, ignoreCase = true) }
            matchGroup && matchQuery
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("3D Exercise Library", style = MaterialTheme.typography.headlineMedium, color = palette.primaryText)
                Text(
                    "33 animated 3D biomechanics guides across all 10 muscle groups",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, palette.glassBorderTop, RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_anatomy_3d_studio_1794653577984),
                        contentDescription = "3D anatomical muscle fiber studio render",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent)))
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text("INTERACTIVE 3D ANATOMY", style = MaterialTheme.typography.labelSmall, color = palette.accentCyan)
                            Text("Drag 360° • Target Muscle Glow", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Posture, Steps, Mistakes & Rest Timer", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
                        }
                    }
                }
            }

            item {
                GlassSearchBar(
                    query = search,
                    onQueryChange = { search = it },
                    placeholder = "Search exercises, muscles, equipment..."
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedGroup == null,
                        onClick = { selectedGroup = null },
                        label = { Text("All (${ExerciseCatalog.allExercises.size})") }
                    )
                    MuscleGroup.entries.forEach { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(group.displayName) },
                            modifier = Modifier.testTag("muscle_chip_${group.name.lowercase()}")
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { ex ->
                ExerciseLibraryCard(
                    exercise = ex,
                    onClick = { onOpenExercise(ex.id) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseLibraryCard(
    exercise: ExerciseItem,
    onClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val diffColor = when (exercise.difficulty) {
        DifficultyLevel.BEGINNER -> palette.accentEmerald
        DifficultyLevel.INTERMEDIATE -> palette.accentCyan
        DifficultyLevel.ADVANCED -> palette.accentAmber
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_card_${exercise.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(116.dp)) {
                Exercise3DCanvas(exercise = exercise, compact = true)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = exercise.muscleGroup.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentCyan
                )
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primaryText
                )
                Text(
                    text = "Primary: ${exercise.primaryMuscles.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
                Text(
                    text = "${exercise.equipment} • ${exercise.defaultSets}×${exercise.defaultReps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
                Text(
                    text = exercise.difficulty.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = diffColor
                )
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val exercise = remember(exerciseId) {
        ExerciseCatalog.findById(exerciseId) ?: ExerciseCatalog.allExercises.first()
    }
    val palette = LocalGlassPalette.current
    val haptic = LocalHapticFeedback.current

    var restRemaining by remember(exercise.id) { mutableIntStateOf(exercise.defaultRestSec) }
    var timerRunning by remember(exercise.id) { mutableStateOf(false) }

    LaunchedEffect(timerRunning, restRemaining) {
        if (timerRunning && restRemaining > 0) {
            delay(1000L)
            restRemaining -= 1
            if (restRemaining == 0) {
                timerRunning = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.emitToast("Rest timer complete! Ready for next set.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
    ) {
        GlassTopHeader(
            title = exercise.name,
            subtitle = "${exercise.muscleGroup.displayName} • ${exercise.difficulty.label}",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Exercise3DCanvas(exercise = exercise, compact = false)
            }

            item {
                GlassCard(elevated = true, accentBorder = palette.accentCyan) {
                    Text("Target Muscle Highlight", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Primary: ${exercise.primaryMuscles.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.accentCyan
                    )
                    Text(
                        "Secondary: ${exercise.secondaryMuscles.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("EQUIPMENT", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                            Text(exercise.equipment, style = MaterialTheme.typography.bodySmall, color = palette.primaryText)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column {
                            Text("SETS & REPS", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                            Text("${exercise.defaultSets} sets × ${exercise.defaultReps}", style = MaterialTheme.typography.titleSmall, color = palette.accentEmerald)
                        }
                        Column {
                            Text("REST", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                            Text("${exercise.defaultRestSec}s", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                        }
                        Column {
                            Text("BURN", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                            Text("~${exercise.caloriesPerSet} kcal/set", style = MaterialTheme.typography.titleSmall, color = palette.accentAmber)
                        }
                    }
                }
            }

            item {
                GlassCard(elevated = true, accentBorder = palette.accentEmerald) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = palette.accentEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rest Timer", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                        }
                        Text(
                            text = "%02d:%02d".format(restRemaining / 60, restRemaining % 60),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.accentEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPillButton(
                            text = if (timerRunning) "Pause" else "Start Rest",
                            icon = if (timerRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            accent = palette.accentEmerald,
                            onClick = { timerRunning = !timerRunning },
                            modifier = Modifier.weight(1f)
                        )
                        GlassPillButton(
                            text = "Reset",
                            icon = Icons.Filled.Refresh,
                            filled = false,
                            onClick = {
                                timerRunning = false
                                restRemaining = exercise.defaultRestSec
                            },
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                }
            }

            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = palette.accentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Correct Posture", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(exercise.postureGuide, style = MaterialTheme.typography.bodyMedium, color = palette.primaryText)
                }
            }

            item {
                GlassCard {
                    Text("Step-by-Step Instructions", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    exercise.steps.forEachIndexed { idx, step ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = palette.accentEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${idx + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = palette.primaryText)
                        }
                    }
                }
            }

            item {
                GlassCard(accentBorder = palette.accentDanger) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = palette.accentDanger)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Common Mistakes", style = MaterialTheme.typography.titleMedium, color = palette.accentDanger)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    exercise.commonMistakes.forEach { m ->
                        Text("• $m", style = MaterialTheme.typography.bodySmall, color = palette.primaryText, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            item {
                GlassCard(accentBorder = palette.accentAmber) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = palette.accentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Safety Instructions", style = MaterialTheme.typography.titleMedium, color = palette.accentAmber)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    exercise.safetyInstructions.forEach { s ->
                        Text("• $s", style = MaterialTheme.typography.bodySmall, color = palette.primaryText, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            item {
                GlassPillButton(
                    text = "Add to Today's Workout (${FitnessCalculators.currentDayCode()})",
                    icon = Icons.Filled.Add,
                    onClick = {
                        viewModel.addExerciseToPlan(
                            dayOfWeek = FitnessCalculators.currentDayCode(),
                            routineName = "${exercise.muscleGroup.displayName} Focus",
                            exercise = exercise
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_exercise_to_plan_btn")
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
