package com.termux.ilynxos.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.termux.ilynxos.model.SessionState
import com.termux.ilynxos.runtime.LinuxSessionService
import com.termux.ilynxos.runtime.PRootRuntimeEngine
import com.termux.ilynxos.runtime.X11DisplayEngine
import com.termux.ilynxos.ui.theme.CardBorder
import com.termux.ilynxos.ui.theme.CrimsonRed
import com.termux.ilynxos.ui.theme.EmeraldGreen
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.Slate900
import com.termux.ilynxos.ui.theme.Slate950
import com.termux.ilynxos.ui.theme.StatusRunning
import com.termux.x11.LorieView
import com.termux.x11.input.InputStub
import kotlin.math.roundToInt

enum class MouseMode(val label: String, val description: String) {
    TRACKPAD("Trackpad Mouse (Relative)", "Slide to move cursor, tap to click, 2-finger tap for right-click"),
    TOUCH("Direct Touch (Absolute)", "Tap directly where you want to click on the desktop"),
    STYLUS("Stylus Pointer", "Direct high-precision stylus tracking")
}

data class DesktopResolution(val width: Int, val height: Int, val name: String)

@Composable
fun DesktopScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val runtimeEngine = remember { PRootRuntimeEngine.getInstance(context) }
    val sessionState by runtimeEngine.sessionState.collectAsState()
    val x11Engine = remember { X11DisplayEngine.getInstance(context) }
    var currentLorieView by remember { mutableStateOf<LorieView?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var showToolbar by remember { mutableStateOf(true) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showMouseModeDialog by remember { mutableStateOf(false) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }

    // Mouse & Touchpad configuration state
    var mouseMode by remember { mutableStateOf(MouseMode.TRACKPAD) }
    var showMousePad by remember { mutableStateOf(true) }
    var isDragLocked by remember { mutableStateOf(false) }
    var pointerSensitivity by remember { mutableFloatStateOf(1.2f) }
    var invertScroll by remember { mutableStateOf(false) }

    // X11 Mouse Pad overlay position state
    var padOffsetX by remember { mutableFloatStateOf(0f) }
    var padOffsetY by remember { mutableFloatStateOf(0f) }
    var isPadMinimized by remember { mutableStateOf(false) }

    var isFullscreen by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }

    val isDeviceLandscape = isLandscape || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Matrix for transforming Android view touch coordinates to native X11 coordinates
    val inputMatrix = remember { Matrix() }

    val resolutions = remember {
        listOf(
            DesktopResolution(0, 0, "Native Fit (Device Screen)"),
            DesktopResolution(1280, 720, "1280 × 720 (720p HD)"),
            DesktopResolution(1920, 1080, "1920 × 1080 (1080p FHD)"),
            DesktopResolution(1024, 768, "1024 × 768 (XGA 4:3)"),
            DesktopResolution(1600, 900, "1600 × 900 (16:9)")
        )
    }
    var selectedResolution by remember { mutableStateOf(resolutions[0]) }

    // Helper: Sends mouse click with asynchronous down/up delay (required by X11/Qt/GTK)
    fun sendClick(button: Int, relative: Boolean = true, delayMs: Long = 40L) {
        val view = currentLorieView ?: return
        view.sendMouseEvent(0f, 0f, button, true, relative)
        mainHandler.postDelayed({
            view.sendMouseEvent(0f, 0f, button, false, relative)
        }, delayMs)
    }

    // Toggle drag lock state
    fun toggleDragLock() {
        val view = currentLorieView ?: return
        val newLocked = !isDragLocked
        isDragLocked = newLocked
        view.sendMouseEvent(0f, 0f, InputStub.BUTTON_LEFT, newLocked, true)
    }

    // Toggle immersive full screen
    LaunchedEffect(isFullscreen) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Handle screen orientation toggling
    LaunchedEffect(isLandscape) {
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Reset orientation on exiting DesktopScreen and ensure transparent window handling
    DisposableEffect(Unit) {
        val window = activity?.window
        val oldBackground = window?.decorView?.background
        window?.let { w ->
            w.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            w.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        }
        onDispose {
            window?.let { w ->
                w.setBackgroundDrawable(oldBackground)
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Integrated Native X11 Surface
        AndroidView(
            factory = { ctx ->
                x11Engine.getOrCreateLorieView(ctx).apply {
                    currentLorieView = this
                    x11Engine.attachLorieView(this)

                    // Track input transformation matrix to translate touch points to X11 resolution
                    setCallback { _, _, matrix ->
                        inputMatrix.set(matrix)
                    }

                    var lastTouchX = 0f
                    var lastTouchY = 0f
                    var downX = 0f
                    var downY = 0f
                    var downTime = 0L
                    var lastTapTime = 0L
                    var lastTapX = 0f
                    var lastTapY = 0f
                    var isDoubleTapDragging = false
                    var isTwoFingerTap = false
                    var twoFingerDownTime = 0L

                    // External Hardware Mouse tracking
                    var savedMouseButtonMask = 0
                    val mouseButtonMap = arrayOf(
                        MotionEvent.BUTTON_PRIMARY to InputStub.BUTTON_LEFT,
                        MotionEvent.BUTTON_SECONDARY to InputStub.BUTTON_RIGHT,
                        MotionEvent.BUTTON_TERTIARY to InputStub.BUTTON_MIDDLE
                    )

                    fun syncMouseButtons(newMask: Int) {
                        for ((mask, x11Btn) in mouseButtonMap) {
                            val wasDown = (savedMouseButtonMask and mask) != 0
                            val isNowDown = (newMask and mask) != 0
                            if (wasDown != isNowDown) {
                                sendMouseEvent(0f, 0f, x11Btn, isNowDown, true)
                            }
                        }
                        savedMouseButtonMask = newMask
                    }

                    fun isHardwareMouseEvent(event: MotionEvent): Boolean {
                        return event.isFromSource(InputDevice.SOURCE_MOUSE) ||
                               event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE) ||
                               (event.pointerCount > 0 && event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE) ||
                               event.buttonState != 0 ||
                               savedMouseButtonMask != 0
                    }

                    setOnTouchListener { _, event ->
                        // 1. Process Hardware External Mouse events (Left, Right, Middle drag & drop)
                        if (isHardwareMouseEvent(event)) {
                            syncMouseButtons(event.buttonState)

                            val pts = floatArrayOf(event.x, event.y)
                            inputMatrix.mapPoints(pts)

                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                    sendMouseEvent(pts[0], pts[1], InputStub.BUTTON_UNDEFINED, false, false)
                                }
                                MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE -> {
                                    syncMouseButtons(event.buttonState)
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    syncMouseButtons(event.buttonState)
                                }
                            }
                            return@setOnTouchListener true
                        }

                        // 2. Process Touchscreen / Stylus gesture events
                        when (mouseMode) {
                            MouseMode.TRACKPAD -> {
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        downX = event.x
                                        downY = event.y
                                        lastTouchX = event.x
                                        lastTouchY = event.y
                                        downTime = System.currentTimeMillis()
                                        isTwoFingerTap = false

                                        // Double-tap and hold to drag
                                        val timeSinceLast = downTime - lastTapTime
                                        val tapDist = Math.hypot((event.x - lastTapX).toDouble(), (event.y - lastTapY).toDouble())
                                        if (timeSinceLast < 300 && tapDist < 50) {
                                            isDoubleTapDragging = true
                                            sendMouseEvent(0f, 0f, InputStub.BUTTON_LEFT, true, true)
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_POINTER_DOWN -> {
                                        if (event.pointerCount == 2) {
                                            isTwoFingerTap = true
                                            twoFingerDownTime = System.currentTimeMillis()
                                            lastTouchY = event.getY(0)
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_MOVE -> {
                                        if (event.pointerCount == 1) {
                                            val dx = (event.x - lastTouchX) * pointerSensitivity
                                            val dy = (event.y - lastTouchY) * pointerSensitivity
                                            sendMouseEvent(dx, dy, InputStub.BUTTON_UNDEFINED, false, true)
                                            lastTouchX = event.x
                                            lastTouchY = event.y
                                        } else if (event.pointerCount >= 2) {
                                            val deltaY = event.getY(0) - lastTouchY
                                            val factor = if (invertScroll) 1.6f else -1.6f
                                            sendMouseWheelEvent(0f, deltaY * factor)
                                            lastTouchY = event.getY(0)
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_POINTER_UP -> {
                                        if (isTwoFingerTap && event.pointerCount == 2) {
                                            val elapsed = System.currentTimeMillis() - twoFingerDownTime
                                            if (elapsed < 350) {
                                                // Two-finger tap -> Right Click (context menu)
                                                sendClick(InputStub.BUTTON_RIGHT, relative = true)
                                            }
                                            isTwoFingerTap = false
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_UP -> {
                                        if (isDoubleTapDragging) {
                                            sendMouseEvent(0f, 0f, InputStub.BUTTON_LEFT, false, true)
                                            isDoubleTapDragging = false
                                        } else if (!isTwoFingerTap) {
                                            val dist = Math.hypot((event.x - downX).toDouble(), (event.y - downY).toDouble())
                                            val duration = System.currentTimeMillis() - downTime
                                            if (dist < 25 && duration < 300) {
                                                // Single tap -> Left Click
                                                sendClick(InputStub.BUTTON_LEFT, relative = true)
                                                lastTapTime = System.currentTimeMillis()
                                                lastTapX = event.x
                                                lastTapY = event.y
                                            }
                                        }
                                        isTwoFingerTap = false
                                        true
                                    }

                                    MotionEvent.ACTION_CANCEL -> {
                                        if (isDoubleTapDragging) {
                                            sendMouseEvent(0f, 0f, InputStub.BUTTON_LEFT, false, true)
                                            isDoubleTapDragging = false
                                        }
                                        isTwoFingerTap = false
                                        true
                                    }

                                    else -> false
                                }
                            }

                            MouseMode.TOUCH -> {
                                val pts = floatArrayOf(event.x, event.y)
                                inputMatrix.mapPoints(pts)
                                val x11X = pts[0]
                                val x11Y = pts[1]

                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        downX = event.x
                                        downY = event.y
                                        lastTouchY = event.y
                                        sendMouseEvent(x11X, x11Y, InputStub.BUTTON_UNDEFINED, false, false)
                                        sendMouseEvent(x11X, x11Y, InputStub.BUTTON_LEFT, true, false)
                                        true
                                    }

                                    MotionEvent.ACTION_POINTER_DOWN -> {
                                        if (event.pointerCount == 2) {
                                            // Two-finger tap in direct mode = Right Click
                                            sendMouseEvent(x11X, x11Y, InputStub.BUTTON_RIGHT, true, false)
                                            mainHandler.postDelayed({
                                                sendMouseEvent(x11X, x11Y, InputStub.BUTTON_RIGHT, false, false)
                                            }, 40)
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_MOVE -> {
                                        if (event.pointerCount == 1) {
                                            sendMouseEvent(x11X, x11Y, InputStub.BUTTON_LEFT, true, false)
                                        } else if (event.pointerCount >= 2) {
                                            val deltaY = event.getY(0) - lastTouchY
                                            val factor = if (invertScroll) 1.5f else -1.5f
                                            sendMouseWheelEvent(0f, deltaY * factor)
                                            lastTouchY = event.getY(0)
                                        }
                                        true
                                    }

                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        sendMouseEvent(x11X, x11Y, InputStub.BUTTON_LEFT, false, false)
                                        true
                                    }

                                    else -> false
                                }
                            }

                            MouseMode.STYLUS -> {
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        sendMouseEvent(event.x, event.y, InputStub.BUTTON_LEFT, true, false)
                                        true
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        sendMouseEvent(event.x, event.y, InputStub.BUTTON_LEFT, true, false)
                                        true
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        sendMouseEvent(event.x, event.y, InputStub.BUTTON_LEFT, false, false)
                                        true
                                    }
                                    else -> false
                                }
                            }
                        }
                    }

                    setOnGenericMotionListener { _, event ->
                        if (isHardwareMouseEvent(event) || event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
                            syncMouseButtons(event.buttonState)
                            when (event.actionMasked) {
                                MotionEvent.ACTION_HOVER_MOVE -> {
                                    val pts = floatArrayOf(event.x, event.y)
                                    inputMatrix.mapPoints(pts)
                                    sendMouseEvent(pts[0], pts[1], InputStub.BUTTON_UNDEFINED, false, false)
                                    true
                                }
                                MotionEvent.ACTION_SCROLL -> {
                                    val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                                    val hScroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
                                    val factor = if (invertScroll) -100f else 100f
                                    sendMouseWheelEvent(hScroll * 100f, -vScroll * factor)
                                    true
                                }
                                MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE -> {
                                    syncMouseButtons(event.buttonState)
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }

                    setOnKeyListener { _, keyCode, event ->
                        val isDown = event.action == KeyEvent.ACTION_DOWN
                        sendKeyEvent(event.scanCode, keyCode, isDown)
                    }

                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                }
            },
            update = { view ->
                view.sendSurface()
                view.triggerCallback()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading / Booting State Overlay (prevents long black screen on initial startup)
        AnimatedVisibility(
            visible = sessionState is SessionState.Starting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.95f)),
                    border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = OrangePrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Starting Desktop Session",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val message = (sessionState as? SessionState.Starting)?.step ?: "Initializing LXQt environment..."
                        Text(
                            text = message,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Dynamic responsive sizing for toolbar buttons
        val buttonSize = if (isDeviceLandscape) 42.dp else 36.dp
        val iconSize = if (isDeviceLandscape) 22.dp else 20.dp
        val buttonSpacing = if (isDeviceLandscape) 6.dp else 4.dp
        val cardHorizontalPadding = if (isDeviceLandscape) 12.dp else 8.dp
        val cardVerticalPadding = if (isDeviceLandscape) 6.dp else 4.dp

        // Floating Fluent Toolbar on Top (Identical items in Portrait & Landscape, with scroll if needed)
        AnimatedVisibility(
            visible = showToolbar,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.94f)),
                border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.25f)),
                modifier = Modifier.widthIn(max = screenWidth - 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = cardHorizontalPadding, vertical = cardVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    // 1. Soft Keyboard Toggle (Orange)
                    ToolbarIconButton(
                        icon = Icons.Default.Keyboard,
                        contentDescription = "Keyboard",
                        tint = OrangePrimary,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = { currentLorieView?.toggleKeyboardVisible() }
                    )

                    // 2. Screen Orientation Toggle (Landscape / Portrait)
                    ToolbarIconButton(
                        icon = Icons.Default.ScreenRotation,
                        contentDescription = "Rotate Screen",
                        tint = if (isDeviceLandscape) OrangePrimary else Color.White,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = { isLandscape = !isLandscape }
                    )

                    // 2b. Force Redraw / Refresh Display (prevents blank screen)
                    ToolbarIconButton(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Refresh Display",
                        tint = OrangePrimary,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = {
                            currentLorieView?.let { v ->
                                v.sendSurface()
                                v.triggerCallback()
                            }
                        }
                    )

                    // 3. X11 Mouse Pad Toggle (Opens/Closes Floating Onscreen Mouse Pad)
                    ToolbarIconButton(
                        icon = Icons.Default.TouchApp,
                        contentDescription = "X11 Mouse Pad",
                        tint = if (showMousePad) OrangePrimary else Color.White,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = { showMousePad = !showMousePad }
                    )

                    // 4. Mouse Mode & Sensitivity Settings
                    ToolbarIconButton(
                        icon = Icons.Default.Mouse,
                        contentDescription = "Mouse Settings",
                        tint = Color.White,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = { showMouseModeDialog = true }
                    )

                    // 5. Resolution Selector
                    ToolbarIconButton(
                        icon = Icons.Default.AspectRatio,
                        contentDescription = "Resolution",
                        tint = Color.White,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = { showResolutionDialog = true }
                    )

                    // 6. Fullscreen Toggle
                    ToolbarIconButton(
                        icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = { isFullscreen = !isFullscreen }
                    )

                    // 7. Back to App Dashboard (Green)
                    ToolbarIconButton(
                        icon = Icons.Default.Dashboard,
                        contentDescription = "Dashboard",
                        tint = StatusRunning,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = onNavigateBack
                    )

                    // 8. Stop Session (Red)
                    ToolbarIconButton(
                        icon = Icons.Default.PowerSettingsNew,
                        contentDescription = "Stop Session",
                        tint = CrimsonRed,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = { showStopConfirmDialog = true }
                    )

                    // 9. Minimize / Collapse Toolbar
                    ToolbarIconButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Minimize Toolbar",
                        tint = Color.LightGray,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        onClick = { showToolbar = false }
                    )
                }
            }
        }

        // Sleek Minimal Expand Handle when Toolbar is minimized
        if (!showToolbar) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .background(Slate900.copy(alpha = 0.88f), RoundedCornerShape(18.dp))
                    .border(1.dp, CardBorder.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 30.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showToolbar = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Toolbar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // =========================================================================
        // Floating Draggable X11 Mouse Pad Overlay
        // =========================================================================
        AnimatedVisibility(
            visible = showMousePad,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .offset { IntOffset(padOffsetX.roundToInt(), padOffsetY.roundToInt()) }
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.94f)),
                border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.4f)),
                modifier = Modifier.width(if (isPadMinimized) 180.dp else 250.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header: Drag Handle & Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    padOffsetX += dragAmount.x
                                    padOffsetY += dragAmount.y
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag Pad",
                                tint = OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "X11 Mouse Pad",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Minimize / Expand
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .clickable { isPadMinimized = !isPadMinimized },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPadMinimized) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle Pad Size",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            // Close pad
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .clickable { showMousePad = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Pad",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (!isPadMinimized) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Physical Mouse Click Buttons Row: [ Left ] [ Drag Lock ] [ Middle ] [ Right ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Left Click (L)
                            MouseActionButton(
                                label = "L",
                                subtitle = "Left",
                                isHighlight = false,
                                modifier = Modifier.weight(1f),
                                onClick = { sendClick(InputStub.BUTTON_LEFT, relative = true) }
                            )

                            // Drag Lock (Holds Left Button down for window move & selection)
                            MouseActionButton(
                                label = if (isDragLocked) "LOCKED" else "LOCK",
                                subtitle = "Drag",
                                isHighlight = isDragLocked,
                                modifier = Modifier.weight(1.1f),
                                onClick = { toggleDragLock() }
                            )

                            // Middle Click (M) - Crucial for Linux terminal paste & tab opening
                            MouseActionButton(
                                label = "M",
                                subtitle = "Mid",
                                isHighlight = false,
                                modifier = Modifier.weight(1f),
                                onClick = { sendClick(InputStub.BUTTON_MIDDLE, relative = true) }
                            )

                            // Right Click (R) - Opens context menu
                            MouseActionButton(
                                label = "R",
                                subtitle = "Right",
                                isHighlight = false,
                                modifier = Modifier.weight(1f),
                                onClick = { sendClick(InputStub.BUTTON_RIGHT, relative = true) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dedicated Touchpad Area with Scroll Bar on right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Touchpad Surface
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(Color(0xFF131722), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardBorder.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .pointerInput(pointerSensitivity, isDragLocked) {
                                        var touchStartX = 0f
                                        var touchStartY = 0f
                                        var lastX = 0f
                                        var lastY = 0f
                                        var padDownTime = 0L

                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                touchStartX = offset.x
                                                touchStartY = offset.y
                                                lastX = offset.x
                                                lastY = offset.y
                                                padDownTime = System.currentTimeMillis()
                                            },
                                            onDragEnd = {
                                                val dist = Math.hypot((lastX - touchStartX).toDouble(), (lastY - touchStartY).toDouble())
                                                val dur = System.currentTimeMillis() - padDownTime
                                                if (dist < 15 && dur < 250) {
                                                    sendClick(InputStub.BUTTON_LEFT, relative = true)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val dx = dragAmount.x * pointerSensitivity
                                                val dy = dragAmount.y * pointerSensitivity
                                                currentLorieView?.sendMouseEvent(
                                                    dx, dy,
                                                    InputStub.BUTTON_UNDEFINED,
                                                    false, true
                                                )
                                                lastX += dragAmount.x
                                                lastY += dragAmount.y
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PanTool,
                                        contentDescription = null,
                                        tint = Color.Gray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Touchpad Surface",
                                        color = Color.Gray.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Glide to move • Tap to click",
                                        color = Color.Gray.copy(alpha = 0.4f),
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            // Scroll Wheel Column (Up / Down)
                            Column(
                                modifier = Modifier
                                    .width(42.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Scroll Up
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Slate950, RoundedCornerShape(8.dp))
                                        .border(1.dp, CardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            currentLorieView?.sendMouseWheelEvent(0f, 150f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropUp,
                                        contentDescription = "Scroll Up",
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Scroll Down
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Slate950, RoundedCornerShape(8.dp))
                                        .border(1.dp, CardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            currentLorieView?.sendMouseWheelEvent(0f, -150f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Scroll Down",
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Minimized quick buttons row
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MouseActionButton(
                                label = "L",
                                subtitle = null,
                                isHighlight = isDragLocked,
                                modifier = Modifier.weight(1f),
                                onClick = { sendClick(InputStub.BUTTON_LEFT, relative = true) }
                            )
                            MouseActionButton(
                                label = "R",
                                subtitle = null,
                                isHighlight = false,
                                modifier = Modifier.weight(1f),
                                onClick = { sendClick(InputStub.BUTTON_RIGHT, relative = true) }
                            )
                        }
                    }
                }
            }
        }

        // Resolution Dialog
        if (showResolutionDialog) {
            AlertDialog(
                onDismissRequest = { showResolutionDialog = false },
                containerColor = Slate900,
                title = { Text("Display Resolution", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        resolutions.forEach { res ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedResolution == res,
                                    onClick = { selectedResolution = res },
                                    colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(res.name, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showResolutionDialog = false
                        currentLorieView?.setCustomResolution(selectedResolution.width, selectedResolution.height)
                    }) {
                        Text("Apply", color = OrangePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Mouse Mode & Configuration Dialog
        if (showMouseModeDialog) {
            AlertDialog(
                onDismissRequest = { showMouseModeDialog = false },
                containerColor = Slate900,
                title = { Text("Mouse & Trackpad Configuration", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Interaction Mode",
                            color = OrangePrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        MouseMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mouseMode = mode }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = mouseMode == mode,
                                    onClick = { mouseMode = mode },
                                    colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(mode.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(mode.description, color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = CardBorder.copy(alpha = 0.3f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Toggle X11 Mouse Pad Overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show X11 Mouse Pad",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "On-screen touchpad with Left, Mid, Right buttons & drag lock",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = showMousePad,
                                onCheckedChange = { showMousePad = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OrangePrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pointer Sensitivity
                        Text(
                            text = "Pointer Speed",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.8f to "Slow", 1.2f to "Normal", 1.8f to "Fast").forEach { (speed, label) ->
                                val selected = pointerSensitivity == speed
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) OrangePrimary else Slate950)
                                        .border(1.dp, if (selected) OrangePrimary else CardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { pointerSensitivity = speed }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Invert Scroll Direction
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Invert Scroll Direction",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Reverse mouse wheel & two-finger scroll",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = invertScroll,
                                onCheckedChange = { invertScroll = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OrangePrimary
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMouseModeDialog = false }) {
                        Text("Done", color = OrangePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Stop Session Confirmation Dialog
        if (showStopConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showStopConfirmDialog = false },
                containerColor = Slate900,
                title = { Text("Stop Desktop Session?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to stop the running LXQt desktop environment?", color = Color.LightGray) },
                confirmButton = {
                    TextButton(onClick = {
                        showStopConfirmDialog = false
                        LinuxSessionService.stopService(context)
                        onNavigateBack()
                    }) {
                        Text("Stop Session", color = CrimsonRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopConfirmDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
private fun MouseActionButton(
    label: String,
    subtitle: String?,
    isHighlight: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighlight) OrangePrimary else Slate950)
            .border(
                1.dp,
                if (isHighlight) OrangePrimary else CardBorder.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (isHighlight) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = if (isHighlight) Color.Black.copy(alpha = 0.8f) else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    buttonSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
