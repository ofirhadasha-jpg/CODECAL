package com.vmm.manager.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vmm.manager.R
import com.vmm.manager.ui.dashboard.DashboardScreen
import com.vmm.manager.ui.machines.MachineListScreen
import com.vmm.manager.ui.products.AddProductScreen
import com.vmm.manager.ui.products.ProductListScreen
import com.vmm.manager.ui.transactions.TransactionListScreen

sealed class Screen(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Dashboard    : Screen("dashboard",    R.string.nav_dashboard,    Icons.Default.Dashboard)
    object Products     : Screen("products",     R.string.nav_products,     Icons.Default.Inventory)
    object Machines     : Screen("machines",     R.string.nav_machines,     Icons.Default.DeviceHub)
    object Transactions : Screen("transactions", R.string.nav_transactions, Icons.Default.Receipt)
    object AddProduct   : Screen("add_product",  R.string.add_product,      Icons.Default.Add)
}

val bottomNavItems = listOf(Screen.Dashboard, Screen.Products, Screen.Machines, Screen.Transactions)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon     = { Icon(screen.icon, stringResource(screen.labelRes)) },
                            label    = { Text(stringResource(screen.labelRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick  = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToProducts     = { navController.navigate(Screen.Products.route) },
                    onNavigateToMachines     = { navController.navigate(Screen.Machines.route) },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) }
                )
            }
            composable(Screen.Products.route) {
                ProductListScreen(
                    onAddProduct  = { navController.navigate(Screen.AddProduct.route) },
                    onEditProduct = { id -> navController.navigate("edit_product/$id") }
                )
            }
            composable(Screen.AddProduct.route) {
                AddProductScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Machines.route) {
                MachineListScreen()
            }
            composable(Screen.Transactions.route) {
                TransactionListScreen()
            }
        }
    }
}
