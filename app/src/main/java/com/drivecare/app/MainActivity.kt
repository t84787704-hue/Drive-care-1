package com.drivecare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.drivecare.app.ui.DriveCareViewModel
import com.drivecare.app.ui.screens.FuelTrackerScreen
import com.drivecare.app.ui.screens.MaintenanceScreen
import com.drivecare.app.ui.screens.ReminderScreen
import com.drivecare.app.ui.screens.SummaryDashboardScreen
import com.drivecare.app.ui.screens.VehicleListScreen
import com.drivecare.app.utils.AppLanguage
import com.drivecare.app.utils.AppStrings
import com.drivecare.app.utils.LocalAppLanguage

enum class NavTab(val stringKey: String, val icon: ImageVector) {
    SUMMARY("tab_summary", Icons.Default.Dashboard),
    GARAGE("tab_garage", Icons.Default.DirectionsCar),
    FUEL("tab_fuel", Icons.Default.LocalGasStation),
    SERVICE("tab_service", Icons.Default.Build),
    REMINDERS("tab_reminders", Icons.Default.Notifications)
}

class MainActivity : ComponentActivity() {
    private val viewModel: DriveCareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val currentLang by viewModel.currentLanguage.collectAsState()

            CompositionLocalProvider(LocalAppLanguage provides currentLang) {
                MaterialTheme {
                    var currentTab by remember { mutableStateOf(NavTab.GARAGE) }

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
                            NavTab.REMINDERS -> ReminderScreen(viewModel = viewModel, modifier = modifier)
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
