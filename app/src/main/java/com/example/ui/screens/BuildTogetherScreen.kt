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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.CollaborationTaskEntity
import com.example.data.local.TrialCollaborationEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildTogetherScreen(
    state: LinkUpUiState,
    initialPartnerId: String?,
    onBack: () -> Unit,
    onCreateTrial: (partnerId: String, title: String, desc: String, responsibilities: String) -> Unit,
    onAddTask: (trialId: String, title: String, assignee: String, dueLabel: String) -> Unit,
    onToggleTask: (CollaborationTaskEntity) -> Unit,
    onSaveNotes: (TrialCollaborationEntity, String) -> Unit,
    onSubmitReview: (TrialCollaborationEntity, Boolean, Boolean, Boolean, Boolean, String) -> Unit
) {
    BackHandler { onBack() }
    val uid = state.currentUser?.id ?: return
    var showCreateDialog by remember { mutableStateOf(initialPartnerId != null) }
    var reviewingTrial by remember { mutableStateOf<TrialCollaborationEntity?>(null) }
    var addingTaskTrialId by remember { mutableStateOf<String?>(null) }

    val connectedPartners = remember(state.myConnections, state.allProfiles) {
        state.myConnections.filter { it.status == "ACCEPTED" }.mapNotNull { conn ->
            val pid = if (conn.senderId == uid) conn.receiverId else conn.senderId
            state.allProfiles.find { it.userId == pid }
        }.distinctBy { it.userId }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { Text("Build Together · 14-Day Trials") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (connectedPartners.isNotEmpty()) {
                            IconButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.testTag("start_trial_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Start Trial", tint = ElectricBlue)
                            }
                        }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = MaterialTheme.linkUpColors.accentBlue, modifier = Modifier.size(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("14-Day Collaboration Sprint", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.linkUpColors.accentText)
                                Text(
                                    "Work through a structured milestone together before making a long-term commitment. Trials do not create a legal partnership or equity agreement.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (state.myTrials.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Build Together trials yet",
                            subtitle = if (connectedPartners.isEmpty()) "Accept a mutual connection first to start a 14-day trial sprint."
                            else "Kick off a 14-day sprint with one of your connected partners.",
                            actionLabel = if (connectedPartners.isNotEmpty()) "Start 14-Day Trial" else null,
                            onAction = if (connectedPartners.isNotEmpty()) { { showCreateDialog = true } } else null,
                            icon = Icons.Rounded.RocketLaunch
                        )
                    }
                } else {
                    items(state.myTrials, key = { it.id }) { trial ->
                        val partnerId = if (trial.userAId == uid) trial.userBId else trial.userAId
                        val partner = state.allProfiles.find { it.userId == partnerId }
                        val tasks = state.allTrialTasks.filter { it.trialId == trial.id }
                        val completedCount = tasks.count { it.isCompleted }
                        val myReview = state.allTrialReviews.find { it.trialId == trial.id && it.reviewerId == uid }
                        val daysElapsed = (((System.currentTimeMillis() - trial.startDateMillis) / 86_400_000L) + 1).coerceIn(1, 14)

                        var notesDraft by remember(trial.id, trial.notes) { mutableStateOf(trial.notes) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    Text(trial.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                    Surface(
                                        color = if (trial.status == "COMPLETED") MaterialTheme.linkUpColors.successBg else MaterialTheme.linkUpColors.accentContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (trial.status == "COMPLETED") "Completed" else "Day $daysElapsed of 14",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (trial.status == "COMPLETED") MaterialTheme.linkUpColors.successFg else MaterialTheme.linkUpColors.onAccentContainer
                                        )
                                    }
                                }
                                Text("With ${partner?.fullName ?: "Co-Builder"}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.linkUpColors.accentBlue)
                                Text(trial.sharedProjectDescription, style = MaterialTheme.typography.bodyMedium)
                                Text("Responsibilities: ${trial.agreedResponsibilities}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                if (tasks.isNotEmpty()) {
                                    LinearProgressIndicator(
                                        progress = { completedCount.toFloat() / tasks.size.toFloat() },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = VerifiedEmerald,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Sprint Tasks ($completedCount/${tasks.size})", style = MaterialTheme.typography.titleSmall)
                                    TextButton(onClick = { addingTaskTrialId = trial.id }) { Text("Add Task") }
                                }

                                tasks.forEach { task ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleTask(task) })
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text("${task.assigneeName} · Due ${task.dueDateLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = notesDraft,
                                    onValueChange = { notesDraft = it },
                                    label = { Text("Collaboration Notes") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onSaveNotes(trial, notesDraft) }, modifier = Modifier.weight(1f)) {
                                        Text("Save Notes")
                                    }
                                    Button(
                                        onClick = { reviewingTrial = trial },
                                        enabled = myReview == null,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                    ) {
                                        Text(if (myReview == null) "Final Review" else "Reviewed")
                                    }
                                }
                                if (myReview != null) {
                                    Surface(color = MaterialTheme.linkUpColors.successBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "Review recorded · Long-term co-founder interest: ${if (myReview.considerLongTermCofounder) "Yes" else "Not yet"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.linkUpColors.successFg,
                                            modifier = Modifier.padding(10.dp)
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

    if (showCreateDialog && connectedPartners.isNotEmpty()) {
        var partnerId by remember { mutableStateOf(initialPartnerId ?: connectedPartners.first().userId) }
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var resp by remember { mutableStateOf("") }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Start 14-Day Build Together Trial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (validationError != null) {
                        Text(validationError!!, style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
                    }
                    Text("Select Partner", style = MaterialTheme.typography.labelSmall)
                    connectedPartners.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = partnerId == p.userId, onClick = { partnerId = p.userId })
                            Text(p.fullName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(title, { title = it; validationError = null }, label = { Text("Project Title *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(desc, { desc = it; validationError = null }, label = { Text("Shared Project Description *") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(resp, { resp = it }, label = { Text("Agreed Responsibilities") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || desc.isBlank()) {
                            validationError = "Please enter both a Project Title and Description."
                        } else {
                            onCreateTrial(partnerId, title.trim(), desc.trim(), resp.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) { Text("Launch 14-Day Trial") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    addingTaskTrialId?.let { trialId ->
        var taskTitle by remember { mutableStateOf("") }
        var assignee by remember { mutableStateOf(state.currentProfile?.fullName ?: "Founder") }
        var due by remember { mutableStateOf("Day 7") }
        var taskError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { addingTaskTrialId = null },
            title = { Text("Assign Sprint Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        taskTitle,
                        { taskTitle = it; taskError = null },
                        label = { Text("Task Title *") },
                        isError = taskError != null,
                        supportingText = taskError?.let { msg -> { Text(msg) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(assignee, { assignee = it }, label = { Text("Assignee") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(due, { due = it }, label = { Text("Deadline (e.g. Day 10)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (taskTitle.isBlank()) {
                        taskError = "Please enter a task title."
                    } else {
                        onAddTask(trialId, taskTitle.trim(), assignee.trim(), due.trim())
                        addingTaskTrialId = null
                    }
                }) { Text("Add Task") }
            },
            dismissButton = { TextButton(onClick = { addingTaskTrialId = null }) { Text("Cancel") } }
        )
    }

    reviewingTrial?.let { trial ->
        var q1 by remember { mutableStateOf(true) }
        var q2 by remember { mutableStateOf(true) }
        var q3 by remember { mutableStateOf(true) }
        var q4 by remember { mutableStateOf(true) }
        var comments by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { reviewingTrial = null },
            title = { Text("14-Day Trial Review") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReviewQuestionRow("Did the other person meet agreed commitments?", q1) { q1 = it }
                    ReviewQuestionRow("Was communication effective?", q2) { q2 = it }
                    ReviewQuestionRow("Did you work well together?", q3) { q3 = it }
                    ReviewQuestionRow("Would you consider becoming long-term co-founders?", q4) { q4 = it }
                    OutlinedTextField(comments, { comments = it }, label = { Text("Final reflections") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Submitting feedback does not create a legal partnership or equity commitment.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitReview(trial, q1, q2, q3, q4, comments)
                        reviewingTrial = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) { Text("Submit Review") }
            },
            dismissButton = { TextButton(onClick = { reviewingTrial = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ReviewQuestionRow(question: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(question, style = MaterialTheme.typography.bodySmall)
    }
}
