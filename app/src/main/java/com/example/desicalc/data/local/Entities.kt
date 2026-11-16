package com.example.desicalc.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerCode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["mobile"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerCode: String,
    val name: String,
    val mobile: String = "",
    val address: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val autoSmsEnabled: Boolean = false,
    val smsConsentGranted: Boolean = false,
    val reminderIntervalDays: Int = 0,
    val lastReminderSentAt: Long? = null,
    val legacyExternalId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: String,
    val amount: Double,
    val description: String = "",
    val isPartialPayment: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val legacyExternalId: String? = null
)

@Entity(
    tableName = "calculation_history",
    indices = [Index(value = ["timestamp"])]
)
data class CalculationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val expression: String,
    val result: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sms_logs",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"])
    ]
)
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long?,
    val customerName: String,
    val mobile: String,
    val templateType: String,
    val messageBody: String,
    val status: String,
    val errorDetail: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_chat_messages",
    indices = [Index(value = ["timestamp"])]
)
data class AiChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val engineUsed: String,
    val pendingActionJson: String? = null,
    val actionConfirmed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
