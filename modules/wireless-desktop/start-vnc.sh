#!/bin/bash
# iLynxOS Wireless Desktop - Start VNC Server (x11vnc)
# Supports dedicated virtual X11 session (:1) with Xvfb + Openbox/LXQt desktop
# or mirroring existing screen (:0) with full RealVNC / noVNC compatibility
set -e

PORT="${1:-5900}"
PASSWORD="$2"
DISPLAY_NUM="${3:-1}"
RESOLUTION="${4:-1920x1080}"

mkdir -p /root/.vnc /var/log /var/run /tmp/.X11-unix 2>/dev/null || true

# 1. Configure authentication
AUTH_ARGS="-nopw"
if [ -n "$PASSWORD" ]; then
    x11vnc -storepasswd "$PASSWORD" /root/.vnc/passwd 2>/dev/null || true
    if [ -f "/root/.vnc/passwd" ]; then
        chmod 600 /root/.vnc/passwd 2>/dev/null || true
        AUTH_ARGS="-rfbauth /root/.vnc/passwd"
    else
        AUTH_ARGS="-passwd $PASSWORD"
    fi
fi

# 2. Setup Display: If DISPLAY_NUM != 0, start a dedicated Xvfb virtual session
if [ "$DISPLAY_NUM" != "0" ]; then
    # Dedicated new session on display :$DISPLAY_NUM
    if [ ! -S "/tmp/.X11-unix/X$DISPLAY_NUM" ] || ! pgrep -f "Xvfb :$DISPLAY_NUM" >/dev/null 2>&1; then
        echo "[VNC Server] Starting new dedicated virtual X11 session on :$DISPLAY_NUM ($RESOLUTION)..."
        rm -f "/tmp/.X$DISPLAY_NUM-lock" "/tmp/.X11-unix/X$DISPLAY_NUM" 2>/dev/null || true

        # Start Xvfb virtual display
        Xvfb ":$DISPLAY_NUM" -screen 0 "${RESOLUTION}x24" -ac +extension XTEST +extension RANDR +extension RENDER -nolisten tcp >/dev/null 2>&1 &

        for i in $(seq 1 40); do
            if [ -S "/tmp/.X11-unix/X$DISPLAY_NUM" ]; then
                break
            fi
            sleep 0.1
        done
        sleep 0.3

        # Launch Desktop Environment on :$DISPLAY_NUM
        echo "[VNC Server] Launching desktop environment on :$DISPLAY_NUM..."
        export DISPLAY=":$DISPLAY_NUM"
        xsetroot -display ":$DISPLAY_NUM" -solid "#1e1e2e" 2>/dev/null || true

        # Launch Openbox window manager
        if command -v openbox >/dev/null 2>&1; then
            if [ -f "/root/.config/openbox/rc.xml" ]; then
                DISPLAY=":$DISPLAY_NUM" openbox --config-file /root/.config/openbox/rc.xml >/dev/null 2>&1 &
            else
                DISPLAY=":$DISPLAY_NUM" openbox >/dev/null 2>&1 &
            fi
        fi
        sleep 0.2

        # Launch pcmanfm-qt desktop icons & wallpaper
        if command -v pcmanfm-qt >/dev/null 2>&1; then
            DISPLAY=":$DISPLAY_NUM" pcmanfm-qt --desktop --profile=lxqt >/dev/null 2>&1 &
        fi

        # Launch Taskbar Panel
        if command -v lxqt-panel >/dev/null 2>&1; then
            DISPLAY=":$DISPLAY_NUM" lxqt-panel >/dev/null 2>&1 &
        elif command -v xfce4-panel >/dev/null 2>&1; then
            DISPLAY=":$DISPLAY_NUM" xfce4-panel --sm-client-disable --disable-wm-check >/dev/null 2>&1 &
        fi
    fi
else
    # Mirroring :0 display
    for i in $(seq 1 30); do
        if [ -S "/tmp/.X11-unix/X0" ]; then
            break
        fi
        sleep 0.1
    done
fi

# Kill any existing x11vnc session for this display/port cleanly
pkill -9 -f "x11vnc.*:$DISPLAY_NUM" 2>/dev/null || true
pkill -9 -f "x11vnc.*-rfbport $PORT" 2>/dev/null || true
sleep 0.2

# Launch x11vnc attached to :$DISPLAY_NUM
# High-speed RFB configuration:
# - Instant screen updates via XDamage
# - Full mouse & keyboard injection via XTEST
# - Single-threaded event loop to prevent PRoot ptrace mutex lag
# - Auto-reconnect & multi-client shared support
x11vnc -display ":$DISPLAY_NUM" \
    -rfbport "$PORT" \
    -listen 0.0.0.0 \
    $AUTH_ARGS \
    -cursor arrow \
    -forever \
    -shared \
    -repeat \
    -noshm \
    -bg \
    -o /var/log/x11vnc.log 2>&1

sleep 0.5
if pgrep -f "x11vnc" >/dev/null 2>&1; then
    PID=$(pgrep -f "x11vnc" | head -n 1)
    echo "$PID" > /var/run/x11vnc.pid 2>/dev/null || true
    echo "$DISPLAY_NUM" > /var/run/vnc_display.txt 2>/dev/null || true
    echo "SUCCESS: x11vnc started on port $PORT (PID: $PID, DISPLAY=:$DISPLAY_NUM)"
    exit 0
else
    echo "ERROR: Failed to start x11vnc. Log output:"
    cat /var/log/x11vnc.log 2>/dev/null || true
    exit 3
fi
