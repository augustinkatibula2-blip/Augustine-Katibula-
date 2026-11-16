package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.FormatUtils
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    state: LinkUpUiState,
    languageCode: String,
    isSignedIn: Boolean,
    onFindCofounder: () -> Unit,
    onFindJob: () -> Unit,
    onBuildTeam: () -> Unit,
    onOpenMessages: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSignInClick: () -> Unit,
    onAboutClick: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Native Mobile Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        LinkUpLogo(iconSize = 24.dp)
                        Text(
                            text = "Malawi · Africa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                            modifier = Modifier.testTag("home_search_icon")
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        }
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("home_notifications_icon")
                        ) {
                            Icon(Icons.Rounded.NotificationsNone, contentDescription = "Notifications")
                        }
                        FilledTonalButton(
                            onClick = onSignInClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("landing_signin_button")
                        ) {
                            Text(if (isSignedIn) "Account" else "Sign In")
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
                // Native Mobile Search Bar Trigger
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSearch() }
                            .testTag("home_search_bar"),
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

                // Compact Horizontal Quick-Access Pills
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickPillAction(
                            icon = Icons.Rounded.Handshake,
                            label = "Co-Founders (${state.cofounderCandidates.size})",
                            onClick = onFindCofounder,
                            modifier = Modifier.testTag("cta_find_cofounder")
                        )
                        QuickPillAction(
                            icon = Icons.Rounded.WorkOutline,
                            label = "Jobs (${state.jobMatches.size})",
                            onClick = onFindJob,
                            modifier = Modifier.testTag("cta_find_job")
                        )
                        QuickPillAction(
                            icon = Icons.Rounded.Groups,
                            label = "Startup Teams (${state.talentMatches.size})",
                            onClick = onBuildTeam,
                            modifier = Modifier.testTag("cta_build_team")
                        )
                    }
                }

                // Slim Account Prompt Card (Non-hero, mobile native)
                if (!isSignedIn) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSignInClick() }
                                .testTag("cta_create_account"),
                            color = MaterialTheme.linkUpColors.accentContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.linkUpColors.accentBlue.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = ElectricBlue,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Personalize your matches", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.linkUpColors.accentText)
                                    Text(
                                        "Create a profile to score compatibility, save jobs & message founders.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Sign In", tint = MaterialTheme.linkUpColors.onAccentContainer, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Live Co-Founder Feed
                if (state.cofounderCandidates.isNotEmpty()) {
                    item {
                        SectionHeaderRow(
                            title = "Co-Founders Near You",
                            actionLabel = "See all",
                            onAction = onFindCofounder
                        )
                    }
                    items(state.cofounderCandidates.take(3), key = { it.user.id }) { candidate ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFindCofounder() },
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                                    VerificationBadgeChip(candidate.profile.verificationStatus)
                                }
                                Text(
                                    candidate.profile.headline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                SkillTagRow(candidate.profile.skills, maxTags = 4)
                            }
                        }
                    }
                }

                // Live Open Roles Feed
                if (state.jobMatches.isNotEmpty()) {
                    item {
                        SectionHeaderRow(
                            title = "Open Roles",
                            actionLabel = "All jobs",
                            onAction = onFindJob
                        )
                    }
                    items(state.jobMatches.take(3), key = { it.job.id }) { item ->
                        val job = item.job
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFindJob() },
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.WorkOutline, contentDescription = null, tint = MaterialTheme.linkUpColors.accentText, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(job.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${job.organization} · ${job.city}, ${job.country} · ${FormatUtils.humanizeEnum(job.workMode)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Open job",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Quiet App Info Link
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(
                            onClick = onAboutClick,
                            modifier = Modifier.testTag("landing_about_button")
                        ) {
                            Text("About LinkUp & Founder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPillAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.linkUpColors.accentBlue, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.linkUpColors.accentText)
        }
    }
}

@Composable
private fun SectionHeaderRow(
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
