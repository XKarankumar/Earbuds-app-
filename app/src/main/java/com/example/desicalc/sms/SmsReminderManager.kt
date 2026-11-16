package com.example.desicalc.sms

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.desicalc.data.local.SmsLogEntity
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.model.SmsTemplateType
import com.example.desicalc.model.formatInr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class SmsReminderManager(
    private val context: Context,
    private val repository: DesiLedgerRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val ACTION_SMS_SENT = "com.example.desicalc.SMS_SENT"
        private const val ACTION_SMS_DELIVERED = "com.example.desicalc.SMS_DELIVERED"
        private const val EXTRA_LOG_ID = "extra_sms_log_id"
    }

    fun hasSendSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasTelephonyHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    fun renderTemplate(
        template: String,
        customerName: String,
        businessName: String,
        amount: Double = 0.0,
        netBalance: Double = 0.0
    ): String {
        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        return template
            .replace("{name}", customerName)
            .replace("{business}", businessName)
            .replace("{amount}", formatInr(amount, includeSymbol = false))
            .replace("{balance}", formatInr(abs(netBalance), includeSymbol = false))
            .replace("{date}", dateStr)
    }

    suspend fun openManualSmsComposer(
        customerId: Long?,
        customerName: String,
        mobile: String,
        templateType: SmsTemplateType,
        messageBody: String
    ): Result<Unit> {
        val cleanedPhone = mobile.trim()
        if (cleanedPhone.isEmpty()) {
            return Result.failure(IllegalArgumentException("Customer mobile number is missing."))
        }

        val logId = repository.insertSmsLog(
            SmsLogEntity(
                customerId = customerId,
                customerName = customerName,
                mobile = cleanedPhone,
                templateType = templateType.code,
                messageBody = messageBody,
                status = "COMPOSER_OPENED"
            )
        )

        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(cleanedPhone)}")
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                if (customerId != null) {
                    repository.markCustomerReminderSent(customerId)
                }
                Result.success(Unit)
            } else {
                repository.updateSmsLogStatus(logId, "FAILED", "No SMS app available on this device")
                Result.failure(IllegalStateException("No SMS composer app found on device."))
            }
        } catch (e: Exception) {
            repository.updateSmsLogStatus(logId, "FAILED", e.localizedMessage)
            Result.failure(e)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    suspend fun sendDirectSmsWithConsent(
        customerId: Long?,
        customerName: String,
        mobile: String,
        templateType: SmsTemplateType,
        messageBody: String,
        userConsented: Boolean
    ): Result<Long> {
        val cleanedPhone = mobile.trim()
        if (cleanedPhone.isEmpty()) {
            return Result.failure(IllegalArgumentException("Customer mobile number is required."))
        }
        if (!userConsented) {
            return Result.failure(IllegalStateException("User consent is required before sending direct SMS."))
        }
        if (!hasSendSmsPermission()) {
            return Result.failure(SecurityException("SEND_SMS permission is not granted."))
        }

        val logId = repository.insertSmsLog(
            SmsLogEntity(
                customerId = customerId,
                customerName = customerName,
                mobile = cleanedPhone,
                templateType = templateType.code,
                messageBody = messageBody,
                status = "PENDING"
            )
        )

        return try {
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (smsManager == null) {
                repository.updateSmsLogStatus(logId, "FAILED", "SmsManager unavailable on this device")
                return Result.failure(IllegalStateException("SMS hardware service unavailable on this device."))
            }

            val sentAction = "${ACTION_SMS_SENT}_$logId"
            val deliveredAction = "${ACTION_SMS_DELIVERED}_$logId"

            registerOneShotStatusReceiver(sentAction, logId, isDelivery = false)
            registerOneShotStatusReceiver(deliveredAction, logId, isDelivery = true)

            val sentPi = PendingIntent.getBroadcast(
                context,
                logId.toInt(),
                Intent(sentAction).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_LOG_ID, logId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val deliveredPi = PendingIntent.getBroadcast(
                context,
                (logId + 50_000L).toInt(),
                Intent(deliveredAction).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_LOG_ID, logId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val parts = smsManager.divideMessage(messageBody)
            if (parts.size > 1) {
                val sentList = ArrayList<PendingIntent>(parts.size).apply { repeat(parts.size) { add(sentPi) } }
                val delivList = ArrayList<PendingIntent>(parts.size).apply { repeat(parts.size) { add(deliveredPi) } }
                smsManager.sendMultipartTextMessage(cleanedPhone, null, parts, sentList, delivList)
            } else {
                smsManager.sendTextMessage(cleanedPhone, null, messageBody, sentPi, deliveredPi)
            }

            if (customerId != null) {
                repository.markCustomerReminderSent(customerId)
            }
            Result.success(logId)
        } catch (e: Exception) {
            repository.updateSmsLogStatus(logId, "FAILED", e.localizedMessage ?: "SMS dispatch failed")
            Result.failure(e)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerOneShotStatusReceiver(
        action: String,
        logId: Long,
        isDelivery: Boolean
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val code = resultCode
                scope.launch {
                    if (isDelivery) {
                        if (code == Activity.RESULT_OK) {
                            repository.updateSmsLogStatus(logId, "DELIVERED", null)
                        }
                    } else {
                        if (code == Activity.RESULT_OK) {
                            repository.updateSmsLogStatus(logId, "SENT", null)
                        } else {
                            val reason = when (code) {
                                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Carrier/radio generic failure"
                                SmsManager.RESULT_ERROR_NO_SERVICE -> "No cellular service"
                                SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU error"
                                SmsManager.RESULT_ERROR_RADIO_OFF -> "Cellular radio is off"
                                else -> "SMS send error code: $code"
                            }
                            repository.updateSmsLogStatus(logId, "FAILED", reason)
                        }
                    }
                }
                runCatching { context.applicationContext.unregisterReceiver(this) }
            }
        }

        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.applicationContext.registerReceiver(receiver, filter)
        }
    }
}
