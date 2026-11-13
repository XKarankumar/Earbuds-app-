package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FirebaseAiIntelligenceService
import com.example.ai.OnDeviceOcrScanner
import com.example.ai.PhoneValidatorEngine
import com.example.data.SearchHistoryRepository
import com.example.export.KaranPdfExporter
import com.example.model.AppLanguage
import com.example.model.AppThemeId
import com.example.model.AsyncUiState
import com.example.model.ChatMessage
import com.example.model.DashboardCardId
import com.example.model.HistoryRecordType
import com.example.model.NumberLookupReport
import com.example.model.PhotoAnalysisReport
import com.example.model.SearchHistoryRecord
import com.example.model.SpamAnalysisReport
import com.example.security.AppPreferencesRepository
import com.example.security.LocalAppPreferences
import com.example.security.RateLimitSnapshot
import com.example.security.RateLimiter
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "MainViewModel"

class MainViewModel(
    application: Application,
    private val historyRepo: SearchHistoryRepository,
    private val prefsRepo: AppPreferencesRepository,
    private val aiService: FirebaseAiIntelligenceService = FirebaseAiIntelligenceService(),
    private val rateLimiter: RateLimiter = RateLimiter(maxRequestsPerMinute = 12)
) : AndroidViewModel(application) {

    val localPrefs: StateFlow<LocalAppPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocalAppPreferences())

    val searchHistory: StateFlow<List<SearchHistoryRecord>> = historyRepo.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rateLimitSnapshot: StateFlow<RateLimitSnapshot> = rateLimiter.snapshot

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val _isLocked = MutableStateFlow<Boolean?>(null)
    val isLocked: StateFlow<Boolean?> = _isLocked.asStateFlow()

    private val _activeScreen = MutableStateFlow<DashboardCardId?>(null)
    val activeScreen: StateFlow<DashboardCardId?> = _activeScreen.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    // Number & Business Lookup State
    private val _lookupQuery = MutableStateFlow("")
    val lookupQuery: StateFlow<String> = _lookupQuery.asStateFlow()

    private val _selectedRegion = MutableStateFlow("IN")
    val selectedRegion: StateFlow<String> = _selectedRegion.asStateFlow()

    private val _businessHint = MutableStateFlow("")
    val businessHint: StateFlow<String> = _businessHint.asStateFlow()

    private val _isBusinessLookupMode = MutableStateFlow(false)
    val isBusinessLookupMode: StateFlow<Boolean> = _isBusinessLookupMode.asStateFlow()

    private val _lookupState = MutableStateFlow<AsyncUiState<NumberLookupReport>>(AsyncUiState.Idle)
    val lookupState: StateFlow<AsyncUiState<NumberLookupReport>> = _lookupState.asStateFlow()

    // Photo Scanner State
    private val _selectedPhotoUri = MutableStateFlow<Uri?>(null)
    val selectedPhotoUri: StateFlow<Uri?> = _selectedPhotoUri.asStateFlow()

    private val _photoState = MutableStateFlow<AsyncUiState<PhotoAnalysisReport>>(AsyncUiState.Idle)
    val photoState: StateFlow<AsyncUiState<PhotoAnalysisReport>> = _photoState.asStateFlow()

    private var cachedScaledBitmap: Bitmap? = null
    private var cachedOcrText: String = ""
    private var cachedOcrPhones: List<String> = emptyList()

    // Spam Detector State
    private val _spamInput = MutableStateFlow("")
    val spamInput: StateFlow<String> = _spamInput.asStateFlow()

    private val _spamState = MutableStateFlow<AsyncUiState<SpamAnalysisReport>>(AsyncUiState.Idle)
    val spamState: StateFlow<AsyncUiState<SpamAnalysisReport>> = _spamState.asStateFlow()

    // AI Assistant Chat State
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "नमस्ते! मैं Karan AI Assistant हूँ। आप मुझसे किसी फ़ोन नंबर की सुरक्षा जाँच, संदिग्ध SMS/लिंक की पहचान, या Security Report (Hindi / English) बनाने के लिए पूछ सकते हैं।\n\nHello! Ask me to explain any lookup result, inspect a suspicious message, or generate a bilingual security report.",
                confidenceTag = "Ready",
                sourceTag = "Firebase AI Logic • Gemini 3.8 Flash"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _attachedContextSummary = MutableStateFlow<String?>(null)
    val attachedContextSummary: StateFlow<String?> = _attachedContextSummary.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            _isLocked.value = prefs.pinLockEnabled
        }
        viewModelScope.launch {
            delay(1100L)
            _showSplash.value = false
        }
    }

    fun dismissSplash() {
        _showSplash.value = false
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockAppNow() {
        if (localPrefs.value.pinLockEnabled) {
            _isLocked.value = true
        } else {
            emitToast("Set a 4-digit PIN in Security Center to enable vault lock.")
        }
    }

    fun navigateTo(card: DashboardCardId?) {
        when (card) {
            DashboardCardId.BUSINESS_LOOKUP -> {
                _isBusinessLookupMode.value = true
                _activeScreen.value = DashboardCardId.NUMBER_LOOKUP
            }
            DashboardCardId.NUMBER_LOOKUP -> {
                _isBusinessLookupMode.value = false
                _activeScreen.value = DashboardCardId.NUMBER_LOOKUP
            }
            else -> _activeScreen.value = card
        }
    }

    fun navigateBackHome() {
        _activeScreen.value = null
    }

    fun toggleQuickTheme() {
        val current = localPrefs.value.themeId
        val next = if (current == AppThemeId.WHITE_GLASS) {
            AppThemeId.DARK_3D_GLASS
        } else {
            AppThemeId.WHITE_GLASS
        }
        setTheme(next)
    }

    fun setTheme(themeId: AppThemeId) {
        viewModelScope.launch { prefsRepo.setTheme(themeId) }
    }

    fun setGlassIntensity(value: Float) {
        viewModelScope.launch { prefsRepo.setGlassIntensity(value) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { prefsRepo.setLanguage(language) }
    }

    fun setSaveSearchHistory(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSaveSearchHistory(enabled) }
    }

    fun setPinCode(pin: String) {
        viewModelScope.launch {
            prefsRepo.setPinCode(pin)
            emitToast("Local PIN vault protection enabled.")
        }
    }

    fun disablePinCode() {
        viewModelScope.launch {
            prefsRepo.disablePinCode()
            emitToast("Vault lock disabled — Guest Mode active.")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setBiometricEnabled(enabled) }
    }

    fun setCameraScanEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setCameraScanEnabled(enabled) }
    }

    fun updateLookupQuery(value: String) {
        _lookupQuery.value = value
    }

    fun updateSelectedRegion(regionCode: String) {
        _selectedRegion.value = regionCode
    }

    fun updateBusinessHint(hint: String) {
        _businessHint.value = hint
    }

    fun setBusinessMode(enabled: Boolean) {
        _isBusinessLookupMode.value = enabled
    }

    fun lookupPhoneNumber(customNumber: String? = null, businessMode: Boolean = _isBusinessLookupMode.value) {
        val query = (customNumber ?: _lookupQuery.value).trim()
        if (query.isBlank()) {
            _lookupState.value = AsyncUiState.Error("Please enter a phone number or official helpline code.")
            return
        }
        _lookupQuery.value = query
        _isBusinessLookupMode.value = businessMode

        val validation = PhoneValidatorEngine.validate(query, _selectedRegion.value)
        val acquired = rateLimiter.tryAcquire()
        if (acquired.isFailure) {
            _lookupState.value = AsyncUiState.Error(
                acquired.exceptionOrNull()?.message ?: "Rate limit exceeded.",
                isRateLimited = true
            )
            return
        }

        viewModelScope.launch {
            _lookupState.value = AsyncUiState.Loading(
                "Validating ${validation.internationalFormat} (${validation.countryName}) & running AI fraud check..."
            )
            try {
                val report = aiService.analyzePhoneNumber(
                    validation = validation,
                    businessQueryHint = _businessHint.value.trim(),
                    isBusinessMode = businessMode
                )
                _lookupState.value = AsyncUiState.Success(report)
                _attachedContextSummary.value = buildNumberContextString(report)

                if (localPrefs.value.saveSearchHistory) {
                    historyRepo.saveRecord(
                        recordType = if (businessMode) HistoryRecordType.BUSINESS else HistoryRecordType.NUMBER,
                        title = report.validation.internationalFormat,
                        subtitle = "${report.publicCallerOrBusinessName} • ${report.validation.countryName} (${report.validation.numberType})",
                        riskLevel = report.riskLevel,
                        riskScore = report.spamRiskScore,
                        summaryText = "${report.summaryEn}\n\n${report.summaryHi}",
                        sourceAttribution = report.sourceAttribution
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Lookup failed", e)
                _lookupState.value = AsyncUiState.Error(
                    e.localizedMessage ?: "Unable to complete AI phone verification."
                )
            }
        }
    }

    fun jumpToNumberLookup(phone: String) {
        _lookupQuery.value = phone
        _isBusinessLookupMode.value = false
        _activeScreen.value = DashboardCardId.NUMBER_LOOKUP
        lookupPhoneNumber(phone, businessMode = false)
    }

    fun scanPhotoUri(uri: Uri) {
        _selectedPhotoUri.value = uri
        viewModelScope.launch {
            _photoState.value = AsyncUiState.Loading("Running on-device ML Kit OCR to extract text & phone numbers...")
            try {
                val ocr = OnDeviceOcrScanner.scanUri(
                    context = getApplication(),
                    uri = uri,
                    defaultRegion = _selectedRegion.value
                )
                cachedScaledBitmap = ocr.scaledBitmap
                cachedOcrText = ocr.rawText
                cachedOcrPhones = ocr.extractedPhoneNumbers

                val acquired = rateLimiter.tryAcquire()
                if (acquired.isFailure) {
                    _photoState.value = AsyncUiState.Error(
                        acquired.exceptionOrNull()?.message ?: "Rate limit exceeded.",
                        isRateLimited = true
                    )
                    return@launch
                }

                _photoState.value = AsyncUiState.Loading("Running HTTPS AI image & object analysis (no facial ID)...")
                val report = aiService.analyzePhoto(
                    bitmap = ocr.scaledBitmap,
                    ocrText = ocr.rawText,
                    ocrPhones = ocr.extractedPhoneNumbers
                )
                _photoState.value = AsyncUiState.Success(report)
                _attachedContextSummary.value =
                    "Photo Scan Summary: ${report.imageDescription}\nExtracted Numbers: ${report.extractedPhoneNumbers.joinToString(", ").ifBlank { "None" }}\nOCR Text: ${report.visibleTextSummary.take(250)}"

                if (localPrefs.value.saveSearchHistory) {
                    historyRepo.saveRecord(
                        recordType = HistoryRecordType.PHOTO,
                        title = "AI Photo Scan (${report.extractedPhoneNumbers.size} numbers)",
                        subtitle = report.detectedObjects.take(3).joinToString(", ").ifBlank { "Image OCR" },
                        riskLevel = "Info",
                        riskScore = 0,
                        summaryText = "${report.imageDescription}\nNumbers: ${report.extractedPhoneNumbers.joinToString(", ").ifBlank { "None" }}",
                        sourceAttribution = report.sourceAttribution
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo analysis failed", e)
                _photoState.value = AsyncUiState.Error(
                    e.localizedMessage ?: "Photo analysis failed. Please try another image."
                )
            }
        }
    }

    fun updateSpamInput(value: String) {
        _spamInput.value = value
    }

    fun analyzeSpamMessage(inputType: String = "Suspicious Message / Number") {
        val text = _spamInput.value.trim()
        if (text.isBlank()) {
            _spamState.value = AsyncUiState.Error("Paste a suspicious SMS, WhatsApp message, URL, or phone number to analyze.")
            return
        }
        val acquired = rateLimiter.tryAcquire()
        if (acquired.isFailure) {
            _spamState.value = AsyncUiState.Error(
                acquired.exceptionOrNull()?.message ?: "Rate limit active.",
                isRateLimited = true
            )
            return
        }

        viewModelScope.launch {
            _spamState.value = AsyncUiState.Loading("Inspecting phishing cues, urgency triggers & fraud patterns...")
            try {
                val report = aiService.analyzeSpamOrMessage(inputType, text)
                _spamState.value = AsyncUiState.Success(report)
                _attachedContextSummary.value =
                    "Spam Scan [${report.scamCategory}] Risk: ${report.riskLevel} (${report.spamRiskScore}/100)\n${report.explanationEn}"

                if (localPrefs.value.saveSearchHistory) {
                    historyRepo.saveRecord(
                        recordType = HistoryRecordType.SPAM,
                        title = report.scamCategory,
                        subtitle = report.inputPreview,
                        riskLevel = report.riskLevel,
                        riskScore = report.spamRiskScore,
                        summaryText = "${report.explanationEn}\n\n${report.explanationHi}",
                        sourceAttribution = report.sourceAttribution
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Spam analysis failed", e)
                _spamState.value = AsyncUiState.Error(
                    e.localizedMessage ?: "Could not analyze message for spam."
                )
            }
        }
    }

    fun sendChatMessage(prompt: String, includeAttachedContext: Boolean = true) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _isChatStreaming.value) return

        val acquired = rateLimiter.tryAcquire()
        if (acquired.isFailure) {
            emitToast(acquired.exceptionOrNull()?.message ?: "Rate limit exceeded.")
            return
        }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            isStreaming = true,
            confidenceTag = "Analyzing...",
            sourceTag = "Firebase AI Logic • Gemini 3.8 Flash"
        )

        val priorHistory: List<Content> = _chatMessages.value
            .filter { it.id != "welcome" && it.text.isNotBlank() }
            .takeLast(8)
            .map { msg ->
                content(if (msg.isUser) "user" else "model") { text(msg.text) }
            }

        _chatMessages.value = _chatMessages.value + userMsg + placeholderMsg
        _isChatStreaming.value = true

        viewModelScope.launch {
            try {
                val contextStr = if (includeAttachedContext) _attachedContextSummary.value else null
                aiService.streamAssistantReply(
                    userMessage = trimmed,
                    historyContent = priorHistory,
                    preferredLanguage = localPrefs.value.language,
                    attachedContext = contextStr
                ).collect { partial ->
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == assistantMsgId) {
                            msg.copy(text = partial, isStreaming = true)
                        } else msg
                    }
                }
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == assistantMsgId) {
                        msg.copy(isStreaming = false, confidenceTag = "Verified AI")
                    } else msg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat streaming failed", e)
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == assistantMsgId) {
                        msg.copy(
                            text = "Error: ${e.localizedMessage ?: "Unable to reach Firebase AI Logic."}",
                            isStreaming = false,
                            confidenceTag = "Error"
                        )
                    } else msg
                }
            } finally {
                _isChatStreaming.value = false
            }
        }
    }

    fun openAssistantWithContext(prompt: String, contextText: String) {
        _attachedContextSummary.value = contextText
        _activeScreen.value = DashboardCardId.AI_ASSISTANT
        sendChatMessage(prompt, includeAttachedContext = true)
    }

    fun deleteHistoryRecord(id: String) {
        viewModelScope.launch {
            historyRepo.deleteRecord(id)
            emitToast("Search record deleted from encrypted vault.")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepo.clearAll()
            emitToast("All local search history cleared.")
        }
    }

    fun exportAndSharePdf(
        context: Context,
        title: String,
        records: List<SearchHistoryRecord>
    ) {
        viewModelScope.launch {
            try {
                val file = KaranPdfExporter.exportRecordsToPdf(context, title, records)
                KaranPdfExporter.sharePdfFile(context, file, title)
                emitToast("PDF report ready to share.")
            } catch (e: Exception) {
                Log.e(TAG, "PDF export failed", e)
                emitToast("PDF export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun exportCurrentLookupPdf(context: Context, report: NumberLookupReport) {
        val record = SearchHistoryRecord(
            id = "temp",
            recordType = if (report.isBusinessMode) HistoryRecordType.BUSINESS else HistoryRecordType.NUMBER,
            title = report.validation.internationalFormat,
            subtitle = "${report.publicCallerOrBusinessName} (${report.validation.countryName})",
            riskLevel = report.riskLevel,
            riskScore = report.spamRiskScore,
            summaryText = buildString {
                appendLine("E.164: ${report.validation.e164Format}")
                appendLine("Type: ${report.validation.numberType}")
                appendLine("Category: ${report.entityCategory}")
                appendLine("Carrier Note: ${report.carrierNetworkNote}")
                appendLine("Summary (EN): ${report.summaryEn}")
                appendLine("Summary (HI): ${report.summaryHi}")
            },
            sourceAttribution = report.sourceAttribution,
            createdAtMillis = report.analyzedAtMillis
        )
        exportAndSharePdf(context, "Number Verification Report", listOf(record))
    }

    fun refreshRateLimiter() {
        rateLimiter.refreshStatus()
    }

    fun emitToast(message: String) {
        _toastMessages.tryEmit(message)
    }

    private fun buildNumberContextString(report: NumberLookupReport): String {
        return buildString {
            appendLine("Number: ${report.validation.internationalFormat} (${report.validation.countryName}, ${report.validation.numberType})")
            appendLine("Public Entity: ${report.publicCallerOrBusinessName} [${report.entityCategory}]")
            appendLine("Risk: ${report.riskLevel} (${report.spamRiskScore}/100)")
            appendLine("Summary: ${report.summaryEn}")
        }
    }
}
