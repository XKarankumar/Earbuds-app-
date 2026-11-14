package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.AttendanceDao
import com.example.data.local.MemberDao
import com.example.data.local.MemberEntity
import com.example.data.local.SmsLogDao
import com.example.data.local.SmsLogEntity
import com.example.model.SmsDeliveryStatus
import com.example.model.SmsEventType
import com.example.security.AppPreferencesRepository
import com.example.security.KeystoreCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class MissedGymScanReport(
    val evaluatedActiveMembers: Int,
    val scheduledTodayCount: Int,
    val checkedInCount: Int,
    val missingCount: Int,
    val sentCount: Int,
    val skippedDuplicateCount: Int,
    val skippedOptOutCount: Int,
    val deadlineReached: Boolean,
    val deadlineLabel: String
)

/**
 * Authorized SMS Gateway & Missed Gym Detection Engine.
 * - Supports HTTPS REST SMS Gateway (Bearer/API Key stored encrypted in Keystore)
 * - Falls back to zero-permission Android SMS Intent handoff (ACTION_SENDTO smsto:)
 * - Prevents duplicate SMS notifications per member, event type, and date via dedupeKey
 */
class SmsGatewayService(
    private val context: Context,
    private val crypto: KeystoreCryptoManager,
    private val prefsRepo: AppPreferencesRepository,
    private val memberDao: MemberDao,
    private val attendanceDao: AttendanceDao,
    private val smsLogDao: SmsLogDao
) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun dispatchMemberSms(
        member: MemberEntity,
        eventType: SmsEventType,
        messageBody: String,
        dateKey: String = FitnessCalculators.todayDateKey(),
        allowIntentLaunch: Boolean = false
    ): SmsDeliveryStatus {
        if (!isMemberOptedIn(member, eventType)) {
            smsLogDao.insert(
                SmsLogEntity(
                    memberId = member.id,
                    memberName = member.fullName,
                    maskedPhone = "${member.countryCode} ••••${member.phoneLast4}",
                    eventType = eventType.name,
                    messageBody = messageBody,
                    deliveryStatus = SmsDeliveryStatus.SKIPPED_OPT_OUT.name,
                    gatewayMode = "OPT_OUT",
                    dedupeKey = buildDedupeKey(eventType, member.id, dateKey)
                )
            )
            return SmsDeliveryStatus.SKIPPED_OPT_OUT
        }

        val dedupeKey = buildDedupeKey(eventType, member.id, dateKey)
        if (smsLogDao.existsByDedupeKey(dedupeKey)) {
            return SmsDeliveryStatus.SKIPPED_DUPLICATE
        }

        val prefs = prefsRepo.current()
        val fullPhone = "${member.countryCode}${crypto.decrypt(member.encryptedPhone)}"
        val masked = "${member.countryCode} ••••${member.phoneLast4}"

        val status: SmsDeliveryStatus
        val mode: String

        if (prefs.smsGatewayEnabled && prefs.smsGatewayUrl.startsWith("https://", ignoreCase = true)) {
            val apiKey = crypto.decrypt(prefs.encryptedSmsApiKey)
            val sent = sendViaAuthorizedHttpsGateway(
                url = prefs.smsGatewayUrl,
                apiKey = apiKey,
                senderId = prefs.smsSenderId,
                recipient = fullPhone,
                message = messageBody
            )
            status = if (sent) SmsDeliveryStatus.SENT_GATEWAY else SmsDeliveryStatus.FAILED
            mode = "HTTPS_GATEWAY"
        } else if (allowIntentLaunch) {
            val launched = launchSmsComposer(fullPhone, messageBody)
            status = if (launched) SmsDeliveryStatus.HANDOFF_OPENED else SmsDeliveryStatus.QUEUED
            mode = "SMS_INTENT_HANDOFF"
        } else {
            status = SmsDeliveryStatus.QUEUED
            mode = "QUEUED_FOR_DISPATCH"
        }

        smsLogDao.insert(
            SmsLogEntity(
                memberId = member.id,
                memberName = member.fullName,
                maskedPhone = masked,
                eventType = eventType.name,
                messageBody = messageBody,
                deliveryStatus = status.name,
                gatewayMode = mode,
                dedupeKey = dedupeKey
            )
        )
        return status
    }

    suspend fun runMissedGymDetection(forceIgnoreDeadline: Boolean = false): MissedGymScanReport {
        val prefs = prefsRepo.current()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val deadlineMinutes = prefs.attendanceDeadlineHour * 60 + prefs.attendanceDeadlineMinute
        val deadlineReached = currentMinutes >= deadlineMinutes
        val deadlineLabel = "%02d:%02d".format(prefs.attendanceDeadlineHour, prefs.attendanceDeadlineMinute)

        val todayKey = FitnessCalculators.todayDateKey(now)
        val dayCode = FitnessCalculators.currentDayCode(now)
        val members = memberDao.getAllOnce().filter { it.isActive }
        val todayRecords = attendanceDao.getByDateOnce(todayKey)
        val presentMemberIds = todayRecords.filter { it.status != "ABSENT" }.map { it.memberId }.toSet()

        val scheduledToday = members.filter { m ->
            m.scheduledDaysCsv.split(",").map { it.trim().uppercase() }.contains(dayCode)
        }
        val missingMembers = scheduledToday.filter { it.id !in presentMemberIds }

        var sentCount = 0
        var dupCount = 0
        var optOutCount = 0

        if (deadlineReached || forceIgnoreDeadline) {
            for (member in missingMembers) {
                val msg = "${prefs.gymName}: Hi ${member.fullName}, we noticed you missed today's scheduled gym session ($dayCode). Stay consistent — see you tomorrow!"
                when (dispatchMemberSms(member, SmsEventType.ATTENDANCE_MISSING, msg, todayKey, allowIntentLaunch = false)) {
                    SmsDeliveryStatus.SENT_GATEWAY,
                    SmsDeliveryStatus.DELIVERED,
                    SmsDeliveryStatus.QUEUED,
                    SmsDeliveryStatus.HANDOFF_OPENED -> sentCount++
                    SmsDeliveryStatus.SKIPPED_DUPLICATE -> dupCount++
                    SmsDeliveryStatus.SKIPPED_OPT_OUT -> optOutCount++
                    SmsDeliveryStatus.FAILED -> Unit
                }
            }
        }

        return MissedGymScanReport(
            evaluatedActiveMembers = members.size,
            scheduledTodayCount = scheduledToday.size,
            checkedInCount = scheduledToday.size - missingMembers.size,
            missingCount = missingMembers.size,
            sentCount = sentCount,
            skippedDuplicateCount = dupCount,
            skippedOptOutCount = optOutCount,
            deadlineReached = deadlineReached,
            deadlineLabel = deadlineLabel
        )
    }

    fun launchSmsComposer(fullPhone: String, messageBody: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(fullPhone)}")
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun sendViaAuthorizedHttpsGateway(
        url: String,
        apiKey: String,
        senderId: String,
        recipient: String,
        message: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("to", recipient)
                put("senderId", senderId)
                put("message", message)
            }.toString()
            val reqBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun isMemberOptedIn(member: MemberEntity, eventType: SmsEventType): Boolean = when (eventType) {
        SmsEventType.ATTENDANCE_MISSING -> member.smsAttendanceMissing
        SmsEventType.WORKOUT_MISSED -> member.smsWorkoutMissed
        SmsEventType.MEMBERSHIP_EXPIRY -> member.smsMembershipExpiry
        SmsEventType.FEE_PENDING -> member.smsFeePending
        SmsEventType.PAYMENT_CONFIRMATION -> member.smsPaymentConfirm
        SmsEventType.NEW_MEMBER_REGISTRATION -> member.smsWelcome
        SmsEventType.TOKEN_EXPIRY -> member.smsTokenExpiry
    }

    private fun buildDedupeKey(eventType: SmsEventType, memberId: Long, dateKey: String): String =
        "${eventType.name}_${memberId}_$dateKey"
}
