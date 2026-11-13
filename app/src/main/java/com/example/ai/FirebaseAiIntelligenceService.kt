package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.NumberLookupReport
import com.example.model.PhoneValidationResult
import com.example.model.PhotoAnalysisReport
import com.example.model.SpamAnalysisReport
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "FirebaseAiService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val REQUEST_TIMEOUT_MS = 60_000L

class FirebaseAiIntelligenceService {

    private val numberLookupSchema = Schema.obj(
        properties = mapOf(
            "publicCallerOrBusinessName" to Schema.string(
                description = "Verified public business/institution name ONLY if widely known public entity or helpline; otherwise 'Unavailable (Unlisted / Private Number)'"
            ),
            "entityCategory" to Schema.enumeration(
                values = listOf(
                    "Public Business",
                    "Official Helpline",
                    "Telecom / Service",
                    "Unlisted / Private"
                ),
                description = "Public classification category"
            ),
            "spamRiskScore" to Schema.integer(
                description = "Estimated scam/spam risk score from 0 to 100 based on number type, VoIP/toll-free/premium-rate risk, and pattern"
            ),
            "riskLevel" to Schema.enumeration(
                values = listOf("Low", "Medium", "High", "Critical"),
                description = "Risk tier"
            ),
            "fraudIndicators" to Schema.array(
                items = Schema.string(),
                description = "Specific non-fabricated technical or scam risk observations"
            ),
            "carrierNetworkNote" to Schema.string(
                description = "Honest statement explaining ITU format/type and noting that real-time subscriber HLR/MNP carrier lookup requires an authorized paid telecom API"
            ),
            "confidenceLevel" to Schema.enumeration(
                values = listOf("High", "Medium", "Low"),
                description = "Confidence level of this assessment"
            ),
            "summaryEn" to Schema.string(
                description = "Clear, factual English summary without fabricating private personal info"
            ),
            "summaryHi" to Schema.string(
                description = "Clear Hindi (Devanagari) summary of the number verification and safety guidance"
            ),
            "safetyRecommendations" to Schema.array(
                items = Schema.string(),
                description = "Actionable safety tips for the user"
            )
        )
    )

    private val photoAnalysisSchema = Schema.obj(
        properties = mapOf(
            "imageDescription" to Schema.string(
                description = "Objective description of the photo, signage, document, or scene without identifying private individuals"
            ),
            "detectedObjects" to Schema.array(
                items = Schema.string(),
                description = "List of visible objects, storefronts, documents, or logos (no personal face identification)"
            ),
            "visibleTextSummary" to Schema.string(
                description = "Clean summary of readable text visible in the image"
            ),
            "extractedPhoneNumbers" to Schema.array(
                items = Schema.string(),
                description = "Any phone numbers visible in the image"
            ),
            "publicSourceGuidance" to Schema.string(
                description = "Authorized public verification guidance (e.g. official business directory check), stating clearly that private photo retrieval and facial recognition are disabled"
            ),
            "confidenceLevel" to Schema.enumeration(
                values = listOf("High", "Medium", "Low"),
                description = "Confidence level"
            )
        )
    )

    private val spamAnalysisSchema = Schema.obj(
        properties = mapOf(
            "spamRiskScore" to Schema.integer(
                description = "Scam/spam risk score 0 to 100"
            ),
            "riskLevel" to Schema.enumeration(
                values = listOf("Low", "Medium", "High", "Critical"),
                description = "Overall threat level"
            ),
            "scamCategory" to Schema.string(
                description = "Category such as KYC/Bank Phishing, Lottery/Reward Scam, Digital Arrest Scam, Job Scam, Legitimate / Informational, etc."
            ),
            "redFlags" to Schema.array(
                items = Schema.string(),
                description = "Detected suspicious phrases, urgency triggers, fake links, or impersonation cues"
            ),
            "confidenceLevel" to Schema.enumeration(
                values = listOf("High", "Medium", "Low"),
                description = "Analysis confidence"
            ),
            "explanationEn" to Schema.string(
                description = "Detailed English security explanation"
            ),
            "explanationHi" to Schema.string(
                description = "Detailed Hindi (Devanagari) security explanation"
            ),
            "recommendedActions" to Schema.array(
                items = Schema.string(),
                description = "Step-by-step defensive actions including reporting to Chakshu / 1930 if relevant"
            )
        )
    )

    private val privacySystemInstruction = content {
        text(
            """
            You are the AI security & verification engine for Karan Number Finder Pro AI (developed by Hardik Kumar).
            STRICT LEGAL & PRIVACY RULES (Indian DPDP Act 2023, IT Act, Google Play & Apple Policies):
            1. NEVER fabricate or guess personal caller names, home addresses, Aadhaar numbers, PAN, bank details, private photographs, or live GPS locations from a phone number.
            2. If a phone number does not belong to a verified, widely known public business, government institution, or official helpline, you MUST set publicCallerOrBusinessName to "Unavailable (Unlisted / Private Number)" and explain honestly that private subscriber data cannot be accessed.
            3. Never perform facial identification or guess identities of private individuals in photos.
            4. Be factual, transparent about data sources and confidence levels, and helpful in both English and Hindi (Devanagari).
            """.trimIndent()
        )
    }

