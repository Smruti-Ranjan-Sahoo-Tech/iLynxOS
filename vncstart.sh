#!/data/data/com.termux/files/usr/bin/bash
# =======================================================
# i-Lynx OS 1.0 - Remote VNC Desktop Launcher for PC View
# =======================================================

# 1. Prevent Android from sleeping
termux-wake-lock 2>/dev/null || true

# 2. Parse resolution (default: 1280x720)
RESOLUTION="${1:-1280x720}"

echo "=========================================="
echo " Starting i-Lynx OS VNC Server for PC... "
echo "=========================================="

# 3. Clean up any existing :1 VNC session
vncserver -kill :1 >/dev/null 2>&1 || true
pkill -9 -f "Xvnc :1" >/dev/null 2>&1 || true
pkill -9 -f "pcmanfm-qt.*vnc" >/dev/null 2>&1 || true
rm -rf "$TMPDIR/.X11-unix/X1" "$TMPDIR/.X1-lock" /tmp/.X11-unix/X1 /tmp/.X1-lock 2>/dev/null || true

# 4. Ensure password is set (default: raja1234)
mkdir -p "$HOME/.vnc" /root/.vnc 2>/dev/null || true
if [ ! -f "$HOME/.vnc/passwd" ] && [ ! -f "/root/.vnc/passwd" ]; then
    printf "raja1234\nraja1234\nn\n" | vncpasswd >/dev/null 2>&1
    chmod 600 "$HOME/.vnc/passwd" 2>/dev/null || true
fi

# 5. Pre-configure Desktop Wallpaper & Desktop Items so screen is never black
WALLPAPER="/usr/share/backgrounds/windows-11-bloom-dark.jpg"
if [ ! -f "$WALLPAPER" ]; then
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

# Ensure settings.conf in vnc profile has wallpaper
if [ -f "$HOME/.config/pcmanfm-qt/lxqt/settings.conf" ]; then
    cp -f "$HOME/.config/pcmanfm-qt/lxqt/settings.conf" "$HOME/.config/pcmanfm-qt/vnc/settings.conf" 2>/dev/null || true
    sed -i "s|Wallpaper=.*|Wallpaper=$WALLPAPER|g" "$HOME/.config/pcmanfm-qt/vnc/settings.conf" 2>/dev/null || true
    sed -i "s|WallpaperMode=.*|WallpaperMode=crop|g" "$HOME/.config/pcmanfm-qt/vnc/settings.conf" 2>/dev/null || true
    sed -i "s|BgColor=.*|BgColor=#1e1e2e|g" "$HOME/.config/pcmanfm-qt/vnc/settings.conf" 2>/dev/null || true
fi

# Ensure ~/.vnc/xstartup exists
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

dbus-uuidgen --ensure 2>/dev/null || true
eval $(dbus-launch --sh-syntax --exit-with-session) 2>/dev/null || true

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

if command -v openbox >/dev/null 2>&1; then
    if [ -f "$HOME/.config/openbox/rc.xml" ]; then
        openbox --config-file "$HOME/.config/openbox/rc.xml" >/dev/null 2>&1 &
    else
        openbox >/dev/null 2>&1 &
    fi
fi
sleep 0.2

if command -v pcmanfm-qt >/dev/null 2>&1; then
    pcmanfm-qt --desktop --profile=vnc >/dev/null 2>&1 &
fi
sleep 0.2

if command -v lxqt-panel >/dev/null 2>&1; then
    lxqt-panel >/dev/null 2>&1 &
elif command -v xfce4-panel >/dev/null 2>&1; then
    xfce4-panel --sm-client-disable --disable-wm-check >/dev/null 2>&1 &
fi

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

# 6. Start VNC Server
vncserver :1 -geometry "$RESOLUTION" -depth 24 -ac

# 7. Retrieve IP address
DEVICE_IP=$(ip -4 addr show 2>/dev/null | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1)
[ -z "$DEVICE_IP" ] && DEVICE_IP="10.65.94.50"

echo ""
echo "======================================================"
echo "  🚀 i-Lynx OS VNC Server is RUNNING!                "
echo "======================================================"
echo "  Connect from your PC using any VNC Viewer:          "
echo "                                                      "
echo "  🖥️  VNC Address : ${DEVICE_IP}:5901 (or ${DEVICE_IP}:1)"
echo "  🔑  Password    : raja1234                          "
echo "  📐  Resolution  : ${RESOLUTION}                     "
echo "                                                      "
echo "  Recommended PC Viewers:                             "
echo "   - TigerVNC Viewer (vncviewer ${DEVICE_IP}:5901)   "
echo "   - RealVNC Viewer                                   "
echo "   - Remmina (Linux)                                  "
echo "                                                      "
echo "  To stop the VNC session:                            "
echo "   run 'vncstop' or '~/vnc-stop.sh'                   "
echo "======================================================"
