#!/data/data/com.termux/files/usr/bin/bash
# If not running in Termux, use standard bash
[ ! -x "/data/data/com.termux/files/usr/bin/bash" ] && exec /bin/bash "$0" "$@"
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
pkill -9 -f "Xvnc :$DISP_NUM" >/dev/null 2>&1 || true
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
cat << EOF_XSTART > "$HOME/.vnc/xstartup"
#!/data/data/com.termux/files/usr/bin/bash
termux-wake-lock 2>/dev/null || true
pulseaudio --start --exit-idle-time=-1 >/dev/null 2>&1 || true
pactl load-module module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1 >/dev/null 2>&1 || true

DISP="\${DISPLAY:-:$DISP_NUM}"

proot-distro login iLynxOS --shared-tmp -- bash -c "
    export DISPLAY=\$DISP
    export TMPDIR=/tmp
    export PULSE_SERVER=127.0.0.1
    export XDG_CURRENT_DESKTOP=LXQt
    export XDG_SESSION_TYPE=x11
    export XDG_SESSION_DESKTOP=LXQt
    export DESKTOP_SESSION=lxqt
    export XDG_CONFIG_HOME=/root/.config
    export XDG_DATA_HOME=/root/.local/share
    export LIBGL_ALWAYS_SOFTWARE=1
    export GDK_BACKEND=x11

    rm -rf /var/run/dbus/pid /run/dbus/pid 2>/dev/null || true
    dbus-uuidgen --ensure 2>/dev/null || true
    eval \\\$(dbus-launch --sh-syntax --exit-with-session) 2>/dev/null || true

    WALLPAPER='/usr/share/backgrounds/windows-11-bloom-dark.jpg'
    [ ! -f \"\\\$WALLPAPER\" ] && WALLPAPER='/usr/share/backgrounds/ilynx-wallpaper.png'

    mkdir -p /root/.config/pcmanfm-qt/lxqt /root/Desktop 2>/dev/null || true
    cat << 'EOF_DESK' > /root/.config/pcmanfm-qt/lxqt/desktop-items-0.conf
[General]
wallpaper=\$WALLPAPER
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
network=false
EOF_DESK
    cp -f /root/.config/pcmanfm-qt/lxqt/desktop-items-0.conf /root/.config/pcmanfm-qt/lxqt/desktop-items-1.conf 2>/dev/null || true
    cp -f /root/.config/pcmanfm-qt/lxqt/desktop-items-0.conf /root/.config/pcmanfm-qt/lxqt/desktop-items-default.conf 2>/dev/null || true
    chmod +x /root/Desktop/*.desktop 2>/dev/null || true

    openbox --config-file /root/.config/openbox/rc.xml &
    sleep 0.5
    pcmanfm-qt --desktop --profile=lxqt &
    sleep 0.5
    [ -f /usr/local/bin/snap-daemon ] && /usr/local/bin/snap-daemon &
    sleep 0.4
    lxqt-notificationd &
    lxqt-panel &
    wait
"
EOF_XSTART
else
cat << EOF_XSTART > "$HOME/.vnc/xstartup"
#!/bin/bash
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock 2>/dev/null || true
pulseaudio --start --exit-idle-time=-1 >/dev/null 2>&1 || true
pactl load-module module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1 >/dev/null 2>&1 || true

export DISPLAY="\${DISPLAY:-:$DISP_NUM}"
export TMPDIR=/tmp
export PULSE_SERVER=127.0.0.1
export XDG_CURRENT_DESKTOP=LXQt
export XDG_SESSION_TYPE=x11
export XDG_SESSION_DESKTOP=LXQt
export DESKTOP_SESSION=lxqt
export XDG_CONFIG_HOME=/root/.config
export XDG_DATA_HOME=/root/.local/share
export LIBGL_ALWAYS_SOFTWARE=1
export GDK_BACKEND=x11

rm -rf /var/run/dbus/pid /run/dbus/pid 2>/dev/null || true
dbus-uuidgen --ensure 2>/dev/null || true
eval \$(dbus-launch --sh-syntax --exit-with-session) 2>/dev/null || true

WALLPAPER='/usr/share/backgrounds/windows-11-bloom-dark.jpg'
[ ! -f "\$WALLPAPER" ] && WALLPAPER='/usr/share/backgrounds/ilynx-wallpaper.png'

mkdir -p /root/.config/pcmanfm-qt/lxqt /root/Desktop 2>/dev/null || true
cat << 'EOF_DESK' > /root/.config/pcmanfm-qt/lxqt/desktop-items-0.conf
[General]
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
network=false
EOF_DESK
cp -f /root/.config/pcmanfm-qt/lxqt/desktop-items-0.conf /root/.config/pcmanfm-qt/lxqt/desktop-items-1.conf 2>/dev/null || true
cp -f /root/.config/pcmanfm-qt/lxqt/desktop-items-0.conf /root/.config/pcmanfm-qt/lxqt/desktop-items-default.conf 2>/dev/null || true
chmod +x /root/Desktop/*.desktop 2>/dev/null || true

openbox --config-file /root/.config/openbox/rc.xml &
sleep 0.5
pcmanfm-qt --desktop --profile=lxqt &
sleep 0.5
[ -f /usr/local/bin/snap-daemon ] && /usr/local/bin/snap-daemon &
sleep 0.4
lxqt-notificationd &
lxqt-panel &
wait
EOF_XSTART
fi

chmod +x "$HOME/.vnc/xstartup" 2>/dev/null || true
if [ "$HOME" != "/root" ]; then
    cp -f "$HOME/.vnc/xstartup" /root/.vnc/xstartup 2>/dev/null || true
    chmod +x /root/.vnc/xstartup 2>/dev/null || true
fi

# 8. Launch TigerVNC Server (Xvnc)
if command -v vncserver >/dev/null 2>&1; then
    vncserver ":$DISP_NUM" -geometry "$RESOLUTION" -depth 24 -ac
elif command -v Xvnc >/dev/null 2>&1; then
    Xvnc ":$DISP_NUM" -geometry "$RESOLUTION" -depth 24 -rfbport "$PORT" -rfbauth "$HOME/.vnc/passwd" -ac &
    sleep 1
    DISPLAY=":$DISP_NUM" "$HOME/.vnc/xstartup" &
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
