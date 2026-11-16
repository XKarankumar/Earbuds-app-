package com.example.desicalc

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.desicalc.data.backup.BackupAndMigrationManager
import com.example.desicalc.data.local.DesiCalculatorDatabase
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.engine.OfflineSmartAssistant
import com.example.desicalc.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DesiLedgerRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: DesiCalculatorDatabase
    private lateinit var repository: DesiLedgerRepository
    private lateinit var backupManager: BackupAndMigrationManager
    private lateinit var offlineAssistant: OfflineSmartAssistant

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DesiCalculatorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DesiLedgerRepository(db)
        backupManager = BackupAndMigrationManager(db)
        offlineAssistant = OfflineSmartAssistant(repository)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app metadata matches Desi Calculator`() {
        assertEquals("Desi Calculator", context.getString(R.string.app_name))
        assertEquals("Desi Calculator AI PRO V2.0", context.getString(R.string.app_full_title))
    }

    @Test
    fun `udhar and jama never mix receivable and payable balances`() = runBlocking {
        val cId = repository.addCustomer(name = "Rahul Sharma", mobile = "9876543210")
        repository.addTransaction(cId, TransactionType.UDHAR_DIYA, 1000.0, "Samaan")
        repository.addTransaction(cId, TransactionType.JAMA_RECEIVED, 400.0, "Partial cash", isPartialPayment = true)
        repository.addTransaction(cId, TransactionType.UDHAR_LIYA, 200.0, "Advance")
        repository.addTransaction(cId, TransactionType.PAYMENT_MADE, 50.0, "Paid back")

        val summary = repository.getAllSummariesOnce().first { it.id == cId }
        assertEquals(600.0, summary.receivableBalance, 0.01)
        assertEquals(150.0, summary.payableBalance, 0.01)
        assertEquals(450.0, summary.netBalance, 0.01)
    }

    @Test
    fun `duplicate transaction detection flags recent identical entry`() = runBlocking {
        val cId = repository.addCustomer(name = "Amit Verma", mobile = "9999988888")
        repository.addTransaction(cId, TransactionType.UDHAR_DIYA, 500.0, "Sugar")

        val dup = repository.checkPossibleDuplicate(cId, TransactionType.UDHAR_DIYA, 500.0)
        assertNotNull(dup)

        val nonDup = repository.checkPossibleDuplicate(cId, TransactionType.JAMA_RECEIVED, 500.0)
        assertNull(nonDup)
    }

    @Test
    fun `legacy html localStorage migration preserves existing records without overwriting`() = runBlocking {
        val existingId = repository.addCustomer(name = "Suresh Kirana", mobile = "9811112222")
        repository.addTransaction(existingId, TransactionType.UDHAR_DIYA, 300.0, "Existing entry")

        val legacyHtmlJson = """
            {
              "customers": [
                {"id": "c_1", "name": "Suresh Kirana", "mobile": "9811112222"},
                {"id": "c_2", "name": "Neha Traders", "mobile": "9822223333"}
              ],
              "transactions": [
                {"id": "t_1", "customerId": "c_2", "type": "udhar", "amount": 1500, "note": "Old balance"},
                {"id": "t_2", "customerId": "c_2", "type": "jama", "amount": 500, "note": "Part payment"}
              ]
            }
        """.trimIndent()

        val result = backupManager.importJsonNonDestructive(legacyHtmlJson)
        assertEquals(1, result.customersImported)
        assertEquals(1, result.customersSkippedExisting)
        assertEquals(2, result.transactionsImported)

        val summaries = repository.getAllSummariesOnce()
        assertEquals(2, summaries.size)
        val suresh = summaries.first { it.name == "Suresh Kirana" }
        val neha = summaries.first { it.name == "Neha Traders" }
        assertEquals(300.0, suresh.netBalance, 0.01)
        assertEquals(1000.0, neha.netBalance, 0.01)
    }

    @Test
    fun `offline smart assistant understands Hindi and English commands without saving silently`() = runBlocking {
        val rahulId = repository.addCustomer(name = "Rahul", mobile = "9876500000")
        repository.addTransaction(rahulId, TransactionType.UDHAR_DIYA, 750.0, "Initial")

        val txProposal = offlineAssistant.processQuery("Rahul ko 500 rupaye udhar diye")
        assertNotNull(txProposal.pendingAction)
        assertEquals(500.0, txProposal.pendingAction!!.amount, 0.01)
        assertEquals("UDHAR_DIYA", txProposal.pendingAction!!.transactionType)
        assertEquals(rahulId, txProposal.pendingAction!!.matchedCustomerId)
        // Confirm database was NOT silently mutated yet!
        assertEquals(750.0, repository.getAllSummariesOnce().first { it.id == rahulId }.netBalance, 0.01)

        val highestDue = offlineAssistant.processQuery("Kis customer ka sabse zyada baki hai?")
        assertTrue(highestDue.replyText.contains("Rahul"))
        assertTrue(highestDue.replyText.contains("750"))

        val todaySummary = offlineAssistant.processQuery("Aaj ka total hisab batao")
        assertTrue(todaySummary.replyText.contains("750"))
    }
}
