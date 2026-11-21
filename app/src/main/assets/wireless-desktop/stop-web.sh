#!/bin/bash
# iLynxOS Wireless Desktop - Stop Web Mode
pkill -9 -f "websockify" 2>/dev/null || true
pkill -9 -f "python3 -m websockify" 2>/dev/null || true
rm -f /var/run/websockify.pid /tmp/web_start.req /tmp/web.status 2>/dev/null || true
echo "SUCCESS: Web Mode stopped"
exit 0
