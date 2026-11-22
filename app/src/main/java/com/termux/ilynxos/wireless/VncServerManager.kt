package com.termux.ilynxos.wireless

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.termux.ilynxos.runtime.LinuxEnvironmentManager
import com.termux.ilynxos.runtime.PRootRuntimeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

class VncServerManager(private val context: Context) {

    companion object {
        private const val TAG = "VncServerManager"
        private const val PREFS_NAME = "ilynxos_wireless_vnc"
        private const val KEY_PORT = "vnc_port"
        private const val KEY_PASSWORD = "vnc_password"
        private const val KEY_ENABLED = "vnc_enabled"
        private const val KEY_SESSION_MODE = "vnc_session_mode"
        const val DEFAULT_PORT = 5901
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val runtimeEngine = PRootRuntimeEngine.getInstance(context)
    private val envManager = LinuxEnvironmentManager.getInstance(context)
    private val networkDiscovery = NetworkDiscovery.getInstance(context)

    private val _state = MutableStateFlow(
        ServiceState(
            status = ServiceStatus.STOPPED,
            port = getPort(),
            address = null
        )
    )
    val state: StateFlow<ServiceState> = _state.asStateFlow()

    init {
        val pass = prefs.getString(KEY_PASSWORD, "") ?: ""
        if (pass.isEmpty() || pass.startsWith("ilynx") || pass == "ilinux1719") {
            setPassword("raja1234")
        }
    }

    fun getPort(): Int {
        val port = prefs.getInt(KEY_PORT, 0)
        if (port != 0 && port != 5900 && port != 5901) return port
        return if (isNewSession()) 5901 else 5900
    }
    fun setPort(port: Int) = prefs.edit().putInt(KEY_PORT, port).apply()

    fun getPassword(): String {
        val pass = prefs.getString(KEY_PASSWORD, "") ?: ""
        if (pass.isEmpty() || pass.startsWith("ilynx") || pass == "ilinux1719") {
            setPassword("raja1234")
            return "raja1234"
        }
        return pass
    }
    fun setPassword(pass: String) = prefs.edit().putString(KEY_PASSWORD, pass).apply()

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)
    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun isNewSession(): Boolean = prefs.getBoolean(KEY_SESSION_MODE, true)
    fun setNewSession(newSession: Boolean) {
        prefs.edit().putBoolean(KEY_SESSION_MODE, newSession).apply()
        val currentPort = prefs.getInt(KEY_PORT, 0)
        if (currentPort == 0 || currentPort == 5900 || currentPort == 5901) {
            val targetPort = if (newSession) 5901 else 5900
            setPort(targetPort)
            _state.value = _state.value.copy(port = targetPort)
        }
    }

