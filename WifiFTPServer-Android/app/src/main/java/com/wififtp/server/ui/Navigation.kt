package com.wififtp.server.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wififtp.server.ui.screens.*
import com.wififtp.server.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val badgeCount: Int = 0) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Files     : Screen("files",     "Files",     Icons.Default.Folder)
    object Devices   : Screen("devices",   "Devices",   Icons.Default.Devices)
    object Activity  : Screen("activity",  "Activity",  Icons.Default.SwapVert)
    object Settings  : Screen("settings",  "Settings",  Icons.Default.Settings)
}

private val screens = listOf(
    Screen.Dashboard, Screen.Files, Screen.Devices, Screen.Activity, Screen.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar messages
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        containerColor = BgDark,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = BgCard,
                    contentColor = TextPrimary,
                    actionColor = Primary,
                )
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar(containerColor = BgCard, tonalElevation = 0.dp) {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    val badgeCount = when (screen) {
                        Screen.Devices  -> state.serverState.connectedClients.size
                        Screen.Activity -> state.transfers.count { it.status == com.wififtp.server.data.TransferStatus.ACTIVE }
                        else            -> 0
                    }

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            BadgedBox(badge = {
                                if (badgeCount > 0) Badge(containerColor = Error) {
                                    Text(badgeCount.toString(), fontSize = 8.sp)
                                }
                            }) {
                                Icon(screen.icon, contentDescription = screen.label)
                            }
                        },
                        label = { Text(screen.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Primary.copy(alpha = 0.15f),
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Screen.Dashboard.route) { HomeScreen(viewModel) }
            composable(Screen.Files.route)     { FileManagerScreen() }
            composable(Screen.Devices.route)   { DevicesScreen(viewModel) }
            composable(Screen.Activity.route)  { ActivityScreen(viewModel) }
            composable(Screen.Settings.route)  { SettingsScreen(viewModel) }
        }
    }
}
