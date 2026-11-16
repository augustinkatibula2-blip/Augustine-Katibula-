package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.data.local.LinkUpDatabase
import com.example.data.repository.LinkUpRepository
import com.example.data.repository.SessionManager
import com.example.domain.LinkUpLocale
import com.example.platform.LinkUpNotificationHelper
import com.example.ui.screens.*
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LinkUpTheme
import com.example.ui.theme.linkUpColors
import com.example.ui.viewmodel.LinkUpViewModel

object LinkUpRoutes {
    const val LANDING = "landing"
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val COFOUNDERS = "cofounders"
    const val JOBS = "jobs"
    const val TALENT = "talent"
    const val CONNECTIONS = "connections"
    const val TRIALS = "trials"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val NOTIFICATIONS = "notifications"
    const val ABOUT = "about"
    const val ADMIN = "admin"
}

private data class NavTab(
    val route: String,
    val labelKey: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    private var navControllerRef: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        LinkUpNotificationHelper.ensureChannel(applicationContext)

        val db = LinkUpDatabase.getInstance(applicationContext)
        val repository = LinkUpRepository(db)
        val sessionManager = SessionManager(applicationContext)

        setContent {
            val vm: LinkUpViewModel = viewModel(
                factory = LinkUpViewModel.Factory(repository, sessionManager)
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (uiState.themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            LaunchedEffect(isDarkTheme) {
                val barStyle = if (isDarkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle
                )
            }

            LinkUpTheme(darkTheme = isDarkTheme) {
                LinkUpApp(
                    vm = vm,
                    isDarkTheme = isDarkTheme,
                    onNavReady = { navControllerRef = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navControllerRef?.handleDeepLink(intent)
    }
}

@Composable
fun LinkUpApp(
    vm: LinkUpViewModel,
    isDarkTheme: Boolean = false,
    onNavReady: (NavController) -> Unit = {}
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: LinkUpRoutes.DASHBOARD

    var pendingTrialPartnerId by remember { mutableStateOf<String?>(null) }
    var isInChatDetail by remember { mutableStateOf(false) }

    LaunchedEffect(navController) {
        onNavReady(navController)
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearSnackbar()
        }
    }

    // Dispatch a real local Android notification on mutual match when permitted
    LaunchedEffect(state.mutualMatchName) {
        val matchedName = state.mutualMatchName
        if (!matchedName.isNullOrBlank()) {
            LinkUpNotificationHelper.showLocalNotification(
                context = context,
                notificationId = matchedName.hashCode(),
                title = "Mutual Co-Founder Match!",
                body = "You and $matchedName both expressed interest. Tap to start messaging.",
                deepLinkUri = "linkup://app/messages"
            )
        }
    }

    // Smooth launch sequence: Native Splash -> First-Install Intro (if unseen & signed out) -> App Shell
    val launchPhase = when {
        state.isLoading -> "SPLASH"
        !state.hasSeenIntro && state.currentUser == null -> "WELCOME"
        else -> "APP"
    }

    Crossfade(
        targetState = launchPhase,
        animationSpec = tween(220),
        label = "launchPhase"
    ) { phase ->
        when (phase) {
            "SPLASH" -> BrandedLaunchSplash()
            "WELCOME" -> AppWelcomeScreen(
                onExploreApp = { vm.completeIntro() },
                onSignIn = {
                    vm.completeIntro()
                    navController.navigate(LinkUpRoutes.AUTH)
                }
            )
            else -> {
                val primaryTabs = listOf(
                    NavTab(LinkUpRoutes.DASHBOARD, "home", Icons.Rounded.Home, Icons.Outlined.Home),
                    NavTab(LinkUpRoutes.COFOUNDERS, "cofounders", Icons.Rounded.Handshake, Icons.Outlined.Handshake),
                    NavTab(LinkUpRoutes.JOBS, "jobs", Icons.Rounded.Work, Icons.Outlined.WorkOutline),
                    NavTab(LinkUpRoutes.TALENT, "talent", Icons.Rounded.Groups, Icons.Outlined.Groups),
                    NavTab(LinkUpRoutes.CONNECTIONS, "messages", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline)
                )
                val primaryRouteSet = primaryTabs.map { it.route }.toSet()
                val showPrimaryShell = currentRoute in primaryRouteSet &&
                    !(currentRoute == LinkUpRoutes.CONNECTIONS && isInChatDetail)
                val pendingConnectionCount = state.myConnections.count {
                    it.receiverId == state.currentUser?.id && it.status == "PENDING"
                }

                fun navigateTopLevel(route: String) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 700.dp

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (showPrimaryShell && !useRail) {
                                Column {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 0.dp
                                    ) {
                                        primaryTabs.forEach { tab ->
                                            val isSelected = currentRoute == tab.route
                                            val label = LinkUpLocale.navLabel(tab.labelKey, state.languageCode)
                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = { navigateTopLevel(tab.route) },
                                                modifier = Modifier.testTag("nav_${tab.labelKey}"),
                                                icon = {
                                                    val iconVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon
                                                    if (tab.route == LinkUpRoutes.CONNECTIONS && pendingConnectionCount > 0) {
                                                        BadgedBox(
                                                            badge = { Badge { Text(pendingConnectionCount.toString()) } }
                                                        ) {
                                                            Icon(iconVector, contentDescription = label)
                                                        }
                                                    } else {
                                                        Icon(iconVector, contentDescription = label)
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                alwaysShowLabel = true,
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.linkUpColors.accentBlue,
                                                    selectedTextColor = MaterialTheme.linkUpColors.accentBlue,
                                                    indicatorColor = MaterialTheme.linkUpColors.accentContainer,
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        ) {
                            if (showPrimaryShell && useRail) {
                                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                                    Spacer(Modifier.height(12.dp))
                                    primaryTabs.forEach { tab ->
                                        val isSelected = currentRoute == tab.route
                                        val label = LinkUpLocale.navLabel(tab.labelKey, state.languageCode)
                                        NavigationRailItem(
                                            selected = isSelected,
                                            onClick = { navigateTopLevel(tab.route) },
                                            modifier = Modifier.testTag("nav_${tab.labelKey}"),
                                            icon = {
                                                val iconVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon
                                                if (tab.route == LinkUpRoutes.CONNECTIONS && pendingConnectionCount > 0) {
                                                    BadgedBox(
                                                        badge = { Badge { Text(pendingConnectionCount.toString()) } }
                                                    ) {
                                                        Icon(iconVector, contentDescription = label)
                                                    }
                                                } else {
                                                    Icon(iconVector, contentDescription = label)
                                                }
                                            },
                                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.linkUpColors.accentBlue,
                                                selectedTextColor = MaterialTheme.linkUpColors.accentBlue,
                                                indicatorColor = MaterialTheme.linkUpColors.accentContainer
                                            )
                                        )
                                    }
                                }
                                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }

                            NavHost(
                                navController = navController,
                                startDestination = LinkUpRoutes.DASHBOARD,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                enterTransition = {
                                    if (targetState.destination.route in primaryRouteSet && initialState.destination.route in primaryRouteSet) {
                                        fadeIn(animationSpec = tween(160))
                                    } else {
                                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(240)) + fadeIn(tween(180))
                                    }
                                },
                                exitTransition = {
                                    if (targetState.destination.route in primaryRouteSet && initialState.destination.route in primaryRouteSet) {
                                        fadeOut(animationSpec = tween(140))
                                    } else {
                                        fadeOut(animationSpec = tween(180))
                                    }
                                },
                                popEnterTransition = {
                                    if (targetState.destination.route in primaryRouteSet && initialState.destination.route in primaryRouteSet) {
                                        fadeIn(animationSpec = tween(160))
                                    } else {
                                        fadeIn(animationSpec = tween(180))
                                    }
                                },
                                popExitTransition = {
                                    if (targetState.destination.route in primaryRouteSet && initialState.destination.route in primaryRouteSet) {
                                        fadeOut(animationSpec = tween(140))
                                    } else {
                                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(240)) + fadeOut(tween(180))
                                    }
                                }
                            ) {
                                composable(LinkUpRoutes.LANDING) {
                                    LandingScreen(
                                        state = state,
                                        languageCode = state.languageCode,
                                        isSignedIn = state.currentUser != null,
                                        onFindCofounder = { navigateTopLevel(LinkUpRoutes.COFOUNDERS) },
                                        onFindJob = { navigateTopLevel(LinkUpRoutes.JOBS) },
                                        onBuildTeam = { navigateTopLevel(LinkUpRoutes.TALENT) },
                                        onOpenMessages = { navigateTopLevel(LinkUpRoutes.CONNECTIONS) },
                                        onOpenSearch = { navController.navigate(LinkUpRoutes.SEARCH) },
                                        onOpenNotifications = { navController.navigate(LinkUpRoutes.NOTIFICATIONS) },
                                        onRefresh = vm::refreshFeed,
                                        onSignInClick = {
                                            if (state.currentUser != null) navigateTopLevel(LinkUpRoutes.DASHBOARD)
                                            else navController.navigate(LinkUpRoutes.AUTH)
                                        },
                                        onAboutClick = { navController.navigate(LinkUpRoutes.ABOUT) },
                                        isDarkTheme = isDarkTheme,
                                        onToggleTheme = { vm.toggleLightDark(isDarkTheme) }
                                    )
                                }

                                composable(LinkUpRoutes.AUTH) {
                                    AuthScreen(
                                        countries = state.countries,
                                        authError = state.authError,
                                        onClearError = vm::clearAuthError,
                                        onLogin = { email, pass ->
                                            vm.login(email, pass) {
                                                navController.navigate(LinkUpRoutes.DASHBOARD) {
                                                    popUpTo(LinkUpRoutes.DASHBOARD) { inclusive = true }
                                                }
                                            }
                                        },
                                        onRegister = { email, pass, name, type, country, city ->
                                            vm.register(email, pass, name, type, country, city) {
                                                navController.navigate(LinkUpRoutes.ONBOARDING) {
                                                    popUpTo(LinkUpRoutes.DASHBOARD) { inclusive = false }
                                                }
                                            }
                                        },
                                        onDemoLogin = { demoId ->
                                            vm.loginDemoAccount(demoId) {
                                                navController.navigate(LinkUpRoutes.DASHBOARD) {
                                                    popUpTo(LinkUpRoutes.DASHBOARD) { inclusive = true }
                                                }
                                            }
                                        },
                                        onBackToLanding = { navController.popBackStack() }
                                    )
                                }

                                composable(LinkUpRoutes.ONBOARDING) {
                                    val profile = state.currentProfile
                                    val prefs = state.currentFounderPrefs
                                    if (profile != null && prefs != null) {
                                        OnboardingWizardScreen(
                                            profile = profile,
                                            founderPrefs = prefs,
                                            onSaveAll = { p, f ->
                                                vm.saveProfile(p, "Onboarding progress saved")
                                                vm.saveFounderPreferences(f)
                                            },
                                            onFinish = {
                                                navController.navigate(LinkUpRoutes.DASHBOARD) {
                                                    popUpTo(LinkUpRoutes.DASHBOARD) { inclusive = true }
                                                }
                                            }
                                        )
                                    }
                                }

                                composable(
                                    route = LinkUpRoutes.DASHBOARD,
                                    deepLinks = listOf(navDeepLink { uriPattern = "linkup://app/home" })
                                ) {
                                    if (state.currentUser != null) {
                                        DashboardScreen(
                                            state = state,
                                            onOpenCofounders = { navigateTopLevel(LinkUpRoutes.COFOUNDERS) },
                                            onOpenJobs = { navigateTopLevel(LinkUpRoutes.JOBS) },
                                            onOpenTalent = { navigateTopLevel(LinkUpRoutes.TALENT) },
                                            onOpenConnections = { navigateTopLevel(LinkUpRoutes.CONNECTIONS) },
                                            onOpenTrials = {
                                                pendingTrialPartnerId = null
                                                navController.navigate(LinkUpRoutes.TRIALS)
                                            },
                                            onOpenProfile = { navController.navigate(LinkUpRoutes.PROFILE) },
                                            onOpenAdmin = { navController.navigate(LinkUpRoutes.ADMIN) },
                                            onOpenAbout = { navController.navigate(LinkUpRoutes.ABOUT) },
                                            onOpenSearch = { navController.navigate(LinkUpRoutes.SEARCH) },
                                            onOpenNotifications = { navController.navigate(LinkUpRoutes.NOTIFICATIONS) },
                                            onRefresh = vm::refreshFeed,
                                            isDarkTheme = isDarkTheme,
                                            onToggleTheme = { vm.toggleLightDark(isDarkTheme) }
                                        )
                                    } else {
                                        LandingScreen(
                                            state = state,
                                            languageCode = state.languageCode,
                                            isSignedIn = false,
                                            onFindCofounder = { navigateTopLevel(LinkUpRoutes.COFOUNDERS) },
                                            onFindJob = { navigateTopLevel(LinkUpRoutes.JOBS) },
                                            onBuildTeam = { navigateTopLevel(LinkUpRoutes.TALENT) },
                                            onOpenMessages = { navigateTopLevel(LinkUpRoutes.CONNECTIONS) },
                                            onOpenSearch = { navController.navigate(LinkUpRoutes.SEARCH) },
                                            onOpenNotifications = { navController.navigate(LinkUpRoutes.NOTIFICATIONS) },
                                            onRefresh = vm::refreshFeed,
                                            onSignInClick = { navController.navigate(LinkUpRoutes.AUTH) },
                                            onAboutClick = { navController.navigate(LinkUpRoutes.ABOUT) },
                                            isDarkTheme = isDarkTheme,
                                            onToggleTheme = { vm.toggleLightDark(isDarkTheme) }
                                        )
                                    }
                                }

                                composable(
                                    route = LinkUpRoutes.COFOUNDERS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "linkup://app/cofounders" })
                                ) {
                                    CofounderDiscoveryScreen(
                                        state = state,
                                        onDecision = vm::swipeCofounder,
                                        onSavePreferences = vm::saveFounderPreferences,
                                        onReportUser = { targetId, reason -> vm.submitReport("USER", targetId, reason) },
                                        onRefresh = vm::refreshFeed,
                                        onRequireAuth = { navController.navigate(LinkUpRoutes.AUTH) }
                                    )
                                }

                                composable(
                                    route = LinkUpRoutes.JOBS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "linkup://app/jobs" })
                                ) {
                                    JobMatchingScreen(
                                        state = state,
                                        onApplyOrSave = vm::applyOrSaveJob,
                                        onPublishJob = vm::publishJob,
                                        onUpdateApplicationStatus = vm::updateApplicationStatus,
                                        onRefresh = vm::refreshFeed,
                                        onRequireAuth = { navController.navigate(LinkUpRoutes.AUTH) }
                                    )
                                }

                                composable(
                                    route = LinkUpRoutes.TALENT,
                                    deepLinks = listOf(navDeepLink { uriPattern = "linkup://app/talent" })
                                ) {
                                    TalentMatchingScreen(
                                        state = state,
                                        onConnectTalent = { founderId, title ->
                                            vm.sendConnectionRequest(founderId, "TALENT", "Interested in startup role: $title")
                                        },
                                        onPublishOpportunity = vm::publishStartupOpportunity,
                                        onRefresh = vm::refreshFeed,
                                        onRequireAuth = { navController.navigate(LinkUpRoutes.AUTH) }
                                    )
                                }

                                composable(
                                    route = LinkUpRoutes.CONNECTIONS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "linkup://app/messages" })
                                ) {
                                    ConnectionsAndChatScreen(
                                        state = state,
                                        onSelectConversation = vm::selectConversation,
                                        onSendMessage = vm::sendMessage,
                                        onRespondConnection = vm::respondToConnection,
                                        onToggleBlock = vm::toggleBlockConversation,
                                        onReportUser = { targetId, reason -> vm.submitReport("USER", targetId, reason) },
                                        onStartTrialWithPartner = { partnerId ->
                                            pendingTrialPartnerId = partnerId
                                            navController.navigate(LinkUpRoutes.TRIALS)
                                        },
                                        onRefresh = vm::refreshFeed,
                                        onRequireAuth = { navController.navigate(LinkUpRoutes.AUTH) },
                                        onChatDetailActive = { isInChatDetail = it }
                                    )
                                }

                                composable(
                                    route = LinkUpRoutes.SEARCH,
                                    deepLinks = listOf(navDeepLink { uriPattern = "linkup://app/search" })
                                ) {
                                    SearchScreen(
                                        state = state,
                                        onBack = { navController.popBackStack() },
                                        onOpenCofounders = { navigateTopLevel(LinkUpRoutes.COFOUNDERS) },
                                        onOpenJobs = { navigateTopLevel(LinkUpRoutes.JOBS) },
                                        onOpenTalent = { navigateTopLevel(LinkUpRoutes.TALENT) }
                                    )
                                }

                                composable(
                                    route = LinkUpRoutes.NOTIFICATIONS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "linkup://app/notifications" })
                                ) {
                                    NotificationsScreen(
                                        state = state,
                                        onBack = { navController.popBackStack() },
                                        onMarkAllRead = vm::markNotificationsRead,
                                        onOpenMessages = { navigateTopLevel(LinkUpRoutes.CONNECTIONS) },
                                        onOpenJobs = { navigateTopLevel(LinkUpRoutes.JOBS) },
                                        onOpenCofounders = { navigateTopLevel(LinkUpRoutes.COFOUNDERS) },
                                        onRequireAuth = { navController.navigate(LinkUpRoutes.AUTH) }
                                    )
                                }

                                composable(
                                    route = LinkUpRoutes.TRIALS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "linkup://app/trials" })
                                ) {
                                    BuildTogetherScreen(
                                        state = state,
                                        initialPartnerId = pendingTrialPartnerId,
                                        onBack = {
                                            pendingTrialPartnerId = null
                                            navController.popBackStack()
                                        },
                                        onCreateTrial = { pid, title, desc, resp ->
                                            pendingTrialPartnerId = null
                                            vm.createTrial(pid, title, desc, resp)
                                        },
                                        onAddTask = vm::addTrialTask,
                                        onToggleTask = vm::toggleTrialTask,
                                        onSaveNotes = vm::updateTrialNotes,
                                        onSubmitReview = vm::submitTrialReview
                                    )
                                }

                                composable(LinkUpRoutes.PROFILE) {
                                    ProfileAndVerificationScreen(
                                        state = state,
                                        onBack = { navController.popBackStack() },
                                        onSaveProfile = { vm.saveProfile(it) },
                                        onSubmitVerification = vm::submitVerification,
                                        onToggleLowData = vm::setLowDataMode,
                                        onChangeLanguage = vm::setLanguageCode,
                                        onChangeThemeMode = vm::setThemeMode,
                                        onOpenOnboarding = { navController.navigate(LinkUpRoutes.ONBOARDING) },
                                        onOpenAbout = { navController.navigate(LinkUpRoutes.ABOUT) },
                                        onLogout = {
                                            vm.logout {
                                                navController.navigate(LinkUpRoutes.DASHBOARD) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        }
                                    )
                                }

                                composable(LinkUpRoutes.ABOUT) {
                                    AboutUsScreen(onBack = { navController.popBackStack() })
                                }

                                composable(LinkUpRoutes.ADMIN) {
                                    AdminDashboardScreen(
                                        state = state,
                                        onBack = { navController.popBackStack() },
                                        onToggleUserSuspend = vm::toggleUserSuspension,
                                        onReviewVerification = vm::reviewVerification,
                                        onModerateJob = vm::moderateJob,
                                        onResolveReport = vm::resolveReport,
                                        onAddCountry = vm::addOrUpdateCountry,
                                        onToggleCountry = vm::toggleCountryActive
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Mutual Match Celebration Dialog
    state.mutualMatchName?.let { matchedName ->
        AlertDialog(
            onDismissRequest = vm::clearMutualMatch,
            icon = { Icon(Icons.Rounded.Handshake, contentDescription = null, tint = ElectricBlue) },
            title = { Text("Mutual Match!") },
            text = {
                Text("You and $matchedName both expressed interest! A 1-on-1 conversation is now open, and you can start a 14-day Build Together trial.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.clearMutualMatch()
                        navController.navigate(LinkUpRoutes.CONNECTIONS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) { Text("Message Now") }
            },
            dismissButton = {
                TextButton(onClick = vm::clearMutualMatch) { Text("Keep Exploring") }
            }
        )
    }
}
