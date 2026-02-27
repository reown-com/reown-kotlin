package com.reown.sample.wallet.ui.routes.host

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.reown.sample.common.ui.theme.blue_accent
import com.reown.sample.wallet.R
import com.reown.sample.wallet.ui.routes.Route

enum class BottomBarItem(val route: Route, val label: String, @DrawableRes val icon: Int) {
    WALLETS(Route.Wallets, "Wallets", R.drawable.ic_wallet),
    CONNECTED_APPS(Route.ConnectedApps, "Connected Apps", R.drawable.ic_stack),
    SETTINGS(Route.Settings, "Settings", R.drawable.ic_settings);

    companion object {
        val orderedList = listOf(WALLETS, CONNECTED_APPS, SETTINGS)
    }
}

@Composable
internal fun rememberBottomBarMutableState(): MutableState<BottomBarState> {
    return remember { mutableStateOf(BottomBarState()) }
}

data class BottomBarState(
    val doesConnectionsItemHaveNotifications: Boolean = false,
    val isDisplayed: Boolean = true,
)


@Composable
fun BottomBar(navController: NavController, state: BottomBarState, screens: Array<BottomBarItem> = BottomBarItem.values()) {
    Column {
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
        BottomNavigation(backgroundColor = MaterialTheme.colors.background, elevation = 0.dp) {
            val currentRoute = currentRoute(navController)
            screens.forEach { screen ->
                val hasNotification = when (screen) {
                    BottomBarItem.WALLETS -> state.doesConnectionsItemHaveNotifications
                    else -> false
                }

                BottomNavigationItem(
                    label = { Text(screen.label, fontSize = 12.sp, maxLines = 1) },
                    icon = { BottomNavIconWithBadge(screen.icon, hasNotification) },
                    selected = currentRoute == screen.route.path,
                    onClick = {
                        navController.navigate(screen.route.path) {
                            popUpTo(Route.Wallets.path) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

@Composable
fun BottomNavIconWithBadge(@DrawableRes icon: Int, hasNotification: Boolean) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(28.dp)
    ) {
        Icon(
            painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )

        if (hasNotification) {
            val color = blue_accent
            val bgColor = MaterialTheme.colors.background
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = bgColor)
                drawCircle(color = color, radius = 3.dp.toPx())
            }
        }
    }
}
