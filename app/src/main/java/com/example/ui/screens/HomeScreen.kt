package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.FitnessCalculators
import com.example.model.AppDestination
import com.example.ui.components.AnimatedGlassRing
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricGlassTile
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigate: (AppDestination) -> Unit
) {
    val palette = LocalGlassPalette.current
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val water by viewModel.waterToday.collectAsStateWithLifecycle()
    val workoutPlan by viewModel.workoutPlan.collectAsStateWithLifecycle()

    val todayKey = FitnessCalculators.todayDateKey()
    val todayDay = FitnessCalculators.currentDayCode()
    val todayPlan = workoutPlan.filter { it.dayOfWeek == todayDay }
    val completedSetsToday = todayPlan.count { it.lastCompletedDateKey == todayKey }
    val todayAttendanceCount = attendance.count { it.dateKey == todayKey && it.status != "ABSENT" }
    val loggedCalories = meals.filter { it.dateKey == todayKey && it.isLogged }.sumOf { it.calories }
    val loggedProtein = meals.filter { it.dateKey == todayKey && it.isLogged }.sumOf { it.proteinG }
    val waterMl = water?.intakeMl ?: 0
    val revenue = payments.filter { it.isPaid }.sumOf { it.amount }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prefs.gymName,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = palette.accentEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${session.role.label} Session • ${session.maskedToken.ifBlank { "Active" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentCyan
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { onNavigate(AppDestination.Profile) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_profile_btn")
                                .clip(CircleShape)
                                .background(palette.glassFill)
                                .border(1.dp, palette.glassBorderTop, CircleShape)
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile", tint = palette.primaryText)
                        }
                        IconButton(
                            onClick = { onNavigate(AppDestination.Settings) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_settings_btn")
                                .clip(CircleShape)
                                .background(palette.glassFill)
                                .border(1.dp, palette.glassBorderTop, CircleShape)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = palette.primaryText)
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, palette.glassBorderTop, RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_fitness_hero_3d_1794653577654),
                        contentDescription = "Fitness Hub 3D glass hero banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Black.copy(alpha = 0.86f), Color.Black.copy(alpha = 0.25f))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TODAY'S FOCUS • $todayDay",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentCyan
                            )
                            Text(
                                text = "${todayPlan.size} Scheduled Exercises",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "$completedSetsToday completed • $loggedCalories/${prefs.dailyCalorieTarget} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Text("Daily Biometrics", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AnimatedGlassRing(
                            progress = loggedCalories.toFloat() / prefs.dailyCalorieTarget.coerceAtLeast(1),
                            centerLabel = "$loggedCalories",
                            subLabel = "kcal",
                            accent = palette.accentCyan
                        )
                        AnimatedGlassRing(
                            progress = loggedProtein.toFloat() / prefs.dailyProteinTargetG.coerceAtLeast(1),
                            centerLabel = "${loggedProtein}g",
                            subLabel = "Protein",
                            accent = palette.accentEmerald
                        )
                        AnimatedGlassRing(
                            progress = waterMl.toFloat() / prefs.dailyWaterTargetMl.coerceAtLeast(1),
                            centerLabel = "${waterMl}ml",
                            subLabel = "Water",
                            accent = Color(0xFF60A5FA)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricGlassTile(
                        title = "MEMBERS",
                        value = "${members.count { it.isActive }}",
                        subtitle = "Active Profiles",
                        icon = Icons.Filled.Group,
                        accent = palette.accentCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Members) }
                    )
                    MetricGlassTile(
                        title = "CHECK-INS",
                        value = "$todayAttendanceCount",
                        subtitle = "Present Today",
                        icon = Icons.Filled.QrCodeScanner,
                        accent = palette.accentEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.Attendance) }
                    )
                    MetricGlassTile(
                        title = "REVENUE",
                        value = FitnessCalculators.formatInr(revenue),
                        subtitle = "Collected",
                        icon = Icons.Filled.Business,
                        accent = palette.accentAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.GymManagement) }
                    )
                }
            }

            item {
                Text(
                    text = "All 10 Modules",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.primaryText
                )
            }

            val modules: List<Triple<String, Pair<String, ImageVector>, AppDestination>> = listOf(
                Triple("3D Exercises", "10 Muscle Groups" to Icons.Filled.FitnessCenter, AppDestination.Exercises),
                Triple("Workout Planner", "Routines & History" to Icons.Filled.Badge, AppDestination.Workout),
                Triple("Diet & Macros", "BMI, Water & Charts" to Icons.Filled.Restaurant, AppDestination.Diet),
                Triple("Members", "OTP & QR Passes" to Icons.Filled.Group, AppDestination.Members),
                Triple("Attendance", "QR & Missed Scan" to Icons.Filled.QrCodeScanner, AppDestination.Attendance),
                Triple("Gym Management", "Fees, PDF & SMS" to Icons.Filled.Business, AppDestination.GymManagement),
                Triple("Token Manager", "Keys, Leases & Audit" to Icons.Filled.Key, AppDestination.TokenManagement),
                Triple("Profile", "Member ID & Goals" to Icons.Filled.AccountCircle, AppDestination.Profile),
                Triple("Settings", "Theme, Gateway & Wipe" to Icons.Filled.Settings, AppDestination.Settings)
            )

            items(modules.chunked(2).size) { rowIdx ->
                val pair = modules.chunked(2)[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (title, meta, dest) ->
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("module_card_${title.lowercase().replace(" ", "_")}"),
                            onClick = { onNavigate(dest) },
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(palette.accentCyan.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(meta.second, contentDescription = null, tint = palette.accentCyan, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.titleSmall, color = palette.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(meta.first, style = MaterialTheme.typography.bodySmall, color = palette.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.addWater(250) },
                    accentBorder = Color(0xFF60A5FA)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalDrink, contentDescription = null, tint = Color(0xFF60A5FA))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Quick Hydration Boost", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                                Text("Tap to log +250 ml ($waterMl / ${prefs.dailyWaterTargetMl} ml)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                            }
                        }
                        Text("+250 ml", style = MaterialTheme.typography.labelLarge, color = Color(0xFF60A5FA))
                    }
                }
            }
        }
    }
}
