package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExerciseCatalog
import com.example.data.FitnessRepository
import com.example.data.local.AccessTokenEntity
import com.example.data.local.AttendanceEntity
import com.example.data.local.CustomRoutineEntity
import com.example.data.local.DietMealEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SmsLogEntity
import com.example.data.local.TokenAuditEntity
import com.example.data.local.WaterLogEntity
import com.example.data.local.WeightLogEntity
import com.example.data.local.WorkoutHistoryEntity
import com.example.data.local.WorkoutPlanItemEntity
import com.example.domain.FitnessCalculators
import com.example.domain.MissedGymScanReport
import com.example.domain.SmsGatewayService
import com.example.domain.WorkoutReminderWorker
import com.example.export.FitnessPdfExporter
import com.example.model.ActivityLevel
import com.example.model.AppDestination
import com.example.model.AttendanceStatus
import com.example.model.ExerciseItem
import com.example.model.FitnessGoalType
import com.example.model.MealSlot
import com.example.model.MemberView
import com.example.model.MembershipPlan
import com.example.model.MuscleGroup
import com.example.model.PaymentMethod
import com.example.model.SmsEventType
import com.example.model.ThemeMode
import com.example.model.TokenSessionState
import com.example.model.TrainerView
import com.example.model.UserRole
import com.example.security.AppPreferencesRepository
import com.example.security.FitnessPreferences
import com.example.security.KeystoreCryptoManager
import com.example.security.TokenSecurityEngine
import com.example.security.TokenVerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(
    application: Application,
    val repository: FitnessRepository,
    val prefsRepo: AppPreferencesRepository,
    val crypto: KeystoreCryptoManager,
    val tokenEngine: TokenSecurityEngine,
    val smsService: SmsGatewayService
) : AndroidViewModel(application) {

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val _session = MutableStateFlow(TokenSessionState())
    val session: StateFlow<TokenSessionState> = _session.asStateFlow()

    private val _backStack = MutableStateFlow<List<AppDestination>>(listOf(AppDestination.Home))
    val backStack: StateFlow<List<AppDestination>> = _backStack.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val toastMessages = _toastMessages.asSharedFlow()

    private val _lastIssuedPlainToken = MutableStateFlow<String?>(null)
    val lastIssuedPlainToken: StateFlow<String?> = _lastIssuedPlainToken.asStateFlow()

    private val _lastMissedScanReport = MutableStateFlow<MissedGymScanReport?>(null)
    val lastMissedScanReport: StateFlow<MissedGymScanReport?> = _lastMissedScanReport.asStateFlow()

    private val _pendingOtp = MutableStateFlow<Pair<String, String>?>(null) // phone to otp
    val pendingOtp: StateFlow<Pair<String, String>?> = _pendingOtp.asStateFlow()

    val prefs: StateFlow<FitnessPreferences> = prefsRepo.preferencesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FitnessPreferences()
    )
    val members: StateFlow<List<MemberView>> = repository.membersFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val trainers: StateFlow<List<TrainerView>> = repository.trainersFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val attendance: StateFlow<List<AttendanceEntity>> = repository.attendanceFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val payments: StateFlow<List<PaymentEntity>> = repository.paymentsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val workoutPlan: StateFlow<List<WorkoutPlanItemEntity>> = repository.workoutPlanFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val customRoutines: StateFlow<List<CustomRoutineEntity>> = repository.customRoutinesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val workoutHistory: StateFlow<List<WorkoutHistoryEntity>> = repository.workoutHistoryFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val meals: StateFlow<List<DietMealEntity>> = repository.mealsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val weightLogs: StateFlow<List<WeightLogEntity>> = repository.weightLogsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val waterToday: StateFlow<WaterLogEntity?> = repository.waterForDateFlow(FitnessCalculators.todayDateKey())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val accessTokens: StateFlow<List<AccessTokenEntity>> = repository.tokensFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val tokenAudits: StateFlow<List<TokenAuditEntity>> = repository.tokenAuditsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val smsLogs: StateFlow<List<SmsLogEntity>> = repository.smsLogsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            _session.value = tokenEngine.restoreCachedOfflineSession()
            delay(1150)
            _showSplash.value = false
        }
    }

    fun emitToast(msg: String) {
        _toastMessages.tryEmit(msg)
    }

    fun navigateTo(dest: AppDestination) {
        val current = _backStack.value
        if (current.lastOrNull() != dest) {
            _backStack.value = current + dest
        }
    }

    fun navigateToRoot(dest: AppDestination) {
        _backStack.value = listOf(dest)
    }

    fun navigateBack(): Boolean {
        val current = _backStack.value
        return if (current.size > 1) {
            _backStack.value = current.dropLast(1)
            true
        } else {
            false
        }
    }

    // ==================== TOKEN ACCESS ====================

    fun initializeOwnerConsole() {
        viewModelScope.launch {
            val pair = tokenEngine.initializeFirstOwnerConsole()
            _lastIssuedPlainToken.value = pair.plainToken
            _session.value = tokenEngine.restoreCachedOfflineSession()
            emitToast("Owner Console initialized with AndroidKeyStore protection")
        }
    }

    fun verifyAccessToken(rawToken: String) {
        viewModelScope.launch {
            when (val res = tokenEngine.verifyToken(rawToken)) {
                is TokenVerificationResult.Verified -> {
                    _session.value = tokenEngine.restoreCachedOfflineSession()
                    emitToast("Access granted (${res.role.label})")
                }
                is TokenVerificationResult.Denied -> {
                    _session.value = tokenEngine.restoreCachedOfflineSession()
                    emitToast(res.reason)
                }
            }
        }
    }

    fun generateNewToken(
        role: UserRole,
        label: String,
        validityDays: Int,
        bindToDevice: Boolean,
        assignedMemberId: Long? = null
    ) {
        viewModelScope.launch {
            val pair = tokenEngine.generateUniqueToken(role, label, validityDays, bindToDevice, assignedMemberId)
            repository.insertToken(pair.entity)
            repository.insertTokenAudit(
                TokenAuditEntity(
                    maskedToken = pair.entity.maskedToken,
                    action = "GENERATED",
                    role = role.name,
                    deviceHashShort = crypto.deviceBindingHash().take(10),
                    success = true,
                    detail = "Issued ${validityDays}d token (${if (bindToDevice) "Device-Bound" else "Multi-Device"})"
                )
            )
            _lastIssuedPlainToken.value = pair.plainToken
            emitToast("Generated ${role.label} token: ${pair.entity.maskedToken}")
        }
    }

    fun clearLastPlainToken() {
        _lastIssuedPlainToken.value = null
    }

    fun renewToken(token: AccessTokenEntity, days: Int) {
        viewModelScope.launch {
            repository.renewToken(token.id, days, token.expiresAtMillis)
            repository.insertTokenAudit(
                TokenAuditEntity(
                    maskedToken = token.maskedToken,
                    action = "RENEWED",
                    role = token.role,
                    deviceHashShort = crypto.deviceBindingHash().take(10),
                    success = true,
                    detail = "Extended expiry by +$days days"
                )
            )
            emitToast("Renewed ${token.maskedToken} by $days days")
        }
    }

    fun revokeToken(token: AccessTokenEntity) {
        viewModelScope.launch {
            repository.revokeToken(token.id)
            repository.insertTokenAudit(
                TokenAuditEntity(
                    maskedToken = token.maskedToken,
                    action = "REVOKED",
                    role = token.role,
                    deviceHashShort = crypto.deviceBindingHash().take(10),
                    success = true,
                    detail = "Token revoked as compromised"
                )
            )
            _session.value = tokenEngine.restoreCachedOfflineSession()
            emitToast("Token revoked: ${token.maskedToken}")
        }
    }

    fun signOutToken() {
        viewModelScope.launch {
            tokenEngine.signOut()
            _session.value = tokenEngine.restoreCachedOfflineSession()
            emitToast("Signed out of token session")
        }
    }

    // ==================== EXERCISES & WORKOUT ====================

    fun addExerciseToPlan(dayOfWeek: String, routineName: String, exercise: ExerciseItem) {
        viewModelScope.launch {
            repository.addWorkoutPlanItem(dayOfWeek, routineName, exercise)
            emitToast("Added ${exercise.name} to $dayOfWeek plan")
        }
    }

    fun toggleWorkoutItemCompleted(item: WorkoutPlanItemEntity) {
        viewModelScope.launch { repository.togglePlanItemCompletedToday(item) }
    }

    fun deleteWorkoutPlanItem(id: Long) {
        viewModelScope.launch { repository.deletePlanItem(id) }
    }

    fun createCustomRoutine(name: String, focus: String, exerciseIds: List<String>) {
        if (name.isBlank() || exerciseIds.isEmpty()) {
            emitToast("Enter routine name and select at least 1 exercise")
            return
        }
        viewModelScope.launch {
            repository.createCustomRoutine(name, focus, exerciseIds, exerciseIds.size * 10)
            emitToast("Saved custom routine: $name")
        }
    }

    fun deleteCustomRoutine(id: Long) {
        viewModelScope.launch { repository.deleteRoutine(id) }
    }

    fun completeWorkoutSession(routineName: String, items: List<WorkoutPlanItemEntity>, durationMinutes: Int) {
        viewModelScope.launch {
            val sets = items.sumOf { it.targetSets }.coerceAtLeast(3)
            val estReps = sets * 10
            val estVol = sets * 380.0
            val cal = items.sumOf { ex ->
                (ExerciseCatalog.findById(ex.exerciseId)?.caloriesPerSet ?: 10) * ex.targetSets
            }.coerceAtLeast(80)
            repository.logWorkoutSession(
                routineName = routineName,
                exerciseCount = items.size.coerceAtLeast(1),
                totalSets = sets,
                totalReps = estReps,
                totalVolumeKg = estVol,
                durationMinutes = durationMinutes.coerceAtLeast(10),
                caloriesBurned = cal
            )
            emitToast("Workout logged! $cal kcal burned")
        }
    }

    // ==================== DIET, WATER & WEIGHT ====================

    fun addMeal(slot: MealSlot, foodName: String, calories: Int, protein: Int, carbs: Int, fats: Int) {
        if (foodName.isBlank()) return
        viewModelScope.launch {
            repository.addDietMeal(slot, foodName, calories, protein, carbs, fats, isLogged = true)
            emitToast("Logged $foodName ($calories kcal)")
        }
    }

    fun toggleMeal(id: Long, current: Boolean) {
        viewModelScope.launch { repository.toggleMealLogged(id, current) }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { repository.deleteMeal(id) }
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            repository.addWaterMl(ml, prefs.value.dailyWaterTargetMl)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            repository.resetWaterToday(prefs.value.dailyWaterTargetMl)
        }
    }

    fun logWeightEntry(weightKg: Float, bodyFat: Float, note: String) {
        viewModelScope.launch {
            repository.logWeight(weightKg, bodyFat, note)
            val p = prefs.value
            prefsRepo.updatePersonalGoals(
                age = p.userAge,
                isMale = p.userIsMale,
                heightCm = p.userHeightCm,
                weightKg = weightKg,
                targetWeightKg = p.targetWeightKg,
                activityLevel = p.activityLevel,
                goalType = p.fitnessGoal,
                waterTargetMl = p.dailyWaterTargetMl,
                calorieTarget = p.dailyCalorieTarget,
                proteinTargetG = p.dailyProteinTargetG,
                weeklyWorkoutDays = p.weeklyWorkoutTargetDays
            )
            emitToast("Weight logged: ${"%.1f".format(weightKg)} kg")
        }
    }

    fun updateGoals(
        age: Int,
        isMale: Boolean,
        heightCm: Float,
        weightKg: Float,
        targetWeightKg: Float,
        activity: ActivityLevel,
        goal: FitnessGoalType,
        waterTargetMl: Int,
        calorieTarget: Int,
        proteinTargetG: Int,
        weeklyDays: Int
    ) {
        viewModelScope.launch {
            prefsRepo.updatePersonalGoals(
                age, isMale, heightCm, weightKg, targetWeightKg,
                activity, goal, waterTargetMl, calorieTarget, proteinTargetG, weeklyDays
            )
            emitToast("Fitness goals & macro targets updated")
        }
    }

    // ==================== MEMBERS, OTP & PHOTO ====================

    fun requestPhoneOtp(fullPhone: String): String {
        val code = crypto.generateNumericOtp(6)
        _pendingOtp.value = fullPhone to code
        return code
    }

    fun verifyOtpInput(fullPhone: String, entered: String): Boolean {
        val pair = _pendingOtp.value ?: return false
        return pair.first == fullPhone && pair.second == entered.trim()
    }

    fun copyPickedPhotoToInternal(uri: Uri): String? {
        return try {
            val dir = File(getApplication<Application>().filesDir, "member_photos").apply { mkdirs() }
            val outFile = File(dir, "member_${System.currentTimeMillis()}.jpg")
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            outFile.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

    fun registerMember(
        fullName: String,
        countryCode: String,
        phoneDigits: String,
        emergencyName: String,
        emergencyPhone: String,
        phoneVerified: Boolean,
        photoPath: String?,
        plan: MembershipPlan,
        fee: Double,
        scheduledDays: Set<String>,
        trainerId: Long?,
        sendWelcomeSms: Boolean
    ) {
        viewModelScope.launch {
            val member = repository.addMember(
                fullName = fullName,
                countryCode = countryCode,
                phoneDigits = phoneDigits,
                emergencyName = emergencyName,
                emergencyPhone = emergencyPhone,
                phoneVerified = phoneVerified,
                photoPath = photoPath,
                plan = plan,
                feeAmount = fee,
                scheduledDays = scheduledDays,
                trainerId = trainerId,
                smsAttendanceMissing = true,
                smsWorkoutMissed = true,
                smsMembershipExpiry = true,
                smsFeePending = true,
                smsPaymentConfirm = true,
                smsWelcome = sendWelcomeSms,
                smsTokenExpiry = true
            )
            if (sendWelcomeSms) {
                smsService.dispatchMemberSms(
                    member = member,
                    eventType = SmsEventType.NEW_MEMBER_REGISTRATION,
                    messageBody = "Welcome to ${prefs.value.gymName}, ${member.fullName}! Your Member ID is ${member.memberCode}. Plan: ${plan.label}."
                )
            }
            emitToast("Registered ${member.fullName} (${member.memberCode})")
        }
    }

    fun updateMemberSmsPreferences(
        memberId: Long,
        attendanceMissing: Boolean,
        workoutMissed: Boolean,
        membershipExpiry: Boolean,
        feePending: Boolean,
        paymentConfirm: Boolean,
        welcome: Boolean,
        tokenExpiry: Boolean
    ) {
        viewModelScope.launch {
            repository.updateMemberSmsPrefs(
                memberId, attendanceMissing, workoutMissed, membershipExpiry,
                feePending, paymentConfirm, welcome, tokenExpiry
            )
            emitToast("Saved member SMS preferences")
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
            emitToast("Member removed")
        }
    }

    // ==================== ATTENDANCE, QR CHECK-IN & MISSED GYM ====================

    fun markAttendanceManual(memberId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            val member = repository.getMemberEntityById(memberId) ?: return@launch
            repository.recordAttendance(member, status, "MANUAL")
            emitToast("${member.fullName}: marked ${status.label}")
        }
    }

    fun checkInByQrCode(memberCode: String) {
        viewModelScope.launch {
            val member = repository.getMemberEntityByCode(memberCode)
            if (member == null) {
                emitToast("No member found for QR ID: $memberCode")
                return@launch
            }
            repository.recordAttendance(member, AttendanceStatus.PRESENT, "QR_SCAN")
            emitToast("QR Check-In recorded for ${member.fullName}")
        }
    }

    fun runMissedGymDetectionScan(forceIgnoreDeadline: Boolean) {
        viewModelScope.launch {
            val report = smsService.runMissedGymDetection(forceIgnoreDeadline)
            _lastMissedScanReport.value = report
            val note = if (!report.deadlineReached && !forceIgnoreDeadline) {
                "Deadline ${report.deadlineLabel} not reached yet (${report.missingCount} currently unlogged)"
            } else {
                "Scan complete: ${report.sentCount} reminders queued/sent, ${report.skippedDuplicateCount} duplicates prevented"
            }
            emitToast(note)
        }
    }

    // ==================== GYM MANAGEMENT, FEES, TRAINERS, SMS & PDF ====================

    fun addTrainer(name: String, specialty: String, countryCode: String, phone: String, shift: String, salary: Double) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addTrainer(name, specialty, countryCode, phone, shift, salary)
            emitToast("Added trainer $name")
        }
    }

    fun deleteTrainer(id: Long) {
        viewModelScope.launch {
            repository.deleteTrainer(id)
            emitToast("Trainer removed")
        }
    }

    fun recordFeePayment(
        memberId: Long,
        amount: Double,
        plan: MembershipPlan,
        method: PaymentMethod,
        periodLabel: String,
        sendConfirmationSms: Boolean
    ) {
        viewModelScope.launch {
            val member = repository.getMemberEntityById(memberId) ?: return@launch
            val payment = repository.recordPayment(member, amount, plan, method, periodLabel, isPaid = true)
            if (sendConfirmationSms) {
                smsService.dispatchMemberSms(
                    member = member,
                    eventType = SmsEventType.PAYMENT_CONFIRMATION,
                    messageBody = "${prefs.value.gymName}: Payment of ${FitnessCalculators.formatInr(amount)} received (${payment.receiptNumber}). Valid for ${plan.label}."
                )
            }
            emitToast("Recorded ${payment.receiptNumber} (${FitnessCalculators.formatInr(amount)})")
        }
    }

    fun triggerEventSms(memberId: Long, eventType: SmsEventType, customBody: String? = null, launchComposer: Boolean = false) {
        viewModelScope.launch {
            val member = repository.getMemberEntityById(memberId) ?: return@launch
            val body = customBody?.takeIf { it.isNotBlank() } ?: when (eventType) {
                SmsEventType.ATTENDANCE_MISSING -> "${prefs.value.gymName}: Hi ${member.fullName}, your gym attendance is missing today. Stay consistent!"
                SmsEventType.WORKOUT_MISSED -> "${prefs.value.gymName}: Hi ${member.fullName}, you missed today's scheduled workout routine. Catch up tomorrow!"
                SmsEventType.MEMBERSHIP_EXPIRY -> "${prefs.value.gymName}: Hi ${member.fullName}, your ${member.planType} membership expires on ${FitnessCalculators.formatDateShort(member.expiryDateMillis)}."
                SmsEventType.FEE_PENDING -> "${prefs.value.gymName}: Reminder for ${member.fullName}, fee of ${FitnessCalculators.formatInr(member.monthlyFee)} is pending."
                SmsEventType.PAYMENT_CONFIRMATION -> "${prefs.value.gymName}: Thank you ${member.fullName}! Your fee payment has been confirmed."
                SmsEventType.NEW_MEMBER_REGISTRATION -> "Welcome to ${prefs.value.gymName}, ${member.fullName}! Member ID: ${member.memberCode}."
                SmsEventType.TOKEN_EXPIRY -> "${prefs.value.gymName}: Your app access token is expiring soon. Contact Admin for renewal."
            }
            val status = smsService.dispatchMemberSms(member, eventType, body, allowIntentLaunch = launchComposer)
            emitToast("SMS ${eventType.label}: ${status.label}")
        }
    }

    fun exportAndShareReceipt(payment: PaymentEntity) {
        viewModelScope.launch {
            val member = members.value.firstOrNull { it.id == payment.memberId }
            val file = withContext(Dispatchers.IO) {
                FitnessPdfExporter.exportPaymentReceipt(
                    context = getApplication(),
                    payment = payment,
                    member = member,
                    gymName = prefs.value.gymName,
                    ownerSignature = prefs.value.ownerSignature
                )
            }
            sharePdfFile(file, "Share Fee Receipt ${payment.receiptNumber}")
        }
    }

    fun exportAndShareRevenueReport() {
        viewModelScope.launch {
            val allPayments = payments.value
            val pending = allPayments.filter { !it.isPaid }.sumOf { it.amount }
            val activeCount = members.value.count { it.isActive }
            val file = withContext(Dispatchers.IO) {
                FitnessPdfExporter.exportMonthlyRevenueReport(
                    context = getApplication(),
                    payments = allPayments,
                    activeMembersCount = activeCount,
                    pendingAmount = pending,
                    gymName = prefs.value.gymName,
                    ownerSignature = prefs.value.ownerSignature
                )
            }
            sharePdfFile(file, "Share Monthly Revenue Report")
        }
    }

    private fun sharePdfFile(file: File, chooserTitle: String) {
        val ctx = getApplication<Application>()
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(chooser)
    }

    // ==================== SETTINGS & PRIVACY ====================

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun saveGatewayAndGymSettings(
        gymName: String,
        owner: String,
        deadlineHour: Int,
        deadlineMinute: Int,
        smsEnabled: Boolean,
        smsUrl: String,
        smsApiKeyPlain: String,
        smsSenderId: String,
        tokenBackendUrl: String,
        leaseHours: Int
    ) {
        viewModelScope.launch {
            prefsRepo.setGymBranding(gymName, owner)
            prefsRepo.setAttendanceDeadline(deadlineHour, deadlineMinute)
            val encKey = if (smsApiKeyPlain.isNotBlank()) crypto.encrypt(smsApiKeyPlain.trim()) else ""
            prefsRepo.setSmsGatewayConfig(smsEnabled, smsUrl, encKey, smsSenderId)
            prefsRepo.setTokenBackendConfig(tokenBackendUrl, leaseHours)
            emitToast("Settings saved securely")
        }
    }

    fun setWorkoutReminders(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setWorkoutReminder(enabled, hour, minute)
            if (enabled) {
                WorkoutReminderWorker.schedulePeriodicChecks(getApplication())
            }
            emitToast(if (enabled) "Workout reminders scheduled for %02d:%02d".format(hour, minute) else "Workout reminders disabled")
        }
    }

    fun wipeAllPersonalData() {
        viewModelScope.launch {
            repository.wipeAllLocalData()
            prefsRepo.wipePersonalPreferences()
            _session.value = TokenSessionState()
            emitToast("All local encrypted personal data wiped")
        }
    }
}
