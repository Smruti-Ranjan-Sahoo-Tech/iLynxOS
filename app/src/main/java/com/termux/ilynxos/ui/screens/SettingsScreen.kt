package com.termux.ilynxos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.ilynxos.runtime.LinuxEnvironmentManager
import com.termux.ilynxos.runtime.PRootRuntimeEngine
import com.termux.ilynxos.runtime.X11DisplayEngine
import com.termux.ilynxos.ui.components.WarmBackgroundScaffold
import com.termux.ilynxos.ui.theme.CardBorder
import com.termux.ilynxos.ui.theme.CardSurface
import com.termux.ilynxos.ui.theme.CrimsonRed
import com.termux.ilynxos.ui.theme.OrangeAccentBg
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.SquircleSettingsBg
import com.termux.ilynxos.ui.theme.SquircleSettingsIcon
import com.termux.ilynxos.ui.theme.TextDarkPrimary
import com.termux.ilynxos.ui.theme.TextDarkSecondary
import com.termux.ilynxos.ui.theme.WarmBackgroundSubtle

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onResetCompleted: () -> Unit
) {
    val context = LocalContext.current
    val envManager = remember { LinuxEnvironmentManager.getInstance(context) }
    val scrollState = rememberScrollState()

    var keepAwake by remember { mutableStateOf(true) }
    var hardwareAcceleration by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }

    WarmBackgroundScaffold(showWave = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextDarkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(OrangePrimary, RoundedCornerShape(11.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextDarkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Display Section
            SectionTitle(icon = Icons.Default.DisplaySettings, title = "Display & Rendering")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingSwitchRow(
                        title = "Hardware Acceleration",
                        subtitle = "Render X11 graphics via hardware ANativeWindow surface",
                        checked = hardwareAcceleration,
                        onCheckedChange = { hardwareAcceleration = it }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingSwitchRow(
                        title = "Keep Screen Awake",
                        subtitle = "Acquires wake lock while desktop session is running",
                        checked = keepAwake,
                        onCheckedChange = { keepAwake = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Audio & Input Section
            SectionTitle(icon = Icons.Default.Mouse, title = "Sound & Inputs")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingSwitchRow(
                        title = "PulseAudio Audio Bridge",
                        subtitle = "Forward Linux application audio to Android sound output",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Storage & Container Management
            SectionTitle(icon = Icons.Default.Storage, title = "Container Maintenance")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Container: ${LinuxEnvironmentManager.CONTAINER_NAME}",
                        color = TextDarkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Path: ${envManager.containerDir.absolutePath}",
                        color = TextDarkSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed),
                        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Container / Reinstall", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    containerColor = CardSurface,
                    title = { Text("Reset Container?", color = TextDarkPrimary, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "This will completely delete the extracted Linux filesystem and return the app to the first-launch setup screen. Your tar.gz backup remains safe.",
                            color = TextDarkSecondary,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showResetDialog = false
                            PRootRuntimeEngine.getInstance(context).stopDesktopSession()
                            X11DisplayEngine.getInstance(context).stopServer()
                            envManager.resetInstallation()
                            onResetCompleted()
                        }) {
                            Text("Reset & Reinstall", color = CrimsonRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Cancel", color = TextDarkSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = OrangePrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextDarkPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, color = TextDarkSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OrangePrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = CardBorder
            )
        )
    }
}
