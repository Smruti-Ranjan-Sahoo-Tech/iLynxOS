#!/bin/bash
# iLynxOS Wireless Desktop - Status Reporter
# Outputs key-value pairs parseable by Kotlin backend

VNC_RUNNING=0
VNC_PID=""
if pgrep -f "x11vnc" >/dev/null 2>&1; then
    VNC_RUNNING=1
    VNC_PID=$(pgrep -f "x11vnc" | head -n 1)
fi

WEB_RUNNING=0
WEB_PID=""
if pgrep -f "websockify" >/dev/null 2>&1; then
    WEB_RUNNING=1
    WEB_PID=$(pgrep -f "websockify" | head -n 1)
fi

X11_RUNNING=0
if [ -S "/tmp/.X11-unix/X0" ]; then
    X11_RUNNING=1
fi

X11VNC_INSTALLED=0
if command -v x11vnc >/dev/null 2>&1; then
    X11VNC_INSTALLED=1
fi

WEB_INSTALLED=0
if [ -d "/usr/local/share/ilynxos/wireless-desktop/novnc" ] && [ -d "/usr/local/share/ilynxos/wireless-desktop/websockify" ]; then
    WEB_INSTALLED=1
fi

echo "X11_RUNNING=$X11_RUNNING"
echo "X11VNC_INSTALLED=$X11VNC_INSTALLED"
echo "VNC_RUNNING=$VNC_RUNNING"
echo "VNC_PID=$VNC_PID"
echo "XPRA_INSTALLED=$WEB_INSTALLED"
echo "WEB_INSTALLED=$WEB_INSTALLED"
echo "WEB_RUNNING=$WEB_RUNNING"
echo "WEB_PID=$WEB_PID"
