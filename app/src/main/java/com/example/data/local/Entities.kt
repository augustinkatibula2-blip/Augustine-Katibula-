package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val authProvider: String = "EMAIL", // EMAIL, GOOGLE, PHONE
    val accountType: String = "FOUNDER", // FOUNDER, JOB_SEEKER, EMPLOYER, STUDENT, TALENT
    val role: String = "USER", // USER, ADMIN
    val isSuspended: Boolean = false,
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val headline: String = "",
    val photoUri: String? = null,
    val country: String = "Malawi",
    val city: String = "Lilongwe",
    val phone: String = "",
    val shareContactWithMatches: Boolean = false,
    val educationLevel: String = "Self-Taught / Portfolio",
    val institution: String = "",
    val experienceSummary: String = "",
    val yearsExperience: Int = 0,
    val skills: String = "", // comma-separated
    val interests: String = "", // comma-separated
    val careerGoals: String = "",
    val portfolioUrl: String = "",
    val languages: String = "English",
    val workMode: String = "HYBRID", // REMOTE, HYBRID, PHYSICAL
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val educationVerified: Boolean = false,
    val professionallyVerified: Boolean = false,
    val verificationStatus: String = "UNVERIFIED",
    val isSampleData: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String
)

@Entity(
    tableName = "user_skills",
    primaryKeys = ["userId", "skillId"],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SkillEntity::class, parentColumns = ["id"], childColumns = ["skillId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("skillId")]
)
data class UserSkillEntity(
    val userId: String,
    val skillId: String,
    val level: String = "INTERMEDIATE"
)

@Entity(
    tableName = "education_records",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("userId")]
)
data class EducationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val institution: String,
    val qualification: String,
    val fieldOfStudy: String,
    val graduationYear: String
)

@Entity(
    tableName = "experience_records",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("userId")]
)
data class ExperienceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val roleTitle: String,
    val organization: String,
    val duration: String,
    val summary: String
)

