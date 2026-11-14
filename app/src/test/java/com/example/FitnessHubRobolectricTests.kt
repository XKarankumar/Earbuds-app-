package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ExerciseCatalog
import com.example.data.FitnessRepository
import com.example.data.local.AppDatabase
import com.example.domain.FitnessCalculators
import com.example.domain.QrCodeGenerator
import com.example.domain.SmsGatewayService
import com.example.model.ActivityLevel
import com.example.model.AttendanceStatus
import com.example.model.CountryCode
import com.example.model.FitnessGoalType
import com.example.model.MembershipPlan
import com.example.model.MuscleGroup
import com.example.model.SmsDeliveryStatus
import com.example.model.SmsEventType
import com.example.model.UserRole
import com.example.security.AppPreferencesRepository
import com.example.security.KeystoreCryptoManager
import com.example.security.TokenSecurityEngine
import com.example.security.TokenVerificationResult
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FitnessHubRobolectricTests {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var crypto: KeystoreCryptoManager
    private lateinit var prefs: AppPreferencesRepository
    private lateinit var repo: FitnessRepository
    private lateinit var tokenEngine: TokenSecurityEngine
    private lateinit var smsService: SmsGatewayService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        crypto = KeystoreCryptoManager(context)
        prefs = AppPreferencesRepository(context)
        repo = FitnessRepository(db, crypto)
        tokenEngine = TokenSecurityEngine(crypto, db.tokenDao(), prefs)
        smsService = SmsGatewayService(
            context = context,
            crypto = crypto,
            prefsRepo = prefs,
            memberDao = db.memberDao(),
            attendanceDao = db.attendanceDao(),
            smsLogDao = db.smsLogDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `exercise catalog covers all 10 muscle groups with full biomechanics data`() {
        MuscleGroup.entries.forEach { group ->
            val list = ExerciseCatalog.byMuscleGroup(group)
            assertTrue("Missing exercises for $group", list.size >= 3)
            list.forEach { ex ->
                assertTrue(ex.primaryMuscles.isNotEmpty())
                assertTrue(ex.equipment.isNotBlank())
                assertTrue(ex.postureGuide.isNotBlank())
                assertTrue(ex.steps.isNotEmpty())
                assertTrue(ex.commonMistakes.isNotEmpty())
                assertTrue(ex.safetyInstructions.isNotEmpty())
                assertTrue(ex.defaultRestSec > 0)
            }
        }
    }

    @Test
    fun `bmi and nutrition calculators produce accurate targets`() {
        val bmi = FitnessCalculators.calculateBmi(weightKg = 72f, heightCm = 175f)
        assertEquals(23.5f, bmi.bmi, 0.2f)
        assertEquals("Normal (Healthy)", bmi.category)

        val macros = FitnessCalculators.calculateNutritionTargets(
            age = 25,
            isMale = true,
            weightKg = 72f,
            heightCm = 175f,
            activityLevel = ActivityLevel.MODERATE,
            goalType = FitnessGoalType.MUSCLE_GAIN
        )
        assertTrue(macros.targetCalories > 2400)
        assertTrue(macros.proteinGrams >= 140)
    }

    @Test
    fun `country phone validation and QR pass generation work`() {
        val india = CountryCode.byDialCode("+91")
        assertTrue(FitnessCalculators.validatePhoneNumber(india, "9876543210").first)
        assertFalse(FitnessCalculators.validatePhoneNumber(india, "12345").first)

        val payload = QrCodeGenerator.buildMemberQrPayload("FH-MEM-1001")
        assertEquals("FH-MEM-1001", QrCodeGenerator.parseMemberCodeFromQr(payload))
        val bmp = QrCodeGenerator.generateQrBitmap(payload, 128)
        assertNotNull(bmp)
        assertEquals(128, bmp!!.width)
    }

    @Test
    fun `aes encryption roundtrip protects sensitive pii`() {
        val secret = "9876543210"
        val enc = crypto.encrypt(secret)
        assertTrue(enc.startsWith("ENC:"))
        assertFalse(enc.contains(secret))
        assertEquals(secret, crypto.decrypt(enc))
    }

    @Test
    fun `token lifecycle supports init, issue, verify, offline lease, revoke and lockout`() = runBlocking {
        val owner = tokenEngine.initializeFirstOwnerConsole()
        assertTrue(owner.plainToken.startsWith("FH-ADM-"))

        val session = tokenEngine.restoreCachedOfflineSession()
        assertTrue(session.isAuthenticated)
        assertEquals(UserRole.ADMIN, session.role)

        val memberPair = tokenEngine.generateUniqueToken(
            role = UserRole.MEMBER,
            label = "Test Member",
            validityDays = 30,
            bindToCurrentDevice = true
        )
        val insertedId = repo.insertToken(memberPair.entity)

        val ok = tokenEngine.verifyToken(memberPair.plainToken)
        assertTrue(ok is TokenVerificationResult.Verified)

        repo.revokeToken(insertedId)
        val revoked = tokenEngine.verifyToken(memberPair.plainToken)
        assertTrue(revoked is TokenVerificationResult.Denied)
    }

    @Test
    fun `missed gym detection notifies unlogged members once and blocks duplicates`() = runBlocking {
        val todayDay = FitnessCalculators.currentDayCode()
        val mMissing = repo.addMember(
            fullName = "Missing Athlete",
            countryCode = "+91",
            phoneDigits = "9876500001",
            emergencyName = "Guard",
            emergencyPhone = "9876500002",
            phoneVerified = true,
            photoPath = null,
            plan = MembershipPlan.MONTHLY,
            feeAmount = 1500.0,
            scheduledDays = setOf(todayDay),
            trainerId = null,
            smsAttendanceMissing = true,
            smsWorkoutMissed = true,
            smsMembershipExpiry = true,
            smsFeePending = true,
            smsPaymentConfirm = true,
            smsWelcome = false,
            smsTokenExpiry = true
        )
        val mPresent = repo.addMember(
            fullName = "Present Athlete",
            countryCode = "+91",
            phoneDigits = "9876500003",
            emergencyName = "Guard",
            emergencyPhone = "9876500004",
            phoneVerified = true,
            photoPath = null,
            plan = MembershipPlan.MONTHLY,
            feeAmount = 1500.0,
            scheduledDays = setOf(todayDay),
            trainerId = null,
            smsAttendanceMissing = true,
            smsWorkoutMissed = true,
            smsMembershipExpiry = true,
            smsFeePending = true,
            smsPaymentConfirm = true,
            smsWelcome = false,
            smsTokenExpiry = true
        )
        repo.recordAttendance(mPresent, AttendanceStatus.PRESENT, "QR_SCAN")

        val firstRun = smsService.runMissedGymDetection(forceIgnoreDeadline = true)
        assertEquals(1, firstRun.missingCount)
        assertEquals(1, firstRun.sentCount)
        assertEquals(0, firstRun.skippedDuplicateCount)

        // Second run same day must skip duplicate
        val secondRun = smsService.runMissedGymDetection(forceIgnoreDeadline = true)
        assertEquals(0, secondRun.sentCount)
        assertEquals(1, secondRun.skippedDuplicateCount)

        // Opt-out member test
        repo.updateMemberSmsPrefs(mMissing.id, false, true, true, true, true, true, true)
        val optOutStatus = smsService.dispatchMemberSms(mMissing.copy(smsAttendanceMissing = false), SmsEventType.ATTENDANCE_MISSING, "Test", "2099-01-01")
        assertEquals(SmsDeliveryStatus.SKIPPED_OPT_OUT, optOutStatus)
    }
}
