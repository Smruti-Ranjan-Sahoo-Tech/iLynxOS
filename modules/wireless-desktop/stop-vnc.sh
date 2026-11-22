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
    command -v vncserver >/dev/null 2>&1 && vncserver -kill ":$DISP_NUM" 2>/dev/null || true
    command -v tigervncserver >/dev/null 2>&1 && tigervncserver -kill ":$DISP_NUM" 2>/dev/null || true
    pkill -9 -f "Xvnc :$DISP_NUM" 2>/dev/null || true
    pkill -9 -f "Xtigervnc :$DISP_NUM" 2>/dev/null || true
    pkill -9 -f "x11vnc.*:$DISP_NUM" 2>/dev/null || true
    rm -rf "$TMPDIR/.X$DISP_NUM-lock" "$TMPDIR/.X11-unix/X$DISP_NUM" 2>/dev/null || true
    rm -rf "/tmp/.X$DISP_NUM-lock" "/tmp/.X11-unix/X$DISP_NUM" 2>/dev/null || true
else
    for d in 1 2 3 4 5; do
        command -v vncserver >/dev/null 2>&1 && vncserver -kill ":$d" 2>/dev/null || true
        command -v tigervncserver >/dev/null 2>&1 && tigervncserver -kill ":$d" 2>/dev/null || true
    done
    pkill -9 -f "Xvnc" 2>/dev/null || true
    pkill -9 -f "Xtigervnc" 2>/dev/null || true
    pkill -9 -f "x11vnc" 2>/dev/null || true
    rm -rf "$TMPDIR"/.X*-lock "$TMPDIR"/.X11-unix/X* 2>/dev/null || true
    rm -rf /tmp/.X*-lock /tmp/.X11-unix/X* 2>/dev/null || true
fi

pkill -9 -f "lxqt-panel" 2>/dev/null || true
pkill -9 -f "pcmanfm-qt" 2>/dev/null || true
pkill -9 -f "openbox" 2>/dev/null || true
pkill -9 -f "lxqt-notificationd" 2>/dev/null || true
pkill -9 -f "snap-daemon" 2>/dev/null || true

rm -rf ~/.dbus /tmp/start-vnc-*.lock /var/run/x11vnc.pid /tmp/vnc.status 2>/dev/null || true
command -v termux-wake-unlock >/dev/null 2>&1 && termux-wake-unlock 2>/dev/null || true

echo "✅ VNC Server stopped successfully."
exit 0
