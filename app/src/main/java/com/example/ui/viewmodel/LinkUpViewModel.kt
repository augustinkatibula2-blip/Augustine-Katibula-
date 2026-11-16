package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.LinkUpRepository
import com.example.data.repository.SessionManager
import com.example.domain.*
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class LinkUpUiState(
    val isLoading: Boolean = true,
    val currentUser: UserEntity? = null,
    val currentProfile: UserProfileEntity? = null,
    val currentFounderPrefs: FounderPreferenceEntity? = null,
    val profileCompletion: Int = 0,
    val allUsers: List<UserEntity> = emptyList(),
    val allProfiles: List<UserProfileEntity> = emptyList(),
    val allFounderPrefs: List<FounderPreferenceEntity> = emptyList(),
    val cofounderCandidates: List<CofounderCandidate> = emptyList(),
    val jobMatches: List<JobMatchItem> = emptyList(),
    val allJobs: List<JobListingEntity> = emptyList(),
    val allApplications: List<JobApplicationEntity> = emptyList(),
    val talentMatches: List<TalentMatchItem> = emptyList(),
    val allStartupOpportunities: List<StartupOpportunityEntity> = emptyList(),
    val myConnections: List<ConnectionRequestEntity> = emptyList(),
    val myConversations: List<ConversationEntity> = emptyList(),
    val activeConversationMessages: List<MessageEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val myEducation: List<EducationEntity> = emptyList(),
    val myExperience: List<ExperienceEntity> = emptyList(),
    val allVerifications: List<VerificationSubmissionEntity> = emptyList(),
    val myTrials: List<TrialCollaborationEntity> = emptyList(),
    val allTrialTasks: List<CollaborationTaskEntity> = emptyList(),
    val allTrialReviews: List<CollaborationReviewEntity> = emptyList(),
    val allReports: List<ReportEntity> = emptyList(),
    val countries: List<CountryRegionEntity> = emptyList(),
    val lowDataMode: Boolean = false,
    val languageCode: String = "en",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val hasSeenIntro: Boolean = true,
    val isRefreshing: Boolean = false,
    val authError: String? = null,
    val snackbarMessage: String? = null,
    val mutualMatchName: String? = null
)

