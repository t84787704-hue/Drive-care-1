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

enum class NavTab(val title: String, val icon: ImageVector) {
    SUMMARY("Summary", Icons.Default.Dashboard),
    GARAGE("Garage", Icons.Default.DirectionsCar),
    FUEL("Fuel", Icons.Default.LocalGasStation),
    SERVICE("Service", Icons.Default.Build),
    REMINDERS("Reminders", Icons.Default.Notifications)
}

class MainActivity : ComponentActivity() {
    private val viewModel: DriveCareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var currentTab by remember { mutableStateOf(NavTab.GARAGE) }

                Scaffold(
                    topBar = {
                        OptInTopBar(title = currentTab.title)
                    },
                    bottomBar = {
                        NavigationBar {
                            NavTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    label = { Text(tab.title) },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) }
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun OptInTopBar(title: String) {
        TopAppBar(
            title = { Text("DriveCare - $title") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}
