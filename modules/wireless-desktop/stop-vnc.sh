#!/bin/bash
# iLynxOS Wireless Desktop - Stop VNC Server (x11vnc)

killall -9 x11vnc 2>/dev/null || true
pkill -9 -f "x11vnc" 2>/dev/null || true
rm -f /var/run/x11vnc.pid 2>/dev/null || true

echo "SUCCESS: x11vnc stopped."
