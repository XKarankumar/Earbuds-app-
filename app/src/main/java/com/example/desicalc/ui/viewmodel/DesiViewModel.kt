package com.example.desicalc.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.desicalc.data.backup.BackupAndMigrationManager
import com.example.desicalc.data.backup.MigrationImportResult
import com.example.desicalc.data.local.AiChatMessageEntity
import com.example.desicalc.data.local.CalculationHistoryEntity
import com.example.desicalc.data.local.CustomerEntity
import com.example.desicalc.data.local.DesiCalculatorDatabase
import com.example.desicalc.data.local.SmsLogEntity
import com.example.desicalc.data.local.TransactionEntity
import com.example.desicalc.data.preferences.DesiPreferencesStore
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.engine.CalculatorEngine
import com.example.desicalc.engine.OfflineSmartAssistant
import com.example.desicalc.engine.OnlineGeminiAssistant
import com.example.desicalc.model.AiEngineMode
import com.example.desicalc.model.AiUiState
import com.example.desicalc.model.AppLanguage
import com.example.desicalc.model.AppSettingsState
import com.example.desicalc.model.AppThemePreset
import com.example.desicalc.model.CalculatorMode
import com.example.desicalc.model.CustomBackgroundStyle
import com.example.desicalc.model.CustomerFilter
import com.example.desicalc.model.CustomerLedgerSummary
import com.example.desicalc.model.DashboardMetrics
import com.example.desicalc.model.PendingTransactionAction
import com.example.desicalc.model.PeriodReport
import com.example.desicalc.model.ReportPeriod
import com.example.desicalc.model.SmsTemplateType
import com.example.desicalc.model.TransactionType
import com.example.desicalc.model.formatInr
import com.example.desicalc.notifications.DesiNotificationManager
import com.example.desicalc.sms.ReminderWorker
import com.example.desicalc.sms.SmsReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

class DesiViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = DesiCalculatorDatabase.getInstance(appContext)
    val repository = DesiLedgerRepository(database)
    val preferencesStore = DesiPreferencesStore(appContext)
    val backupManager = BackupAndMigrationManager(database)
    val notificationManager = DesiNotificationManager(appContext)
    val smsManager = SmsReminderManager(appContext, repository)
    private val offlineAssistant = OfflineSmartAssistant(repository)
    private val onlineAssistant = OnlineGeminiAssistant(repository)
    private val json = Json { ignoreUnknownKeys = true }

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    val settings: StateFlow<AppSettingsState> = preferencesStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsState())

    val customerSummaries: StateFlow<List<CustomerLedgerSummary>> = repository.customerSummariesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dashboardMetrics: StateFlow<DashboardMetrics> = repository.dashboardMetricsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics())

    val calculationHistory: StateFlow<List<CalculationHistoryEntity>> = repository.calculationHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val smsLogs: StateFlow<List<SmsLogEntity>> = repository.smsLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val aiChatMessages: StateFlow<List<AiChatMessageEntity>> = repository.aiChatFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Search & Filter state for Customers
    private val _customerSearch = MutableStateFlow("")
    val customerSearch: StateFlow<String> = _customerSearch.asStateFlow()

    private val _customerFilter = MutableStateFlow(CustomerFilter.ALL)
    val customerFilter: StateFlow<CustomerFilter> = _customerFilter.asStateFlow()

    val filteredCustomers: StateFlow<List<CustomerLedgerSummary>> = combine(
        customerSummaries,
        _customerSearch,
        _customerFilter
    ) { list, query, filter ->
        val q = query.trim().lowercase(Locale.ROOT)
        list.filter { c ->
            val matchesQuery = q.isEmpty() ||
                c.name.lowercase(Locale.ROOT).contains(q) ||
                c.mobile.lowercase(Locale.ROOT).contains(q) ||
                c.customerCode.lowercase(Locale.ROOT).contains(q)
            val matchesFilter = when (filter) {
                CustomerFilter.ALL -> true
                CustomerFilter.LENA_HAI -> c.netBalance > 0.01
                CustomerFilter.DENA_HAI -> c.netBalance < -0.01
                CustomerFilter.SETTLED -> kotlin.math.abs(c.netBalance) <= 0.01
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Selected customer for ledger detail
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    // Calculator state
    private val _calcMode = MutableStateFlow(CalculatorMode.BASIC)
    val calcMode: StateFlow<CalculatorMode> = _calcMode.asStateFlow()

    private val _calcExpression = MutableStateFlow("")
    val calcExpression: StateFlow<String> = _calcExpression.asStateFlow()

    private val _calcResult = MutableStateFlow("0")
    val calcResult: StateFlow<String> = _calcResult.asStateFlow()

    private val _calcError = MutableStateFlow<String?>(null)
    val calcError: StateFlow<String?> = _calcError.asStateFlow()

    // AI Assistant state
    private val _aiUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiUiState: StateFlow<AiUiState> = _aiUiState.asStateFlow()

    // Report period state
    private val _reportPeriod = MutableStateFlow(ReportPeriod.WEEKLY)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    val activePeriodReport: StateFlow<PeriodReport> = combine(
        allTransactions,
        _reportPeriod
    ) { txs, period ->
        repository.buildPeriodReport(txs, period)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        repository.buildPeriodReport(emptyList(), ReportPeriod.WEEKLY)
    )

    // Duplicate warning dialog state for manual transaction entry
    private val _duplicateWarningPrompt = MutableStateFlow<PendingTransactionAction?>(null)
    val duplicateWarningPrompt: StateFlow<PendingTransactionAction?> = _duplicateWarningPrompt.asStateFlow()

    // Migration / backup result banner
    private val _lastBackupMessage = MutableStateFlow<String?>(null)
    val lastBackupMessage: StateFlow<String?> = _lastBackupMessage.asStateFlow()

    fun setCustomerSearch(query: String) {
        _customerSearch.value = query
    }

    fun setCustomerFilter(filter: CustomerFilter) {
        _customerFilter.value = filter
    }

    fun selectCustomer(id: Long?) {
        _selectedCustomerId.value = id
    }

    fun setCalculatorMode(mode: CalculatorMode) {
        _calcMode.value = mode
    }

    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    // --- Basic Calculator actions ---
    fun appendCalcToken(token: String) {
        _calcError.value = null
        val current = _calcExpression.value
        val next = current + token
        _calcExpression.value = next
        val eval = CalculatorEngine.evaluateExpression(next)
        if (eval.isSuccess) {
            _calcResult.value = CalculatorEngine.formatCleanNumber(eval.getOrThrow())
        }
    }

    fun backspaceCalc() {
        _calcError.value = null
        if (_calcExpression.value.isNotEmpty()) {
            val next = _calcExpression.value.dropLast(1)
            _calcExpression.value = next
            if (next.isEmpty()) {
                _calcResult.value = "0"
            } else {
                CalculatorEngine.evaluateExpression(next).onSuccess {
                    _calcResult.value = CalculatorEngine.formatCleanNumber(it)
                }
            }
        }
    }

    fun clearCalc() {
        _calcExpression.value = ""
        _calcResult.value = "0"
        _calcError.value = null
    }

    fun evaluateAndSaveCalc() {
        val expr = _calcExpression.value.trim()
        if (expr.isEmpty()) return
        val result = CalculatorEngine.evaluateExpression(expr)
        result.onSuccess { value ->
            val formatted = CalculatorEngine.formatCleanNumber(value)
            _calcResult.value = formatted
            _calcExpression.value = formatted
            _calcError.value = null
            viewModelScope.launch {
                repository.addCalculationHistory("BASIC", expr, formatted)
            }
        }.onFailure { err ->
            _calcError.value = err.message ?: "Invalid expression"
        }
    }

    fun saveSpecializedCalculation(category: String, expression: String, result: String, details: String) {
        viewModelScope.launch {
            repository.addCalculationHistory(category, expression, result, details)
            _toastEvents.tryEmit("Saved to Calculation History")
        }
    }

    fun reuseHistoryExpression(expression: String) {
        _calcMode.value = CalculatorMode.BASIC
        _calcExpression.value = expression
        CalculatorEngine.evaluateExpression(expression).onSuccess {
            _calcResult.value = CalculatorEngine.formatCleanNumber(it)
        }
    }

    fun clearCalculationHistory() {
        viewModelScope.launch {
            repository.clearCalculationHistory()
            _toastEvents.tryEmit("Calculation history cleared")
        }
    }

    // --- Customer CRUD ---
    fun addCustomer(
        name: String,
        mobile: String,
        address: String,
        notes: String,
        photoUri: Uri?,
        autoSmsEnabled: Boolean,
        smsConsentGranted: Boolean,
        reminderIntervalDays: Int
    ) {
        viewModelScope.launch {
            try {
                val savedPhoto = photoUri?.let { copyImageToInternalStorage(it, "cust_${System.currentTimeMillis()}") }
                val id = repository.addCustomer(
                    name = name,
                    mobile = mobile,
                    address = address,
                    notes = notes,
                    photoPath = savedPhoto,
                    autoSmsEnabled = autoSmsEnabled,
                    smsConsentGranted = smsConsentGranted,
                    reminderIntervalDays = reminderIntervalDays
                )
                if (autoSmsEnabled && smsConsentGranted) {
                    ReminderWorker.schedulePeriodicReminders(appContext)
                }
                _selectedCustomerId.value = id
                _toastEvents.tryEmit("Customer '$name' added")
            } catch (e: Exception) {
                _toastEvents.tryEmit(e.localizedMessage ?: "Failed to add customer")
            }
        }
    }

    fun updateCustomer(
        existing: CustomerLedgerSummary,
        name: String,
        mobile: String,
        address: String,
        notes: String,
        newPhotoUri: Uri?,
        autoSmsEnabled: Boolean,
        smsConsentGranted: Boolean,
        reminderIntervalDays: Int
    ) {
        viewModelScope.launch {
            val currentEntity = repository.getCustomerById(existing.id) ?: return@launch
            val savedPhoto = if (newPhotoUri != null) {
                copyImageToInternalStorage(newPhotoUri, "cust_${existing.id}_${System.currentTimeMillis()}")
            } else {
                currentEntity.photoPath
            }
            repository.updateCustomer(
                currentEntity.copy(
                    name = name,
                    mobile = mobile,
                    address = address,
                    notes = notes,
                    photoPath = savedPhoto,
                    autoSmsEnabled = autoSmsEnabled,
                    smsConsentGranted = smsConsentGranted,
                    reminderIntervalDays = reminderIntervalDays
                )
            )
            if (autoSmsEnabled && smsConsentGranted) {
                ReminderWorker.schedulePeriodicReminders(appContext)
            }
            _toastEvents.tryEmit("Customer updated")
        }
    }

    fun deleteCustomer(customerId: Long, deleteTransactionsToo: Boolean) {
        viewModelScope.launch {
            val deleted = repository.deleteCustomerSafely(customerId, deleteTransactionsToo)
            if (deleted) {
                if (_selectedCustomerId.value == customerId) {
                    _selectedCustomerId.value = null
                }
                _toastEvents.tryEmit("Customer removed")
            } else {
                _toastEvents.tryEmit("Customer has ledger transactions. Confirm delete all entries first.")
            }
        }
    }

    // --- Transaction CRUD with Duplicate Guard & Notifications ---
    fun requestAddTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        description: String,
        isPartialPayment: Boolean = false,
        timestamp: Long = System.currentTimeMillis(),
        bypassDuplicateCheck: Boolean = false
    ) {
        viewModelScope.launch {
            if (amount <= 0.0 || !amount.isFinite()) {
                _toastEvents.tryEmit("Enter a valid positive amount")
                return@launch
            }
            if (!bypassDuplicateCheck) {
                val dup = repository.checkPossibleDuplicate(customerId, type, amount)
                if (dup != null) {
                    val cust = repository.getCustomerById(customerId)
                    _duplicateWarningPrompt.value = PendingTransactionAction(
                        customerName = cust?.name ?: "Customer",
                        matchedCustomerId = customerId,
                        customerMobile = cust?.mobile.orEmpty(),
                        transactionType = type.code,
                        amount = amount,
                        description = description,
                        isPossibleDuplicate = true,
                        duplicateReason = "An identical ${type.labelEn} of ${formatInr(amount)} was recorded at ${DesiLedgerRepository.formatTimestamp(dup.timestamp)}. Save anyway?"
                    )
                    return@launch
                }
            }
            executeSaveTransaction(customerId, type, amount, description, isPartialPayment, timestamp)
        }
    }

    fun confirmDuplicatePromptSave() {
        val pending = _duplicateWarningPrompt.value ?: return
        _duplicateWarningPrompt.value = null
        val cId = pending.matchedCustomerId ?: return
        viewModelScope.launch {
            executeSaveTransaction(
                customerId = cId,
                type = TransactionType.fromCode(pending.transactionType),
                amount = pending.amount,
                description = pending.description,
                isPartialPayment = false,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun dismissDuplicatePrompt() {
        _duplicateWarningPrompt.value = null
    }

    private suspend fun executeSaveTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        description: String,
        isPartialPayment: Boolean,
        timestamp: Long
    ) {
        repository.addTransaction(customerId, type, amount, description, isPartialPayment, timestamp)
        val currentSettings = settings.value
        val cust = repository.getCustomerById(customerId)
        if (currentSettings.notificationsEnabled && currentSettings.notifyOnTransaction && cust != null) {
            notificationManager.showTransactionNotification(
                title = "${type.labelEn}: ${cust.name}",
                body = "${formatInr(amount)} recorded${if (description.isNotBlank()) " — $description" else ""}."
            )
        }
        _toastEvents.tryEmit("${type.labelEn} ${formatInr(amount)} saved")
    }

    fun updateTransaction(tx: TransactionEntity, newType: TransactionType, newAmount: Double, newDescription: String) {
        viewModelScope.launch {
            if (newAmount <= 0.0 || !newAmount.isFinite()) {
                _toastEvents.tryEmit("Amount must be greater than zero")
                return@launch
            }
            repository.updateTransaction(
                tx.copy(
                    type = newType.code,
                    amount = newAmount,
                    description = newDescription
                )
            )
            _toastEvents.tryEmit("Transaction updated")
        }
    }

    fun deleteTransaction(txId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(txId)
            _toastEvents.tryEmit("Transaction deleted")
        }
    }

    // --- AI Smart Assistant ---
    fun sendAiMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            repository.insertAiMessage(
                AiChatMessageEntity(
                    role = "USER",
                    text = trimmed,
                    engineUsed = "USER"
                )
            )
            _aiUiState.value = AiUiState.Thinking

            val mode = settings.value.aiEngineMode
            when (mode) {
                AiEngineMode.OFFLINE -> {
                    val res = offlineAssistant.processQuery(trimmed)
                    saveAssistantOutcome(res.replyText, res.engineName, res.pendingAction)
                    _aiUiState.value = AiUiState.Idle
                }
                AiEngineMode.ONLINE -> {
                    val geminiResult = onlineAssistant.askGemini(trimmed)
                    geminiResult.onSuccess { res ->
                        saveAssistantOutcome(res.replyText, res.engineName, res.pendingAction)
                        _aiUiState.value = AiUiState.Idle
                    }.onFailure { err ->
                        _aiUiState.value = AiUiState.Error(
                            err.localizedMessage ?: "Online Gemini AI failed. Check internet/App Check or switch to Offline Engine."
                        )
                    }
                }
                AiEngineMode.AUTO -> {
                    // Try Online Gemini first; if network/App Check fails, run Offline Smart Engine transparently
                    val geminiResult = onlineAssistant.askGemini(trimmed)
                    if (geminiResult.isSuccess) {
                        val res = geminiResult.getOrThrow()
                        saveAssistantOutcome(res.replyText, res.engineName, res.pendingAction)
                        _aiUiState.value = AiUiState.Idle
                    } else {
                        val offline = offlineAssistant.processQuery(trimmed)
                        val note = "(Offline Smart Engine fallback — Online AI unavailable: ${geminiResult.exceptionOrNull()?.localizedMessage ?: "offline"})\n\n${offline.replyText}"
                        saveAssistantOutcome(note, offline.engineName, offline.pendingAction)
                        _aiUiState.value = AiUiState.Idle
                    }
                }
            }
        }
    }

    private suspend fun saveAssistantOutcome(
        replyText: String,
        engineName: String,
        pendingAction: PendingTransactionAction?
    ) {
        val pendingJson = pendingAction?.let {
            json.encodeToString(PendingTransactionAction.serializer(), it)
        }
        repository.insertAiMessage(
            AiChatMessageEntity(
                role = "ASSISTANT",
                text = replyText,
                engineUsed = engineName,
                pendingActionJson = pendingJson,
                actionConfirmed = false
            )
        )
    }

    fun confirmAiPendingTransaction(messageId: Long, action: PendingTransactionAction) {
        viewModelScope.launch {
            try {
                val customerId = action.matchedCustomerId
                    ?: repository.addCustomer(
                        name = action.customerName,
                        mobile = action.customerMobile
                    )
                val txType = TransactionType.fromCode(action.transactionType)
                executeSaveTransaction(
                    customerId = customerId,
                    type = txType,
                    amount = action.amount,
                    description = action.description,
                    isPartialPayment = false,
                    timestamp = System.currentTimeMillis()
                )
                repository.markAiActionConfirmed(messageId)
            } catch (e: Exception) {
                _toastEvents.tryEmit(e.localizedMessage ?: "Could not save AI transaction")
            }
        }
    }

    fun decodePendingAction(pendingJson: String?): PendingTransactionAction? {
        if (pendingJson.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString(PendingTransactionAction.serializer(), pendingJson)
        }.getOrNull()
    }

    fun clearAiChat() {
        viewModelScope.launch {
            repository.clearAiChat()
        }
    }

    // --- SMS Actions ---
    fun buildSmsPreview(
        customer: CustomerLedgerSummary,
        templateType: SmsTemplateType,
        latestAmount: Double = kotlin.math.abs(customer.netBalance)
    ): String {
        val s = settings.value
        val template = when (templateType) {
            SmsTemplateType.REMINDER -> s.reminderTemplate
            SmsTemplateType.UDHAR_CONFIRM -> s.udharTemplate
            SmsTemplateType.JAMA_CONFIRM -> s.jamaTemplate
            SmsTemplateType.BALANCE_SUMMARY -> s.summaryTemplate
            SmsTemplateType.CUSTOM -> s.reminderTemplate
        }
        return smsManager.renderTemplate(
            template = template,
            customerName = customer.name,
            businessName = s.businessName,
            amount = latestAmount,
            netBalance = customer.netBalance
        )
    }

    fun openManualSmsComposer(
        customer: CustomerLedgerSummary?,
        mobile: String,
        templateType: SmsTemplateType,
        messageBody: String
    ) {
        viewModelScope.launch {
            val res = smsManager.openManualSmsComposer(
                customerId = customer?.id,
                customerName = customer?.name ?: "Customer",
                mobile = mobile,
                templateType = templateType,
                messageBody = messageBody
            )
            res.onFailure {
                _toastEvents.tryEmit(it.localizedMessage ?: "Unable to open SMS composer")
            }
        }
    }

    fun sendDirectSms(
        customer: CustomerLedgerSummary?,
        mobile: String,
        templateType: SmsTemplateType,
        messageBody: String,
        userConsented: Boolean
    ) {
        viewModelScope.launch {
            val res = smsManager.sendDirectSmsWithConsent(
                customerId = customer?.id,
                customerName = customer?.name ?: "Customer",
                mobile = mobile,
                templateType = templateType,
                messageBody = messageBody,
                userConsented = userConsented
            )
            res.onSuccess {
                _toastEvents.tryEmit("SMS queued via Android SmsManager")
            }.onFailure {
                _toastEvents.tryEmit(it.localizedMessage ?: "Direct SMS failed")
            }
        }
    }

    fun triggerScheduledRemindersNow() {
        ReminderWorker.runImmediateReminderCheck(appContext)
        _toastEvents.tryEmit("Scheduled reminder check queued with WorkManager")
    }

    // --- Settings, Themes, Profile, Backup ---
    fun setThemePreset(preset: AppThemePreset) {
        viewModelScope.launch { preferencesStore.setThemePreset(preset) }
    }

    fun setCustomAccentColor(colorArgb: Long) {
        viewModelScope.launch { preferencesStore.setCustomAccentColor(colorArgb) }
    }

    fun setCustomBackgroundStyle(style: CustomBackgroundStyle) {
        viewModelScope.launch { preferencesStore.setCustomBackgroundStyle(style) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesStore.setLanguage(language) }
    }

    fun updateProfile(businessName: String, ownerMobile: String) {
        viewModelScope.launch {
            preferencesStore.updateProfile(businessName, ownerMobile)
            _toastEvents.tryEmit("Business profile updated")
        }
    }

    fun updateProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            val path = copyImageToInternalStorage(uri, "owner_profile_${System.currentTimeMillis()}")
            if (path != null) {
                preferencesStore.setProfilePhotoPath(path)
                _toastEvents.tryEmit("Profile photo updated")
            }
        }
    }

    fun updateNotificationSettings(
        enabled: Boolean,
        onTransaction: Boolean,
        paymentReminder: Boolean,
        dailySummary: Boolean,
        scheduledDailySummary: Boolean
    ) {
        viewModelScope.launch {
            preferencesStore.updateNotificationSettings(
                enabled,
                onTransaction,
                paymentReminder,
                dailySummary,
                scheduledDailySummary
            )
            if (scheduledDailySummary) {
                ReminderWorker.schedulePeriodicReminders(appContext)
            }
        }
    }

    fun setAiEngineMode(mode: AiEngineMode) {
        viewModelScope.launch { preferencesStore.setAiEngineMode(mode) }
    }

    fun updateSmsTemplates(reminder: String, udhar: String, jama: String, summary: String) {
        viewModelScope.launch {
            preferencesStore.updateSmsTemplates(reminder, udhar, jama, summary)
            _toastEvents.tryEmit("SMS templates saved")
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonText = withContext(Dispatchers.IO) { backupManager.exportFullJsonBackup() }
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(jsonText.toByteArray(Charsets.UTF_8))
                    }
                }
                _lastBackupMessage.value = "JSON backup saved successfully."
                _toastEvents.tryEmit("JSON backup exported")
            } catch (e: Exception) {
                _toastEvents.tryEmit("Export failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportCsvToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val csvText = withContext(Dispatchers.IO) { backupManager.exportLedgerCsv() }
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(csvText.toByteArray(Charsets.UTF_8))
                    }
                }
                _lastBackupMessage.value = "CSV ledger exported successfully."
                _toastEvents.tryEmit("CSV exported")
            } catch (e: Exception) {
                _toastEvents.tryEmit("CSV export failed: ${e.localizedMessage}")
            }
        }
    }

    fun importBackupOrLegacyFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        .orEmpty()
                }
                importBackupOrLegacyRawJson(raw)
            } catch (e: Exception) {
                _toastEvents.tryEmit("Import failed: ${e.localizedMessage}")
            }
        }
    }

    fun importBackupOrLegacyRawJson(rawJson: String) {
        viewModelScope.launch {
            try {
                val res: MigrationImportResult = withContext(Dispatchers.IO) {
                    backupManager.importJsonNonDestructive(rawJson)
                }
                val summary = "${res.sourceFormatDetected}: +${res.customersImported} customers, +${res.transactionsImported} transactions imported (${res.customersSkippedExisting} existing customers & ${res.transactionsSkippedExisting} existing entries safely preserved)."
                _lastBackupMessage.value = summary
                _toastEvents.tryEmit("Data migrated safely")
            } catch (e: Exception) {
                _toastEvents.tryEmit("Migration failed: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun copyImageToInternalStorage(uri: Uri, prefix: String): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(appContext.filesDir, "avatars").apply { mkdirs() }
                val dest = File(dir, "$prefix.jpg")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                dest.absolutePath
            }.getOrNull()
        }
    }
}
