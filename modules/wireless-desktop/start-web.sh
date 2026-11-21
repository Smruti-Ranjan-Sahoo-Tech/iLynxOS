#!/bin/bash
# iLynxOS Wireless Desktop - Start Web Mode (Xpra Shadow Mode + HTML5 Client)
# Shares existing X11 display :0 via WebSockets & HTML5
set -e

PORT="${1:-14500}"
PASSWORD="$2"
USE_TLS="${3:-1}"

if [ -z "$PASSWORD" ]; then
    echo "ERROR: Password required for secure Web access."
    exit 1
fi

if [ ! -S "/tmp/.X11-unix/X0" ]; then
    echo "ERROR: X11 display socket /tmp/.X11-unix/X0 not found. Start desktop first."
    exit 2
fi

export DISPLAY=:0
mkdir -p /root/.xpra /var/log /var/run 2>/dev/null || true

# Kill any existing Xpra shadow sessions
xpra stop shadow::0 2>/dev/null || true
pkill -9 -f "xpra shadow" 2>/dev/null || true
sleep 0.3

# Store password securely in /root/.xpra/password.txt
echo "$PASSWORD" > /root/.xpra/password.txt
chmod 600 /root/.xpra/password.txt

# Ensure TLS certificate is ready if TLS requested
if [ "$USE_TLS" = "1" ] || [ "$USE_TLS" = "true" ]; then
    if [ ! -f "/root/.xpra/cert.pem" ] || [ ! -f "/root/.xpra/key.pem" ]; then
        openssl req -new -newkey rsa:2048 -days 365 -nodes -x509 \
            -subj "/C=US/ST=iLynxOS/L=Local/O=iLynxOS/CN=iLynxOS-Wireless" \
            -keyout /root/.xpra/key.pem -out /root/.xpra/cert.pem 2>/dev/null || true
        chmod 600 /root/.xpra/key.pem 2>/dev/null || true
    fi
fi

# Launch Xpra in shadow mode sharing :0
# When TLS is enabled, try --bind-ssl; fallback to --bind-tcp
if [ "$USE_TLS" = "1" ] || [ "$USE_TLS" = "true" ]; then
    xpra shadow :0 \
        --bind-ssl=0.0.0.0:"$PORT",auth=password:file=/root/.xpra/password.txt \
        --ssl-cert=/root/.xpra/cert.pem \
        --ssl-key=/root/.xpra/key.pem \
        --html=on \
        --daemon=yes \
        --exit-with-client=no \
        --clipboard=yes \
        --notifications=yes \
        --log-file=/var/log/xpra-web.log 2>&1 || \
    xpra shadow :0 \
        --bind-tcp=0.0.0.0:"$PORT",auth=password:file=/root/.xpra/password.txt \
        --html=on \
        --daemon=yes \
        --exit-with-client=no \
        --clipboard=yes \
        --notifications=yes \
        --log-file=/var/log/xpra-web.log 2>&1
else
    xpra shadow :0 \
        --bind-tcp=0.0.0.0:"$PORT",auth=password:file=/root/.xpra/password.txt \
        --html=on \
        --daemon=yes \
        --exit-with-client=no \
        --clipboard=yes \
        --notifications=yes \
        --log-file=/var/log/xpra-web.log 2>&1
fi

sleep 1
if pgrep -f "xpra shadow" >/dev/null 2>&1 || pgrep -x xpra >/dev/null 2>&1; then
    PID=$(pgrep -f "xpra shadow" | head -n 1)
    echo "$PID" > /var/run/xpra-web.pid 2>/dev/null || true
    echo "SUCCESS: Web Mode (Xpra Shadow) started on port $PORT (PID: $PID)"
    exit 0
else
    echo "ERROR: Failed to start Xpra shadow. Log output:"
    cat /var/log/xpra-web.log 2>/dev/null || true
    exit 3
fi
