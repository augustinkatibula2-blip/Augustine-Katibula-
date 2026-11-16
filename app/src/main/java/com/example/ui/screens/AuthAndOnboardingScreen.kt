package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.CountryRegionEntity
import com.example.data.local.FounderPreferenceEntity
import com.example.data.local.UserProfileEntity
import com.example.domain.AccountType
import com.example.domain.FormatUtils
import com.example.ui.components.InitialsOrPhotoAvatar
import com.example.ui.components.LinkUpLogo
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    countries: List<CountryRegionEntity>,
    authError: String?,
    onClearError: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, password: String, fullName: String, type: AccountType, country: String, city: String) -> Unit,
    onDemoLogin: (userId: String) -> Unit,
    onBackToLanding: () -> Unit
) {
    BackHandler { onBackToLanding() }

    var isRegisterTab by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.FOUNDER) }
    var selectedCountry by remember { mutableStateOf("Malawi") }
    var city by remember { mutableStateOf("Lilongwe") }
    var localError by remember { mutableStateOf<String?>(null) }
    var showDemoSheet by remember { mutableStateOf(false) }

    val displayedError = localError ?: authError

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { LinkUpLogo() },
                    navigationIcon = {
                        IconButton(onClick = onBackToLanding) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                    .widthIn(max = 560.dp)
                    .imePadding(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isRegisterTab) "Create your LinkUp account" else "Sign in to LinkUp",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = if (isRegisterTab) "Choose your role and connect across Africa."
                            else "Continue building your network, venture, or career.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !isRegisterTab,
                            onClick = { isRegisterTab = false; localError = null; onClearError() },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("tab_login")
                        ) { Text("Sign In") }
                        SegmentedButton(
                            selected = isRegisterTab,
                            onClick = { isRegisterTab = true; localError = null; onClearError() },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("tab_register")
                        ) { Text("Create Account") }
                    }
                }

                if (!displayedError.isNullOrBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.linkUpColors.errorBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.linkUpColors.errorFg.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayedError,
                                color = MaterialTheme.linkUpColors.errorFg,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isRegisterTab) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it; localError = null },
                                    label = { Text("Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("register_name_input")
                                )
                            }
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; localError = null; onClearError() },
                                label = { Text("Email Address") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; localError = null; onClearError() },
                                label = { Text("Password") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (isRegisterTab) ImeAction.Next else ImeAction.Done
                                ),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = if (showPassword) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                            )

                            if (isRegisterTab) {
                                Text("Account Type", style = MaterialTheme.typography.titleSmall)
                                AccountType.entries.forEach { type ->
                                    val selected = selectedType == type
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedType = type },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) MaterialTheme.linkUpColors.accentContainer else MaterialTheme.colorScheme.background,
                                        border = BorderStroke(1.dp, if (selected) MaterialTheme.linkUpColors.accentBlue else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = selected, onClick = { selectedType = type })
                                            Spacer(Modifier.width(6.dp))
                                            Column {
                                                Text(type.label, style = MaterialTheme.typography.titleSmall)
                                                Text(type.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                var countryExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = countryExpanded,
                                    onExpandedChange = { countryExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedCountry,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Country") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = countryExpanded,
                                        onDismissRequest = { countryExpanded = false }
                                    ) {
                                        countries.filter { it.isActive }.forEach { country ->
                                            DropdownMenuItem(
                                                text = { Text(country.name) },
                                                onClick = {
                                                    selectedCountry = country.name
                                                    city = country.cities.split(",").firstOrNull()?.trim().orEmpty()
                                                    countryExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it; localError = null },
                                    label = { Text("City / Town") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = {
                                    val trimmedEmail = email.trim()
                                    when {
                                        isRegisterTab && fullName.isBlank() -> {
                                            localError = "Please enter your full name."
                                        }
                                        !trimmedEmail.contains("@") || !trimmedEmail.contains(".") -> {
                                            localError = "Please enter a valid email address."
                                        }
                                        password.length < 6 -> {
                                            localError = "Password must be at least 6 characters."
                                        }
                                        isRegisterTab && city.isBlank() -> {
                                            localError = "Please enter your city or town."
                                        }
                                        isRegisterTab -> {
                                            onRegister(trimmedEmail, password, fullName.trim(), selectedType, selectedCountry, city.trim())
                                        }
                                        else -> {
                                            onLogin(trimmedEmail, password)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("auth_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isRegisterTab) "Continue" else "Sign In")
                            }
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(
                            onClick = { showDemoSheet = true },
                            modifier = Modifier.testTag("open_demo_accounts_button")
                        ) {
                            Text("Preview with a sample account", color = MaterialTheme.linkUpColors.accentBlue)
                        }
                    }
                }
            }
        }
    }

    if (showDemoSheet) {
        ModalBottomSheet(onDismissRequest = { showDemoSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select a Sample Account", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Explore LinkUp workflows across roles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                val demos = listOf(
                    "sample_founder_1" to "Chisomo Banda · AgriTech Founder (Lilongwe)",
                    "sample_founder_2" to "Tadziona Phiri · Android Dev Co-Founder (Blantyre)",
                    "sample_seeker_1" to "Limbani Chirwa · Job Seeker (Lilongwe)",
                    "sample_employer_1" to "NyasaTech Labs · Employer (Blantyre)",
                    "user_admin" to "Augustine Katibula · Founder & Admin"
                )
                demos.forEach { (id, label) ->
                    OutlinedButton(
                        onClick = {
                            showDemoSheet = false
                            onDemoLogin(id)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("demo_login_$id"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizardScreen(
    profile: UserProfileEntity,
    founderPrefs: FounderPreferenceEntity,
    onSaveAll: (UserProfileEntity, FounderPreferenceEntity) -> Unit,
    onFinish: () -> Unit
) {
    BackHandler { onFinish() }

    var step by remember { mutableIntStateOf(0) }
    var fullName by remember(profile.userId) { mutableStateOf(profile.fullName) }
    var headline by remember(profile.userId) { mutableStateOf(profile.headline) }
    var photoUri by remember(profile.userId) { mutableStateOf(profile.photoUri) }
    var country by remember(profile.userId) { mutableStateOf(profile.country) }
    var city by remember(profile.userId) { mutableStateOf(profile.city) }
    var phone by remember(profile.userId) { mutableStateOf(profile.phone) }
    var languages by remember(profile.userId) { mutableStateOf(profile.languages) }

    var educationLevel by remember(profile.userId) { mutableStateOf(profile.educationLevel) }
    var institution by remember(profile.userId) { mutableStateOf(profile.institution) }
    var experienceSummary by remember(profile.userId) { mutableStateOf(profile.experienceSummary) }
    var yearsExp by remember(profile.userId) { mutableStateOf(profile.yearsExperience.toString()) }

    var skills by remember(profile.userId) { mutableStateOf(profile.skills) }
    var interests by remember(profile.userId) { mutableStateOf(profile.interests) }
    var careerGoals by remember(profile.userId) { mutableStateOf(profile.careerGoals) }
    var portfolioUrl by remember(profile.userId) { mutableStateOf(profile.portfolioUrl) }
    var workMode by remember(profile.userId) { mutableStateOf(profile.workMode) }

    var industry by remember(founderPrefs.userId) { mutableStateOf(founderPrefs.industry) }
    var businessIdea by remember(founderPrefs.userId) { mutableStateOf(founderPrefs.businessIdea) }
    var skillsNeeded by remember(founderPrefs.userId) { mutableStateOf(founderPrefs.skillsNeeded) }
    var commitmentType by remember(founderPrefs.userId) { mutableStateOf(founderPrefs.commitmentType) }
    var weeklyHours by remember(founderPrefs.userId) { mutableStateOf(founderPrefs.weeklyHoursAvailable.toString()) }
    var equityExp by remember(founderPrefs.userId) { mutableStateOf(founderPrefs.equityExpectation) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = { Text("Profile Setup (${step + 1}/4)") },
                    actions = {
                        TextButton(onClick = onFinish) { Text("Skip") }
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
                    .widthIn(max = 680.dp)
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (step + 1) / 4f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = ElectricBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (step) {
                                    0 -> {
                                        Text("Identity & Location", style = MaterialTheme.typography.titleLarge)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            InitialsOrPhotoAvatar(fullName = fullName, photoUri = photoUri, size = 60.dp)
                                            OutlinedButton(onClick = {
                                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }) {
                                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Add Photo")
                                            }
                                        }
                                        OutlinedTextField(fullName, { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(headline, { headline = it }, label = { Text("Professional Headline") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(country, { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(city, { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(phone, { phone = it }, label = { Text("Phone Number (Optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(languages, { languages = it }, label = { Text("Languages Spoken") }, modifier = Modifier.fillMaxWidth())
                                    }
                                    1 -> {
                                        Text("Education & Experience", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            "Practical experience, self-taught skills, and portfolios count equally.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedTextField(educationLevel, { educationLevel = it }, label = { Text("Education Level") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(institution, { institution = it }, label = { Text("Institution / Learning Path") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(yearsExp, { yearsExp = it.filter(Char::isDigit) }, label = { Text("Years of Experience") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(
                                            experienceSummary,
                                            { experienceSummary = it },
                                            label = { Text("Experience Summary") },
                                            minLines = 3,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    2 -> {
                                        Text("Skills, Interests & Work Mode", style = MaterialTheme.typography.titleLarge)
                                        OutlinedTextField(skills, { skills = it }, label = { Text("Skills (comma-separated)") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(interests, { interests = it }, label = { Text("Interests") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(careerGoals, { careerGoals = it }, label = { Text("Career Goals") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(portfolioUrl, { portfolioUrl = it }, label = { Text("Portfolio or LinkedIn URL") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
                                        Text("Preferred Work Mode", style = MaterialTheme.typography.titleSmall)
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    else -> {
                                        Text("Co-Founder & Venture Preferences", style = MaterialTheme.typography.titleLarge)
                                        OutlinedTextField(industry, { industry = it }, label = { Text("Startup Industry") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(businessIdea, { businessIdea = it }, label = { Text("Business Idea or Problem") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(skillsNeeded, { skillsNeeded = it }, label = { Text("Skills Needed in Co-Founder") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(weeklyHours, { weeklyHours = it.filter(Char::isDigit) }, label = { Text("Weekly Hours Available") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                        Text("Commitment", style = MaterialTheme.typography.titleSmall)
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("FULL_TIME", "PART_TIME", "FLEXIBLE").forEach { c ->
                                                FilterChip(
                                                    selected = commitmentType == c,
                                                    onClick = { commitmentType = c },
                                                    label = { Text(FormatUtils.humanizeEnum(c)) }
                                                )
                                            }
                                        }
                                        OutlinedTextField(equityExp, { equityExp = it }, label = { Text("Equity Expectations") }, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step > 0) {
                        OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            val updatedProfile = profile.copy(
                                fullName = fullName.trim().ifBlank { profile.fullName },
                                headline = headline.trim(),
                                photoUri = photoUri,
                                country = country.trim(),
                                city = city.trim(),
                                phone = phone.trim(),
                                languages = languages.trim(),
                                educationLevel = educationLevel.trim(),
                                institution = institution.trim(),
                                experienceSummary = experienceSummary.trim(),
                                yearsExperience = yearsExp.toIntOrNull() ?: 0,
                                skills = skills.trim(),
                                interests = interests.trim(),
                                careerGoals = careerGoals.trim(),
                                portfolioUrl = portfolioUrl.trim(),
                                workMode = workMode
                            )
                            val updatedPrefs = founderPrefs.copy(
                                industry = industry.trim(),
                                businessIdea = businessIdea.trim(),
                                skillsPossessed = skills.trim(),
                                skillsNeeded = skillsNeeded.trim(),
                                commitmentType = commitmentType,
                                weeklyHoursAvailable = weeklyHours.toIntOrNull() ?: 20,
                                equityExpectation = equityExp.trim()
                            )
                            onSaveAll(updatedProfile, updatedPrefs)
                            if (step < 3) step++ else onFinish()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("onboarding_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(if (step < 3) "Save & Continue" else "Finish Setup")
                    }
                }
            }
        }
    }
}
