// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Корневой Scaffold приложения.
 *
 * Здесь крутится NavHost и BottomNavBar. BottomNavBar скрывается на
 * вложенных экранах (например, на детали записи): пользователю удобнее
 * полностью отдать высоту контенту, а возвращаться через back-стрелку.
 */
@Composable
fun RootScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route } &&
        !imeVisible

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "root-content-fade",
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) BottomNavBar(navController = navController)
        },
    ) { padding ->
        Box(modifier = Modifier.alpha(contentAlpha)) {
            PocketReflectNavGraph(
                navController = navController,
                innerPadding = padding,
            )
        }
    }
}
