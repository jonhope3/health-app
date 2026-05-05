package com.fittrack.app

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fittrack.app.theme.interFamily
import com.fittrack.app.ui.home.HomeScreen
import com.fittrack.app.ui.log.LogScreen
import com.fittrack.app.ui.settings.SettingsScreen
import com.fittrack.app.ui.steps.StepsScreen
import kotlinx.serialization.Serializable

// ── Type-safe navigation routes ───────────────────────────────────────────────
// Using @Serializable data objects (Navigation 2.8+) for compile-time safety.
// No more string literals — the compiler validates all routes.
sealed interface AppRoute {
    @Serializable data object Home     : AppRoute
    @Serializable data object Log      : AppRoute
    @Serializable data object Steps    : AppRoute
    @Serializable data object Settings : AppRoute
}

data class NavItem(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem(AppRoute.Home,     "Home",     Icons.Filled.Home),
    NavItem(AppRoute.Log,      "Log Food", Icons.Filled.Restaurant),
    NavItem(AppRoute.Steps,    "Steps",    Icons.AutoMirrored.Outlined.DirectionsWalk),
    NavItem(AppRoute.Settings, "Settings", Icons.Filled.Settings),
)

@Composable
fun FitTrackApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                navItems.forEach { item ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.hasRoute(item.route::class) } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontFamily = interFamily,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 11.sp,
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop back to the start destination, keeping the back stack clean
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Home,
            modifier = Modifier.padding(innerPadding),
            // M3 Expressive: spring-based transitions feel more natural than tween
            enterTransition = {
                fadeIn(animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f)) +
                    slideInVertically(animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f)) { it / 12 }
            },
            exitTransition = {
                fadeOut(animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f))
            },
            popEnterTransition = {
                fadeIn(animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f))
            },
            popExitTransition = {
                fadeOut(animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f)) +
                    slideOutVertically(animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f)) { it / 12 }
            },
        ) {
            composable<AppRoute.Home>     { HomeScreen(navController, hiltViewModel()) }
            composable<AppRoute.Log>      { LogScreen(hiltViewModel()) }
            composable<AppRoute.Steps>    { StepsScreen(hiltViewModel()) }
            composable<AppRoute.Settings> { SettingsScreen(hiltViewModel()) }
        }
    }
}
