#!/bin/bash
# iLynxOS Wireless Desktop - Stop VNC Server
pkill -9 -f "x11vnc" 2>/dev/null || true
rm -f /var/run/x11vnc.pid /tmp/vnc_start.req /tmp/vnc.status 2>/dev/null || true

# If Web Mode is also not running, clean up virtual display :1
if ! pgrep -f "websockify" >/dev/null 2>&1; then
    pkill -9 -f "Xvfb :1" 2>/dev/null || true
    pkill -9 -f "openbox.*:1" 2>/dev/null || true
    rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 /var/run/vnc_display.txt 2>/dev/null || true
fi

echo "SUCCESS: x11vnc stopped"
exit 0
