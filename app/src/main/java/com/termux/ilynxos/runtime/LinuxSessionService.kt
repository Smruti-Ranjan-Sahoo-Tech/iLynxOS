package com.termux.ilynxos.runtime

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.termux.ilynxos.ILynxApplication
import com.termux.ilynxos.R
import com.termux.ilynxos.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class LinuxSessionService : Service() {

    companion object {
        private const val TAG = "LinuxSessionService"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SESSION = "com.termux.ilynxos.ACTION_START_SESSION"
        const val ACTION_STOP_SESSION = "com.termux.ilynxos.ACTION_STOP_SESSION"
        const val EXTRA_START_SCRIPT = "extra_start_script"

        fun startService(context: Context, startScriptPath: String? = null) {
            val intent = Intent(context, LinuxSessionService::class.java).apply {
                action = ACTION_START_SESSION
                startScriptPath?.let { putExtra(EXTRA_START_SCRIPT, it) }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LinuxSessionService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
            context.startService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var ipcWatcherJob: Job? = null

    private fun startIpcWatcher() {
        ipcWatcherJob?.cancel()
        ipcWatcherJob = scope.launch(Dispatchers.IO) {
            val envManager = LinuxEnvironmentManager.getInstance(applicationContext)
            val reqFile = File(envManager.tmpDir, "browser_open.req")
            while (true) {
                try {
                    if (reqFile.exists()) {
                        val url = reqFile.readText().trim()
                        reqFile.delete()
                        if (url.isNotEmpty()) {
                            Log.i(TAG, "Opening external browser for URL: $url")
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error handling browser IPC request: ${e.message}")
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        try {
            TermuxApiBridge.getInstance(applicationContext).startIpcWatcher()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start TermuxApiBridge IPC watcher: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SESSION

        when (action) {
            ACTION_STOP_SESSION -> {
                Log.i(TAG, "Stop action requested from notification/UI")
                stopDesktopSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_SESSION -> {
                val startScriptPath = intent?.getStringExtra(EXTRA_START_SCRIPT)
                val scriptFile = startScriptPath?.let { File(it) }

                val notification = buildForegroundNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                launchSession(scriptFile)
            }
        }

        return START_STICKY
    }

    private fun launchSession(scriptFile: File?) {
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Start embedded X11 server
                val x11Engine = X11DisplayEngine.getInstance(applicationContext)
                x11Engine.startServer()

                // Wait for embedded X11 socket to become ready
                val envManager = LinuxEnvironmentManager.getInstance(applicationContext)
                val socketFile = File(envManager.tmpDir, ".X11-unix/X0")
                for (i in 1..25) {
                    if (socketFile.exists()) {
                        Log.i(TAG, "X11 socket verified ready at ${socketFile.absolutePath}")
                        break
                    }
                    kotlinx.coroutines.delay(100)
                }

                // 2. Launch PRoot desktop runtime
                val runtimeEngine = PRootRuntimeEngine.getInstance(applicationContext)
                runtimeEngine.startDesktopSession(scriptFile)

                // 3. Start IPC watcher for browser & application bridge
                startIpcWatcher()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting session in foreground service", e)
            }
        }
    }

    private fun stopDesktopSession() {
        try {
            ipcWatcherJob?.cancel()
            ipcWatcherJob = null

            val runtimeEngine = PRootRuntimeEngine.getInstance(applicationContext)
            runtimeEngine.stopDesktopSession()

            scope.launch(Dispatchers.IO) {
                try {
                    com.termux.ilynxos.wireless.WirelessDesktopManager.getInstance(applicationContext).stopAll()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping wireless desktop services: ${e.message}")
                }
            }

            val x11Engine = X11DisplayEngine.getInstance(applicationContext)
            x11Engine.stopServer()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping session", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "iLynxOS::DesktopSessionWakeLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LinuxSessionService::class.java).apply {
            action = ACTION_STOP_SESSION
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ILynxApplication.CHANNEL_ID)
            .setContentTitle("iLynxOS — Desktop Session Active")
            .setContentText("LXQt Modern desktop running on display :0")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Desktop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopDesktopSession()
        releaseWakeLock()
        super.onDestroy()
    }
}
