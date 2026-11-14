package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExerciseCatalog
import com.example.domain.FitnessCalculators
import com.example.model.AppDestination
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WorkoutScreen(
    viewModel: MainViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    val planItems by viewModel.workoutPlan.collectAsStateWithLifecycle()
    val routines by viewModel.customRoutines.collectAsStateWithLifecycle()
    val history by viewModel.workoutHistory.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()

    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Planner, 1=Routines, 2=History
    var selectedDay by remember { mutableStateOf(FitnessCalculators.currentDayCode()) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showCreateRoutineDialog by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setWorkoutReminders(true, prefs.workoutReminderHour, prefs.workoutReminderMinute)
        } else {
            viewModel.emitToast("Notification permission needed for workout reminders")
        }
    }

    val todayKey = FitnessCalculators.todayDateKey()
    val dayItems = planItems.filter { it.dayOfWeek == selectedDay }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Personal Workout", style = MaterialTheme.typography.headlineMedium, color = palette.primaryText)
                Text("Daily planner, custom splits, reminders & training history", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
            }

            item {
                GlassCard(elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = palette.accentCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Workout Reminders", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                                Text(
                                    "Daily schedule at %02d:%02d".format(prefs.workoutReminderHour, prefs.workoutReminderMinute),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryText
                                )
                            }
                        }
                        Switch(
                            checked = prefs.workoutReminderEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Switch
                                    }
                                }
                                viewModel.setWorkoutReminders(enable, prefs.workoutReminderHour, prefs.workoutReminderMinute)
                            }
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedTab == 0, onClick = { selectedTab = 0 }, label = { Text("Daily Planner") })
                    FilterChip(selected = selectedTab == 1, onClick = { selectedTab = 1 }, label = { Text("Routines (${routines.size})") })
                    FilterChip(selected = selectedTab == 2, onClick = { selectedTab = 2 }, label = { Text("History (${history.size})") })
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            days.forEach { d ->
                                FilterChip(
                                    selected = selectedDay == d,
                                    onClick = { selectedDay = d },
                                    label = { Text(d) }
                                )
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            GlassPillButton(
                                text = "Add Exercise",
                                icon = Icons.Filled.Add,
                                onClick = { showAddExerciseDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_workout_exercise_btn")
                            )
                            GlassPillButton(
                                text = "Finish Session",
                                icon = Icons.Filled.PlayArrow,
                                accent = palette.accentEmerald,
                                onClick = {
                                    viewModel.completeWorkoutSession("$selectedDay Workout", dayItems, 45)
                                },
                                enabled = dayItems.isNotEmpty(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (dayItems.isEmpty()) {
                        item {
                            GlassCard {
                                Text("Rest Day / No exercises planned for $selectedDay", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                                Text("Tap 'Add Exercise' to build your $selectedDay split.", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                            }
                        }
                    } else {
                        items(dayItems, key = { it.id }) { item ->
                            val done = item.lastCompletedDateKey == todayKey
                            GlassCard(
                                onClick = { onNavigate(AppDestination.ExerciseDetail(item.exerciseId)) },
                                accentBorder = if (done) palette.accentEmerald else null
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    IconButton(onClick = { viewModel.toggleWorkoutItemCompleted(item) }) {
                                        Icon(
                                            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            contentDescription = "Toggle exercise completed",
                                            tint = if (done) palette.accentEmerald else palette.secondaryText
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.exerciseName, style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                                        Text(
                                            "${item.muscleGroup} • ${item.targetSets}×${item.targetReps} • Rest ${item.restSeconds}s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.secondaryText
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteWorkoutPlanItem(item.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete planned exercise", tint = palette.accentDanger)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        GlassPillButton(
                            text = "Create Custom Routine",
                            icon = Icons.Filled.Add,
                            onClick = { showCreateRoutineDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(routines, key = { it.id }) { r ->
                        val ids = r.exerciseIdsCsv.split(",").filter { it.isNotBlank() }
                        GlassCard(elevated = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.name, style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                                    Text("${r.focusArea} • ${ids.size} exercises • ~${r.estimatedMinutes} min", style = MaterialTheme.typography.bodySmall, color = palette.accentCyan)
                                }
                                IconButton(onClick = { viewModel.deleteCustomRoutine(r.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete routine", tint = palette.accentDanger)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            GlassPillButton(
                                text = "Load into $selectedDay & Log",
                                onClick = {
                                    val exList = ids.mapNotNull { ExerciseCatalog.findById(it) }
                                    exList.forEach { viewModel.addExerciseToPlan(selectedDay, r.name, it) }
                                    selectedTab = 0
                                },
                                filled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                else -> {
                    if (history.isEmpty()) {
                        item {
                            GlassCard {
                                Text("No completed sessions yet", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                            }
                        }
                    } else {
                        items(history, key = { it.id }) { h ->
                            GlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(h.routineName, style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                                        Text(
                                            "${h.exerciseCount} exercises • ${h.totalSets} sets • ${h.durationMinutes} min",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.secondaryText
                                        )
                                        Text(
                                            FitnessCalculators.formatDateTimeShort(h.completedAtMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.accentCyan
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${h.caloriesBurned} kcal", style = MaterialTheme.typography.titleSmall, color = palette.accentEmerald)
                                        Text("${h.totalVolumeKg.toInt()} kg vol", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Add 3D Exercise to $selectedDay") },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExerciseCatalog.allExercises, key = { it.id }) { ex ->
                        GlassCard(
                            onClick = {
                                viewModel.addExerciseToPlan(selectedDay, "${ex.muscleGroup.displayName} Split", ex)
                                showAddExerciseDialog = false
                            },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(ex.name, style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                            Text("${ex.muscleGroup.displayName} • ${ex.defaultSets}×${ex.defaultReps}", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) { Text("Close") }
            }
        )
    }

    if (showCreateRoutineDialog) {
        var rName by remember { mutableStateOf("") }
        var rFocus by remember { mutableStateOf("") }
        val picked = remember { mutableStateListOf<String>() }
        AlertDialog(
            onDismissRequest = { showCreateRoutineDialog = false },
            title = { Text("Create Custom Routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rName, onValueChange = { rName = it }, label = { Text("Routine Name") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(value = rFocus, onValueChange = { rFocus = it }, label = { Text("Target Focus") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    Text("Select Exercises:", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.height(210.dp)) {
                        items(ExerciseCatalog.allExercises, key = { it.id }) { ex ->
                            val checked = ex.id in picked
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { if (it) picked.add(ex.id) else picked.remove(ex.id) }
                                )
                                Text("${ex.name} (${ex.muscleGroup.shortLabel})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createCustomRoutine(rName, rFocus.ifBlank { "Custom Split" }, picked.toList())
                    showCreateRoutineDialog = false
                }) { Text("Save Routine") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoutineDialog = false }) { Text("Cancel") }
            }
        )
    }
}
