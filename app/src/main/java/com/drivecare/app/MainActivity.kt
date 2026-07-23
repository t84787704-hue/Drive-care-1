package com.drivecare.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
                    var currentTab by remember { mutableStateOf(NavTab.SUMMARY) }
                    var moreSubSection by remember { mutableStateOf(MoreSubSection.MENU) }

                    Scaffold(
                        topBar = {
                            OptInTopBar(
                                titleKey = currentTab.stringKey,
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
                                            if (tab == NavTab.MORE && currentTab != NavTab.MORE) {
                                                moreSubSection = MoreSubSection.MENU
                                            }
                                            currentTab = tab
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
                                    when (target) {
                                        "GARAGE" -> currentTab = NavTab.GARAGE
                                        "FUEL" -> currentTab = NavTab.FUEL
                                        "SERVICES", "SERVICE" -> currentTab = NavTab.SERVICE
                                        "EXPENSES" -> {
                                            moreSubSection = MoreSubSection.EXPENSES
                                            currentTab = NavTab.MORE
                                        }
                                        "TIMELINE" -> {
                                            moreSubSection = MoreSubSection.TIMELINE
                                            currentTab = NavTab.MORE
                                        }
                                        "GPS" -> {
                                            moreSubSection = MoreSubSection.GPS_TRACKING
                                            currentTab = NavTab.MORE
                                        }
                                        "SHARING" -> {
                                            moreSubSection = MoreSubSection.FAMILY_SHARING
                                            currentTab = NavTab.MORE
                                        }
                                        else -> currentTab = NavTab.MORE
                                    }
                                }
                            )
                            NavTab.GARAGE -> VehicleListScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.FUEL -> FuelTrackerScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.SERVICE -> MaintenanceScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.MORE -> MoreScreen(
                                viewModel = viewModel,
                                modifier = modifier,
                                initialSubSection = moreSubSection
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
        titleKey: String,
        currentLanguage: AppLanguage,
        onLanguageSelected: (AppLanguage) -> Unit
    ) {
        var menuExpanded by remember { mutableStateOf(false) }

        TopAppBar(
            title = {
                Text("${AppStrings.get("app_name", currentLanguage)} - ${AppStrings.get(titleKey, currentLanguage)}")
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
