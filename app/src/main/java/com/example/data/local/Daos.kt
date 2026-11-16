package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isSuspended = :suspended WHERE id = :userId")
    suspend fun setSuspended(userId: String, suspended: Boolean)

    @Query("UPDATE users SET role = :role WHERE id = :userId")
    suspend fun setRole(userId: String, role: String)

    @Query("SELECT * FROM user_profiles")
    fun observeAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM founder_preferences")
    fun observeAllFounderPrefs(): Flow<List<FounderPreferenceEntity>>

    @Query("SELECT * FROM founder_preferences WHERE userId = :userId LIMIT 1")
    fun observeFounderPref(userId: String): Flow<FounderPreferenceEntity?>

    @Query("SELECT * FROM founder_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getFounderPref(userId: String): FounderPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFounderPref(pref: FounderPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<SkillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSkills(userSkills: List<UserSkillEntity>)

    @Query("DELETE FROM user_skills WHERE userId = :userId")
    suspend fun clearUserSkills(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEducation(record: EducationEntity)

    @Query("SELECT * FROM education_records WHERE userId = :userId")
    fun observeEducation(userId: String): Flow<List<EducationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperience(record: ExperienceEntity)

    @Query("SELECT * FROM experience_records WHERE userId = :userId")
    fun observeExperience(userId: String): Flow<List<ExperienceEntity>>
}

@Dao
interface JobAndTalentDao {
    @Query("SELECT * FROM job_listings ORDER BY createdAt DESC")
    fun observeAllJobs(): Flow<List<JobListingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobListingEntity)

    @Query("UPDATE job_listings SET moderationStatus = :status WHERE id = :jobId")
    suspend fun updateJobModeration(jobId: String, status: String)

    @Query("DELETE FROM job_listings WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("SELECT * FROM job_applications ORDER BY createdAt DESC")
    fun observeAllApplications(): Flow<List<JobApplicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(app: JobApplicationEntity)

    @Query("UPDATE job_applications SET status = :status, isSavedOnly = :savedOnly WHERE id = :appId")
    suspend fun updateApplicationStatus(appId: String, status: String, savedOnly: Boolean)

    @Query("SELECT * FROM startup_opportunities ORDER BY createdAt DESC")
    fun observeAllStartupOpportunities(): Flow<List<StartupOpportunityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStartupOpportunity(opp: StartupOpportunityEntity)

    @Query("DELETE FROM startup_opportunities WHERE id = :id")
    suspend fun deleteStartupOpportunity(id: String)
}

@Dao
interface MatchingAndMessagingDao {
    @Query("SELECT * FROM match_records WHERE fromUserId = :userId")
    fun observeMyMatchRecords(userId: String): Flow<List<MatchRecordEntity>>

    @Query("SELECT * FROM match_records")
    fun observeAllMatchRecords(): Flow<List<MatchRecordEntity>>

    @Query("SELECT * FROM match_records WHERE fromUserId = :fromId AND toUserId = :toId AND matchCategory = :category LIMIT 1")
    suspend fun getMatchRecord(fromId: String, toId: String, category: String): MatchRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchRecord(record: MatchRecordEntity)

    @Query("SELECT * FROM connection_requests ORDER BY createdAt DESC")
    fun observeAllConnectionRequests(): Flow<List<ConnectionRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnectionRequest(req: ConnectionRequestEntity)

    @Query("UPDATE connection_requests SET status = :status WHERE id = :id")
    suspend fun updateConnectionStatus(id: String, status: String)

    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    fun observeAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE (participantAId = :u1 AND participantBId = :u2) OR (participantAId = :u2 AND participantBId = :u1) LIMIT 1")
    suspend fun findConversationBetween(u1: String, u2: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conv: ConversationEntity)

    @Query("UPDATE conversations SET lastMessage = :lastMsg, lastTimestamp = :ts WHERE id = :convId")
    suspend fun updateConversationLastMessage(convId: String, lastMsg: String, ts: Long)

    @Query("UPDATE conversations SET isBlocked = :blocked, blockedByUserId = :byUser WHERE id = :convId")
    suspend fun setConversationBlocked(convId: String, blocked: Boolean, byUser: String?)

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun observeMessages(convId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :convId AND senderId != :readerId")
    suspend fun markMessagesRead(convId: String, readerId: String)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notif: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)
}

@Dao
interface TrustTrialAndAdminDao {
    @Query("SELECT * FROM verification_submissions ORDER BY createdAt DESC")
    fun observeAllVerifications(): Flow<List<VerificationSubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(sub: VerificationSubmissionEntity)

    @Query("UPDATE verification_submissions SET status = :status, reviewerNotes = :notes WHERE id = :id")
    suspend fun updateVerificationStatus(id: String, status: String, notes: String)

    @Query("SELECT * FROM trial_collaborations ORDER BY startDateMillis DESC")
    fun observeAllTrials(): Flow<List<TrialCollaborationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrial(trial: TrialCollaborationEntity)

    @Update
    suspend fun updateTrial(trial: TrialCollaborationEntity)

    @Query("SELECT * FROM collaboration_tasks ORDER BY id ASC")
    fun observeAllTrialTasks(): Flow<List<CollaborationTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrialTask(task: CollaborationTaskEntity)

    @Query("UPDATE collaboration_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: String, completed: Boolean)

    @Query("SELECT * FROM collaboration_reviews")
    fun observeAllTrialReviews(): Flow<List<CollaborationReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrialReview(review: CollaborationReviewEntity)

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String)

    @Query("SELECT * FROM country_regions ORDER BY name ASC")
    fun observeCountries(): Flow<List<CountryRegionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountry(country: CountryRegionEntity)

    @Query("UPDATE country_regions SET isActive = :active WHERE code = :code")
    suspend fun setCountryActive(code: String, active: Boolean)
}
