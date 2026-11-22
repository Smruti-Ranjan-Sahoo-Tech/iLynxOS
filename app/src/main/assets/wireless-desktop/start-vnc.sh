#!/bin/bash
# If not running in Termux, use standard bash
[ ! -x "/bin/bash" ] && [ -x "/data/data/com.termux/files/usr/bin/bash" ] && exec /data/data/com.termux/files/usr/bin/bash "$0" "$@"
# =======================================================
# i-Lynx OS 1.0 - High Performance Remote VNC Desktop Launcher
# =======================================================

# 1. Prevent Android from sleeping (CPU WakeLock)
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock 2>/dev/null || true

# 2. Parse arguments flexibly
PORT=""
PASSWORD=""
TARGET_DISP=""
RESOLUTION="1280x720"

if [[ "$1" =~ ^[0-9]{4,5}$ ]]; then
    PORT="$1"
    PASSWORD="$2"
    TARGET_DISP="$3"
    [ -n "$4" ] && RESOLUTION="$4"
else
    for arg in "$@"; do
        if [[ "$arg" =~ ^:[0-9]+$ ]]; then
            TARGET_DISP="${arg#:}"
        elif [[ "$arg" =~ ^[0-9]+x[0-9]+$ ]]; then
            RESOLUTION="$arg"
        elif [[ "$arg" =~ ^[0-9]{4,5}$ ]]; then
            PORT="$arg"
        elif [ -z "$PASSWORD" ]; then
            PASSWORD="$arg"
        fi
    done
fi

TMPDIR="${TMPDIR:-/data/data/com.termux/files/usr/tmp}"
[ ! -d "$TMPDIR" ] && TMPDIR="/tmp"
export TMPDIR
mkdir -p "$TMPDIR" /tmp 2>/dev/null || true

# Helper to check if a TCP port is bindable on 0.0.0.0
is_port_free() {
    python3 -c "import socket; s = socket.socket(); s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1); s.bind(('0.0.0.0', $1)); s.close()" 2>/dev/null
}

# 3. Determine Display Number & Port
if [ -n "$TARGET_DISP" ]; then
    DISP_NUM="$TARGET_DISP"
elif [ -n "$PORT" ]; then
    DISP_NUM=$((PORT - 5900))
    [ "$DISP_NUM" -le 0 ] && DISP_NUM=1
else
    DISP_NUM=1
    for d in 1 2 3 4 5; do
        p=$((5900 + d))
        if [ -f "$TMPDIR/.X$d-lock" ] || [ -S "$TMPDIR/.X11-unix/X$d" ] || [ -f "/tmp/.X$d-lock" ] || [ -S "/tmp/.X11-unix/X$d" ]; then
            command -v vncserver >/dev/null 2>&1 && vncserver -kill ":$d" >/dev/null 2>&1 || true
            pkill -9 -f "Xvnc :$d" >/dev/null 2>&1 || true
            rm -rf "$TMPDIR/.X$d-lock" "$TMPDIR/.X11-unix/X$d" "/tmp/.X$d-lock" "/tmp/.X11-unix/X$d" 2>/dev/null || true
        fi
        if is_port_free "$p"; then
            DISP_NUM=$d
            break
        fi
    done
fi

if [ -z "$PORT" ]; then
    PORT=$((5900 + DISP_NUM))
fi

echo "=========================================="
echo " Starting i-Lynx OS TigerVNC Server...   "
echo " Display : :$DISP_NUM | Port : $PORT | Res : $RESOLUTION "
echo "=========================================="

# 4. Clean up any existing session / stale locks for this display
command -v vncserver >/dev/null 2>&1 && vncserver -kill ":$DISP_NUM" >/dev/null 2>&1 || true
command -v tigervncserver >/dev/null 2>&1 && tigervncserver -kill ":$DISP_NUM" >/dev/null 2>&1 || true
pkill -9 -f "Xvnc :$DISP_NUM" >/dev/null 2>&1 || true
pkill -9 -f "Xtigervnc.*:$DISP_NUM" >/dev/null 2>&1 || true
pkill -9 -f "Xvfb :$DISP_NUM" >/dev/null 2>&1 || true
pkill -9 -f "x11vnc.*:$DISP_NUM" 2>/dev/null || true
pkill -9 -f "x11vnc.*-rfbport $PORT" 2>/dev/null || true
rm -rf "$TMPDIR/.X$DISP_NUM-lock" "$TMPDIR/.X11-unix/X$DISP_NUM" 2>/dev/null || true
rm -rf "/tmp/.X$DISP_NUM-lock" "/tmp/.X11-unix/X$DISP_NUM" 2>/dev/null || true
sleep 0.2

