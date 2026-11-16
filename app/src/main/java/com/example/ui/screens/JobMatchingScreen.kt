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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.JobApplicationEntity
import com.example.domain.AccountType
import com.example.domain.FormatUtils
import com.example.domain.JobMatchItem
import com.example.ui.components.CompatibilityScoreBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SkillTagRow
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobMatchingScreen(
    state: LinkUpUiState,
    onApplyOrSave: (JobMatchItem, String, Boolean) -> Unit,
    onPublishJob: (
        title: String, org: String, desc: String, reqQuals: String, degreeReq: Boolean,
        reqSkills: String, expLevel: String, minYears: Int, country: String, city: String,
        workMode: String, salary: String, empType: String, deadline: String, instructions: String
    ) -> Unit,
    onUpdateApplicationStatus: (JobApplicationEntity, String, String) -> Unit,
    onRefresh: () -> Unit = {},
    onRequireAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user = state.currentUser
    val isSignedIn = user != null
    val isEmployer = user?.accountType == AccountType.EMPLOYER.code || user?.role == "ADMIN"
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPublishSheet by remember { mutableStateOf(false) }
    var inspectedJob by remember { mutableStateOf<JobMatchItem?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var workModeFilter by remember { mutableStateOf("ALL") }

    val filteredJobs = remember(state.jobMatches, searchQuery, workModeFilter) {
        val q = searchQuery.trim()
        state.jobMatches.filter { item ->
            val job = item.job
            val queryOk = q.isBlank() ||
                job.title.contains(q, true) ||
                job.organization.contains(q, true) ||
                job.requiredSkills.contains(q, true) ||
                job.city.contains(q, true)
            val modeOk = when (workModeFilter) {
                "ALL" -> true
                "SKILLS_FIRST" -> !job.degreeRequired
                else -> job.workMode.equals(workModeFilter, ignoreCase = true)
            }
            queryOk && modeOk
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
                            Text("Jobs", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Matched on your skills, experience & portfolio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isEmployer) {
                            Button(
                                onClick = { showPublishSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier.testTag("publish_job_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Post Job")
                            }
                        }
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(0, if (isEmployer) 3 else 2)
                        ) { Text("Recommended") }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(1, if (isEmployer) 3 else 2)
                        ) { Text("My Tracker") }
                        if (isEmployer) {
                            SegmentedButton(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                shape = SegmentedButtonDefaults.itemShape(2, 3)
                            ) { Text("Applicants") }
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
                when (selectedTab) {
                    0 -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search roles, organizations, skills, or city") },
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
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "ALL" to "All Roles",
                                        "REMOTE" to "Remote",
                                        "HYBRID" to "Hybrid",
                                        "PHYSICAL" to "On-site",
                                        "SKILLS_FIRST" to "Skills-First (No Degree)"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = workModeFilter == key,
                                            onClick = { workModeFilter = key },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                        if (filteredJobs.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = if (state.jobMatches.isEmpty()) "No open roles right now" else "No matching jobs",
                                    subtitle = if (state.jobMatches.isEmpty()) "Employers can publish new vacancies directly from this tab."
                                    else "Try clearing your search or work-mode filter.",
                                    actionLabel = if (searchQuery.isNotBlank() || workModeFilter != "ALL") "Reset Filters" else null,
                                    onAction = if (searchQuery.isNotBlank() || workModeFilter != "ALL") {
                                        { searchQuery = ""; workModeFilter = "ALL" }
                                    } else null,
                                    icon = Icons.Rounded.WorkOutline
                                )
                            }
                        } else {
                            items(filteredJobs, key = { it.job.id }) { item ->
                                JobMatchCard(
                                    item = item,
                                    onInspect = { inspectedJob = item },
                                    onApply = { if (isSignedIn) inspectedJob = item else onRequireAuth() },
                                    onSave = { if (isSignedIn) onApplyOrSave(item, "", true) else onRequireAuth() }
                                )
                            }
                        }
                    }
                    1 -> {
                        val myApps = state.allApplications.filter { it.applicantId == user?.id }
                        if (!isSignedIn) {
                            item {
                                EmptyStateCard(
                                    title = "Track your applications",
                                    subtitle = "Sign in or create an account to save jobs and track application statuses.",
                                    actionLabel = "Sign In",
                                    onAction = onRequireAuth,
                                    icon = Icons.Rounded.BookmarkBorder
                                )
                            }
                        } else if (myApps.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No saved or submitted applications",
                                    subtitle = "Apply to or save recommended jobs to track progress here.",
                                    icon = Icons.Rounded.BookmarkBorder
                                )
                            }
                        } else {
                            items(myApps, key = { it.id }) { app ->
                                val job = state.allJobs.find { it.id == app.jobId }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(job?.title ?: "Vacancy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                            Surface(color = MaterialTheme.linkUpColors.accentContainer, shape = RoundedCornerShape(6.dp)) {
                                                Text(
                                                    FormatUtils.humanizeEnum(app.status),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.linkUpColors.onAccentContainer
                                                )
                                            }
                                        }
                                        Text("${job?.organization ?: ""} · ${app.matchScore}% Match", style = MaterialTheme.typography.bodySmall)
                                        if (app.coverNote.isNotBlank()) {
                                            Text("Note: ${app.coverNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val myPostedJobs = state.allJobs.filter { it.employerId == user?.id || user?.role == "ADMIN" }
                        if (myPostedJobs.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No vacancies posted yet",
                                    subtitle = "Tap Post Job above to create your first vacancy.",
                                    icon = Icons.Rounded.WorkOutline
                                )
                            }
                        } else {
                            items(myPostedJobs, key = { it.id }) { job ->
                                val applicants = state.allApplications.filter { it.jobId == job.id && !it.isSavedOnly }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(job.title, style = MaterialTheme.typography.titleMedium)
                                        Text("${applicants.size} applicant(s) · ${job.city}, ${job.country}", style = MaterialTheme.typography.bodySmall)
                                        applicants.forEach { app ->
                                            val applicantProfile = state.allProfiles.find { it.userId == app.applicantId }
                                            Surface(
                                                color = MaterialTheme.colorScheme.background,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${applicantProfile?.fullName ?: "Candidate"} (${app.matchScore}% Match)", style = MaterialTheme.typography.titleSmall)
                                                        Text(FormatUtils.humanizeEnum(app.status), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.linkUpColors.onAccentContainer)
                                                    }
                                                    if (!applicantProfile?.skills.isNullOrBlank()) {
                                                        SkillTagRow(applicantProfile!!.skills, maxTags = 4)
                                                    }
                                                    Row(
                                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        listOf("SHORTLISTED", "INTERVIEW", "OFFER").forEach { st ->
                                                            FilterChip(
                                                                selected = app.status == st,
                                                                onClick = { onUpdateApplicationStatus(app, st, job.title) },
                                                                label = { Text(FormatUtils.humanizeEnum(st)) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedJob?.let { item ->
        var note by remember(item.job.id) { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { inspectedJob = null }) {
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
                    CompatibilityScoreBadge(item.matchScore)
                    Text(
                        "${FormatUtils.humanizeEnum(item.job.employmentType)} · ${FormatUtils.humanizeEnum(item.job.workMode)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.linkUpColors.accentBlue
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.job.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${item.job.organization} · ${item.job.city}, ${item.job.country}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(item.job.description, style = MaterialTheme.typography.bodyMedium)
                if (item.job.requiredQualifications.isNotBlank()) {
                    Text("Qualifications: ${item.job.requiredQualifications}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${item.job.salaryRange.ifBlank { "Competitive" }} · Closes ${item.job.deadline}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.linkUpColors.accentText
                )
                SkillTagRow(item.job.requiredSkills, maxTags = 6)

                if (item.applicationStatus == null || item.isSaved) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Cover Note / Portfolio Highlights") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            if (isSignedIn) {
                                onApplyOrSave(item, "", true)
                                inspectedJob = null
                            } else {
                                inspectedJob = null
                                onRequireAuth()
                            }
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (item.isSaved) "Saved" else "Save")
                    }
                    Button(
                        onClick = {
                            if (isSignedIn) {
                                onApplyOrSave(item, note, false)
                                inspectedJob = null
                            } else {
                                inspectedJob = null
                                onRequireAuth()
                            }
                        },
                        enabled = item.applicationStatus == null || item.isSaved,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(
                            if (item.applicationStatus != null && !item.isSaved) FormatUtils.humanizeEnum(item.applicationStatus)
                            else if (isSignedIn) "Submit Application" else "Sign In to Apply"
                        )
                    }
                }
            }
        }
    }

    if (showPublishSheet) {
        PublishJobBottomSheet(
            onDismiss = { showPublishSheet = false },
            onPublish = { t, o, d, rq, deg, rs, el, minY, c, ct, wm, sal, et, dl, ins ->
                onPublishJob(t, o, d, rq, deg, rs, el, minY, c, ct, wm, sal, et, dl, ins)
                showPublishSheet = false
            }
        )
    }
}

@Composable
private fun JobMatchCard(
    item: JobMatchItem,
    onInspect: () -> Unit,
    onApply: () -> Unit,
    onSave: () -> Unit
) {
    val job = item.job
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() },
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
                CompatibilityScoreBadge(item.matchScore)
                Text(
                    "${FormatUtils.humanizeEnum(job.employmentType)} · ${FormatUtils.humanizeEnum(job.workMode)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.linkUpColors.accentBlue
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(job.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${job.organization} · ${job.city}, ${job.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(job.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            SkillTagRow(job.requiredSkills, maxTags = 5)
            Text(
                "${job.salaryRange.ifBlank { "Competitive" }} · Closes ${job.deadline}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.linkUpColors.accentText
            )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSave) {
                    Text(if (item.isSaved) "Saved" else "Save Job")
                }
                Button(
                    onClick = onApply,
                    enabled = item.applicationStatus == null || item.isSaved,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text(
                        when {
                            item.applicationStatus != null && !item.isSaved -> FormatUtils.humanizeEnum(item.applicationStatus)
                            else -> "Apply Now"
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishJobBottomSheet(
    onDismiss: () -> Unit,
    onPublish: (String, String, String, String, Boolean, String, String, Int, String, String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var org by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var quals by remember { mutableStateOf("") }
    var degreeRequired by remember { mutableStateOf(false) }
    var skills by remember { mutableStateOf("") }
    var minYears by remember { mutableStateOf("1") }
    var country by remember { mutableStateOf("Malawi") }
    var city by remember { mutableStateOf("Lilongwe") }
    var workMode by remember { mutableStateOf("HYBRID") }
    var salary by remember { mutableStateOf("") }
    var empType by remember { mutableStateOf("FULL_TIME") }
    var deadline by remember { mutableStateOf("2026-12-31") }
    var instructions by remember { mutableStateOf("Apply through LinkUp with your skills & portfolio.") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Publish Job Vacancy", style = MaterialTheme.typography.titleLarge)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false).heightIn(max = 420.dp)
            ) {
                if (validationError != null) {
                    item {
                        Text(validationError!!, style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
                    }
                }
                item { OutlinedTextField(title, { title = it; validationError = null }, label = { Text("Job Title *") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(org, { org = it; validationError = null }, label = { Text("Organization *") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(desc, { desc = it }, label = { Text("Job Description") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(skills, { skills = it }, label = { Text("Required Skills (comma-separated)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(quals, { quals = it }, label = { Text("Required Qualifications") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text("Work Mode", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("REMOTE", "HYBRID", "PHYSICAL").forEach { mode ->
                            FilterChip(
                                selected = workMode == mode,
                                onClick = { workMode = mode },
                                label = { Text(FormatUtils.humanizeEnum(mode)) }
                            )
                        }
                    }
                }
                item {
                    Text("Employment Type", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP").forEach { et ->
                            FilterChip(
                                selected = empType == et,
                                onClick = { empType = et },
                                label = { Text(FormatUtils.humanizeEnum(et)) }
                            )
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = degreeRequired, onCheckedChange = { degreeRequired = it })
                        Column {
                            Text("Require formal university degree", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Leave unchecked to evaluate candidates primarily on skills & portfolio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        minYears,
                        { minYears = it.filter(Char::isDigit) },
                        label = { Text("Min Years Experience") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { OutlinedTextField(country, { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(city, { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(salary, { salary = it }, label = { Text("Salary Range (optional)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(deadline, { deadline = it }, label = { Text("Application Deadline") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(instructions, { instructions = it }, label = { Text("Application Instructions") }, modifier = Modifier.fillMaxWidth()) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text("Cancel") }
                Button(
                    onClick = {
                        if (title.isBlank() || org.isBlank()) {
                            validationError = "Please provide both Job Title and Organization."
                        } else {
                            onPublish(
                                title.trim(), org.trim(), desc.trim(), quals.trim(), degreeRequired, skills.trim(),
                                "Intermediate", minYears.toIntOrNull() ?: 1,
                                country.trim(), city.trim(), workMode, salary.trim(), empType, deadline.trim(), instructions.trim()
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) { Text("Publish") }
            }
        }
    }
}
