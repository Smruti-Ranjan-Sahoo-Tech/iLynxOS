#!/bin/bash
# iLynxOS Wireless Desktop - Stop Web Mode
pkill -9 -f "websockify" 2>/dev/null || true
pkill -9 -f "python3 -m websockify" 2>/dev/null || true
rm -f /var/run/websockify.pid /tmp/web_start.req /tmp/web.status 2>/dev/null || true

# If VNC server is also not running, clean up virtual display :1
if ! pgrep -f "x11vnc" >/dev/null 2>&1; then
    pkill -9 -f "Xvfb :1" 2>/dev/null || true
    pkill -9 -f "openbox.*:1" 2>/dev/null || true
    rm -f /tmp/.X1-lock /tmp/.X11-unix/X1 /var/run/vnc_display.txt 2>/dev/null || true
fi

echo "SUCCESS: Web Mode stopped"
exit 0
