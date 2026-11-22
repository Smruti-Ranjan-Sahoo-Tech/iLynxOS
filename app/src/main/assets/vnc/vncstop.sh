#!/bin/bash
[ ! -x "/bin/bash" ] && [ -x "/data/data/com.termux/files/usr/bin/bash" ] && exec /data/data/com.termux/files/usr/bin/bash "$0" "$@"
# =======================================================
# i-Lynx OS 1.0 - Remote VNC Desktop Stopper
# =======================================================

TARGET_DISP="$1"
TMPDIR="${TMPDIR:-/data/data/com.termux/files/usr/tmp}"
[ ! -d "$TMPDIR" ] && TMPDIR="/tmp"

echo "Stopping i-Lynx OS VNC Server..."

if [ -n "$TARGET_DISP" ]; then
    DISP_NUM="${TARGET_DISP#:}"
    if [ "$DISP_NUM" != "0" ]; then
        pkill -9 -f "Xvnc :$DISP_NUM" 2>/dev/null || true
        pkill -9 -f "Xtigervnc.*:$DISP_NUM" 2>/dev/null || true
        pkill -9 -f "tigervncserver.*:$DISP_NUM" 2>/dev/null || true
        pkill -9 -f "vncserver.*:$DISP_NUM" 2>/dev/null || true
        rm -rf "$TMPDIR/.X$DISP_NUM-lock" "$TMPDIR/.X11-unix/X$DISP_NUM" 2>/dev/null || true
        rm -rf "/tmp/.X$DISP_NUM-lock" "/tmp/.X11-unix/X$DISP_NUM" 2>/dev/null || true
    fi
    pkill -9 -f "x11vnc.*:$DISP_NUM" 2>/dev/null || true
    pkill -9 -f "x11vnc.*-rfbport" 2>/dev/null || true
else
    pkill -9 -f "Xvnc" 2>/dev/null || true
    pkill -9 -f "Xtigervnc" 2>/dev/null || true
    pkill -9 -f "x11vnc" 2>/dev/null || true
    pkill -9 -f "tigervncserver" 2>/dev/null || true
    pkill -9 -f "vncserver" 2>/dev/null || true
    for d in 1 2 3 4 5; do
        rm -rf "$TMPDIR/.X$d-lock" "$TMPDIR/.X11-unix/X$d" 2>/dev/null || true
        rm -rf "/tmp/.X$d-lock" "/tmp/.X11-unix/X$d" 2>/dev/null || true
    done
fi

pkill -9 -f "snap-daemon" 2>/dev/null || true
pkill -9 -f "xstartup" 2>/dev/null || true

rm -rf ~/.dbus /tmp/start-vnc-*.lock /var/run/x11vnc.pid /tmp/vnc.status 2>/dev/null || true
command -v termux-wake-unlock >/dev/null 2>&1 && termux-wake-unlock 2>/dev/null || true

echo "✅ VNC Server stopped successfully."
exit 0
