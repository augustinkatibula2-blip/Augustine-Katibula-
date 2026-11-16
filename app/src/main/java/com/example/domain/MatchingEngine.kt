package com.example.domain

import com.example.data.local.FounderPreferenceEntity
import com.example.data.local.JobListingEntity
import com.example.data.local.StartupOpportunityEntity
import com.example.data.local.UserProfileEntity
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Deterministic, two-way compatibility matching engine calculated strictly from real profile & preference data.
 * Weights:
 * - Complementary skills: 25%
 * - Shared startup interests & vision: 20%
 * - Commitment & availability: 20%
 * - Goals & expectations: 15%
 * - Working style & values: 10%
 * - Location & communication preferences: 10%
 */
object MatchingEngine {

    private fun tokenize(csv: String): Set<String> =
        csv.split(",", ";", "/")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun extractKeywords(text: String): Set<String> {
        val stopWords = setOf("the", "and", "for", "with", "that", "this", "from", "into", "across", "want", "build", "startup")
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 4 && it !in stopWords }
            .toSet()
    }

    private fun overlapRatio(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val matches = a.count { itemA -> b.any { itemB -> itemA == itemB || itemA.contains(itemB) || itemB.contains(itemA) } }
        return (matches.toDouble() / min(a.size, b.size).toDouble()).coerceIn(0.0, 1.0)
    }

    fun calculateCofounderCompatibility(
        me: UserProfileEntity,
        myPref: FounderPreferenceEntity,
        other: UserProfileEntity,
        otherPref: FounderPreferenceEntity
    ): CompatibilityBreakdown {
        val reasons = mutableListOf<String>()

        // 0. Essential Filter Check (e.g., if neither accepts remote and they are in different cities)
        val locationCompatible = myPref.willingRemote || otherPref.willingRemote ||
            (me.country.equals(other.country, true) && me.city.equals(other.city, true))

        if (!locationCompatible) {
            return CompatibilityBreakdown(
                score = 0,
                passedEssentialFilter = false,
                filterFailureReason = "Neither party is open to remote collaboration and cities differ (${me.city} vs ${other.city}).",
                complementarySkillsScore = 0,
                sharedVisionScore = 0,
                commitmentScore = 0,
                goalsScore = 0,
                workingStyleScore = 0,
                locationCommScore = 0,
                reasons = listOf("Filtered out: incompatible physical-only location requirements.")
            )
        }

        // 1. Complementary Skills (25%) — TWO-WAY check!
        val myPossessed = tokenize(myPref.skillsPossessed) + tokenize(me.skills)
        val myNeeded = tokenize(myPref.skillsNeeded)
        val otherPossessed = tokenize(otherPref.skillsPossessed) + tokenize(other.skills)
        val otherNeeded = tokenize(otherPref.skillsNeeded)

        val otherSatisfiesMe = if (myNeeded.isEmpty()) 0.6 else overlapRatio(myNeeded, otherPossessed)
        val iSatisfyOther = if (otherNeeded.isEmpty()) 0.6 else overlapRatio(otherNeeded, myPossessed)
        val complementaryRaw = ((otherSatisfiesMe + iSatisfyOther) / 2.0)
        val complementaryPoints = (complementaryRaw * 25.0).roundToInt()

        if (otherSatisfiesMe >= 0.5 && iSatisfyOther >= 0.5) {
            reasons.add("Your skills complement what they need, and their strengths cover your missing skills.")
        } else if (otherSatisfiesMe >= 0.4) {
            reasons.add("They bring key skills you are looking for (${otherPref.skillsPossessed.split(",").firstOrNull()?.trim().orEmpty()}).")
        }

        // 2. Shared Startup Interests & Vision (20%)
        val sameIndustry = myPref.industry.equals(otherPref.industry, true) ||
            myPref.industry.lowercase().contains(otherPref.industry.lowercase()) ||
            otherPref.industry.lowercase().contains(myPref.industry.lowercase())
        val myInterests = tokenize(me.interests) + extractKeywords(myPref.businessIdea) + tokenize(myPref.industry)
        val otherInterests = tokenize(other.interests) + extractKeywords(otherPref.businessIdea) + tokenize(otherPref.industry)
        val interestOverlap = overlapRatio(myInterests, otherInterests)
        val visionRatio = ((if (sameIndustry) 0.55 else 0.15) + (interestOverlap * 0.45)).coerceIn(0.0, 1.0)
        val visionPoints = (visionRatio * 20.0).roundToInt()

        if (sameIndustry) {
            reasons.add("You share a focus on ${otherPref.industry} innovation.")
        } else if (interestOverlap >= 0.35) {
            reasons.add("Strong alignment across your startup interests and problem space.")
        }

        // 3. Commitment & Availability (20%)
        val sameCommitment = myPref.commitmentType.equals(otherPref.commitmentType, true)
        val flexibleMatch = myPref.commitmentType == "FLEXIBLE" || otherPref.commitmentType == "FLEXIBLE"
        val hoursDiff = abs(myPref.weeklyHoursAvailable - otherPref.weeklyHoursAvailable)
        val hoursScore = (1.0 - (hoursDiff / 40.0)).coerceIn(0.2, 1.0)
        val commitBase = when {
            sameCommitment -> 1.0
            flexibleMatch -> 0.8
            else -> 0.45
        }
        val commitmentPoints = (((commitBase * 0.6) + (hoursScore * 0.4)) * 20.0).roundToInt()

        if (sameCommitment) {
            val label = otherPref.commitmentType.lowercase().replace("_", "-")
            reasons.add("Both of you are available for $label collaboration (~${otherPref.weeklyHoursAvailable} hrs/wk).")
        }

        // 4. Goals & Expectations (15%)
        val stageMatch = when {
            myPref.startupStage == otherPref.startupStage -> 1.0
            else -> 0.6
        }
        val equityMatch = if (myPref.equityExpectation.equals(otherPref.equityExpectation, true) ||
            myPref.equityExpectation.contains("Negotiable", true) ||
            otherPref.equityExpectation.contains("Negotiable", true)
        ) 1.0 else 0.65
        val oneHasIdea = myPref.hasBusinessIdea || otherPref.hasBusinessIdea
        val ideaBonus = if (oneHasIdea) 1.0 else 0.7
        val goalsPoints = (((stageMatch * 0.4) + (equityMatch * 0.35) + (ideaBonus * 0.25)) * 15.0).roundToInt()

        if (stageMatch >= 0.9 && equityMatch >= 0.9) {
            reasons.add("You have compatible startup stage and equity expectations.")
        }

        // 5. Working Style & Values (10%)
        val styleScore = if (myPref.workingStyle.equals(otherPref.workingStyle, true)) 1.0 else 0.65
        val workingStylePoints = (styleScore * 10.0).roundToInt()
        if (styleScore == 1.0) {
            reasons.add("Aligned working style (${otherPref.workingStyle}).")
        }

        // 6. Location & Communication Preferences (10%)
        val sharedLangs = tokenize(me.languages).intersect(tokenize(other.languages))
        val langRatio = if (sharedLangs.isNotEmpty()) 1.0 else 0.2
        val geoRatio = when {
            me.city.equals(other.city, true) && me.country.equals(other.country, true) -> 1.0
            me.country.equals(other.country, true) -> 0.85
            myPref.willingRemote && otherPref.willingRemote -> 0.75
            else -> 0.4
        }
        val locationPoints = (((langRatio * 0.5) + (geoRatio * 0.5)) * 10.0).roundToInt()
        if (sharedLangs.isNotEmpty() && me.country.equals(other.country, true)) {
            reasons.add("Both based in ${other.country} and communicate in ${sharedLangs.joinToString(", ") { it.replaceFirstChar(Char::uppercase) }}.")
        } else if (myPref.willingRemote && otherPref.willingRemote) {
            reasons.add("Both open to remote collaboration across borders.")
        }

        val total = (complementaryPoints + visionPoints + commitmentPoints + goalsPoints + workingStylePoints + locationPoints)
            .coerceIn(15, 99)

        if (reasons.isEmpty()) {
            reasons.add("Balanced baseline match on goals, remote readiness, and shared startup values.")
        }

        return CompatibilityBreakdown(
            score = total,
            passedEssentialFilter = true,
            complementarySkillsScore = complementaryPoints,
            sharedVisionScore = visionPoints,
            commitmentScore = commitmentPoints,
            goalsScore = goalsPoints,
            workingStyleScore = workingStylePoints,
            locationCommScore = locationPoints,
            reasons = reasons.distinct().take(4)
        )
    }

    fun calculateJobMatch(profile: UserProfileEntity, job: JobListingEntity): Pair<Int, List<String>> {
        val reasons = mutableListOf<String>()
        val mySkills = tokenize(profile.skills)
        val reqSkills = tokenize(job.requiredSkills)
        val skillRatio = if (reqSkills.isEmpty()) 0.7 else overlapRatio(reqSkills, mySkills)

        if (skillRatio >= 0.5) {
            val matched = reqSkills.filter { req -> mySkills.any { it.contains(req) || req.contains(it) } }
            reasons.add("Skills alignment: ${matched.take(3).joinToString(", ") { it.replaceFirstChar(Char::uppercase) }}.")
        }

        // Experience check (never excludes self-taught talent!)
        val expScore = if (profile.yearsExperience >= job.minYearsExperience) 1.0
        else (profile.yearsExperience.toDouble() / job.minYearsExperience.coerceAtLeast(1).toDouble()).coerceIn(0.3, 0.9)
        if (profile.yearsExperience >= job.minYearsExperience) {
            reasons.add("Meets ${job.minYearsExperience}+ years practical experience guideline.")
        }

        // Location & work mode
        val locScore = when {
            job.workMode.equals("REMOTE", true) -> 1.0
            profile.country.equals(job.country, true) && profile.city.equals(job.city, true) -> 1.0
            profile.country.equals(job.country, true) -> 0.8
            else -> 0.35
        }
        if (job.workMode.equals("REMOTE", true)) {
            reasons.add("Remote-friendly role compatible with your location.")
        } else if (profile.city.equals(job.city, true)) {
            reasons.add("Located in your city (${job.city}, ${job.country}).")
        }

        // Education: NEVER auto-reject or penalize non-graduates unless job.degreeRequired == true
        val eduScore = if (!job.degreeRequired) {
            reasons.add("Practical portfolio & skills prioritized (no formal degree gate).")
            1.0
        } else {
            val hasDegree = profile.educationLevel.contains("Bachelor", true) || profile.educationLevel.contains("Master", true)
            if (hasDegree) {
                reasons.add("Matches employer's formal degree requirement.")
                1.0
            } else 0.45
        }

        val interestMatch = overlapRatio(tokenize(profile.interests), extractKeywords(job.description + " " + job.title))
        val score = ((skillRatio * 45.0) + (expScore * 20.0) + (locScore * 15.0) + (eduScore * 10.0) + (interestMatch * 10.0))
            .roundToInt().coerceIn(20, 99)

        return score to reasons.take(4)
    }

    fun calculateTalentMatch(profile: UserProfileEntity, opp: StartupOpportunityEntity): Pair<Int, List<String>> {
        val reasons = mutableListOf<String>()
        val mySkills = tokenize(profile.skills)
        val reqSkills = tokenize(opp.requiredSkills)
        val skillRatio = if (reqSkills.isEmpty()) 0.65 else overlapRatio(reqSkills, mySkills)
        if (skillRatio >= 0.4) {
            reasons.add("Your skillset aligns with ${opp.roleCategory} needs.")
        }
        val locScore = if (opp.workMode.equals("REMOTE", true) || profile.country.equals(opp.country, true)) 1.0 else 0.45
        if (opp.workMode.equals("REMOTE", true)) {
            reasons.add("Open to remote startup collaboration.")
        } else if (profile.country.equals(opp.country, true)) {
            reasons.add("Local startup team in ${opp.country}.")
        }
        val interestRatio = overlapRatio(tokenize(profile.interests), extractKeywords(opp.description + " " + opp.startupName))
        if (interestRatio > 0.2) {
            reasons.add("Aligns with your career interests.")
        }
        val score = ((skillRatio * 55.0) + (locScore * 25.0) + (interestRatio * 20.0)).roundToInt().coerceIn(25, 98)
        return score to reasons.ifEmpty { listOf("Open African startup opportunity matching your profile.") }
    }

    fun calculateProfileCompletion(profile: UserProfileEntity?, pref: FounderPreferenceEntity?): Int {
        if (profile == null) return 0
        var pts = 0
        if (profile.fullName.isNotBlank()) pts += 15
        if (profile.headline.isNotBlank()) pts += 10
        if (profile.city.isNotBlank() && profile.country.isNotBlank()) pts += 10
        if (profile.skills.isNotBlank()) pts += 15
        if (profile.interests.isNotBlank()) pts += 10
        if (profile.experienceSummary.isNotBlank()) pts += 10
        if (profile.careerGoals.isNotBlank()) pts += 10
        if (profile.portfolioUrl.isNotBlank()) pts += 10
        if (pref != null && pref.skillsNeeded.isNotBlank()) pts += 10
        return pts.coerceIn(0, 100)
    }
}
