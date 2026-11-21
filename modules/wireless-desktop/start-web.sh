#!/bin/bash
# iLynxOS Wireless Desktop - Start Web Mode (noVNC + WebSockify)
# Streams dedicated virtual X11 session (:1) or screen (:0) via HTML5 client
set -e

PORT="${1:-14500}"
PASSWORD="$2"
USE_TLS="${3:-0}"
DISPLAY_NUM="${4:-1}"
VNC_PORT="5900"

SCRIPT_DIR="/usr/local/share/ilynxos/wireless-desktop"
NOVNC_DIR="$SCRIPT_DIR/novnc"

mkdir -p /var/log /var/run /root/.ilynxos/ssl 2>/dev/null || true

# 1. Ensure VNC server backend is running on DISPLAY_NUM
if ! pgrep -f "x11vnc" >/dev/null 2>&1; then
    echo "[Web Mode] VNC backend not running. Starting VNC on display :$DISPLAY_NUM..."
    if [ -x "$SCRIPT_DIR/start-vnc.sh" ]; then
        "$SCRIPT_DIR/start-vnc.sh" "$VNC_PORT" "$PASSWORD" "$DISPLAY_NUM"
    elif [ -x "/usr/local/bin/start-vnc" ]; then
        /usr/local/bin/start-vnc "$VNC_PORT" "$PASSWORD" "$DISPLAY_NUM"
    fi
fi

# Wait up to 3 seconds for VNC port to accept connections
for i in $(seq 1 30); do
    if (exec 3<>/dev/tcp/127.0.0.1/"$VNC_PORT") 2>/dev/null; then
        exec 3<&-
        exec 3>&-
        break
    fi
    sleep 0.1
done

# 2. Stop any existing websockify instance
pkill -9 -f "websockify.*$PORT" 2>/dev/null || true
pkill -9 -f "python3 -m websockify.*$PORT" 2>/dev/null || true
sleep 0.2

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
echo "[Web Mode] Launching optimized noVNC + WebSockify server on port $PORT..."
PYTHONPATH="$SCRIPT_DIR" python3 -O -u -m websockify \
    --web="$NOVNC_DIR" \
    $SSL_ARGS \
    --log-file=/var/log/websockify.log \
    "$PORT" "127.0.0.1:$VNC_PORT" >/dev/null 2>&1 &

PID=$!
echo "$PID" > /var/run/websockify.pid 2>/dev/null || true

sleep 0.6
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
