#!/bin/bash
# iLynxOS Wireless Desktop - Start VNC Server (x11vnc)
# Attaches to existing X11 display :0
set -e

PORT="${1:-5900}"
PASSWORD="$2"

if [ -z "$PASSWORD" ]; then
    echo "ERROR: Password required for secure VNC access."
    exit 1
fi

if [ ! -S "/tmp/.X11-unix/X0" ]; then
    echo "ERROR: X11 display socket /tmp/.X11-unix/X0 not found. Start desktop first."
    exit 2
fi

export DISPLAY=:0
mkdir -p /root/.vnc /var/log /var/run 2>/dev/null || true

# Kill any existing x11vnc session
killall -9 x11vnc 2>/dev/null || true
sleep 0.3

# Generate password file securely
x11vnc -storepasswd "$PASSWORD" /root/.vnc/passwd 2>/dev/null
chmod 600 /root/.vnc/passwd 2>/dev/null || true

# Start x11vnc attached to existing :0 display
# -forever: keep listening after disconnect
# -shared: allow multiple/reconnecting clients
# -rfbport: listening port
# -rfbauth: password authentication file
# -repeat: key repeat support
# -bg: run in background as daemon
# -o: output log
x11vnc -display :0 \
    -rfbport "$PORT" \
    -rfbauth /root/.vnc/passwd \
    -forever \
    -shared \
    -repeat \
    -bg \
    -o /var/log/x11vnc.log

sleep 0.5
if pgrep -x x11vnc >/dev/null 2>&1; then
    PID=$(pgrep -x x11vnc | head -n 1)
    echo "$PID" > /var/run/x11vnc.pid 2>/dev/null || true
    echo "SUCCESS: x11vnc started on port $PORT (PID: $PID)"
    exit 0
else
    echo "ERROR: Failed to start x11vnc. Check /var/log/x11vnc.log"
    cat /var/log/x11vnc.log 2>/dev/null || true
    exit 3
fi
