#!/bin/bash
# =======================================================
# i-Lynx OS 1.0 - Remote VNC Desktop Launcher for PC View
# =======================================================

# 1. Prevent Android from sleeping
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock 2>/dev/null || true

# 2. Parse arguments (defaults: port 5901, display :1, resolution 1280x720)
PORT="${1:-5901}"
PASSWORD="$2"
DISPLAY_NUM="${3:-1}"
RESOLUTION="${4:-1280x720}"

TMPDIR="${TMPDIR:-/tmp}"
export TMPDIR

# Ensure lock so parallel calls don't race and kill each other
LOCKFILE="/tmp/start-vnc-${DISPLAY_NUM}.lock"
exec 200>"$LOCKFILE" 2>/dev/null || true
if command -v flock >/dev/null 2>&1; then
    flock -n 200 || { echo "VNC server launcher already running for display :$DISPLAY_NUM"; exit 0; }
fi

echo "=========================================="
echo " Starting i-Lynx OS VNC Server...        "
echo " Display: :$DISPLAY_NUM | Port: $PORT | Res: $RESOLUTION "
echo "=========================================="

mkdir -p "$HOME/.vnc" /root/.vnc /var/log /var/run /tmp/.X11-unix "$TMPDIR/.X11-unix" 2>/dev/null || true

# 3. Clean up any existing session for this display if restarting
if [ "$DISPLAY_NUM" != "0" ]; then
    command -v vncserver >/dev/null 2>&1 && vncserver -kill ":$DISPLAY_NUM" >/dev/null 2>&1 || true
    pkill -9 -f "Xvnc :$DISPLAY_NUM" >/dev/null 2>&1 || true
    pkill -9 -f "Xvfb :$DISPLAY_NUM" >/dev/null 2>&1 || true
    pkill -9 -f "pcmanfm-qt.*vnc" 2>/dev/null || true
    rm -rf "/tmp/.X11-unix/X$DISPLAY_NUM" "/tmp/.X$DISPLAY_NUM-lock" \
           "$TMPDIR/.X11-unix/X$DISPLAY_NUM" "$TMPDIR/.X$DISPLAY_NUM-lock" 2>/dev/null || true
fi
pkill -9 -f "x11vnc.*:$DISPLAY_NUM" 2>/dev/null || true
pkill -9 -f "x11vnc.*-rfbport $PORT" 2>/dev/null || true
sleep 0.2

# 4. Ensure password is set (default: raja1234)
PASS="${PASSWORD:-raja1234}"
if [ -z "$PASS" ] || [[ "$PASS" == ilynx* ]]; then
    PASS="raja1234"
fi

if [ -n "$PASS" ]; then
    printf "%s\n%s\nn\n" "$PASS" "$PASS" | vncpasswd >/dev/null 2>&1 || true
    if command -v x11vnc >/dev/null 2>&1; then
        x11vnc -storepasswd "$PASS" "$HOME/.vnc/passwd" 2>/dev/null || true
    fi
    [ -f "$HOME/.vnc/passwd" ] && cp -f "$HOME/.vnc/passwd" /root/.vnc/passwd 2>/dev/null || true
    [ -f "/root/.vnc/passwd" ] && cp -f "/root/.vnc/passwd" "$HOME/.vnc/passwd" 2>/dev/null || true
    chmod 600 "$HOME/.vnc/passwd" /root/.vnc/passwd 2>/dev/null || true
fi

# 5. Pre-configure Desktop Wallpaper & Desktop Items so screen is never black
WALLPAPER="/usr/share/backgrounds/windows-11-bloom-dark.jpg"
if [ ! -f "$WALLPAPER" ]; then
    [ -f "/usr/share/backgrounds/ilynx-wallpaper.png" ] && WALLPAPER="/usr/share/backgrounds/ilynx-wallpaper.png"
    if [ ! -f "$WALLPAPER" ]; then
        FOUND_WALLPAPER=$(find /usr/share/backgrounds /usr/share/wallpapers -type f \( -name "*.jpg" -o -name "*.png" \) 2>/dev/null | head -n 1)
        [ -n "$FOUND_WALLPAPER" ] && WALLPAPER="$FOUND_WALLPAPER"
    fi
fi

# Ensure Desktop directory exists with default desktop apps
mkdir -p "$HOME/Desktop" /root/Desktop 2>/dev/null || true
mkdir -p "$HOME/.config" /root/.config 2>/dev/null || true

# Setup standard user-dirs
cat << 'EOF' > /root/.config/user-dirs.dirs 2>/dev/null || true
XDG_DESKTOP_DIR="$HOME/Desktop"
XDG_DOWNLOAD_DIR="$HOME/Downloads"
XDG_TEMPLATES_DIR="$HOME/Templates"
XDG_PUBLICSHARE_DIR="$HOME/Public"
XDG_DOCUMENTS_DIR="$HOME/Documents"
XDG_MUSIC_DIR="$HOME/Music"
XDG_PICTURES_DIR="$HOME/Pictures"
XDG_VIDEOS_DIR="$HOME/Videos"
EOF

