# iLynxOS 1.03 (Noble Modern Edition) - Full Technical & Setup Documentation
**Standalone Native Android OS Environment with LXQt Modern Desktop**

### 📥 Downloads & Quick Links

| Deliverable / Resource | Download / Access Link | Size & Notes |
| :--- | :--- | :--- |
| **📱 iLynxOS Android APK** | [**Download iLynxOS-v1.03-NobleModern.apk**](https://github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS/raw/main/iLynxOS-v1.03-NobleModern.apk) • [Local File Link](file:///media/smruti/bb847b45-9fb8-4899-b51d-779c6768f27f1/mobile-dev-os/smartphone-movile-os/iLynxOS/iLynxOS-v1.03-NobleModern.apk) | **~28 MB** • Noble Modern Edition standalone APK |
| **📦 Rootfs Archive (iLynx.tar.gz)** | [**Download iLynx.tar.gz (Google Drive)**](https://drive.google.com/drive/folders/1NccoZ5Rzscwexz7wHoOnbkjgeeWpkwaf) | **~1.4 GB** • Official Ubuntu 24.04 LTS LXQt filesystem |
| **🏠 GitHub Repository** | [**Smruti-Ranjan-Sahoo-Tech/iLynxOS**](https://github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS) | Source repository & releases |

---

Developer & Lead Architect: **Smruti Ranjan Sahoo**  
Email: [s.r.sahoo370@gmail.com](mailto:s.r.sahoo370@gmail.com)  
GitHub Profile: [https://github.com/Smruti-Ranjan-Sahoo-Tech](https://github.com/Smruti-Ranjan-Sahoo-Tech)  
Repository: [https://github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS](https://github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS)  

---

## 🌟 Overview

**iLynxOS** is a standalone, native Android application built in **Kotlin** and **Jetpack Compose** that embeds a full **Ubuntu 24.04 LTS (Noble Numbat)** modern Linux distribution powered by the **LXQt Desktop Environment**.

Unlike traditional mobile Linux setups, **iLynxOS requires no external Termux app, no Termux:X11 app, and no VNC viewer**. Everything is bundled directly into a single, cohesive Android application featuring an in-process native X11 display server (`libXlorie`), an embedded PRoot user-space virtualization engine, and a hardware-accelerated OpenGL ES surface view.

<div align="center">
  <img src="assets/dashboard-preview.png" width="340" alt="iLynxOS Dashboard on Realme Narzo 50 Pro 5G" style="border-radius: 18px; box-shadow: 0 10px 30px rgba(0,0,0,0.3);" />
  <p><strong>Figure 1:</strong> <em>iLynxOS Dashboard on Realme Narzo 50 Pro 5G</em></p>
  <p><a href="file:///media/smruti/bb847b45-9fb8-4899-b51d-779c6768f27f1/mobile-dev-os/smartphone-movile-os/iLynxOS/assets/dashboard-preview.png">Open High-Resolution Image</a></p>
</div>

---

## 🏗️ System Architecture

```mermaid
graph TD
    subgraph Android Host ["Android 15+ Native Application (com.termux.ilynxos)"]
        UI["Jetpack Compose UI<br/>(Dashboard, Terminal, Settings, About)"]
        Service["LinuxSessionService<br/>(Foreground Service & WakeLock)"]
        LorieView["LorieView (SurfaceHolder)<br/>OpenGL ES Accelerated Display"]
        Server["Embedded X11 Server (libXlorie.so)<br/>Display :0 on UNIX Domain Socket"]
    end

    subgraph Runtime ["PRoot Sandboxing Engine"]
        PRoot["libproot.so (SysV IPC, Fake Root 0:0)"]
        Loader["libloader.so (Syscall Interceptor)"]
        QEMU["libqemu-aarch64.so (Dynamic Translation)"]
    end

    subgraph Guest ["Ubuntu 24.04 LTS (Noble Numbat) Rootfs"]
        DBus["D-Bus Session Daemon"]
        LXQt["LXQt Desktop Environment (startlxqt)"]
        PCManFM["PCManFM-Qt (Desktop & Wallpapers)"]
        Apps["Desktop Apps (VS Code, Firefox, VLC, Store)"]
        Shell["GNU Bash 5.2 Interactive Shell"]
    end

    UI --> Service
    Service --> Server
    Server <--> LorieView
    Service --> PRoot
    PRoot --> Loader
    PRoot --> QEMU
    PRoot --> Guest
    Guest --> DBus
    DBus --> LXQt
    LXQt --> PCManFM
    LXQt --> Apps
    Guest -- "X11 Protocol (:0)" --> Server
```

---

## 🚀 Key Features

1. **Embedded In-Process X11 Server (`libXlorie`)**:
   - Zero-dependency display pipeline: `libXlorie.so` runs directly inside the Android app process.
   - Listens on UNIX domain socket `/data/user/0/com.termux.ilynxos/files/usr/tmp/.X11-unix/X0`.
   - Native hardware-accelerated surface composition via EGL / GLES 2.0 with async surface dispatching.
   - Automatic dynamic connection recovery when navigating between screens.

2. **Embedded PRoot Sandbox**:
   - Executes the pre-installed Ubuntu 24.04 aarch64 rootfs directly in user space without requiring device root.
   - Full support for `--sysvipc`, `--link2symlink`, POSIX shared memory (`/dev/shm`), and root privilege emulation (`--change-id=0:0`).
   - Seamless compatibility bridges for user-provided [install.sh](file:///media/smruti/bb847b45-9fb8-4899-b51d-779c6768f27f1/mobile-dev-os/smartphone-movile-os/iLynxOS/install.sh) and [start.sh](file:///media/smruti/bb847b45-9fb8-4899-b51d-779c6768f27f1/mobile-dev-os/smartphone-movile-os/iLynxOS/start.sh).

3. **LXQt Modern Desktop Environment**:
   - Complete desktop experience with custom wallpaper and pre-configured application shortcuts:
     - `code.desktop` (VS Code)
     - `firefox.desktop` (Firefox Browser)
     - `ilynx-store.desktop` (Software Store)
     - `vlc.desktop` (VLC Media Player)
     - `change-wallpaper.desktop` (Wallpaper Switcher)
   - Real-time touch and mouse pointer tracking.

4. **Floating Fluent Toolbar**:
   - **Keyboard Toggle**: Instantly show or hide the virtual Android on-screen keyboard (IME).
   - **Rotate Screen**: Toggle between portrait and landscape orientations with automatic surface recalibration.
   - **Refresh Display**: Force-redraws the X11 surface to eliminate any transient blank frames.
   - **X11 Mouse Pad Toggle**: Opens/closes the floating onscreen mouse pad with Left/Right/Middle buttons and scroll wheels.
   - **Mouse Mode Selector**: Toggle between *Direct Touch*, *Touchpad Mouse (Relative)*, and *Stylus Pointer*.
   - **Resolution Selector**: Choose between *Native Fit*, *1280x720 (720p HD)*, *1920x1080 (1080p FHD)*, *1024x768 (4:3)*, and *1600x900 (16:9)*.
   - **Fullscreen Toggle**: One-tap immersive mode hiding system navigation and status bars.
   - **Dashboard Return**: Switch back to the dashboard while keeping the Linux session running in the background.
   - **Session Shutdown**: Safe power-down button guarded by a confirmation dialog to prevent accidental session termination.
   - **Scrollable Container**: Responsive horizontal scroll mode guarantees all 10 controls are fully accessible on vertical and horizontal phone screens.

5. **Integrated PRoot Linux Shell**:
   - Built-in terminal emulator executing directly inside the guest rootfs.
   - Quick command chips (`cat /etc/os-release`, `uname -a`, `whoami`, `ls -la /`, `df -h`, `free -m`, `ps aux`, `which startlxqt`).
   - One-tap clipboard copy and history clearing.

6. **Wireless Remote Desktop (Dual Mode)**:
   - **RealVNC / RFB Server**: Multithreaded `x11vnc` with `-noxdamage`, low CPU overhead, and software arrow cursor. Compatible with RealVNC Viewer, TigerVNC, and Remmina.
   - **Web Mode (HTML5 Browser)**: Integrated `websockify` + `noVNC` with fast zlib compression (`compression=1`) and QR code auto-connect for browser access without client installation.

7. **Foreground Service & Lifecycle**:
   - `LinuxSessionService` keeps the desktop environment alive in the background.
   - Persistent partial wake lock prevents Android doze and standby CPU throttling.
   - Ongoing system notification with status and one-tap Stop action.

---

## 📱 Deliverable APK

| Item | Details |
| :--- | :--- |
| **Filename** | [iLynxOS-v1.03-NobleModern.apk](file:///media/smruti/bb847b45-9fb8-4899-b51d-779c6768f27f1/mobile-dev-os/smartphone-movile-os/iLynxOS/iLynxOS-v1.03-NobleModern.apk) |
| **Package Name** | `com.termux.ilynxos` |
| **Target SDK** | Android 14 / 15 (API 34/35) |
| **Min SDK** | Android 8.0 (API 26) |
| **Size** | ~28 MB |
| **Supported ABIs** | `arm64-v8a`, `x86_64` |

---

## 🛠️ Building and Running

### Build Prerequisites
- JDK 17+
- Android SDK (API 34/35, Build Tools 34.0.0+)
- Android NDK (r25c or newer)

### Build Command
```bash
./gradlew assembleDebug
```

The compiled APK will be generated at:
```bash
app/build/outputs/apk/debug/app-debug.apk
```

### Install to Device / Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Android 12+ Child Process Setting (Recommended)
On Android 12, 13, 14, and 15, Android's `ActivityManager` limits spawned child processes to 32 by default. To allow heavy multitasking in Linux desktops, run:
```bash
adb shell "/system/bin/device_config put activity_manager max_phantom_processes 2147483647"
adb shell "setprop persist.sys.fflag.override.settings_enable_monitor_phantom_procs false"
```

---

## 📄 License & Attribution

- **iLynxOS Launcher & UI**: Developed by Smruti Ranjan Sahoo (Apache License 2.0).
- **Embedded X11 Server**: Forked & integrated from Termux:X11 / Lorie (GPL-3.0).
- **PRoot Sandbox**: PRoot Project (GPL-2.0+).
- **Desktop Environment**: LXQt Team (LGPL-2.1+ / GPL-2.0+).
