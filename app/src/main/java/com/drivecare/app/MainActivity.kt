package com.drivecare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.screens.*
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage
import com.drivecare.app.utils.LocaleManager

enum class NavTab(val stringKey: String, val icon: ImageVector) {
    SUMMARY("tab_dashboard", Icons.Default.Dashboard),
    GARAGE("tab_garage", Icons.Default.DirectionsCar),
    FUEL("tab_fuel", Icons.Default.LocalGasStation),
    SERVICE("tab_service", Icons.Default.AutoAwesome),
    DOCUMENTS("tab_documents", Icons.Default.FolderOpen),
    EMERGENCY("tab_emergency", Icons.Default.Emergency),
    ACHIEVEMENTS("tab_achievements", Icons.Default.EmojiEvents),
    SETTINGS("tab_settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: DriveCareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val currentLang by viewModel.currentLanguage.collectAsState()
            val layoutDirection = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            LaunchedEffect(currentLang) {
                LocaleManager.applyLocale(this@MainActivity, currentLang)
            }

            CompositionLocalProvider(
                LocalAppLanguage provides currentLang,
                LocalLayoutDirection provides layoutDirection
            ) {
                MaterialTheme {
                    var currentTab by remember { mutableStateOf(NavTab.SUMMARY) }

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
                                        onClick = { currentTab = tab },
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
                            NavTab.SUMMARY -> SummaryDashboardScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.GARAGE -> VehicleListScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.FUEL -> FuelTrackerScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.SERVICE -> MaintenanceScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.DOCUMENTS -> DocumentsScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.EMERGENCY -> EmergencyScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.ACHIEVEMENTS -> AchievementsScreen(viewModel = viewModel, modifier = modifier)
                            NavTab.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = modifier)
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
