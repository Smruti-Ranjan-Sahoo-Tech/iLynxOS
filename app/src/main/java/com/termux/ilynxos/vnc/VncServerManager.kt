package com.termux.ilynxos.vnc

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.termux.ilynxos.runtime.LinuxEnvironmentManager
import com.termux.ilynxos.runtime.LinuxSessionService
import com.termux.ilynxos.runtime.PRootRuntimeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

class VncServerManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "VncServerManager"
        private const val PREFS_NAME = "ilynxos_vnc_settings"
        private const val KEY_PORT = "vnc_port"
        private const val KEY_PASSWORD = "vnc_password"
        private const val KEY_SESSION_MODE = "vnc_session_mode"
        private const val KEY_RESOLUTION = "vnc_resolution"

        const val DEFAULT_VIRTUAL_PORT = 5901
        const val DEFAULT_MIRROR_PORT = 5900
        const val DEFAULT_PASSWORD = "raja1234"
        const val DEFAULT_RESOLUTION = "1280x720"

        @Volatile
        private var instance: VncServerManager? = null

        fun getInstance(context: Context): VncServerManager {
            return instance ?: synchronized(this) {
                instance ?: VncServerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val runtimeEngine = PRootRuntimeEngine.getInstance(context)
    private val envManager = LinuxEnvironmentManager.getInstance(context)
    private val networkDiscovery = NetworkDiscovery.getInstance(context)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _state = MutableStateFlow(
        VncServerState(
            status = VncStatus.STOPPED,
            port = getPort(),
            display = if (isNewSession()) 1 else 0,
            mode = if (isNewSession()) VncSessionMode.VIRTUAL_DISPLAY else VncSessionMode.MIRROR_DISPLAY,
            resolution = getResolution()
        )
    )
    val state: StateFlow<VncServerState> = _state.asStateFlow()

    init {
        val pass = prefs.getString(KEY_PASSWORD, "") ?: ""
        if (pass.isEmpty() || pass.startsWith("ilynx") || pass == "ilinux1719") {
            setPassword(DEFAULT_PASSWORD)
        }
    }

    fun addLog(message: String) {
        val current = _logs.value.toMutableList()
        current.add(message)
        if (current.size > 200) current.removeAt(0)
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun getPort(): Int {
        val port = prefs.getInt(KEY_PORT, 0)
        if (port != 0 && port != DEFAULT_MIRROR_PORT && port != DEFAULT_VIRTUAL_PORT) return port
        return if (isNewSession()) DEFAULT_VIRTUAL_PORT else DEFAULT_MIRROR_PORT
    }

    fun setPort(port: Int) {
        prefs.edit().putInt(KEY_PORT, port).apply()
        _state.value = _state.value.copy(port = port)
    }

    fun getPassword(): String {
        val pass = prefs.getString(KEY_PASSWORD, "") ?: ""
        if (pass.isEmpty() || pass.startsWith("ilynx") || pass == "ilinux1719") {
            setPassword(DEFAULT_PASSWORD)
            return DEFAULT_PASSWORD
        }
        return pass
    }

    fun setPassword(pass: String) {
        prefs.edit().putString(KEY_PASSWORD, pass).apply()
    }

    fun isNewSession(): Boolean = prefs.getBoolean(KEY_SESSION_MODE, true)

    fun setNewSession(newSession: Boolean) {
        prefs.edit().putBoolean(KEY_SESSION_MODE, newSession).apply()
        val targetPort = if (newSession) DEFAULT_VIRTUAL_PORT else DEFAULT_MIRROR_PORT
        setPort(targetPort)
        _state.value = _state.value.copy(
            mode = if (newSession) VncSessionMode.VIRTUAL_DISPLAY else VncSessionMode.MIRROR_DISPLAY,
            display = if (newSession) 1 else 0,
            port = targetPort
        )
    }

    fun getResolution(): String {
        return prefs.getString(KEY_RESOLUTION, DEFAULT_RESOLUTION) ?: DEFAULT_RESOLUTION
    }

    fun setResolution(res: String) {
        prefs.edit().putString(KEY_RESOLUTION, res).apply()
        _state.value = _state.value.copy(resolution = res)
    }

    private fun checkSocketListening(port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), 600)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Start the VNC Server based on vncstart analysis
     */
    suspend fun start(
        port: Int = getPort(),
        password: String = getPassword(),
        newSession: Boolean = isNewSession(),
        resolution: String = getResolution()
    ): Boolean = withContext(Dispatchers.IO) {
        if (!envManager.isInstalled()) {
            _state.value = _state.value.copy(
                status = VncStatus.NOT_INSTALLED,
                error = "iLynxOS container is not installed"
            )
            return@withContext false
        }

        val safePass = if (password.isEmpty() || password.startsWith("ilynx") || password == "ilinux1719") DEFAULT_PASSWORD else password
        val effectivePort = if (newSession && port == DEFAULT_MIRROR_PORT) DEFAULT_VIRTUAL_PORT else if (!newSession && port == DEFAULT_VIRTUAL_PORT) DEFAULT_MIRROR_PORT else port
        val dispNum = if (newSession) 1 else 0

        setPort(effectivePort)
        setPassword(safePass)
        setNewSession(newSession)
        setResolution(resolution)

        _state.value = _state.value.copy(
            status = VncStatus.STARTING,
            port = effectivePort,
            display = dispNum,
            mode = if (newSession) VncSessionMode.VIRTUAL_DISPLAY else VncSessionMode.MIRROR_DISPLAY,
            error = null
        )

        addLog("▶ Starting i-Lynx OS VNC Server on port $effectivePort (Display :$dispNum, Res: $resolution)...")

        // 1. If mirror mode (display :0), ensure local desktop session is running
        if (dispNum == 0 && !runtimeEngine.isSessionRunning()) {
            addLog("Display :0 mirror mode requires on-screen session; launching LinuxSessionService...")
            try {
                LinuxSessionService.startService(context)
                val socketFile = File(envManager.tmpDir, ".X11-unix/X0")
                for (i in 1..40) {
                    if (socketFile.exists() && runtimeEngine.isSessionRunning()) {
                        addLog("Local X11 display :0 socket verified ready.")
                        break
                    }
                    kotlinx.coroutines.delay(100)
                }
            } catch (e: Exception) {
                addLog("Warning: Could not start local session: ${e.message}")
            }
        }

        // 2. Ensure vnc scripts are unpacked into the rootfs
        envManager.unpackVncScripts()

        // 3. Write request for supervisor loop (if background supervisor is running)
        try {
            val reqFile = File(envManager.tmpDir, "vnc_start.req")
            reqFile.writeText("$effectivePort\n$safePass\n$dispNum\n$resolution\n")
        } catch (_: Exception) {}

        // 4. Trigger startup via direct CLI execution (vncstart)
        try {
            val scriptCmd = "bash /usr/local/share/ilynxos/vnc/vncstart.sh $effectivePort '$safePass' $dispNum $resolution"
            runtimeEngine.executeCliCommand(scriptCmd) { line ->
                addLog(line)
                Log.d(TAG, "[vncstart] $line")
            }
        } catch (e: Exception) {
            addLog("Execution warning: ${e.message}")
            Log.e(TAG, "Failed executing vncstart: ${e.message}")
        }

        // 5. Poll for TCP port availability on 127.0.0.1
        var socketOpen = false
        for (i in 1..40) {
            kotlinx.coroutines.delay(250)
            if (checkSocketListening(effectivePort)) {
                socketOpen = true
                addLog("✅ VNC Server successfully listening on port $effectivePort!")
                break
            }
        }

        val ip = networkDiscovery.getFreshNetworkInfo().ipAddress
        val address = if (ip != null) "$ip:$effectivePort" else "localhost:$effectivePort"

        _state.value = _state.value.copy(
            status = if (socketOpen) VncStatus.RUNNING else VncStatus.STOPPED,
            port = effectivePort,
            display = dispNum,
            address = if (socketOpen) address else null,
            error = if (!socketOpen) "Failed to bind VNC port $effectivePort. Check logs." else null
        )

        return@withContext socketOpen
    }

    /**
     * Stop the VNC Server based on vncstop analysis
     */
    suspend fun stop(display: Int? = null): Boolean = withContext(Dispatchers.IO) {
        val targetDisp = display ?: _state.value.display
        addLog("⏹ Stopping VNC Server on display :$targetDisp...")

        try {
            val stopFile = File(envManager.tmpDir, "vnc_stop.req")
            stopFile.writeText("stop\n")
        } catch (_: Exception) {}

        try {
            val cmd = "bash /usr/local/share/ilynxos/vnc/vncstop.sh :$targetDisp"
            runtimeEngine.executeCliCommand(cmd) { line ->
                addLog(line)
            }
        } catch (e: Exception) {
            addLog("Stop notice: ${e.message}")
        }

        // Wait until port closes
        val port = getPort()
        for (i in 1..15) {
            kotlinx.coroutines.delay(200)
            if (!checkSocketListening(port)) {
                break
            }
        }

        addLog("VNC Server stopped.")
        _state.value = _state.value.copy(
            status = VncStatus.STOPPED,
            address = null,
            error = null
        )

        return@withContext true
    }

    suspend fun restart(
        port: Int = getPort(),
        password: String = getPassword(),
        newSession: Boolean = isNewSession(),
        resolution: String = getResolution()
    ): Boolean {
        stop()
        kotlinx.coroutines.delay(400)
        return start(port, password, newSession, resolution)
    }

    suspend fun checkStatus(): Boolean = withContext(Dispatchers.IO) {
        val port = getPort()
        val isRunning = checkSocketListening(port)

        if (_state.value.status == VncStatus.STARTING && !isRunning) {
            return@withContext false
        }

        val ip = networkDiscovery.getFreshNetworkInfo().ipAddress
        val address = if (ip != null) "$ip:$port" else "localhost:$port"

        _state.value = _state.value.copy(
            status = if (isRunning) VncStatus.RUNNING else VncStatus.STOPPED,
            port = port,
            address = if (isRunning) address else null,
            error = null
        )

        return@withContext isRunning
    }
}
