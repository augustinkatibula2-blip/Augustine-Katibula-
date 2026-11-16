package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AccountType
import com.example.domain.FormatUtils
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: LinkUpUiState,
    onOpenCofounders: () -> Unit,
    onOpenJobs: () -> Unit,
    onOpenTalent: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenTrials: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onRefresh: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user = state.currentUser
    val profile = state.currentProfile
    val accountType = AccountType.fromCode(user?.accountType ?: "FOUNDER")
    val unreadNotifs = state.notifications.count { !it.isRead }
    val pendingConns = state.myConnections.count { it.receiverId == user?.id && it.status == "PENDING" }
    val appliedJobs = state.allApplications.count { it.applicantId == user?.id && !it.isSavedOnly }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Consistent Mobile Top App Bar
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        LinkUpLogo(iconSize = 24.dp)
                        Text(
                            text = "${profile?.city ?: "Lilongwe"}, ${profile?.country ?: "Malawi"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("toggle_theme_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode"
                            )
                        }
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier.testTag("dashboard_search_icon")
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        }
                        if (user?.role == "ADMIN") {
                            IconButton(
                                onClick = onOpenAdmin,
                                modifier = Modifier.testTag("dashboard_open_admin")
                            ) {
                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = "Admin Console", tint = MaterialTheme.linkUpColors.accentText)
                            }
                        }
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("dashboard_notifications_icon")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge { Text(unreadNotifs.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.NotificationsNone, contentDescription = "Notifications")
                            }
                        }
                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier.testTag("dashboard_edit_profile")
                        ) {
                            InitialsOrPhotoAvatar(
                                fullName = profile?.fullName ?: "Member",
                                photoUri = profile?.photoUri,
                                lowDataMode = state.lowDataMode,
                                size = 34.dp
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mobile Search Bar Trigger
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSearch() }
                            .testTag("dashboard_search_bar"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Search co-founders, jobs, or startup roles",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Compact Member Status Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.linkUpColors.featureCardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.linkUpColors.featureCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Hi, ${profile?.fullName?.split(" ")?.firstOrNull() ?: "Builder"}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${accountType.label} · ${profile?.city ?: "Lilongwe"}, ${profile?.country ?: "Malawi"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                                VerificationBadgeChip(profile?.verificationStatus ?: "UNVERIFIED")
                            }

                            if (state.profileCompletion < 100) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenProfile() },
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Profile completeness", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                                        Text("${state.profileCompletion}% · Tap to complete", style = MaterialTheme.typography.labelSmall, color = ElectricBlueBright)
                                    }
                                    LinearProgressIndicator(
                                        progress = { state.profileCompletion / 100f },
                                        modifier = Modifier.fillMaxWidth().height(5.dp),
                                        color = ElectricBlueBright,
                                        trackColor = Color.White.copy(alpha = 0.16f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Unified 4-Column Activity Strip
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SummaryStatCell("Founders", state.cofounderCandidates.size.toString(), onOpenCofounders, Modifier.weight(1f))
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryStatCell("Requests", pendingConns.toString(), onOpenConnections, Modifier.weight(1f))
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryStatCell("Applied", appliedJobs.toString(), onOpenJobs, Modifier.weight(1f))
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryStatCell("Trials", state.myTrials.size.toString(), onOpenTrials, Modifier.weight(1f))
                        }
                    }
                }

                // Native Action List (Trials, Profile/Settings, Admin)
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            WorkspaceRow(
                                icon = Icons.Rounded.RocketLaunch,
                                title = "Build Together · 14-Day Trials",
                                subtitle = if (state.myTrials.isEmpty()) "Test working chemistry with a connected partner" else "${state.myTrials.size} active sprint(s)",
                                onClick = onOpenTrials
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            WorkspaceRow(
                                icon = Icons.Rounded.VerifiedUser,
                                title = "Profile, Verification & Settings",
                                subtitle = "Credentials, dark mode, privacy & alerts",
                                onClick = onOpenProfile
                            )
                            if (user?.role == "ADMIN") {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                WorkspaceRow(
                                    icon = Icons.Rounded.AdminPanelSettings,
                                    title = "Admin Console",
                                    subtitle = "Manage members, verification reviews & countries",
                                    onClick = onOpenAdmin
                                )
                            }
                        }
                    }
                }

                // Role-Tailored Recommendations
                when (accountType) {
                    AccountType.JOB_SEEKER, AccountType.STUDENT -> {
                        if (state.jobMatches.isNotEmpty()) {
                            item {
                                DashboardSectionHeader("Recommended Roles", "See all", onOpenJobs)
                            }
                            items(state.jobMatches.take(3), key = { it.job.id }) { item ->
                                JobPreviewRow(
                                    title = item.job.title,
                                    org = item.job.organization,
                                    location = "${item.job.city}, ${item.job.country} · ${FormatUtils.humanizeEnum(item.job.workMode)}",
                                    score = item.matchScore,
                                    onClick = onOpenJobs
                                )
                            }
                        }
                    }
                    AccountType.TALENT -> {
                        if (state.talentMatches.isNotEmpty()) {
                            item {
                                DashboardSectionHeader("Startup Team Opportunities", "See all", onOpenTalent)
                            }
                            items(state.talentMatches.take(3), key = { it.opportunity.id }) { item ->
                                JobPreviewRow(
                                    title = item.opportunity.title,
                                    org = item.opportunity.startupName,
                                    location = "${item.opportunity.city}, ${item.opportunity.country} · ${FormatUtils.humanizeEnum(item.opportunity.workMode)}",
                                    score = item.matchScore,
                                    onClick = onOpenTalent
                                )
                            }
                        }
                    }
                    AccountType.EMPLOYER -> {
                        val myJobs = state.allJobs.filter { it.employerId == user?.id }
                        item {
                            DashboardSectionHeader("Your Job Postings", "Manage", onOpenJobs)
                        }
                        if (myJobs.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No active vacancies",
                                    subtitle = "Publish a role to reach skills-matched candidates.",
                                    actionLabel = "Post a Job",
                                    onAction = onOpenJobs,
                                    icon = Icons.Rounded.WorkOutline
                                )
                            }
                        } else {
                            items(myJobs.take(3), key = { it.id }) { job ->
                                val count = state.allApplications.count { it.jobId == job.id && !it.isSavedOnly }
                                JobPreviewRow(
                                    title = job.title,
                                    org = job.organization,
                                    location = "$count applicant(s) · ${job.city}, ${job.country}",
                                    score = 100,
                                    showScore = false,
                                    onClick = onOpenJobs
                                )
                            }
                        }
                    }
                    AccountType.FOUNDER -> {
                        if (state.cofounderCandidates.isNotEmpty()) {
                            item {
                                DashboardSectionHeader("Top Co-Founder Matches", "See all", onOpenCofounders)
                            }
                            items(state.cofounderCandidates.take(2), key = { it.user.id }) { candidate ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenCofounders() },
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            InitialsOrPhotoAvatar(
                                                fullName = candidate.profile.fullName,
                                                photoUri = candidate.profile.photoUri,
                                                lowDataMode = state.lowDataMode
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(candidate.profile.fullName, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    "${candidate.preferences.industry} · ${candidate.profile.city}, ${candidate.profile.country}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            CompatibilityScoreBadge(candidate.compatibility.score)
                                        }
                                        SkillTagRow(candidate.profile.skills, maxTags = 4)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onOpenAbout) {
                            Text("About LinkUp", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCell(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.linkUpColors.accentText)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun WorkspaceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.linkUpColors.accentContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.linkUpColors.onAccentContainer, modifier = Modifier.size(18.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DashboardSectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text(actionLabel, color = MaterialTheme.linkUpColors.accentBlue)
        }
    }
}

@Composable
private fun JobPreviewRow(
    title: String,
    org: String,
    location: String,
    score: Int,
    showScore: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "$org · $location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showScore) {
                CompatibilityScoreBadge(score)
            }
        }
    }
}
