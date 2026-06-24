package com.henjicc.swiftformat.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.henjicc.swiftformat.feature.history.HistoryScreen
import com.henjicc.swiftformat.feature.home.HomeScreen
import com.henjicc.swiftformat.feature.progress.ConversionProgressScreen
import com.henjicc.swiftformat.feature.settings.SettingsScreen

/**
 * 底部导航三个同级目的地之间切换用 Material 的"淡入淡出（fade through）"：先淡出再淡入+轻微放大，
 * 而不是平移——三者是底部导航的并列顶级页面，没有左右顺序关系，不适合用 Pager 式整页平移。
 */
private const val FADE_THROUGH_EXIT_DURATION = 90
private const val FADE_THROUGH_ENTER_DURATION = 210

private fun fadeThroughEnter() = fadeIn(tween(FADE_THROUGH_ENTER_DURATION, delayMillis = FADE_THROUGH_EXIT_DURATION)) +
    scaleIn(initialScale = 0.92f, animationSpec = tween(FADE_THROUGH_ENTER_DURATION, delayMillis = FADE_THROUGH_EXIT_DURATION))

private fun fadeThroughExit() = fadeOut(tween(FADE_THROUGH_EXIT_DURATION))

/** 转换进度页是推入式全屏页（比底部导航深一层），用标准的"从右侧滑入/滑出"前进式转场。 */
private const val PUSH_TRANSITION_DURATION = 300

private fun pushEnter() = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(PUSH_TRANSITION_DURATION),
) + fadeIn(tween(PUSH_TRANSITION_DURATION))

private fun pushExit() = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(PUSH_TRANSITION_DURATION),
) + fadeOut(tween(PUSH_TRANSITION_DURATION))

/** 应用根布局：底部导航 + 内容区。转换进度页面（见 SPEC 4.5）是推入式全屏页，不显示底部导航。 */
@Composable
fun SwiftFormatApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    var convertHasFiles by remember { mutableStateOf(false) }
    val isConvertDestination = currentDestination?.route == TopLevelDestination.CONVERT.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentDestination?.route } &&
        !(isConvertDestination && convertHasFiles)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(destination.labelRes),
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.CONVERT.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeThroughEnter() },
            exitTransition = { fadeThroughExit() },
            popEnterTransition = { fadeThroughEnter() },
            popExitTransition = { fadeThroughExit() },
        ) {
            composable(TopLevelDestination.CONVERT.route) {
                HomeScreen(
                    onConversionStarted = { navController.navigate(CONVERSION_PROGRESS_ROUTE) },
                    onOpenActiveTask = { navController.navigate(CONVERSION_PROGRESS_ROUTE) },
                    onFileSelectionModeChange = { convertHasFiles = it },
                )
            }
            composable(TopLevelDestination.HISTORY.route) {
                HistoryScreen(onOpenProgress = { navController.navigate(CONVERSION_PROGRESS_ROUTE) })
            }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
            composable(
                CONVERSION_PROGRESS_ROUTE,
                enterTransition = { pushEnter() },
                popExitTransition = { pushExit() },
            ) {
                ConversionProgressScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
