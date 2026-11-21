#!/bin/bash
# iLynxOS Wireless Desktop - Start Web Mode (noVNC + WebSockify)
# Streams existing X11 display :0 via WebSockets & HTML5 client
set -e

PORT="${1:-14500}"
PASSWORD="$2"
USE_TLS="${3:-0}"

SCRIPT_DIR="/usr/local/share/ilynxos/wireless-desktop"
NOVNC_DIR="$SCRIPT_DIR/novnc"

mkdir -p /var/log /var/run /root/.ilynxos/ssl 2>/dev/null || true

# 1. Ensure VNC server is running on display :0 (port 5900)
if ! pgrep -f "x11vnc" >/dev/null 2>&1; then
    echo "[Web Mode] VNC backend not running. Starting x11vnc on 5900..."
    if [ -x "$SCRIPT_DIR/start-vnc.sh" ]; then
        "$SCRIPT_DIR/start-vnc.sh" 5900 "$PASSWORD"
    else
        x11vnc -display :0 -rfbport 5900 -listen 0.0.0.0 -nopw -forever -shared -repeat -noshm -bg -o /var/log/x11vnc.log 2>&1
    fi
fi

# Wait up to 3 seconds for port 5900 to accept connections
for i in $(seq 1 30); do
    if (exec 3<>/dev/tcp/127.0.0.1/5900) 2>/dev/null; then
        exec 3<&-
        exec 3>&-
        break
    fi
    sleep 0.1
done

# 2. Stop any existing websockify instance
pkill -9 -f "websockify" 2>/dev/null || true
pkill -9 -f "python3 -m websockify" 2>/dev/null || true
sleep 0.3

# 3. Handle TLS certificate if requested
SSL_ARGS=""
if [ "$USE_TLS" = "1" ] || [ "$USE_TLS" = "true" ]; then
    CERT="/root/.ilynxos/ssl/web.pem"
    if [ ! -f "$CERT" ]; then
        openssl req -new -newkey rsa:2048 -days 365 -nodes -x509 \
            -subj "/C=US/ST=iLynxOS/L=Local/O=iLynxOS/CN=iLynxOS-Web" \
            -keyout "$CERT" -out "$CERT" 2>/dev/null || true
        chmod 600 "$CERT" 2>/dev/null || true
    fi
    if [ -f "$CERT" ]; then
        SSL_ARGS="--cert=$CERT"
    fi
fi

# 4. Launch websockify with embedded noVNC web server in background
# Optimized python invocation with -O (bytecode optimize) and -u (unbuffered)
echo "[Web Mode] Launching optimized noVNC + WebSockify server on port $PORT..."
PYTHONPATH="$SCRIPT_DIR" python3 -O -u -m websockify \
    --web="$NOVNC_DIR" \
    $SSL_ARGS \
    --log-file=/var/log/websockify.log \
    "$PORT" 127.0.0.1:5900 >/dev/null 2>&1 &

PID=$!
echo "$PID" > /var/run/websockify.pid 2>/dev/null || true

sleep 0.8
if pgrep -f "websockify" >/dev/null 2>&1; then
    PID=$(pgrep -f "websockify" | head -n 1)
    echo "$PID" > /var/run/websockify.pid 2>/dev/null || true
    echo "SUCCESS: Web Mode (noVNC HTML5) started on port $PORT (PID: $PID)"
    exit 0
else
    echo "ERROR: Failed to start websockify. Log output:"
    cat /var/log/websockify.log 2>/dev/null || true
    exit 3
fi