    private fun checkSocketListening(port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), 1000)
                Log.d(TAG, "checkSocketListening($port): OPEN")
                true
            }
        } catch (e: Exception) {
            Log.d(TAG, "checkSocketListening($port): CLOSED (${e.message})")
            false
        }
    }

    suspend fun start(
        port: Int = getPort(),
        password: String = getPassword(),
        newSession: Boolean = isNewSession()
    ): Boolean = withContext(Dispatchers.IO) {
        if (!envManager.isInstalled()) {
            _state.value = _state.value.copy(
                status = ServiceStatus.NOT_INSTALLED,
                error = "iLynxOS container not installed"
            )
            return@withContext false
        }

        val safePass = if (password.isEmpty() || password.startsWith("ilynx") || password == "ilinux1719") "raja1234" else password
        val effectivePort = if (newSession && port == 5900) 5901 else if (!newSession && port == 5901) 5900 else port
        setPort(effectivePort)
        setPassword(safePass)
        setNewSession(newSession)

        _state.value = _state.value.copy(status = ServiceStatus.STARTING, error = null)

        val dispNum = if (newSession) "1" else "0"

        // 1. Ensure desktop session and X11 server are active only for display :0 (Mirror Mode)
        if (dispNum == "0" && !runtimeEngine.isSessionRunning()) {
            Log.i(TAG, "Desktop session not running; starting LinuxSessionService for display :0 mirror...")
            try {
                com.termux.ilynxos.runtime.LinuxSessionService.startService(context)
                val socketFile = File(envManager.tmpDir, ".X11-unix/X0")
                for (i in 1..40) {
                    if (socketFile.exists() && runtimeEngine.isSessionRunning()) {
                        Log.i(TAG, "Session and X11 socket verified ready for VNC")
                        break
                    }
                    kotlinx.coroutines.delay(100)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed starting desktop session: ${e.message}")
            }
        }

        // 2. Ensure wireless scripts unpacked
        envManager.unpackWirelessDesktopScripts()

        // 3. Write request for supervisor loop (if desktop session is running)
        try {
            val reqFile = File(envManager.tmpDir, "vnc_start.req")
            reqFile.writeText("$effectivePort\n$safePass\n$dispNum\n1280x720\n")
        } catch (_: Exception) {}

        // 3b. Trigger startup via CLI execution
        try {
            runtimeEngine.executeCliCommand("bash /usr/local/share/ilynxos/wireless-desktop/start-vnc.sh $effectivePort '$safePass' $dispNum 1280x720") { line ->
                Log.d(TAG, "[VNC Output] $line")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing start-vnc.sh: ${e.message}")
        }

        // 4. Poll for socket availability
        var socketOpen = false
        for (i in 1..100) {
            kotlinx.coroutines.delay(250)
            if (checkSocketListening(effectivePort)) {
                Log.i(TAG, "VNC server detected listening on port $effectivePort (attempt $i)")
                socketOpen = true
                break
            }
        }

        val ip = networkDiscovery.getFreshNetworkInfo().ipAddress
        val address = if (ip != null) "$ip:$effectivePort" else "localhost:$effectivePort"

        _state.value = ServiceState(
            status = if (socketOpen) ServiceStatus.RUNNING else ServiceStatus.STOPPED,
            port = effectivePort,
            address = if (socketOpen) address else null,
            pid = null,
            error = if (!socketOpen) "Failed to bind VNC port $effectivePort" else null
        )

        return@withContext socketOpen
    }

    suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        try {
            val stopFile = File(envManager.tmpDir, "vnc_stop.req")
            stopFile.writeText("stop\n")
        } catch (_: Exception) {}

        try {
            runtimeEngine.executeCliCommand("bash /usr/local/share/ilynxos/wireless-desktop/stop-vnc.sh") { line ->
                Log.d(TAG, "[VNC Stop] $line")
            }
        } catch (_: Exception) {}

        for (i in 1..10) {
            kotlinx.coroutines.delay(300)
            if (!checkSocketListening(getPort())) {
                break
            }
        }
        _state.value = ServiceState(status = ServiceStatus.STOPPED, port = getPort(), address = null)
        return@withContext true
    }

    suspend fun restart(port: Int = getPort(), password: String = getPassword(), newSession: Boolean = isNewSession()): Boolean {
        stop()
        kotlinx.coroutines.delay(500)
        return start(port, password, newSession)
    }

    suspend fun checkStatus(): Boolean = withContext(Dispatchers.IO) {
        val port = getPort()
        val isRunning = checkSocketListening(port)

        if (_state.value.status == ServiceStatus.STARTING && !isRunning) {
            return@withContext false
        }

        val ip = networkDiscovery.getFreshNetworkInfo().ipAddress
        val address = if (ip != null) "$ip:$port" else "localhost:$port"

        _state.value = ServiceState(
            status = if (isRunning) ServiceStatus.RUNNING else ServiceStatus.STOPPED,
            port = port,
            address = if (isRunning) address else null,
            pid = null,
            error = null
        )

        return@withContext isRunning
    }
}
