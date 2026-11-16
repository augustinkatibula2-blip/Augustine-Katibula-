package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.LinkUpRepository
import com.example.data.repository.SessionManager
import com.example.domain.AccountType
import com.example.domain.MatchingEngine
import com.example.domain.VerificationTier
import com.example.platform.LinkUpNotificationHelper
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LinkUpFlowTest {

    private lateinit var db: LinkUpDatabase
    private lateinit var repo: LinkUpRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LinkUpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LinkUpRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `two-way cofounder compatibility rewards complementary skills and filters incompatible physical locations`() {
        val agriFounder = UserProfileEntity(
            userId = "u1",
            fullName = "Chisomo",
            country = "Malawi",
            city = "Lilongwe",
            skills = "Agribusiness,Business Development",
            interests = "AgriTech,Rural Markets",
            languages = "English,Chichewa"
        )
        val agriPref = FounderPreferenceEntity(
            userId = "u1",
            industry = "AgriTech",
            businessIdea = "Smallholder grain marketplace",
            skillsPossessed = "Agribusiness,Business Development",
            skillsNeeded = "Kotlin / Android,Full-Stack Web",
            commitmentType = "PART_TIME",
            weeklyHoursAvailable = 25,
            willingRemote = true,
            equityExpectation = "Equal Split (50/50)"
        )

        val devFounder = UserProfileEntity(
            userId = "u2",
            fullName = "Tadziona",
            country = "Malawi",
            city = "Blantyre",
            skills = "Kotlin / Android,Full-Stack Web",
            interests = "AgriTech,Offline Mobile",
            languages = "English,Chichewa"
        )
        val devPref = FounderPreferenceEntity(
            userId = "u2",
            industry = "AgriTech",
            businessIdea = "Open to AgriTech co-founding",
            skillsPossessed = "Kotlin / Android,Full-Stack Web",
            skillsNeeded = "Agribusiness,Business Development",
            commitmentType = "PART_TIME",
            weeklyHoursAvailable = 25,
            willingRemote = true,
            equityExpectation = "Equal Split (50/50)"
        )

        val result = MatchingEngine.calculateCofounderCompatibility(agriFounder, agriPref, devFounder, devPref)
        assertTrue(result.passedEssentialFilter)
        assertTrue("Expected high score >= 85, got ${result.score}", result.score >= 85)
        assertTrue(result.reasons.isNotEmpty())

        // Test essential filter failure when neither accepts remote and cities differ
        val rigidMatch = MatchingEngine.calculateCofounderCompatibility(
            agriFounder,
            agriPref.copy(willingRemote = false),
            devFounder,
            devPref.copy(willingRemote = false)
        )
        assertFalse(rigidMatch.passedEssentialFilter)
        assertEquals(0, rigidMatch.score)
    }

    @Test
    fun `job matching never penalizes self-taught candidates when degree is not required`() {
        val selfTaughtDev = UserProfileEntity(
            userId = "dev1",
            fullName = "Self Taught Builder",
            country = "Malawi",
            city = "Blantyre",
            educationLevel = "Self-Taught / Portfolio",
            yearsExperience = 3,
            skills = "Kotlin / Android,UI/UX Design",
            interests = "FinTech,Mobile"
        )
        val job = JobListingEntity(
            id = "j1",
            employerId = "emp1",
            title = "Android Engineer",
            organization = "NyasaTech",
            description = "Build mobile banking app",
            requiredQualifications = "Portfolio of shipped apps",
            degreeRequired = false,
            requiredSkills = "Kotlin / Android,UI/UX Design",
            experienceLevel = "Intermediate",
            minYearsExperience = 2,
            country = "Malawi",
            city = "Blantyre",
            workMode = "HYBRID"
        )

        val (score, reasons) = MatchingEngine.calculateJobMatch(selfTaughtDev, job)
        assertTrue("Self-taught candidate with matching skills should score >= 85, got $score", score >= 85)
        assertTrue(reasons.any { it.contains("no formal degree gate", ignoreCase = true) })
    }

    @Test
    fun `registration hashes password and mutual match opens conversation and trial`() = runBlocking {
        val reg1 = repo.registerUser("founder1@mw.test", "Secret123", "Founder One", AccountType.FOUNDER, "Malawi", "Lilongwe")
        val reg2 = repo.registerUser("founder2@mw.test", "Secret123", "Founder Two", AccountType.FOUNDER, "Malawi", "Blantyre")
        assertTrue(reg1.isSuccess)
        assertTrue(reg2.isSuccess)

        val u1 = reg1.getOrThrow()
        val u2 = reg2.getOrThrow()
        // Password must never be stored in plain text
        assertNotEquals("Secret123", u1.passwordHash)
        assertTrue(repo.loginUser("founder1@mw.test", "Secret123").isSuccess)
        assertTrue(repo.loginUser("founder1@mw.test", "WrongPass").isFailure)

        // One-way interest is not mutual yet
        val mutualFirst = repo.recordCofounderDecision(u1.id, u2.id, "INTERESTED", 88, "Skills complement")
        assertFalse(mutualFirst)

        // Reciprocal interest triggers mutual match + opens conversation
        val mutualSecond = repo.recordCofounderDecision(u2.id, u1.id, "INTERESTED", 88, "Skills complement")
        assertTrue(mutualSecond)

        val convs = repo.allConversations.first()
        assertEquals(1, convs.size)

        // Send message and verify persistence
        repo.sendMessage(convs.first(), u1.id, "Let us start a 14-day Build Together trial!")
        val msgs = repo.observeMessages(convs.first().id).first()
        assertEquals(1, msgs.size)
        assertEquals("Let us start a 14-day Build Together trial!", msgs.first().body)

        // Start 14-day trial and submit review
        repo.createTrialCollaboration(u1.id, u2.id, "AgriMVP", "Build prototype", "U1 product, U2 code")
        val trials = repo.allTrials.first()
        assertEquals(1, trials.size)
        repo.submitTrialReview(trials.first(), u1.id, true, true, true, true, "Great sprint!")
        assertEquals("COMPLETED", repo.allTrials.first().first().status)

        // Verification requires admin approval before badge is awarded
        repo.submitVerification(u1.id, "EDUCATION", "UNIMA Certificate", "Registrar")
        val pending = repo.allVerifications.first().first()
        assertEquals(VerificationTier.UNVERIFIED.code, db.userDao().getProfile(u1.id)?.verificationStatus)
        repo.reviewVerification(pending, approve = true, notes = "Verified")
        assertEquals(VerificationTier.EDUCATION_VERIFIED.code, db.userDao().getProfile(u1.id)?.verificationStatus)
    }

    @Test
    fun `session intro flag and android notification channel register cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = SessionManager(context)
        assertFalse(session.hasSeenIntro.first())
        session.setHasSeenIntro(true)
        assertTrue(session.hasSeenIntro.first())

        LinkUpNotificationHelper.ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(LinkUpNotificationHelper.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(LinkUpNotificationHelper.CHANNEL_ID, channel.id)
    }

    @Test
    fun `theme mode preference defaults to SYSTEM and persists light and dark choices`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val session = SessionManager(context)
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromCode(session.themeMode.first()))

        session.setThemeMode(AppThemeMode.DARK.code)
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromCode(session.themeMode.first()))

        session.setThemeMode(AppThemeMode.LIGHT.code)
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromCode(session.themeMode.first()))
    }
}
