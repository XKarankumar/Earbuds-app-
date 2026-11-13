package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.export.ReportExporter
import com.example.model.SmsDispatchMethod
import com.example.model.TransactionType
import com.example.security.CryptoVault
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EncryptedLocalLedgerTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.createInMemory(context)
        repository = LedgerRepository(db.ledgerDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun encryptedRoomDatabase_hidesPlaintextAndUpdatesBalanceAtomically() = runBlocking {
        repository.ensureOwnerProfile()
        val custId = repository.createCustomer(
            name = "Ramesh Traders",
            phone = "9876543210",
            notes = "Wholesale account"
        ).getOrThrow()

        // Raw database entity must NOT contain plaintext customer name or phone
        val rawEntity = db.ledgerDao().getCustomerById(custId)
        assertNotNull(rawEntity)
        assertFalse(rawEntity!!.encryptedPayload.contains("Ramesh Traders"))
        assertFalse(rawEntity.encryptedPayload.contains("9876543210"))

        val customer = repository.observeCustomers().first().first { it.id == custId }
        repository.recordTransaction(
            customer = customer,
            type = TransactionType.UDHAAR,
            amountPaise = 250_000L, // ₹2,500.00
            note = "Cement bags",
            billReference = "INV-101",
            smsReceiptSent = true,
            smsMethod = SmsDispatchMethod.COMPOSER
        ).getOrThrow()

        val afterUdhaar = repository.observeCustomers().first().first { it.id == custId }
        assertEquals(250_000L, afterUdhaar.totalUdhaarPaise)
        assertEquals(250_000L, afterUdhaar.balancePaise)

        repository.recordTransaction(
            customer = afterUdhaar,
            type = TransactionType.JAMA,
            amountPaise = 100_000L, // ₹1,000.00
            note = "Cash payment",
            billReference = "RCPT-01",
            smsReceiptSent = true,
            smsMethod = SmsDispatchMethod.DIRECT_SMS
        ).getOrThrow()

        val afterJama = repository.observeCustomers().first().first { it.id == custId }
        assertEquals(250_000L, afterJama.totalUdhaarPaise)
        assertEquals(100_000L, afterJama.totalJamaPaise)
        assertEquals(150_000L, afterJama.balancePaise)

        val logs = repository.observeAuditLogs().first()
        assertEquals(3, logs.size) // 1 customer created + 2 transactions
    }

    @Test
    fun pinHashingAndPortableBackupEncryption_roundTripSuccessfully() = runBlocking {
        val (hash, salt) = CryptoVault.hashPinWithSalt("4829")
        assertTrue(CryptoVault.verifyPin("4829", hash, salt))
        assertFalse(CryptoVault.verifyPin("1234", hash, salt))

        val custId = repository.createCustomer("Suresh Kirana", "9988776655", "").getOrThrow()
        val customer = repository.observeCustomers().first().first { it.id == custId }
        repository.recordTransaction(
            customer = customer,
            type = TransactionType.UDHAAR,
            amountPaise = 50_000L,
            note = "Sugar sack",
            billReference = "B-22",
            smsReceiptSent = false,
            smsMethod = SmsDispatchMethod.NONE
        ).getOrThrow()

        val customers = repository.observeCustomers().first()
        val transactions = repository.observeTransactions().first()

        val baos = ByteArrayOutputStream()
        ReportExporter.exportEncryptedBackup(baos, "hardik@2026", customers, transactions)
        val encryptedStr = baos.toString(Charsets.UTF_8.name())
        assertTrue(encryptedStr.startsWith("SSCBAK1|"))
        assertFalse(encryptedStr.contains("Suresh Kirana"))

        val parsed = ReportExporter.parseEncryptedBackup(encryptedStr, "hardik@2026").getOrThrow()
        assertEquals(1, parsed.customers.size)
        assertEquals("Suresh Kirana", parsed.customers.first().name)
        assertEquals(1, parsed.transactions.size)
        assertEquals(50_000L, parsed.transactions.first().amountPaise)
    }
}
