#!/bin/bash
# iLynxOS Wireless Desktop - Stop VNC Server
pkill -9 -f "x11vnc" 2>/dev/null || true
rm -f /var/run/x11vnc.pid /tmp/vnc_start.req /tmp/vnc.status 2>/dev/null || true
echo "SUCCESS: x11vnc stopped"
exit 0
