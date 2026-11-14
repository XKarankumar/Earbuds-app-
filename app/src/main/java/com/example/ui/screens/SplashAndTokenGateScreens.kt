package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.TokenSessionState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.theme.LocalGlassPalette

@Composable
fun SplashScreen() {
    val palette = LocalGlassPalette.current
    val pulse by rememberInfiniteTransition(label = "splash_pulse").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .scale(pulse)
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(palette.accentCyan, Color.White.copy(alpha = 0.25f), palette.accentEmerald)),
                        shape = RoundedCornerShape(34.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_fitness_hub_fg_1794653577273),
                    contentDescription = "Fitness Hub emblem",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "FITNESS HUB",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "3D Training • Gym Management • Token Security",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.accentCyan,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Version 1.0  •  Developer: Hardik Kumar",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun TokenGateScreen(
    session: TokenSessionState,
    onVerifyToken: (String) -> Unit,
    onInitializeOwner: () -> Unit
) {
    val palette = LocalGlassPalette.current
    var tokenInput by remember { mutableStateOf("") }
    val lockedOut = session.lockedOutUntilMillis > System.currentTimeMillis()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, palette.glassBorderTop, RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_fitness_hero_3d_1794653577654),
                    contentDescription = "Fitness Hub 3D AMOLED studio",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text("FITNESS HUB", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(
                            "Token-Based App Access • Zero Hardcoded Master Keys",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.accentCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlassCard(
                elevated = true,
                modifier = Modifier.fillMaxWidth(),
                accentBorder = palette.accentCyan
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.accentCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = palette.accentCyan)
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text("Activate Fitness Hub", style = MaterialTheme.typography.titleLarge, color = palette.primaryText)
                        Text(
                            "Enter an issued Admin, Trainer, or Member token",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Access Token") },
                    placeholder = { Text("FH-ADM-XXXX-XXXX-XXXX-CHK") },
                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = palette.accentCyan) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("token_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentCyan,
                        unfocusedBorderColor = palette.glassBorderTop,
                        focusedTextColor = palette.primaryText,
                        unfocusedTextColor = palette.primaryText
                    )
                )

                if (session.failedAttempts > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (lockedOut) {
                            "Invalid token protection active — wait before retrying."
                        } else {
                            "Failed attempts: ${session.failedAttempts}/5 before temporary lockout"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accentDanger
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                GlassPillButton(
                    text = "Verify & Activate Token",
                    icon = Icons.Filled.VerifiedUser,
                    onClick = { onVerifyToken(tokenInput) },
                    enabled = tokenInput.isNotBlank() && !lockedOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_token_button")
                )
            }

            if (!session.hasAnyAdminToken) {
                Spacer(modifier = Modifier.height(14.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = palette.accentEmerald
                ) {
                    Text(
                        "First-Time Device Setup",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "No admin token exists in this installation's AndroidKeyStore vault yet. Generate a unique hardware-bound Owner Admin token dynamically on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassPillButton(
                        text = "Initialize Owner Console",
                        icon = Icons.Filled.AdminPanelSettings,
                        accent = palette.accentEmerald,
                        onClick = onInitializeOwner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("init_owner_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Offline access uses time-limited AES-256-GCM cached leases. Developer: Hardik Kumar.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}
