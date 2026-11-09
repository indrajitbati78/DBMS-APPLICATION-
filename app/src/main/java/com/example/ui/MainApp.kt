package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.MobileShopApplication
import com.example.data.model.User
import com.example.ui.activity.ActivityLogScreen
import com.example.ui.activity.ActivityLogViewModel
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.customers.CustomersScreen
import com.example.ui.customers.CustomersViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.navigation.Screen
import com.example.ui.profile.CustomerProfileScreen
import com.example.ui.profile.CustomerProfileViewModel
import com.example.ui.register.RegisterCustomerScreen
import com.example.ui.register.RegisterCustomerViewModel

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    application: MobileShopApplication,
    authViewModel: AuthViewModel = viewModel { AuthViewModel(application.authRepository) }
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showRoleSwitchDialog by remember { mutableStateOf(false) }

    // If not signed in, show LoginScreen
    if (currentUser == null) {
        LoginScreen(
            viewModel = authViewModel,
            onLoginSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
        return
    }

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_home"
        ),
        BottomNavItem(
            route = Screen.Register.route,
            label = "Register",
            selectedIcon = Icons.Filled.PersonAdd,
            unselectedIcon = Icons.Outlined.PersonAdd,
            testTag = "nav_register"
        ),
        BottomNavItem(
            route = Screen.Customers.route,
            label = "Customers",
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People,
            testTag = "nav_customers"
        )
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Register.route,
        Screen.Customers.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Mobile Shop Manager",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${currentUser?.name} • ${if (currentUser?.isAdmin == true) "Owner (Admin)" else "Staff"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        // Role Switcher toggle for instant testing of Admin vs Staff permissions
                        TextButton(
                            onClick = { showRoleSwitchDialog = true },
                            modifier = Modifier.testTag("switch_role_button")
                        ) {
                            Text(
                                text = if (currentUser?.isAdmin == true) "Role: Admin" else "Role: Staff",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Activity log button (Admin only)
                        if (currentUser?.isAdmin == true) {
                            IconButton(
                                onClick = { navController.navigate(Screen.ActivityLog.route) },
                                modifier = Modifier.testTag("nav_activity_logs")
                            ) {
                                Icon(Icons.Default.History, contentDescription = "Activity Audit Logs")
                            }
                        }

                        // Logout button
                        IconButton(
                            onClick = { authViewModel.signOut() },
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val homeVm = viewModel<HomeViewModel> {
                    HomeViewModel(application.customerRepository)
                }
                HomeScreen(
                    viewModel = homeVm,
                    currentUser = currentUser,
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                    onNavigateToCustomerProfile = { id ->
                        navController.navigate(Screen.CustomerProfile.createRoute(id))
                    },
                    onNavigateToActivityLog = { navController.navigate(Screen.ActivityLog.route) }
                )
            }

            composable(Screen.Register.route) {
                val registerVm = viewModel<RegisterCustomerViewModel> {
                    RegisterCustomerViewModel(application.customerRepository, application.storageRepository)
                }
                RegisterCustomerScreen(
                    viewModel = registerVm,
                    currentUser = currentUser,
                    onCustomerSaved = { customerId ->
                        navController.navigate(Screen.CustomerProfile.createRoute(customerId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.Customers.route) {
                val customersVm = viewModel<CustomersViewModel> {
                    CustomersViewModel(application.customerRepository)
                }
                CustomersScreen(
                    viewModel = customersVm,
                    currentUser = currentUser,
                    onNavigateToProfile = { id ->
                        navController.navigate(Screen.CustomerProfile.createRoute(id))
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(
                route = Screen.CustomerProfile.route,
                arguments = listOf(navArgument("customerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                val profileVm = viewModel<CustomerProfileViewModel>(key = "profile_$customerId") {
                    CustomerProfileViewModel(customerId, application.customerRepository, application.storageRepository)
                }
                CustomerProfileScreen(
                    viewModel = profileVm,
                    currentUser = currentUser,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ActivityLog.route) {
                val activityVm = viewModel<ActivityLogViewModel> {
                    ActivityLogViewModel(application.customerRepository)
                }
                ActivityLogScreen(
                    viewModel = activityVm,
                    currentUser = currentUser,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    // Role switcher dialog (helps testing Staff vs Admin delete permissions instantly)
    if (showRoleSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            title = { Text("Switch Role (Test Permissions)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Test role-based access control rules directly without signing out:\n\n" +
                        "• Admin (Owner): Can delete customers, remove purchases, and view Activity Logs.\n" +
                        "• Staff: Can view, register, edit customers, and add purchases, but CANNOT delete.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newRole = if (currentUser?.isAdmin == true) User.ROLE_STAFF else User.ROLE_ADMIN
                        authViewModel.switchRoleForTesting(newRole)
                        showRoleSwitchDialog = false
                    }
                ) {
                    Text(if (currentUser?.isAdmin == true) "Switch to Staff" else "Switch to Admin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRoleSwitchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
