package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FitnessCalculators
import com.example.model.ActivityLevel
import com.example.model.FitnessGoalType
import com.example.model.MealSlot
import com.example.ui.components.BarProgressChart
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.WeightTrendChart
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DietScreen(viewModel: MainViewModel) {
    val palette = LocalGlassPalette.current
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val water by viewModel.waterToday.collectAsStateWithLifecycle()
    val weightLogs by viewModel.weightLogs.collectAsStateWithLifecycle()
    val workoutHistory by viewModel.workoutHistory.collectAsStateWithLifecycle()

    var activeSection by remember { mutableIntStateOf(0) } // 0=Diet & Water, 1=Calculators, 2=Weight & Progress
    var showAddMealDialog by remember { mutableStateOf(false) }

    val todayKey = FitnessCalculators.todayDateKey()
    val todayMeals = meals.filter { it.dateKey == todayKey }
    val consumedCal = todayMeals.filter { it.isLogged }.sumOf { it.calories }
    val consumedProt = todayMeals.filter { it.isLogged }.sumOf { it.proteinG }
    val waterMl = water?.intakeMl ?: 0

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Diet, Hydration & Biometrics", style = MaterialTheme.typography.headlineMedium, color = palette.primaryText)
                Text("Meal planner, Calories/Protein/BMI calculators, Water & Weight charts", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = activeSection == 0, onClick = { activeSection = 0 }, label = { Text("Diet & Water") })
                    FilterChip(selected = activeSection == 1, onClick = { activeSection = 1 }, label = { Text("Calculators") })
                    FilterChip(selected = activeSection == 2, onClick = { activeSection = 2 }, label = { Text("Progress") })
                }
            }

            when (activeSection) {
                0 -> {
                    item {
                        GlassCard(elevated = true, accentBorder = Color(0xFF60A5FA)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocalDrink, contentDescription = null, tint = Color(0xFF60A5FA))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Water Tracker", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                                        Text("$waterMl / ${prefs.dailyWaterTargetMl} ml", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60A5FA))
                                    }
                                }
                                IconButton(onClick = { viewModel.resetWater() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Reset water", tint = palette.secondaryText)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (waterMl.toFloat() / prefs.dailyWaterTargetMl.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = Color(0xFF60A5FA)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassPillButton(text = "+250 ml", onClick = { viewModel.addWater(250) }, filled = false, modifier = Modifier.weight(1f))
                                GlassPillButton(text = "+500 ml", onClick = { viewModel.addWater(500) }, filled = false, modifier = Modifier.weight(1f))
                                GlassPillButton(text = "+750 ml", onClick = { viewModel.addWater(750) }, filled = false, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        GlassCard(elevated = true) {
                            Text("Today's Macro Adherence", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Calories: $consumedCal / ${prefs.dailyCalorieTarget} kcal", style = MaterialTheme.typography.bodyMedium, color = palette.accentCyan)
                            LinearProgressIndicator(
                                progress = { (consumedCal.toFloat() / prefs.dailyCalorieTarget.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = palette.accentCyan
                            )
                            Text("Protein: ${consumedProt}g / ${prefs.dailyProteinTargetG}g", style = MaterialTheme.typography.bodyMedium, color = palette.accentEmerald)
                            LinearProgressIndicator(
                                progress = { (consumedProt.toFloat() / prefs.dailyProteinTargetG.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = palette.accentEmerald
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            GlassPillButton(
                                text = "Add Meal to Plan",
                                icon = Icons.Filled.Add,
                                onClick = { showAddMealDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("add_meal_btn")
                            )
                        }
                    }

                    items(todayMeals, key = { it.id }) { meal ->
                        GlassCard(accentBorder = if (meal.isLogged) palette.accentEmerald else null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = { viewModel.toggleMeal(meal.id, meal.isLogged) }) {
                                    Icon(
                                        imageVector = if (meal.isLogged) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = "Toggle meal logged",
                                        tint = if (meal.isLogged) palette.accentEmerald else palette.secondaryText
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${meal.mealSlot.replace("_", " ")} • ${meal.foodName}", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                                    Text("${meal.calories} kcal • P ${meal.proteinG}g • C ${meal.carbsG}g • F ${meal.fatsG}g", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                                }
                                IconButton(onClick = { viewModel.deleteMeal(meal.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete meal", tint = palette.accentDanger)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        var heightInput by remember(prefs.userHeightCm) { mutableStateOf(prefs.userHeightCm.toInt().toString()) }
                        var weightInput by remember(prefs.userWeightKg) { mutableStateOf(prefs.userWeightKg.toInt().toString()) }
                        var ageInput by remember(prefs.userAge) { mutableStateOf(prefs.userAge.toString()) }
                        var isMale by remember(prefs.userIsMale) { mutableStateOf(prefs.userIsMale) }
                        var selectedGoal by remember(prefs.fitnessGoal) { mutableStateOf(prefs.fitnessGoal) }
                        var selectedActivity by remember(prefs.activityLevel) { mutableStateOf(prefs.activityLevel) }

                        val hCm = heightInput.toFloatOrNull() ?: prefs.userHeightCm
                        val wKg = weightInput.toFloatOrNull() ?: prefs.userWeightKg
                        val age = ageInput.toIntOrNull() ?: prefs.userAge
                        val bmiRes = FitnessCalculators.calculateBmi(wKg, hCm)
                        val macroRes = FitnessCalculators.calculateNutritionTargets(age, isMale, wKg, hCm, selectedActivity, selectedGoal)

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassCard(elevated = true, accentBorder = palette.accentCyan) {
                                Text("BMI & Body Composition Calculator", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = heightInput,
                                        onValueChange = { heightInput = it },
                                        label = { Text("Height (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = weightInput,
                                        onValueChange = { weightInput = it },
                                        label = { Text("Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = ageInput,
                                        onValueChange = { ageInput = it },
                                        label = { Text("Age") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.75f),
                                        singleLine = true
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "BMI: ${bmiRes.bmi} — ${bmiRes.category}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = palette.accentCyan
                                )
                                Text(
                                    "Healthy weight range: ${bmiRes.healthyMinKg}–${bmiRes.healthyMaxKg} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { bmiRes.normalizedGauge },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = palette.accentCyan
                                )
                            }

                            GlassCard(elevated = true, accentBorder = palette.accentEmerald) {
                                Text("Calories & Protein Calculator", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = isMale, onClick = { isMale = true }, label = { Text("Male") })
                                    FilterChip(selected = !isMale, onClick = { isMale = false }, label = { Text("Female") })
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    FitnessGoalType.entries.take(3).forEach { g ->
                                        FilterChip(selected = selectedGoal == g, onClick = { selectedGoal = g }, label = { Text(g.label) })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    ActivityLevel.entries.take(3).forEach { a ->
                                        FilterChip(selected = selectedActivity == a, onClick = { selectedActivity = a }, label = { Text(a.name.lowercase().replaceFirstChar { it.uppercase() }) })
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Daily Target: ${macroRes.targetCalories} kcal (BMR ${macroRes.bmr} • TDEE ${macroRes.tdee})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.accentEmerald
                                )
                                Text(
                                    "Protein: ${macroRes.proteinGrams}g  •  Carbs: ${macroRes.carbsGrams}g  •  Fats: ${macroRes.fatsGrams}g",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.primaryText
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                GlassPillButton(
                                    text = "Apply Targets to My Goals",
                                    accent = palette.accentEmerald,
                                    onClick = {
                                        viewModel.updateGoals(
                                            age = age,
                                            isMale = isMale,
                                            heightCm = hCm,
                                            weightKg = wKg,
                                            targetWeightKg = prefs.targetWeightKg,
                                            activity = selectedActivity,
                                            goal = selectedGoal,
                                            waterTargetMl = prefs.dailyWaterTargetMl,
                                            calorieTarget = macroRes.targetCalories,
                                            proteinTargetG = macroRes.proteinGrams,
                                            weeklyDays = prefs.weeklyWorkoutTargetDays
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                else -> {
                    item {
                        var newWeight by remember { mutableStateOf("") }
                        var newBf by remember { mutableStateOf("") }
                        var note by remember { mutableStateOf("") }
                        GlassCard(elevated = true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = palette.accentCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Weight & Body Fat", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newWeight,
                                    onValueChange = { newWeight = it },
                                    label = { Text("Weight (kg)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = newBf,
                                    onValueChange = { newBf = it },
                                    label = { Text("Body Fat %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Progress note (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            GlassPillButton(
                                text = "Save Weigh-In",
                                onClick = {
                                    val w = newWeight.toFloatOrNull()
                                    if (w != null && w in 25f..300f) {
                                        viewModel.logWeightEntry(w, newBf.toFloatOrNull() ?: 0f, note)
                                        newWeight = ""
                                        newBf = ""
                                        note = ""
                                    } else {
                                        viewModel.emitToast("Enter valid weight between 25 and 300 kg")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        GlassCard(elevated = true) {
                            Text("Weight Progress Chart", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                            WeightTrendChart(weights = weightLogs.map { it.weightKg }, accent = palette.accentCyan)
                        }
                    }

                    item {
                        GlassCard(elevated = true) {
                            Text("Workout Volume Progress", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                            Spacer(modifier = Modifier.height(8.dp))
                            val bars = workoutHistory.take(6).reversed().mapIndexed { idx, w ->
                                "S${idx + 1}" to w.caloriesBurned.toFloat()
                            }.ifEmpty { listOf("Mon" to 320f, "Wed" to 410f, "Fri" to 380f) }
                            BarProgressChart(bars = bars, accent = palette.accentEmerald)
                        }
                    }
                }
            }
        }
    }

    if (showAddMealDialog) {
        var slot by remember { mutableStateOf(MealSlot.LUNCH) }
        var food by remember { mutableStateOf("") }
        var cal by remember { mutableStateOf("") }
        var prot by remember { mutableStateOf("") }
        var carbs by remember { mutableStateOf("") }
        var fats by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddMealDialog = false },
            title = { Text("Log Meal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MealSlot.entries.take(3).forEach { s ->
                            FilterChip(selected = slot == s, onClick = { slot = s }, label = { Text(s.label) })
                        }
                    }
                    OutlinedTextField(value = food, onValueChange = { food = it }, label = { Text("Meal Name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = cal, onValueChange = { cal = it }, label = { Text("kcal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = prot, onValueChange = { prot = it }, label = { Text("Prot g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = fats, onValueChange = { fats = it }, label = { Text("Fat g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMeal(
                        slot = slot,
                        foodName = food,
                        calories = cal.toIntOrNull() ?: 350,
                        protein = prot.toIntOrNull() ?: 25,
                        carbs = carbs.toIntOrNull() ?: 35,
                        fats = fats.toIntOrNull() ?: 10
                    )
                    showAddMealDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddMealDialog = false }) { Text("Cancel") } }
        )
    }
}