# 5. Ensure password is set (default: raja1234)
VNC_PASS="${PASSWORD:-raja1234}"
if [ -z "$VNC_PASS" ] || [[ "$VNC_PASS" == ilynx* ]] || [ "$VNC_PASS" = "ilinux1719" ]; then
    VNC_PASS="raja1234"
fi

mkdir -p "$HOME/.vnc" /root/.vnc 2>/dev/null || true
cat << 'EOF_VNCCONFIG' > "$HOME/.vnc/config"
UseBlacklist=0
BlacklistThreshold=0
BlacklistTimeout=0
localhost=no
EOF_VNCCONFIG
cp -f "$HOME/.vnc/config" /root/.vnc/config 2>/dev/null || true

printf "%s\n%s\nn\n" "$VNC_PASS" "$VNC_PASS" | vncpasswd >/dev/null 2>&1 || true
chmod 600 "$HOME/.vnc/passwd" 2>/dev/null || true
if [ -f "$HOME/.vnc/passwd" ] && [ "$HOME" != "/root" ]; then
    cp -f "$HOME/.vnc/passwd" /root/.vnc/passwd 2>/dev/null || true
    chmod 600 /root/.vnc/passwd 2>/dev/null || true
elif [ -f "/root/.vnc/passwd" ] && [ "$HOME" != "/root" ]; then
    cp -f "/root/.vnc/passwd" "$HOME/.vnc/passwd" 2>/dev/null || true
    chmod 600 "$HOME/.vnc/passwd" 2>/dev/null || true
fi

# 6. Start PulseAudio server in background
pulseaudio --start --exit-idle-time=-1 >/dev/null 2>&1 || true
pactl load-module module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1 >/dev/null 2>&1 || true

# 7. Create/Ensure xstartup script
if command -v proot-distro >/dev/null 2>&1; then
cat << 'EOF_XSTART' > "$HOME/.vnc/xstartup"
#!/data/data/com.termux/files/usr/bin/bash
termux-wake-lock 2>/dev/null || true
pulseaudio --start --exit-idle-time=-1 >/dev/null 2>&1 || true
pactl load-module module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1 >/dev/null 2>&1 || true

export DISPLAY="${DISPLAY:-:1}"

proot-distro login iLynxOS --shared-tmp -- bash -c '
    export DISPLAY="${DISPLAY:-:1}"
    export HOME=/root
    export USER=root
    export TMPDIR=/tmp
    export PULSE_SERVER=127.0.0.1
    export XDG_CURRENT_DESKTOP=LXQt
    export XDG_SESSION_TYPE=x11
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
    export XDG_CONFIG_HOME=/root/.config
    export XDG_DATA_HOME=/root/.local/share
    export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH

    rm -rf /var/run/dbus/pid /run/dbus/pid 2>/dev/null || true
    dbus-uuidgen --ensure 2>/dev/null || true
    eval $(dbus-launch --sh-syntax --exit-with-session) 2>/dev/null || true

    WALLPAPER=""
    for wp in \
        "/usr/share/backgrounds/windows-11-bloom-dark.jpg" \
        "/usr/share/backgrounds/ilynx-wallpaper.png" \
        "/usr/share/backgrounds/dark-abstract-flow.jpg" \
        "/usr/share/backgrounds/honda-ev-cyberpunk.jpg" \
        "/usr/share/backgrounds/windows-11-bloom-light.jpg" \
        "/usr/share/backgrounds/xfce/xfce-blue.jpg"; do
        if [ -f "$wp" ]; then
            WALLPAPER="$wp"
            break
        fi
    done
    if [ -z "$WALLPAPER" ]; then
        WALLPAPER=$(find /usr/share/backgrounds /usr/share/wallpapers -type f \( -name "*.jpg" -o -name "*.png" \) 2>/dev/null | head -n 1)
    fi

    # Configure PCManFM-Qt config files
    for d in /root/.config/pcmanfm-qt/lxqt /root/.config/pcmanfm-qt/vnc /root/.config/pcmanfm-qt/default /etc/xdg/pcmanfm-qt/lxqt /etc/xdg/pcmanfm-qt/default; do
        mkdir -p "$d" 2>/dev/null || true
        cat << SETEOF > "$d/settings.conf"
