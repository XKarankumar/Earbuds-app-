package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppRepository
import com.example.data.local.AppDatabase
import com.example.domain.CalculatorEngine
import com.example.domain.ConverterAndFinanceEngine
import com.example.domain.UnitCategory
import com.example.model.CalcRecordCategory
import com.example.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SimpleSmsCalculatorTests {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `calculator engine evaluates arithmetic, parentheses, percentages and zero division`() {
        val addMul = CalculatorEngine.evaluate("12+8×5")
        assertTrue(addMul is CalculatorEngine.EvalResult.Success)
        assertEquals("52", (addMul as CalculatorEngine.EvalResult.Success).formatted)

        val parens = CalculatorEngine.evaluate("(100+50)÷3")
        assertTrue(parens is CalculatorEngine.EvalResult.Success)
        assertEquals("50", (parens as CalculatorEngine.EvalResult.Success).formatted)

        val pct = CalculatorEngine.evaluate("200×15%")
        assertTrue(pct is CalculatorEngine.EvalResult.Success)
        assertEquals("30", (pct as CalculatorEngine.EvalResult.Success).formatted)

        val divZero = CalculatorEngine.evaluate("45÷0")
        assertTrue(divZero is CalculatorEngine.EvalResult.Error)
    }

    @Test
    fun `unit converter and finance engines compute accurate values`() {
        val lengthUnits = ConverterAndFinanceEngine.unitCatalog[UnitCategory.LENGTH]!!
        val meter = lengthUnits.first { it.id == "m" }
        val km = lengthUnits.first { it.id == "km" }
        assertEquals(2.5, ConverterAndFinanceEngine.convert(UnitCategory.LENGTH, 2500.0, meter, km), 0.0001)

        val tempUnits = ConverterAndFinanceEngine.unitCatalog[UnitCategory.TEMPERATURE]!!
        val c = tempUnits.first { it.id == "c" }
        val f = tempUnits.first { it.id == "f" }
        assertEquals(212.0, ConverterAndFinanceEngine.convert(UnitCategory.TEMPERATURE, 100.0, c, f), 0.0001)

        val gstExcl = ConverterAndFinanceEngine.calculateGst(1000.0, 18.0, isInclusive = false)
        assertEquals(180.0, gstExcl.totalGst, 0.01)
        assertEquals(1180.0, gstExcl.totalAmount, 0.01)

        val gstIncl = ConverterAndFinanceEngine.calculateGst(1180.0, 18.0, isInclusive = true)
        assertEquals(1000.0, gstIncl.baseAmount, 0.01)
        assertEquals(180.0, gstIncl.totalGst, 0.01)

        val emi = ConverterAndFinanceEngine.calculateEmi(120000.0, 12.0, 12)
        assertEquals(10661.85, emi.monthlyEmi, 0.5)
    }

    @Test
    fun `room repository tracks customers, jama udhaar balances, calc history, and sms logs`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = AppRepository(
            customerDao = db.customerDao(),
            ledgerDao = db.ledgerDao(),
            calcHistoryDao = db.calcHistoryDao(),
            smsLogDao = db.smsLogDao()
        )

        val cid = repo.addCustomer("Rohan Sharma", "9811012345", "Regular buyer", 500.0, TransactionType.UDHAAR)
        repo.addTransaction(cid, TransactionType.JAMA, 200.0, "Part payment")

        val summaries = repo.customerSummariesFlow.first()
        assertEquals(1, summaries.size)
        assertEquals(300.0, summaries.first().netBalance, 0.01)

        val metrics = repo.dashboardMetricsFlow.first()
        assertEquals(200.0, metrics.totalJama, 0.01)
        assertEquals(500.0, metrics.totalUdhaar, 0.01)
        assertEquals(300.0, metrics.netBalance, 0.01)

        repo.saveCalculation(CalcRecordCategory.STANDARD, "250+150", "400")
        assertEquals(1, repo.calcHistoryFlow.first().size)

        repo.saveSmsLog("Rohan Sharma", "9811012345", "Reminder ₹300", "Payment Reminder")
        assertEquals(1, repo.smsLogsFlow.first().size)

        db.close()
    }

    @Test
    fun `calculator computes expression and navigates across dashboard, ledger, sms, and tools`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simple SMS Calculator").assertIsDisplayed()

        // Perform 25 + 75 = 100
        composeRule.onNodeWithTag("key_2").performClick()
        composeRule.onNodeWithTag("key_5").performClick()
        composeRule.onNodeWithTag("key_plus").performClick()
        composeRule.onNodeWithTag("key_7").performClick()
        composeRule.onNodeWithTag("key_5").performClick()
        composeRule.onNodeWithTag("key_equals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calc_result_text").assertTextEquals("100")

        // Navigate to Dashboard
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("card_total_jama").assertIsDisplayed()
        composeRule.onNodeWithTag("card_total_udhaar").assertIsDisplayed()

        // Navigate to Hisab-Kitab
        composeRule.onNodeWithTag("nav_ledger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hisab-Kitab").assertIsDisplayed()

        // Navigate to SMS Center
        composeRule.onNodeWithTag("nav_sms").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SMS Center").assertIsDisplayed()

        // Navigate to Tools & open Settings -> switch to Light Ocean Emerald
        composeRule.onNodeWithTag("nav_tools").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tools_list")
            .performScrollToNode(hasTestTag("tool_settings"))
        composeRule.onNodeWithTag("tool_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Settings & Theme").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_mode_light").performClick()
        composeRule.waitForIdle()
    }
}
