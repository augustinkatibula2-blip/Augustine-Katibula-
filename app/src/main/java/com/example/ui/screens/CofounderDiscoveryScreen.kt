package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.FounderPreferenceEntity
import com.example.domain.CofounderCandidate
import com.example.domain.FormatUtils
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CofounderDiscoveryScreen(
    state: LinkUpUiState,
    onDecision: (CofounderCandidate, String) -> Unit,
    onSavePreferences: (FounderPreferenceEntity) -> Unit,
    onReportUser: (String, String) -> Unit,
    onRefresh: () -> Unit = {},
    onRequireAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSignedIn = state.currentUser != null
    var isSwipeMode by remember { mutableStateOf(true) }
    var showPrefsModal by remember { mutableStateOf(false) }
    var inspectedCandidate by remember { mutableStateOf<CofounderCandidate?>(null) }
    var industryFilter by remember { mutableStateOf("All") }

    val filteredCandidates = remember(state.cofounderCandidates, industryFilter, isSwipeMode) {
        state.cofounderCandidates.filter { cand ->
            val industryOk = industryFilter == "All" || cand.preferences.industry.contains(industryFilter, true)
            val swipeOk = if (isSwipeMode) cand.myDecision == null else true
            industryOk && swipeOk
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                Column(
                    modifier = Modifier
                        .widthIn(max = 820.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Co-Founders", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Matched across skills, vision & commitment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                if (isSignedIn) showPrefsModal = true else onRequireAuth()
                            },
                            modifier = Modifier.testTag("cofounder_edit_prefs_button")
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isSignedIn) "Preferences" else "Sign In")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = isSwipeMode,
                                onClick = { isSwipeMode = true },
                                shape = SegmentedButtonDefaults.itemShape(0, 2)
                            ) { Text("Swipe Cards") }
                            SegmentedButton(
                                selected = !isSwipeMode,
                                onClick = { isSwipeMode = false },
                                shape = SegmentedButtonDefaults.itemShape(1, 2)
                            ) { Text("Browse All") }
                        }
                        Text(
                            "${filteredCandidates.size} profiles",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricBlue
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (isSwipeMode) {
            val topCandidate = filteredCandidates.firstOrNull()
            if (topCandidate == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 820.dp)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = "You've reviewed all current cards",
                        subtitle = "Switch to Browse All to revisit saved or passed profiles, or update your Co-Founder Preferences.",
                        actionLabel = "Browse All Matches",
                        onAction = { isSwipeMode = false },
                        icon = Icons.Rounded.Handshake
                    )
                }
            } else {
                SwipeCofounderDeck(
                    candidate = topCandidate,
                    lowDataMode = state.lowDataMode,
                    onInterested = { if (isSignedIn) onDecision(topCandidate, "INTERESTED") else onRequireAuth() },
                    onPass = { if (isSignedIn) onDecision(topCandidate, "PASS") else onRequireAuth() },
                    onSave = { if (isSignedIn) onDecision(topCandidate, "SAVED") else onRequireAuth() },
                    onViewProfile = { inspectedCandidate = topCandidate },
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .padding(16.dp)
                )
            }
        } else {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "AgriTech", "FinTech", "Clean Energy", "EdTech").forEach { ind ->
                                FilterChip(
                                    selected = industryFilter == ind,
                                    onClick = { industryFilter = ind },
                                    label = { Text(ind) }
                                )
                            }
                        }
                    }
                    if (filteredCandidates.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No matching profiles for \"$industryFilter\"",
                                subtitle = "Try clearing your filter or broadening remote preferences.",
                                actionLabel = "Show All Industries",
                                onAction = { industryFilter = "All" },
                                icon = Icons.Rounded.SearchOff
                            )
                        }
                    } else {
                        items(filteredCandidates, key = { it.user.id }) { candidate ->
                            CofounderBrowseCard(
                                candidate = candidate,
                                lowDataMode = state.lowDataMode,
                                onInterested = { if (isSignedIn) onDecision(candidate, "INTERESTED") else onRequireAuth() },
                                onSave = { if (isSignedIn) onDecision(candidate, "SAVED") else onRequireAuth() },
                                onViewProfile = { inspectedCandidate = candidate }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPrefsModal && state.currentFounderPrefs != null) {
        FounderPreferencesBottomSheet(
            prefs = state.currentFounderPrefs,
            onDismiss = { showPrefsModal = false },
            onSave = { updated ->
                onSavePreferences(updated)
                showPrefsModal = false
            }
        )
    }

    inspectedCandidate?.let { cand ->
        CofounderDetailBottomSheet(
            candidate = cand,
            lowDataMode = state.lowDataMode,
            onDismiss = { inspectedCandidate = null },
            onInterested = {
                if (isSignedIn) onDecision(cand, "INTERESTED") else onRequireAuth()
                inspectedCandidate = null
            },
            onReport = { reason ->
                if (isSignedIn) onReportUser(cand.user.id, reason) else onRequireAuth()
                inspectedCandidate = null
            }
        )
    }
}

@Composable
private fun SwipeCofounderDeck(
    candidate: CofounderCandidate,
    lowDataMode: Boolean,
    onInterested: () -> Unit,
    onPass: () -> Unit,
    onSave: () -> Unit,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember(candidate.user.id) { mutableFloatStateOf(0f) }
    val animatedX by animateFloatAsState(targetValue = dragOffsetX, label = "swipeX")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(animatedX.roundToInt(), 0) }
                    .graphicsLayer { rotationZ = (animatedX / 60f).coerceIn(-6f, 6f) }
                    .pointerInput(candidate.user.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    dragOffsetX > 180f -> onInterested()
                                    dragOffsetX < -180f -> onPass()
                                }
                                dragOffsetX = 0f
                            },
                            onDragCancel = { dragOffsetX = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragOffsetX += dragAmount }
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        animatedX > 80f -> VerifiedEmerald
                        animatedX < -80f -> ErrorCrimson
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InitialsOrPhotoAvatar(
                                    fullName = candidate.profile.fullName,
                                    photoUri = candidate.profile.photoUri,
                                    lowDataMode = lowDataMode,
                                    size = 56.dp
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(candidate.profile.fullName, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "${candidate.profile.city}, ${candidate.profile.country}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    VerificationBadgeChip(candidate.profile.verificationStatus)
                                }
                                CompatibilityScoreBadge(candidate.compatibility.score)
                            }
                        }

                        if (candidate.theyInterestedInMe) {
                            item {
                                Surface(color = MaterialTheme.linkUpColors.accentContainer, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        "Interested in connecting with you",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.linkUpColors.onAccentContainer
                                    )
                                }
                            }
                        }

                        item {
                            Text(candidate.profile.headline, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.linkUpColors.accentText)
                        }

                        if (candidate.compatibility.reasons.isNotEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Compatibility Highlights", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.linkUpColors.accentText)
                                        candidate.compatibility.reasons.forEach { reason ->
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.linkUpColors.successFg,
                                                    modifier = Modifier.size(15.dp).padding(top = 2.dp)
                                                )
                                                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Core Skills", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SkillTagRow(candidate.profile.skills, maxTags = 5)
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Looking For (${candidate.preferences.preferredRoles})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                SkillTagRow(candidate.preferences.skillsNeeded, maxTags = 4, highlightAccent = true)
                            }
                        }

                        item {
                            val stageLabel = when (candidate.preferences.startupStage) {
                                "MVP" -> "MVP"
                                else -> FormatUtils.humanizeEnum(candidate.preferences.startupStage)
                            }
                            InfoRow("Industry & Stage", "${candidate.preferences.industry} · $stageLabel")
                            InfoRow(
                                "Commitment",
                                "${FormatUtils.humanizeEnum(candidate.preferences.commitmentType)} · ~${candidate.preferences.weeklyHoursAvailable} hrs/wk"
                            )
                            InfoRow("Experience", "${candidate.profile.yearsExperience} yrs · ${candidate.profile.experienceSummary}")
                        }
                    }

                    if (abs(animatedX) > 45f) {
                        val isRight = animatedX > 0f
                        val ext = MaterialTheme.linkUpColors
                        Surface(
                            color = if (isRight) ext.successBg else ext.errorBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isRight) ext.successFg else ext.errorFg),
                            modifier = Modifier
                                .align(if (isRight) Alignment.TopStart else Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isRight) "INTERESTED" else "PASS",
                                color = if (isRight) ext.successFg else ext.errorFg,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPass,
                modifier = Modifier.weight(1f).height(48.dp).testTag("swipe_pass_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCrimson)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Pass", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pass")
            }
            OutlinedButton(
                onClick = onSave,
                modifier = Modifier.height(48.dp).testTag("swipe_save_button")
            ) {
                Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Save")
            }
            OutlinedButton(
                onClick = onViewProfile,
                modifier = Modifier.height(48.dp).testTag("swipe_view_profile_button")
            ) {
                Text("Profile")
            }
            Button(
                onClick = onInterested,
                modifier = Modifier.weight(1.2f).height(48.dp).testTag("swipe_interested_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Icon(Icons.Rounded.Favorite, contentDescription = "Interested", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Interested")
            }
        }
    }
}

