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

echo "=========================================="
echo " Starting i-Lynx OS VNC Server...        "
echo " Display: :$DISPLAY_NUM | Port: $PORT | Res: $RESOLUTION "
echo "=========================================="

mkdir -p "$HOME/.vnc" /root/.vnc /var/log /var/run /tmp/.X11-unix "$TMPDIR/.X11-unix" 2>/dev/null || true

# 3. Clean up any existing session for this display
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
    # Look for any installed wallpaper
    FOUND_WALLPAPER=$(find /usr/share/backgrounds /usr/share/wallpapers -type f \( -name "*.jpg" -o -name "*.png" \) 2>/dev/null | head -n 1)
    [ -n "$FOUND_WALLPAPER" ] && WALLPAPER="$FOUND_WALLPAPER"
fi

mkdir -p "$HOME/.config/pcmanfm-qt/vnc" "$HOME/.config/pcmanfm-qt/lxqt" \
         /root/.config/pcmanfm-qt/vnc /root/.config/pcmanfm-qt/lxqt 2>/dev/null || true

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
           "/root/.config/pcmanfm-qt/vnc" "/root/.config/pcmanfm-qt/lxqt"; do
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-0.conf" 2>/dev/null || true
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-1.conf" 2>/dev/null || true
    echo "$DESKTOP_CONFIG" > "$dir/desktop-items-default.conf" 2>/dev/null || true
done

# Ensure settings.conf in vnc profile has wallpaper and SingleClick enabled
if [ -f "$HOME/.config/pcmanfm-qt/lxqt/settings.conf" ]; then
    cp -f "$HOME/.config/pcmanfm-qt/lxqt/settings.conf" "$HOME/.config/pcmanfm-qt/vnc/settings.conf" 2>/dev/null || true
    sed -i "s|Wallpaper=.*|Wallpaper=$WALLPAPER|g" "$HOME/.config/pcmanfm-qt/vnc/settings.conf" 2>/dev/null || true
    sed -i "s|WallpaperMode=.*|WallpaperMode=crop|g" "$HOME/.config/pcmanfm-qt/vnc/settings.conf" 2>/dev/null || true
    sed -i "s|BgColor=.*|BgColor=#1e1e2e|g" "$HOME/.config/pcmanfm-qt/vnc/settings.conf" 2>/dev/null || true
fi

# 6. Generate ~/.vnc/xstartup
cat << 'EOF' > "$HOME/.vnc/xstartup"
#!/bin/bash
unset SESSION_MANAGER
unset DBUS_SESSION_BUS_ADDRESS
export DISPLAY=:1
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

# Clean up D-Bus locks and launch session bus
dbus-uuidgen --ensure 2>/dev/null || true
eval $(dbus-launch --sh-syntax --exit-with-session) 2>/dev/null || true

# Set background immediately so screen is never pitch black
WALLPAPER="/usr/share/backgrounds/windows-11-bloom-dark.jpg"
if [ ! -f "$WALLPAPER" ]; then
    FOUND=$(find /usr/share/backgrounds /usr/share/wallpapers -type f \( -name "*.jpg" -o -name "*.png" \) 2>/dev/null | head -n 1)
    [ -n "$FOUND" ] && WALLPAPER="$FOUND"
fi

if command -v feh >/dev/null 2>&1 && [ -f "$WALLPAPER" ]; then
    feh --bg-fill "$WALLPAPER" 2>/dev/null || true
elif command -v hsetroot >/dev/null 2>&1 && [ -f "$WALLPAPER" ]; then
    hsetroot -fill "$WALLPAPER" 2>/dev/null || true
elif command -v xsetroot >/dev/null 2>&1; then
    xsetroot -solid "#1e1e2e" 2>/dev/null || true
fi

# Launch Openbox Window Manager
if command -v openbox >/dev/null 2>&1; then
    if [ -f "$HOME/.config/openbox/rc.xml" ]; then
        openbox --config-file "$HOME/.config/openbox/rc.xml" >/dev/null 2>&1 &
    else
        openbox >/dev/null 2>&1 &
    fi
fi
sleep 0.2

# Launch PCManFM-Qt with dedicated profile 'vnc' to prevent collision with :0
if command -v pcmanfm-qt >/dev/null 2>&1; then
    pcmanfm-qt --desktop --profile=vnc >/dev/null 2>&1 &
fi
sleep 0.2

# Launch LXQt Taskbar Panel
if command -v lxqt-panel >/dev/null 2>&1; then
    lxqt-panel >/dev/null 2>&1 &
elif command -v xfce4-panel >/dev/null 2>&1; then
    xfce4-panel --sm-client-disable --disable-wm-check >/dev/null 2>&1 &