    private val numberModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = numberLookupSchema
                temperature = 0.2f
            },
            systemInstruction = privacySystemInstruction
        )
    }

    private val photoModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = photoAnalysisSchema
                temperature = 0.2f
            },
            systemInstruction = privacySystemInstruction
        )
    }

    private val spamModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = spamAnalysisSchema
                temperature = 0.2f
            },
            systemInstruction = privacySystemInstruction
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.4f
            },
            systemInstruction = content {
                text(
                    """
                    You are Karan AI Assistant inside Karan Number Finder Pro AI (by Hardik Kumar).
                    Capabilities:
                    - Bilingual fluency in English and Hindi (Devanagari or Hinglish as requested).
                    - Explain phone number validation, E.164 formatting, spam/fraud risk, and suspicious messages (KYC scam, OTP fraud, Digital Arrest scam, fake courier/loan scams).
                    - Generate clear Security Reports with explicit Confidence Level (High/Medium/Low) and Data Source Attribution.
                    - Strict privacy compliance: Never invent personal identities, home addresses, Aadhaar/bank info, or live locations for private phone numbers. Always mention official Indian reporting channels like 1930 (Cyber Crime) and Sanchar Saathi Chakshu when fraud is suspected.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun analyzePhoneNumber(
        validation: PhoneValidationResult,
        businessQueryHint: String,
        isBusinessMode: Boolean
    ): NumberLookupReport {
        val prompt = buildString {
            appendLine("Analyze this phone number verification request:")
            appendLine("- Mode: ${if (isBusinessMode) "Public Business / Entity Lookup" else "Phone Number Risk & Caller Lookup"}")
            appendLine("- Raw Input: ${validation.rawInput}")
            appendLine("- E.164 Format: ${validation.e164Format}")
            appendLine("- International Format: ${validation.internationalFormat}")
            appendLine("- Country: ${validation.countryName} (${validation.regionCode}, ${validation.countryCallingCode})")
            appendLine("- Libphonenumber Valid: ${validation.isValid}, Possible: ${validation.isPossible}")
            appendLine("- ITU Number Type: ${validation.numberType}")
            if (!validation.officialHelplineName.isNullOrBlank()) {
                appendLine("- Local Official Registry Match: ${validation.officialHelplineName}")
            }
            if (businessQueryHint.isNotBlank()) {
                appendLine("- User Business/Context Hint: $businessQueryHint")
            }
            appendLine("Provide an honest, privacy-compliant structured assessment.")
        }

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                numberModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI response received.")
            val json = JSONObject(rawJson)

            val fraudList = mutableListOf<String>()
            json.optJSONArray("fraudIndicators")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { fraudList.add(it) }
                }
            }
            val recList = mutableListOf<String>()
            json.optJSONArray("safetyRecommendations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { recList.add(it) }
                }
            }

            val freshness = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).format(Date())
            NumberLookupReport(
                validation = validation,
                publicCallerOrBusinessName = json.optString(
                    "publicCallerOrBusinessName",
                    validation.officialHelplineName ?: "Unavailable (Unlisted / Private Number)"
                ),
                entityCategory = json.optString("entityCategory", "Unlisted / Private"),
                spamRiskScore = json.optInt("spamRiskScore", if (validation.isValid) 15 else 65).coerceIn(0, 100),
                riskLevel = json.optString("riskLevel", if (validation.isValid) "Low" else "Medium"),
                fraudIndicators = fraudList,
                carrierNetworkNote = json.optString(
                    "carrierNetworkNote",
                    "Format verified via ITU E.164 (${validation.numberType}). Live carrier/MNP subscriber status requires an authorized paid telecom API."
                ),
                confidenceLevel = json.optString("confidenceLevel", "Medium"),
                sourceAttribution = "Google libphonenumber (ITU-T E.164) + Firebase AI Logic (Gemini 3.8 Flash)",
                dataFreshness = "Verified $freshness",
                summaryEn = json.optString("summaryEn", ""),
                summaryHi = json.optString("summaryHi", ""),
                safetyRecommendations = recList,
                isBusinessMode = isBusinessMode
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Number analysis timed out", e)
            throw IllegalStateException("AI verification timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Number analysis failed", e)
            throw e
        }
    }

    suspend fun analyzePhoto(
        bitmap: Bitmap,
        ocrText: String,
        ocrPhones: List<String>
    ): PhotoAnalysisReport {
        val promptText = buildString {
            appendLine("Analyze this user-selected image for Karan Number Finder Pro AI.")
            appendLine("Tasks:")
            appendLine("1. Describe the image objectively (signboard, business card, receipt, screenshot, flyer, etc.).")
            appendLine("2. List visible objects, logos, and text elements.")
            appendLine("3. Extract any visible phone numbers.")
            appendLine("4. Provide authorized public verification guidance only. NEVER identify private individuals or retrieve private photos.")
            if (ocrText.isNotBlank()) {
                appendLine("On-device ML Kit OCR text already detected:\n$ocrText")
            }
            if (ocrPhones.isNotEmpty()) {
                appendLine("On-device OCR numbers detected: ${ocrPhones.joinToString(", ")}")
            }
        }

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                photoModel.generateContent(
                    content {
                        image(bitmap)
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI vision response.")
            val json = JSONObject(rawJson)

            val objects = mutableListOf<String>()
            json.optJSONArray("detectedObjects")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { objects.add(it) }
                }
            }
            val aiPhones = linkedSetOf<String>()
            aiPhones.addAll(ocrPhones)
            json.optJSONArray("extractedPhoneNumbers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { aiPhones.add(it) }
                }
            }

            PhotoAnalysisReport(
                ocrRawText = ocrText,
                extractedPhoneNumbers = aiPhones.toList(),
                imageDescription = json.optString("imageDescription", "No description generated."),
                detectedObjects = objects,
                visibleTextSummary = json.optString("visibleTextSummary", ocrText),
                publicSourceGuidance = json.optString(
                    "publicSourceGuidance",
                    "Verify extracted business details against official websites or public directories. Facial identification is disabled."
                ),
                privacySafetyNote = "On-device ML Kit OCR + user-initiated HTTPS Gemini Vision. No automatic photo upload; no facial recognition.",
                confidenceLevel = json.optString("confidenceLevel", "Medium"),
                sourceAttribution = "ML Kit On-Device OCR + Firebase AI Logic Multimodal Vision"
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Photo analysis timed out", e)
            throw IllegalStateException("AI photo analysis timed out after 60s. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Photo analysis failed", e)
            throw e
        }
    }

    suspend fun analyzeSpamOrMessage(
        inputType: String,
        contentToAnalyze: String
    ): SpamAnalysisReport {
        val prompt = """
            Analyze the following $inputType for spam, phishing, social engineering, or financial fraud risk (including Indian scams such as fake KYC, UPI collect scams, Digital Arrest impersonation, courier customs, fake job/loan offers):
            ---
            $contentToAnalyze
            ---
            Return a factual, privacy-safe structured risk report with both English and Hindi explanations.
        """.trimIndent()

        return try {
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                spamModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty AI spam analysis response.")
            val json = JSONObject(rawJson)

            val flags = mutableListOf<String>()
            json.optJSONArray("redFlags")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { flags.add(it) }
                }
            }
            val actions = mutableListOf<String>()
            json.optJSONArray("recommendedActions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let { actions.add(it) }
                }
            }

            SpamAnalysisReport(
                inputType = inputType,
                inputPreview = contentToAnalyze.take(160),
                spamRiskScore = json.optInt("spamRiskScore", 50).coerceIn(0, 100),
                riskLevel = json.optString("riskLevel", "Medium"),
                scamCategory = json.optString("scamCategory", "General Suspicious Pattern"),
                redFlags = flags,
                confidenceLevel = json.optString("confidenceLevel", "Medium"),
                sourceAttribution = "Firebase AI Logic (Gemini 3.8 Flash) Heuristic & NLP Security Engine",
                explanationEn = json.optString("explanationEn", ""),
                explanationHi = json.optString("explanationHi", ""),
                recommendedActions = actions
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Spam analysis timed out", e)
            throw IllegalStateException("Spam detection timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Spam analysis failed", e)
            throw e
        }
    }

    fun streamAssistantReply(
        userMessage: String,
        historyContent: List<Content>,
        preferredLanguage: AppLanguage,
        attachedContext: String?
    ): Flow<String> = flow {
        val languageDirective = when (preferredLanguage) {
            AppLanguage.ENGLISH -> "Respond clearly in English. Include Confidence Level and Source Attribution at the end."
            AppLanguage.HINDI -> "Respond clearly in Hindi (Devanagari script). Include Confidence Level (विश्वसनीयता स्तर) and Source Attribution (स्रोत) at the end."
            AppLanguage.BILINGUAL -> "Respond clearly in the user's language (English or Hindi), and provide a brief summary in the other language if helpful for security clarity. Include Confidence Level and Source Attribution."
        }
        val composedMessage = buildString {
            appendLine("[Language Instruction: $languageDirective]")
            if (!attachedContext.isNullOrBlank()) {
                appendLine("[Attached App Context:\n$attachedContext\n]")
            }
            append(userMessage)
        }

        val chat = chatModel.startChat(history = historyContent)
        val accumulated = StringBuilder()
        withTimeout(REQUEST_TIMEOUT_MS) {
            chat.sendMessageStream(composedMessage).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Assistant returned an empty response.")
        }
    }
}
