package com.termux.ilynxos.ui.wireless

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
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
import androidx.compose.ui.platform.LocalContext
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
import com.termux.ilynxos.wireless.ServiceState
import com.termux.ilynxos.wireless.ServiceStatus

@Composable
fun WebModeCard(
    state: ServiceState,
    isEnabled: Boolean,
    isTlsEnabled: Boolean,
    isXpraInstalled: Boolean,
    isInstalling: Boolean,
    initialPassword: String,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleTls: (Boolean) -> Unit,
    onStart: (port: Int, pass: String, tls: Boolean) -> Unit,
    onStop: () -> Unit,
    onRestart: (port: Int, pass: String, tls: Boolean) -> Unit,
    onInstallDependencies: () -> Unit,
    onShowQr: (url: String) -> Unit
) {
    val context = LocalContext.current
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
                            .background(EmeraldGreen.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Web Mode (HTML5 Browser Desktop)",
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Access via any web browser (:0)",
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
                        checkedTrackColor = EmeraldGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Badge & Live URL
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = { onShowQr(state.address) },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(13.dp), tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("QR", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.address)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextDarkPrimary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Open", fontSize = 11.sp, color = TextDarkPrimary)
                        }
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
                    label = { Text("Web Port", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.35f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextDarkSecondary,
                        cursorColor = EmeraldGreen
                    )
                )

                // Password Field
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    label = { Text("Web Password", fontSize = 12.sp) },
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
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextDarkSecondary,
                        cursorColor = EmeraldGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TLS / HTTPS Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TLS Encryption (HTTPS / WSS)",
                        color = TextDarkPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isTlsEnabled) "Self-signed certificate (HTTPS / WSS)" else "Plain HTTP / WS (Recommended for local Wi-Fi)",
                        color = TextDarkSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isTlsEnabled,
                    onCheckedChange = onToggleTls,
                    enabled = !isRunning,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldGreen
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
                            val port = portText.toIntOrNull() ?: 14500
                            if (!isEnabled) onToggleEnabled(true)
                            onStart(port, passwordText, isTlsEnabled)
                        },
                        enabled = !isStarting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isStarting) "Starting..." else "Start Web", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Web", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            val port = portText.toIntOrNull() ?: 14500
                            onRestart(port, passwordText, isTlsEnabled)
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

            // Security note
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ℹ️ Browser may warn about self-signed certificate on first connect. Click 'Advanced' -> 'Proceed'.",
                color = TextDarkSecondary,
                fontSize = 11.sp
            )
        }
    }
}