# Ensure desktop shortcuts are executable
chmod +x /root/Desktop/*.desktop "$HOME/Desktop"/*.desktop 2>/dev/null || true

# Pre-configure pcmanfm-qt desktop items for both 'lxqt' and 'vnc' profiles across all screen IDs
DESKTOP_CONFIG="[General]
wallpaper=$WALLPAPER
wallpaper_mode=crop
show_wm_menu=false
sort_type=0
sort_by=0
shadow_hidden=false
show_hidden=false

[Folder]
home=true
trash=true
computer=true
network=false"

for dir in "$HOME/.config/pcmanfm-qt/vnc" "$HOME/.config/pcmanfm-qt/lxqt" \
           "$HOME/.config/pcmanfm-qt/default" "/etc/xdg/pcmanfm-qt/default" \
           "/etc/xdg/pcmanfm-qt/lxqt" "/root/.config/pcmanfm-qt/vnc" \
           "/root/.config/pcmanfm-qt/lxqt" "/root/.config/pcmanfm-qt/default"; do
    mkdir -p "$dir" 2>/dev/null || true
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-0.conf" 2>/dev/null || true
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-1.conf" 2>/dev/null || true
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-default.conf" 2>/dev/null || true
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-screen.conf" 2>/dev/null || true
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-screen0.conf" 2>/dev/null || true
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-VIRTUAL1.conf" 2>/dev/null || true
done

# Ensure settings.conf in vnc profile has wallpaper and SingleClick enabled
for dir in "$HOME/.config/pcmanfm-qt/vnc" "/root/.config/pcmanfm-qt/vnc"; do
    if [ -f "$HOME/.config/pcmanfm-qt/lxqt/settings.conf" ]; then
        cp -f "$HOME/.config/pcmanfm-qt/lxqt/settings.conf" "$dir/settings.conf" 2>/dev/null || true
    fi
    if [ -f "$dir/settings.conf" ]; then
        sed -i "s|Wallpaper=.*|Wallpaper=$WALLPAPER|g" "$dir/settings.conf" 2>/dev/null || true
        sed -i "s|WallpaperMode=.*|WallpaperMode=crop|g" "$dir/settings.conf" 2>/dev/null || true
        sed -i "s|BgColor=.*|BgColor=#1e1e2e|g" "$dir/settings.conf" 2>/dev/null || true
        sed -i "s|SingleClick=false|SingleClick=true|g" "$dir/settings.conf" 2>/dev/null || true
    fi
done

# 6. Start Desktop Session & VNC Server
if [ "$DISPLAY_NUM" != "0" ]; then
    echo "[VNC Server] Launching Xvfb virtual display :$DISPLAY_NUM (${RESOLUTION}x24)..."
    rm -rf "/tmp/.X11-unix/X$DISPLAY_NUM" "/tmp/.X$DISPLAY_NUM-lock" \
           "$TMPDIR/.X11-unix/X$DISPLAY_NUM" "$TMPDIR/.X$DISPLAY_NUM-lock" 2>/dev/null || true

    Xvfb ":$DISPLAY_NUM" -screen 0 "${RESOLUTION}x24" -ac +extension XTEST +extension RANDR +extension RENDER -nolisten tcp >/dev/null 2>&1 &

    # Wait for X11 socket
    for i in $(seq 1 40); do
        if [ -S "/tmp/.X11-unix/X$DISPLAY_NUM" ] || [ -S "$TMPDIR/.X11-unix/X$DISPLAY_NUM" ]; then
            break
        fi
        sleep 0.1
    done

    # Set initial background and standard arrow cursor for PC VNC viewers
    if command -v xsetroot >/dev/null 2>&1; then
        xsetroot -display ":$DISPLAY_NUM" -solid "#1e1e2e" -cursor_name left_ptr 2>/dev/null || true
    fi

    # 7. Start isolated desktop environment for :$DISPLAY_NUM
    (
        export DISPLAY=":$DISPLAY_NUM"
        export HOME=/root
        export USER=root
        export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH
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
        export XDG_DATA_DIRS=/usr/local/share:/usr/share:/var/lib/snapd/desktop
        export XDG_CONFIG_DIRS=/etc/xdg
        export XDG_DESKTOP_DIR="/root/Desktop"

        # ISOLATE D-BUS: decouple from Display :0 so PCManFM-Qt does not collide or exit
        unset DBUS_SESSION_BUS_ADDRESS
        unset SESSION_MANAGER
        dbus-uuidgen --ensure 2>/dev/null || true
        eval $(dbus-launch --sh-syntax --exit-with-session) 2>/dev/null || true

        # Set cursor and background
        xsetroot -display ":$DISPLAY_NUM" -solid "#1e1e2e" -cursor_name left_ptr 2>/dev/null || true

        # Start Openbox Window Manager
        if command -v openbox >/dev/null 2>&1; then
            if [ -f "/root/.config/openbox/rc.xml" ]; then
                openbox --config-file "/root/.config/openbox/rc.xml" >/dev/null 2>&1 &
            else
                openbox >/dev/null 2>&1 &
            fi
        fi
        sleep 0.2

        # Start PCManFM-Qt for Desktop (renders wallpaper and ~/Desktop icons)
        if command -v pcmanfm-qt >/dev/null 2>&1; then
            pcmanfm-qt --desktop --profile=vnc >/dev/null 2>&1 &
        fi
        sleep 0.2

        # Start Taskbar Panel
        if command -v lxqt-panel >/dev/null 2>&1; then
            lxqt-panel >/dev/null 2>&1 &
        elif command -v xfce4-panel >/dev/null 2>&1; then
            xfce4-panel --sm-client-disable --disable-wm-check >/dev/null 2>&1 &
        fi

        # Apply wallpaper explicitly
        if command -v pcmanfm-qt >/dev/null 2>&1 && [ -f "$WALLPAPER" ]; then
            sleep 1
            pcmanfm-qt --set-wallpaper="$WALLPAPER" --wallpaper-mode=crop --profile=vnc >/dev/null 2>&1 || true
        fi
    ) &

    # 8. Start x11vnc with optimal options for PC VNC viewers & Mobile VNC
    AUTH_ARGS="-nopw"
    if [ -f "$HOME/.vnc/passwd" ]; then
        AUTH_ARGS="-rfbauth $HOME/.vnc/passwd"
    elif [ -f "/root/.vnc/passwd" ]; then
        AUTH_ARGS="-rfbauth /root/.vnc/passwd"
    elif [ -n "$PASS" ]; then
        AUTH_ARGS="-passwd $PASS"
    fi

    echo "[VNC Server] Launching x11vnc on 0.0.0.0:$PORT (display :$DISPLAY_NUM)..."
    x11vnc -display ":$DISPLAY_NUM" \
        -rfbport "$PORT" \
        -listen 0.0.0.0 \
        $AUTH_ARGS \
        -cursor arrow \
        -always_inject \
        -noxdamage \
        -nowf \
        -repeat \
        -shared \
        -forever \
        -noshm \
        -bg -o /var/log/x11vnc.log 2>&1
else
    # Mirror mode (Display :0)
    # Ensure root cursor is set
    xsetroot -display ":0" -cursor_name left_ptr 2>/dev/null || true

    AUTH_ARGS="-nopw"
    if [ -f "$HOME/.vnc/passwd" ]; then
        AUTH_ARGS="-rfbauth $HOME/.vnc/passwd"
    elif [ -f "/root/.vnc/passwd" ]; then
        AUTH_ARGS="-rfbauth /root/.vnc/passwd"
    elif [ -n "$PASS" ]; then
        AUTH_ARGS="-passwd $PASS"
    fi

    echo "[VNC Server] Launching x11vnc on 0.0.0.0:$PORT (mirroring :0)..."
    x11vnc -display ":0" \
        -rfbport "$PORT" \
        -listen 0.0.0.0 \
        $AUTH_ARGS \
        -cursor arrow \
        -always_inject \
        -noxdamage \
        -nowf \
        -repeat \
        -shared \
        -forever \
        -noshm \
        -bg -o /var/log/x11vnc.log 2>&1
fi

# 9. Retrieve IP address and verify socket
DEVICE_IP=$(ip -4 addr show 2>/dev/null | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1)
[ -z "$DEVICE_IP" ] && DEVICE_IP="10.65.94.50"

# Wait up to 3 seconds for port to accept connections
for i in $(seq 1 30); do
    if python3 -c "import socket; s = socket.socket(); s.settimeout(0.2); s.connect(('127.0.0.1', $PORT)); s.close()" 2>/dev/null; then
        break
    fi
    sleep 0.1
done

echo ""
echo "======================================================"
echo "  🚀 i-Lynx OS VNC Server is RUNNING!                "
echo "======================================================"
echo "  Connect from your PC using any VNC Viewer:          "
echo "                                                      "
echo "  🖥️  VNC Address : ${DEVICE_IP}:${PORT} (or ${DEVICE_IP}:${DISPLAY_NUM})"
echo "  🔑  Password    : ${PASS}                           "
echo "  📐  Resolution  : ${RESOLUTION}                     "
echo "                                                      "
echo "  Recommended PC Viewers:                             "
echo "   - TigerVNC Viewer (vncviewer ${DEVICE_IP}:${PORT}) "
echo "   - RealVNC Viewer                                   "
echo "   - Remmina (Linux)                                  "
echo "                                                      "
echo "  To stop the VNC session:                            "
echo "   run 'vncstop' or '~/vnc-stop.sh'                   "
echo "======================================================"
exit 0
