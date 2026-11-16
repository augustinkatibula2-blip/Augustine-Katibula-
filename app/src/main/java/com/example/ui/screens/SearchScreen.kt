package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.FormatUtils
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    state: LinkUpUiState,
    onBack: () -> Unit,
    onOpenCofounders: () -> Unit,
    onOpenJobs: () -> Unit,
    onOpenTalent: () -> Unit
) {
    BackHandler { onBack() }

    var query by remember { mutableStateOf("") }
    var scopeFilter by remember { mutableStateOf("ALL") } // ALL, FOUNDERS, JOBS, TALENT
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val q = query.trim()

    val founderResults = remember(state.cofounderCandidates, q, scopeFilter) {
        if (scopeFilter !in listOf("ALL", "FOUNDERS")) emptyList()
        else state.cofounderCandidates.filter { cand ->
            q.isBlank() ||
                cand.profile.fullName.contains(q, true) ||
                cand.profile.headline.contains(q, true) ||
                cand.profile.skills.contains(q, true) ||
                cand.preferences.industry.contains(q, true) ||
                cand.profile.city.contains(q, true)
        }
    }

    val jobResults = remember(state.jobMatches, q, scopeFilter) {
        if (scopeFilter !in listOf("ALL", "JOBS")) emptyList()
        else state.jobMatches.filter { item ->
            val job = item.job
            q.isBlank() ||
                job.title.contains(q, true) ||
                job.organization.contains(q, true) ||
                job.requiredSkills.contains(q, true) ||
                job.city.contains(q, true) ||
                job.workMode.contains(q, true)
        }
    }

    val talentResults = remember(state.talentMatches, q, scopeFilter) {
        if (scopeFilter !in listOf("ALL", "TALENT")) emptyList()
        else state.talentMatches.filter { item ->
            val opp = item.opportunity
            q.isBlank() ||
                opp.title.contains(q, true) ||
                opp.startupName.contains(q, true) ||
                opp.requiredSkills.contains(q, true) ||
                opp.roleCategory.contains(q, true) ||
                opp.city.contains(q, true)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search people, jobs, skills...") },
                            singleLine = true,
                            trailingIcon = {
                                if (query.isNotBlank()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .focusRequester(focusRequester)
                                .testTag("global_search_input")
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "FOUNDERS" to "Co-Founders",
                            "JOBS" to "Jobs",
                            "TALENT" to "Startup Teams"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = scopeFilter == key,
                                onClick = { scopeFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (q.isBlank()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Popular Searches",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("AgriTech", "FinTech", "Kotlin", "Marketing", "Remote", "Lilongwe", "Blantyre").forEach { tag ->
                                    SuggestionChip(
                                        onClick = { query = tag },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (founderResults.isEmpty() && jobResults.isEmpty() && talentResults.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No results for \"$q\"",
                            subtitle = "Try a broader skill, city, or switch category filters above.",
                            actionLabel = "Clear Search",
                            onAction = { query = ""; scopeFilter = "ALL" },
                            icon = Icons.Rounded.SearchOff
                        )
                    }
                } else {
                    if (founderResults.isNotEmpty()) {
                        item {
                            Text(
                                "Co-Founders (${founderResults.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.linkUpColors.accentText
                            )
                        }
                        items(founderResults.take(5), key = { "f_${it.user.id}" }) { cand ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenCofounders() },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    InitialsOrPhotoAvatar(cand.profile.fullName, cand.profile.photoUri, state.lowDataMode, size = 44.dp)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(cand.profile.fullName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${cand.preferences.industry} · ${cand.profile.city}, ${cand.profile.country}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    CompatibilityScoreBadge(cand.compatibility.score)
                                }
                            }
                        }
                    }

                    if (jobResults.isNotEmpty()) {
                        item {
                            Text(
                                "Jobs (${jobResults.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.linkUpColors.accentText
                            )
                        }
                        items(jobResults.take(5), key = { "j_${it.job.id}" }) { item ->
                            val job = item.job
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenJobs() },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Rounded.WorkOutline, contentDescription = null, tint = MaterialTheme.linkUpColors.accentBlue)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(job.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${job.organization} · ${job.city} · ${FormatUtils.humanizeEnum(job.workMode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (talentResults.isNotEmpty()) {
                        item {
                            Text(
                                "Startup Team Roles (${talentResults.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.linkUpColors.accentText
                            )
                        }
                        items(talentResults.take(5), key = { "t_${it.opportunity.id}" }) { item ->
                            val opp = item.opportunity
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenTalent() },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.linkUpColors.accentBlue)
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(opp.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${opp.startupName} · ${opp.roleCategory}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    CompensationBadge(opp.compensationType)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
