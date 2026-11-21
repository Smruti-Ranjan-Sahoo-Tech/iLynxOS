#!/bin/bash
# =======================================================
# i-Lynx OS 1.0 - Remote Web Desktop Stopper
# =======================================================

echo "Stopping i-Lynx OS Web Mode..."
pkill -9 -f "websockify" 2>/dev/null || true
pkill -9 -f "python3 -m websockify" 2>/dev/null || true
rm -f /var/run/websockify.pid /tmp/web_start.req /tmp/web.status 2>/dev/null || true

# If standalone VNC server is also not running, clean up the VNC desktop session
if ! pgrep -f "x11vnc" >/dev/null 2>&1 && [ ! -f "/tmp/vnc.status" ]; then
    SCRIPT_DIR="/usr/local/share/ilynxos/wireless-desktop"
    if [ -x "$SCRIPT_DIR/stop-vnc.sh" ]; then
        "$SCRIPT_DIR/stop-vnc.sh" 2>/dev/null || true
    elif [ -x "/usr/local/bin/stop-vnc" ]; then
        /usr/local/bin/stop-vnc 2>/dev/null || true
    fi
fi

echo "✅ Web Mode stopped successfully."
exit 0
