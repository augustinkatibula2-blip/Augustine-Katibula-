package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CountryRegionEntity
import com.example.data.local.UserEntity
import com.example.data.local.VerificationSubmissionEntity
import com.example.domain.FormatUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    state: LinkUpUiState,
    onBack: () -> Unit,
    onToggleUserSuspend: (UserEntity) -> Unit,
    onReviewVerification: (VerificationSubmissionEntity, Boolean, String) -> Unit,
    onModerateJob: (String, String) -> Unit,
    onResolveReport: (String, String) -> Unit,
    onAddCountry: (String, String, String) -> Unit,
    onToggleCountry: (CountryRegionEntity) -> Unit
) {
    BackHandler { onBack() }

    if (state.currentUser?.role != "ADMIN") {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        title = { Text("Restricted") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    title = "Administrator Access Required",
                    subtitle = "Only authorized administrators can access moderation, verification review, and platform management tools.",
                    icon = Icons.Rounded.AdminPanelSettings
                )
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var userSearchQuery by remember { mutableStateOf("") }
    var showAddCountryDialog by remember { mutableStateOf(false) }

    val filteredUsers = remember(state.allUsers, state.allProfiles, userSearchQuery) {
        val q = userSearchQuery.trim()
        if (q.isBlank()) state.allUsers
        else state.allUsers.filter { user ->
            val profile = state.allProfiles.find { it.userId == user.id }
            (profile?.fullName?.contains(q, true) == true) ||
                user.email.contains(q, true) ||
                (profile?.country?.contains(q, true) == true)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { Text("Admin Console") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Users (${state.allUsers.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Verify (${state.allVerifications.count { it.status == "PENDING" }})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Moderation (${state.allReports.count { it.status == "OPEN" }})") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Regions") })
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AdminStatCell(state.allUsers.size.toString(), "Accounts", Modifier.weight(1f))
                                    VerticalDivider(modifier = Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    AdminStatCell(state.allJobs.size.toString(), "Jobs", Modifier.weight(1f))
                                    VerticalDivider(modifier = Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    AdminStatCell(state.allStartupOpportunities.size.toString(), "Roles", Modifier.weight(1f))
                                    VerticalDivider(modifier = Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    AdminStatCell(state.myTrials.size.toString(), "Trials", Modifier.weight(1f))
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = userSearchQuery,
                                onValueChange = { userSearchQuery = it },
                                placeholder = { Text("Search users by name, email, or country") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (userSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { userSearchQuery = "" }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        items(filteredUsers, key = { it.id }) { user ->
                            val profile = state.allProfiles.find { it.userId == user.id }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(profile?.fullName ?: user.email, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${FormatUtils.humanizeEnum(user.accountType)} · ${FormatUtils.humanizeEnum(user.role)} · ${profile?.country.orEmpty()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (user.role != "ADMIN") {
                                        OutlinedButton(onClick = { onToggleUserSuspend(user) }) {
                                            Text(if (user.isSuspended) "Reactivate" else "Suspend")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (state.allVerifications.isEmpty()) {
                            item { EmptyStateCard("No verification submissions", "Member credentials submitted for review will appear here.", icon = Icons.Rounded.VerifiedUser) }
                        } else {
                            items(state.allVerifications, key = { it.id }) { sub ->
                                val profile = state.allProfiles.find { it.userId == sub.userId }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "${profile?.fullName ?: sub.userId} · ${FormatUtils.humanizeEnum(sub.verificationType)} (${FormatUtils.humanizeEnum(sub.status)})",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text("Evidence: ${sub.evidenceText}", style = MaterialTheme.typography.bodySmall)
                                        if (sub.referenceContact.isNotBlank()) {
                                            Text("Ref: ${sub.referenceContact}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (sub.status == "PENDING") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { onReviewVerification(sub, false, "Rejected by admin") },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Reject") }
                                                Button(
                                                    onClick = { onReviewVerification(sub, true, "Verified by admin") },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald)
                                                ) { Text("Approve Badge") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        item { Text("Safety Reports", style = MaterialTheme.typography.titleMedium) }
                        if (state.allReports.isEmpty()) {
                            item { EmptyStateCard("No open reports", "Reported profiles or conversations will appear here for review.", icon = Icons.Rounded.Flag) }
                        } else {
                            items(state.allReports, key = { it.id }) { rep ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "${FormatUtils.humanizeEnum(rep.targetType)} · ${FormatUtils.humanizeEnum(rep.status)}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(rep.reason, style = MaterialTheme.typography.bodySmall)
                                        if (rep.status == "OPEN") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { onResolveReport(rep.id, "DISMISSED") }) { Text("Dismiss") }
                                                Button(onClick = { onResolveReport(rep.id, "RESOLVED") }, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                                                    Text("Resolve")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Text("Job Listings Moderation", style = MaterialTheme.typography.titleMedium) }
                        items(state.allJobs, key = { it.id }) { job ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${job.title} · ${FormatUtils.humanizeEnum(job.moderationStatus)}", style = MaterialTheme.typography.titleSmall)
                                    Text("${job.organization} · ${job.city}, ${job.country}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { onModerateJob(job.id, "APPROVED") }) { Text("Approve") }
                                        OutlinedButton(onClick = { onModerateJob(job.id, "REMOVED") }) { Text("Remove", color = ErrorCrimson) }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("African Launch & Expansion Countries", style = MaterialTheme.typography.titleMedium)
                                Button(onClick = { showAddCountryDialog = true }) { Text("Add Country") }
                            }
                        }
                        items(state.countries, key = { it.code }) { country ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${country.name} (${country.code})", style = MaterialTheme.typography.titleSmall)
                                        Text("Cities: ${country.cities}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = country.isActive, onCheckedChange = { onToggleCountry(country) })
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Storage & Environment", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.linkUpColors.accentText)
                                    Text(
                                        "External cloud sync and deployment environment settings are documented in BACKEND_AND_DEPLOYMENT.md.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCountryDialog) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var cities by remember { mutableStateOf("") }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCountryDialog = false },
            title = { Text("Add Expansion Country") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (validationError != null) {
                        Text(validationError!!, style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
                    }
                    OutlinedTextField(code, { code = it; validationError = null }, label = { Text("ISO Code (e.g. ZA) *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(name, { name = it; validationError = null }, label = { Text("Country Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cities, { cities = it }, label = { Text("Cities (comma-separated)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (code.isBlank() || name.isBlank()) {
                        validationError = "Please enter both an ISO code and a country name."
                    } else {
                        onAddCountry(code.trim().uppercase(), name.trim(), cities.trim())
                        showAddCountryDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddCountryDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminStatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.linkUpColors.accentText)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
