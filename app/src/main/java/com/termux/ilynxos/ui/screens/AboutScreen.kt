package com.termux.ilynxos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.ilynxos.ui.components.WarmBackgroundScaffold
import com.termux.ilynxos.ui.theme.CardBorder
import com.termux.ilynxos.ui.theme.CardSurface
import com.termux.ilynxos.ui.theme.OrangeAccentBg
import com.termux.ilynxos.ui.theme.OrangeAccentBorder
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.SquircleAboutBg
import com.termux.ilynxos.ui.theme.SquircleAboutIcon
import com.termux.ilynxos.ui.theme.TextDarkMuted
import com.termux.ilynxos.ui.theme.TextDarkPrimary
import com.termux.ilynxos.ui.theme.TextDarkSecondary

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

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
                        .border(1.dp, Color.White, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "About i-Lynx OS",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextDarkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // System Branding Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // White & Orange Hero Logo
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(OrangePrimary, RoundedCornerShape(22.dp))
                            .border(2.5.dp, Color.White, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesktopWindows,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "i-Lynx OS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextDarkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Version 1.03 (Noble Modern Edition)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrangePrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Full Native Linux Desktop Runtime for Android",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDarkSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Developer Profile Card (Prominent Developer Details)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(OrangeAccentBg, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Developer Information",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextDarkPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Lead Architect & Maintainer",
                                color = TextDarkSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Developer Name
                    DeveloperItemRow(
                        label = "Lead Developer",
                        value = "Smruti Ranjan Sahoo",
                        isHighlight = true
                    )

                    // Email Link
                    DeveloperItemRow(
                        label = "Email Support",
                        value = "s.r.sahoo370@gmail.com",
                        actionLabel = "Compose",
                        onClick = { openUrl("mailto:s.r.sahoo370@gmail.com") }
                    )

                    // GitHub Profile Link
                    DeveloperItemRow(
                        label = "GitHub Profile",
                        value = "github.com/Smruti-Ranjan-Sahoo-Tech",
                        actionLabel = "Visit",
                        onClick = { openUrl("https://github.com/Smruti-Ranjan-Sahoo-Tech") }
                    )

                    // Project Repo Link
                    DeveloperItemRow(
                        label = "Project Repository",
                        value = "github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS",
                        actionLabel = "Source",
                        onClick = { openUrl("https://github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Open Source Attributions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(SquircleAboutBg, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = SquircleAboutIcon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Core Components & Licenses",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    AttributionRow("PRoot", "PRoot Project (GPL v2+)", "User-space chroot/mount emulator engine")
                    AttributionRow("Termux:X11 / Lorie", "Termux Project (GPL v3)", "Embedded X11 hardware display server")
                    AttributionRow("LXQt Desktop", "LXQt Team (LGPL / GPL)", "Lightweight Qt modern desktop environment")
                    AttributionRow("Openbox & Picom", "Openbox / Picom (GPL / MIT)", "Window manager & dark glassmorphic compositor")
                    AttributionRow("Jetpack Compose", "Google / AOSP (Apache 2.0)", "Modern Android declarative reactive UI toolkit")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Architecture Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Native Sandboxing Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextDarkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "i-Lynx OS executes completely inside private Android app storage. It requires zero root privileges or external helper apps. All graphics render through libXlorie hardware surfaces via ANativeWindow with high FPS and low latency.",
                        color = TextDarkSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DeveloperItemRow(
    label: String,
    value: String,
    actionLabel: String? = null,
    isHighlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextDarkMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            if (actionLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(actionLabel, color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = if (isHighlight) OrangePrimary else TextDarkPrimary,
            fontSize = if (isHighlight) 15.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = if (value.contains("/")) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun AttributionRow(title: String, license: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextDarkPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(license, color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(desc, color = TextDarkSecondary, fontSize = 11.sp)
    }
}
