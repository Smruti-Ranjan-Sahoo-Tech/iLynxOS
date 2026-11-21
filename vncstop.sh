#!/bin/bash
# =======================================================
# i-Lynx OS 1.0 - Remote VNC Desktop Stopper
# =======================================================

echo "Stopping i-Lynx OS VNC Server..."
command -v vncserver >/dev/null 2>&1 && vncserver -kill :1 2>/dev/null || true
pkill -9 -f "Xvnc :1" 2>/dev/null || true
pkill -9 -f "Xvnc" 2>/dev/null || true
pkill -9 -f "x11vnc.*:1" 2>/dev/null || true
pkill -9 -f "pcmanfm-qt.*vnc" 2>/dev/null || true

# If Web Mode is also not running, clean up desktop session and locks
if ! pgrep -f "websockify" >/dev/null 2>&1; then
    pkill -9 -f "openbox.*:1" 2>/dev/null || true
    pkill -9 -f "Xvfb :1" 2>/dev/null || true
    rm -rf /tmp/.X11-unix/X1 /tmp/.X1-lock "$TMPDIR/.X11-unix/X1" "$TMPDIR/.X1-lock" 2>/dev/null || true
fi

rm -f /var/run/x11vnc.pid /var/run/vnc_display.txt /tmp/vnc_start.req /tmp/vnc.status /tmp/start-vnc-*.lock 2>/dev/null || true

echo "✅ VNC Server stopped successfully."
exit 0
