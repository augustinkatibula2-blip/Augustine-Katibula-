package com.example.data.repository

import com.example.data.local.*
import com.example.domain.AccountType
import com.example.domain.VerificationTier
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LinkUpRepository(private val db: LinkUpDatabase) {
    private val userDao = db.userDao()
    private val jobDao = db.jobAndTalentDao()
    private val matchDao = db.matchingAndMessagingDao()
    private val adminDao = db.trustTrialAndAdminDao()

    val allUsers: Flow<List<UserEntity>> = userDao.observeAllUsers()
    val allProfiles: Flow<List<UserProfileEntity>> = userDao.observeAllProfiles()
    val allFounderPrefs: Flow<List<FounderPreferenceEntity>> = userDao.observeAllFounderPrefs()
    val allJobs: Flow<List<JobListingEntity>> = jobDao.observeAllJobs()
    val allApplications: Flow<List<JobApplicationEntity>> = jobDao.observeAllApplications()
    val allStartupOpportunities: Flow<List<StartupOpportunityEntity>> = jobDao.observeAllStartupOpportunities()
    val allMatchRecords: Flow<List<MatchRecordEntity>> = matchDao.observeAllMatchRecords()
    val allConnections: Flow<List<ConnectionRequestEntity>> = matchDao.observeAllConnectionRequests()
    val allConversations: Flow<List<ConversationEntity>> = matchDao.observeAllConversations()
    val allVerifications: Flow<List<VerificationSubmissionEntity>> = adminDao.observeAllVerifications()
    val allTrials: Flow<List<TrialCollaborationEntity>> = adminDao.observeAllTrials()
    val allTrialTasks: Flow<List<CollaborationTaskEntity>> = adminDao.observeAllTrialTasks()
    val allTrialReviews: Flow<List<CollaborationReviewEntity>> = adminDao.observeAllTrialReviews()
    val allReports: Flow<List<ReportEntity>> = adminDao.observeAllReports()
    val allCountries: Flow<List<CountryRegionEntity>> = adminDao.observeCountries()

    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> =
        matchDao.observeNotifications(userId)

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        matchDao.observeMessages(conversationId)

    fun observeEducation(userId: String): Flow<List<EducationEntity>> =
        userDao.observeEducation(userId)

    fun observeExperience(userId: String): Flow<List<ExperienceEntity>> =
        userDao.observeExperience(userId)

    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String,
        accountType: AccountType,
        country: String,
        city: String
    ): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (!SecurityUtils.isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (fullName.trim().length < 2) {
            return Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }

        val uid = "usr_${UUID.randomUUID().toString().take(8)}"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(password, salt)

        val user = UserEntity(
            id = uid,
            email = cleanEmail,
            passwordHash = hash,
            passwordSalt = salt,
            authProvider = "EMAIL",
            accountType = accountType.code,
            role = "USER",
            isSuspended = false,
            isSampleData = false
        )
        userDao.insertUser(user)

        val profile = UserProfileEntity(
            userId = uid,
            fullName = SecurityUtils.sanitizeInput(fullName, 80),
            headline = "${accountType.label} in $city, $country",
            country = country,
            city = city,
            verificationStatus = VerificationTier.UNVERIFIED.code,
            isSampleData = false
        )
        userDao.insertProfile(profile)

        userDao.insertFounderPref(
            FounderPreferenceEntity(
                userId = uid,
                industry = "AgriTech",
                hasBusinessIdea = accountType == AccountType.FOUNDER,
                commitmentType = "PART_TIME"
            )
        )

        matchDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = uid,
                title = "Welcome to LinkUp!",
                body = "Complete your profile and verification to boost match quality across Africa.",
                type = "SYSTEM"
            )
        )

        return Result.success(user)
    }

    suspend fun loginUser(email: String, password: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found for that email."))
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("This account has been suspended by an administrator."))
        }
        if (!SecurityUtils.verifyPassword(password, user.passwordSalt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }
        return Result.success(user)
    }

    suspend fun getUserById(userId: String): UserEntity? = userDao.getUserById(userId)

    suspend fun saveProfile(profile: UserProfileEntity) {
        userDao.insertProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveFounderPreferences(pref: FounderPreferenceEntity) {
        userDao.insertFounderPref(pref)
    }

    suspend fun addEducation(userId: String, institution: String, qualification: String, field: String, year: String) {
        userDao.insertEducation(
            EducationEntity(
                id = "edu_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                institution = SecurityUtils.sanitizeInput(institution, 100),
                qualification = SecurityUtils.sanitizeInput(qualification, 100),
                fieldOfStudy = SecurityUtils.sanitizeInput(field, 100),
                graduationYear = SecurityUtils.sanitizeInput(year, 12)
            )
        )
    }

    suspend fun addExperience(userId: String, role: String, org: String, duration: String, summary: String) {
        userDao.insertExperience(
            ExperienceEntity(
                id = "exp_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                roleTitle = SecurityUtils.sanitizeInput(role, 100),
                organization = SecurityUtils.sanitizeInput(org, 100),
                duration = SecurityUtils.sanitizeInput(duration, 40),
                summary = SecurityUtils.sanitizeInput(summary, 300)
            )
        )
    }

    /**
     * Records a swipe/browse decision. If both users express INTERESTED, creates a mutual connection,
     * opens a 1-on-1 conversation, and sends notifications. Returns true if mutual match occurred!
     */
    suspend fun recordCofounderDecision(
        fromUserId: String,
        toUserId: String,
        decision: String, // INTERESTED, PASS, SAVED
        score: Int,
        reasonsSummary: String
    ): Boolean {
        val existing = matchDao.getMatchRecord(fromUserId, toUserId, "COFOUNDER")
        val record = MatchRecordEntity(
            id = existing?.id ?: "mr_${UUID.randomUUID().toString().take(8)}",
            fromUserId = fromUserId,
            toUserId = toUserId,
            matchCategory = "COFOUNDER",
            decision = decision,
            compatibilityScore = score,
            reasonsSummary = reasonsSummary
        )
        matchDao.insertMatchRecord(record)

        if (decision == "INTERESTED") {
            val reciprocal = matchDao.getMatchRecord(toUserId, fromUserId, "COFOUNDER")
            if (reciprocal?.decision == "INTERESTED") {
                ensureMutualConnectionAndChat(fromUserId, toUserId, "COFOUNDER", "Mutual Co-Founder Match ($score% compatibility)!")
                return true
            } else {
                // Create a pending connection request so the other user can respond too
                matchDao.insertConnectionRequest(
                    ConnectionRequestEntity(
                        id = "conn_${UUID.randomUUID().toString().take(8)}",
                        senderId = fromUserId,
                        receiverId = toUserId,
                        contextType = "COFOUNDER",
                        note = "Interested in exploring a co-founder partnership ($score% compatibility).",
                        status = "PENDING"
                    )
                )
                matchDao.insertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        userId = toUserId,
                        title = "New Co-Founder Interest",
                        body = "Someone expressed interest in co-founding with you ($score% match).",
                        type = "MATCH"
                    )
                )
            }
        }
        return false
    }

    suspend fun sendConnectionRequest(
        senderId: String,
        receiverId: String,
        contextType: String,
        note: String
    ) {
        matchDao.insertConnectionRequest(
            ConnectionRequestEntity(
                id = "conn_${UUID.randomUUID().toString().take(8)}",
                senderId = senderId,
                receiverId = receiverId,
                contextType = contextType,
                note = SecurityUtils.sanitizeInput(note, 300),
                status = "PENDING"
            )
        )
        matchDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = receiverId,
                title = "New Connection Request",
                body = "You received a $contextType connection request.",
                type = "CONNECTION"
            )
        )
    }

    suspend fun respondToConnectionRequest(request: ConnectionRequestEntity, accept: Boolean) {
        val status = if (accept) "ACCEPTED" else "DECLINED"
        matchDao.updateConnectionStatus(request.id, status)
        if (accept) {
            ensureMutualConnectionAndChat(
                request.senderId,
                request.receiverId,
                request.contextType,
                "Your ${request.contextType.lowercase()} connection request was accepted!"
            )
        }
    }

    private suspend fun ensureMutualConnectionAndChat(
        userA: String,
        userB: String,
        contextType: String,
        notifMessage: String
    ) {
        val existingConv = matchDao.findConversationBetween(userA, userB)
        if (existingConv == null) {
            val convId = "conv_${UUID.randomUUID().toString().take(8)}"
            matchDao.insertConversation(
                ConversationEntity(
                    id = convId,
                    participantAId = userA,
                    participantBId = userB,
                    lastMessage = "Connected on LinkUp ($contextType). Say hello!",
                    lastTimestamp = System.currentTimeMillis()
                )
            )
        }
        listOf(userA, userB).forEach { uid ->
            matchDao.insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                    userId = uid,
                    title = "Mutual Match Established!",
                    body = notifMessage,
                    type = "MATCH"
                )
            )
        }
    }

    suspend fun sendMessage(conversation: ConversationEntity, senderId: String, body: String) {
        if (conversation.isBlocked) return
        val clean = SecurityUtils.sanitizeInput(body, 1000)
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        matchDao.insertMessage(
            MessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversation.id,
                senderId = senderId,
                body = clean,
                timestamp = now,
                isRead = false
            )
        )
        matchDao.updateConversationLastMessage(conversation.id, clean, now)
        val recipient = if (conversation.participantAId == senderId) conversation.participantBId else conversation.participantAId
        matchDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = recipient,
                title = "New Message",
                body = clean.take(60),
                type = "MESSAGE"
            )
        )
    }

    suspend fun markConversationRead(conversationId: String, readerId: String) {
        matchDao.markMessagesRead(conversationId, readerId)
    }

    suspend fun toggleBlockConversation(conversationId: String, block: Boolean, byUserId: String?) {
        matchDao.setConversationBlocked(conversationId, block, if (block) byUserId else null)
    }

    suspend fun publishJob(job: JobListingEntity) {
        jobDao.insertJob(job)
    }

    suspend fun applyOrSaveJob(
        jobId: String,
        applicantId: String,
        coverNote: String,
        matchScore: Int,
        matchReasons: String,
        saveOnly: Boolean,
        employerId: String,
        jobTitle: String
    ) {
        val status = if (saveOnly) "SAVED" else "SUBMITTED"
        jobDao.insertApplication(
            JobApplicationEntity(
                id = "app_${jobId}_$applicantId",
                jobId = jobId,
                applicantId = applicantId,
                coverNote = SecurityUtils.sanitizeInput(coverNote, 500),
                matchScore = matchScore,
                matchReasons = matchReasons,
                status = status,
                isSavedOnly = saveOnly
            )
        )
        if (!saveOnly) {
            matchDao.insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                    userId = employerId,
                    title = "New Job Applicant",
                    body = "A candidate ($matchScore% match) applied for $jobTitle.",
                    type = "JOB"
                )
            )
        }
    }

    suspend fun updateApplicationStatus(app: JobApplicationEntity, newStatus: String, jobTitle: String) {
        jobDao.updateApplicationStatus(app.id, newStatus, newStatus == "SAVED")
        matchDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = app.applicantId,
                title = "Application Update: $jobTitle",
                body = "Your application status is now $newStatus.",
                type = "JOB"
            )
        )
    }

    suspend fun publishStartupOpportunity(opp: StartupOpportunityEntity) {
        jobDao.insertStartupOpportunity(opp)
    }

    suspend fun createTrialCollaboration(
        userAId: String,
        userBId: String,
        title: String,
        description: String,
        responsibilities: String
    ) {
        val now = System.currentTimeMillis()
        val trial = TrialCollaborationEntity(
            id = "trial_${UUID.randomUUID().toString().take(8)}",
            userAId = userAId,
            userBId = userBId,
            title = SecurityUtils.sanitizeInput(title, 100),
            sharedProjectDescription = SecurityUtils.sanitizeInput(description, 600),
            agreedResponsibilities = SecurityUtils.sanitizeInput(responsibilities, 600),
            startDateMillis = now,
            endDateMillis = now + 14L * 86_400_000L,
            status = "ACTIVE"
        )
        adminDao.insertTrial(trial)
        matchDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = userBId,
                title = "New 14-Day Build Together Trial",
                body = "You were invited to collaborate on \"$title\".",
                type = "TRIAL"
            )
        )
    }

    suspend fun addTrialTask(trialId: String, title: String, assignee: String, dueLabel: String) {
        adminDao.insertTrialTask(
            CollaborationTaskEntity(
                id = "task_${UUID.randomUUID().toString().take(8)}",
                trialId = trialId,
                title = SecurityUtils.sanitizeInput(title, 120),
                assigneeName = SecurityUtils.sanitizeInput(assignee, 60),
                dueDateLabel = SecurityUtils.sanitizeInput(dueLabel, 30),
                isCompleted = false
            )
        )
    }

    suspend fun toggleTrialTask(taskId: String, completed: Boolean) {
        adminDao.setTaskCompleted(taskId, completed)
    }

    suspend fun updateTrialNotes(trial: TrialCollaborationEntity, notes: String) {
        adminDao.updateTrial(trial.copy(notes = SecurityUtils.sanitizeInput(notes, 800)))
    }

    suspend fun submitTrialReview(
        trial: TrialCollaborationEntity,
        reviewerId: String,
        metCommitments: Boolean,
        effectiveComm: Boolean,
        workedWell: Boolean,
        considerCofounder: Boolean,
        comments: String
    ) {
        adminDao.insertTrialReview(
            CollaborationReviewEntity(
                id = "rev_${trial.id}_$reviewerId",
                trialId = trial.id,
                reviewerId = reviewerId,
                metCommitments = metCommitments,
                effectiveCommunication = effectiveComm,
                workedWellTogether = workedWell,
                considerLongTermCofounder = considerCofounder,
                comments = SecurityUtils.sanitizeInput(comments, 400)
            )
        )
        adminDao.updateTrial(trial.copy(status = "COMPLETED"))
    }

    suspend fun submitVerification(
        userId: String,
        type: String,
        evidence: String,
        referenceContact: String
    ) {
        adminDao.insertVerification(
            VerificationSubmissionEntity(
                id = "ver_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                verificationType = type,
                evidenceText = SecurityUtils.sanitizeInput(evidence, 400),
                referenceContact = SecurityUtils.sanitizeInput(referenceContact, 120),
                status = "PENDING"
            )
        )
    }

    suspend fun reviewVerification(sub: VerificationSubmissionEntity, approve: Boolean, notes: String) {
        val status = if (approve) "APPROVED" else "REJECTED"
        adminDao.updateVerificationStatus(sub.id, status, notes)
        if (approve) {
            val profile = userDao.getProfile(sub.userId)
            if (profile != null) {
                val updated = when (sub.verificationType) {
                    "EMAIL" -> profile.copy(emailVerified = true, verificationStatus = VerificationTier.EMAIL_VERIFIED.code)
                    "PHONE" -> profile.copy(phoneVerified = true, verificationStatus = VerificationTier.PHONE_VERIFIED.code)
                    "EDUCATION" -> profile.copy(educationVerified = true, verificationStatus = VerificationTier.EDUCATION_VERIFIED.code)
                    else -> profile.copy(professionallyVerified = true, verificationStatus = VerificationTier.PROFESSIONALLY_VERIFIED.code)
                }
                userDao.insertProfile(updated)
            }
        }
        matchDao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = sub.userId,
                title = "Verification $status",
                body = "Your ${sub.verificationType.lowercase()} verification was $status.",
                type = "VERIFICATION"
            )
        )
    }

    suspend fun submitReport(reporterId: String, targetType: String, targetId: String, reason: String) {
        adminDao.insertReport(
            ReportEntity(
                id = "rep_${UUID.randomUUID().toString().take(8)}",
                reporterId = reporterId,
                targetType = targetType,
                targetId = targetId,
                reason = SecurityUtils.sanitizeInput(reason, 400),
                status = "OPEN"
            )
        )
    }

    suspend fun resolveReport(reportId: String, status: String) {
        adminDao.updateReportStatus(reportId, status)
    }

    suspend fun moderateJob(jobId: String, status: String) {
        jobDao.updateJobModeration(jobId, status)
    }

    suspend fun setUserSuspended(userId: String, suspended: Boolean) {
        userDao.setSuspended(userId, suspended)
    }

    suspend fun addOrToggleCountry(code: String, name: String, cities: String, active: Boolean) {
        adminDao.insertCountry(CountryRegionEntity(code.uppercase().take(3), name.trim(), cities.trim(), active))
    }

    suspend fun setCountryActive(code: String, active: Boolean) {
        adminDao.setCountryActive(code, active)
    }

    suspend fun markAllNotificationsRead(userId: String) {
        matchDao.markAllNotificationsRead(userId)
    }
}
