package com.termux.ilynxos.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.termux.ilynxos.runtime.LinuxEnvironmentManager
import com.termux.ilynxos.ui.navigation.Screen
import com.termux.ilynxos.ui.screens.AboutScreen
import com.termux.ilynxos.ui.screens.DesktopScreen
import com.termux.ilynxos.ui.screens.HomeScreen
import com.termux.ilynxos.ui.screens.InstallationScreen
import com.termux.ilynxos.ui.screens.SettingsScreen
import com.termux.ilynxos.ui.screens.SplashScreen
import com.termux.ilynxos.ui.screens.TerminalScreen
import com.termux.ilynxos.ui.screens.WelcomeScreen
import com.termux.ilynxos.ui.wireless.WirelessDesktopScreen
import com.termux.ilynxos.ui.theme.ILynxOSTheme
import com.termux.ilynxos.ui.theme.Slate950

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure window is transparent so underlying SurfaceViews (X11 desktop) can be composited without occlusion
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        window.setFormat(android.graphics.PixelFormat.TRANSLUCENT)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val envManager = LinuxEnvironmentManager.getInstance(this)

        setContent {
            ILynxOSTheme {
                AppNavigation(envManager)
            }
        }
    }
}

@Composable
fun AppNavigation(envManager: LinuxEnvironmentManager) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }

    // Hardware back press handling
    BackHandler(enabled = currentScreen != Screen.Home && currentScreen != Screen.Splash) {
        currentScreen = Screen.Home
    }

    when (currentScreen) {
        is Screen.Splash -> {
            SplashScreen(
                isInstalled = envManager.isInstalled(),
                onNavigateNext = { installed ->
                    currentScreen = if (installed) Screen.Home else Screen.Welcome
                }
            )
        }
        is Screen.Welcome -> {
            WelcomeScreen(
                onStartInstall = { currentScreen = Screen.Install }
            )
        }
        is Screen.Install -> {
            InstallationScreen(
                onInstallationComplete = { currentScreen = Screen.Home }
            )
        }
        is Screen.Home -> {
            HomeScreen(
                onOpenDesktop = {
                    android.util.Log.i("AppNavigation", "Switching screen to Screen.Desktop")
                    currentScreen = Screen.Desktop
                },
                onOpenWireless = { currentScreen = Screen.Wireless },
                onOpenTerminal = { currentScreen = Screen.Terminal },
                onOpenSettings = { currentScreen = Screen.Settings },
                onOpenAbout = { currentScreen = Screen.About }
            )
        }
        is Screen.Wireless -> {
            WirelessDesktopScreen(
                onNavigateBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.Desktop -> {
            DesktopScreen(
                onNavigateBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.Terminal -> {
            TerminalScreen(
                onNavigateBack = { currentScreen = Screen.Home }
            )
        }
        is Screen.Settings -> {
            SettingsScreen(
                onNavigateBack = { currentScreen = Screen.Home },
                onResetCompleted = { currentScreen = Screen.Welcome }
            )
        }
        is Screen.About -> {
            AboutScreen(
                onNavigateBack = { currentScreen = Screen.Home }
            )
        }
    }
}
