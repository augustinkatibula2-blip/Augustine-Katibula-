package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.local.ConnectionRequestEntity
import com.example.data.local.ConversationEntity
import com.example.domain.FormatUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InitialsOrPhotoAvatar
import com.example.ui.components.VerificationBadgeChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsAndChatScreen(
    state: LinkUpUiState,
    onSelectConversation: (String?) -> Unit,
    onSendMessage: (ConversationEntity, String) -> Unit,
    onRespondConnection: (ConnectionRequestEntity, Boolean) -> Unit,
    onToggleBlock: (ConversationEntity) -> Unit,
    onReportUser: (String, String) -> Unit,
    onStartTrialWithPartner: (String) -> Unit,
    onRefresh: () -> Unit = {},
    onRequireAuth: () -> Unit = {},
    onChatDetailActive: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uid = state.currentUser?.id
    if (uid == null) {
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Messages & Connections", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Direct messaging and connection requests with verified members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "Sign in to message matches",
                    subtitle = "Create an account or sign in to chat with co-founders, employers, and startup teams.",
                    actionLabel = "Sign In or Register",
                    onAction = onRequireAuth,
                    icon = Icons.Rounded.ChatBubbleOutline
                )
            }
        }
        return
    }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Messages, 1=Connections
    var activeConvId by remember { mutableStateOf<String?>(null) }

    val activeConversation = state.myConversations.find { it.id == activeConvId }
    val isInChat = activeConversation != null

    LaunchedEffect(isInChat) {
        onChatDetailActive(isInChat)
    }
    DisposableEffect(Unit) {
        onDispose { onChatDetailActive(false) }
    }

    if (activeConversation != null) {
        BackHandler {
            activeConvId = null
            onSelectConversation(null)
        }
        val partnerId = if (activeConversation.participantAId == uid) activeConversation.participantBId else activeConversation.participantAId
        val partnerProfile = state.allProfiles.find { it.userId == partnerId }
        ChatDetailScreen(
            conversation = activeConversation,
            partnerName = partnerProfile?.fullName ?: "LinkUp Member",
            partnerHeadline = partnerProfile?.headline.orEmpty(),
            partnerPhoto = partnerProfile?.photoUri,
            partnerShareContact = partnerProfile?.shareContactWithMatches == true,
            partnerPhone = partnerProfile?.phone.orEmpty(),
            currentUserId = uid,
            lowDataMode = state.lowDataMode,
            messages = state.activeConversationMessages,
            onBack = {
                activeConvId = null
                onSelectConversation(null)
            },
            onSend = { text -> onSendMessage(activeConversation, text) },
            onToggleBlock = { onToggleBlock(activeConversation) },
            onReport = { reason -> onReportUser(partnerId, reason) },
            onStartTrial = { onStartTrialWithPartner(partnerId) }
        )
        return
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
                    Text("Messages & Connections", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Phone numbers stay private unless shared by a member.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Messages (${state.myConversations.size})") }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Connections (${state.myConnections.size})") }
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            if (selectedTab == 0) {
                if (state.myConversations.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No conversations yet",
                            subtitle = "Accept a connection request or get a mutual co-founder match to start messaging.",
                            icon = Icons.Rounded.ChatBubbleOutline
                        )
                    }
                } else {
                    items(state.myConversations, key = { it.id }) { conv ->
                        val partnerId = if (conv.participantAId == uid) conv.participantBId else conv.participantAId
                        val partner = state.allProfiles.find { it.userId == partnerId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeConvId = conv.id
                                    onSelectConversation(conv.id)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InitialsOrPhotoAvatar(
                                    fullName = partner?.fullName ?: "Member",
                                    photoUri = partner?.photoUri,
                                    lowDataMode = state.lowDataMode
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(partner?.fullName ?: "Member", style = MaterialTheme.typography.titleMedium)
                                        Text(formatShortTime(conv.lastTimestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = if (conv.isBlocked) "Blocked conversation" else conv.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (conv.isBlocked) MaterialTheme.linkUpColors.errorFg else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val incoming = state.myConnections.filter { it.receiverId == uid && it.status == "PENDING" }
                val others = state.myConnections.filter { !(it.receiverId == uid && it.status == "PENDING") }

                if (incoming.isNotEmpty()) {
                    item { Text("Incoming Requests", style = MaterialTheme.typography.titleMedium) }
                    items(incoming, key = { it.id }) { req ->
                        val sender = state.allProfiles.find { it.userId == req.senderId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    InitialsOrPhotoAvatar(sender?.fullName ?: "Member", sender?.photoUri, state.lowDataMode, size = 44.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sender?.fullName ?: "Member", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${FormatUtils.humanizeEnum(req.contextType)} · ${sender?.city.orEmpty()}, ${sender?.country.orEmpty()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    VerificationBadgeChip(sender?.verificationStatus ?: "UNVERIFIED")
                                }
                                if (req.note.isNotBlank()) Text(req.note, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onRespondConnection(req, false) }, modifier = Modifier.weight(1f)) {
                                        Text("Decline")
                                    }
                                    Button(
                                        onClick = { onRespondConnection(req, true) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                    ) {
                                        Text("Accept")
                                    }
                                }
                            }
                        }
                    }
                }

                item { Text("All Connections", style = MaterialTheme.typography.titleMedium) }
                if (others.isEmpty() && incoming.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No connections yet",
                            subtitle = "Swipe on co-founders or reach out to startup teams to connect.",
                            icon = Icons.Rounded.Handshake
                        )
                    }
                } else {
                    items(others, key = { it.id }) { req ->
                        val partnerId = if (req.senderId == uid) req.receiverId else req.senderId
                        val partner = state.allProfiles.find { it.userId == partnerId }
                        val matchingConv = state.myConversations.find {
                            (it.participantAId == uid && it.participantBId == partnerId) ||
                                (it.participantBId == uid && it.participantAId == partnerId)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    InitialsOrPhotoAvatar(partner?.fullName ?: "Member", partner?.photoUri, state.lowDataMode, size = 44.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(partner?.fullName ?: "Member", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${FormatUtils.humanizeEnum(req.contextType)} · ${FormatUtils.humanizeEnum(req.status)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (req.status == "ACCEPTED") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (matchingConv != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    activeConvId = matchingConv.id
                                                    onSelectConversation(matchingConv.id)
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Message")
                                            }
                                        }
                                        FilledTonalButton(
                                            onClick = { onStartTrialWithPartner(partnerId) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("14d Trial")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailScreen(
    conversation: ConversationEntity,
    partnerName: String,
    partnerHeadline: String,
    partnerPhoto: String?,
    partnerShareContact: Boolean,
    partnerPhone: String,
    currentUserId: String,
    lowDataMode: Boolean,
    messages: List<com.example.data.local.MessageEntity>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onToggleBlock: () -> Unit,
    onReport: (String) -> Unit,
    onStartTrial: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InitialsOrPhotoAvatar(partnerName, partnerPhoto, lowDataMode, size = 36.dp)
                            Column {
                                Text(partnerName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (partnerShareContact && partnerPhone.isNotBlank()) "Phone: $partnerPhone" else partnerHeadline.take(36),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = onStartTrial,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("14d Trial", style = MaterialTheme.typography.labelMedium)
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Conversation options")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (conversation.isBlocked) "Unblock Member" else "Block Member") },
                                    leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onToggleBlock()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report Conversation", color = ErrorCrimson) },
                                    leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null, tint = ErrorCrimson) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showReportDialog = true
                                    }
                                )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp)
                    .imePadding()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMine = msg.senderId == currentUserId
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                color = if (isMine) ElectricBlue else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = msg.body,
                                        color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = formatShortTime(msg.timestamp),
                                            color = if (isMine) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        if (isMine) {
                                            Icon(
                                                imageVector = if (msg.isRead) Icons.Rounded.DoneAll else Icons.Rounded.Done,
                                                contentDescription = if (msg.isRead) "Read" else "Sent",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (conversation.isBlocked) {
                    Surface(color = MaterialTheme.linkUpColors.errorBg, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Messaging disabled because this conversation is blocked.",
                            color = MaterialTheme.linkUpColors.errorFg,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = { Text("Write a professional message...") },
                            modifier = Modifier.weight(1f).testTag("chat_message_input"),
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (draft.isNotBlank()) {
                                        onSend(draft.trim())
                                        draft = ""
                                    }
                                }
                            )
                        )
                        IconButton(
                            onClick = {
                                if (draft.isNotBlank()) {
                                    onSend(draft.trim())
                                    draft = ""
                                }
                            },
                            modifier = Modifier.size(48.dp).testTag("chat_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = ElectricBlue)
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("") }
        var reasonError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Conversation") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it; reasonError = null },
                    label = { Text("Describe policy or safety issue") },
                    isError = reasonError != null,
                    supportingText = reasonError?.let { msg -> { Text(msg) } },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reason.isBlank()) {
                            reasonError = "Please provide a brief description."
                        } else {
                            onReport(reason.trim())
                            showReportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson)
                ) { Text("Submit Report") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun formatShortTime(ts: Long): String {
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ts))
}