[Desktop]
Wallpaper=$WALLPAPER
WallpaperMode=zoom
PerScreenWallpaper=false
TransformWallpaper=true
DesktopIconSize=48
WorkAreaMargins=12, 12, 12, 12
BgColor=#1e1e2e
FgColor=#ffffff

[Behavior]
SingleClick=true
ConfirmDelete=true
QuickExec=true

[FolderView]
Mode=icon
SETEOF
        for c in desktop-items-0.conf desktop-items-1.conf desktop-items-VNC-0.conf desktop-items-VNC-1.conf desktop-items-screen.conf desktop-items-screen0.conf desktop-items-Screen0.conf desktop-items-SCREEN.conf desktop-items-default.conf desktop-items-None.conf desktop-items-VIRTUAL1.conf desktop-items.conf; do
            cat << CONFEOF > "$d/$c"
[General]
wallpaper=$WALLPAPER
wallpaper_mode=zoom
show_wm_menu=false
sort_type=0
sort_by=0
CONFEOF
        done
    done

    # Pre-generate PCManFM-Qt wallpaper cache so desktop never displays black
    if [ -n "$WALLPAPER" ] && [ -f "$WALLPAPER" ]; then
        for cache_dir in /root/.cache/pcmanfm-qt/lxqt /root/.cache/pcmanfm-qt/vnc /root/.cache/pcmanfm-qt/default; do
            mkdir -p "$cache_dir" 2>/dev/null || true
            if command -v convert >/dev/null 2>&1; then
                convert "$WALLPAPER" -resize 1280x720^ -gravity center -extent 1280x720 "$cache_dir/wallpaper.cache" 2>/dev/null || cp -f "$WALLPAPER" "$cache_dir/wallpaper.cache" 2>/dev/null || true
            else
                cp -f "$WALLPAPER" "$cache_dir/wallpaper.cache" 2>/dev/null || true
            fi
            echo "$WALLPAPER" > "$cache_dir/wallpaper.cache.info" 2>/dev/null || true
            date +%Y-%m-%d-%H:%M:%S.%3N >> "$cache_dir/wallpaper.cache.info" 2>/dev/null || true
        done

        # Render wallpaper to root window
        if command -v display >/dev/null 2>&1; then
            display -window root -backdrop "$WALLPAPER" >/dev/null 2>&1 &
        elif command -v feh >/dev/null 2>&1; then
            feh --bg-fill "$WALLPAPER" >/dev/null 2>&1 || true
        fi
    fi
    xsetroot -cursor_name left_ptr -solid "#1e1e2e" 2>/dev/null || true

    chmod +x /root/Desktop/*.desktop 2>/dev/null || true

    openbox --config-file /root/.config/openbox/rc.xml &
    sleep 0.4
    pcmanfm-qt --desktop --profile=lxqt &
    sleep 0.4
    [ -f /usr/local/bin/snap-daemon ] && /usr/local/bin/snap-daemon &
    sleep 0.3
    lxqt-notificationd &
    lxqt-panel &
    (
        sleep 1
        if [ -x /usr/local/bin/set-wallpaper ] && [ -n "$WALLPAPER" ]; then
            /usr/local/bin/set-wallpaper "$WALLPAPER" zoom >/dev/null 2>&1 || true
        fi
        pcmanfm-qt --profile=lxqt -w "$WALLPAPER" --wallpaper-mode=zoom >/dev/null 2>&1 || true
        pcmanfm-qt -w "$WALLPAPER" --wallpaper-mode=zoom >/dev/null 2>&1 || true
    ) &
    wait
'
EOF_XSTART
else
cat << 'EOF_XSTART' > "$HOME/.vnc/xstartup"
#!/bin/bash
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock 2>/dev/null || true
pulseaudio --start --exit-idle-time=-1 >/dev/null 2>&1 || true
pactl load-module module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1 >/dev/null 2>&1 || true

export DISPLAY="${DISPLAY:-:1}"
export HOME=/root
export USER=root
export TMPDIR=/tmp
export PULSE_SERVER=127.0.0.1
export XDG_CURRENT_DESKTOP=LXQt
export XDG_SESSION_TYPE=x11
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
export XDG_CONFIG_HOME=/root/.config
export XDG_DATA_HOME=/root/.local/share
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH

rm -rf /var/run/dbus/pid /run/dbus/pid 2>/dev/null || true
dbus-uuidgen --ensure 2>/dev/null || true
eval $(dbus-launch --sh-syntax --exit-with-session) 2>/dev/null || true

WALLPAPER=""
for wp in \
    "/usr/share/backgrounds/windows-11-bloom-dark.jpg" \
    "/usr/share/backgrounds/ilynx-wallpaper.png" \
    "/usr/share/backgrounds/dark-abstract-flow.jpg" \
    "/usr/share/backgrounds/honda-ev-cyberpunk.jpg" \
    "/usr/share/backgrounds/windows-11-bloom-light.jpg" \
    "/usr/share/backgrounds/xfce/xfce-blue.jpg"; do
    if [ -f "$wp" ]; then
        WALLPAPER="$wp"
        break
    fi
done
if [ -z "$WALLPAPER" ]; then
    WALLPAPER=$(find /usr/share/backgrounds /usr/share/wallpapers -type f \( -name "*.jpg" -o -name "*.png" \) 2>/dev/null | head -n 1)
fi

for d in /root/.config/pcmanfm-qt/lxqt /root/.config/pcmanfm-qt/vnc /root/.config/pcmanfm-qt/default /etc/xdg/pcmanfm-qt/lxqt /etc/xdg/pcmanfm-qt/default; do
    mkdir -p "$d" 2>/dev/null || true
    cat << SETEOF > "$d/settings.conf"
[Desktop]
Wallpaper=$WALLPAPER
WallpaperMode=zoom
PerScreenWallpaper=false
TransformWallpaper=true
DesktopIconSize=48
WorkAreaMargins=12, 12, 12, 12
BgColor=#1e1e2e
FgColor=#ffffff

[Behavior]
SingleClick=true
ConfirmDelete=true
QuickExec=true

[FolderView]
Mode=icon
SETEOF
    for c in desktop-items-0.conf desktop-items-1.conf desktop-items-VNC-0.conf desktop-items-VNC-1.conf desktop-items-screen.conf desktop-items-screen0.conf desktop-items-Screen0.conf desktop-items-SCREEN.conf desktop-items-default.conf desktop-items-None.conf desktop-items-VIRTUAL1.conf desktop-items.conf; do
        cat << CONFEOF > "$d/$c"
[General]
wallpaper=$WALLPAPER
wallpaper_mode=zoom
show_wm_menu=false
sort_type=0
sort_by=0
CONFEOF
    done
done

if [ -n "$WALLPAPER" ] && [ -f "$WALLPAPER" ]; then
    for cache_dir in /root/.cache/pcmanfm-qt/lxqt /root/.cache/pcmanfm-qt/vnc /root/.cache/pcmanfm-qt/default; do
        mkdir -p "$cache_dir" 2>/dev/null || true
        if command -v convert >/dev/null 2>&1; then
            convert "$WALLPAPER" -resize 1280x720^ -gravity center -extent 1280x720 "$cache_dir/wallpaper.cache" 2>/dev/null || cp -f "$WALLPAPER" "$cache_dir/wallpaper.cache" 2>/dev/null || true
        else
            cp -f "$WALLPAPER" "$cache_dir/wallpaper.cache" 2>/dev/null || true
        fi
        echo "$WALLPAPER" > "$cache_dir/wallpaper.cache.info" 2>/dev/null || true
        date +%Y-%m-%d-%H:%M:%S.%3N >> "$cache_dir/wallpaper.cache.info" 2>/dev/null || true
    done

    if command -v display >/dev/null 2>&1; then
        display -window root -backdrop "$WALLPAPER" >/dev/null 2>&1 &
    elif command -v feh >/dev/null 2>&1; then
        feh --bg-fill "$WALLPAPER" >/dev/null 2>&1 || true
    fi
fi
xsetroot -cursor_name left_ptr -solid "#1e1e2e" 2>/dev/null || true

chmod +x /root/Desktop/*.desktop 2>/dev/null || true

openbox --config-file /root/.config/openbox/rc.xml &
sleep 0.4
pcmanfm-qt --desktop --profile=lxqt &
sleep 0.4
[ -f /usr/local/bin/snap-daemon ] && /usr/local/bin/snap-daemon &
sleep 0.3
lxqt-notificationd &
lxqt-panel &
(
    sleep 1
    if [ -x /usr/local/bin/set-wallpaper ] && [ -n "$WALLPAPER" ]; then
        /usr/local/bin/set-wallpaper "$WALLPAPER" zoom >/dev/null 2>&1 || true
    fi
    pcmanfm-qt --profile=lxqt -w "$WALLPAPER" --wallpaper-mode=zoom >/dev/null 2>&1 || true
    pcmanfm-qt -w "$WALLPAPER" --wallpaper-mode=zoom >/dev/null 2>&1 || true
) &
wait
EOF_XSTART
fi

chmod +x "$HOME/.vnc/xstartup" 2>/dev/null || true
if [ "$HOME" != "/root" ]; then
    cp -f "$HOME/.vnc/xstartup" /root/.vnc/xstartup 2>/dev/null || true
    chmod +x /root/.vnc/xstartup 2>/dev/null || true
fi

# 8. Launch VNC Server
EXTRA_VNC_OPTS="-UseBlacklist=0 -BlacklistThreshold=0 -BlacklistTimeout=0 -localhost no"

if [ "$DISP_NUM" = "0" ]; then
    echo "[VNC] Attaching x11vnc to active display :0 on port $PORT..."
    x11vnc -display :0 -rfbport "$PORT" -rfbauth "$HOME/.vnc/passwd" -shared -forever -bg -noxdamage -repeat >/dev/null 2>&1
elif command -v vncserver >/dev/null 2>&1; then
    vncserver ":$DISP_NUM" -geometry "$RESOLUTION" -depth 24 -ac $EXTRA_VNC_OPTS
elif command -v tigervncserver >/dev/null 2>&1; then
    tigervncserver ":$DISP_NUM" -geometry "$RESOLUTION" -depth 24 -ac $EXTRA_VNC_OPTS
elif command -v Xvnc >/dev/null 2>&1; then
    Xvnc ":$DISP_NUM" -geometry "$RESOLUTION" -depth 24 -rfbport "$PORT" -rfbauth "$HOME/.vnc/passwd" -ac $EXTRA_VNC_OPTS &
    sleep 1
    DISPLAY=":$DISP_NUM" "$HOME/.vnc/xstartup" &
elif command -v Xtigervnc >/dev/null 2>&1; then
    Xtigervnc ":$DISP_NUM" -geometry "$RESOLUTION" -depth 24 -rfbport "$PORT" -rfbauth "$HOME/.vnc/passwd" -ac $EXTRA_VNC_OPTS &
    sleep 1
    DISPLAY=":$DISP_NUM" "$HOME/.vnc/xstartup" &
elif command -v Xvfb >/dev/null 2>&1 && command -v x11vnc >/dev/null 2>&1; then
    echo "[VNC] Launching virtual Xvfb + x11vnc fallback..."
    Xvfb ":$DISP_NUM" -screen 0 "${RESOLUTION}x24" >/dev/null 2>&1 &
    sleep 0.5
    DISPLAY=":$DISP_NUM" "$HOME/.vnc/xstartup" &
    sleep 0.5
    x11vnc -display ":$DISP_NUM" -rfbport "$PORT" -rfbauth "$HOME/.vnc/passwd" -shared -forever -bg >/dev/null 2>&1
fi

# 9. Retrieve active IP address reliably
DEVICE_IP=$(ifconfig 2>/dev/null | awk '/inet / && $2 !~ /^127\./ {print $2}' | head -n 1)
[ -z "$DEVICE_IP" ] && DEVICE_IP=$(ip -4 addr show 2>/dev/null | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1)
[ -z "$DEVICE_IP" ] && DEVICE_IP=$(python3 -c "import socket; s=socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.connect(('8.8.8.8', 80)); print(s.getsockname()[0]); s.close()" 2>/dev/null)
[ -z "$DEVICE_IP" ] && DEVICE_IP="10.160.148.192"

echo ""
echo "======================================================"
echo "  🚀 i-Lynx OS TigerVNC Server is RUNNING!           "
echo "======================================================"
echo "  Connect from your PC using any VNC Viewer:          "
echo "                                                      "
echo "  🖥️  VNC Address : ${DEVICE_IP}:${PORT} (or ${DEVICE_IP}:${DISP_NUM})"
echo "  🔑  Password    : ${VNC_PASS}                       "
echo "  📐  Resolution  : ${RESOLUTION}                     "
echo "  📺  Display     : :${DISP_NUM}                      "
echo "                                                      "
echo "  Recommended PC Viewers:                             "
echo "   - TigerVNC Viewer (vncviewer ${DEVICE_IP}:${PORT}) "
echo "   - RealVNC Viewer                                   "
echo "   - Remmina (Linux)                                  "
echo "                                                      "
echo "  To stop the VNC session:                            "
echo "   run 'vncstop' or 'vncstop :${DISP_NUM}'            "
echo "======================================================"