fi

# Ensure wallpaper applied
if command -v pcmanfm-qt >/dev/null 2>&1 && [ -f "$WALLPAPER" ]; then
    (
        sleep 1
        pcmanfm-qt --set-wallpaper="$WALLPAPER" --wallpaper-mode=crop --profile=vnc >/dev/null 2>&1 || true
    ) &
fi

wait
EOF
chmod +x "$HOME/.vnc/xstartup"
[ "$HOME" != "/root" ] && cp -f "$HOME/.vnc/xstartup" /root/.vnc/xstartup 2>/dev/null && chmod +x /root/.vnc/xstartup || true

# 7. Start VNC Server
if [ "$DISPLAY_NUM" != "0" ]; then
    STARTED=0
    # Primary Method: vncserver (Xvnc :1)
    if command -v vncserver >/dev/null 2>&1; then
        echo "[VNC Server] Launching vncserver :$DISPLAY_NUM -geometry $RESOLUTION -depth 24 -ac..."
        vncserver ":$DISPLAY_NUM" -geometry "$RESOLUTION" -depth 24 -ac >/var/log/vncserver.log 2>&1 || true
        sleep 0.5
        if pgrep -f "Xvnc :$DISPLAY_NUM" >/dev/null 2>&1; then
            STARTED=1
        fi
    fi

    # Fallback Method: Xvfb + Openbox/LXQt + x11vnc
    if [ "$STARTED" = "0" ]; then
        echo "[VNC Server] vncserver not running, launching Xvfb + x11vnc fallback..."
        rm -rf "/tmp/.X11-unix/X$DISPLAY_NUM" "/tmp/.X$DISPLAY_NUM-lock" 2>/dev/null || true
        Xvfb ":$DISPLAY_NUM" -screen 0 "${RESOLUTION}x24" -ac +extension XTEST +extension RANDR +extension RENDER -nolisten tcp >/dev/null 2>&1 &
        for i in $(seq 1 30); do
            [ -S "/tmp/.X11-unix/X$DISPLAY_NUM" ] && break
            sleep 0.1
        done

        DISPLAY=":$DISPLAY_NUM" openbox >/dev/null 2>&1 &
        DISPLAY=":$DISPLAY_NUM" pcmanfm-qt --desktop --profile=vnc >/dev/null 2>&1 &
        DISPLAY=":$DISPLAY_NUM" lxqt-panel >/dev/null 2>&1 &

        if command -v feh >/dev/null 2>&1 && [ -f "$WALLPAPER" ]; then
            DISPLAY=":$DISPLAY_NUM" feh --bg-fill "$WALLPAPER" 2>/dev/null || true
        elif command -v xsetroot >/dev/null 2>&1; then
            xsetroot -display ":$DISPLAY_NUM" -solid "#1e1e2e" 2>/dev/null || true
        fi

        AUTH_ARGS="-nopw"
        if [ -f "$HOME/.vnc/passwd" ]; then
            AUTH_ARGS="-rfbauth $HOME/.vnc/passwd"
        fi

        x11vnc -display ":$DISPLAY_NUM" \
            -rfbport "$PORT" \
            -listen 0.0.0.0 \
            $AUTH_ARGS \
            -cursor arrow \
            -forever \
            -shared \
            -repeat \
            -noshm \
            -bg -o /var/log/x11vnc.log 2>&1
    fi
else
    # Mirroring phone display :0
    for i in $(seq 1 30); do
        [ -S "/tmp/.X11-unix/X0" ] && break
        sleep 0.1
    done

    AUTH_ARGS="-nopw"
    if [ -f "$HOME/.vnc/passwd" ]; then
        AUTH_ARGS="-rfbauth $HOME/.vnc/passwd"
    elif [ -n "$PASS" ]; then
        AUTH_ARGS="-passwd $PASS"
    fi

    x11vnc -display ":0" \
        -rfbport "$PORT" \
        -listen 0.0.0.0 \
        $AUTH_ARGS \
        -cursor arrow \
        -forever \
        -shared \
        -repeat \
        -noshm \
        -bg -o /var/log/x11vnc.log 2>&1
fi

# 8. Retrieve IP address and verify socket
DEVICE_IP=$(ip -4 addr show 2>/dev/null | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1)
[ -z "$DEVICE_IP" ] && DEVICE_IP="10.65.94.50"

# Wait up to 3 seconds for port to accept connections
for i in $(seq 1 30); do
    if (exec 3<>/dev/tcp/127.0.0.1/"$PORT") 2>/dev/null; then
        exec 3<&-
        exec 3>&-
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
