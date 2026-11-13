package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.local.CalcHistoryEntity
import com.example.data.local.SmsLogEntity
import com.example.domain.CalculatorEngine
import com.example.export.LedgerPdfExporter
import com.example.model.AnimationPreset
import com.example.model.AppDestination
import com.example.model.CalcRecordCategory
import com.example.model.CurrencyOption
import com.example.model.CustomerSummary
import com.example.model.DashboardMetrics
import com.example.model.OceanEmeraldMode
import com.example.model.RecentTransactionItem
import com.example.model.TransactionType
import com.example.security.AppPreferencesRepository
import com.example.security.LocalAppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "0",
    val preview: String? = null,
    val memoryValue: Double = 0.0,
    val hasEvaluated: Boolean = false,
    val errorText: String? = null
)

data class SmsDraftState(
    val recipientName: String = "",
    val recipientPhone: String = "",
    val amountText: String = "",
    val templateIndex: Int = 0,
    val messageBody: String = ""
)

class MainViewModel(
    application: Application,
    private val repository: AppRepository,
    private val prefsRepo: AppPreferencesRepository
) : AndroidViewModel(application) {

    private val _backStack = MutableStateFlow<List<AppDestination>>(listOf(AppDestination.Calculator))
    val backStack: StateFlow<List<AppDestination>> = _backStack.asStateFlow()

    val currentDestination: AppDestination
        get() = _backStack.value.lastOrNull() ?: AppDestination.Calculator

    val prefs: StateFlow<LocalAppPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalAppPreferences())

    val customerSummaries: StateFlow<List<CustomerSummary>> = repository.customerSummariesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<RecentTransactionItem>> = repository.recentTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardMetrics: StateFlow<DashboardMetrics> = repository.dashboardMetricsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val calcHistory: StateFlow<List<CalcHistoryEntity>> = repository.calcHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smsLogs: StateFlow<List<SmsLogEntity>> = repository.smsLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _calcState = MutableStateFlow(CalculatorUiState())
    val calcState: StateFlow<CalculatorUiState> = _calcState.asStateFlow()

    private val _smsDraft = MutableStateFlow(SmsDraftState())
    val smsDraft: StateFlow<SmsDraftState> = _smsDraft.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessages = _toastMessages.asSharedFlow()

    // --- Navigation ---

    fun navigateToRootTab(destination: AppDestination) {
        _backStack.value = listOf(destination)
    }

    fun navigateTo(destination: AppDestination) {
        val current = _backStack.value
        if (current.lastOrNull() != destination) {
            _backStack.value = current + destination
        }
    }

    fun navigateBack(): Boolean {
        val current = _backStack.value
        return if (current.size > 1) {
            _backStack.value = current.dropLast(1)
            true
        } else if (current.firstOrNull() != AppDestination.Calculator) {
            _backStack.value = listOf(AppDestination.Calculator)
            true
        } else {
            false
        }
    }

    // --- Theme & Preferences ---

    fun toggleThemeMode() {
        val next = if (prefs.value.themeMode == OceanEmeraldMode.DARK) {
            OceanEmeraldMode.LIGHT
        } else {
            OceanEmeraldMode.DARK
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: OceanEmeraldMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setAnimationPreset(preset: AnimationPreset) {
        viewModelScope.launch { prefsRepo.setAnimationPreset(preset) }
    }

    fun setGlassIntensity(value: Float) {
        viewModelScope.launch { prefsRepo.setGlassIntensity(value) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticFeedback(enabled) }
    }

    fun setCurrency(option: CurrencyOption) {
        viewModelScope.launch { prefsRepo.setCurrency(option) }
    }

    fun setBusinessSignature(signature: String) {
        viewModelScope.launch {
            prefsRepo.setBusinessSignature(signature)
            emitToast("Signature saved")
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            prefsRepo.resetDefaults()
            emitToast("Ocean Emerald 3.5 defaults restored")
        }
    }

    // --- Calculator Logic ---

    fun onCalcKey(key: String) {
        val current = _calcState.value
        when (key) {
            "AC" -> {
                _calcState.value = current.copy(
                    expression = "",
                    result = "0",
                    preview = null,
                    hasEvaluated = false,
                    errorText = null
                )
            }
            "⌫" -> {
                if (current.expression.isNotEmpty()) {
                    val nextExpr = current.expression.dropLast(1)
                    _calcState.value = current.copy(
                        expression = nextExpr,
                        preview = CalculatorEngine.tryPreview(nextExpr),
                        hasEvaluated = false,
                        errorText = null
                    )
                }
            }
            "=" -> evaluateCurrentExpression()
            "±" -> toggleSign()
            "( )" -> insertParenthesis()
            "MC" -> {
                _calcState.value = current.copy(memoryValue = 0.0)
                emitToast("Memory cleared")
            }
            "MR" -> {
                val formatted = CalculatorEngine.formatNumber(current.memoryValue)
                appendToken(formatted)
            }
            "M+" -> {
                val activeVal = currentNumericValue()
                _calcState.value = current.copy(memoryValue = current.memoryValue + activeVal)
                emitToast("Added to Memory")
            }
            "M-" -> {
                val activeVal = currentNumericValue()
                _calcState.value = current.copy(memoryValue = current.memoryValue - activeVal)
                emitToast("Subtracted from Memory")
            }
            else -> appendToken(key)
        }
    }

    private fun appendToken(token: String) {
        val current = _calcState.value
        val isOperator = token in listOf("+", "−", "×", "÷", "%")
        val baseExpr = if (current.hasEvaluated) {
            if (isOperator) current.result else ""
        } else {
            current.expression
        }

        val nextExpr = if (isOperator && baseExpr.isEmpty() && token != "−") {
            "0$token"
        } else if (isOperator && baseExpr.isNotEmpty() && baseExpr.last().toString() in listOf("+", "−", "×", "÷")) {
            baseExpr.dropLast(1) + token
        } else {
            baseExpr + token
        }

        _calcState.value = current.copy(
            expression = nextExpr,
            preview = CalculatorEngine.tryPreview(nextExpr),
            hasEvaluated = false,
            errorText = null
        )
    }

    private fun insertParenthesis() {
        val expr = _calcState.value.expression
        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }
        val token = if (openCount > closeCount && expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')')) {
            ")"
        } else if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')')) {
            "×("
        } else {
            "("
        }
        appendToken(token)
    }

    private fun toggleSign() {
        val current = _calcState.value
        val expr = current.expression
        if (expr.isEmpty()) {
            appendToken("−")
            return
        }
        val toggled = if (expr.startsWith("−")) expr.drop(1) else "−$expr"
        _calcState.value = current.copy(
            expression = toggled,
            preview = CalculatorEngine.tryPreview(toggled),
            hasEvaluated = false,
            errorText = null
        )
    }

    private fun evaluateCurrentExpression() {
        val current = _calcState.value
        val expr = current.expression.ifBlank { current.result }
        when (val eval = CalculatorEngine.evaluate(expr)) {
            is CalculatorEngine.EvalResult.Success -> {
                _calcState.value = current.copy(
                    expression = expr,
                    result = eval.formatted,
                    preview = null,
                    hasEvaluated = true,
                    errorText = null
                )
                if (expr.any { it in listOf('+', '−', '×', '÷', '%', '*', '/') }) {
                    saveCalculationRecord(CalcRecordCategory.STANDARD, expr, eval.formatted)
                }
            }
            is CalculatorEngine.EvalResult.Error -> {
                _calcState.value = current.copy(errorText = eval.message)
            }
        }
    }

    private fun currentNumericValue(): Double {
        val current = _calcState.value
        val expr = current.expression
        if (expr.isNotEmpty()) {
            val eval = CalculatorEngine.evaluate(expr)
            if (eval is CalculatorEngine.EvalResult.Success) return eval.value
        }
        return current.result.toDoubleOrNull() ?: 0.0
    }

    fun loadHistoryIntoCalculator(expression: String, result: String) {
        _calcState.value = _calcState.value.copy(
            expression = expression,
            result = result,
            preview = null,
            hasEvaluated = true,
            errorText = null
        )
        navigateToRootTab(AppDestination.Calculator)
    }

    fun saveCalculationRecord(category: CalcRecordCategory, expression: String, result: String) {
        viewModelScope.launch {
            repository.saveCalculation(category, expression, result)
        }
    }

    fun deleteCalcHistory(id: Long) {
        viewModelScope.launch { repository.deleteCalculation(id) }
    }

    fun clearCalcHistory() {
        viewModelScope.launch {
            repository.clearCalculationHistory()
            emitToast("Calculation history cleared")
        }
    }

    // --- Hisab-Kitab & Customer Ledger ---

    fun observeCustomerTransactions(customerId: Long) =
        repository.observeCustomerTransactions(customerId)

    fun addCustomer(
        name: String,
        phone: String,
        notes: String,
        openingAmount: Double,
        openingType: TransactionType
    ) {
        if (name.isBlank()) {
            emitToast("Please enter a customer name")
            return
        }
        viewModelScope.launch {
            repository.addCustomer(name, phone, notes, openingAmount, openingType)
            emitToast("Customer \"$name\" added")
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            emitToast("Customer removed")
        }
    }

    fun addLedgerTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        note: String,
        expression: String = ""
    ) {
        if (amount <= 0.0) {
            emitToast("Enter an amount greater than 0")
            return
        }
        viewModelScope.launch {
            repository.addTransaction(customerId, type, amount, note, expression)
            emitToast("${type.label} saved")
        }
    }

    fun deleteLedgerTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            emitToast("Entry deleted")
        }
    }

    // --- SMS Composer & Zero-Permission Handoff ---

    fun prepareSmsForCalculation(expression: String, result: String) {
        val symbol = prefs.value.currency.symbol
        val sig = prefs.value.businessSignature
        val body = buildString {
            append("Calculation Summary:\n")
            if (expression.isNotBlank()) append("$expression = $result\n")
            else append("Total Amount: $symbol$result\n")
            append("Sent via Simple SMS Calculator ($sig)")
        }
        _smsDraft.value = _smsDraft.value.copy(
            amountText = result,
            templateIndex = 3,
            messageBody = body
        )
        navigateToRootTab(AppDestination.SmsCenter)
    }

    fun prepareSmsForCustomer(customer: CustomerSummary, templateIndex: Int = 0) {
        val symbol = prefs.value.currency.symbol
        val sig = prefs.value.businessSignature
        val body = buildCustomerTemplateMessage(
            customerName = customer.name,
            netBalance = customer.netBalance,
            totalJama = customer.totalJama,
            totalUdhaar = customer.totalUdhaar,
            templateIndex = templateIndex,
            currencySymbol = symbol,
            signature = sig
        )
        _smsDraft.value = SmsDraftState(
            recipientName = customer.name,
            recipientPhone = customer.phone,
            amountText = CalculatorEngine.formatNumber(kotlin.math.abs(customer.netBalance)),
            templateIndex = templateIndex,
            messageBody = body
        )
        navigateToRootTab(AppDestination.SmsCenter)
    }

    fun updateSmsDraft(
        recipientName: String? = null,
        recipientPhone: String? = null,
        amountText: String? = null,
        templateIndex: Int? = null,
        messageBody: String? = null
    ) {
        val current = _smsDraft.value
        _smsDraft.value = current.copy(
            recipientName = recipientName ?: current.recipientName,
            recipientPhone = recipientPhone ?: current.recipientPhone,
            amountText = amountText ?: current.amountText,
            templateIndex = templateIndex ?: current.templateIndex,
            messageBody = messageBody ?: current.messageBody
        )
    }

    fun applySmsTemplate(templateIndex: Int, selectedCustomer: CustomerSummary?) {
        val symbol = prefs.value.currency.symbol
        val sig = prefs.value.businessSignature
        val draft = _smsDraft.value
        val name = selectedCustomer?.name ?: draft.recipientName.ifBlank { "Customer" }
        val amount = draft.amountText.toDoubleOrNull()
            ?: selectedCustomer?.netBalance
            ?: 0.0
        val body = buildCustomerTemplateMessage(
            customerName = name,
            netBalance = amount,
            totalJama = selectedCustomer?.totalJama ?: 0.0,
            totalUdhaar = selectedCustomer?.totalUdhaar ?: amount,
            templateIndex = templateIndex,
            currencySymbol = symbol,
            signature = sig
        )
        _smsDraft.value = draft.copy(templateIndex = templateIndex, messageBody = body)
    }

    fun buildCustomerTemplateMessage(
        customerName: String,
        netBalance: Double,
        totalJama: Double,
        totalUdhaar: Double,
        templateIndex: Int,
        currencySymbol: String,
        signature: String
    ): String {
        val absStr = CalculatorEngine.formatCurrency(kotlin.math.abs(netBalance), currencySymbol)
        val jamaStr = CalculatorEngine.formatCurrency(totalJama, currencySymbol)
        val udhaarStr = CalculatorEngine.formatCurrency(totalUdhaar, currencySymbol)
        return when (templateIndex) {
            0 -> "Namaste $customerName ji, your current Hisab balance of $absStr is pending. Kindly settle at your convenience. Thank you! - $signature"
            1 -> "Thank you $customerName ji! Payment (Jama) of $absStr has been received and updated in your ledger. Total Jama: $jamaStr. - $signature"
            2 -> "Hisab Summary for $customerName:\nTotal Udhaar: $udhaarStr\nTotal Jama: $jamaStr\nNet Balance: $absStr\nRegards, $signature"
            else -> "Estimate / Calculation for $customerName: Total Amount $absStr. Sent from Simple SMS Calculator - $signature"
        }
    }

    fun launchSmsIntent(context: Context, phone: String, message: String, recipientName: String, templateName: String) {
        if (message.isBlank()) {
            emitToast("Enter an SMS message first")
            return
        }
        viewModelScope.launch {
            repository.saveSmsLog(recipientName, phone, message, templateName)
        }
        val cleanPhone = phone.trim()
        val uri = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
        val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching {
            context.startActivity(smsIntent)
            true
        }.getOrDefault(false)

        if (!launched) {
            sharePlainText(context, message, "Send SMS")
        }
    }

    fun deleteSmsLog(id: Long) {
        viewModelScope.launch { repository.deleteSmsLog(id) }
    }

    fun clearSmsLogs() {
        viewModelScope.launch {
            repository.clearSmsLogs()
            emitToast("SMS log cleared")
        }
    }

    // --- Utility Helpers (Clipboard, Share, Dial, PDF Export) ---

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        emitToast("Copied to clipboard")
    }

    fun sharePlainText(context: Context, text: String, chooserTitle: String = "Share Calculation") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    fun dialPhone(context: Context, phone: String) {
        if (phone.isBlank()) {
            emitToast("No phone number saved for this customer")
            return
        }
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone.trim())}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(dialIntent) }
            .onFailure { emitToast("No dialer app available") }
    }

    fun exportAndSharePdfReport(context: Context) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                LedgerPdfExporter.exportFullLedgerReport(
                    context = context,
                    metrics = dashboardMetrics.value,
                    customers = customerSummaries.value,
                    recentTransactions = recentTransactions.value,
                    currencySymbol = prefs.value.currency.symbol,
                    businessSignature = prefs.value.businessSignature
                )
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Hisab-Kitab PDF Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(chooser) }
                .onSuccess { emitToast("PDF report ready") }
                .onFailure { emitToast("PDF saved to exports cache") }
        }
    }

    fun emitToast(message: String) {
        _toastMessages.tryEmit(message)
    }
}
