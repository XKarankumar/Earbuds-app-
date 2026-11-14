package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.FitnessCalculators
import com.example.model.MembershipPlan
import com.example.model.PaymentMethod
import com.example.model.SmsEventType
import com.example.model.UserRole
import com.example.ui.components.BarProgressChart
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.GlassTopHeader
import com.example.ui.components.MetricGlassTile
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

@Composable
fun GymManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalGlassPalette.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val trainers by viewModel.trainers.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val smsLogs by viewModel.smsLogs.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) } // 0=Fees & Revenue, 1=Trainers, 2=SMS Center
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showTrainerDialog by remember { mutableStateOf(false) }
    var showTriggerSmsDialog by remember { mutableStateOf(false) }

    val collected = payments.filter { it.isPaid }.sumOf { it.amount }
    val pending = payments.filter { !it.isPaid }.sumOf { it.amount }
    val now = System.currentTimeMillis()
    val expiringSoon = members.filter { it.expiryDateMillis - now in 0L..(7L * 86_400_000L) }

    Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
        GlassTopHeader(
            title = "Gym Management",
            subtitle = "Role: ${session.role.label} • Fees, Trainers, Revenue & SMS",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, palette.glassBorderTop, RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gym_equipment_3d_1794653579812),
                        contentDescription = "3D gym equipment visual",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text("ADMIN COMMAND CENTER", style = MaterialTheme.typography.labelSmall, color = palette.accentCyan)
                            Text("${members.size} Members • ${trainers.size} Trainers", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text("Collected ${FitnessCalculators.formatInr(collected)} • Pending ${FitnessCalculators.formatInr(pending)}", style = MaterialTheme.typography.bodySmall, color = palette.accentEmerald)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricGlassTile(
                        title = "REVENUE",
                        value = FitnessCalculators.formatInr(collected),
                        subtitle = "${payments.count { it.isPaid }} receipts",
                        icon = Icons.Filled.Payments,
                        accent = palette.accentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricGlassTile(
                        title = "EXPIRING <=7D",
                        value = "${expiringSoon.size}",
                        subtitle = "Pending ${FitnessCalculators.formatInr(pending)}",
                        icon = Icons.Filled.Warning,
                        accent = palette.accentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Fees & Reports") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Trainers") })
                    FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("SMS System") })
                }
            }

            when (tab) {
                0 -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            GlassPillButton(
                                text = "Collect Fee",
                                icon = Icons.Filled.Add,
                                onClick = { showPaymentDialog = true },
                                enabled = session.role != UserRole.MEMBER,
                                modifier = Modifier.weight(1f).testTag("collect_fee_btn")
                            )
                            GlassPillButton(
                                text = "Revenue PDF",
                                icon = Icons.Filled.PictureAsPdf,
                                filled = false,
                                onClick = { viewModel.exportAndShareRevenueReport() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        GlassCard(elevated = true) {
                            Text("Revenue Breakdown by Plan", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                            Spacer(modifier = Modifier.height(8.dp))
                            val bars = MembershipPlan.entries.map { p ->
                                p.label to payments.filter { it.isPaid && it.planType == p.name }.sumOf { it.amount }.toFloat()
                            }
                            BarProgressChart(bars = bars, accent = palette.accentCyan)
                        }
                    }

                    if (expiringSoon.isNotEmpty()) {
                        item {
                            GlassCard(accentBorder = palette.accentAmber) {
                                Text("Membership Expiry Alerts", style = MaterialTheme.typography.titleSmall, color = palette.accentAmber)
                                expiringSoon.forEach { m ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${m.fullName} (${FitnessCalculators.formatDateShort(m.expiryDateMillis)})", style = MaterialTheme.typography.bodySmall, color = palette.primaryText)
                                        TextButton(onClick = {
                                            viewModel.triggerEventSms(m.id, SmsEventType.MEMBERSHIP_EXPIRY, launchComposer = true)
                                        }) { Text("Alert SMS") }
                                    }
                                }
                            }
                        }
                    }

                    items(payments, key = { it.id }) { p ->
                        GlassCard(accentBorder = if (p.isPaid) palette.accentEmerald else palette.accentDanger) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${p.memberName} • ${p.receiptNumber}", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                                    Text("${p.planType} • ${p.paymentMethod} • ${p.periodLabel}", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                                    Text(
                                        if (p.isPaid) "PAID ${FitnessCalculators.formatInr(p.amount)}" else "PENDING ${FitnessCalculators.formatInr(p.amount)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (p.isPaid) palette.accentEmerald else palette.accentDanger
                                    )
                                }
                                IconButton(onClick = { viewModel.exportAndShareReceipt(p) }) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF receipt", tint = palette.accentCyan)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        GlassPillButton(
                            text = "Add Gym Trainer",
                            icon = Icons.Filled.Add,
                            onClick = { showTrainerDialog = true },
                            enabled = session.role == UserRole.ADMIN,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(trainers, key = { it.id }) { tr ->
                        GlassCard(elevated = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.SportsGymnastics, contentDescription = null, tint = palette.accentCyan)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(tr.fullName, style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                                        Text("${tr.specialty} • ${tr.shift}", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                                        Text("${tr.countryCode} ${tr.phoneNumber} • Clients: ${tr.activeClients}", style = MaterialTheme.typography.labelSmall, color = palette.accentEmerald)
                                    }
                                }
                                if (session.role == UserRole.ADMIN) {
                                    IconButton(onClick = { viewModel.deleteTrainer(tr.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove trainer", tint = palette.accentDanger)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    item {
                        GlassCard(elevated = true, accentBorder = palette.accentCyan) {
                            Text("Automated SMS Gateway System", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                            Text(
                                "Supports Attendance Missing, Workout Missed, Membership Expiry, Fee Pending, Payment Confirmation, Registration & Token Expiry with duplicate blocking & delivery status.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            GlassPillButton(
                                text = "Dispatch Event SMS",
                                icon = Icons.Filled.Sms,
                                onClick = { showTriggerSmsDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    items(smsLogs, key = { it.id }) { log ->
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${log.memberName} (${log.maskedPhone})", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                                Text(log.deliveryStatus, style = MaterialTheme.typography.labelSmall, color = palette.accentCyan)
                            }
                            Text(log.eventType.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = palette.accentEmerald)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.messageBody, style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog && members.isNotEmpty()) {
        RecordFeeDialog(
            members = members,
            onDismiss = { showPaymentDialog = false },
            onSave = { mId, amt, plan, method, period, sms ->
                viewModel.recordFeePayment(mId, amt, plan, method, period, sms)
                showPaymentDialog = false
            }
        )
    }

    if (showTrainerDialog) {
        var tName by remember { mutableStateOf("") }
        var tSpec by remember { mutableStateOf("") }
        var tPhone by remember { mutableStateOf("") }
        var tShift by remember { mutableStateOf("Morning (6 AM – 12 PM)") }
        var tSalary by remember { mutableStateOf("28000") }
        AlertDialog(
            onDismissRequest = { showTrainerDialog = false },
            title = { Text("Add Trainer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tName, onValueChange = { tName = it }, label = { Text("Trainer Name") }, singleLine = true)
                    OutlinedTextField(value = tSpec, onValueChange = { tSpec = it }, label = { Text("Specialty") }, singleLine = true)
                    OutlinedTextField(value = tPhone, onValueChange = { tPhone = it }, label = { Text("Mobile") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                    OutlinedTextField(value = tShift, onValueChange = { tShift = it }, label = { Text("Shift") }, singleLine = true)
                    OutlinedTextField(value = tSalary, onValueChange = { tSalary = it }, label = { Text("Monthly Salary") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addTrainer(tName, tSpec.ifBlank { "Strength Coach" }, "+91", tPhone, tShift, tSalary.toDoubleOrNull() ?: 25000.0)
                    showTrainerDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTrainerDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTriggerSmsDialog && members.isNotEmpty()) {
        var chosenMember by remember { mutableStateOf(members.first()) }
        var chosenEvent by remember { mutableStateOf(SmsEventType.FEE_PENDING) }
        AlertDialog(
            onDismissRequest = { showTriggerSmsDialog = false },
            title = { Text("Send Automated Event SMS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Member: ${chosenMember.fullName}", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        members.take(3).forEach { m ->
                            FilterChip(selected = chosenMember.id == m.id, onClick = { chosenMember = m }, label = { Text(m.fullName.substringBefore(" ")) })
                        }
                    }
                    Text("Event Type:", style = MaterialTheme.typography.labelMedium)
                    SmsEventType.entries.forEach { ev ->
                        FilterChip(
                            selected = chosenEvent == ev,
                            onClick = { chosenEvent = ev },
                            label = { Text(ev.label) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.triggerEventSms(chosenMember.id, chosenEvent, launchComposer = true)
                    showTriggerSmsDialog = false
                }) { Text("Dispatch") }
            },
            dismissButton = { TextButton(onClick = { showTriggerSmsDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordFeeDialog(
    members: List<com.example.model.MemberView>,
    onDismiss: () -> Unit,
    onSave: (Long, Double, MembershipPlan, PaymentMethod, String, Boolean) -> Unit
) {
    var selectedMember by remember { mutableStateOf(members.first()) }
    var expanded by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf(MembershipPlan.MONTHLY) }
    var method by remember { mutableStateOf(PaymentMethod.UPI) }
    var amount by remember { mutableStateOf(plan.defaultFeeInr.toInt().toString()) }
    var period by remember { mutableStateOf("Nov 2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Membership Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = "${selectedMember.fullName} (${selectedMember.memberCode})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Member") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.fullName} (${m.memberCode})") },
                                onClick = { selectedMember = m; expanded = false }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MembershipPlan.entries.forEach { p ->
                        FilterChip(selected = plan == p, onClick = { plan = p; amount = p.defaultFeeInr.toInt().toString() }, label = { Text(p.label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { pm ->
                        FilterChip(selected = method == pm, onClick = { method = pm }, label = { Text(pm.label) })
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (INR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = period, onValueChange = { period = it }, label = { Text("Billing Period") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(selectedMember.id, amount.toDoubleOrNull() ?: plan.defaultFeeInr, plan, method, period, true)
            }) { Text("Confirm & Receipt") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
