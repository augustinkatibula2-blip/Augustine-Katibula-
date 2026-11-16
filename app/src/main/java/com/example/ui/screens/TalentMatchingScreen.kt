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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.domain.CompensationType
import com.example.domain.FormatUtils
import com.example.domain.TalentMatchItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalentMatchingScreen(
    state: LinkUpUiState,
    onConnectTalent: (founderId: String, oppTitle: String) -> Unit,
    onPublishOpportunity: (
        startupName: String, roleCategory: String, title: String, description: String,
        requiredSkills: String, compType: CompensationType, compDetails: String,
        country: String, city: String, workMode: String
    ) -> Unit,
    onRefresh: () -> Unit = {},
    onRequireAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSignedIn = state.currentUser != null
    var selectedCompFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    var inspectedRole by remember { mutableStateOf<TalentMatchItem?>(null) }

    val filtered = remember(state.talentMatches, selectedCompFilter, searchQuery) {
        val q = searchQuery.trim()
        state.talentMatches.filter { item ->
            val opp = item.opportunity
            val compOk = selectedCompFilter == "ALL" || opp.compensationType == selectedCompFilter
            val queryOk = q.isBlank() ||
                opp.title.contains(q, true) ||
                opp.startupName.contains(q, true) ||
                opp.requiredSkills.contains(q, true) ||
                opp.roleCategory.contains(q, true) ||
                opp.city.contains(q, true)
            compOk && queryOk
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Startup Talent", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Join startup teams with transparent compensation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { if (isSignedIn) showCreateSheet = true else onRequireAuth() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.testTag("post_talent_role_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Post Role")
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = selectedCompFilter == "ALL", onClick = { selectedCompFilter = "ALL" }, label = { Text("All") })
                        CompensationType.entries.forEach { comp ->
                            FilterChip(
                                selected = selectedCompFilter == comp.code,
                                onClick = { selectedCompFilter = comp.code },
                                label = { Text(comp.label) }
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
                .fillMaxSize()
                .widthIn(max = 820.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search role, startup, category, or skills") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No roles under this filter",
                            subtitle = "Try selecting All or clearing your search query.",
                            actionLabel = if (searchQuery.isNotBlank() || selectedCompFilter != "ALL") "Reset Filters" else null,
                            onAction = if (searchQuery.isNotBlank() || selectedCompFilter != "ALL") {
                                { searchQuery = ""; selectedCompFilter = "ALL" }
                            } else null,
                            icon = Icons.Rounded.Groups
                        )
                    }
                } else {
                    items(filtered, key = { it.opportunity.id }) { item ->
                        val opp = item.opportunity
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inspectedRole = item },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CompensationBadge(opp.compensationType)
                                    CompatibilityScoreBadge(item.matchScore)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(opp.title, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "${opp.startupName} · ${opp.roleCategory} · ${opp.city}, ${opp.country} · ${FormatUtils.humanizeEnum(opp.workMode)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(opp.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                Text("Compensation: ${opp.compensationDetails}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.linkUpColors.accentText)
                                SkillTagRow(opp.requiredSkills, maxTags = 5)
                                if (item.reasons.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        item.reasons.forEach { reason ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.linkUpColors.successFg, modifier = Modifier.size(15.dp))
                                                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.linkUpColors.successFg)
                                            }
                                        }
                                    }
                                }
                                if (opp.founderId != state.currentUser?.id) {
                                    Button(
                                        onClick = { if (isSignedIn) onConnectTalent(opp.founderId, opp.title) else onRequireAuth() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                    ) {
                                        Text("Express Interest to ${item.founderName}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedRole?.let { item ->
        val opp = item.opportunity
        ModalBottomSheet(onDismissRequest = { inspectedRole = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompensationBadge(opp.compensationType)
                    CompatibilityScoreBadge(item.matchScore)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(opp.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${opp.startupName} · ${opp.roleCategory} · ${opp.city}, ${opp.country}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(opp.description, style = MaterialTheme.typography.bodyMedium)
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Compensation & Work Mode", style = MaterialTheme.typography.titleSmall)
                        Text(opp.compensationDetails, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.linkUpColors.accentText)
                        Text("Work mode: ${FormatUtils.humanizeEnum(opp.workMode)} · Founder: ${item.founderName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SkillTagRow(opp.requiredSkills, maxTags = 6)
                if (opp.founderId != state.currentUser?.id) {
                    Button(
                        onClick = {
                            inspectedRole = null
                            if (isSignedIn) onConnectTalent(opp.founderId, opp.title) else onRequireAuth()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(if (isSignedIn) "Express Interest to ${item.founderName}" else "Sign In to Connect")
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        PublishTalentBottomSheet(
            onDismiss = { showCreateSheet = false },
            onPublish = { s, r, t, d, sk, ct, cd, c, city, wm ->
                onPublishOpportunity(s, r, t, d, sk, ct, cd, c, city, wm)
                showCreateSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishTalentBottomSheet(
    onDismiss: () -> Unit,
    onPublish: (String, String, String, String, String, CompensationType, String, String, String, String) -> Unit
) {
    val roleCategories = listOf(
        "Software Developer", "Marketing Professional", "Sales Representative",
        "Business Development Officer", "Graphic Designer", "Financial Professional",
        "Operations Manager", "Product Manager", "Other"
    )
    var startupName by remember { mutableStateOf("") }
    var roleCat by remember { mutableStateOf(roleCategories.first()) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var compType by remember { mutableStateOf(CompensationType.PAID_POSITION) }
    var compDetails by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Malawi") }
    var city by remember { mutableStateOf("Blantyre") }
    var workMode by remember { mutableStateOf("REMOTE") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Post Startup Team Opportunity", style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false).heightIn(max = 420.dp)
            ) {
                if (validationError != null) {
                    item {
                        Text(validationError!!, style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
                    }
                }
                item { OutlinedTextField(startupName, { startupName = it; validationError = null }, label = { Text("Startup Name *") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(title, { title = it; validationError = null }, label = { Text("Role Title *") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text("Role Category", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        roleCategories.forEach { rc ->
                            FilterChip(selected = roleCat == rc, onClick = { roleCat = rc }, label = { Text(rc) })
                        }
                    }
                }
                item {
                    Text("Work Mode", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("REMOTE", "HYBRID", "PHYSICAL").forEach { mode ->
                            FilterChip(selected = workMode == mode, onClick = { workMode = mode }, label = { Text(FormatUtils.humanizeEnum(mode)) })
                        }
                    }
                }
                item { OutlinedTextField(desc, { desc = it }, label = { Text("Role Description") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(skills, { skills = it }, label = { Text("Required Skills") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text("Compensation Type (Required & Transparent)", style = MaterialTheme.typography.labelSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CompensationType.entries.forEach { ct ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = compType == ct, onClick = { compType = ct })
                                Text(ct.label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item { OutlinedTextField(compDetails, { compDetails = it; validationError = null }, label = { Text("Compensation Details (Salary / Equity % / Stipend) *") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(country, { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(city, { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth()) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text("Cancel") }
                Button(
                    onClick = {
                        if (startupName.isBlank() || title.isBlank() || compDetails.isBlank()) {
                            validationError = "Please fill in Startup Name, Role Title, and Compensation Details."
                        } else {
                            onPublish(startupName.trim(), roleCat, title.trim(), desc.trim(), skills.trim(), compType, compDetails.trim(), country.trim(), city.trim(), workMode)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) { Text("Publish") }
            }
        }
    }
}
