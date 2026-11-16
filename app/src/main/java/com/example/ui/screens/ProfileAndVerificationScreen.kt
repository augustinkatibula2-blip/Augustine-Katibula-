package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.UserProfileEntity
import com.example.domain.FormatUtils
import com.example.domain.LinkUpLocale
import com.example.platform.LinkUpNotificationHelper
import com.example.ui.components.InitialsOrPhotoAvatar
import com.example.ui.components.VerificationBadgeChip
import com.example.ui.theme.*
import com.example.ui.viewmodel.LinkUpUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileAndVerificationScreen(
    state: LinkUpUiState,
    onBack: () -> Unit,
    onSaveProfile: (UserProfileEntity) -> Unit,
    onSubmitVerification: (type: String, evidence: String, referenceContact: String) -> Unit,
    onToggleLowData: (Boolean) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onChangeThemeMode: (AppThemeMode) -> Unit = {},
    onOpenOnboarding: () -> Unit,
    onOpenAbout: () -> Unit,
    onLogout: () -> Unit
) {
    BackHandler { onBack() }
    val profile = state.currentProfile ?: return
    val uid = state.currentUser?.id ?: return
    val context = LocalContext.current

    var fullName by remember(profile.userId) { mutableStateOf(profile.fullName) }
    var headline by remember(profile.userId) { mutableStateOf(profile.headline) }
    var photoUri by remember(profile.userId) { mutableStateOf(profile.photoUri) }
    var country by remember(profile.userId) { mutableStateOf(profile.country) }
    var city by remember(profile.userId) { mutableStateOf(profile.city) }
    var phone by remember(profile.userId) { mutableStateOf(profile.phone) }
    var shareContact by remember(profile.userId) { mutableStateOf(profile.shareContactWithMatches) }
    var skills by remember(profile.userId) { mutableStateOf(profile.skills) }
    var interests by remember(profile.userId) { mutableStateOf(profile.interests) }
    var experienceSummary by remember(profile.userId) { mutableStateOf(profile.experienceSummary) }
    var portfolioUrl by remember(profile.userId) { mutableStateOf(profile.portfolioUrl) }
    var languages by remember(profile.userId) { mutableStateOf(profile.languages) }
    var nameError by remember { mutableStateOf<String?>(null) }

    var showVerifySheet by remember { mutableStateOf(false) }
    var deviceAlertsGranted by remember {
        mutableStateOf(LinkUpNotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        deviceAlertsGranted = granted || LinkUpNotificationHelper.hasNotificationPermission(context)
        if (deviceAlertsGranted) LinkUpNotificationHelper.ensureChannel(context)
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    val myVerifications = state.allVerifications.filter { it.userId == uid }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { Text("Profile & Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = onLogout, modifier = Modifier.testTag("logout_button")) {
                            Text("Sign Out", color = MaterialTheme.linkUpColors.errorFg)
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
                    .widthIn(max = 820.dp)
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                InitialsOrPhotoAvatar(fullName, photoUri, state.lowDataMode, size = 60.dp)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(fullName.ifBlank { profile.fullName }, style = MaterialTheme.typography.titleLarge)
                                    VerificationBadgeChip(profile.verificationStatus)
                                }
                                OutlinedButton(onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) {
                                    Text("Photo")
                                }
                            }

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it; nameError = null },
                                label = { Text("Full Name") },
                                isError = nameError != null,
                                supportingText = nameError?.let { msg -> { Text(msg) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(headline, { headline = it }, label = { Text("Headline") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(country, { country = it }, label = { Text("Country") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(city, { city = it }, label = { Text("City") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(
                                phone,
                                { phone = it },
                                label = { Text("Phone (private by default)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = shareContact, onCheckedChange = { shareContact = it })
                                Spacer(Modifier.width(8.dp))
                                Text("Share phone with mutual matches", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedTextField(skills, { skills = it }, label = { Text("Skills") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(interests, { interests = it }, label = { Text("Interests") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(experienceSummary, { experienceSummary = it }, label = { Text("Experience") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(
                                portfolioUrl,
                                { portfolioUrl = it },
                                label = { Text("Portfolio / LinkedIn URL") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(languages, { languages = it }, label = { Text("Languages Spoken") }, modifier = Modifier.fillMaxWidth())

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onOpenOnboarding, modifier = Modifier.weight(1f)) {
                                    Text("Guided Setup")
                                }
                                Button(
                                    onClick = {
                                        if (fullName.isBlank()) {
                                            nameError = "Full name cannot be empty."
                                        } else {
                                            onSaveProfile(
                                                profile.copy(
                                                    fullName = fullName.trim(),
                                                    headline = headline.trim(),
                                                    photoUri = photoUri,
                                                    country = country.trim(),
                                                    city = city.trim(),
                                                    phone = phone.trim(),
                                                    shareContactWithMatches = shareContact,
                                                    skills = skills.trim(),
                                                    interests = interests.trim(),
                                                    experienceSummary = experienceSummary.trim(),
                                                    portfolioUrl = portfolioUrl.trim(),
                                                    languages = languages.trim()
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("save_profile_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                ) {
                                    Text("Save Profile")
                                }
                            }
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
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Verification & Trust", style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "Submit credentials for verification review.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(onClick = { showVerifySheet = true }, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                                    Text("Submit")
                                }
                            }
                            if (myVerifications.isNotEmpty()) {
                                myVerifications.forEach { sub ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                "${FormatUtils.humanizeEnum(sub.verificationType)} · ${FormatUtils.humanizeEnum(sub.status)}",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(sub.evidenceText, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
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
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("App Preferences & Alerts", style = MaterialTheme.typography.titleLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Android Device Alerts", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        if (deviceAlertsGranted) "System notifications enabled for matches & messages."
                                        else "Allow Android notifications for match and message alerts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (deviceAlertsGranted) {
                                    Surface(color = MaterialTheme.linkUpColors.successBg, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "Active",
                                            color = MaterialTheme.linkUpColors.successFg,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                } else {
                                    FilledTonalButton(onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            LinkUpNotificationHelper.ensureChannel(context)
                                            deviceAlertsGranted = true
                                        }
                                    }) {
                                        Text("Enable")
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Appearance", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Choose Light, Dark, or follow your Android system setting.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    AppThemeMode.entries.forEachIndexed { index, mode ->
                                        val icon = when (mode) {
                                            AppThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                            AppThemeMode.LIGHT -> Icons.Rounded.LightMode
                                            AppThemeMode.DARK -> Icons.Rounded.DarkMode
                                        }
                                        SegmentedButton(
                                            selected = state.themeMode == mode,
                                            onClick = { onChangeThemeMode(mode) },
                                            shape = SegmentedButtonDefaults.itemShape(index, AppThemeMode.entries.size),
                                            modifier = Modifier.testTag("theme_mode_${mode.code.lowercase()}"),
                                            icon = {
                                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        ) {
                                            Text(mode.label, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Low-Data Mode", style = MaterialTheme.typography.titleSmall)
                                    Text("Uses lightweight initials instead of loading photo thumbnails.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = state.lowDataMode, onCheckedChange = onToggleLowData)
                            }
                            Text("App Language", style = MaterialTheme.typography.titleSmall)
                            LinkUpLocale.supportedLanguages.forEach { lang ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = state.languageCode == lang.code, onClick = { onChangeLanguage(lang.code) })
                                    Text(
                                        text = if (lang.status == "Active") lang.displayName else "${lang.displayName} · ${lang.status}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            OutlinedButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) {
                                Text("About LinkUp & Founder")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVerifySheet) {
        var verType by remember { mutableStateOf("PORTFOLIO") }
        var evidence by remember { mutableStateOf("") }
        var refContact by remember { mutableStateOf("") }
        var evidenceError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(onDismissRequest = { showVerifySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Submit Verification Evidence", style = MaterialTheme.typography.titleLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("EMAIL", "PHONE", "EDUCATION", "PROFESSIONAL", "PORTFOLIO", "REFERENCE").forEach { t ->
                        FilterChip(
                            selected = verType == t,
                            onClick = { verType = t },
                            label = { Text(FormatUtils.humanizeEnum(t)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = evidence,
                    onValueChange = { evidence = it; evidenceError = null },
                    label = { Text("Evidence / Credential / Link *") },
                    isError = evidenceError != null,
                    supportingText = evidenceError?.let { msg -> { Text(msg) } },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    refContact,
                    { refContact = it },
                    label = { Text("Reference Contact (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showVerifySheet = false }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (evidence.isBlank()) {
                                evidenceError = "Please enter credential details or a link."
                            } else {
                                onSubmitVerification(verType, evidence.trim(), refContact.trim())
                                showVerifySheet = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Submit for Review")
                    }
                }
            }
        }
    }
}
