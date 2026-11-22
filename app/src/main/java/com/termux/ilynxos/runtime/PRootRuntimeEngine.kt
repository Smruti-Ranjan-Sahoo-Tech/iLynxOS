package com.termux.ilynxos.runtime

import android.content.Context
import android.util.Log
import com.termux.ilynxos.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PRootRuntimeEngine(private val context: Context) {

    companion object {
        private const val TAG = "PRootRuntimeEngine"

        @Volatile
        private var instance: PRootRuntimeEngine? = null

        fun getInstance(context: Context): PRootRuntimeEngine {
            return instance ?: synchronized(this) {
                instance ?: PRootRuntimeEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val envManager = LinuxEnvironmentManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var activeProcess: Process? = null
    private var activeJob: Job? = null

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Stopped)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _terminalOutput = MutableSharedFlow<String>(replay = 100)
    val terminalOutput: SharedFlow<String> = _terminalOutput.asSharedFlow()

    fun isSessionRunning(): Boolean {
        return activeProcess?.isAlive == true
    }

    /**
     * Executes start.sh or the LXQt desktop launch sequence inside PRoot
     */
    suspend fun startDesktopSession(startScriptFile: File? = null) = withContext(Dispatchers.IO) {
        if (isSessionRunning()) {
            Log.w(TAG, "Session already active. Ignoring duplicate start request.")
            return@withContext
        }

        _sessionState.value = SessionState.Starting("Initializing runtime environment...")
        envManager.setupBaseEnvironment()
        envManager.optimizeDesktopConfiguration()
        File(envManager.tmpDir, ".X11-unix").mkdirs()
        File(envManager.tmpDir, "shm").mkdirs()

        // Ensure embedded X11 server is started and socket is ready
        val x11Engine = X11DisplayEngine.getInstance(context)
        x11Engine.startServer()
        val socketFile = File(envManager.tmpDir, ".X11-unix/X0")
        for (i in 1..30) {
            if (socketFile.exists()) {
                Log.i(TAG, "X11 server socket verified ready at ${socketFile.absolutePath}")
                break
            }
            kotlinx.coroutines.delay(100)
        }

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val prootBin = File(nativeLibDir, "libproot.so")
        val loaderBin = File(nativeLibDir, "libloader.so")

        if (!prootBin.exists()) {
            _sessionState.value = SessionState.Error("PRoot binary not found at ${prootBin.absolutePath}")
            return@withContext
        }

        // Clean stale display sockets
        cleanupStaleLocks()

        val rootfsPath = envManager.rootfsDir.absolutePath
        val tmpPath = envManager.tmpDir.absolutePath
        val homePath = envManager.homeDir.absolutePath
        val prefixPath = envManager.prefixDir.absolutePath
        val binPath = envManager.binDir.absolutePath

        // Direct LXQt boot via PRoot (or custom script if explicitly passed)
        val pb = if (startScriptFile != null && startScriptFile.exists()) {
            _sessionState.value = SessionState.Starting("Executing ${startScriptFile.name}...")
            ProcessBuilder("/system/bin/sh", startScriptFile.absolutePath, "-d")
                .directory(startScriptFile.parentFile ?: envManager.filesDir)
        } else {
            // Direct LXQt boot via PRoot
            _sessionState.value = SessionState.Starting("Launching LXQt modern desktop...")
            val qemuBin = File(nativeLibDir, "libqemu-aarch64.so").takeIf { it.exists() }
            val prootArgs = mutableListOf<String>()
            if (qemuBin != null) {
                prootArgs.add("-q")
                prootArgs.add(qemuBin.absolutePath)
            }
            prootArgs.addAll(listOf(
                "--rootfs=$rootfsPath",
                "--cwd=/root",
                "--bind=/dev",
                "--bind=/proc",
                "--bind=/sys",
                "--bind=/dev/urandom:/dev/random",
                "--bind=$tmpPath:/tmp",
                "--bind=$tmpPath/.X11-unix:/tmp/.X11-unix",
                "--bind=$tmpPath/shm:/dev/shm",
                "--sysvipc",
                "--link2symlink",
                "--kill-on-exit",
                "--kernel-release=6.1.0",
                "--change-id=0:0",
                "/bin/bash",
                "-c",
                """
                    unset LD_LIBRARY_PATH
                    unset LD_PRELOAD
                    export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${'$'}PATH
                    export DISPLAY=:0
                    export TMPDIR=/tmp
                    export PULSE_SERVER=127.0.0.1
                    export LIBGL_ALWAYS_SOFTWARE=1
                    export MOZ_ENABLE_WAYLAND=0
                    export GDK_BACKEND=x11
                    export HOME=/root
                    export USER=root
                    mkdir -p -m 700 /root/runtime-root
                    export XDG_RUNTIME_DIR=/root/runtime-root
                    mkdir -p -m 777 /tmp/runtime-root 2>/dev/null || true
                    rm -rf /var/run/dbus/pid /run/dbus/pid
                    dbus-uuidgen --ensure 2>/dev/null || true
                    if [ -f /usr/local/bin/start-ilynxos-desktop ]; then
                        exec /bin/bash /usr/local/bin/start-ilynxos-desktop
                    else
                        exec dbus-launch --exit-with-session startlxqt
                    fi
                """.trimIndent()
            ))

            val shScript = buildString {
                append("export LD_LIBRARY_PATH=\"$nativeLibDir\"\n")
                append("export PROOT_LOADER=\"${loaderBin.absolutePath}\"\n")
                append("export PROOT_TMP_DIR=\"$tmpPath\"\n")
                append("exec \"${prootBin.absolutePath}\"")
                for (arg in prootArgs) {
                    append(" '").append(arg.replace("'", "'\\''")).append("'")
                }
            }
            ProcessBuilder("/system/bin/sh", "-c", shScript).directory(envManager.filesDir)
        }

        val env = pb.environment()
        env["PREFIX"] = prefixPath
        env["TERMUX_PREFIX"] = prefixPath
        env["HOME"] = homePath
        env["TMPDIR"] = tmpPath
        env["PROOT_TMP_DIR"] = tmpPath
        env["PATH"] = "$binPath:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = nativeLibDir
        env["PROOT_LOADER"] = loaderBin.absolutePath
        env["DISPLAY"] = ":0"
        env["CONTAINER_NAME"] = LinuxEnvironmentManager.CONTAINER_NAME
        env["ARCH"] = "aarch64"
        env["USER"] = "root"

        pb.redirectErrorStream(true)

        try {
            val process = pb.start()
            activeProcess = process
            _sessionState.value = SessionState.Starting("Launching LXQt modern desktop...")

            activeJob = scope.launch {
                // Fallback timer: after 6 seconds, transition to Running even if log parsing doesn't match
                val timeoutJob = launch {
                    kotlinx.coroutines.delay(6000)
                    if (_sessionState.value is SessionState.Starting && activeProcess?.isAlive == true) {
                        _sessionState.value = SessionState.Running(System.currentTimeMillis(), ":0")
                    }
                }

                try {
                    val reader = process.inputStream.bufferedReader()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val safeLine = line ?: ""
                        Log.d(TAG, "[Desktop] $safeLine")
                        _terminalOutput.emit(safeLine)

                        if (_sessionState.value is SessionState.Starting) {
                            if (safeLine.contains("Desktop Environment started successfully", ignoreCase = true) ||
                                safeLine.contains("pcmanfm-qt", ignoreCase = true) ||
                                safeLine.contains("lxqt-panel", ignoreCase = true) ||
                                safeLine.contains("openbox", ignoreCase = true)) {
                                timeoutJob.cancel()
                                _sessionState.value = SessionState.Running(System.currentTimeMillis(), ":0")
                            }
                        }
                    }

                    val exitCode = process.waitFor()
                    timeoutJob.cancel()
                    Log.i(TAG, "Desktop session terminated with code $exitCode")
                    cleanupStaleLocks()
                    _sessionState.value = SessionState.Stopped
                } catch (e: Exception) {
                    timeoutJob.cancel()
                    Log.i(TAG, "Desktop session reader closed: ${e.message}")
                    cleanupStaleLocks()
                    _sessionState.value = SessionState.Stopped
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch desktop session", e)
            _sessionState.value = SessionState.Error("Launch failed: ${e.message}")
        }
    }

    /**
     * Starts an interactive CLI shell inside the iLynxOS rootfs
     */
    suspend fun executeCliCommand(
        command: String,
        onOutput: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val trimmed = command.trim()

        // 1. Direct Termux API command handling
        if (trimmed.startsWith("termux-")) {
            val parts = trimmed.split(Regex("\\s+"), limit = 2)
            val cmd = parts[0]
            val args = parts.getOrElse(1) { "" }
            try {
                val bridge = TermuxApiBridge.getInstance(context)
                val res = bridge.executeCommand(cmd, args)
                res.lines().forEach { line ->
                    if (line.isNotEmpty()) onOutput(line)
                }
            } catch (e: Exception) {
                onOutput("[Termux-API Error] ${e.message}")
            }
            return@withContext
        }

        // 2. Check installation
        if (!envManager.isInstalled()) {
            onOutput("[Notice] iLynxOS container is not installed yet.")
            onOutput("Please run the automated installer from the Dashboard or Welcome screen.")
            return@withContext
        }

        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val prootBin = File(nativeLibDir, "libproot.so")
        val loaderBin = File(nativeLibDir, "libloader.so")
        val rootfsPath = envManager.rootfsDir.absolutePath
        val tmpPath = envManager.tmpDir.absolutePath

        val qemuBin = File(nativeLibDir, "libqemu-aarch64.so").takeIf { it.exists() }
        val prootArgs = mutableListOf<String>()
        if (qemuBin != null) {
            prootArgs.add("-q")
            prootArgs.add(qemuBin.absolutePath)
        }
        prootArgs.addAll(listOf(
            "--rootfs=$rootfsPath",
            "--cwd=/root",
            "--bind=/dev",
            "--bind=/proc",
            "--bind=/sys",
            "--bind=/dev/urandom:/dev/random",
            "--bind=$tmpPath:/tmp",
            "--bind=$tmpPath/.X11-unix:/tmp/.X11-unix",
            "--bind=$tmpPath/shm:/dev/shm",
            "--sysvipc",
            "--link2symlink",
            "--kernel-release=6.1.0",
            "--change-id=0:0",
            "/bin/bash",
            "-c",
            "unset LD_LIBRARY_PATH; unset LD_PRELOAD; export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH; $command"
        ))

        val shScript = buildString {
            append("export LD_LIBRARY_PATH=\"$nativeLibDir\"\n")
            append("export PROOT_LOADER=\"${loaderBin.absolutePath}\"\n")
            append("export PROOT_TMP_DIR=\"$tmpPath\"\n")
            append("exec \"${prootBin.absolutePath}\"")
            for (arg in prootArgs) {
                append(" '").append(arg.replace("'", "'\\''")).append("'")
            }
        }

        val pb = ProcessBuilder("/system/bin/sh", "-c", shScript).directory(envManager.filesDir)
        val env = pb.environment()
        env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = nativeLibDir
        env["PROOT_LOADER"] = loaderBin.absolutePath
        env["PROOT_TMP_DIR"] = tmpPath
        env["HOME"] = "/root"
        env["USER"] = "root"
        env["TMPDIR"] = "/tmp"
        pb.redirectErrorStream(true)

        val proc = pb.start()
        try {
            proc.outputStream.close()
        } catch (_: Exception) {}

        val reader = proc.inputStream.bufferedReader()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            line?.let { onOutput(it) }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
        } else {
            proc.waitFor()
        }
    }

    fun stopDesktopSession() {
        Log.i(TAG, "Stopping desktop session...")
        activeProcess?.destroyForcibly()
        activeProcess = null
        activeJob?.cancel()
        cleanupStaleLocks()
        _sessionState.value = SessionState.Stopped
    }

    private fun cleanupStaleLocks() {
        try {
            val tmpDir = envManager.tmpDir
            File(tmpDir, ".X0-lock").delete()
            if (!X11DisplayEngine.getInstance(context).isServerRunning.value) {
                File(tmpDir, ".X11-unix/X0").delete()
            }
            File("/tmp/.X0-lock").delete()
        } catch (e: Exception) {
            // Ignore lock deletion failures
        }
    }
}
