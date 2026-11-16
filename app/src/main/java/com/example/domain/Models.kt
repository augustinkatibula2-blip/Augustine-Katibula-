package com.example.domain

import com.example.data.local.FounderPreferenceEntity
import com.example.data.local.JobListingEntity
import com.example.data.local.StartupOpportunityEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserProfileEntity

enum class AccountType(val code: String, val label: String, val subtitle: String) {
    FOUNDER("FOUNDER", "Entrepreneur / Founder", "Looking for a co-founder or building a startup team"),
    JOB_SEEKER("JOB_SEEKER", "Job Seeker", "Discovering roles matched to your skills & experience"),
    EMPLOYER("EMPLOYER", "Employer / Recruiter", "Publishing vacancies & reviewing matched candidates"),
    STUDENT("STUDENT", "Student / Graduate", "Exploring internships, teams & early career paths"),
    TALENT("TALENT", "Professional / Talent", "Joining startups as developer, designer, marketer, or operator");

    companion object {
        fun fromCode(code: String): AccountType = entries.find { it.code == code } ?: FOUNDER
    }
}

enum class VerificationTier(val code: String, val label: String) {
    UNVERIFIED("UNVERIFIED", "Unverified"),
    EMAIL_VERIFIED("EMAIL_VERIFIED", "Email Verified"),
    PHONE_VERIFIED("PHONE_VERIFIED", "Phone Verified"),
    EDUCATION_VERIFIED("EDUCATION_VERIFIED", "Education Verified"),
    PROFESSIONALLY_VERIFIED("PROFESSIONALLY_VERIFIED", "Professionally Verified");

    companion object {
        fun fromCode(code: String): VerificationTier = entries.find { it.code == code } ?: UNVERIFIED
    }
}

enum class CompensationType(val code: String, val label: String, val isPaid: Boolean) {
    PAID_POSITION("PAID_POSITION", "Paid Position", true),
    PAID_FREELANCE("PAID_FREELANCE", "Paid Freelance Project", true),
    EQUITY_BASED("EQUITY_BASED", "Equity-Based Position", false),
    UNPAID_INTERNSHIP("UNPAID_INTERNSHIP", "Unpaid Internship", false),
    VOLUNTEER("VOLUNTEER", "Volunteer Opportunity", false);

    companion object {
        fun fromCode(code: String): CompensationType = entries.find { it.code == code } ?: PAID_POSITION
    }
}

data class CompatibilityBreakdown(
    val score: Int,
    val passedEssentialFilter: Boolean,
    val filterFailureReason: String? = null,
    val complementarySkillsScore: Int,
    val sharedVisionScore: Int,
    val commitmentScore: Int,
    val goalsScore: Int,
    val workingStyleScore: Int,
    val locationCommScore: Int,
    val reasons: List<String>
)

data class CofounderCandidate(
    val user: UserEntity,
    val profile: UserProfileEntity,
    val preferences: FounderPreferenceEntity,
    val compatibility: CompatibilityBreakdown,
    val myDecision: String? = null,
    val theyInterestedInMe: Boolean = false
)

data class JobMatchItem(
    val job: JobListingEntity,
    val matchScore: Int,
    val reasons: List<String>,
    val applicationStatus: String? = null,
    val isSaved: Boolean = false
)

data class TalentMatchItem(
    val opportunity: StartupOpportunityEntity,
    val founderName: String,
    val matchScore: Int,
    val reasons: List<String>
)

/**
 * Extensible localization dictionary supporting English at launch,
 * structured so Chichewa (ny), Swahili (sw), and French (fr) can be seamlessly expanded.
 */
object LinkUpLocale {
    data class LanguageOption(val code: String, val displayName: String, val status: String)

    val supportedLanguages = listOf(
        LanguageOption("en", "English", "Active"),
        LanguageOption("ny", "Chichewa (Malawi)", "Preview Pack"),
        LanguageOption("sw", "Kiswahili (East Africa)", "Preview Pack"),
        LanguageOption("fr", "Français (West/Central Africa)", "Preview Pack")
    )

    fun heroHeadline(lang: String): String = when (lang) {
        "ny" -> "Pezani Anthu Oyenera. Pangani Zinthu Zodabwitsa."
        "sw" -> "Pata Watu Sahihi. Jenga Kitu cha Kipekee."
        "fr" -> "Trouvez vos partenaires. Bâtissez l'extraordinaire."
        else -> "Find Your People. Build Something Extraordinary."
    }

    fun tagline(lang: String): String = when (lang) {
        "ny" -> "Lumikizanani. Gwirizanani. Pangani."
        "sw" -> "Unganisha. Shirikiana. Jenga."
        "fr" -> "Connectez. Collaborez. Bâtissez."
        else -> "Connect. Collaborate. Build."
    }

    fun navLabel(key: String, lang: String): String = when (lang) {
        "ny" -> when (key) {
            "home" -> "Poyambira"
            "cofounders" -> "Abwenzi"
            "jobs" -> "Ntchito"
            "talent" -> "Luso"
            "messages" -> "Mauthenga"
            else -> key
        }
        "sw" -> when (key) {
            "home" -> "Nyumbani"
            "cofounders" -> "Waanzilishi"
            "jobs" -> "Ajira"
            "talent" -> "Vipaji"
            "messages" -> "Ujumbe"
            else -> key
        }
        "fr" -> when (key) {
            "home" -> "Accueil"
            "cofounders" -> "Cofondateurs"
            "jobs" -> "Emplois"
            "talent" -> "Talents"
            "messages" -> "Messages"
            else -> key
        }
        else -> when (key) {
            "home" -> "Home"
            "cofounders" -> "Founders"
            "jobs" -> "Jobs"
            "talent" -> "Talent"
            "messages" -> "Messages"
            else -> key
        }
    }
}

object FormatUtils {
    fun humanizeEnum(code: String): String {
        return code.lowercase()
            .split("_")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
    }

    fun parseTags(csv: String, limit: Int = 6): List<String> {
        return csv.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(limit)
    }
}

