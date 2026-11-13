package com.example.sms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.example.data.LedgerRepository
import com.example.model.Customer
import com.example.model.SmsDispatchMethod
import com.example.model.TransactionType
import com.example.permissions.PermissionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsDispatchResult(
    val dispatched: Boolean,
    val method: SmsDispatchMethod,
    val message: String
)

object SmsReceiptHelper {

    fun formatReceiptBody(
        businessName: String,
        customerName: String,
        type: TransactionType,
        amountPaise: Long,
        newBalancePaise: Long,
        note: String,
        billRef: String
    ): String {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val amtStr = LedgerRepository.formatCurrencyPaise(amountPaise)
        val balStr = LedgerRepository.formatCurrencyPaise(kotlin.math.abs(newBalancePaise))
        val balLabel = when {
            newBalancePaise > 0L -> "Net Due (Udhaar): $balStr"
            newBalancePaise < 0L -> "Advance (Jama): $balStr"
            else -> "Account Settled (₹0.00)"
        }
        val txLabel = if (type == TransactionType.UDHAAR) "Credit / Udhaar Added" else "Payment / Jama Received"

        return buildString {
            appendLine("$businessName — Payment Receipt")
            appendLine("Customer: $customerName")
            appendLine("$txLabel: $amtStr")
            if (billRef.isNotBlank()) appendLine("Ref: $billRef")
            if (note.isNotBlank()) appendLine("Note: $note")
            appendLine(balLabel)
            append("Date: $dateStr | Simple SMS Calculator")
        }
    }

    fun formatCustomerReminderBody(
        businessName: String,
        customer: Customer
    ): String {
        val balStr = LedgerRepository.formatCurrencyPaise(kotlin.math.abs(customer.balancePaise))
        val status = if (customer.balancePaise >= 0) "pending Udhaar balance of $balStr" else "advance Jama of $balStr"
        return "Hello ${customer.name}, friendly ledger reminder from $businessName: your current $status. Thank you!"
    }

    fun sendSms(
        context: Context,
        phone: String,
        body: String,
        preferDirectSms: Boolean
    ): SmsDispatchResult {
        val cleanPhone = phone.trim()
        if (cleanPhone.isEmpty()) {
            return SmsDispatchResult(
                dispatched = false,
                method = SmsDispatchMethod.NONE,
                message = "Customer phone number is empty"
            )
        }

        val hasTelephony = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        val hasSmsPermission = PermissionManager.isGranted(context, Manifest.permission.SEND_SMS)

        if (preferDirectSms && hasTelephony && hasSmsPermission) {
            return try {
                val smsManager = resolveDefaultSimSmsManager(context)
                if (smsManager != null) {
                    val parts = smsManager.divideMessage(body)
                    if (parts.size > 1) {
                        smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(cleanPhone, null, body, null, null)
                    }
                    SmsDispatchResult(
                        dispatched = true,
                        method = SmsDispatchMethod.DIRECT_SMS,
                        message = "SMS receipt sent via phone SIM to $cleanPhone"
                    )
                } else {
                    launchComposer(context, cleanPhone, body)
                }
            } catch (_: Exception) {
                launchComposer(context, cleanPhone, body)
            }
        }

        return launchComposer(context, cleanPhone, body)
    }

    private fun resolveDefaultSimSmsManager(context: Context): SmsManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val subId = SmsManager.getDefaultSmsSubscriptionId()
            val mgr = context.getSystemService(SmsManager::class.java)
            if (mgr != null && subId >= 0) {
                mgr.createForSubscriptionId(subId)
            } else {
                mgr
            }
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    fun launchComposer(context: Context, phone: String, body: String): SmsDispatchResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(phone)}")
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                SmsDispatchResult(
                    dispatched = true,
                    method = SmsDispatchMethod.COMPOSER,
                    message = "Opened native SMS composer for $phone"
                )
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Payment Receipt").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                SmsDispatchResult(
                    dispatched = true,
                    method = SmsDispatchMethod.COMPOSER,
                    message = "No SMS app found; opened share sheet for receipt"
                )
            }
        } catch (e: Exception) {
            SmsDispatchResult(
                dispatched = false,
                method = SmsDispatchMethod.NONE,
                message = "Unable to open SMS app: ${e.localizedMessage ?: "error"}"
            )
        }
    }
}
