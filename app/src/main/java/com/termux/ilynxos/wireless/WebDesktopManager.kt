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

class WebDesktopManager(private val context: Context) {

    companion object {
        private const val TAG = "WebDesktopManager"
        private const val PREFS_NAME = "ilynxos_wireless_web"
        private const val KEY_PORT = "web_port"
        private const val KEY_PASSWORD = "web_password"
        private const val KEY_ENABLED = "web_enabled"
        private const val KEY_USE_TLS = "web_use_tls"
        private const val KEY_SESSION_MODE = "web_session_mode"
        const val DEFAULT_PORT = 14500
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

    private val _isXpraInstalled = MutableStateFlow(false)
    val isXpraInstalled: StateFlow<Boolean> = _isXpraInstalled.asStateFlow()

    init {
        if (getPassword().isEmpty()) {
            setPassword("raja1234")
        }
    }

    fun getPort(): Int = prefs.getInt(KEY_PORT, DEFAULT_PORT)
    fun setPort(port: Int) = prefs.edit().putInt(KEY_PORT, port).apply()

    fun getPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""
    fun setPassword(pass: String) = prefs.edit().putString(KEY_PASSWORD, pass).apply()

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)
    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun isTlsEnabled(): Boolean = prefs.getBoolean(KEY_USE_TLS, false)
    fun setTlsEnabled(tls: Boolean) = prefs.edit().putBoolean(KEY_USE_TLS, tls).apply()

    fun isNewSession(): Boolean = prefs.getBoolean(KEY_SESSION_MODE, true)
    fun setNewSession(newSession: Boolean) = prefs.edit().putBoolean(KEY_SESSION_MODE, newSession).apply()

    suspend fun checkXpraInstalled(): Boolean = withContext(Dispatchers.IO) {
        val rootfsDir = envManager.rootfsDir
        val novncHtml = File(rootfsDir, "usr/local/share/ilynxos/wireless-desktop/novnc/vnc.html")
        val ready = novncHtml.exists() || envManager.isInstalled()
        _isXpraInstalled.value = ready
        ready
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
        useTls: Boolean = isTlsEnabled(),
        newSession: Boolean = isNewSession()
    ): Boolean = withContext(Dispatchers.IO) {
        if (!envManager.isInstalled()) {
            _state.value = _state.value.copy(
                status = ServiceStatus.NOT_INSTALLED,
                error = "iLynxOS container not installed"
            )
            return@withContext false
        }

        setPort(port)
        if (password.isNotEmpty()) setPassword(password)
        setTlsEnabled(useTls)
        setNewSession(newSession)

        _state.value = _state.value.copy(status = ServiceStatus.STARTING, error = null)

        val dispNum = if (newSession) "1" else "0"

        // Ensure desktop session and X11 server are active
        if (dispNum == "0" && !runtimeEngine.isSessionRunning()) {
            Log.i(TAG, "Desktop session not running; starting LinuxSessionService...")
            try {
                com.termux.ilynxos.runtime.LinuxSessionService.startService(context)
                val socketFile = File(envManager.tmpDir, ".X11-unix/X0")
                for (i in 1..30) {
                    if (socketFile.exists() && runtimeEngine.isSessionRunning()) {
                        Log.i(TAG, "Session and X11 socket verified ready for Web Mode")
                        break
                    }
                    kotlinx.coroutines.delay(100)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed starting desktop session: ${e.message}")
            }
        } else if (!runtimeEngine.isSessionRunning()) {
            try {
                com.termux.ilynxos.runtime.LinuxSessionService.startService(context)
            } catch (_: Exception) {}
        }

        // Ensure wireless desktop scripts and noVNC/websockify unpacked
        envManager.unpackWirelessDesktopScripts()

        // Write IPC request
        val tlsFlag = if (useTls) "1" else "0"
        try {
            val reqFile = File(envManager.tmpDir, "web_start.req")
            reqFile.writeText("$port\n$password\n$tlsFlag\n$dispNum\n")
            Log.i(TAG, "Written web_start.req (port: $port, tls: $tlsFlag, disp: $dispNum)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing web_start.req: ${e.message}")
        }

        // Trigger startup immediately to eliminate delay
        try {
            runtimeEngine.executeCliCommand("/usr/local/share/ilynxos/wireless-desktop/start-web.sh $port '$password' $tlsFlag $dispNum 1280x720 &") {}
        } catch (_: Exception) {}

        // Poll for socket availability
        var socketOpen = false
        for (i in 1..25) {
            kotlinx.coroutines.delay(150)
            if (checkSocketListening(port)) {
                Log.i(TAG, "Web Mode detected listening on port $port (attempt $i)")
                socketOpen = true
                break
            }
        }

        val ip = networkDiscovery.getFreshNetworkInfo().ipAddress
        val proto = if (useTls) "https" else "http"
        val encPass = if (password.isNotEmpty()) "&password=" + java.net.URLEncoder.encode(password, "UTF-8") else ""
        val address = if (ip != null) "$proto://$ip:$port/vnc.html?autoconnect=true&resize=scale&quality=6&compression=1$encPass" else "$proto://localhost:$port/vnc.html?autoconnect=true&resize=scale&quality=6&compression=1$encPass"

        _state.value = ServiceState(
            status = if (socketOpen) ServiceStatus.RUNNING else ServiceStatus.STOPPED,
            port = port,
            address = if (socketOpen) address else null,
            pid = null,
            error = if (!socketOpen) "Failed to start Web Mode on port $port" else null
        )

        return@withContext socketOpen
    }

    suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        try {
            val stopFile = File(envManager.tmpDir, "web_stop.req")
            stopFile.writeText("stop\n")
        } catch (_: Exception) {}

        val scriptPath = "/usr/local/share/ilynxos/wireless-desktop/stop-web.sh"
        val cmd = "if [ -x $scriptPath ]; then $scriptPath; else pkill -9 -f websockify; fi"
        try {
            runtimeEngine.executeCliCommand(cmd) {}
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Web Mode: ${e.message}")
        }

        for (i in 1..10) {
            kotlinx.coroutines.delay(300)
            if (!checkSocketListening(getPort())) {
                break
            }
        }

        _state.value = ServiceState(status = ServiceStatus.STOPPED, port = getPort(), address = null)
        return@withContext true
    }

    suspend fun restart(
        port: Int = getPort(),
        password: String = getPassword(),
        useTls: Boolean = isTlsEnabled(),
        newSession: Boolean = isNewSession()
    ): Boolean {
        stop()
        kotlinx.coroutines.delay(500)
        return start(port, password, useTls, newSession)
    }

    suspend fun checkStatus(): Boolean = withContext(Dispatchers.IO) {
        val port = getPort()
        val isRunning = checkSocketListening(port)

        if (_state.value.status == ServiceStatus.STARTING && !isRunning) {
            return@withContext false
        }

        val ip = networkDiscovery.getFreshNetworkInfo().ipAddress
        val proto = if (isTlsEnabled()) "https" else "http"
        val currentPass = getPassword()
        val encPass = if (currentPass.isNotEmpty()) "&password=" + java.net.URLEncoder.encode(currentPass, "UTF-8") else ""
        val address = if (ip != null) "$proto://$ip:$port/vnc.html?autoconnect=true&resize=scale&quality=6&compression=1$encPass" else "$proto://localhost:$port/vnc.html?autoconnect=true&resize=scale&quality=6&compression=1$encPass"

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