class LinkUpViewModel(
    private val repository: LinkUpRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _authError = MutableStateFlow<String?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _mutualMatchName = MutableStateFlow<String?>(null)
    private val _activeConversationId = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    private data class CoreBundle(
        val userId: String?,
        val users: List<UserEntity>,
        val profiles: List<UserProfileEntity>,
        val prefs: List<FounderPreferenceEntity>,
        val matchRecords: List<MatchRecordEntity>
    )

    private data class WorkBundle(
        val jobs: List<JobListingEntity>,
        val applications: List<JobApplicationEntity>,
        val opportunities: List<StartupOpportunityEntity>,
        val connections: List<ConnectionRequestEntity>,
        val conversations: List<ConversationEntity>
    )

    private data class AdminBundle(
        val verifications: List<VerificationSubmissionEntity>,
        val trials: List<TrialCollaborationEntity>,
        val tasks: List<CollaborationTaskEntity>,
        val reviews: List<CollaborationReviewEntity>,
        val reports: List<ReportEntity>,
        val countries: List<CountryRegionEntity>,
        val lowData: Boolean,
        val lang: String,
        val themeMode: AppThemeMode,
        val hasSeenIntro: Boolean,
        val isRefreshing: Boolean
    )

    private val coreFlow = combine(
        sessionManager.currentUserId,
        repository.allUsers,
        repository.allProfiles,
        repository.allFounderPrefs,
        repository.allMatchRecords
    ) { uid, users, profiles, prefs, records ->
        CoreBundle(uid, users, profiles, prefs, records)
    }

    private val workFlow = combine(
        repository.allJobs,
        repository.allApplications,
        repository.allStartupOpportunities,
        repository.allConnections,
        repository.allConversations
    ) { jobs, apps, opps, conns, convs ->
        WorkBundle(jobs, apps, opps, conns, convs)
    }

    private val adminFlow = combine(
        repository.allVerifications,
        repository.allTrials,
        repository.allTrialTasks,
        repository.allTrialReviews,
        repository.allReports,
        repository.allCountries,
        sessionManager.lowDataMode,
        sessionManager.languageCode,
        sessionManager.themeMode,
        sessionManager.hasSeenIntro,
        _isRefreshing
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        AdminBundle(
            verifications = arr[0] as List<VerificationSubmissionEntity>,
            trials = arr[1] as List<TrialCollaborationEntity>,
            tasks = arr[2] as List<CollaborationTaskEntity>,
            reviews = arr[3] as List<CollaborationReviewEntity>,
            reports = arr[4] as List<ReportEntity>,
            countries = arr[5] as List<CountryRegionEntity>,
            lowData = arr[6] as Boolean,
            lang = arr[7] as String,
            themeMode = AppThemeMode.fromCode(arr[8] as String),
            hasSeenIntro = arr[9] as Boolean,
            isRefreshing = arr[10] as Boolean
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val userScopedFlow = sessionManager.currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(Triple(emptyList<NotificationEntity>(), emptyList<EducationEntity>(), emptyList<ExperienceEntity>()))
        } else {
            combine(
                repository.observeNotifications(uid),
                repository.observeEducation(uid),
                repository.observeExperience(uid)
            ) { notifs, edu, exp -> Triple(notifs, edu, exp) }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val activeMessagesFlow = _activeConversationId.flatMapLatest { convId ->
        if (convId.isNullOrBlank()) flowOf(emptyList()) else repository.observeMessages(convId)
    }

    val uiState: StateFlow<LinkUpUiState> = combine(
        coreFlow,
        workFlow,
        adminFlow,
        userScopedFlow,
        activeMessagesFlow,
        _authError,
        _snackbarMessage,
        _mutualMatchName
    ) { values ->
        val core = values[0] as CoreBundle
        val work = values[1] as WorkBundle
        val admin = values[2] as AdminBundle
        @Suppress("UNCHECKED_CAST")
        val scoped = values[3] as Triple<List<NotificationEntity>, List<EducationEntity>, List<ExperienceEntity>>
        @Suppress("UNCHECKED_CAST")
        val activeMessages = values[4] as List<MessageEntity>
        val authErr = values[5] as String?
        val snack = values[6] as String?
        val mutualName = values[7] as String?

        val uid = core.userId
        val currentUser = core.users.find { it.id == uid }
        val currentProfile = core.profiles.find { it.userId == uid }
        val currentPrefs = core.prefs.find { it.userId == uid }

        val blockedPartnerIds = if (uid == null) emptySet() else {
            work.conversations.filter { it.isBlocked && (it.participantAId == uid || it.participantBId == uid) }
                .map { if (it.participantAId == uid) it.participantBId else it.participantAId }
                .toSet()
        }

        val candidates = if (currentProfile != null && currentPrefs != null) {
            core.users.filter { it.id != uid && !it.isSuspended && it.id !in blockedPartnerIds }
                .mapNotNull { otherUser ->
                    val otherProfile = core.profiles.find { it.userId == otherUser.id } ?: return@mapNotNull null
                    val otherPref = core.prefs.find { it.userId == otherUser.id } ?: return@mapNotNull null
                    val compatibility = MatchingEngine.calculateCofounderCompatibility(
                        currentProfile, currentPrefs, otherProfile, otherPref
                    )
                    val myRecord = core.matchRecords.find {
                        it.fromUserId == uid && it.toUserId == otherUser.id && it.matchCategory == "COFOUNDER"
                    }
                    val theirRecord = core.matchRecords.find {
                        it.fromUserId == otherUser.id && it.toUserId == uid && it.matchCategory == "COFOUNDER"
                    }
                    CofounderCandidate(
                        user = otherUser,
                        profile = otherProfile,
                        preferences = otherPref,
                        compatibility = compatibility,
                        myDecision = myRecord?.decision,
                        theyInterestedInMe = theirRecord?.decision == "INTERESTED"
                    )
                }
                .sortedByDescending { it.compatibility.score }
        } else {
            val guestBaselineProfile = UserProfileEntity(
                userId = "guest",
                fullName = "Guest",
                country = "Malawi",
                city = "Lilongwe",
                skills = "Product Management,Software Engineering,Marketing,Business Development",
                interests = "AgriTech,FinTech,Clean Energy,EdTech",
                languages = "English,Chichewa"
            )
            val guestBaselinePrefs = FounderPreferenceEntity(
                userId = "guest",
                industry = "Technology",
                skillsPossessed = guestBaselineProfile.skills,
                skillsNeeded = "Engineering,Growth,Operations,Design",
                commitmentType = "FLEXIBLE",
                weeklyHoursAvailable = 25,
                willingRemote = true
            )
            core.users.filter { !it.isSuspended && it.role != "ADMIN" }
                .mapNotNull { otherUser ->
                    val otherProfile = core.profiles.find { it.userId == otherUser.id } ?: return@mapNotNull null
                    val otherPref = core.prefs.find { it.userId == otherUser.id } ?: return@mapNotNull null
                    val compatibility = MatchingEngine.calculateCofounderCompatibility(
                        guestBaselineProfile, guestBaselinePrefs, otherProfile, otherPref
                    )
                    CofounderCandidate(
                        user = otherUser,
                        profile = otherProfile,
                        preferences = otherPref,
                        compatibility = compatibility
                    )
                }
                .sortedByDescending { it.compatibility.score }
        }

        val approvedJobs = work.jobs.filter { it.moderationStatus == "APPROVED" }
        val jobMatches = if (currentProfile != null && uid != null) {
            approvedJobs.map { job ->
                val (score, reasons) = MatchingEngine.calculateJobMatch(currentProfile, job)
                val app = work.applications.find { it.jobId == job.id && it.applicantId == uid }
                JobMatchItem(
                    job = job,
                    matchScore = score,
                    reasons = reasons,
                    applicationStatus = app?.status,
                    isSaved = app?.isSavedOnly == true
                )
            }.sortedByDescending { it.matchScore }
        } else {
            approvedJobs.map { JobMatchItem(it, 75, listOf("Sign in to calculate personalized compatibility.")) }
        }

        val talentMatches = if (currentProfile != null) {
            work.opportunities.map { opp ->
                val (score, reasons) = MatchingEngine.calculateTalentMatch(currentProfile, opp)
                val founderName = core.profiles.find { it.userId == opp.founderId }?.fullName ?: "Startup Founder"
                TalentMatchItem(opp, founderName, score, reasons)
            }.sortedByDescending { it.matchScore }
        } else {
            work.opportunities.map { opp ->
                val founderName = core.profiles.find { it.userId == opp.founderId }?.fullName ?: "Startup Founder"
                TalentMatchItem(opp, founderName, 75, listOf("Sign in to view personalized fit."))
            }
        }

        val myConns = if (uid == null) emptyList() else work.connections.filter {
            it.senderId == uid || it.receiverId == uid
        }
        val myConvs = if (uid == null) emptyList() else work.conversations.filter {
            it.participantAId == uid || it.participantBId == uid
        }
        val myTrials = if (uid == null) emptyList() else admin.trials.filter {
            it.userAId == uid || it.userBId == uid
        }

        LinkUpUiState(
            isLoading = false,
            currentUser = currentUser,
            currentProfile = currentProfile,
            currentFounderPrefs = currentPrefs,
            profileCompletion = MatchingEngine.calculateProfileCompletion(currentProfile, currentPrefs),
            allUsers = core.users,
            allProfiles = core.profiles,
            allFounderPrefs = core.prefs,
            cofounderCandidates = candidates,
            jobMatches = jobMatches,
            allJobs = work.jobs,
            allApplications = work.applications,
            talentMatches = talentMatches,
            allStartupOpportunities = work.opportunities,
            myConnections = myConns,
            myConversations = myConvs,
            activeConversationMessages = activeMessages,
            notifications = scoped.first,
            myEducation = scoped.second,
            myExperience = scoped.third,
            allVerifications = admin.verifications,
            myTrials = myTrials,
            allTrialTasks = admin.tasks,
            allTrialReviews = admin.reviews,
            allReports = admin.reports,
            countries = admin.countries,
            lowDataMode = admin.lowData,
            languageCode = admin.lang,
            themeMode = admin.themeMode,
            hasSeenIntro = admin.hasSeenIntro,
            isRefreshing = admin.isRefreshing,
            authError = authErr,
            snackbarMessage = snack,
            mutualMatchName = mutualName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LinkUpUiState(isLoading = true)
    )

    fun clearAuthError() { _authError.value = null }
    fun clearSnackbar() { _snackbarMessage.value = null }
    fun clearMutualMatch() { _mutualMatchName.value = null }

    fun register(
        email: String,
        password: String,
        fullName: String,
        accountType: AccountType,
        country: String,
        city: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authError.value = null
            val res = repository.registerUser(email, password, fullName, accountType, country, city)
            res.onSuccess { user ->
                sessionManager.setCurrentUserId(user.id)
                _snackbarMessage.value = "Account created! Complete your onboarding profile."
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Registration failed"
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val res = repository.loginUser(email, password)
            res.onSuccess { user ->
                sessionManager.setCurrentUserId(user.id)
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Login failed"
            }
        }
    }

    fun loginDemoAccount(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserById(userId)
            if (user != null && !user.isSuspended) {
                sessionManager.setCurrentUserId(user.id)
                _snackbarMessage.value = "Signed in as ${user.email}"
                onSuccess()
            } else {
                _authError.value = "Account unavailable or suspended."
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            _activeConversationId.value = null
            sessionManager.setCurrentUserId(null)
            onDone()
        }
    }

    fun saveProfile(profile: UserProfileEntity, notifyMessage: String = "Profile saved") {
        viewModelScope.launch {
            repository.saveProfile(profile)
            _snackbarMessage.value = notifyMessage
        }
    }

    fun saveFounderPreferences(pref: FounderPreferenceEntity) {
        viewModelScope.launch {
            repository.saveFounderPreferences(pref)
            _snackbarMessage.value = "Co-founder preferences updated & matches recalculated"
        }
    }

    fun addEducation(institution: String, qualification: String, field: String, year: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.addEducation(uid, institution, qualification, field, year)
            _snackbarMessage.value = "Education record added"
        }
    }

    fun addExperience(role: String, org: String, duration: String, summary: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.addExperience(uid, role, org, duration, summary)
            _snackbarMessage.value = "Experience record added"
        }
    }

    fun swipeCofounder(candidate: CofounderCandidate, decision: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val isMutual = repository.recordCofounderDecision(
                fromUserId = uid,
                toUserId = candidate.user.id,
                decision = decision,
                score = candidate.compatibility.score,
                reasonsSummary = candidate.compatibility.reasons.joinToString(" ")
            )
            if (isMutual) {
                _mutualMatchName.value = candidate.profile.fullName
            } else {
                _snackbarMessage.value = when (decision) {
                    "INTERESTED" -> "Interest sent to ${candidate.profile.fullName}"
                    "SAVED" -> "Saved ${candidate.profile.fullName} for later"
                    else -> "Passed on ${candidate.profile.fullName}"
                }
            }
        }
    }

    fun sendConnectionRequest(receiverId: String, contextType: String, note: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.sendConnectionRequest(uid, receiverId, contextType, note)
            _snackbarMessage.value = "Connection request sent"
        }
    }

    fun respondToConnection(request: ConnectionRequestEntity, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToConnectionRequest(request, accept)
            _snackbarMessage.value = if (accept) "Connection accepted! Conversation opened." else "Request declined."
        }
    }

    fun selectConversation(conversationId: String?) {
        _activeConversationId.value = conversationId
        val uid = uiState.value.currentUser?.id
        if (!conversationId.isNullOrBlank() && !uid.isNullOrBlank()) {
            viewModelScope.launch {
                repository.markConversationRead(conversationId, uid)
            }
        }
    }

    fun sendMessage(conversation: ConversationEntity, body: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.sendMessage(conversation, uid, body)
        }
    }

    fun toggleBlockConversation(conversation: ConversationEntity) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val block = !conversation.isBlocked
            repository.toggleBlockConversation(conversation.id, block, uid)
            _snackbarMessage.value = if (block) "User blocked in this conversation." else "User unblocked."
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.submitReport(uid, targetType, targetId, reason)
            _snackbarMessage.value = "Report submitted for admin moderation review."
        }
    }

    fun publishJob(
        title: String,
        organization: String,
        description: String,
        requiredQualifications: String,
        degreeRequired: Boolean,
        requiredSkills: String,
        experienceLevel: String,
        minYears: Int,
        country: String,
        city: String,
        workMode: String,
        salaryRange: String,
        employmentType: String,
        deadline: String,
        instructions: String
    ) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.publishJob(
                JobListingEntity(
                    id = "job_${UUID.randomUUID().toString().take(8)}",
                    employerId = uid,
                    title = title.trim(),
                    organization = organization.trim(),
                    description = description.trim(),
                    requiredQualifications = requiredQualifications.trim(),
                    degreeRequired = degreeRequired,
                    requiredSkills = requiredSkills.trim(),
                    experienceLevel = experienceLevel,
                    minYearsExperience = minYears,
                    country = country,
                    city = city,
                    workMode = workMode,
                    salaryRange = salaryRange.ifBlank { "Undisclosed" },
                    employmentType = employmentType,
                    deadline = deadline.ifBlank { "Open until filled" },
                    applicationInstructions = instructions.ifBlank { "Apply via LinkUp profile." },
                    moderationStatus = "APPROVED",
                    isSampleData = false
                )
            )
            _snackbarMessage.value = "Vacancy published to LinkUp Jobs!"
        }
    }

    fun applyOrSaveJob(item: JobMatchItem, coverNote: String, saveOnly: Boolean) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.applyOrSaveJob(
                jobId = item.job.id,
                applicantId = uid,
                coverNote = coverNote,
                matchScore = item.matchScore,
                matchReasons = item.reasons.joinToString(" "),
                saveOnly = saveOnly,
                employerId = item.job.employerId,
                jobTitle = item.job.title
            )
            _snackbarMessage.value = if (saveOnly) "Saved job: ${item.job.title}" else "Application submitted to ${item.job.organization}"
        }
    }

    fun updateApplicationStatus(app: JobApplicationEntity, newStatus: String, jobTitle: String) {
        viewModelScope.launch {
            repository.updateApplicationStatus(app, newStatus, jobTitle)
            _snackbarMessage.value = "Applicant marked $newStatus"
        }
    }

    fun publishStartupOpportunity(
        startupName: String,
        roleCategory: String,
        title: String,
        description: String,
        requiredSkills: String,
        compensationType: CompensationType,
        compensationDetails: String,
        country: String,
        city: String,
        workMode: String
    ) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.publishStartupOpportunity(
                StartupOpportunityEntity(
                    id = "opp_${UUID.randomUUID().toString().take(8)}",
                    founderId = uid,
                    startupName = startupName.trim(),
                    roleCategory = roleCategory,
                    title = title.trim(),
                    description = description.trim(),
                    requiredSkills = requiredSkills.trim(),
                    compensationType = compensationType.code,
                    compensationDetails = compensationDetails.trim(),
                    country = country,
                    city = city,
                    workMode = workMode,
                    isSampleData = false
                )
            )
            _snackbarMessage.value = "Startup role published!"
        }
    }

    fun createTrial(partnerId: String, title: String, description: String, responsibilities: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.createTrialCollaboration(uid, partnerId, title, description, responsibilities)
            _snackbarMessage.value = "14-day Build Together trial started!"
        }
    }

    fun addTrialTask(trialId: String, title: String, assignee: String, dueLabel: String) {
        viewModelScope.launch {
            repository.addTrialTask(trialId, title, assignee, dueLabel)
        }
    }

    fun toggleTrialTask(task: CollaborationTaskEntity) {
        viewModelScope.launch {
            repository.toggleTrialTask(task.id, !task.isCompleted)
        }
    }

    fun updateTrialNotes(trial: TrialCollaborationEntity, notes: String) {
        viewModelScope.launch {
            repository.updateTrialNotes(trial, notes)
            _snackbarMessage.value = "Collaboration notes saved"
        }
    }

    fun submitTrialReview(
        trial: TrialCollaborationEntity,
        metCommitments: Boolean,
        effectiveComm: Boolean,
        workedWell: Boolean,
        considerCofounder: Boolean,
        comments: String
    ) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.submitTrialReview(trial, uid, metCommitments, effectiveComm, workedWell, considerCofounder, comments)
            _snackbarMessage.value = "Trial review submitted!"
        }
    }

    fun submitVerification(type: String, evidence: String, referenceContact: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.submitVerification(uid, type, evidence, referenceContact)
            _snackbarMessage.value = "Verification submitted for admin review."
        }
    }

    fun reviewVerification(sub: VerificationSubmissionEntity, approve: Boolean, notes: String) {
        viewModelScope.launch {
            repository.reviewVerification(sub, approve, notes)
            _snackbarMessage.value = if (approve) "Verification approved" else "Verification rejected"
        }
    }

    fun resolveReport(reportId: String, status: String) {
        viewModelScope.launch {
            repository.resolveReport(reportId, status)
            _snackbarMessage.value = "Report marked $status"
        }
    }

    fun moderateJob(jobId: String, status: String) {
        viewModelScope.launch {
            repository.moderateJob(jobId, status)
            _snackbarMessage.value = "Job listing updated to $status"
        }
    }

    fun toggleUserSuspension(user: UserEntity) {
        viewModelScope.launch {
            repository.setUserSuspended(user.id, !user.isSuspended)
            _snackbarMessage.value = if (!user.isSuspended) "User suspended" else "User reactivated"
        }
    }

    fun addOrUpdateCountry(code: String, name: String, cities: String) {
        viewModelScope.launch {
            repository.addOrToggleCountry(code, name, cities, true)
            _snackbarMessage.value = "Country region saved ($name)"
        }
    }

    fun toggleCountryActive(country: CountryRegionEntity) {
        viewModelScope.launch {
            repository.setCountryActive(country.code, !country.isActive)
        }
    }

    fun markNotificationsRead() {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(uid)
        }
    }

    fun setLowDataMode(enabled: Boolean) {
        viewModelScope.launch { sessionManager.setLowDataMode(enabled) }
    }

    fun setLanguageCode(code: String) {
        viewModelScope.launch { sessionManager.setLanguageCode(code) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { sessionManager.setThemeMode(mode.code) }
    }

    fun toggleLightDark(currentlyDark: Boolean) {
        val next = if (currentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(next)
    }

    fun completeIntro() {
        viewModelScope.launch { sessionManager.setHasSeenIntro(true) }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(450L)
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val repository: LinkUpRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LinkUpViewModel(repository, sessionManager) as T
        }
    }
}
