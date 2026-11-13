package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LedgerRepository
import com.example.export.ReportExporter
import com.example.model.AppThemeId
import com.example.model.AuditAction
import com.example.model.AuditLog
import com.example.model.CalcHistoryEntry
import com.example.model.CalculatorEvaluator
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.LocalBackupSnapshot
import com.example.model.SmsDispatchMethod
import com.example.model.TransactionType
import com.example.model.UserProfile
import com.example.security.AppPreferencesRepository
import com.example.security.LocalAppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CalculatorUiState(
    val expression: String = "0",
    val previewResult: String = "0",
    val memoryValue: Double = 0.0,
    val hasError: Boolean = false,
    val justEvaluated: Boolean = false
)

class MainViewModel(
    private val repository: LedgerRepository,
    private val prefsRepo: AppPreferencesRepository
) : ViewModel() {

    private val _calcState = MutableStateFlow(CalculatorUiState())
    val calcState: StateFlow<CalculatorUiState> = _calcState.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()

    private val _isLocked = MutableStateFlow<Boolean?>(null)
    val isLocked: StateFlow<Boolean?> = _isLocked.asStateFlow()

    private val _localSnapshots = MutableStateFlow<List<LocalBackupSnapshot>>(emptyList())
    val localSnapshots: StateFlow<List<LocalBackupSnapshot>> = _localSnapshots.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessages = _toastMessages.asSharedFlow()

    val localPrefs: StateFlow<LocalAppPreferences> = prefsRepo.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = LocalAppPreferences()
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .catch { error ->
            handleError("observeUserProfile", error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val customersState: StateFlow<UiState<List<Customer>>> = repository.observeCustomers()
        .map<List<Customer>, UiState<List<Customer>>> { UiState.Success(it) }
        .catch { error ->
            val msg = handleError("observeCustomers", error)
            emit(UiState.Error(msg))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val transactionsState: StateFlow<UiState<List<LedgerTransaction>>> =
        repository.observeTransactions()
            .map<List<LedgerTransaction>, UiState<List<LedgerTransaction>>> { UiState.Success(it) }
            .catch { error ->
                val msg = handleError("observeTransactions", error)
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val auditLogsState: StateFlow<UiState<List<AuditLog>>> =
        repository.observeAuditLogs()
            .map<List<AuditLog>, UiState<List<AuditLog>>> { UiState.Success(it) }
            .catch { error ->
                val msg = handleError("observeAuditLogs", error)
                emit(UiState.Error(msg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val calcHistory: StateFlow<List<CalcHistoryEntry>> =
        repository.observeCalcHistory()
            .catch { error ->
                handleError("observeCalcHistory", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch {
            repository.ensureOwnerProfile()
        }
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                if (_isLocked.value == null) {
                    _isLocked.value = prefs.pinLockEnabled
                }
            }
        }
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockAppNow() {
        if (localPrefs.value.pinLockEnabled) {
            _isLocked.value = true
        }
    }

    fun emitMessage(message: String) {
        _toastMessages.tryEmit(message)
    }

    // --- Calculator Actions ---
    fun onCalcKey(key: String) {
        val current = _calcState.value
        when (key) {
            "AC" -> {
                _calcState.value = current.copy(
                    expression = "0",
                    previewResult = "0",
                    hasError = false,
                    justEvaluated = false
                )
            }
            "DEL" -> {
                val nextExpr = if (current.justEvaluated || current.expression.length <= 1) {
                    "0"
                } else {
                    current.expression.dropLast(1)
                }
                updateExprAndPreview(nextExpr, justEvaluated = false)
            }
            "=" -> {
                val res = CalculatorEvaluator.evaluate(current.expression)
                if (res.isSuccess) {
                    val value = res.getOrThrow()
                    val expr = current.expression
                    _calcState.value = current.copy(
                        expression = value,
                        previewResult = value,
                        hasError = false,
                        justEvaluated = true
                    )
                    if (expr != value) {
                        viewModelScope.launch {
                            repository.saveCalcHistory(expr, value)
                        }
                    }
                } else {
                    _calcState.value = current.copy(hasError = true, previewResult = "Error")
                }
            }
            "±" -> {
                val expr = current.expression
                val next = if (expr.startsWith("-")) expr.drop(1) else if (expr == "0") "0" else "-$expr"
                updateExprAndPreview(next, justEvaluated = false)
            }
            "x²" -> {
                val res = CalculatorEvaluator.evaluate(current.expression)
                if (res.isSuccess) {
                    val v = res.getOrThrow().toDoubleOrNull() ?: 0.0
                    val sq = CalculatorEvaluator.formatNumber(v * v)
                    updateExprAndPreview(sq, justEvaluated = true)
                }
            }
            "MC" -> {
                _calcState.value = current.copy(memoryValue = 0.0)
                emitMessage("Calculator memory cleared")
            }
            "MR" -> {
                val memStr = CalculatorEvaluator.formatNumber(current.memoryValue)
                val next = if (current.expression == "0" || current.justEvaluated) memStr else current.expression + memStr
                updateExprAndPreview(next, justEvaluated = false)
            }
            "M+" -> {
                val v = current.previewResult.toDoubleOrNull() ?: 0.0
                _calcState.value = current.copy(memoryValue = current.memoryValue + v)
                emitMessage("Added $v to memory")
            }
            "M-" -> {
                val v = current.previewResult.toDoubleOrNull() ?: 0.0
                _calcState.value = current.copy(memoryValue = current.memoryValue - v)
                emitMessage("Subtracted $v from memory")
            }
            else -> {
                val isOperator = key in setOf("+", "−", "×", "÷", "%")
                val base = when {
                    current.justEvaluated && !isOperator -> ""
                    current.expression == "0" && !isOperator && key != "." -> ""
                    else -> current.expression
                }
                val nextExpr = base + key
                updateExprAndPreview(nextExpr, justEvaluated = false)
            }
        }
    }

    fun loadValueIntoCalculator(value: String) {
        val sanitized = value.trim().ifBlank { "0" }
        updateExprAndPreview(sanitized, justEvaluated = true)
    }

    fun deleteCalcHistoryEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteCalcHistory(entryId)
        }
    }

    private fun updateExprAndPreview(expr: String, justEvaluated: Boolean) {
        val res = CalculatorEvaluator.evaluate(expr)
        _calcState.value = _calcState.value.copy(
            expression = expr,
            previewResult = res.getOrElse { _calcState.value.previewResult },
            hasError = false,
            justEvaluated = justEvaluated
        )
    }

    // --- Ledger & Customers ---
    fun selectCustomer(customerId: String?) {
        _selectedCustomerId.value = customerId
    }

    fun addCustomer(name: String, phone: String, notes: String, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.createCustomer(name, phone, notes)
            res.onSuccess { id ->
                _selectedCustomerId.value = id
                emitMessage("Customer \"$name\" saved to offline vault")
                onCreated(id)
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to add customer")
            }
        }
    }

    fun updateCustomer(customerId: String, name: String, phone: String, notes: String) {
        viewModelScope.launch {
            val res = repository.updateCustomerDetails(customerId, name, phone, notes)
            res.onSuccess {
                emitMessage("Updated customer details")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to update customer")
            }
        }
    }

    fun addTransaction(
        customer: Customer,
        type: TransactionType,
        amountRupees: Double,
        note: String,
        billRef: String,
        smsSent: Boolean,
        smsMethod: SmsDispatchMethod
    ) {
        val paise = kotlin.math.round(amountRupees * 100.0).toLong()
        if (paise <= 0L) {
            emitMessage("Please enter a valid positive amount")
            return
        }
        viewModelScope.launch {
            val res = repository.recordTransaction(
                customer = customer,
                type = type,
                amountPaise = paise,
                note = note,
                billReference = billRef,
                smsReceiptSent = smsSent,
                smsMethod = smsMethod
            )
            res.onSuccess {
                emitMessage("${type.label} saved (${LedgerRepository.formatCurrencyPaise(paise)})")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to save transaction")
            }
        }
    }

    fun logAudit(action: AuditAction, entityId: String, summary: String, details: String = "") {
        viewModelScope.launch {
            repository.logAuditEvent(action, entityId, summary, details)
        }
    }

    // --- Settings, Theme, Security & Local Backup ---
    fun setTheme(themeId: AppThemeId) {
        viewModelScope.launch {
            prefsRepo.setTheme(themeId)
            repository.updateOwnerProfileSettings(preferredTheme = themeId)
        }
    }

    fun setGlassIntensity(value: Float) {
        viewModelScope.launch {
            prefsRepo.setGlassIntensity(value)
        }
    }

    fun updateBusinessProfile(businessName: String, phone: String) {
        viewModelScope.launch {
            val res = repository.updateOwnerProfileSettings(
                businessName = businessName,
                phone = phone
            )
            res.onSuccess {
                emitMessage("Business profile saved offline")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun enablePinLock(pin: String) {
        viewModelScope.launch {
            prefsRepo.setPinCode(pin)
            repository.logAuditEvent(
                action = AuditAction.SECURITY_CHANGE,
                entityId = "pin_lock",
                summary = "Enabled optional 4-digit encrypted PIN lock"
            )
            emitMessage("PIN lock enabled")
        }
    }

    fun disablePinLock() {
        viewModelScope.launch {
            prefsRepo.disablePinCode()
            repository.logAuditEvent(
                action = AuditAction.SECURITY_CHANGE,
                entityId = "pin_lock",
                summary = "Disabled optional PIN & biometric lock"
            )
            emitMessage("App lock disabled")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setBiometricEnabled(enabled)
            repository.logAuditEvent(
                action = AuditAction.SECURITY_CHANGE,
                entityId = "biometric",
                summary = if (enabled) "Enabled optional biometric unlock" else "Disabled biometric unlock"
            )
        }
    }

    fun setDirectSmsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDirectSmsEnabled(enabled) }
    }

    fun setContactsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setContactsEnabled(enabled) }
    }

    fun setCameraScanEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setCameraScanEnabled(enabled) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setRemindersEnabled(enabled) }
    }

    fun refreshLocalSnapshots(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(context.filesDir, "backups").apply { mkdirs() }
            val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".sscbak") }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()
            _localSnapshots.value = files.map { f ->
                LocalBackupSnapshot(
                    fileName = f.name,
                    filePath = f.absolutePath,
                    sizeBytes = f.length(),
                    modifiedEpochMs = f.lastModified()
                )
            }
        }
    }

    fun createLocalVaultSnapshot(
        context: Context,
        passphrase: String,
        customers: List<Customer>,
        transactions: List<LedgerTransaction>
    ) {
        viewModelScope.launch {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "SimpleSmsCalc_$stamp.sscbak"
            withContext(Dispatchers.IO) {
                val dir = File(context.filesDir, "backups").apply { mkdirs() }
                val target = File(dir, fileName)
                target.outputStream().use { out ->
                    ReportExporter.exportEncryptedBackup(out, passphrase, customers, transactions)
                }
            }
            val now = System.currentTimeMillis()
            repository.updateOwnerProfileSettings(lastBackupEpochMs = now)
            repository.logAuditEvent(
                action = AuditAction.BACKUP_SYNC,
                entityId = fileName,
                summary = "Created encrypted local snapshot ($fileName)"
            )
            refreshLocalSnapshots(context)
            emitMessage("Encrypted local snapshot saved: $fileName")
        }
    }

    fun markExternalBackupSaved(fileName: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.updateOwnerProfileSettings(lastBackupEpochMs = now)
            repository.logAuditEvent(
                action = AuditAction.BACKUP_SYNC,
                entityId = fileName,
                summary = "Exported AES-256 encrypted backup to local storage"
            )
            emitMessage("Encrypted backup (.sscbak) saved to device storage!")
        }
    }

    fun deleteLocalSnapshot(context: Context, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
            refreshLocalSnapshots(context)
        }
    }

    fun restoreBackupData(customers: List<Customer>, transactions: List<LedgerTransaction>) {
        viewModelScope.launch {
            val res = repository.restoreCustomersAndTransactions(customers, transactions)
            res.onSuccess { count ->
                emitMessage("Restored $count records into encrypted local vault")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Restore failed")
            }
        }
    }

    private fun handleError(opName: String, error: Throwable): String {
        if (error is CancellationException) throw error
        Log.w(TAG, "Error in $opName", error)
        return error.localizedMessage ?: "Unable to read encrypted local database"
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
