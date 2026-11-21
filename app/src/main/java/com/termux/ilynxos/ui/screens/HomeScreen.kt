package com.termux.ilynxos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.ilynxos.model.SessionState
import com.termux.ilynxos.runtime.LinuxEnvironmentManager
import com.termux.ilynxos.runtime.LinuxSessionService
import com.termux.ilynxos.runtime.PRootRuntimeEngine
import com.termux.ilynxos.ui.components.WarmBackgroundScaffold
import com.termux.ilynxos.ui.theme.CardBorder
import com.termux.ilynxos.ui.theme.CardSurface
import com.termux.ilynxos.ui.theme.CrimsonRed
import com.termux.ilynxos.ui.theme.OrangeAccentBg
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.SquircleAboutBg
import com.termux.ilynxos.ui.theme.SquircleAboutIcon
import com.termux.ilynxos.ui.theme.SquircleSettingsBg
import com.termux.ilynxos.ui.theme.SquircleSettingsIcon
import com.termux.ilynxos.ui.theme.SquircleTerminalBg
import com.termux.ilynxos.ui.theme.SquircleTerminalIcon
import com.termux.ilynxos.ui.theme.StatusRunning
import com.termux.ilynxos.ui.theme.StatusStarting
import com.termux.ilynxos.ui.theme.StatusStopped
import com.termux.ilynxos.ui.theme.TextDarkMuted
import com.termux.ilynxos.ui.theme.TextDarkPrimary
import com.termux.ilynxos.ui.theme.TextDarkSecondary

@Composable
fun HomeScreen(
    onOpenDesktop: () -> Unit,
    onOpenWireless: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val envManager = remember { LinuxEnvironmentManager.getInstance(context) }
    val runtimeEngine = remember { PRootRuntimeEngine.getInstance(context) }
    val sessionState by runtimeEngine.sessionState.collectAsState()
    val scrollState = rememberScrollState()

    val isRunning = sessionState is SessionState.Running
    val isStarting = sessionState is SessionState.Starting

    WarmBackgroundScaffold(showWave = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "iLynxOS Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextDarkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Noble Modern Edition • aarch64",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrangePrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // Header Logo Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(OrangePrimary, RoundedCornerShape(13.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Desktop Session Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Desktop Session",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        // Status Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    when {
                                        isRunning -> StatusRunning.copy(alpha = 0.12f)
                                        isStarting -> StatusStarting.copy(alpha = 0.12f)
                                        else -> StatusStopped.copy(alpha = 0.12f)
                                    },
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        when {
                                            isRunning -> StatusRunning
                                            isStarting -> StatusStarting
                                            else -> StatusStopped
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when {
                                    isRunning -> "RUNNING (:0)"
                                    isStarting -> "STARTING"
                                    else -> "STOPPED"
                                },
                                color = when {
                                    isRunning -> StatusRunning
                                    isStarting -> StatusStarting
                                    else -> StatusStopped
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (val state = sessionState) {
                            is SessionState.Running -> "LXQt desktop is live and responding on display ${state.display}."
                            is SessionState.Starting -> state.step
                            is SessionState.Error -> "Session error: ${state.message}"
                            else -> "Session inactive. Click Start Desktop to launch LXQt."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDarkSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Launch / Control Buttons - Responsive and unclipped
                    if (isRunning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Open Desktop Button
                            Button(
                                onClick = onOpenDesktop,
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(13.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangePrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open Desktop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Stop Button
                            Button(
                                onClick = { LinuxSessionService.stopService(context) },
                                modifier = Modifier
                                    .weight(0.75f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(13.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Stop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                LinuxSessionService.startService(context)
                                onOpenDesktop()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isStarting,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isStarting) "Booting Runtime..." else "Start Desktop Session",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Wireless Desktop Quick Access Card
            Card(
                onClick = onOpenWireless,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OrangeAccentBg, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CastConnected,
                                contentDescription = "Wireless Desktop",
                                tint = OrangePrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wireless Desktop",
                                color = TextDarkPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "VNC & HTML5 Web remote control",
                                color = TextDarkSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Navigation Tiles: Terminal, Settings, About
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Terminal Card
                Card(
                    onClick = onOpenTerminal,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(11.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(SquircleTerminalBg, RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = SquircleTerminalIcon,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = OrangePrimary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Terminal",
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Root Shell",
                            color = TextDarkSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Settings Card
                Card(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(11.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(SquircleSettingsBg, RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = SquircleSettingsIcon,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = OrangePrimary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Settings",
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Display & Input",
                            color = TextDarkSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // About Card
                Card(
                    onClick = onOpenAbout,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(11.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(SquircleAboutBg, RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = SquircleAboutIcon,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = OrangePrimary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "About",
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "v1.03 Info",
                            color = TextDarkSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // System Environment Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(OrangePrimary, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Computer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "System Environment",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp
                        )
                    }

                    InfoRow("Distribution", "Ubuntu 24.04 LTS (Noble Numbat)")
                    InfoRow("Desktop UI", "LXQt Desktop Environment")
                    InfoRow("Runtime Engine", "PRoot User-Space Sandbox")
                    InfoRow("Graphics Server", "Embedded X11 Server (libXlorie)")
                    InfoRow("Rootfs Location", envManager.rootfsDir.absolutePath)
                    InfoRow("Display Socket", "${envManager.tmpDir.absolutePath}/.X11-unix/X0")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Developer & iLynxOS Branding Footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.9f)),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Developed by Smruti Ranjan Sahoo",
                        color = TextDarkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "i-Lynx OS 1.03 (Noble Modern Edition) • Standalone Mobile Linux",
                        color = OrangePrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // Generous bottom spacer so content never hides under the system navigation bar
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextDarkMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.42f)
            )
            Text(
                text = value,
                color = TextDarkPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (value.startsWith("/")) FontFamily.Monospace else FontFamily.Default,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.58f)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
    }
}
