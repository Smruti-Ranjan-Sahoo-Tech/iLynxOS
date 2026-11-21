package com.termux.ilynxos.ui.wireless

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.ilynxos.ui.theme.CardBorder
import com.termux.ilynxos.ui.theme.CardSurface
import com.termux.ilynxos.ui.theme.CrimsonRed
import com.termux.ilynxos.ui.theme.EmeraldGreen
import com.termux.ilynxos.ui.theme.OrangeAccentBg
import com.termux.ilynxos.ui.theme.OrangeAccentBorder
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.TextDarkPrimary
import com.termux.ilynxos.ui.theme.TextDarkSecondary
import com.termux.ilynxos.ui.theme.WarmBackground
import com.termux.ilynxos.wireless.ServiceState
import com.termux.ilynxos.wireless.ServiceStatus

@Composable
fun VncModeCard(
    state: ServiceState,
    networkIp: String? = null,
    isEnabled: Boolean,
    isNewSession: Boolean = true,
    initialPassword: String,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleNewSession: (Boolean) -> Unit = {},
    onStart: (port: Int, pass: String, newSession: Boolean) -> Unit,
    onStop: () -> Unit,
    onRestart: (port: Int, pass: String, newSession: Boolean) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var portText by remember(state.port) { mutableStateOf(state.port.toString()) }
    var passwordText by remember(initialPassword) { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isRunning = state.status == ServiceStatus.RUNNING
    val isStarting = state.status == ServiceStatus.STARTING

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(OrangeAccentBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VNC Mode (x11vnc)",
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isNewSession) "Dedicated Virtual Session (:1)" else "Mirror Phone Desktop (:0)",
                            color = TextDarkSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OrangePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Session Mode Selector: New Dedicated Session (:1) vs Mirror Current Screen (:0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNewSession) "Dedicated Virtual Desktop (:1)" else "Mirror Phone Desktop (:0)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = if (isNewSession) "New 1080p desktop with low latency & full input" else "Shares the phone screen directly",
                        fontSize = 11.sp,
                        color = TextDarkSecondary
                    )
                }
                Switch(
                    checked = isNewSession,
                    onCheckedChange = onToggleNewSession,
                    enabled = !isRunning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OrangePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val badgeColor = when (state.status) {
                    ServiceStatus.RUNNING -> EmeraldGreen
                    ServiceStatus.STARTING -> OrangePrimary
                    ServiceStatus.ERROR -> CrimsonRed
                    else -> TextDarkSecondary
                }
                val badgeText = when (state.status) {
                    ServiceStatus.RUNNING -> "RUNNING"
                    ServiceStatus.STARTING -> "STARTING..."
                    ServiceStatus.ERROR -> "ERROR"
                    ServiceStatus.NOT_INSTALLED -> "NOT INSTALLED"
                    ServiceStatus.STOPPED -> "STOPPED"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(badgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (isRunning && state.pid != null) {
                        Text(
                            text = " (PID ${state.pid})",
                            color = TextDarkSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isRunning && state.address != null) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(state.address))
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAccentBorder),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = OrangeAccentBg),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(state.address, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextDarkPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // RealVNC / TigerVNC Quick Connect Banner
            if (isRunning) {
                Spacer(modifier = Modifier.height(10.dp))
                val targetAddress = state.address ?: "${networkIp ?: "127.0.0.1"}:${state.port}"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "REALVNC / RFB VIEWER TARGET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Display :0",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextDarkSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = targetAddress,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextDarkPrimary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { clipboardManager.setText(AnnotatedString(targetAddress)) },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAccentBorder),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = OrangeAccentBg),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = OrangePrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Target", fontSize = 11.sp, color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (passwordText.isNotEmpty()) "Authentication: VNC Password enabled ($passwordText)" else "Authentication: None (no password required in RealVNC)",
                            fontSize = 11.sp,
                            color = TextDarkSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Configuration Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Port Field
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Port", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.35f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary,
                        focusedLabelColor = OrangePrimary,
                        unfocusedLabelColor = TextDarkSecondary,
                        cursorColor = OrangePrimary
                    )
                )

                // Password Field
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    label = { Text("VNC Password", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextDarkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.weight(0.65f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary,
                        focusedLabelColor = OrangePrimary,
                        unfocusedLabelColor = TextDarkSecondary,
                        cursorColor = OrangePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isRunning) {
                    Button(
                        onClick = {
                            val port = portText.toIntOrNull() ?: 5900
                            if (!isEnabled) onToggleEnabled(true)
                            onStart(port, passwordText, isNewSession)
                        },
                        enabled = !isStarting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isStarting) "Starting..." else "Start VNC", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                    }
                } else {
                    Button(
                        onClick = onStop,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop VNC", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            val port = portText.toIntOrNull() ?: 5900
                            onRestart(port, passwordText, isNewSession)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextDarkPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart", color = TextDarkPrimary, maxLines = 1, softWrap = false)
                    }
                }
            }

            // Security & Compatibility note
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚡ RealVNC Viewer compatible: direct RFB screen sharing without damage lag (-noxdamage).",
                color = TextDarkSecondary,
                fontSize = 11.sp
            )
        }
    }
}
