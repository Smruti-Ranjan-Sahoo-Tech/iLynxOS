package com.termux.ilynxos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.termux.ilynxos.ui.theme.PeachWaveLayer1
import com.termux.ilynxos.ui.theme.PeachWaveLayer2
import com.termux.ilynxos.ui.theme.PeachWaveLayer3
import com.termux.ilynxos.ui.theme.WarmBackground

@Composable
fun WarmBackgroundScaffold(
    modifier: Modifier = Modifier,
    showWave: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {
        if (showWave) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Wave Layer 1 (Subtle upper peach mist)
                val path1 = Path().apply {
                    moveTo(0f, h * 0.74f)
                    cubicTo(
                        w * 0.25f, h * 0.82f,
                        w * 0.65f, h * 0.68f,
                        w, h * 0.78f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path1,
                    brush = Brush.verticalGradient(
                        colors = listOf(PeachWaveLayer1, PeachWaveLayer2),
                        startY = h * 0.70f,
                        endY = h
                    )
                )

                // Wave Layer 2 (Warm flowing peach curve)
                val path2 = Path().apply {
                    moveTo(0f, h * 0.83f)
                    cubicTo(
                        w * 0.30f, h * 0.76f,
                        w * 0.70f, h * 0.90f,
                        w, h * 0.81f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path2,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x40FFA767), Color(0x65FF8C3B)),
                        startY = h * 0.76f,
                        endY = h
                    )
                )

                // Wave Layer 3 (Bottom subtle glow)
                val path3 = Path().apply {
                    moveTo(0f, h * 0.91f)
                    cubicTo(
                        w * 0.40f, h * 0.86f,
                        w * 0.75f, h * 0.96f,
                        w, h * 0.89f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path3,
                    brush = Brush.verticalGradient(
                        colors = listOf(PeachWaveLayer3, Color(0x95FF701A)),
                        startY = h * 0.86f,
                        endY = h
                    )
                )
            }
        }

        content()
    }
}
