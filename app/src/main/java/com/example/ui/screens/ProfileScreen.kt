package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FitnessCalculators
import com.example.domain.QrCodeGenerator
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTopHeader
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalGlassPalette.current
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val primaryMember = members.firstOrNull()
    val bmi = FitnessCalculators.calculateBmi(prefs.userWeightKg, prefs.userHeightCm)
    val qrBmp = remember(primaryMember?.memberCode ?: prefs.activeMemberCode) {
        QrCodeGenerator.generateQrBitmap(
            QrCodeGenerator.buildMemberQrPayload(primaryMember?.memberCode ?: prefs.activeMemberCode),
            340
        )
    }

    Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
        GlassTopHeader(
            title = "Member & Athlete Profile",
            subtitle = "${prefs.gymName} • ${session.role.label}",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(elevated = true, accentBorder = palette.accentCyan) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(palette.accentCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile Avatar", tint = palette.accentCyan, modifier = Modifier.size(38.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(primaryMember?.fullName ?: prefs.ownerSignature, style = MaterialTheme.typography.titleLarge, color = palette.primaryText)
                            Text("ID: ${primaryMember?.memberCode ?: prefs.activeMemberCode}", style = MaterialTheme.typography.labelMedium, color = palette.accentCyan)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = palette.accentEmerald, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Session: ${session.role.label} • ${session.maskedToken}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.accentEmerald
                                )
                            }
                        }
                    }
                }
            }

            if (qrBmp != null) {
                item {
                    GlassCard(elevated = true) {
                        Text("Quick QR Gym Pass", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = qrBmp.asImageBitmap(),
                                contentDescription = "Member QR Check-In Pass",
                                modifier = Modifier
                                    .size(168.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }

            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = palette.accentEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personal Fitness Goals", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Goal: ${prefs.fitnessGoal.label} • Activity: ${prefs.activityLevel.label}", style = MaterialTheme.typography.bodyMedium, color = palette.primaryText)
                    Text("Current Weight: ${prefs.userWeightKg} kg → Target: ${prefs.targetWeightKg} kg", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                    Text("BMI: ${bmi.bmi} (${bmi.category})", style = MaterialTheme.typography.bodySmall, color = palette.accentCyan)
                    Text("Daily Targets: ${prefs.dailyCalorieTarget} kcal • ${prefs.dailyProteinTargetG}g protein • ${prefs.dailyWaterTargetMl} ml water", style = MaterialTheme.typography.bodySmall, color = palette.accentEmerald)
                }
            }
        }
    }
}