@Composable
private fun CofounderBrowseCard(
    candidate: CofounderCandidate,
    lowDataMode: Boolean,
    onInterested: () -> Unit,
    onSave: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialsOrPhotoAvatar(candidate.profile.fullName, candidate.profile.photoUri, lowDataMode)
                Column(modifier = Modifier.weight(1f)) {
                    Text(candidate.profile.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${candidate.profile.city}, ${candidate.profile.country} · ${FormatUtils.humanizeEnum(candidate.preferences.commitmentType)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CompatibilityScoreBadge(candidate.compatibility.score)
            }
            Text(candidate.profile.headline, style = MaterialTheme.typography.bodyMedium)
            SkillTagRow(candidate.profile.skills, maxTags = 4)
            candidate.compatibility.reasons.take(2).forEach { reason ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.linkUpColors.successFg, modifier = Modifier.size(15.dp))
                    Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.linkUpColors.successFg)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onViewProfile, modifier = Modifier.weight(1f)) { Text("View Profile") }
                OutlinedButton(onClick = onSave) { Text(if (candidate.myDecision == "SAVED") "Saved" else "Save") }
                Button(
                    onClick = onInterested,
                    enabled = candidate.myDecision != "INTERESTED",
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text(if (candidate.myDecision == "INTERESTED") "Sent" else "Interested")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "Not specified" }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BreakdownMetricRow(label: String, score: Int, max: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$score / $max", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.linkUpColors.accentText, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CofounderDetailBottomSheet(
    candidate: CofounderCandidate,
    lowDataMode: Boolean,
    onDismiss: () -> Unit,
    onInterested: () -> Unit,
    onReport: (String) -> Unit
) {
    var showReportBox by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InitialsOrPhotoAvatar(candidate.profile.fullName, candidate.profile.photoUri, lowDataMode, size = 52.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(candidate.profile.fullName, style = MaterialTheme.typography.titleLarge)
                        Text("${candidate.profile.city}, ${candidate.profile.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CompatibilityScoreBadge(candidate.compatibility.score)
                }
            }
            item {
                VerificationBadgeChip(candidate.profile.verificationStatus)
            }
            item {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Compatibility Breakdown", style = MaterialTheme.typography.titleSmall)
                        BreakdownMetricRow("Complementary Skills", candidate.compatibility.complementarySkillsScore, 25)
                        BreakdownMetricRow("Shared Vision & Interests", candidate.compatibility.sharedVisionScore, 20)
                        BreakdownMetricRow("Commitment & Hours", candidate.compatibility.commitmentScore, 20)
                        BreakdownMetricRow("Goals & Equity", candidate.compatibility.goalsScore, 15)
                        BreakdownMetricRow("Working Style", candidate.compatibility.workingStyleScore, 10)
                        BreakdownMetricRow("Location & Language", candidate.compatibility.locationCommScore, 10)
                    }
                }
            }
            item {
                InfoRow("Business Idea / Problem", candidate.preferences.businessIdea)
                InfoRow("Education", "${candidate.profile.educationLevel} · ${candidate.profile.institution}")
                InfoRow("Equity & Funding", "${candidate.preferences.equityExpectation} · ${candidate.preferences.fundingSituation}")
                InfoRow("Languages", candidate.profile.languages)
                InfoRow("Portfolio", candidate.profile.portfolioUrl)
            }
            if (showReportBox) {
                item {
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it; reportError = null },
                        label = { Text("Reason for report") },
                        isError = reportError != null,
                        supportingText = reportError?.let { msg -> { Text(msg) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = {
                        if (reportReason.isBlank()) reportError = "Please describe the reason for this report."
                        else onReport(reportReason.trim())
                    }) {
                        Text("Submit Report", color = ErrorCrimson)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = { showReportBox = !showReportBox }) {
                        Text("Report", color = ErrorCrimson)
                    }
                    Button(
                        onClick = onInterested,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Express Interest")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FounderPreferencesBottomSheet(
    prefs: FounderPreferenceEntity,
    onDismiss: () -> Unit,
    onSave: (FounderPreferenceEntity) -> Unit
) {
    var industry by remember { mutableStateOf(prefs.industry) }
    var businessIdea by remember { mutableStateOf(prefs.businessIdea) }
    var hasIdea by remember { mutableStateOf(prefs.hasBusinessIdea) }
    var skillsPossessed by remember { mutableStateOf(prefs.skillsPossessed) }
    var skillsNeeded by remember { mutableStateOf(prefs.skillsNeeded) }
    var preferredRoles by remember { mutableStateOf(prefs.preferredRoles) }
    var weeklyHours by remember { mutableStateOf(prefs.weeklyHoursAvailable.toString()) }
    var commitment by remember { mutableStateOf(prefs.commitmentType) }
    var stage by remember { mutableStateOf(prefs.startupStage) }
    var funding by remember { mutableStateOf(prefs.fundingSituation) }
    var willingRemote by remember { mutableStateOf(prefs.willingRemote) }
    var equity by remember { mutableStateOf(prefs.equityExpectation) }
    var style by remember { mutableStateOf(prefs.workingStyle) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Co-Founder Preferences", style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false).heightIn(max = 420.dp)
            ) {
                item { OutlinedTextField(industry, { industry = it }, label = { Text("Startup Industry") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasIdea, onCheckedChange = { hasIdea = it })
                        Text("I already have a business idea", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item { OutlinedTextField(businessIdea, { businessIdea = it }, label = { Text("Business Idea / Problem") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(skillsPossessed, { skillsPossessed = it }, label = { Text("Skills You Possess") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(skillsNeeded, { skillsNeeded = it }, label = { Text("Skills Needed") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(preferredRoles, { preferredRoles = it }, label = { Text("Preferred Co-Founder Roles") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(
                        weeklyHours,
                        { weeklyHours = it.filter(Char::isDigit) },
                        label = { Text("Weekly Hours Available") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Commitment", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("FULL_TIME", "PART_TIME", "FLEXIBLE").forEach { c ->
                            FilterChip(
                                selected = commitment == c,
                                onClick = { commitment = c },
                                label = { Text(FormatUtils.humanizeEnum(c)) }
                            )
                        }
                    }
                }
                item {
                    Text("Startup Stage", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("IDEA", "MVP", "EARLY_TRACTION").forEach { s ->
                            val label = if (s == "MVP") "MVP" else FormatUtils.humanizeEnum(s)
                            FilterChip(selected = stage == s, onClick = { stage = s }, label = { Text(label) })
                        }
                    }
                }
                item { OutlinedTextField(funding, { funding = it }, label = { Text("Funding Situation") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(equity, { equity = it }, label = { Text("Equity Expectations") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(style, { style = it }, label = { Text("Working Style") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = willingRemote, onCheckedChange = { willingRemote = it })
                        Text("Willing to work remotely", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            prefs.copy(
                                industry = industry.trim(),
                                hasBusinessIdea = hasIdea,
                                businessIdea = businessIdea.trim(),
                                skillsPossessed = skillsPossessed.trim(),
                                skillsNeeded = skillsNeeded.trim(),
                                preferredRoles = preferredRoles.trim(),
                                weeklyHoursAvailable = weeklyHours.toIntOrNull() ?: 20,
                                commitmentType = commitment,
                                startupStage = stage,
                                fundingSituation = funding.trim(),
                                willingRemote = willingRemote,
                                equityExpectation = equity.trim(),
                                workingStyle = style.trim()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) { Text("Save & Match") }
            }
        }
    }
}
