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
        const val DEFAULT_PORT = 5900
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
        if (getPassword().isEmpty()) {
            val autoPass = "ilynx" + UUID.randomUUID().toString().take(4)
            setPassword(autoPass)
        }
    }

    fun getPort(): Int = prefs.getInt(KEY_PORT, DEFAULT_PORT)
    fun setPort(port: Int) = prefs.edit().putInt(KEY_PORT, port).apply()

    fun getPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""
    fun setPassword(pass: String) = prefs.edit().putString(KEY_PASSWORD, pass).apply()

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)
    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

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

    suspend fun start(port: Int = getPort(), password: String = getPassword()): Boolean = withContext(Dispatchers.IO) {
        if (!envManager.isInstalled()) {
            _state.value = _state.value.copy(
                status = ServiceStatus.NOT_INSTALLED,
                error = "iLynxOS container not installed"
            )
            return@withContext false
        }

        setPort(port)
        setPassword(password)

        _state.value = _state.value.copy(status = ServiceStatus.STARTING, error = null)

        // 1. Ensure desktop session and X11 server are active
        if (!runtimeEngine.isSessionRunning()) {
            Log.i(TAG, "Desktop session not running; starting LinuxSessionService...")
            try {
                com.termux.ilynxos.runtime.LinuxSessionService.startService(context)
                val socketFile = File(envManager.tmpDir, ".X11-unix/X0")
                for (i in 1..40) {
                    if (socketFile.exists() && runtimeEngine.isSessionRunning()) {
                        Log.i(TAG, "Session and X11 socket verified ready for VNC")
                        break
                    }
                    kotlinx.coroutines.delay(200)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed starting desktop session: ${e.message}")
            }
        }

        // 2. Ensure wireless scripts unpacked
        envManager.unpackWirelessDesktopScripts()

        // 3. Write IPC request to tmpDir (watched by supervisor loop in PRoot desktop session)
        try {
            val reqFile = File(envManager.tmpDir, "vnc_start.req")
            reqFile.writeText("$port\n$password\n")
            Log.i(TAG, "Written vnc_start.req (port: $port)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing vnc_start.req: ${e.message}")
        }

        // 4. Poll for socket availability
        var socketOpen = false
        for (i in 1..25) {
            kotlinx.coroutines.delay(400)
            if (checkSocketListening(port)) {
                Log.i(TAG, "VNC server detected listening on port $port (attempt $i)")
                socketOpen = true
                break
            }
            if (i == 5 && !checkSocketListening(port)) {
                try {
                    runtimeEngine.executeCliCommand("/usr/local/share/ilynxos/wireless-desktop/start-vnc.sh $port '$password' &") {}
                } catch (_: Exception) {}
            }
        }

        val ip = networkDiscovery.getFreshNetworkInfo().ipAddress
        val address = if (ip != null) "$ip:$port" else "localhost:$port"

        _state.value = ServiceState(
            status = if (socketOpen) ServiceStatus.RUNNING else ServiceStatus.STOPPED,
            port = port,
            address = if (socketOpen) address else null,
            pid = null,
            error = if (!socketOpen) "Failed to bind VNC port $port" else null
        )

        return@withContext socketOpen
    }

    suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        try {
            val stopFile = File(envManager.tmpDir, "vnc_stop.req")
            stopFile.writeText("stop\n")
        } catch (_: Exception) {}

        try {
            runtimeEngine.executeCliCommand("/usr/local/share/ilynxos/wireless-desktop/stop-vnc.sh &") {}
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

    suspend fun restart(port: Int = getPort(), password: String = getPassword()): Boolean {
        stop()
        kotlinx.coroutines.delay(500)
        return start(port, password)
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
