package com.termux.ilynxos.ui.wireless

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.ilynxos.ui.theme.CardBorder
import com.termux.ilynxos.ui.theme.CardSurface
import com.termux.ilynxos.ui.theme.ConsoleBackground
import com.termux.ilynxos.ui.theme.ConsoleBorder
import com.termux.ilynxos.ui.theme.ConsoleTextGreen
import com.termux.ilynxos.ui.theme.CrimsonRed
import com.termux.ilynxos.ui.theme.EmeraldGreen
import com.termux.ilynxos.ui.theme.OrangeAccentBg
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.TextDarkPrimary
import com.termux.ilynxos.ui.theme.TextDarkSecondary
import com.termux.ilynxos.ui.theme.WarmBackground
import com.termux.ilynxos.wireless.ServiceStatus
import com.termux.ilynxos.wireless.WirelessDesktopManager
import kotlinx.coroutines.launch

@Composable
fun WirelessDesktopScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val manager = remember { WirelessDesktopManager.getInstance(context) }

    val networkInfo by manager.networkDiscovery.networkInfo.collectAsState()
    val vncState by manager.vncManager.state.collectAsState()
    val webState by manager.webManager.state.collectAsState()
    val isXpraInstalled by manager.webManager.isXpraInstalled.collectAsState()
    val isInstalling by manager.isInstallingDependencies.collectAsState()
    val logs by manager.logs.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }
    var qrDialogUrl by remember { mutableStateOf("") }
    var qrDialogPassword by remember { mutableStateOf("") }
    var showLogs by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            manager.checkAllStatuses()
            kotlinx.coroutines.delay(2000)
        }
    }

    val isVncRunning = vncState.status == ServiceStatus.RUNNING
    val isWebRunning = webState.status == ServiceStatus.RUNNING
    val isAnyRunning = isVncRunning || isWebRunning

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(CardSurface, CircleShape)
                        .border(1.dp, CardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextDarkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CastConnected,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Wireless Desktop",
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Text(
                        text = "Access and control iLynxOS from any device on Wi-Fi",
                        color = TextDarkSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Global Master Controls Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remote Desktop Master Control",
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        // Status Chip
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isAnyRunning) EmeraldGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isAnyRunning) "ACTIVE" else "OFFLINE",
                                color = if (isAnyRunning) EmeraldGreen else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch { manager.startAll() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start All", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                scope.launch { manager.stopAll() }
                            },
                            enabled = isAnyRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop All", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // A. Network Information Card
            NetworkInfoCard(
                networkInfo = networkInfo,
                onRefresh = { manager.networkDiscovery.refresh() },
                onShowQr = {
                    val info = manager.getConnectionInfo()
                    qrDialogUrl = info.webUrl ?: "https://${networkInfo.ipAddress ?: "127.0.0.1"}:14500/"
                    qrDialogPassword = manager.webManager.getPassword()
                    showQrDialog = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // B. VNC Mode Card
            VncModeCard(
                state = vncState,
                networkIp = networkInfo.ipAddress,
                isEnabled = manager.vncManager.isEnabled(),
                initialPassword = manager.vncManager.getPassword(),
                onToggleEnabled = { manager.vncManager.setEnabled(it) },
                onStart = { port, pass ->
                    scope.launch { manager.startVnc(port, pass) }
                },
                onStop = {
                    scope.launch { manager.stopVnc() }
                },
                onRestart = { port, pass ->
                    scope.launch { manager.restartVnc(port, pass) }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // C. Web Mode Card
            WebModeCard(
                state = webState,
                isEnabled = manager.webManager.isEnabled(),
                isTlsEnabled = manager.webManager.isTlsEnabled(),
                isXpraInstalled = isXpraInstalled,
                isInstalling = isInstalling,
                initialPassword = manager.webManager.getPassword(),
                onToggleEnabled = { manager.webManager.setEnabled(it) },
                onToggleTls = { manager.webManager.setTlsEnabled(it) },
                onStart = { port, pass, tls ->
                    scope.launch { manager.startWeb(port, pass, tls) }
                },
                onStop = {
                    scope.launch { manager.stopWeb() }
                },
                onRestart = { port, pass, tls ->
                    scope.launch { manager.restartWeb(port, pass, tls) }
                },
                onInstallDependencies = {
                    scope.launch { manager.installDependencies {} }
                },
                onShowQr = { url ->
                    qrDialogUrl = url
                    qrDialogPassword = manager.webManager.getPassword()
                    showQrDialog = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // D. Logs & Terminal Output Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Service Logs & Output",
                                color = TextDarkPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showLogs = !showLogs }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = if (showLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextDarkSecondary
                                )
                            }
                        }
                    }

                    if (showLogs) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(ConsoleBackground, RoundedCornerShape(10.dp))
                                .border(1.dp, ConsoleBorder, RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text(
                                        text = logs.ifEmpty { "No active logs." },
                                        color = ConsoleTextGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { clipboardManager.setText(AnnotatedString(logs)) },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextDarkSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Logs", fontSize = 11.sp, color = TextDarkSecondary)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { manager.clearLogs() },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(13.dp), tint = CrimsonRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 11.sp, color = CrimsonRed)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        // QR Code Dialog
        if (showQrDialog) {
            ConnectionQrDialog(
                url = qrDialogUrl,
                password = qrDialogPassword,
                onDismiss = { showQrDialog = false }
            )
        }
    }
}
