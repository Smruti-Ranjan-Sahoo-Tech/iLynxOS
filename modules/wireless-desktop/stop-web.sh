#!/bin/bash
# iLynxOS Wireless Desktop - Stop Web Mode (Xpra Shadow)

xpra stop shadow::0 2>/dev/null || true
pkill -9 -f "xpra shadow" 2>/dev/null || true
rm -f /var/run/xpra-web.pid 2>/dev/null || true

echo "SUCCESS: Web Mode (Xpra shadow) stopped."
