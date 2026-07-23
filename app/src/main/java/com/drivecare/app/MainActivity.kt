package com.drivecare.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.screens.*
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.DriveCareNotificationScheduler
import com.drivecare.app.utils.LocalAppLanguage
import com.drivecare.app.utils.LocaleManager

enum class NavTab(val stringKey: String, val icon: ImageVector) {
    SUMMARY("tab_dashboard", Icons.Default.Dashboard),
    GARAGE("tab_garage", Icons.Default.DirectionsCar),
    FUEL("tab_fuel", Icons.Default.LocalGasStation),
    SERVICE("tab_services", Icons.Default.Build),
    MORE("tab_more", Icons.Default.MoreHoriz)
}

data class NavDestination(
    val tab: NavTab,
    val subSection: MoreSubSection = MoreSubSection.MENU
)

class MainActivity : ComponentActivity() {
    private val viewModel: DriveCareViewModel by viewModels()
    private var pendingNavigationExtra by mutableStateOf<Pair<NavDestination, Long?>?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            DriveCareNotificationScheduler.triggerImmediateCheck(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule WorkManager system notification worker
        DriveCareNotificationScheduler.schedulePeriodicCheck(this)

        // Process notification intent if launched from a notification
        processIntent(intent)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val currentLang by viewModel.currentLanguage.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val systemInDark = isSystemInDarkTheme()

            val useDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDark
            }

            val layoutDirection = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            LaunchedEffect(currentLang) {
                LocaleManager.applyLocale(this@MainActivity, currentLang)
            }

            CompositionLocalProvider(
                LocalAppLanguage provides currentLang,
                LocalLayoutDirection provides layoutDirection
            ) {
                val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()
                MaterialTheme(colorScheme = colorScheme) {
                    val backStack = remember { mutableStateListOf(NavDestination(NavTab.SUMMARY)) }

                    // Handle deep link notification navigation while preserving back navigation
                    val navPair = pendingNavigationExtra
                    LaunchedEffect(navPair) {
                        if (navPair != null) {
                            val (targetDest, _) = navPair
                            backStack.clear()
                            backStack.add(NavDestination(NavTab.SUMMARY))
                            if (targetDest != NavDestination(NavTab.SUMMARY)) {
                                backStack.add(targetDest)
                            }
                        }
                    }

                    val currentDestination = backStack.lastOrNull() ?: NavDestination(NavTab.SUMMARY)
                    val currentTab = currentDestination.tab
                    val currentSubSection = currentDestination.subSection
                    val isSecondary = backStack.size > 1 || currentDestination != NavDestination(NavTab.SUMMARY)
                    val currentHighlightId = if (navPair != null && navPair.first == currentDestination) navPair.second else null

                    val popBackStack: () -> Unit = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            backStack.clear()
                            backStack.add(NavDestination(NavTab.SUMMARY))
                        }
                    }

                    BackHandler(enabled = isSecondary) {
                        popBackStack()
                    }

                    val titleText = if (currentTab == NavTab.MORE && currentSubSection != MoreSubSection.MENU) {
                        when (currentSubSection) {
                            MoreSubSection.INSURANCE -> "Insurance & Renewals"
                            MoreSubSection.EXPENSES -> "Expense Manager"
                            MoreSubSection.TIMELINE -> "Vehicle Timeline"
                            MoreSubSection.GPS_TRACKING -> "GPS Live Tracking & Trips"
                            MoreSubSection.FAMILY_SHARING -> "Family Sharing & Drivers"
                            MoreSubSection.DOCUMENTS -> AppStrings.get("tab_documents", currentLang)
                            MoreSubSection.EMERGENCY -> AppStrings.get("tab_emergency", currentLang)
                            MoreSubSection.ACHIEVEMENTS -> AppStrings.get("tab_achievements", currentLang)
                            MoreSubSection.SETTINGS -> AppStrings.get("settings_title", currentLang)
                            MoreSubSection.PROFILE -> "User Profile"
                            MoreSubSection.AUTH -> "Cloud Account Sign In"
                            else -> AppStrings.get("tab_more", currentLang)
                        }
                    } else {
                        "${AppStrings.get("app_name", currentLang)} - ${AppStrings.get(currentTab.stringKey, currentLang)}"
                    }

