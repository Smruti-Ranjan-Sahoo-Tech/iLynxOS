package com.termux.ilynxos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.ilynxos.runtime.PRootRuntimeEngine
import com.termux.ilynxos.ui.components.WarmBackgroundScaffold
import com.termux.ilynxos.ui.theme.CardBorder
import com.termux.ilynxos.ui.theme.CardSurface
import com.termux.ilynxos.ui.theme.ConsoleBackground
import com.termux.ilynxos.ui.theme.ConsoleBorder
import com.termux.ilynxos.ui.theme.ConsoleTextCyan
import com.termux.ilynxos.ui.theme.ConsoleTextError
import com.termux.ilynxos.ui.theme.ConsoleTextGreen
import com.termux.ilynxos.ui.theme.ConsoleTextWhite
import com.termux.ilynxos.ui.theme.OrangeAccentBg
import com.termux.ilynxos.ui.theme.OrangeAccentBorder
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.OrangePrimaryDark
import com.termux.ilynxos.ui.theme.TextDarkMuted
import com.termux.ilynxos.ui.theme.TextDarkPrimary
import com.termux.ilynxos.ui.theme.TextDarkSecondary
import com.termux.ilynxos.ui.theme.WarmPillBackground
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val runtimeEngine = remember { PRootRuntimeEngine.getInstance(context) }
    val clipboardManager = LocalClipboardManager.current

    val terminalHistory = remember {
        mutableStateListOf(
            "iLynxOS 1.03 (Noble Modern Edition) [PRoot Container Linux]",
            "Linux 6.1.0-ilynxos aarch64",
            "Type a command below or tap a quick chip to execute.",
            "---"
        )
    }

    var inputCommand by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf("cat /etc/os-release") }
    var isExecuting by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val quickChipScrollState = rememberScrollState()

    val quickCommands = listOf(
        "termux-battery-status",
        "termux-wifi-connectioninfo",
        "termux-clipboard-get",
        "termux-toast 'Hello from iLynxOS'",
        "termux-tts-speak 'iLynxOS is ready'",
        "cat /etc/os-release",
        "uname -a",
        "df -h",
        "free -m",
        "whoami",
        "ps aux",
        "which start-ilynxos-desktop"
    )

    fun runCommand(cmd: String) {
        if (cmd.isBlank() || isExecuting) return
        val commandToRun = cmd.trim()
        selectedChip = commandToRun
        inputCommand = ""
        isExecuting = true
        terminalHistory.add("root@iLynxOS:~# $commandToRun")

        scope.launch {
            try {
                runtimeEngine.executeCliCommand(commandToRun) { line ->
                    terminalHistory.add(line)
                }
            } catch (e: Exception) {
                terminalHistory.add("[Error] ${e.message}")
            } finally {
                isExecuting = false
            }
        }
    }

    LaunchedEffect(terminalHistory.size) {
        if (terminalHistory.isNotEmpty()) {
            listState.animateScrollToItem(terminalHistory.size - 1)
        }
    }

    WarmBackgroundScaffold(showWave = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Toolbar (Matching image.png left screen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDarkPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // White & Orange Squircle with Terminal Icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(OrangePrimary, RoundedCornerShape(11.dp))
                            .border(1.5.dp, Color.White, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Linux Shell",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextDarkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy Output Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangeAccentBg, RoundedCornerShape(10.dp))
                            .border(1.dp, OrangeAccentBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                val fullText = terminalHistory.joinToString("\n")
                                clipboardManager.setText(AnnotatedString(fullText))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = { terminalHistory.clear() }) {
                        Icon(
                            Icons.Default.ClearAll,
                            contentDescription = "Clear",
                            tint = TextDarkSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Command Chips (Matching image.png: Active chip highlighted in orange)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(quickChipScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickCommands.forEach { cmd ->
                    val isActive = selectedChip == cmd
                    Box(
                        modifier = Modifier
                            .background(
                                if (isActive) OrangeAccentBg else WarmPillBackground,
                                RoundedCornerShape(20.dp)
                            )
                            .border(
                                if (isActive) 1.5.dp else 1.dp,
                                if (isActive) OrangePrimary else CardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { runCommand(cmd) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cmd,
                            fontSize = 12.sp,
                            color = if (isActive) OrangePrimaryDark else TextDarkPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dark Console Window (Matching image.png left screen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ConsoleBackground),
                border = BorderStroke(1.dp, ConsoleBorder)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    items(terminalHistory) { line ->
                        val color = when {
                            line.startsWith("root@iLynxOS") -> ConsoleTextGreen
                            line.startsWith("[Error]") -> ConsoleTextError
                            line.startsWith("iLynxOS") -> ConsoleTextCyan
                            line.startsWith("Linux") -> ConsoleTextCyan
                            else -> ConsoleTextWhite
                        }
                        Text(
                            text = line,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Command Input Field & Orange Send Button (Matching image.png left screen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter command...", color = TextDarkMuted, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { runCommand(inputCommand) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        cursorColor = OrangePrimary
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Solid Orange Send Button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            if (!isExecuting && inputCommand.isNotBlank()) OrangePrimary else OrangePrimary.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !isExecuting && inputCommand.isNotBlank()) {
                            runCommand(inputCommand)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
