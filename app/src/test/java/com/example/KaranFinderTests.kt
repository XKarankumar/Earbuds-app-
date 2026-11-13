package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.PhoneValidatorEngine
import com.example.data.SearchHistoryRepository
import com.example.data.local.AppDatabase
import com.example.model.HistoryRecordType
import com.example.security.CryptoVault
import com.example.security.RateLimiter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KaranFinderTests {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `phone validator validates international and official helpline numbers`() {
        val inResult = PhoneValidatorEngine.validate("9811012345", "IN")
        assertTrue(inResult.isValid)
        assertEquals("+91", inResult.countryCallingCode)
        assertEquals("IN", inResult.regionCode)
        assertEquals("+919811012345", inResult.e164Format)

        val usResult = PhoneValidatorEngine.validate("+1 650 253 0000", "IN")
        assertTrue(usResult.isValid)
        assertEquals("+1", usResult.countryCallingCode)
        assertEquals("US", usResult.regionCode)

        val helpline = PhoneValidatorEngine.validate("1930", "IN")
        assertTrue(helpline.isValid)
        assertEquals("OFFICIAL_HELPLINE", helpline.numberType)
        assertNotNull(helpline.officialHelplineName)

        val invalid = PhoneValidatorEngine.validate("12", "IN")
        assertFalse(invalid.isValid)
    }

    @Test
    fun `phone extractor finds numbers inside OCR text`() {
        val ocrText = "Acme Logistics Delhi\nSupport: +91 98110 22334\nToll Free: +1-800-555-0199"
        val extracted = PhoneValidatorEngine.extractPhoneNumbersFromText(ocrText, "IN")
        assertTrue(extracted.any { it.contains("98110") })
    }

    @Test
    fun `crypto vault encrypts and verifies pins accurately`() {
        val secret = "Test Phone Record +91 98110 12345"
        val encrypted = CryptoVault.encryptLocalString(secret)
        assertTrue(encrypted.contains(":"))
        assertEquals(secret, CryptoVault.decryptLocalString(encrypted))

        val (hash, salt) = CryptoVault.hashPinWithSalt("4829")
        assertTrue(CryptoVault.verifyPin("4829", hash, salt))
        assertFalse(CryptoVault.verifyPin("0000", hash, salt))
    }

    @Test
    fun `rate limiter enforces sliding window limit`() {
        var now = 1000L
        val limiter = RateLimiter(maxRequestsPerMinute = 2, windowMillis = 60_000L) { now }

        assertTrue(limiter.tryAcquire().isSuccess)
        assertTrue(limiter.tryAcquire().isSuccess)
        val third = limiter.tryAcquire()
        assertTrue(third.isFailure)

        now += 61_000L
        assertTrue(limiter.tryAcquire().isSuccess)
    }

    @Test
    fun `encrypted search history repository saves reads and deletes records`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = SearchHistoryRepository(db.searchHistoryDao())

        repo.saveRecord(
            recordType = HistoryRecordType.NUMBER,
            title = "+91 98110 12345",
            subtitle = "Unlisted / Private",
            riskLevel = "Low",
            riskScore = 12,
            summaryText = "Valid Indian mobile format.",
            sourceAttribution = "ITU E.164"
        )

        val saved = repo.allRecords.first()
        assertEquals(1, saved.size)
        assertEquals("+91 98110 12345", saved.first().title)

        repo.deleteRecord(saved.first().id)
        val afterDelete = repo.allRecords.first()
        assertTrue(afterDelete.isEmpty())
        db.close()
    }

    @Test
    fun `dashboard loads in guest mode with all modules and navigates cleanly`() {
        composeRule.waitForIdle()
        runCatching {
            composeRule.onNodeWithTag("splash_overlay").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5000L) {
            runCatching {
                composeRule.onNodeWithText("Karan Number Finder").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("card_number_lookup")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("card_security_center")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Security Center & Privacy").assertIsDisplayed()

        composeRule.onNodeWithTag("btn_back_security").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Karan Number Finder").assertIsDisplayed()
    }
}
