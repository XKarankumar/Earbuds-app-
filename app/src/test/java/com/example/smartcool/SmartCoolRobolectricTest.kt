package com.example.smartcool

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.MainActivity
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmartCoolRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appResources_matchSmartCoolPro() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Smart Cool Pro", ctx.getString(R.string.app_name))
    }

    @Test
    fun navigationTabs_andThemes_workAcrossAllScreens() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SMART COOL & BATTERY SAVER PRO").assertIsDisplayed()

        // Cooling screen
        composeRule.onNodeWithTag("nav_cooling").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SMART COOLING & THERMAL API").assertIsDisplayed()

        // Battery screen
        composeRule.onNodeWithTag("nav_battery").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("BATTERY MANAGEMENT").assertIsDisplayed()

        // Performance Monitor screen
        composeRule.onNodeWithTag("nav_performance").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PERFORMANCE MONITOR").assertIsDisplayed()

        // Settings & 7 Themes Studio
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("THEME STUDIO (7 LIQUID THEMES)").assertIsDisplayed()
        composeRule.onNodeWithTag("live_theme_preview_card").assertIsDisplayed()

        // Select Midnight AMOLED & Ocean Blue themes
        composeRule.onNodeWithTag("theme_option_midnight_amoled").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_option_ocean_blue").performClick()
        composeRule.waitForIdle()

        // Scroll to Custom Theme & Apply
        composeRule.onNodeWithTag("settings_themes_list")
            .performScrollToNode(hasTestTag("apply_custom_theme_btn"))
        composeRule.onNodeWithTag("apply_custom_theme_btn").performClick()
        composeRule.waitForIdle()

        // Navigate back to Overview and open Modes & Daily Report
        composeRule.onNodeWithTag("nav_overview").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("overview_list")
            .performScrollToNode(hasTestTag("overview_open_modes_btn"))
        composeRule.onNodeWithTag("overview_open_modes_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SMART MODES & DAILY REPORT").assertIsDisplayed()
    }
}
