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
                    val currentDestination = backStack.lastOrNull() ?: NavDestination(NavTab.SUMMARY)
                    val currentTab = currentDestination.tab
                    val currentSubSection = currentDestination.subSection
                    val isSecondary = backStack.size > 1 || currentDestination != NavDestination(NavTab.SUMMARY)

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
                            NavTab.FUEL -> FuelTrackerScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.SERVICE -> MaintenanceScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.MORE -> MoreScreen(
                                viewModel = viewModel,
                                modifier = modifier,
                                subSection = currentSubSection,
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
