#!/bin/bash
# iLynxOS Wireless Desktop - Start VNC Server (x11vnc)
# Attaches directly to existing X11 display :0 with full RealVNC Viewer compatibility
set -e

PORT="${1:-5900}"
PASSWORD="$2"

export DISPLAY=:0
mkdir -p /root/.vnc /var/log /var/run 2>/dev/null || true

# Kill any existing x11vnc session cleanly
pkill -9 -f "x11vnc" 2>/dev/null || true
sleep 0.3

# Configure authentication
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

# Ensure display :0 socket is ready
for i in $(seq 1 30); do
    if [ -S "/tmp/.X11-unix/X0" ]; then
        break
    fi
    sleep 0.1
done

# Start x11vnc attached to existing :0 display
# -threads: Multithreaded RFB server (decouples screen capture from client encoding/network transmit)
# -noxdamage: CRITICAL for RealVNC & noVNC on Xlorie (prevents black/frozen screen)
# -wait 30 -defer 15: Smooth ~30 FPS low-latency screen updates with ~45% lower CPU load
# -noxrecord: Prevents XRecord hooks that lag PRoot input events
# -nosynckeys: Prevents round-trip modifier key queries that lag typing
# -nowf: Disables wireframe window copy overhead
# -cursor arrow: Renders software arrow cursor visible in RealVNC and noVNC
# -forever -shared: Allows multiple simultaneous viewers and auto-reconnect
# -repeat: Standard key repeat
# -noshm: Android kernel compatible
# -listen 0.0.0.0: Bind on all interfaces (Wi-Fi LAN, hotspot, USB)
x11vnc -display :0 \
    -rfbport "$PORT" \
    -listen 0.0.0.0 \
    $AUTH_ARGS \
    -noxdamage \
    -wait 30 \
    -defer 15 \
    -threads \
    -noxrecord \
    -nosynckeys \
    -nowf \
    -cursor arrow \
    -forever \
    -shared \
    -repeat \
    -noshm \
    -bg \
    -o /var/log/x11vnc.log || \
x11vnc -display :0 \
    -rfbport "$PORT" \
    -listen 0.0.0.0 \
    $AUTH_ARGS \
    -noxdamage \
    -wait 30 \
    -defer 15 \
    -cursor arrow \
    -forever \
    -shared \
    -repeat \
    -noshm \
    -bg \
    -o /var/log/x11vnc.log

sleep 0.6
if pgrep -f "x11vnc" >/dev/null 2>&1; then
    PID=$(pgrep -f "x11vnc" | head -n 1)
    echo "$PID" > /var/run/x11vnc.pid 2>/dev/null || true
    echo "SUCCESS: x11vnc started on port $PORT (PID: $PID)"
    exit 0
else
    echo "ERROR: Failed to start x11vnc. Check /var/log/x11vnc.log"
    cat /var/log/x11vnc.log 2>/dev/null || true
    exit 3
fi
