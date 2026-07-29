package com.wolffentp.stockstreamlocal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wolffentp.stockstreamlocal.auth.AuthState
import com.wolffentp.stockstreamlocal.settings.SettingsViewModel
import com.wolffentp.stockstreamlocal.ui.screens.*

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val authState by settingsVm.authState.collectAsStateWithLifecycle()

    // Redirect to lock screen whenever auth state becomes Locked
    LaunchedEffect(authState) {
        if (authState == AuthState.Locked) {
            navController.navigate(NavRoutes.LOCK) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = when (authState) {
        is AuthState.Locked -> NavRoutes.LOCK
        else -> NavRoutes.HOME
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.LOCK) {
            LockScreen(onUnlocked = {
                navController.navigate(NavRoutes.HOME) {
                    popUpTo(NavRoutes.LOCK) { inclusive = true }
                }
            })
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToWatchlist = { navController.navigate(NavRoutes.WATCHLIST) },
                onNavigateToCsvImport = { navController.navigate(NavRoutes.CSV_IMPORT) },
                onNavigateToSettings  = { navController.navigate(NavRoutes.SETTINGS) },
                onNavigateToFullScreen = { navController.navigate(NavRoutes.FULL_SCREEN_DISPLAY) },
                onNavigateToLegend    = { navController.navigate(NavRoutes.DATA_QUALITY_LEGEND) },
            )
        }

        composable(NavRoutes.WATCHLIST) {
            WatchlistScreen(
                onAddTicker     = { navController.navigate(NavRoutes.ADD_TICKER) },
                onTickerDetail  = { sym -> navController.navigate(NavRoutes.tickerDetail(sym)) },
                onNavigateBack  = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.ADD_TICKER) {
            AddTickerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.TICKER_DETAIL,
            arguments = listOf(navArgument("symbol") { type = NavType.StringType }),
        ) { back ->
            val symbol = back.arguments?.getString("symbol") ?: ""
            TickerDetailScreen(symbol = symbol, onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CSV_IMPORT) {
            CsvImportScreen(
                onNavigateToValidation = { navController.navigate(NavRoutes.IMPORT_VALIDATION) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.IMPORT_VALIDATION) {
            ImportValidationScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack   = { navController.popBackStack() },
                onContextCleared = {
                    navController.navigate(NavRoutes.LOCK) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProviderSettings = {
                    navController.navigate(NavRoutes.PROVIDER_SETTINGS)
                },
            )
        }

        composable(NavRoutes.PROVIDER_SETTINGS) {
            ProviderSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.REFRESH_SETTINGS) {
            RefreshSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.COLUMN_LAYOUT_EDITOR,
            arguments = listOf(navArgument("viewId") { type = NavType.StringType }),
        ) { back ->
            val viewId = back.arguments?.getString("viewId") ?: "default"
            ColumnLayoutEditorScreen(viewId = viewId, onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.ROTATING_VIEWS_EDITOR) {
            RotatingViewsEditorScreen(
                onEditLayout = { id -> navController.navigate(NavRoutes.columnLayoutEditor(id)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.FULL_SCREEN_DISPLAY) {
            FullScreenRotatingDisplay(onExit = { navController.popBackStack() })
        }

        composable(NavRoutes.DATA_QUALITY_LEGEND) {
            DataQualityLegendScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.ABOUT) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