                    Scaffold(
                        topBar = {
                            OptInTopBar(
                                titleText = titleText,
                                showBackButton = isSecondary,
                                onBackClick = popBackStack,
                                currentLanguage = currentLang,
                                onLanguageSelected = { viewModel.setLanguage(it) }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                NavTab.entries.forEach { tab ->
                                    val localizedTitle = AppStrings.get(tab.stringKey, currentLang)
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = {
                                            if (tab == NavTab.SUMMARY) {
                                                if (currentDestination != NavDestination(NavTab.SUMMARY)) {
                                                    backStack.clear()
                                                    backStack.add(NavDestination(NavTab.SUMMARY))
                                                }
                                            } else {
                                                val dest = NavDestination(tab, MoreSubSection.MENU)
                                                if (currentDestination != dest) {
                                                    backStack.add(dest)
                                                }
                                            }
                                        },
                                        label = { Text(localizedTitle) },
                                        icon = { Icon(tab.icon, contentDescription = localizedTitle) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        val modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)

                        when (currentTab) {
                            NavTab.SUMMARY -> SummaryDashboardScreen(
                                viewModel = viewModel,
                                modifier = modifier,
                                onNavigateTab = { target ->
                                    val dest = when (target) {
                                        "GARAGE" -> NavDestination(NavTab.GARAGE)
                                        "FUEL" -> NavDestination(NavTab.FUEL)
                                        "SERVICES", "SERVICE" -> NavDestination(NavTab.SERVICE)
                                        "EXPENSES" -> NavDestination(NavTab.MORE, MoreSubSection.EXPENSES)
                                        "TIMELINE" -> NavDestination(NavTab.MORE, MoreSubSection.TIMELINE)
                                        "GPS" -> NavDestination(NavTab.MORE, MoreSubSection.GPS_TRACKING)
                                        "SHARING" -> NavDestination(NavTab.MORE, MoreSubSection.FAMILY_SHARING)
                                        else -> NavDestination(NavTab.MORE, MoreSubSection.MENU)
                                    }
                                    if (currentDestination != dest) {
                                        backStack.add(dest)
                                    }
                                }
                            )
                            NavTab.GARAGE -> VehicleListScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.FUEL -> FuelTrackerScreen(
                                viewModel = viewModel,
                                modifier = modifier,
                                highlightRecordId = currentHighlightId
                            )
                            NavTab.SERVICE -> MaintenanceScreen(
                                viewModel = viewModel,
                                modifier = modifier,
                                highlightRecordId = currentHighlightId
                            )
                            NavTab.MORE -> MoreScreen(
                                viewModel = viewModel,
                                modifier = modifier,
                                subSection = currentSubSection,
                                highlightRecordId = currentHighlightId,
                                onSubSectionSelect = { sub ->
                                    val dest = NavDestination(NavTab.MORE, sub)
                                    if (currentDestination != dest) {
                                        backStack.add(dest)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val tab = intent.getStringExtra(com.drivecare.app.utils.DriveCareNotificationReceiver.EXTRA_TARGET_TAB)
        val section = intent.getStringExtra(com.drivecare.app.utils.DriveCareNotificationReceiver.EXTRA_TARGET_SECTION)
        val recId = intent.getLongExtra(com.drivecare.app.utils.DriveCareNotificationReceiver.EXTRA_RECORD_ID, -1L).let {
            if (it != -1L) it else null
        }
        val dest = mapToDestination(tab, section)
        if (dest != null) {
            pendingNavigationExtra = Pair(dest, recId)
        }
    }

    private fun mapToDestination(tabExtra: String?, sectionExtra: String?): NavDestination? {
        if (tabExtra == null) return null
        return when (tabExtra.uppercase(java.util.Locale.US)) {
            "INSURANCE" -> NavDestination(NavTab.MORE, MoreSubSection.INSURANCE)
            "DOCUMENTS" -> NavDestination(NavTab.MORE, MoreSubSection.DOCUMENTS)
            "EXPENSES" -> NavDestination(NavTab.MORE, MoreSubSection.EXPENSES)
            "SERVICE", "SERVICES", "MAINTENANCE" -> NavDestination(NavTab.SERVICE)
            "FUEL" -> NavDestination(NavTab.FUEL)
            "GARAGE" -> NavDestination(NavTab.GARAGE)
            "SUMMARY", "DASHBOARD" -> NavDestination(NavTab.SUMMARY)
            "MORE" -> {
                val sec = sectionExtra?.uppercase(java.util.Locale.US)
                val sub = when (sec) {
                    "INSURANCE" -> MoreSubSection.INSURANCE
                    "DOCUMENTS" -> MoreSubSection.DOCUMENTS
                    "EXPENSES" -> MoreSubSection.EXPENSES
                    "TIMELINE" -> MoreSubSection.TIMELINE
                    "GPS", "GPS_TRACKING" -> MoreSubSection.GPS_TRACKING
                    "SHARING", "FAMILY_SHARING" -> MoreSubSection.FAMILY_SHARING
                    "EMERGENCY" -> MoreSubSection.EMERGENCY
                    "ACHIEVEMENTS" -> MoreSubSection.ACHIEVEMENTS
                    "SETTINGS" -> MoreSubSection.SETTINGS
                    "PROFILE" -> MoreSubSection.PROFILE
                    "AUTH" -> MoreSubSection.AUTH
                    else -> MoreSubSection.MENU
                }
                NavDestination(NavTab.MORE, sub)
            }
            else -> null
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun OptInTopBar(
        titleText: String,
        showBackButton: Boolean,
        onBackClick: () -> Unit,
        currentLanguage: AppLanguage,
        onLanguageSelected: (AppLanguage) -> Unit
    ) {
        var menuExpanded by remember { mutableStateOf(false) }

        TopAppBar(
            title = {
                Text(titleText)
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.Language, contentDescription = "Language")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    AppLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.displayName) },
                            onClick = {
                                onLanguageSelected(lang)
                                menuExpanded = false
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}
