package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FitnessCalculators
import com.example.model.AttendanceStatus
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.GlassTopHeader
import com.example.ui.components.MetricGlassTile
import com.example.ui.components.QrScannerDialog
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalGlassPalette.current
    val members by viewModel.members.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val scanReport by viewModel.lastMissedScanReport.collectAsStateWithLifecycle()

    var showQrScanner by remember { mutableStateOf(false) }

    val todayKey = FitnessCalculators.todayDateKey()
    val todayDay = FitnessCalculators.currentDayCode()
    val todayLogs = attendance.filter { it.dateKey == todayKey }
    val presentIds = todayLogs.filter { it.status != AttendanceStatus.ABSENT.name }.map { it.memberId }.toSet()
    val scheduledToday = members.filter { it.isActive && todayDay in it.scheduledDays }

    Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
        GlassTopHeader(
            title = "Attendance & QR Check-In",
            subtitle = "$todayKey ($todayDay) • Deadline %02d:%02d".format(prefs.attendanceDeadlineHour, prefs.attendanceDeadlineMinute),
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricGlassTile(
                        title = "SCHEDULED",
                        value = "${scheduledToday.size}",
                        subtitle = "Expected $todayDay",
                        icon = Icons.Filled.Schedule,
                        accent = palette.accentCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricGlassTile(
                        title = "CHECKED IN",
                        value = "${presentIds.size}",
                        subtitle = "${(scheduledToday.size - presentIds.count { id -> scheduledToday.any { it.id == id } }).coerceAtLeast(0)} missing",
                        icon = Icons.Filled.Check,
                        accent = palette.accentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                GlassPillButton(
                    text = "Scan Member QR Pass (Camera / ID)",
                    icon = Icons.Filled.QrCodeScanner,
                    onClick = { showQrScanner = true },
                    modifier = Modifier.fillMaxWidth().testTag("open_qr_scanner_btn")
                )
            }

            item {
                GlassCard(elevated = true, accentBorder = palette.accentAmber) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = palette.accentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Missed Gym Detection Engine", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Compares scheduled days against today's check-ins after %02d:%02d, respects member SMS opt-outs, and prevents duplicate reminders."
                            .format(prefs.attendanceDeadlineHour, prefs.attendanceDeadlineMinute),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                    if (scanReport != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Last Scan: ${scanReport!!.missingCount} missing • ${scanReport!!.sentCount} notified • ${scanReport!!.skippedDuplicateCount} duplicates blocked",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accentCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassPillButton(
                            text = "Check Deadline",
                            filled = false,
                            onClick = { viewModel.runMissedGymDetectionScan(forceIgnoreDeadline = false) },
                            modifier = Modifier.weight(1f)
                        )
                        GlassPillButton(
                            text = "Notify Missing Now",
                            accent = palette.accentAmber,
                            onClick = { viewModel.runMissedGymDetectionScan(forceIgnoreDeadline = true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Text("Mark Daily Attendance", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
            }

            items(members, key = { it.id }) { m ->
                val checkedIn = m.id in presentIds
                GlassCard(accentBorder = if (checkedIn) palette.accentEmerald else null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.fullName, style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                            Text(
                                "${m.memberCode} • Days: ${m.scheduledDays.joinToString(",")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryText
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = checkedIn,
                                onClick = { viewModel.markAttendanceManual(m.id, AttendanceStatus.PRESENT) },
                                label = { Text(if (checkedIn) "Present ✓" else "Present") }
                            )
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.markAttendanceManual(m.id, AttendanceStatus.ABSENT) },
                                label = { Text("Absent") }
                            )
                        }
                    }
                }
            }

            if (todayLogs.isNotEmpty()) {
                item {
                    Text("Today's Check-In Log", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                }
                items(todayLogs, key = { it.id }) { log ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${log.memberName} (${log.method})", style = MaterialTheme.typography.bodyMedium, color = palette.primaryText)
                            Text(
                                "${log.status} • ${FitnessCalculators.formatDateTimeShort(log.checkInMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.accentCyan
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onMemberCodeScanned = { code ->
                showQrScanner = false
                viewModel.checkInByQrCode(code)
            }
        )
    }
}
