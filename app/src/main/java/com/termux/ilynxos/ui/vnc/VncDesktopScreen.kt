package com.termux.ilynxos.ui.vnc

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.ilynxos.ui.components.WarmBackgroundScaffold
import com.termux.ilynxos.ui.theme.CardBorder
import com.termux.ilynxos.ui.theme.CardSurface
import com.termux.ilynxos.ui.theme.ConsoleBackground
import com.termux.ilynxos.ui.theme.ConsoleBorder
import com.termux.ilynxos.ui.theme.ConsoleTextGreen
import com.termux.ilynxos.ui.theme.CrimsonRed
import com.termux.ilynxos.ui.theme.EmeraldGreen
import com.termux.ilynxos.ui.theme.OrangeAccentBg
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.StatusRunning
import com.termux.ilynxos.ui.theme.StatusStarting
import com.termux.ilynxos.ui.theme.StatusStopped
import com.termux.ilynxos.ui.theme.TextDarkMuted
import com.termux.ilynxos.ui.theme.TextDarkPrimary
import com.termux.ilynxos.ui.theme.TextDarkSecondary
import com.termux.ilynxos.vnc.NetworkDiscovery
import com.termux.ilynxos.vnc.QrCodeGenerator
import com.termux.ilynxos.vnc.VncServerManager
import com.termux.ilynxos.vnc.VncStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VncDesktopScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val vncManager = remember { VncServerManager.getInstance(context) }
    val networkDiscovery = remember { NetworkDiscovery.getInstance(context) }

    val networkInfo by networkDiscovery.networkInfo.collectAsState()
    val vncState by vncManager.state.collectAsState()
    val logs by vncManager.logs.collectAsState()

    var showPassword by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf(vncManager.getPassword()) }
    var portInput by remember { mutableStateOf(vncManager.getPort().toString()) }
    var selectedRes by remember { mutableStateOf(vncManager.getResolution()) }
    var isNewSession by remember { mutableStateOf(vncManager.isNewSession()) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    val isRunning = vncState.status == VncStatus.RUNNING
    val isStarting = vncState.status == VncStatus.STARTING

    LaunchedEffect(Unit) {
        while (true) {
            vncManager.checkStatus()
            delay(2500)
        }
    }

    val displayIp = networkInfo.ipAddress ?: "127.0.0.1"
    val fullVncAddress = "$displayIp:${vncState.port}"

    WarmBackgroundScaffold(showWave = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(CardSurface, CircleShape)
                            .border(1.dp, CardBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDarkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "VNC Remote Desktop",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "RFB / TigerVNC Server Management",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDarkSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            when {
                                isRunning -> EmeraldGreen.copy(alpha = 0.12f)
                                isStarting -> OrangePrimary.copy(alpha = 0.12f)
                                else -> TextDarkMuted.copy(alpha = 0.12f)
                            },
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when {
                                    isRunning -> StatusRunning
                                    isStarting -> StatusStarting
                                    else -> StatusStopped
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isRunning -> "LIVE (: ${vncState.display})"
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

            Spacer(modifier = Modifier.height(16.dp))

            // Network Connection Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (networkInfo.isConnected) EmeraldGreen.copy(alpha = 0.15f) else OrangeAccentBg,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (networkInfo.isConnected) EmeraldGreen else OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = networkInfo.ssid,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (networkInfo.isConnected) "IP: ${networkInfo.ipAddress ?: "Assigned"}" else "Connect to Wi-Fi to access from PC",
                            color = TextDarkSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { networkDiscovery.refresh() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh IP", tint = OrangePrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Connection Card (When Running)
            if (isRunning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VNC Server is Active",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                fontSize = 15.sp
                            )
                            IconButton(
                                onClick = { showQrDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = EmeraldGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Address Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface, RoundedCornerShape(10.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "VNC ADDRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDarkMuted)
                                Text(
                                    text = fullVncAddress,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextDarkPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(fullVncAddress))
                                    Toast.makeText(context, "Copied $fullVncAddress", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = OrangePrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Password Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface, RoundedCornerShape(10.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDarkMuted)
                                Text(
                                    text = vncManager.getPassword(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextDarkPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(vncManager.getPassword()))
                                    Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = OrangePrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Details Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Display :${vncState.display} (${if (vncState.display == 0) "Mirror" else "Virtual"})",
                                fontSize = 11.5.sp,
                                color = TextDarkSecondary
                            )
                            Text(
                                text = "Resolution: ${vncState.resolution}",
                                fontSize = 11.5.sp,
                                color = TextDarkSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Primary Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isRunning) {
                    Button(
                        onClick = {
                            scope.launch { vncManager.stop() }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Stop VNC", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val port = portInput.toIntOrNull() ?: 5901
                                vncManager.restart(port, passwordInput, isNewSession, selectedRes)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Restart", color = TextDarkPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                val port = portInput.toIntOrNull() ?: if (isNewSession) 5901 else 5900
                                vncManager.start(port, passwordInput, isNewSession, selectedRes)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isStarting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStarting) "Launching TigerVNC..." else "Start VNC Server",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Server Configuration",
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Selection
                    Text(text = "DESKTOP SESSION MODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDarkMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Virtual Session Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isNewSession) OrangeAccentBg else CardSurface,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isNewSession) OrangePrimary else CardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isNewSession = true
                                    portInput = "5901"
                                    vncManager.setNewSession(true)
                                }
                                .padding(vertical = 10.dp, horizontal = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Virtual (:1)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isNewSession) OrangePrimary else TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Independent headless session",
                                    fontSize = 10.5.sp,
                                    color = TextDarkSecondary
                                )
                            }
                        }

                        // Mirror Session Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (!isNewSession) OrangeAccentBg else CardSurface,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (!isNewSession) OrangePrimary else CardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isNewSession = false
                                    portInput = "5900"
                                    vncManager.setNewSession(false)
                                }
                                .padding(vertical = 10.dp, horizontal = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Mirror (:0)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (!isNewSession) OrangePrimary else TextDarkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Shares phone screen",
                                    fontSize = 10.5.sp,
                                    color = TextDarkSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Port & Password Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port", fontSize = 12.sp) },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = CardBorder
                            )
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                vncManager.setPassword(it)
                            },
                            label = { Text("Password", fontSize = 12.sp) },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = CardBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Resolution Chips
                    Text(text = "RESOLUTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDarkMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    val resolutions = listOf("1280x720", "1600x900", "1920x1080", "1024x768")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        resolutions.forEach { res ->
                            val isSelected = selectedRes == res
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) OrangePrimary else CardSurface,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) OrangePrimary else CardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedRes = res
                                        vncManager.setResolution(res)
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextDarkPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Client Connection Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "How to Connect from PC",
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. Connect your PC and phone to the same Wi-Fi network.\n" +
                               "2. Open any VNC viewer (TigerVNC, RealVNC, Remmina, or AVNC).\n" +
                               "3. Enter the VNC address: $fullVncAddress\n" +
                               "4. Enter password: ${vncManager.getPassword()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDarkSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Execution Logs Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogs = !showLogs },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "vncstart / vncstop Logs (${logs.size})",
                                fontWeight = FontWeight.SemiBold,
                                color = TextDarkPrimary,
                                fontSize = 13.sp
                            )
                        }

                        Icon(
                            if (showLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextDarkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showLogs) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(ConsoleBackground, RoundedCornerShape(10.dp))
                                .border(1.dp, ConsoleBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    text = "Ready. Logs from vncstart and vncstop will appear here.",
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            } else {
                                Text(
                                    text = logs.joinToString("\n"),
                                    color = ConsoleTextGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { vncManager.clearLogs() }) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Clear Logs", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // QR Code Dialog
    if (showQrDialog) {
        val qrBitmap = remember(fullVncAddress) {
            QrCodeGenerator.generateQrBitmap("vnc://$fullVncAddress")
        }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(text = "Scan to Connect (VNC)", fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "VNC QR Code",
                            modifier = Modifier
                                .size(220.dp)
                                .padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = fullVncAddress,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Password: ${vncManager.getPassword()}",
                        color = TextDarkSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Close", color = OrangePrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
