package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        UserProfileEntity::class,
        SkillEntity::class,
        UserSkillEntity::class,
        EducationEntity::class,
        ExperienceEntity::class,
        FounderPreferenceEntity::class,
        JobListingEntity::class,
        JobApplicationEntity::class,
        StartupOpportunityEntity::class,
        MatchRecordEntity::class,
        ConnectionRequestEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        VerificationSubmissionEntity::class,
        TrialCollaborationEntity::class,
        CollaborationTaskEntity::class,
        CollaborationReviewEntity::class,
        ReportEntity::class,
        CountryRegionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LinkUpDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun jobAndTalentDao(): JobAndTalentDao
    abstract fun matchingAndMessagingDao(): MatchingAndMessagingDao
    abstract fun trustTrialAndAdminDao(): TrustTrialAndAdminDao

    companion object {
        @Volatile
        private var INSTANCE: LinkUpDatabase? = null

        fun getInstance(context: Context): LinkUpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LinkUpDatabase::class.java,
                    "linkup_database.db"
                )
                    .addCallback(SeedCallback(context.applicationContext))
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback(private val appContext: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                populateInitialData(getInstance(appContext))
            }
        }
    }
}

suspend fun populateInitialData(db: LinkUpDatabase) {
    val userDao = db.userDao()
    val jobDao = db.jobAndTalentDao()
    val matchDao = db.matchingAndMessagingDao()
    val adminDao = db.trustTrialAndAdminDao()

    // 1. Seed Countries & Cities (Launching in Malawi, expandable across Africa)
    val countries = listOf(
        CountryRegionEntity("MW", "Malawi", "Lilongwe,Blantyre,Zomba,Mzuzu,Mangochi", true),
        CountryRegionEntity("ZM", "Zambia", "Lusaka,Ndola,Kitwe,Livingstone", true),
        CountryRegionEntity("TZ", "Tanzania", "Dar es Salaam,Arusha,Dodoma,Mwanza", true),
        CountryRegionEntity("KE", "Kenya", "Nairobi,Mombasa,Kisumu,Nakuru", true),
        CountryRegionEntity("UG", "Uganda", "Kampala,Entebbe,Jinja", true),
        CountryRegionEntity("RW", "Rwanda", "Kigali,Huye,Musanze", true),
        CountryRegionEntity("GH", "Ghana", "Accra,Kumasi,Tamale", true),
        CountryRegionEntity("NG", "Nigeria", "Lagos,Abuja,Ibadan,Port Harcourt", true)
    )
    countries.forEach { adminDao.insertCountry(it) }

    // 2. Seed Core Skills Catalog
    val skills = listOf(
        SkillEntity("sk_kotlin", "Kotlin / Android", "Engineering"),
        SkillEntity("sk_fullstack", "Full-Stack Web", "Engineering"),
        SkillEntity("sk_python", "Python & Data", "Engineering"),
        SkillEntity("sk_agri", "Agribusiness", "Industry"),
        SkillEntity("sk_bizdev", "Business Development", "Business"),
        SkillEntity("sk_product", "Product Management", "Product"),
        SkillEntity("sk_uiux", "UI/UX Design", "Design"),
        SkillEntity("sk_finance", "Financial Modeling", "Finance"),
        SkillEntity("sk_marketing", "Digital Marketing", "Growth"),
        SkillEntity("sk_sales", "B2B Sales", "Growth"),
        SkillEntity("sk_ops", "Supply Chain & Operations", "Operations"),
        SkillEntity("sk_solar", "Renewable Energy Systems", "Engineering")
    )
    userDao.insertSkills(skills)

    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L

    fun createSampleAccount(
        id: String,
        email: String,
        pass: String,
        accountType: String,
        role: String = "USER",
        isSample: Boolean = true
    ): UserEntity {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(pass, salt)
        return UserEntity(
            id = id,
            email = email,
            passwordHash = hash,
            passwordSalt = salt,
            authProvider = "EMAIL",
            accountType = accountType,
            role = role,
            isSuspended = false,
            isSampleData = isSample,
            createdAt = now - dayMs * 10
        )
    }

    // Demo & Sample Users (all passwords hashed with PBKDF2 + random salt)
    val users = listOf(
        createSampleAccount("user_admin", "augustinkatibula2@gmail.com", "LinkUpAdmin2026!", "FOUNDER", role = "ADMIN", isSample = false),
        createSampleAccount("sample_founder_1", "chisomo.banda@example.mw", "SamplePass1!", "FOUNDER"),
        createSampleAccount("sample_founder_2", "tadziona.phiri@example.mw", "SamplePass1!", "FOUNDER"),
        createSampleAccount("sample_founder_3", "amara.okafor@example.ng", "SamplePass1!", "FOUNDER"),
        createSampleAccount("sample_talent_1", "takondwa.mwale@example.mw", "SamplePass1!", "TALENT"),
        createSampleAccount("sample_seeker_1", "limbani.chirwa@example.mw", "SamplePass1!", "JOB_SEEKER"),
        createSampleAccount("sample_employer_1", "careers@nyasatech.example.mw", "SamplePass1!", "EMPLOYER"),
        createSampleAccount("sample_student_1", "faith.kabwe@example.zm", "SamplePass1!", "STUDENT")
    )
    users.forEach { userDao.insertUser(it) }

    val profiles = listOf(
        UserProfileEntity(
            userId = "user_admin",
            fullName = "Augustine Katibula",
            headline = "Founder of LinkUp · Building African Opportunity Infrastructure",
            country = "Malawi",
            city = "Lilongwe",
            phone = "+265 999 000 100",
            educationLevel = "Self-Taught / Portfolio",
            institution = "Malawi Innovation Hub",
            experienceSummary = "Entrepreneurship, business development, and technology solutions for African professionals.",
            yearsExperience = 4,
            skills = "Business Development,Product Management,Startup Strategy,Community Building",
            interests = "Entrepreneurship,EdTech,FinTech,African Talent",
            careerGoals = "Connect African talent with compatible co-founders, employers, and meaningful opportunities.",
            portfolioUrl = "https://linkup.africa",
            languages = "English,Chichewa",
            workMode = "HYBRID",
            emailVerified = true,
            professionallyVerified = true,
            verificationStatus = "PROFESSIONALLY_VERIFIED",
            isSampleData = false
        ),
        UserProfileEntity(
            userId = "sample_founder_1",
            fullName = "Chisomo Banda",
            headline = "AgriTech Entrepreneur & Supply Chain Strategist",
            country = "Malawi",
            city = "Lilongwe",
            phone = "+265 888 111 222",
            educationLevel = "Bachelor's",
            institution = "LUANAR (Lilongwe University of Agriculture)",
            experienceSummary = "5 years working with smallholder maize & groundnut cooperatives in Central Malawi.",
            yearsExperience = 5,
            skills = "Agribusiness,Business Development,Supply Chain & Operations,B2B Sales",
            interests = "AgriTech,Climate Resilience,Rural FinTech,Mobile Commerce",
            careerGoals = "Launch a mobile marketplace connecting Malawian smallholder farmers directly to regional buyers.",
            portfolioUrl = "https://linkedin.com/in/chisomo-banda",
            languages = "English,Chichewa",
            workMode = "HYBRID",
            emailVerified = true,
            educationVerified = true,
            verificationStatus = "EDUCATION_VERIFIED",
            isSampleData = true
        ),
        UserProfileEntity(
            userId = "sample_founder_2",
            fullName = "Tadziona Phiri",
            headline = "Full-Stack & Android Software Developer",
            country = "Malawi",
            city = "Blantyre",
            phone = "+265 991 222 333",
            educationLevel = "Self-Taught / Portfolio",
            institution = "Self-Taught & MUBAS Open Tech Labs",
            experienceSummary = "4 years building offline-first Android & USSD payment apps for low-bandwidth regions.",
            yearsExperience = 4,
            skills = "Kotlin / Android,Full-Stack Web,Python & Data,Product Management",
            interests = "AgriTech,FinTech,Offline-First Mobile,AI for Agriculture",
            careerGoals = "Partner with a domain-expert co-founder to build impactful African AgriTech or FinTech software.",
            portfolioUrl = "https://github.com/tadziona-phiri",
            languages = "English,Chichewa,Tumbuka",
            workMode = "REMOTE",
            emailVerified = true,
            professionallyVerified = true,
            verificationStatus = "PROFESSIONALLY_VERIFIED",
            isSampleData = true
        ),
        UserProfileEntity(
            userId = "sample_founder_3",
            fullName = "Amara Okafor",
            headline = "Solar Microgrid Engineer & Clean Energy Builder",
            country = "Kenya",
            city = "Nairobi",
            educationLevel = "Master's / PhD",
            institution = "University of Nairobi",
            experienceSummary = "6 years designing pay-as-you-go clean energy hardware and rural mini-grids across East Africa.",
            yearsExperience = 6,
            skills = "Renewable Energy Systems,Financial Modeling,Supply Chain & Operations",
            interests = "Clean Energy,AgriTech,Climate Tech,Hardware Startups",
            careerGoals = "Expand solar cold-chain hubs into Malawi, Zambia, and Tanzania with local operations co-founders.",
            portfolioUrl = "https://linkedin.com/in/amara-okafor",
            languages = "English,Swahili",
            workMode = "REMOTE",
            emailVerified = true,
            professionallyVerified = true,
            verificationStatus = "PROFESSIONALLY_VERIFIED",
            isSampleData = true
        ),
        UserProfileEntity(
            userId = "sample_talent_1",
            fullName = "Takondwa Mwale",
            headline = "UI/UX Product Designer & Brand Strategist",
            country = "Malawi",
            city = "Mzuzu",
            educationLevel = "Certificate / Diploma",
            institution = "Mzuzu Tech & Design Collective",
            experienceSummary = "3 years crafting mobile-first interfaces and low-data UX for startups in Lilongwe and Kigali.",
            yearsExperience = 3,
            skills = "UI/UX Design,Digital Marketing,Product Management",
            interests = "EdTech,FinTech,Design Systems,Creative Startups",
            careerGoals = "Join an early-stage African startup team as founding designer or product partner.",
            portfolioUrl = "https://behance.net/takondwa-mwale",
            languages = "English,Chichewa",
            workMode = "REMOTE",
            emailVerified = true,
            verificationStatus = "EMAIL_VERIFIED",
            isSampleData = true
        ),
        UserProfileEntity(
            userId = "sample_seeker_1",
            fullName = "Limbani Chirwa",
            headline = "Backend Engineer & Data Analyst",
            country = "Malawi",
            city = "Lilongwe",
            educationLevel = "Bachelor's",
            institution = "University of Malawi (UNIMA)",
            experienceSummary = "3 years building APIs, mobile money integrations, and SQL analytics dashboards.",
            yearsExperience = 3,
            skills = "Python & Data,Full-Stack Web,Kotlin / Android",
            interests = "FinTech,HealthTech,Logistics",
            careerGoals = "Grow in a high-impact software engineering role with regional exposure.",
            portfolioUrl = "https://github.com/limbani-chirwa",
            languages = "English,Chichewa",
            workMode = "HYBRID",
            emailVerified = true,
            verificationStatus = "EMAIL_VERIFIED",
            isSampleData = true
        ),
        UserProfileEntity(
            userId = "sample_employer_1",
            fullName = "NyasaTech Labs",
            headline = "Software & Digital Infrastructure Studio in Blantyre",
            country = "Malawi",
            city = "Blantyre",
            educationLevel = "Bachelor's",
            institution = "NyasaTech Labs Ltd",
            experienceSummary = "Building digital banking, logistics, and civic technology platforms in Malawi.",
            yearsExperience = 7,
            skills = "Kotlin / Android,Full-Stack Web,Product Management,UI/UX Design",
            interests = "FinTech,CivicTech,Software Engineering",
            careerGoals = "Hire practical, portfolio-driven engineers and product builders across Malawi.",
            portfolioUrl = "https://nyasatech.mw",
            languages = "English,Chichewa",
            workMode = "HYBRID",
            emailVerified = true,
            professionallyVerified = true,
            verificationStatus = "PROFESSIONALLY_VERIFIED",
            isSampleData = true
        ),
        UserProfileEntity(
            userId = "sample_student_1",
            fullName = "Faith Kabwe",
            headline = "Final-Year Computer Science & Growth Enthusiast",
            country = "Zambia",
            city = "Lusaka",
            educationLevel = "Bachelor's",
            institution = "University of Zambia",
            experienceSummary = "Built campus marketplace MVP and led student developer workshops.",
            yearsExperience = 1,
            skills = "Full-Stack Web,Digital Marketing,UI/UX Design",
            interests = "EdTech,E-Commerce,Youth Entrepreneurship",
            careerGoals = "Join an early-stage startup internship or partner on an EdTech venture.",
            languages = "English",
            workMode = "REMOTE",
            emailVerified = false,
            verificationStatus = "UNVERIFIED",
            isSampleData = true
        )
    )
    profiles.forEach { userDao.insertProfile(it) }

    // 3. Seed Founder Preferences (for 2-way matching)
    val founderPrefs = listOf(
        FounderPreferenceEntity(
            userId = "user_admin",
            industry = "EdTech & Talent",
            businessIdea = "Expanding cross-border co-founder & startup collaboration across Southern & East Africa.",
            hasBusinessIdea = true,
            skillsPossessed = "Business Development,Product Management,Startup Strategy",
            skillsNeeded = "Kotlin / Android,Full-Stack Web,UI/UX Design",
            preferredRoles = "CTO / Software Developer,Product Designer",
            preferredExperienceLevel = "Intermediate (2-4 yrs)",
            preferredEducation = "Any (Skills & Portfolio First)",
            weeklyHoursAvailable = 30,
            commitmentType = "FULL_TIME",
            startupStage = "MVP",
            fundingSituation = "BOOTSTRAPPED",
            willingRemote = true,
            equityExpectation = "Vesting 20-40%",
            workingStyle = "Fast Iterative Builder"
        ),
        FounderPreferenceEntity(
            userId = "sample_founder_1",
            industry = "AgriTech",
            businessIdea = "USSD + Android platform linking Malawian smallholder farmers to verified buyers and cold storage.",
            hasBusinessIdea = true,
            skillsPossessed = "Agribusiness,Business Development,Supply Chain & Operations,B2B Sales",
            skillsNeeded = "Kotlin / Android,Full-Stack Web,Python & Data",
            preferredRoles = "CTO / Software Developer",
            preferredExperienceLevel = "Intermediate (2-4 yrs)",
            preferredEducation = "Any (Skills & Portfolio First)",
            weeklyHoursAvailable = 25,
            commitmentType = "PART_TIME",
            startupStage = "IDEA",
            fundingSituation = "BOOTSTRAPPED",
            willingRemote = true,
            equityExpectation = "Equal Split (50/50)",
            workingStyle = "Community & Impact Led"
        ),
        FounderPreferenceEntity(
            userId = "sample_founder_2",
            industry = "AgriTech",
            businessIdea = "Open to co-founding an offline-first AgriTech or FinTech mobile platform in Malawi.",
            hasBusinessIdea = false,
            skillsPossessed = "Kotlin / Android,Full-Stack Web,Python & Data",
            skillsNeeded = "Agribusiness,Business Development,B2B Sales,Supply Chain & Operations",
            preferredRoles = "CEO / Business Strategy,COO / Operations",
            preferredExperienceLevel = "Intermediate (2-4 yrs)",
            preferredEducation = "Any (Skills & Portfolio First)",
            weeklyHoursAvailable = 25,
            commitmentType = "PART_TIME",
            startupStage = "IDEA",
            fundingSituation = "BOOTSTRAPPED",
            willingRemote = true,
            equityExpectation = "Equal Split (50/50)",
            workingStyle = "Community & Impact Led"
        ),
        FounderPreferenceEntity(
            userId = "sample_founder_3",
            industry = "Clean Energy",
            businessIdea = "Solar-powered cold-chain hubs for agricultural markets across Southern & East Africa.",
            hasBusinessIdea = true,
            skillsPossessed = "Renewable Energy Systems,Financial Modeling,Supply Chain & Operations",
            skillsNeeded = "Business Development,Kotlin / Android,B2B Sales",
            preferredRoles = "COO / Operations,CTO / Software Developer",
            preferredExperienceLevel = "Senior (5+ yrs)",
            preferredEducation = "Any (Skills & Portfolio First)",
            weeklyHoursAvailable = 40,
            commitmentType = "FULL_TIME",
            startupStage = "EARLY_TRACTION",
            fundingSituation = "PRE_SEED",
            willingRemote = true,
            equityExpectation = "Vesting 20-40%",
            workingStyle = "Structured & Data-Driven"
        ),
        FounderPreferenceEntity(
            userId = "sample_talent_1",
            industry = "FinTech",
            businessIdea = "Interested in joining an early-stage mobile product team focused on youth financial tools.",
            hasBusinessIdea = false,
            skillsPossessed = "UI/UX Design,Digital Marketing,Product Management",
            skillsNeeded = "Kotlin / Android,Business Development,Financial Modeling",
            preferredRoles = "CTO / Software Developer,CEO / Business Strategy",
            preferredExperienceLevel = "Any",
            preferredEducation = "Any (Skills & Portfolio First)",
            weeklyHoursAvailable = 20,
            commitmentType = "PART_TIME",
            startupStage = "MVP",
            fundingSituation = "BOOTSTRAPPED",
            willingRemote = true,
            equityExpectation = "Negotiable",
            workingStyle = "Research & Design Focused"
        )
    )
    founderPrefs.forEach { userDao.insertFounderPref(it) }

    // 4. Seed Sample Job Listings (Never require degree unless genuinely required)
    val jobs = listOf(
        JobListingEntity(
            id = "job_sample_1",
            employerId = "sample_employer_1",
            title = "Android Kotlin Developer",
            organization = "NyasaTech Labs",
            description = "Build resilient, low-bandwidth Android apps using Jetpack Compose and offline caching for Malawian SMEs.",
            requiredQualifications = "Demonstrated portfolio of shipped Android apps; university degree not mandatory.",
            degreeRequired = false,
            requiredSkills = "Kotlin / Android,Full-Stack Web,UI/UX Design",
            experienceLevel = "Intermediate (2-4 yrs)",
            minYearsExperience = 2,
            country = "Malawi",
            city = "Blantyre",
            workMode = "HYBRID",
            salaryRange = "MWK 1,400,000 - 2,200,000 / mo",
            employmentType = "FULL_TIME",
            deadline = "2026-12-30",
            applicationInstructions = "Submit your profile and link to 2 real Android projects or GitHub repositories.",
            moderationStatus = "APPROVED",
            isSampleData = true,
            createdAt = now - dayMs * 3
        ),
        JobListingEntity(
            id = "job_sample_2",
            employerId = "sample_employer_1",
            title = "Agri-Supply Operations Coordinator",
            organization = "Chitedze Harvest Network",
            description = "Coordinate farmer onboarding, collection schedules, and quality checks across Lilongwe and Dedza.",
            requiredQualifications = "Experience with agricultural logistics or cooperative coordination.",
            degreeRequired = false,
            requiredSkills = "Agribusiness,Supply Chain & Operations,B2B Sales",
            experienceLevel = "Intermediate (2-4 yrs)",
            minYearsExperience = 2,
            country = "Malawi",
            city = "Lilongwe",
            workMode = "PHYSICAL",
            salaryRange = "MWK 950,000 - 1,350,000 / mo",
            employmentType = "FULL_TIME",
            deadline = "2026-12-15",
            applicationInstructions = "Apply via LinkUp and share your field operations experience.",
            moderationStatus = "APPROVED",
            isSampleData = true,
            createdAt = now - dayMs * 2
        ),
        JobListingEntity(
            id = "job_sample_3",
            employerId = "sample_employer_1",
            title = "Product Designer — Remote East & Southern Africa",
            organization = "ZuriPay FinTech",
            description = "Design intuitive mobile savings and merchant onboarding flows for low-cost Android devices.",
            requiredQualifications = "Strong mobile UX portfolio showing user research and Figma prototypes.",
            degreeRequired = false,
            requiredSkills = "UI/UX Design,Product Management,Digital Marketing",
            experienceLevel = "Intermediate (2-4 yrs)",
            minYearsExperience = 2,
            country = "Kenya",
            city = "Nairobi",
            workMode = "REMOTE",
            salaryRange = "USD 1,200 - 1,800 / mo",
            employmentType = "CONTRACT",
            deadline = "2026-12-20",
            applicationInstructions = "Include portfolio link in your LinkUp profile before applying.",
            moderationStatus = "APPROVED",
            isSampleData = true,
            createdAt = now - dayMs
        )
    )
    jobs.forEach { jobDao.insertJob(it) }

    // 5. Seed Startup Talent Opportunities (Explicit compensation types)
    val opportunities = listOf(
        StartupOpportunityEntity(
            id = "opp_sample_1",
            founderId = "sample_founder_1",
            startupName = "Mzinda AgriLink",
            roleCategory = "Software Developer",
            title = "Lead Mobile Engineer (Equity + Revenue Share)",
            description = "Build our offline-first Android farmer directory and USSD price alert engine for Malawi.",
            requiredSkills = "Kotlin / Android,Python & Data",
            compensationType = "EQUITY_BASED",
            compensationDetails = "15%–25% co-builder equity (12-month cliff, 4-year vesting) + revenue share post-pilot",
            country = "Malawi",
            city = "Lilongwe",
            workMode = "REMOTE",
            isSampleData = true,
            createdAt = now - dayMs * 4
        ),
        StartupOpportunityEntity(
            id = "opp_sample_2",
            founderId = "sample_founder_3",
            startupName = "JuaCold Hubs",
            roleCategory = "Graphic Designer",
            title = "Brand & Packaging Freelance Designer",
            description = "Create visual identity, farmer training posters, and mobile UI kit for cold-chain hubs.",
            requiredSkills = "UI/UX Design,Digital Marketing",
            compensationType = "PAID_FREELANCE",
            compensationDetails = "Fixed milestone fee: USD 450 for 3-week sprint",
            country = "Kenya",
            city = "Nairobi",
            workMode = "REMOTE",
            isSampleData = true,
            createdAt = now - dayMs * 2
        ),
        StartupOpportunityEntity(
            id = "opp_sample_3",
            founderId = "sample_founder_1",
            startupName = "Mzinda AgriLink",
            roleCategory = "Business Development Officer",
            title = "Cooperative Outreach Intern (Unpaid Learning Track)",
            description = "Shadow field leads during 6-week cooperative onboarding in Lilongwe. Mentorship & transport stipend.",
            requiredSkills = "Business Development,Agribusiness",
            compensationType = "UNPAID_INTERNSHIP",
            compensationDetails = "Unpaid 6-week learning internship · Transport & lunch allowance provided (MWK 45,000/mo)",
            country = "Malawi",
            city = "Lilongwe",
            workMode = "PHYSICAL",
            isSampleData = true,
            createdAt = now - dayMs
        )
    )
    opportunities.forEach { jobDao.insertStartupOpportunity(it) }

    // 6. Seed Sample Verification Submission & Report for Admin Dashboard testing
    adminDao.insertVerification(
        VerificationSubmissionEntity(
            id = "ver_sample_1",
            userId = "sample_talent_1",
            verificationType = "PORTFOLIO",
            evidenceText = "https://behance.net/sample-takondwa — 3 shipped Android & web case studies.",
            referenceContact = "Mentor: Chifundo M., Blantyre Hub",
            status = "PENDING",
            createdAt = now - dayMs
        )
    )
    adminDao.insertReport(
        ReportEntity(
            id = "rep_sample_1",
            reporterId = "sample_seeker_1",
            targetType = "JOB",
            targetId = "job_sample_2",
            reason = "Sample moderation ticket: verify employer registration certificate.",
            status = "OPEN",
            createdAt = now - dayMs / 2
        )
    )

    // 7. Seed a Sample Mutual Connection, Conversation & 14-Day Build Together Trial
    matchDao.insertMatchRecord(
        MatchRecordEntity(
            id = "mr_seed_1",
            fromUserId = "sample_founder_2",
            toUserId = "sample_founder_1",
            matchCategory = "COFOUNDER",
            decision = "INTERESTED",
            compatibilityScore = 91,
            reasonsSummary = "Complementary tech & agribusiness skills; shared AgriTech mission in Malawi.",
            createdAt = now - dayMs * 2
        )
    )
    matchDao.insertMatchRecord(
        MatchRecordEntity(
            id = "mr_seed_2",
            fromUserId = "sample_founder_1",
            toUserId = "sample_founder_2",
            matchCategory = "COFOUNDER",
            decision = "INTERESTED",
            compatibilityScore = 91,
            reasonsSummary = "Complementary tech & agribusiness skills; shared AgriTech mission in Malawi.",
            createdAt = now - dayMs * 2
        )
    )
    matchDao.insertConnectionRequest(
        ConnectionRequestEntity(
            id = "conn_seed_1",
            senderId = "sample_founder_1",
            receiverId = "sample_founder_2",
            contextType = "COFOUNDER",
            note = "Hi Tadziona! Would love to test building our AgriLink farmer prototype together.",
            status = "ACCEPTED",
            createdAt = now - dayMs * 2
        )
    )
    matchDao.insertConversation(
        ConversationEntity(
            id = "conv_seed_1",
            participantAId = "sample_founder_1",
            participantBId = "sample_founder_2",
            lastMessage = "Great! Let's kick off a 14-day Build Together sprint to validate farmer signups.",
            lastTimestamp = now - 3_600_000L
        )
    )
    matchDao.insertMessage(
        MessageEntity(
            id = "msg_seed_1",
            conversationId = "conv_seed_1",
            senderId = "sample_founder_1",
            body = "Hi Tadziona, your offline-first Android experience fits what Malawian cooperatives need.",
            timestamp = now - 7_200_000L,
            isRead = true
        )
    )
    matchDao.insertMessage(
        MessageEntity(
            id = "msg_seed_2",
            conversationId = "conv_seed_1",
            senderId = "sample_founder_2",
            body = "Great! Let's kick off a 14-day Build Together sprint to validate farmer signups.",
            timestamp = now - 3_600_000L,
            isRead = true
        )
    )
    val trialStart = now - dayMs * 3
    adminDao.insertTrial(
        TrialCollaborationEntity(
            id = "trial_seed_1",
            userAId = "sample_founder_1",
            userBId = "sample_founder_2",
            title = "AgriLink Farmer Onboarding Pilot",
            sharedProjectDescription = "Test a 14-day lightweight farmer registration & produce price board MVP in Lilongwe.",
            agreedResponsibilities = "Chisomo: Interview 15 cooperative leads & define pricing data. Tadziona: Build clickable Android prototype & offline sync schema.",
            startDateMillis = trialStart,
            endDateMillis = trialStart + dayMs * 14,
            notes = "Day 3 check-in: 6 cooperatives interviewed; UI wireframe approved.",
            status = "ACTIVE"
        )
    )
    adminDao.insertTrialTask(
        CollaborationTaskEntity("task_seed_1", "trial_seed_1", "Conduct 10 farmer pricing interviews", "Chisomo Banda", "Day 5", true)
    )
    adminDao.insertTrialTask(
        CollaborationTaskEntity("task_seed_2", "trial_seed_1", "Build offline-first farmer registration screen", "Tadziona Phiri", "Day 7", false)
    )
    adminDao.insertTrialTask(
        CollaborationTaskEntity("task_seed_3", "trial_seed_1", "Run field usability session with 5 buyers", "Both Founders", "Day 12", false)
    )
}
