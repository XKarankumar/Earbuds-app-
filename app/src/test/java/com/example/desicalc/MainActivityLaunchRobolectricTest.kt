package com.example.desicalc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.example.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class MainActivityLaunchRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `main activity launches and executes core user journeys without crashing`() {
        composeRule.mainClock.autoAdvance = true
        // Wait for animated splash screen to finish (~900ms + fade)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // 1. Calculator pad test
        composeRule.onNodeWithTag("nav_calculator").performClick()
        composeRule.onNodeWithTag("calculator_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("calc_key_7").performClick()
        composeRule.onNodeWithTag("calc_key_+").performClick()
        composeRule.onNodeWithTag("calc_key_3").performClick()
        composeRule.onNodeWithTag("calc_key_=").performClick()
        composeRule.onNodeWithTag("calc_result_text").assertTextEquals("10")

        // Switch through specialized calculator tabs
        composeRule.onNodeWithTag("calc_mode_GST").performClick()
        composeRule.onNodeWithTag("calc_mode_DISCOUNT").performClick()
        composeRule.onNodeWithTag("calc_mode_UNIT").performClick()
        composeRule.onNodeWithTag("calc_mode_BASIC").performClick()

        // 2. Customers & Udhar/Jama Ledger test (including CustomerFormDialog & TransactionEntryDialog)
        composeRule.onNodeWithTag("nav_customers").performClick()
        composeRule.onNodeWithTag("customers_list_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_add_customer").performClick()
        composeRule.onNodeWithTag("dialog_customer_name").performTextReplacement("Ramesh")
        composeRule.onNodeWithTag("dialog_customer_mobile").performTextReplacement("9876543210")
        composeRule.onNodeWithTag("dialog_save_customer_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching {
                composeRule.onNodeWithTag("customer_ledger_detail").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("btn_udhar_diya").performClick()
        composeRule.onNodeWithTag("tx_amount_input").performTextReplacement("500")
        composeRule.onNodeWithTag("tx_confirm_save_btn").performClick()

        // 3. AI Smart Assistant tab
        composeRule.onNodeWithTag("nav_ai_assistant").performClick()
        composeRule.onNodeWithTag("ai_assistant_screen").assertIsDisplayed()

        // 4. SMS & Reports tab
        composeRule.onNodeWithTag("nav_sms_reports").performClick()
        composeRule.onNodeWithTag("sms_reports_screen").assertIsDisplayed()

        // 5. Profile & Themes tab
        composeRule.onNodeWithTag("nav_profile").performClick()
        composeRule.onNodeWithTag("profile_settings_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_dashboard").performClick()
        composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    }
}
