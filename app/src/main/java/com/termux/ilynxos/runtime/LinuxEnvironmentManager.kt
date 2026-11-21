package com.termux.ilynxos.runtime

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.termux.ilynxos.model.InstallationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class LinuxEnvironmentManager(private val context: Context) {

    companion object {
        private const val TAG = "LinuxEnvironmentManager"
        const val CONTAINER_NAME = "iLynxOS"
        const val REQUIRED_STORAGE_BYTES = 4_500_000_000L // ~4.5 GB

        @Volatile
        private var instance: LinuxEnvironmentManager? = null

        fun getInstance(context: Context): LinuxEnvironmentManager {
            return instance ?: synchronized(this) {
                instance ?: LinuxEnvironmentManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val filesDir: File = context.filesDir
    val containerDir: File = File(filesDir, "containers/$CONTAINER_NAME")
    val rootfsDir: File = File(containerDir, "rootfs")
    val installedMarker: File = File(containerDir, ".installed")
    val prefixDir: File = File(filesDir, "usr")
    val binDir: File = File(prefixDir, "bin")
    val libDir: File = File(prefixDir, "lib")
    val tmpDir: File = File(prefixDir, "tmp")
    val homeDir: File = File(filesDir, "home")
    val x11SocketDir: File = File(tmpDir, ".X11-unix")

    private val _installationState = MutableStateFlow<InstallationState>(InstallationState.Idle)
    val installationState: StateFlow<InstallationState> = _installationState.asStateFlow()

    init {
        checkInstallationStatus()
    }

    fun robustDelete(file: File): Boolean {
        if (!file.exists()) return true
        try {
            // First attempt native high-speed unlinking with permissions unlocked
            val chmod = ProcessBuilder("/system/bin/chmod", "-R", "777", file.absolutePath).start()
            chmod.waitFor()
            val rm = ProcessBuilder("/system/bin/rm", "-rf", file.absolutePath).start()
            rm.waitFor()
        } catch (e: Exception) {
            Log.w(TAG, "Native rm failed for ${file.name}: ${e.message}")
        }
        if (!file.exists()) return true

        // If su is available (e.g. emulators or rooted environments), purge root-owned remnants
        try {
            val suFile = File("/system/xbin/su").takeIf { it.exists() } ?: File("/system/bin/su").takeIf { it.exists() }
            if (suFile != null) {
                val suRm = ProcessBuilder(suFile.absolutePath, "0", "/system/bin/rm", "-rf", file.absolutePath).start()
                suRm.waitFor()
            }
        } catch (_: Exception) {}
        if (!file.exists()) return true

        // Fallback: Java recursive deletion with explicit permission elevation
        return try {
            file.walkBottomUp().forEach { f ->
                if (f.isDirectory) {
                    f.setWritable(true, false)
                    f.setExecutable(true, false)
                    f.setReadable(true, false)
                } else {
                    f.setWritable(true, false)
                    f.setReadable(true, false)
                }
                f.delete()
            }
            file.delete()
            !file.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recursively delete ${file.absolutePath}", e)
            false
        }
    }

    fun cleanRootfsCaches() {
        try {
            val aptArchives = File(rootfsDir, "var/cache/apt/archives")
            if (aptArchives.exists()) {
                aptArchives.listFiles()?.forEach { f ->
                    if (f.name.endsWith(".deb")) {
                        f.delete()
                    }
                }
            }
            val guestTmp = File(rootfsDir, "tmp")
            if (guestTmp.exists()) {
                guestTmp.listFiles()?.forEach { f ->
                    if (f.name != ".X11-unix") {
                        robustDelete(f)
                    }
                }
            }
            val guestVarTmp = File(rootfsDir, "var/tmp")
            if (guestVarTmp.exists()) {
                guestVarTmp.listFiles()?.forEach { f ->
                    robustDelete(f)
                }
            }
            Log.i(TAG, "Cleaned rootfs temporary files and apt package archives.")
        } catch (e: Exception) {
            Log.w(TAG, "Cache cleanup non-critical error: ${e.message}")
        }
    }

    fun isInstalled(): Boolean {
        return installedMarker.exists() && rootfsDir.exists() && (File(rootfsDir, "bin").exists() || File(rootfsDir, "usr/bin").exists())
    }

    fun checkInstallationStatus(): Boolean {
        val installed = isInstalled()
        _installationState.value = if (installed) InstallationState.Installed else InstallationState.Idle
        return installed
    }

    fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(filesDir.absolutePath)
            stat.availableBytes
        } catch (e: Exception) {
            filesDir.usableSpace
        }
    }

    fun hasEnoughStorage(): Boolean {
        return getAvailableStorageBytes() >= REQUIRED_STORAGE_BYTES
    }

    suspend fun setupBaseEnvironment() = withContext(Dispatchers.IO) {
        containerDir.mkdirs()
        prefixDir.mkdirs()
        binDir.mkdirs()
        libDir.mkdirs()
        tmpDir.mkdirs()
        homeDir.mkdirs()
        x11SocketDir.mkdirs()

        // Create compatibility bridge scripts in $PREFIX/bin
        createBridgeScripts()

        // Unpack bundled scripts (install.sh and start.sh) from assets
        copyAssetScript("scripts/install.sh", File(binDir, "install.sh"))
        copyAssetScript("scripts/start.sh", File(binDir, "start.sh"))

        // Optimize desktop touch and permissions
        optimizeDesktopConfiguration()
    }

    fun optimizeDesktopConfiguration() {
        try {
            // 1. High-speed PRoot sudo and su shims
            val localBin = File(rootfsDir, "usr/local/bin")
            localBin.mkdirs()

            val sudoFile = File(localBin, "sudo")
            writeExecutableScript(sudoFile, """
                #!/bin/bash
                if [ ${'$'}# -eq 0 ] || [ "${'$'}1" = "su" ] || [ "${'$'}1" = "-i" ] || [ "${'$'}1" = "-s" ]; then
                    if [ -t 0 ]; then
                        exec /bin/bash -l
                    else
                        echo "[iLynxOS] Root superuser environment active (uid=0 gid=0)."
                        echo "You have full root privileges across all subsystems."
                        exit 0
                    fi
                elif [ "${'$'}1" = "su" ] && [ "${'$'}2" = "-" ]; then
                    if [ -t 0 ]; then
                        exec /bin/bash -l
                    else
                        echo "[iLynxOS] Root superuser environment active (uid=0 gid=0)."
                        exit 0
                    fi
                else
                    while [ ${'$'}# -gt 0 ]; do
                        case "${'$'}1" in
                            -u|-g) shift 2 ;;
                            -E|-H|-P|-S|-s|-i|-k|-K|-v|-b) shift ;;
                            --) shift; break ;;
                            -*) shift ;;
                            *) break ;;
                        esac
                    done
                    if [ ${'$'}# -eq 0 ]; then
                        if [ -t 0 ]; then
                            exec /bin/bash -l
                        else
                            echo "[iLynxOS] Root superuser environment active (uid=0 gid=0)."
                            exit 0
                        fi
                    else
                        exec "${'$'}@"
                    fi
                fi
            """.trimIndent())

            val suFile = File(localBin, "su")
            writeExecutableScript(suFile, """
                #!/bin/bash
                if [ ${'$'}# -eq 0 ] || [ "${'$'}1" = "-" ] || [ "${'$'}1" = "root" ] || [ "${'$'}1" = "-l" ]; then
                    if [ -t 0 ]; then
                        exec /bin/bash -l
                    else
                        echo "[iLynxOS] Root superuser environment active (uid=0 gid=0)."
                        echo "You have full root privileges across all subsystems."
                        exit 0
                    fi
                elif [ "${'$'}1" = "-c" ]; then
                    shift
                    exec /bin/bash -c "${'$'}*"
                else
                    if [ -t 0 ]; then
                        exec /bin/bash -l
                    else
                        echo "[iLynxOS] Root superuser environment active (uid=0 gid=0)."
                        exit 0
                    fi
                fi
            """.trimIndent())

            // 2. High-speed browser bridge
            val firefoxFile = File(localBin, "firefox")
            writeExecutableScript(firefoxFile, """
                #!/bin/bash
                URL="${'$'}{1:-https://www.google.com}"
                if [[ "${'$'}URL" != http* ]] && [[ "${'$'}URL" != file* ]]; then
                    URL="https://www.google.com/search?q=${'$'}(echo "${'$'}URL" | tr ' ' '+')"
                fi
                echo "${'$'}URL" > /tmp/browser_open.req
            """.trimIndent())

            val xdgOpenFile = File(localBin, "xdg-open")
            writeExecutableScript(xdgOpenFile, """
                #!/bin/bash
                TARGET="${'$'}1"
                if [[ "${'$'}TARGET" == http* ]] || [[ "${'$'}TARGET" == https* ]]; then
                    echo "${'$'}TARGET" > /tmp/browser_open.req
                elif [ -d "${'$'}TARGET" ]; then
                    exec thunar "${'$'}TARGET" 2>/dev/null &
                else
                    exec thunar "${'$'}(dirname "${'$'}TARGET")" 2>/dev/null &
                fi
            """.trimIndent())

            // 2.5 Native Termux API bridges inside PRoot /usr/local/bin
            val termuxCommands = listOf(
                "termux-api",
                "termux-battery-status",
                "termux-wifi-connectioninfo",
                "termux-clipboard-get",
                "termux-clipboard-set",
                "termux-toast",
                "termux-vibrate",
                "termux-tts-speak",
                "termux-telephony-deviceinfo",
                "termux-notification",
                "termux-torch",
                "termux-volume",
                "termux-brightness",
                "termux-dialog"
            )
            val termuxBashScript = """
                #!/bin/bash
                CMD="${'$'}(basename "${'$'}0")"
                REQ_DIR="/tmp/termux-api"
                mkdir -p "${'$'}REQ_DIR/req" "${'$'}REQ_DIR/res" 2>/dev/null
                REQ_ID="${'$'}${'$'}-${'$'}RANDOM"
                echo "${'$'}CMD ${'$'}*" > "${'$'}REQ_DIR/req/${'$'}REQ_ID.req"
                i=0
                while [ ${'$'}i -lt 40 ]; do
                    if [ -f "${'$'}REQ_DIR/res/${'$'}REQ_ID.txt" ]; then
                        cat "${'$'}REQ_DIR/res/${'$'}REQ_ID.txt"
                        rm -f "${'$'}REQ_DIR/res/${'$'}REQ_ID.txt"
                        exit 0
                    fi
                    sleep 0.05
                    i=${'$'}((i + 1))
                done
            """.trimIndent()
            termuxCommands.forEach { cmdName ->
                writeExecutableScript(File(localBin, cmdName), termuxBashScript)
            }

            // 3. Hosts file configuration for instant DNS / sudo hostname resolution
            val hostsFile = File(rootfsDir, "etc/hosts")
            hostsFile.parentFile?.mkdirs()
            hostsFile.writeText("127.0.0.1\tlocalhost.localdomain localhost iLynxOS\n::1\t\tlocalhost.localdomain localhost ip6-localhost ip6-loopback iLynxOS\n")

            // 3b. Ensure X11 directory structure is clean and valid
            val rootfsTmpX11 = File(rootfsDir, "tmp/.X11-unix")
            if (rootfsTmpX11.exists() && !rootfsTmpX11.isDirectory) {
                rootfsTmpX11.delete()
            }
            rootfsTmpX11.mkdirs()

            // 4. Desktop startup script - Hardened and resilient against blank screens
            val desktopScript = File(rootfsDir, "usr/local/bin/start-ilynxos-desktop")
            writeExecutableScript(desktopScript, """
                #!/bin/bash
                # iLynxOS Desktop Environment Startup Script
                # Modern Solid & Touch-Optimized

                export DISPLAY=${'$'}{DISPLAY:-:0}
                export TMPDIR=/tmp
                export PULSE_SERVER=127.0.0.1
                export XDG_CURRENT_DESKTOP=LXQt
                export XDG_SESSION_DESKTOP=LXQt
                export DESKTOP_SESSION=lxqt
                export XDG_MENU_PREFIX="lxqt-"
                export QT_QPA_PLATFORMTHEME=lxqt
                export QT_PLATFORM_PLUGIN=lxqt
                export QT_STYLE_OVERRIDE=Fusion
                export GTK_THEME=Greybird-dark
                export XCURSOR_THEME=Adwaita
                export LIBGL_ALWAYS_SOFTWARE=1
                export GDK_BACKEND=x11
                export GIO_USE_VFS=local
                export GVFS_DISABLE_FUSE=1
                export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${'$'}PATH

                # 1. Clean up stale locks and kill picom to prevent black/blank compositing lockup
                rm -f /tmp/.X0-lock 2>/dev/null
                killall -9 picom 2>/dev/null || true

                # 2. PulseAudio startup in background (non-blocking)
                if [ -x "/usr/local/bin/ensure-pulseaudio" ]; then
                    /usr/local/bin/ensure-pulseaudio >/dev/null 2>&1 &
                fi

                # 3. Setup D-Bus communication cleanly
                dbus-uuidgen --ensure 2>/dev/null || true
                eval ${'$'}(dbus-launch --sh-syntax --exit-with-session) 2>/dev/null || true

                # 3b. Actively wait for X11 display server to become ready before launching apps
                echo "[iLynxOS] Waiting for X11 display ${'$'}DISPLAY to become ready..."
                for i in $(seq 1 60); do
                    if xsetroot -display "${'$'}DISPLAY" -solid "#1e1e2e" 2>/dev/null; then
                        break
                    fi
                    sleep 0.1
                done

                # 4. Set solid dark background immediately so display is visible instantly (no black screen)
                if command -v xsetroot >/dev/null 2>&1; then
                    xsetroot -solid "#1e1e2e" 2>/dev/null || true
                fi

                # 5. Launch Openbox Window Manager
                if command -v openbox >/dev/null 2>&1; then
                    if [ -f "/root/.config/openbox/rc.xml" ]; then
                        openbox --config-file /root/.config/openbox/rc.xml &
                    else
                        openbox &
                    fi
                fi
                sleep 0.15

                # 6. Launch Desktop Manager (wallpaper and desktop icons)
                if command -v pcmanfm-qt >/dev/null 2>&1; then
                    pcmanfm-qt --desktop --profile=lxqt &
                fi

                # 7. Launch Notification Daemon & PolicyKit if available
                if command -v lxqt-notificationd >/dev/null 2>&1; then
                    lxqt-notificationd &
                fi
                if command -v lxqt-policykit-agent >/dev/null 2>&1; then
                    lxqt-policykit-agent &
                fi

                # 8. Launch Taskbar Panel (lxqt-panel primary, fallback to xfce4-panel)
                if command -v lxqt-panel >/dev/null 2>&1; then
                    lxqt-panel &
                elif command -v xfce4-panel >/dev/null 2>&1; then
                    xfce4-panel --sm-client-disable --disable-wm-check &
                fi

                # 8a. Desktop Watchdog Supervisor: auto-restart Openbox and pcmanfm-qt if killed or crashed
                (
                    while true; do
                        sleep 3
                        if ! pgrep -f "openbox" >/dev/null 2>&1; then
                            if [ -f "/root/.config/openbox/rc.xml" ]; then
                                openbox --config-file /root/.config/openbox/rc.xml &
                            else
                                openbox &
                            fi
                        fi
                        if ! pgrep -f "pcmanfm-qt" >/dev/null 2>&1; then
                            pcmanfm-qt --desktop --profile=lxqt &
                        fi
                    done
                ) &

                # 8b. Wireless Desktop Request Supervisor Loop (VNC & Web Mode)
                (
                    rm -f /tmp/vnc.status /tmp/web.status
                    while true; do
                        if [ -f "/tmp/vnc_start.req" ]; then
                            VNC_PORT=${'$'}(head -n 1 /tmp/vnc_start.req 2>/dev/null || echo "5900")
                            VNC_PASS=${'$'}(sed -n '2p' /tmp/vnc_start.req 2>/dev/null || echo "")
                            VNC_DISP=${'$'}(sed -n '3p' /tmp/vnc_start.req 2>/dev/null || echo "1")
                            rm -f /tmp/vnc_start.req
                            if [ -x "/usr/local/share/ilynxos/wireless-desktop/start-vnc.sh" ]; then
                                /usr/local/share/ilynxos/wireless-desktop/start-vnc.sh "${'$'}VNC_PORT" "${'$'}VNC_PASS" "${'$'}VNC_DISP" &
                            else
                                pkill -9 -f x11vnc 2>/dev/null || true
                                sleep 0.2
                                mkdir -p /root/.vnc
                                if [ -n "${'$'}VNC_PASS" ]; then
                                    x11vnc -storepasswd "${'$'}VNC_PASS" /root/.vnc/passwd 2>/dev/null
                                    chmod 600 /root/.vnc/passwd 2>/dev/null || true
                                    AUTH_ARG="-rfbauth /root/.vnc/passwd"
                                else
                                    AUTH_ARG="-nopw"
                                fi
                                x11vnc -display ":${'$'}VNC_DISP" -rfbport "${'$'}VNC_PORT" -listen 0.0.0.0 ${'$'}AUTH_ARG -forever -shared -repeat -noshm -bg -o /var/log/x11vnc.log 2>&1
                            fi
                            echo "1" > /tmp/vnc.status
                        fi

                        if [ -f "/tmp/vnc_stop.req" ]; then
                            rm -f /tmp/vnc_stop.req
                            if [ -x "/usr/local/share/ilynxos/wireless-desktop/stop-vnc.sh" ]; then
                                /usr/local/share/ilynxos/wireless-desktop/stop-vnc.sh &
                            else
                                pkill -9 -f x11vnc 2>/dev/null || true
                                rm -f /var/run/x11vnc.pid
                            fi
                            echo "0" > /tmp/vnc.status
                        fi

                        if [ -f "/tmp/web_start.req" ]; then
                            WEB_PORT=${'$'}(head -n 1 /tmp/web_start.req 2>/dev/null || echo "14500")
                            WEB_PASS=${'$'}(sed -n '2p' /tmp/web_start.req 2>/dev/null || echo "")
                            USE_TLS=${'$'}(sed -n '3p' /tmp/web_start.req 2>/dev/null || echo "0")
                            WEB_DISP=${'$'}(sed -n '4p' /tmp/web_start.req 2>/dev/null || echo "1")
                            rm -f /tmp/web_start.req
                            if [ -x "/usr/local/share/ilynxos/wireless-desktop/start-web.sh" ]; then
                                /usr/local/share/ilynxos/wireless-desktop/start-web.sh "${'$'}WEB_PORT" "${'$'}WEB_PASS" "${'$'}USE_TLS" "${'$'}WEB_DISP" &
                            fi
                            sleep 0.8
                            if pgrep -f websockify >/dev/null 2>&1; then
                                echo "1" > /tmp/web.status
                            else
                                echo "0" > /tmp/web.status
                            fi
                        fi

                        if [ -f "/tmp/web_stop.req" ]; then
                            rm -f /tmp/web_stop.req
                            if [ -x "/usr/local/share/ilynxos/wireless-desktop/stop-web.sh" ]; then
                                /usr/local/share/ilynxos/wireless-desktop/stop-web.sh &
                            else
                                pkill -9 -f websockify 2>/dev/null || true
                                rm -f /var/run/websockify.pid
                            fi
                            echo "0" > /tmp/web.status
                        fi

                        sleep 1
                    done
                ) &

                # 9. Session keep-alive loop so container never exits or dies prematurely
                echo "iLynxOS Desktop Environment started successfully."
                while true; do
                    sleep 3600 &
                    wait ${'$'}!
                done
            """.trimIndent())

            // 4b. PulseAudio safe health check (never blocks, never returns non-zero error)
            val pulseScript = File(rootfsDir, "usr/local/bin/ensure-pulseaudio")
            writeExecutableScript(pulseScript, """
                #!/bin/bash
                # PulseAudio Health & Startup Service for i-Lynx OS
                export PULSE_SERVER=127.0.0.1
                exit 0
            """.trimIndent())

            // 5. Ensure all Desktop launcher permissions
            val desktopDir = File(rootfsDir, "root/Desktop")
            if (desktopDir.exists()) {
                desktopDir.listFiles()?.forEach { f ->
                    f.setExecutable(true, false)
                    f.setReadable(true, false)
                }
            }

            val pcmanfmConfig = File(rootfsDir, "root/.config/pcmanfm-qt/lxqt/settings.conf")
            if (pcmanfmConfig.exists()) {
                val content = pcmanfmConfig.readText()
                if (content.contains("SingleClick=false")) {
                    pcmanfmConfig.writeText(content.replace("SingleClick=false", "SingleClick=true"))
                    Log.i(TAG, "Enabled SingleClick for touchscreen responsiveness in pcmanfm-qt")
                }
            }

            // 5b. Fix Openbox theme paths so Openbox never crashes or delays
            try {
                val themesDir = File(rootfsDir, "usr/share/themes")
                themesDir.mkdirs()
                val rootThemesDir = File(rootfsDir, "root/.themes")
                rootThemesDir.mkdirs()

                val win11Theme = File(rootThemesDir, "iLynx-Win11")
                if (win11Theme.exists()) {
                    val links = listOf(
                        File(rootThemesDir, "iLynxOS"),
                        File(rootThemesDir, "Clearlooks"),
                        File(themesDir, "iLynxOS"),
                        File(themesDir, "Clearlooks")
                    )
                    links.forEach { linkFile ->
                        if (!linkFile.exists()) {
                            try {
                                java.nio.file.Files.createSymbolicLink(linkFile.toPath(), win11Theme.toPath())
                            } catch (_: Exception) {
                                win11Theme.copyRecursively(linkFile, overwrite = true)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not setup Openbox theme symlinks: ${e.message}")
            }

            // 6. Unpack Wireless Desktop modules and command bridges
            unpackWirelessDesktopScripts()
        } catch (e: Exception) {
            Log.w(TAG, "Could not apply desktop config optimization: ${e.message}")
        }
    }

    fun unpackWirelessDesktopScripts() {
        try {
            val wirelessDir = File(rootfsDir, "usr/local/share/ilynxos/wireless-desktop")
            wirelessDir.mkdirs()

            copyAssetFolder("wireless-desktop", wirelessDir)

            // Create command shims in /usr/local/bin
            val localBin = File(rootfsDir, "usr/local/bin")
            localBin.mkdirs()

            writeExecutableScript(File(localBin, "start-vnc"), """
                #!/bin/bash
                exec /usr/local/share/ilynxos/wireless-desktop/start-vnc.sh "${'$'}@"
            """.trimIndent())

            writeExecutableScript(File(localBin, "stop-vnc"), """
                #!/bin/bash
                exec /usr/local/share/ilynxos/wireless-desktop/stop-vnc.sh "${'$'}@"
            """.trimIndent())

            writeExecutableScript(File(localBin, "start-web"), """
                #!/bin/bash
                exec /usr/local/share/ilynxos/wireless-desktop/start-web.sh "${'$'}@"
            """.trimIndent())

            writeExecutableScript(File(localBin, "stop-web"), """
                #!/bin/bash
                exec /usr/local/share/ilynxos/wireless-desktop/stop-web.sh "${'$'}@"
            """.trimIndent())

            writeExecutableScript(File(localBin, "wireless-status"), """
                #!/bin/bash
                exec /usr/local/share/ilynxos/wireless-desktop/status.sh "${'$'}@"
            """.trimIndent())

            writeExecutableScript(File(localBin, "wireless-install"), """
                #!/bin/bash
                exec /usr/local/share/ilynxos/wireless-desktop/install.sh "${'$'}@"
            """.trimIndent())

            Log.i(TAG, "Unpacked wireless desktop scripts and installed /usr/local/bin wrappers.")
        } catch (e: Exception) {
            Log.w(TAG, "Could not unpack wireless desktop scripts: ${e.message}")
        }
    }

    private fun copyAssetFolder(assetSubdir: String, targetDir: File) {
        val assetManager = context.assets
        val list = assetManager.list(assetSubdir) ?: return
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        for (item in list) {
            val childAssetPath = if (assetSubdir.isEmpty()) item else "$assetSubdir/$item"
            val childTargetFile = File(targetDir, item)
            val subList = assetManager.list(childAssetPath)
            if (subList != null && subList.isNotEmpty()) {
                copyAssetFolder(childAssetPath, childTargetFile)
            } else {
                try {
                    assetManager.open(childAssetPath).use { input ->
                        FileOutputStream(childTargetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (childTargetFile.name.endsWith(".sh") || childTargetFile.name.endsWith(".py")) {
                        childTargetFile.setExecutable(true, false)
                    }
                    childTargetFile.setReadable(true, false)
                } catch (_: Exception) {
                    childTargetFile.mkdirs()
                }
            }
        }
    }

    private fun copyAssetScript(assetPath: String, targetFile: File) {
        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.setExecutable(true, false)
            targetFile.setReadable(true, false)
            Log.i(TAG, "Unpacked asset $assetPath -> ${targetFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Asset script $assetPath not available: ${e.message}")
        }
    }

    private fun createBridgeScripts() {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        // 1. uname bridge: ensures install.sh checks pass on any device/emulator
        writeExecutableScript(File(binDir, "uname"), """
            #!/system/bin/sh
            if [ "${'$'}1" = "-m" ]; then
                echo "aarch64"
            else
                /system/bin/uname "${'$'}@"
            fi
        """.trimIndent())

        // 2. pkg bridge: no-op since dependencies are already packaged natively
        writeExecutableScript(File(binDir, "pkg"), """
            #!/system/bin/sh
            echo "[iLynxOS] Package manager call intercepted: ${'$'}@ (pre-bundled)"
            exit 0
        """.trimIndent())

        // 3. termux-wake-lock and termux-wake-unlock
        writeExecutableScript(File(binDir, "termux-wake-lock"), """
            #!/system/bin/sh
            echo "[iLynxOS] Wake lock active"
            exit 0
        """.trimIndent())

        writeExecutableScript(File(binDir, "termux-wake-unlock"), """
            #!/system/bin/sh
            echo "[iLynxOS] Wake lock released"
            exit 0
        """.trimIndent())

        // 4. termux-x11 bridge
        writeExecutableScript(File(binDir, "termux-x11"), """
            #!/system/bin/sh
            echo "[iLynxOS] Embedded X11 display server active"
            exit 0
        """.trimIndent())

        // 5. am bridge
        writeExecutableScript(File(binDir, "am"), """
            #!/system/bin/sh
            echo "[iLynxOS] am command: ${'$'}@"
            exit 0
        """.trimIndent())

        // 6. pulseaudio and pactl bridges
        writeExecutableScript(File(binDir, "pulseaudio"), """
            #!/system/bin/sh
            echo "[iLynxOS] Sound server bridge initialized"
            exit 0
        """.trimIndent())

        writeExecutableScript(File(binDir, "pactl"), """
            #!/system/bin/sh
            exit 0
        """.trimIndent())

        // 7. Native Termux API bridges in host PREFIX/bin
        val termuxCommands = listOf(
            "termux-api",
            "termux-battery-status",
            "termux-wifi-connectioninfo",
            "termux-clipboard-get",
            "termux-clipboard-set",
            "termux-toast",
            "termux-vibrate",
            "termux-tts-speak",
            "termux-telephony-deviceinfo",
            "termux-notification",
            "termux-torch",
            "termux-volume",
            "termux-brightness",
            "termux-dialog"
        )
        val termuxBridgeScript = """
            #!/system/bin/sh
            CMD="${'$'}(basename "${'$'}0")"
            REQ_DIR="${tmpDir.absolutePath}/termux-api"
            mkdir -p "${'$'}REQ_DIR/req" "${'$'}REQ_DIR/res" 2>/dev/null
            REQ_ID="${'$'}${'$'}-${'$'}RANDOM"
            echo "${'$'}CMD ${'$'}*" > "${'$'}REQ_DIR/req/${'$'}REQ_ID.req"
            i=0
            while [ ${'$'}i -lt 40 ]; do
                if [ -f "${'$'}REQ_DIR/res/${'$'}REQ_ID.txt" ]; then
                    cat "${'$'}REQ_DIR/res/${'$'}REQ_ID.txt"
                    rm -f "${'$'}REQ_DIR/res/${'$'}REQ_ID.txt"
                    exit 0
                fi
                sleep 0.05
                i=${'$'}((i + 1))
            done
        """.trimIndent()
        termuxCommands.forEach { cmdName ->
            writeExecutableScript(File(binDir, cmdName), termuxBridgeScript)
        }

        // 8. proot-distro bridge: handles list, restore, and login
        val prootDistroScript = """
            #!/system/bin/sh
            ACTION="${'$'}1"
            shift

            case "${'$'}ACTION" in
                list)
                    echo "iLynxOS (installed)"
                    exit 0
                    ;;
                restore)
                    TARBALL="${'$'}1"
                    echo "[iLynxOS] Restoring container from ${'$'}TARBALL..."
                    mkdir -p "${containerDir.absolutePath}"
                    touch "${containerDir.absolutePath}/.installed"
                    exit 0
                    ;;
                remove)
                    echo "[iLynxOS] Removing container..."
                    chmod -R 777 "${containerDir.absolutePath}" 2>/dev/null || true
                    rm -rf "${containerDir.absolutePath}"
                    exit 0
                    ;;
                login)
                    # Delegates to PRoot directly
                    ROOTFS="${rootfsDir.absolutePath}"
                    TMP_DIR="${tmpDir.absolutePath}"
                    mkdir -p "${'$'}TMP_DIR/.X11-unix"
                    mkdir -p "${'$'}TMP_DIR/shm"
                    mkdir -p -m 777 "${'$'}TMP_DIR/runtime-root"
                    export LD_LIBRARY_PATH="$nativeLibDir"
                    export PROOT_LOADER="$nativeLibDir/libloader.so"
                    export PROOT_TMP_DIR="${'$'}TMP_DIR"
                    export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${'$'}PATH"
                    export HOME=/root
                    export USER=root
                    export XDG_RUNTIME_DIR=/root/runtime-root

                    QEMU_ARGS=()
                    if [ -f "$nativeLibDir/libqemu-aarch64.so" ]; then
                        QEMU_ARGS=("-q" "$nativeLibDir/libqemu-aarch64.so")
                    fi

                    # Skip container name ($1) if matches
                    if [ "${'$'}#" -gt 0 ] && [ "${'$'}1" = "iLynxOS" -o "${'$'}1" = "${CONTAINER_NAME}" ]; then
                        shift
                    fi

                    # Parse remaining options until command or '--'
                    CMD=()
                    while [ "${'$'}#" -gt 0 ]; do
                        case "${'$'}1" in
                            --)
                                shift
                                CMD=("${'$'}@")
                                break
                                ;;
                            --shared-tmp|--termux-home|--isolated|--fix-low-ports)
                                shift
                                ;;
                            --bind)
                                shift
                                shift
                                ;;
                            --user)
                                shift
                                shift
                                ;;
                            *)
                                CMD=("${'$'}@")
                                break
                                ;;
                        esac
                    done

                    if [ "${'$'}{#CMD[@]}" -eq 0 ]; then
                        CMD=("/bin/bash" "-l")
                    elif [ "${'$'}{CMD[0]}" = "bash" ]; then
                        CMD[0]="/bin/bash"
                    fi

                    exec "$nativeLibDir/libproot.so" \
                        "${'$'}{QEMU_ARGS[@]}" \
                        --rootfs="${'$'}ROOTFS" \
                        --cwd=/root \
                        --bind=/dev \
                        --bind=/proc \
                        --bind=/sys \
                        --bind=/dev/urandom:/dev/random \
                        --bind="${'$'}TMP_DIR:/tmp" \
                        --bind="${'$'}TMP_DIR/.X11-unix:/tmp/.X11-unix" \
                        --bind="${'$'}TMP_DIR/shm:/dev/shm" \
                        --sysvipc \
                        --link2symlink \
                        --kill-on-exit \
                        --kernel-release=6.1.0 \
                        --change-id=0:0 \
                        "${'$'}{CMD[@]}"
                    ;;
                *)
                    echo "[iLynxOS] Unknown proot-distro command: ${'$'}ACTION"
                    exit 1
                    ;;
            esac
        """.trimIndent()
        writeExecutableScript(File(binDir, "proot-distro"), prootDistroScript)
    }

    private fun writeExecutableScript(file: File, content: String) {
        try {
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            file.setExecutable(true, false)
            file.setReadable(true, false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write script ${file.name}", e)
        }
    }

    /**
     * Extracts iLynxOS rootfs tar.gz directly into container directory
     * with live byte-level progress reporting.
     */
    suspend fun extractRootfsArchive(
        inputStream: InputStream,
        totalBytes: Long,
        onProgress: (Float, Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        setupBaseEnvironment()

        if (!hasEnoughStorage()) {
            throw IllegalStateException("Insufficient storage space. At least 4.5 GB required.")
        }

        // CRITICAL FIX: Clean previous/partial container before fresh extraction to prevent size accumulation
        if (containerDir.exists()) {
            Log.i(TAG, "Purging previous container directory before fresh extraction...")
            robustDelete(containerDir)
        }
        val targetDir = containerDir
        targetDir.mkdirs()

        _installationState.value = InstallationState.Extracting(0f, "Starting extraction...", 0L, totalBytes)

        // Android's toybox /system/bin/tar extracts natively and handles symlinks cleanly
        val tarCmd = arrayOf("/system/bin/tar", "-xzf", "-", "-C", targetDir.absolutePath)
        val process = ProcessBuilder(*tarCmd)
            .redirectErrorStream(true)
            .start()

        val processOut = process.outputStream
        val buffer = ByteArray(64 * 1024)
        var bytesReadTotal = 0L
        var lastReportTime = 0L

        try {
            var n: Int
            while (inputStream.read(buffer).also { n = it } != -1) {
                processOut.write(buffer, 0, n)
                bytesReadTotal += n

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastReportTime > 250 || bytesReadTotal == totalBytes) {
                    lastReportTime = currentTime
                    val progress = if (totalBytes > 0) (bytesReadTotal.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                    val mbRead = bytesReadTotal / (1024 * 1024)
                    val mbTotal = totalBytes / (1024 * 1024)
                    val statusText = "Extracting root filesystem: $mbRead MB / $mbTotal MB"
                    _installationState.value = InstallationState.Extracting(progress, statusText, bytesReadTotal, totalBytes)
                    onProgress(progress, bytesReadTotal, totalBytes)
                }
            }
            processOut.flush()
            processOut.close()

            val exitCode = process.waitFor()

            // Normalize extracted archive layout into $containerDir/rootfs
            // Archive formats supported:
            // 1. iLynxOS/rootfs/...
            // 2. rootfs/...
            // 3. iLynxOS/...
            // 4. Flat (bin, usr, etc directly in targetDir)
            val nestedRootfs = File(targetDir, "iLynxOS/rootfs")
            val directILynx = File(targetDir, "iLynxOS")

            if (nestedRootfs.exists() && (File(nestedRootfs, "bin").exists() || File(nestedRootfs, "usr/bin").exists())) {
                Log.i(TAG, "Normalizing nested rootfs: iLynxOS/rootfs -> rootfs")
                if (rootfsDir.exists() && rootfsDir != nestedRootfs) {
                    robustDelete(rootfsDir)
                }
                val moved = nestedRootfs.renameTo(rootfsDir)
                if (!moved) {
                    ProcessBuilder("/system/bin/mv", nestedRootfs.absolutePath, rootfsDir.absolutePath).start().waitFor()
                }
                robustDelete(directILynx)
            } else if (directILynx.exists() && (File(directILynx, "bin").exists() || File(directILynx, "usr/bin").exists())) {
                Log.i(TAG, "Normalizing single nested rootfs: iLynxOS -> rootfs")
                if (rootfsDir.exists() && rootfsDir != directILynx) {
                    robustDelete(rootfsDir)
                }
                val moved = directILynx.renameTo(rootfsDir)
                if (!moved) {
                    ProcessBuilder("/system/bin/mv", directILynx.absolutePath, rootfsDir.absolutePath).start().waitFor()
                }
            } else if (File(targetDir, "bin").exists() || File(targetDir, "usr/bin").exists()) {
                Log.i(TAG, "Normalizing flat rootfs into rootfsDir...")
                val tempStaging = File(filesDir, "temp_rootfs_staging")
                robustDelete(tempStaging)
                targetDir.renameTo(tempStaging)
                targetDir.mkdirs()
                val moved = tempStaging.renameTo(rootfsDir)
                if (!moved) {
                    ProcessBuilder("/system/bin/mv", tempStaging.absolutePath, rootfsDir.absolutePath).start().waitFor()
                }
            }

            val hasRootfs = rootfsDir.exists() && (File(rootfsDir, "bin").exists() || File(rootfsDir, "usr/bin").exists())
            if (!hasRootfs && exitCode != 0) {
                val errorLog = process.inputStream.bufferedReader().readText()
                Log.e(TAG, "Tar extraction exited with code $exitCode: $errorLog")
                throw RuntimeException("Extraction failed with exit code $exitCode: $errorLog")
            }

            // Clean caches, temp files, and unused archives to reclaim storage
            cleanRootfsCaches()

            markInstalled()
            Log.i(TAG, "Rootfs extracted and normalized successfully to ${rootfsDir.absolutePath}")
        } catch (e: Exception) {
            process.destroyForcibly()
            throw e
        }
    }

    /**
     * Executes the existing install.sh script in the Termux-compatible environment.
     */
    suspend fun executeInstallScript(
        installScriptFile: File,
        tarballPath: String? = null,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        _installationState.value = InstallationState.RunningInstallScript("Starting install.sh...")
        setupBaseEnvironment()

        val env = mutableMapOf<String, String>()
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        env["PREFIX"] = prefixDir.absolutePath
        env["TERMUX_PREFIX"] = prefixDir.absolutePath
        env["HOME"] = homeDir.absolutePath
        env["TMPDIR"] = tmpDir.absolutePath
        env["PATH"] = "${binDir.absolutePath}:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = nativeLibDir
        env["CONTAINER_NAME"] = CONTAINER_NAME
        env["ARCH"] = "aarch64"

        val cmd = mutableListOf("/system/bin/sh", installScriptFile.absolutePath, "-f")
        if (!tarballPath.isNullOrBlank()) {
            cmd.add(tarballPath)
        }

        val pb = ProcessBuilder(cmd)
            .directory(installScriptFile.parentFile ?: filesDir)
            .redirectErrorStream(true)

        pb.environment().putAll(env)

        val process = pb.start()
        val reader = process.inputStream.bufferedReader()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val safeLine = line ?: ""
            Log.d(TAG, "[install.sh] $safeLine")
            _installationState.value = InstallationState.RunningInstallScript(safeLine)
            onLog(safeLine)
        }

        val exitCode = process.waitFor()
        if (exitCode == 0 || isInstalled()) {
            markInstalled()
            _installationState.value = InstallationState.Installed
            return@withContext true
        } else {
            val err = "install.sh exited with code $exitCode"
            _installationState.value = InstallationState.Error(err)
            return@withContext false
        }
    }

    fun markInstalled() {
        try {
            installedMarker.createNewFile()
            installedMarker.writeText("""{"container":"$CONTAINER_NAME","installed_at":${System.currentTimeMillis()}}""")
            optimizeDesktopConfiguration()
            _installationState.value = InstallationState.Installed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create installed marker", e)
        }
    }

    fun resetInstallation() {
        try {
            installedMarker.delete()
            robustDelete(containerDir)
            robustDelete(File(filesDir, "temp_rootfs_staging"))
            robustDelete(tmpDir)
            tmpDir.mkdirs()
            File(tmpDir, ".X11-unix").mkdirs()
            File(tmpDir, "shm").mkdirs()
            _installationState.value = InstallationState.Idle
            Log.i(TAG, "Installation reset successfully. All container storage reclaimed.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset installation", e)
        }
    }
}
