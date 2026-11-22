package com.termux.ilynxos.ui.navigation

sealed class Screen(val title: String) {
    object Splash : Screen("Splash")
    object Welcome : Screen("Welcome")
    object Install : Screen("Install")
    object Home : Screen("Home")
    object Desktop : Screen("Desktop")
    object Terminal : Screen("Terminal")
    object Vnc : Screen("VNC Desktop")
    object Settings : Screen("Settings")
    object About : Screen("About")
}