@Entity(
    tableName = "founder_preferences",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class FounderPreferenceEntity(
    @PrimaryKey val userId: String,
    val industry: String = "AgriTech",
    val businessIdea: String = "",
    val hasBusinessIdea: Boolean = true,
    val skillsPossessed: String = "",
    val skillsNeeded: String = "",
    val preferredRoles: String = "",
    val preferredExperienceLevel: String = "Any",
    val preferredEducation: String = "Any (Skills & Portfolio First)",
    val weeklyHoursAvailable: Int = 25,
    val commitmentType: String = "PART_TIME", // FULL_TIME, PART_TIME, FLEXIBLE
    val startupStage: String = "IDEA", // IDEA, MVP, EARLY_TRACTION, SCALING
    val fundingSituation: String = "BOOTSTRAPPED", // BOOTSTRAPPED, GRANTS, PRE_SEED, SEED_READY
    val willingRemote: Boolean = true,
    val equityExpectation: String = "Equal Split (50/50)",
    val workingStyle: String = "Fast Iterative Builder"
)

@Entity(
    tableName = "job_listings",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["employerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("employerId")]
)
data class JobListingEntity(
    @PrimaryKey val id: String,
    val employerId: String,
    val title: String,
    val organization: String,
    val description: String,
    val requiredQualifications: String,
    val degreeRequired: Boolean = false,
    val requiredSkills: String,
    val experienceLevel: String,
    val minYearsExperience: Int = 0,
    val country: String = "Malawi",
    val city: String = "Lilongwe",
    val workMode: String = "HYBRID",
    val salaryRange: String = "Undisclosed",
    val employmentType: String = "FULL_TIME",
    val deadline: String = "Open until filled",
    val applicationInstructions: String = "Apply through LinkUp with your profile and brief cover note.",
    val moderationStatus: String = "APPROVED", // APPROVED, PENDING, FLAGGED, REMOVED
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "job_applications",
    foreignKeys = [
        ForeignKey(entity = JobListingEntity::class, parentColumns = ["id"], childColumns = ["jobId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["applicantId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("jobId"), Index("applicantId")]
)
data class JobApplicationEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val applicantId: String,
    val coverNote: String = "",
    val matchScore: Int = 0,
    val matchReasons: String = "",
    val status: String = "SUBMITTED", // SAVED, SUBMITTED, SHORTLISTED, INTERVIEW, OFFER, CLOSED
    val isSavedOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "startup_opportunities",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["founderId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("founderId")]
)
data class StartupOpportunityEntity(
    @PrimaryKey val id: String,
    val founderId: String,
    val startupName: String,
    val roleCategory: String,
    val title: String,
    val description: String,
    val requiredSkills: String,
    val compensationType: String, // PAID_POSITION, PAID_FREELANCE, EQUITY_BASED, UNPAID_INTERNSHIP, VOLUNTEER
    val compensationDetails: String,
    val country: String = "Malawi",
    val city: String = "Blantyre",
    val workMode: String = "REMOTE",
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "match_records",
    indices = [Index(value = ["fromUserId", "toUserId", "matchCategory"], unique = true)]
)
data class MatchRecordEntity(
    @PrimaryKey val id: String,
    val fromUserId: String,
    val toUserId: String,
    val matchCategory: String = "COFOUNDER", // COFOUNDER, TALENT
    val decision: String, // INTERESTED, PASS, SAVED
    val compatibilityScore: Int,
    val reasonsSummary: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "connection_requests",
    indices = [Index("senderId"), Index("receiverId")]
)
data class ConnectionRequestEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val contextType: String = "COFOUNDER", // COFOUNDER, JOB, TALENT
    val note: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED, BLOCKED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val participantAId: String,
    val participantBId: String,
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val blockedByUserId: String? = null
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(entity = ConversationEntity::class, parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(
    tableName = "notifications",
    indices = [Index("userId")]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String = "MATCH", // MATCH, CONNECTION, MESSAGE, JOB, TRIAL, VERIFICATION
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "verification_submissions",
    indices = [Index("userId")]
)
data class VerificationSubmissionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val verificationType: String, // EMAIL, PHONE, EDUCATION, PROFESSIONAL, PORTFOLIO, REFERENCE
    val evidenceText: String,
    val referenceContact: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val reviewerNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trial_collaborations")
data class TrialCollaborationEntity(
    @PrimaryKey val id: String,
    val userAId: String,
    val userBId: String,
    val title: String,
    val sharedProjectDescription: String,
    val agreedResponsibilities: String,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val notes: String = "",
    val status: String = "ACTIVE" // ACTIVE, COMPLETED
)

@Entity(
    tableName = "collaboration_tasks",
    foreignKeys = [
        ForeignKey(entity = TrialCollaborationEntity::class, parentColumns = ["id"], childColumns = ["trialId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("trialId")]
)
data class CollaborationTaskEntity(
    @PrimaryKey val id: String,
    val trialId: String,
    val title: String,
    val assigneeName: String,
    val dueDateLabel: String,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "collaboration_reviews",
    foreignKeys = [
        ForeignKey(entity = TrialCollaborationEntity::class, parentColumns = ["id"], childColumns = ["trialId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("trialId")]
)
data class CollaborationReviewEntity(
    @PrimaryKey val id: String,
    val trialId: String,
    val reviewerId: String,
    val metCommitments: Boolean,
    val effectiveCommunication: Boolean,
    val workedWellTogether: Boolean,
    val considerLongTermCofounder: Boolean,
    val comments: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val targetType: String, // USER, JOB, MESSAGE
    val targetId: String,
    val reason: String,
    val status: String = "OPEN", // OPEN, RESOLVED, DISMISSED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "country_regions")
data class CountryRegionEntity(
    @PrimaryKey val code: String,
    val name: String,
    val cities: String, // comma-separated
    val isActive: Boolean = true
)
