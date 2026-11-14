package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.FitnessCalculators
import com.example.domain.QrCodeGenerator
import com.example.model.AppDestination
import com.example.model.CountryCode
import com.example.model.MemberView
import com.example.model.MembershipPlan
import com.example.model.SmsEventType
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.GlassSearchBar
import com.example.ui.components.GlassTopHeader
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun MembersScreen(
    viewModel: MainViewModel,
    onOpenMember: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalGlassPalette.current
    val members by viewModel.members.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(members, query) {
        val q = query.trim()
        if (q.isBlank()) members else members.filter {
            it.fullName.contains(q, ignoreCase = true) ||
                it.memberCode.contains(q, ignoreCase = true) ||
                it.phoneLast4.contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
        GlassTopHeader(
            title = "Members & Mobile IDs",
            subtitle = "${members.size} registered • AES-256-GCM PII Vault",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassSearchBar(query = query, onQueryChange = { query = it }, placeholder = "Search name, ID or phone...")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                GlassPillButton(
                    text = "Register New Member (Mobile + OTP)",
                    icon = Icons.Filled.Add,
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("register_member_btn")
                )
            }

            items(filtered, key = { it.id }) { m ->
                MemberCard(member = m, onClick = { onOpenMember(m.id) })
            }
        }
    }

    if (showAddDialog) {
        AddMemberDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun MemberCard(member: MemberView, onClick: () -> Unit) {
    val palette = LocalGlassPalette.current
    val daysLeft = ((member.expiryDateMillis - System.currentTimeMillis()) / 86_400_000L).toInt()
    val statusColor = when {
        daysLeft <= 0 -> palette.accentDanger
        daysLeft <= 7 -> palette.accentAmber
        else -> palette.accentEmerald
    }
    GlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(palette.glassElevatedFill)
                    .border(1.dp, palette.glassBorderTop, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!member.photoPath.isNullOrBlank() && File(member.photoPath).exists()) {
                    AsyncImage(
                        model = File(member.photoPath),
                        contentDescription = "Photo of ${member.fullName}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = palette.accentCyan)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.fullName, style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    if (member.phoneVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.Verified, contentDescription = "OTP Verified", tint = palette.accentEmerald, modifier = Modifier.size(16.dp))
                    }
                }
                Text("${member.memberCode} • ${member.countryCode} ••••••${member.phoneLast4}", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                Text("${member.plan.label} • ${if (daysLeft > 0) "$daysLeft days left" else "Expired"}", style = MaterialTheme.typography.labelMedium, color = statusColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    var name by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryCode.supported.first()) }
    var countryExpanded by remember { mutableStateOf(false) }
    var phoneDigits by remember { mutableStateOf("") }
    var otpGenerated by remember { mutableStateOf<String?>(null) }
    var otpInput by remember { mutableStateOf("") }
    var phoneVerified by remember { mutableStateOf(false) }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf(MembershipPlan.MONTHLY) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var sendWelcomeSms by remember { mutableStateOf(true) }
    val days = remember { mutableStateListOf("MON", "TUE", "WED", "THU", "FRI", "SAT") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoPath = viewModel.copyPickedPhotoToInternal(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Gym Member") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)

                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { countryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCountry.name} (${selectedCountry.dialCode})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Country Code") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false }
                    ) {
                        CountryCode.supported.forEach { cc ->
                            DropdownMenuItem(
                                text = { Text("${cc.name} (${cc.dialCode})") },
                                onClick = { selectedCountry = cc; countryExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phoneDigits,
                    onValueChange = { phoneDigits = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassPillButton(
                        text = if (phoneVerified) "OTP Verified" else "Send OTP",
                        filled = false,
                        onClick = {
                            val (ok, res) = FitnessCalculators.validatePhoneNumber(selectedCountry, phoneDigits)
                            if (!ok) {
                                viewModel.emitToast(res)
                            } else {
                                otpGenerated = viewModel.requestPhoneOtp(res)
                                viewModel.emitToast("Verification OTP generated: $otpGenerated")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (otpGenerated != null && !phoneVerified) {
                    Text("Verification Code: $otpGenerated", style = MaterialTheme.typography.labelSmall, color = palette.accentCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { otpInput = it },
                            label = { Text("Enter 6-digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        TextButton(onClick = {
                            val full = "${selectedCountry.dialCode}$phoneDigits"
                            if (viewModel.verifyOtpInput(full, otpInput)) {
                                phoneVerified = true
                                viewModel.emitToast("Mobile verified via OTP")
                            } else {
                                viewModel.emitToast("Incorrect OTP")
                            }
                        }) { Text("Verify") }
                    }
                }

                OutlinedTextField(value = emergencyName, onValueChange = { emergencyName = it }, label = { Text("Emergency Contact Name") }, singleLine = true)
                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    label = { Text("Emergency Contact Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MembershipPlan.entries.forEach { p ->
                        FilterChip(selected = plan == p, onClick = { plan = p }, label = { Text(p.label) })
                    }
                }

                GlassPillButton(
                    text = if (photoPath != null) "Photo Attached ✓" else "Pick Member Photo",
                    icon = Icons.Filled.CameraAlt,
                    filled = false,
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Send Welcome SMS", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = sendWelcomeSms, onCheckedChange = { sendWelcomeSms = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val (ok, err) = FitnessCalculators.validatePhoneNumber(selectedCountry, phoneDigits)
                if (name.isBlank() || !ok) {
                    viewModel.emitToast(if (name.isBlank()) "Enter member name" else err)
                    return@TextButton
                }
                viewModel.registerMember(
                    fullName = name,
                    countryCode = selectedCountry.dialCode,
                    phoneDigits = phoneDigits,
                    emergencyName = emergencyName,
                    emergencyPhone = emergencyPhone,
                    phoneVerified = phoneVerified,
                    photoPath = photoPath,
                    plan = plan,
                    fee = plan.defaultFeeInr,
                    scheduledDays = days.toSet(),
                    trainerId = null,
                    sendWelcomeSms = sendWelcomeSms
                )
                onDismiss()
            }) { Text("Register") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MemberDetailScreen(
    memberId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalGlassPalette.current
    val members by viewModel.members.collectAsStateWithLifecycle()
    val member = members.firstOrNull { it.id == memberId }

    if (member == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopHeader(title = "Member Profile", subtitle = "Not found", onBack = onBack)
        }
        return
    }

    val qrBmp = remember(member.memberCode) {
        QrCodeGenerator.generateQrBitmap(QrCodeGenerator.buildMemberQrPayload(member.memberCode), 360)
    }

    var smsAtt by remember(member.id) { mutableStateOf(member.smsAttendanceMissing) }
    var smsWork by remember(member.id) { mutableStateOf(member.smsWorkoutMissed) }
    var smsExp by remember(member.id) { mutableStateOf(member.smsMembershipExpiry) }
    var smsFee by remember(member.id) { mutableStateOf(member.smsFeePending) }
    var smsPay by remember(member.id) { mutableStateOf(member.smsPaymentConfirm) }
    var smsWel by remember(member.id) { mutableStateOf(member.smsWelcome) }
    var smsTok by remember(member.id) { mutableStateOf(member.smsTokenExpiry) }

    Column(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp)) {
        GlassTopHeader(
            title = member.fullName,
            subtitle = "${member.memberCode} • ${member.plan.label}",
            onBack = onBack,
            trailing = {
                IconButton(onClick = {
                    viewModel.deleteMember(member.id)
                    onBack()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete member", tint = palette.accentDanger)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(elevated = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = palette.accentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Mobile: ${member.countryCode} ${member.phoneNumber}", style = MaterialTheme.typography.titleSmall, color = palette.primaryText)
                            Text(
                                if (member.phoneVerified) "OTP Verified • Keystore Encrypted" else "OTP Pending • Keystore Encrypted",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (member.phoneVerified) palette.accentEmerald else palette.accentAmber
                            )
                            if (member.emergencyName.isNotBlank()) {
                                Text("Emergency: ${member.emergencyName} (${member.emergencyPhone})", style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
                            }
                            Text("Expires: ${FitnessCalculators.formatDateShort(member.expiryDateMillis)}", style = MaterialTheme.typography.bodySmall, color = palette.accentCyan)
                        }
                    }
                }
            }

            item {
                GlassCard(elevated = true, accentBorder = palette.accentCyan) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QrCode2, contentDescription = null, tint = palette.accentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Member QR Check-In Pass", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (qrBmp != null) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = qrBmp.asImageBitmap(),
                                contentDescription = "QR Pass for ${member.memberCode}",
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Payload: ${QrCodeGenerator.buildMemberQrPayload(member.memberCode)}", style = MaterialTheme.typography.labelSmall, color = palette.secondaryText)
                }
            }

            item {
                GlassCard {
                    Text("Member SMS Notification Preferences", style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    SmsPrefToggle("Attendance Missing Alerts", smsAtt) { smsAtt = it }
                    SmsPrefToggle("Scheduled Workout Missed", smsWork) { smsWork = it }
                    SmsPrefToggle("Membership Expiry Alerts", smsExp) { smsExp = it }
                    SmsPrefToggle("Fee Pending Reminders", smsFee) { smsFee = it }
                    SmsPrefToggle("Payment Confirmations", smsPay) { smsPay = it }
                    SmsPrefToggle("Registration Welcome", smsWel) { smsWel = it }
                    SmsPrefToggle("Token Expiry Alerts", smsTok) { smsTok = it }
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassPillButton(
                        text = "Save SMS Preferences",
                        onClick = {
                            viewModel.updateMemberSmsPreferences(member.id, smsAtt, smsWork, smsExp, smsFee, smsPay, smsWel, smsTok)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassPillButton(
                        text = "Send Fee Reminder SMS",
                        filled = false,
                        onClick = { viewModel.triggerEventSms(member.id, SmsEventType.FEE_PENDING, launchComposer = true) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsPrefToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.primaryText)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
