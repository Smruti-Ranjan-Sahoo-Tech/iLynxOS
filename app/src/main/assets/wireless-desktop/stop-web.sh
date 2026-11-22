#!/data/data/com.termux/files/usr/bin/bash
# If not running in Termux, use standard bash
[ ! -x "/data/data/com.termux/files/usr/bin/bash" ] && exec /bin/bash "$0" "$@"
# =======================================================
# i-Lynx OS 1.0 - Remote Web Desktop Stopper
# =======================================================

echo "Stopping i-Lynx OS Web Mode..."

# 1. Kill websockify processes
pkill -9 -f "websockify" 2>/dev/null || true
pkill -9 -f "python3.*websockify" 2>/dev/null || true

TMPDIR="${TMPDIR:-/data/data/com.termux/files/usr/tmp}"
[ ! -d "$TMPDIR" ] && TMPDIR="/tmp"
rm -f /var/run/websockify.pid /tmp/web_start.req /tmp/web.status /tmp/start-web*.lock "$TMPDIR"/start-web*.lock 2>/dev/null || true

# 2. If VNC server is not explicitly requested as active, stop the display :1 session
CURRENT_DIR="$(cd "$(dirname "$0")" 2>/dev/null && pwd)"
VNC_ACTIVE=$(cat /tmp/vnc.status 2>/dev/null || echo "0")

if [ "$VNC_ACTIVE" != "1" ]; then
    for s in \
        "$CURRENT_DIR/vncstop.sh" \
        "$CURRENT_DIR/stop-vnc.sh" \
        "$CURRENT_DIR/modules/wireless-desktop/stop-vnc.sh" \
        "$CURRENT_DIR/app/src/main/assets/wireless-desktop/stop-vnc.sh" \
        "/usr/local/share/ilynxos/wireless-desktop/stop-vnc.sh" \
        "/usr/local/bin/stop-vnc" \
        "/usr/local/bin/vncstop" \
        "vncstop" \
        "stop-vnc"; do
        if [ -x "$s" ] || command -v "$s" >/dev/null 2>&1; then
            "$s" :1 2>/dev/null || true
            break
        fi
    done
    command -v vncserver >/dev/null 2>&1 && vncserver -kill :1 2>/dev/null || true
    pkill -9 -f "Xvnc :1" 2>/dev/null || true
fi

command -v termux-wake-unlock >/dev/null 2>&1 && termux-wake-unlock 2>/dev/null || true
echo "✅ Web Mode stopped successfully."
exit 0
