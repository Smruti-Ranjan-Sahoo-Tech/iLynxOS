#!/data/data/com.termux/files/usr/bin/bash
# =======================================================
# i-Lynx OS 1.0 - Remote VNC Desktop Stopper
# =======================================================

echo "Stopping i-Lynx OS VNC Server..."
vncserver -kill :1 2>/dev/null || true
pkill -9 -f "Xvnc :1" 2>/dev/null || true
pkill -9 -f lxqt-panel 2>/dev/null || true
pkill -9 -f pcmanfm-qt 2>/dev/null || true
pkill -9 -f "pcmanfm-qt.*vnc" 2>/dev/null || true
pkill -9 -f openbox 2>/dev/null || true
rm -rf "$TMPDIR/.X11-unix/X1" "$TMPDIR/.X1-lock" /tmp/.X11-unix/X1 /tmp/.X1-lock 2>/dev/null || true

echo "✅ VNC Server stopped successfully."
