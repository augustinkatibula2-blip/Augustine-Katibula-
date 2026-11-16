package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.platform.LinkUpNotificationHelper
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: LinkUpUiState,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenJobs: () -> Unit,
    onOpenCofounders: () -> Unit,
    onRequireAuth: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var deviceAlertsEnabled by remember {
        mutableStateOf(LinkUpNotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        deviceAlertsEnabled = granted || LinkUpNotificationHelper.hasNotificationPermission(context)
        if (deviceAlertsEnabled) {
            LinkUpNotificationHelper.ensureChannel(context)
        }
    }

    val unreadCount = state.notifications.count { !it.isRead }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { Text("Notifications") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (unreadCount > 0) {
                            TextButton(onClick = onMarkAllRead) {
                                Text("Mark all read", color = MaterialTheme.linkUpColors.accentBlue)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!deviceAlertsEnabled) {
                    item {
                        Surface(
                            color = MaterialTheme.linkUpColors.accentContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.linkUpColors.accentBlue.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.linkUpColors.onAccentContainer)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Enable device alerts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.linkUpColors.accentText)
                                    Text(
                                        "Receive Android system alerts for mutual matches and incoming messages.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            LinkUpNotificationHelper.ensureChannel(context)
                                            deviceAlertsEnabled = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Allow")
                                }
                            }
                        }
                    }
                }

                if (state.currentUser == null) {
                    item {
                        EmptyStateCard(
                            title = "Sign in to view activity alerts",
                            subtitle = "Track co-founder matches, messages, and job application updates in one place.",
                            actionLabel = "Sign In or Register",
                            onAction = onRequireAuth,
                            icon = Icons.Rounded.NotificationsNone
                        )
                    }
                } else if (state.notifications.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "You're all caught up",
                            subtitle = "Matches, connection responses, and application status updates will appear here.",
                            icon = Icons.Rounded.NotificationsNone
                        )
                    }
                } else {
                    items(state.notifications, key = { it.id }) { notif ->
                        val icon = when (notif.type.uppercase()) {
                            "MATCH", "CONNECTION" -> Icons.Rounded.Handshake
                            "JOB" -> Icons.Rounded.WorkOutline
                            "MESSAGE" -> Icons.Rounded.ChatBubbleOutline
                            else -> Icons.Rounded.Notifications
                        }
                        Surface(
                            color = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.linkUpColors.accentContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (notif.isRead) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.linkUpColors.accentBlue.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (notif.type.uppercase()) {
                                        "MESSAGE", "CONNECTION" -> onOpenMessages()
                                        "JOB" -> onOpenJobs()
                                        "MATCH" -> onOpenCofounders()
                                        else -> {}
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.linkUpColors.accentContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (notif.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.linkUpColors.onAccentContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(notif.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                        Text(
                                            formatNotifTime(notif.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        notif.body,
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
}

private fun formatNotifTime(ts: Long): String {
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ts))
}
