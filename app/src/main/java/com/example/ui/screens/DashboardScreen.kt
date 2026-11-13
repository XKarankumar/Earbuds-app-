package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.DashboardCardId
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalAppThemeId
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onOpenCard: (DashboardCardId) -> Unit
) {
    val prefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()
    val rateSnapshot by viewModel.rateLimitSnapshot.collectAsStateWithLifecycle()
    val themeId = LocalAppThemeId.current
    val glass = LocalGlassPalette.current
    var quickNumber by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp
        val columns = if (isWide) 4 else 2

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Brand Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_karan_finder_fg_1794567636995),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = "Karan Number Finder",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Pro AI • Guest Mode Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (prefs.pinLockEnabled) {
                            IconButton(
                                onClick = viewModel::lockAppNow,
                                modifier = Modifier.testTag("btn_lock_vault")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = stringResource(R.string.cd_lock_app)
                                )
                            }
                        }
                        IconButton(
                            onClick = viewModel::toggleQuickTheme,
                            modifier = Modifier.testTag("btn_toggle_theme")
                        ) {
                            Icon(
                                imageVector = if (themeId.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = stringResource(R.string.cd_theme_toggle)
                            )
                        }
                    }
                }
            }

            // Hero Glass Card with Quick Number Lookup
            item {
                GlassCard(
                    elevated = true,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                    ) {
                        val heroRes = if (themeId.isDark) {
                            R.drawable.img_karan_dark_hero_1794567638092
                        } else {
                            R.drawable.img_karan_white_hero_1794567639255
                        }
                        Image(
                            painter = painterResource(heroRes),
                            contentDescription = stringResource(R.string.cd_hero_banner),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = glass.safeAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "HTTPS + App Check • ${history.size} Encrypted Records",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.safeAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Verify Numbers, Photos & Scams Safely",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickNumber,
                                onValueChange = { quickNumber = it },
                                placeholder = { Text("+91 98XXX XXXXX or 1930") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (quickNumber.isNotBlank()) {
                                            viewModel.jumpToNumberLookup(quickNumber)
                                        }
                                    }
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_quick_input")
                            )
                            Button(
                                onClick = {
                                    if (quickNumber.isNotBlank()) {
                                        viewModel.jumpToNumberLookup(quickNumber)
                                    } else {
                                        onOpenCard(DashboardCardId.NUMBER_LOOKUP)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("dashboard_quick_search_btn")
                            ) {
                                Text("Scan")
                            }
                        }

                        Text(
                            text = "Privacy Shield: Never exposes private home addresses, Aadhaar, banking info, or live location.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 8 Dashboard Cards Grid
            item {
                Text(
                    text = "Intelligence Modules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            val rows = DashboardCardId.entries.chunked(columns)
            items(rows.size) { rowIdx ->
                val rowCards = rows[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowCards.forEach { card ->
                        DashboardModuleCard(
                            card = card,
                            historyCount = history.size,
                            rateRemaining = (rateSnapshot.maxPerMinute - rateSnapshot.requestsInLastMinute).coerceAtLeast(0),
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenCard(card) }
                        )
                    }
                    repeat(columns - rowCards.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Footer Branding
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.brand_copyright),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Karan Number Finder Pro AI • Version 1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardModuleCard(
    card: DashboardCardId,
    historyCount: Int,
    rateRemaining: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val glass = LocalGlassPalette.current
    val (icon, accent) = cardVisuals(card, glass.infoAccent, glass.safeAccent, glass.warningAccent, glass.dangerAccent)
    val dynamicBadge = when (card) {
        DashboardCardId.SEARCH_HISTORY -> "$historyCount Saved"
        DashboardCardId.SECURITY_CENTER -> "$rateRemaining/min Left"
        else -> card.badge
    }

    GlassCard(
        modifier = modifier.testTag("card_${card.name.lowercase()}"),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = card.title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = accent.copy(alpha = 0.14f)
            ) {
                Text(
                    text = dynamicBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = card.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = card.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun cardVisuals(
    card: DashboardCardId,
    info: Color,
    safe: Color,
    warn: Color,
    danger: Color
): Pair<ImageVector, Color> = when (card) {
    DashboardCardId.NUMBER_LOOKUP -> Icons.Default.PhoneInTalk to info
    DashboardCardId.PHOTO_SCANNER -> Icons.Default.DocumentScanner to safe
    DashboardCardId.SPAM_DETECTOR -> Icons.Default.Shield to danger
    DashboardCardId.AI_ASSISTANT -> Icons.Default.SmartToy to info
    DashboardCardId.BUSINESS_LOOKUP -> Icons.Default.Business to warn
    DashboardCardId.SEARCH_HISTORY -> Icons.Default.History to safe
    DashboardCardId.SECURITY_CENTER -> Icons.Default.Security to safe
    DashboardCardId.SETTINGS -> Icons.Default.Settings to info
}
