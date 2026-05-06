package com.fittrack.app

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.theme.FamilyColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.ui.family.FamilyScreen
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
    @Serializable data class  Log(val mealFilter: String? = null) : AppRoute
    @Serializable data object Steps    : AppRoute
    @Serializable data object Family   : AppRoute
    @Serializable data object Settings : AppRoute
}

data class NavItem(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun FitTrackApp(goalsRepository: GoalsRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Reactively observe family toggle
    val familyEnabled by goalsRepository.familyEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)

    val navItems = buildList {
        add(NavItem(AppRoute.Home,     "Home",     Icons.Filled.Home))
        add(NavItem(AppRoute.Log(),    "Diary",    Icons.Filled.Restaurant))
        add(NavItem(AppRoute.Steps,    "Steps",    Icons.AutoMirrored.Outlined.DirectionsWalk))
        if (familyEnabled) {
            add(NavItem(AppRoute.Family, "Family", Icons.Filled.Favorite))
        }
        add(NavItem(AppRoute.Settings, "Settings", Icons.Filled.Settings))
    }

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

                    // Use the Family pink accent for the Family tab
                    val isFamilyItem = item.route is AppRoute.Family

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
                        colors = if (isFamilyItem) {
                            NavigationBarItemDefaults.colors(
                                selectedIconColor   = FamilyColors.primary,
                                selectedTextColor   = FamilyColors.primary,
                                unselectedIconColor = FamilyColors.primaryLight,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor      = FamilyColors.primarySurface,
                            )
                        } else {
                            NavigationBarItemDefaults.colors(
                                selectedIconColor   = MaterialTheme.colorScheme.primary,
                                selectedTextColor   = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                            )
                        },
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
            composable<AppRoute.Log>      { backStackEntry ->
                val route = backStackEntry.toRoute<AppRoute.Log>()
                LogScreen(hiltViewModel(), mealFilter = route.mealFilter)
            }
            composable<AppRoute.Steps>    { StepsScreen(hiltViewModel()) }
            composable<AppRoute.Family>   { FamilyScreen(hiltViewModel()) }
            composable<AppRoute.Settings> { SettingsScreen(hiltViewModel()) }
        }
    }
}

