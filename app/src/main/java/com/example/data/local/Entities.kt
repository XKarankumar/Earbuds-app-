package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ledger_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class LedgerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: String, // JAMA or UDHAAR
    val amount: Double,
    val note: String = "",
    val expression: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "calc_history")
data class CalcHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val expression: String,
    val result: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientName: String,
    val recipientPhone: String,
    val message: String,
    val templateName: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
