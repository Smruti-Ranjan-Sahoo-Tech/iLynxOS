package com.termux.ilynxos.runtime

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import com.termux.x11.CmdEntryPoint
import com.termux.x11.LorieView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class X11DisplayEngine(private val context: Context) {

    companion object {
        private const val TAG = "X11DisplayEngine"

        @Volatile
        private var instance: X11DisplayEngine? = null

        fun getInstance(context: Context): X11DisplayEngine {
            return instance ?: synchronized(this) {
                instance ?: X11DisplayEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val envManager = LinuxEnvironmentManager.getInstance(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cmdEntryPoint: CmdEntryPoint? = null
    private var lorieViewInstance: LorieView? = null
    private var xConnectionFd: ParcelFileDescriptor? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _isClientConnected = MutableStateFlow(false)
    val isClientConnected: StateFlow<Boolean> = _isClientConnected.asStateFlow()

    init {
        try {
            System.loadLibrary("Xlorie")
            Log.i(TAG, "libXlorie.so loaded successfully into app process.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libXlorie.so", e)
        }
    }

    /**
     * Starts the embedded X11 server thread in our app process.
     * Guaranteed to run on the MainLooper thread so native AChoreographer finds a valid Looper.
     */
    fun startServer(): Boolean {
        if (_isServerRunning.value || cmdEntryPoint != null) {
            Log.i(TAG, "X11 server is already running.")
            return true
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            var result = false
            val latch = java.util.concurrent.CountDownLatch(1)
            mainHandler.post {
                try {
                    result = startServerInternal()
                } finally {
                    latch.countDown()
                }
            }
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
            return result
        }

        return startServerInternal()
    }

    @Synchronized
    private fun startServerInternal(): Boolean {
        if (_isServerRunning.value || cmdEntryPoint != null) {
            return true
        }

        try {
            // Point TMPDIR to our application private tmp dir where .X11-unix socket lives
            val tmpDir = envManager.tmpDir.absolutePath
            val x11Unix = File(envManager.tmpDir, ".X11-unix")
            x11Unix.mkdirs()
            val shmDir = File(envManager.tmpDir, "shm")
            shmDir.mkdirs()

            android.system.Os.setenv("TMPDIR", tmpDir, true)
            android.system.Os.setenv("XDG_RUNTIME_DIR", "$tmpDir/runtime-root", true)

            val xkbDir = File(envManager.rootfsDir, "usr/share/X11/xkb").absolutePath
            if (File(xkbDir).exists()) {
                android.system.Os.setenv("XKB_CONFIG_ROOT", xkbDir, true)
                Log.i(TAG, "Set XKB_CONFIG_ROOT to $xkbDir")
            } else {
                Log.w(TAG, "XKB config directory not found at $xkbDir")
            }

            // Start X server with display :0, access control disabled (-ac), and legacy software drawing
            val args = if (File(xkbDir).exists()) {
                arrayOf(":0", "-ac", "-legacy-drawing", "-xkbdir", xkbDir)
            } else {
                arrayOf(":0", "-ac", "-legacy-drawing")
            }

            cmdEntryPoint = CmdEntryPoint(args)
            _isServerRunning.value = true
            Log.i(TAG, "X11 server started on display :0 (TMPDIR=$tmpDir)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start X11 server", e)
            _isServerRunning.value = false
            return false
        }
    }

    /**
     * Connects LorieView to the running embedded X server
     */
    fun attachLorieView(view: LorieView) {
        lorieViewInstance = view
        if (!_isServerRunning.value) {
            startServer()
        }

        scheduleConnect(view, 35)
    }

    /**
     * Reuses an existing connected LorieView across screen navigation
     * to eliminate reconnection delays, black screen delays, and socket detachment issues.
     */
    fun getOrCreateLorieView(ctx: Context): LorieView {
        val existing = lorieViewInstance
        if (existing != null && existing.connected()) {
            (existing.parent as? android.view.ViewGroup)?.removeView(existing)
            return existing
        }
        (existing?.parent as? android.view.ViewGroup)?.removeView(existing)
        val newView = LorieView(ctx)
        lorieViewInstance = newView
        return newView
    }

    private fun scheduleConnect(view: LorieView, attemptsLeft: Int) {
        if (view.connected()) {
            _isClientConnected.value = true
            Log.i(TAG, "LorieView already connected to X11.")
            view.sendSurface()
            view.triggerCallback()
            return
        }

        try {
            val fd = cmdEntryPoint?.xConnection ?: CmdEntryPoint.instance?.xConnection
            if (fd != null && fd.fd != -1) {
                view.connect(fd.detachFd())
                xConnectionFd = null
                view.sendSurface()
                view.triggerCallback()
                _isClientConnected.value = true
                Log.i(TAG, "LorieView connected to X11 socket successfully.")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "LorieView connection attempt failed: ${e.message}")
        }

        if (attemptsLeft > 0) {
            mainHandler.postDelayed({
                scheduleConnect(view, attemptsLeft - 1)
            }, 200)
        } else {
            Log.e(TAG, "Failed to connect LorieView after maximum retries.")
        }
    }

    fun getLorieView(): LorieView? = lorieViewInstance

    fun stopServer() {
        Log.i(TAG, "Stopping X11 Display Server...")
        try {
            xConnectionFd?.close()
            xConnectionFd = null
            cmdEntryPoint = null
            lorieViewInstance = null
            _isServerRunning.value = false
            _isClientConnected.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping X11 server", e)
        }
    }
}
