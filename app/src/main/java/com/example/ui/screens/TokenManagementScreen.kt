package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FitnessCalculators
import com.example.model.UserRole
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.GlassTopHeader
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TokenManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalGlassPalette.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val tokens by viewModel.accessTokens.collectAsStateWithLifecycle()
    val audits by viewModel.tokenAudits.collectAsStateWithLifecycle()
    val lastPlainToken by viewModel.lastIssuedPlainToken.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
        GlassTopHeader(
            title = "Token Management",
            subtitle = "Zero-Hardcoded-Secret Security • Offline Lease Cache",
            onBack = onBack,
            trailing = {
                IconButton(onClick = { viewModel.signOutToken() }) {
                    Icon(Icons.Filled.Logout, contentDescription = "Lock & Sign Out", tint = palette.accentAmber)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(elevated = true, accentBorder = palette.accentCyan) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = palette.accentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Session Lease", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Token: ${session.maskedToken} (${session.role.label})", style = MaterialTheme.typography.bodyMedium, color = palette.primaryText)
                    Text(
                        "Token Expiry: ${FitnessCalculators.formatDateShort(session.expiresAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accentEmerald
                    )
                    Text(
                        "Offline Lease Until: ${FitnessCalculators.formatDateTimeShort(session.offlineLeaseUntilMillis)} • ${if (session.deviceBound) "One-Device Bound" else "Multi-Device"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                }
            }

            if (lastPlainToken != null) {
                item {
                    GlassCard(elevated = true, accentBorder = palette.accentEmerald) {
                        Text("Newly Issued Token (Shown Once)", style = MaterialTheme.typography.titleSmall, color = palette.accentEmerald)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(lastPlainToken!!, style = MaterialTheme.typography.labelLarge, color = palette.primaryText)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassPillButton(
                                text = "Copy Token",
                                icon = Icons.Filled.ContentCopy,
                                accent = palette.accentEmerald,
                                onClick = {
                                    clipboard.setText(AnnotatedString(lastPlainToken!!))
                                    viewModel.emitToast("Copied token to clipboard")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearLastPlainToken() }) { Text("Dismiss") }
                        }
                    }
                }
            }

            item {
                GlassPillButton(
                    text = "Generate Unique Access Token",
                    icon = Icons.Filled.Add,
                    onClick = { showCreateDialog = true },
                    enabled = session.role == UserRole.ADMIN,
                    modifier = Modifier.fillMaxWidth().testTag("generate_token_btn")
                )
            }

            item {
                Text("Issued Tokens (${tokens.size})", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
            }

            items(tokens, key = { it.id }) { tok ->
                val expired = tok.expiresAtMillis <= System.currentTimeMillis()
                val statusColor = when {
                    tok.isRevoked -> palette.accentDanger
                    expired -> palette.accentAmber
                    else -> palette.accentEmerald
                }
                GlassCard(accentBorder = statusColor.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${tok.label} (${tok.role})", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                            Text(tok.maskedToken, style = MaterialTheme.typography.labelMedium, color = palette.accentCyan)
                            Text(
                                when {
                                    tok.isRevoked -> "REVOKED"
                                    expired -> "EXPIRED"
                                    else -> "Valid until ${FitnessCalculators.formatDateShort(tok.expiresAtMillis)}"
                                } + if (!tok.deviceBindingHash.isNullOrBlank()) " • Device-Bound" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )
                        }
                        if (session.role == UserRole.ADMIN && !tok.isRevoked) {
                            Row {
                                IconButton(onClick = { viewModel.renewToken(tok, 30) }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Renew token +30d", tint = palette.accentEmerald)
                                }
                                IconButton(onClick = { viewModel.revokeToken(tok) }) {
                                    Icon(Icons.Filled.Block, contentDescription = "Revoke compromised token", tint = palette.accentDanger)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Token Activation & Audit History", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
            }

            items(audits.take(20), key = { it.id }) { a ->
                GlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${a.action} • ${a.maskedToken}", style = MaterialTheme.typography.labelMedium, color = if (a.success) palette.accentEmerald else palette.accentDanger)
                        Text(FitnessCalculators.formatDateTimeShort(a.timestampMillis), style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                    }
                    Text("${a.detail} (Device ${a.deviceHashShort})", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                }
            }
        }
    }

    if (showCreateDialog) {
        var role by remember { mutableStateOf(UserRole.MEMBER) }
        var label by remember { mutableStateOf("") }
        var days by remember { mutableIntStateOf(30) }
        var bindDevice by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Generate Access Token") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        UserRole.entries.forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.label) })
                        }
                    }
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Token Label / Member Name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7, 30, 90, 365).forEach { d ->
                            FilterChip(selected = days == d, onClick = { days = d }, label = { Text("${d}d") })
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("One-Device Binding", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = bindDevice, onCheckedChange = { bindDevice = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.generateNewToken(role, label, days, bindDevice)
                    showCreateDialog = false
                }) { Text("Issue Token") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }
}
