package com.expensemanager.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensemanager.app.ui.components.GlassBottomBar
import com.expensemanager.app.ui.components.SyncOverlay
import com.expensemanager.app.ui.screens.accounts.AccountsScreen
import com.expensemanager.app.ui.screens.accounts.AccountsViewModel
import com.expensemanager.app.ui.screens.analytics.AnalyticsScreen
import com.expensemanager.app.ui.screens.analytics.AnalyticsViewModel
import com.expensemanager.app.ui.screens.dashboard.DashboardScreen
import com.expensemanager.app.ui.screens.dashboard.DashboardViewModel
import com.expensemanager.app.ui.screens.settings.SettingsScreen
import com.expensemanager.app.ui.screens.settings.SettingsViewModel
import com.expensemanager.app.ui.screens.transactions.TransactionDetailDialog
import com.expensemanager.app.ui.screens.transactions.TransactionsScreen
import com.expensemanager.app.ui.screens.transactions.TransactionsViewModel
import com.expensemanager.app.ui.theme.CleanAppBackground

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.AutoMirrored.Filled.ReceiptLong)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.PieChart)
    object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalanceWallet)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    dashboardViewModel: DashboardViewModel,
    transactionsViewModel: TransactionsViewModel,
    analyticsViewModel: AnalyticsViewModel,
    accountsViewModel: AccountsViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTxnForEdit by transactionsViewModel.selectedTransactionForEdit.collectAsState()
    val syncState by dashboardViewModel.syncState.collectAsState()

    CleanAppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialOffset = { (it * 0.08f).toInt() }
                            )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) +
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(200),
                                targetOffset = { (-it * 0.08f).toInt() }
                            )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialOffset = { (-it * 0.08f).toInt() }
                            )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) +
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(200),
                                targetOffset = { (it * 0.08f).toInt() }
                            )
                }
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToTransactions = {
                            navController.navigate(Screen.Transactions.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToInsights = {
                            navController.navigate(Screen.Analytics.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onTransactionClick = { txn ->
                            transactionsViewModel.openTransactionDetail(txn)
                        }
                    )
                }
                composable(Screen.Transactions.route) {
                    TransactionsScreen(viewModel = transactionsViewModel)
                }
                composable(Screen.Analytics.route) {
                    AnalyticsScreen(
                        viewModel = analyticsViewModel,
                        onTransactionClick = { txn ->
                            transactionsViewModel.openTransactionDetail(txn)
                        }
                    )
                }
                composable(Screen.Accounts.route) {
                    AccountsScreen(
                        viewModel = accountsViewModel,
                        onTransactionClick = { txn ->
                            transactionsViewModel.openTransactionDetail(txn)
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel = settingsViewModel)
                }
            }

            // Shared Global Transaction Detail Bottom Sheet
            selectedTxnForEdit?.let { txn ->
                TransactionDetailDialog(
                    transaction = txn,
                    onDismiss = { transactionsViewModel.closeTransactionDetail() },
                    onSave = { updatedTxn, applyRule ->
                        transactionsViewModel.updateTransaction(updatedTxn, applyRule)
                    },
                    onDelete = { id ->
                        transactionsViewModel.deleteTransaction(id)
                    }
                )
            }

            // Clean Floating Navigation Bar
            GlassBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (currentRoute != route) {
                        if (route == Screen.Dashboard.route) {
                            // Directly pop back to Home / Dashboard without saveState/restoreState collision
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )

            // Global Sync & Classification Frosted Glass Loading Overlay
            SyncOverlay(syncState = syncState)
        }
    }
}
