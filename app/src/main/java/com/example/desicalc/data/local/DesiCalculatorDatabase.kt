package com.example.desicalc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CustomerEntity::class,
        TransactionEntity::class,
        CalculationHistoryEntity::class,
        SmsLogEntity::class,
        AiChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DesiCalculatorDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun calculationHistoryDao(): CalculationHistoryDao
    abstract fun smsLogDao(): SmsLogDao
    abstract fun aiChatDao(): AiChatDao

    companion object {
        private const val DB_NAME = "desi_calculator_ai_pro.db"

        /**
         * Non-destructive v1 -> v2 schema migration:
         * Preserves all existing customers, Udhar/Jama transactions, and history while
         * adding customer SMS consent & scheduled reminder columns.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE customers ADD COLUMN smsConsentGranted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE customers ADD COLUMN reminderIntervalDays INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE customers ADD COLUMN lastReminderSentAt INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN isPartialPayment INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile
        private var INSTANCE: DesiCalculatorDatabase? = null

        fun getInstance(context: Context): DesiCalculatorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DesiCalculatorDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
