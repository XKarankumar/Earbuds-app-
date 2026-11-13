package com.example.model

enum class AppThemeId(
    val displayName: String,
    val subtitle: String,
    val isDark: Boolean
) {
    DARK_3D_GLASS("Dark 3D Glass", "Obsidian depth with neon cyan & emerald glass", true),
    WHITE_GLASS("White Glass", "Crisp frosted crystal glass for daylight clarity", false),
    CYBER_AURORA("Cyber Aurora", "Midnight violet & luminous prism glass", true);

    companion object {
        fun fromKey(key: String?): AppThemeId {
            return entries.firstOrNull { it.name == key } ?: DARK_3D_GLASS
        }
    }
}

enum class AppLanguage(val label: String, val code: String) {
    BILINGUAL("EN + हिन्दी", "bilingual"),
    ENGLISH("English", "en"),
    HINDI("हिन्दी", "hi");

    companion object {
        fun fromKey(key: String?): AppLanguage {
            return entries.firstOrNull { it.name == key } ?: BILINGUAL
        }
    }
}

enum class DashboardCardId(
    val title: String,
    val subtitle: String,
    val badge: String
) {
    NUMBER_LOOKUP("Number Lookup", "International validation & AI risk", "E.164 + AI"),
    PHOTO_SCANNER("AI Photo Scanner", "OCR phone extraction & vision", "ML Kit + AI"),
    SPAM_DETECTOR("AI Spam Detector", "Fraud, phishing & scam shield", "Risk Engine"),
    AI_ASSISTANT("AI Assistant", "Hindi & English security copilot", "हिन्दी / EN"),
    BUSINESS_LOOKUP("Business Lookup", "Public entity & helpline verify", "Public Only"),
    SEARCH_HISTORY("Search History", "Encrypted vault & PDF export", "AES-256"),
    SECURITY_CENTER("Security Center", "Permissions, privacy & rate limit", "Zero-Track"),
    SETTINGS("Settings", "Themes, docs & Hardik Kumar v1.0", "Preferences")
}

data class PhoneValidationResult(
    val rawInput: String,
    val isValid: Boolean,
    val isPossible: Boolean,
    val e164Format: String,
    val internationalFormat: String,
    val nationalFormat: String,
    val countryCallingCode: String,
    val regionCode: String,
    val countryName: String,
    val numberType: String,
    val officialHelplineName: String? = null
)

data class NumberLookupReport(
    val validation: PhoneValidationResult,
    val publicCallerOrBusinessName: String,
    val entityCategory: String,
    val spamRiskScore: Int,
    val riskLevel: String,
    val fraudIndicators: List<String>,
    val carrierNetworkNote: String,
    val confidenceLevel: String,
    val sourceAttribution: String,
    val dataFreshness: String,
    val summaryEn: String,
    val summaryHi: String,
    val safetyRecommendations: List<String>,
    val isBusinessMode: Boolean = false,
    val analyzedAtMillis: Long = System.currentTimeMillis()
)

data class PhotoAnalysisReport(
    val ocrRawText: String,
    val extractedPhoneNumbers: List<String>,
    val imageDescription: String,
    val detectedObjects: List<String>,
    val visibleTextSummary: String,
    val publicSourceGuidance: String,
    val privacySafetyNote: String,
    val confidenceLevel: String,
    val sourceAttribution: String,
    val analyzedAtMillis: Long = System.currentTimeMillis()
)

data class SpamAnalysisReport(
    val inputType: String,
    val inputPreview: String,
    val spamRiskScore: Int,
    val riskLevel: String,
    val scamCategory: String,
    val redFlags: List<String>,
    val confidenceLevel: String,
    val sourceAttribution: String,
    val explanationEn: String,
    val explanationHi: String,
    val recommendedActions: List<String>,
    val analyzedAtMillis: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val confidenceTag: String? = null,
    val sourceTag: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class HistoryRecordType(val label: String) {
    NUMBER("Number"),
    BUSINESS("Business"),
    PHOTO("Photo"),
    SPAM("Spam")
}

data class SearchHistoryRecord(
    val id: String,
    val recordType: HistoryRecordType,
    val title: String,
    val subtitle: String,
    val riskLevel: String,
    val riskScore: Int,
    val summaryText: String,
    val sourceAttribution: String,
    val createdAtMillis: Long
)

sealed interface AsyncUiState<out T> {
    data object Idle : AsyncUiState<Nothing>
    data class Loading(val stepMessage: String) : AsyncUiState<Nothing>
    data class Success<T>(val data: T) : AsyncUiState<T>
    data class Error(val message: String, val isRateLimited: Boolean = false) : AsyncUiState<Nothing>
}
