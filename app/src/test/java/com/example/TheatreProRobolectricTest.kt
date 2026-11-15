package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TheatreProRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appResources_matchTheatreProTitle() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals("Theatre Pro", ctx.getString(R.string.app_name))
    }

    @Test
    fun navigationTabs_switchAcrossAllFiveStudios() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ULTIMATE HOME THEATRE PRO").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_bass_eq").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("EXTRA BASS & SUBWOOFER LFE").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_dj").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("DUAL DECK DJ STUDIO").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_rgb").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rgb_studio_list")
            .performScrollToNode(hasText("WLED / ESP32 / BLE CONTROLLERS"))
        composeRule.onNodeWithText("WLED / ESP32 / BLE CONTROLLERS").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_system").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ANC HEADPHONE INSPECTOR").assertIsDisplayed()
        composeRule.onNodeWithTag("system_hub_list")
            .performScrollToNode(hasText("DOLBY ATMOS CERTIFICATION"))
        composeRule.onNodeWithText("DOLBY ATMOS CERTIFICATION").assertIsDisplayed()
    }
}
