#!/data/data/com.termux/files/usr/bin/bash
# =======================================================
# i-Lynx OS 1.0 - Remote VNC Desktop Launcher for PC View
# =======================================================

# 1. Prevent Android from sleeping
termux-wake-lock

# 2. Parse resolution (default: 1280x720)
RESOLUTION="${1:-1280x720}"

echo "=========================================="
echo " Starting i-Lynx OS VNC Server for PC... "
echo "=========================================="

# 3. Clean up any existing :1 VNC session
vncserver -kill :1 >/dev/null 2>&1 || true
pkill -9 -f "Xvnc :1" >/dev/null 2>&1 || true
rm -rf "$TMPDIR/.X11-unix/X1" "$TMPDIR/.X1-lock" 2>/dev/null

# 4. Ensure password is set (default: raja1234)
if [ ! -f "$HOME/.vnc/passwd" ]; then
    mkdir -p "$HOME/.vnc"
    printf "raja1234\nraja1234\nn\n" | vncpasswd >/dev/null 2>&1
fi

# 5. Start VNC Server
vncserver :1 -geometry "$RESOLUTION" -depth 24 -ac

# 6. Retrieve IP address
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
