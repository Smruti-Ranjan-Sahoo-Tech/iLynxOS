#!/bin/bash
# =======================================================
# i-Lynx OS 1.0 - Remote Web Desktop Launcher (HTML5 noVNC)
# =======================================================
set -e

# 1. Prevent Android from sleeping
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock 2>/dev/null || true

PORT="${1:-14500}"
PASSWORD="${2:-raja1234}"
USE_TLS="${3:-0}"
DISPLAY_NUM="${4:-1}"
RESOLUTION="${5:-1280x720}"

if [ "$DISPLAY_NUM" = "0" ]; then
    VNC_PORT="5900"
else
    VNC_PORT="5901"
fi

SCRIPT_DIR="/usr/local/share/ilynxos/wireless-desktop"
NOVNC_DIR="$SCRIPT_DIR/novnc"

mkdir -p /var/log /var/run /root/.ilynxos/ssl 2>/dev/null || true

echo "=========================================="
echo " Starting i-Lynx OS Web Desktop (HTML5)... "
echo " Port: $PORT | Display: :$DISPLAY_NUM | VNC Backend: :$VNC_PORT "
echo "=========================================="

# 2. Ensure VNC server backend is running on 127.0.0.1:$VNC_PORT
if ! (exec 3<>/dev/tcp/127.0.0.1/"$VNC_PORT") 2>/dev/null; then
    echo "[Web Mode] VNC backend not running on port $VNC_PORT. Starting VNC backend..."
    if [ -x "$SCRIPT_DIR/start-vnc.sh" ]; then
        "$SCRIPT_DIR/start-vnc.sh" "$VNC_PORT" "$PASSWORD" "$DISPLAY_NUM" "$RESOLUTION"
    elif [ -x "/usr/local/bin/start-vnc" ]; then
        /usr/local/bin/start-vnc "$VNC_PORT" "$PASSWORD" "$DISPLAY_NUM" "$RESOLUTION"
    fi
else
    exec 3<&- 2>/dev/null || true
    exec 3>&- 2>/dev/null || true
fi

# Wait up to 4 seconds for VNC port to accept connections
for i in $(seq 1 40); do
    if (exec 3<>/dev/tcp/127.0.0.1/"$VNC_PORT") 2>/dev/null; then
        exec 3<&-
        exec 3>&-
        break
    fi
    sleep 0.1
done

# 3. Stop any existing websockify instance
pkill -9 -f "websockify.*$PORT" 2>/dev/null || true
pkill -9 -f "python3 -m websockify.*$PORT" 2>/dev/null || true
sleep 0.2

# 4. Handle TLS certificate if requested
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

# 5. Launch websockify with embedded noVNC web server in background
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

    DEVICE_IP=$(ip -4 addr show 2>/dev/null | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1)
    [ -z "$DEVICE_IP" ] && DEVICE_IP="10.65.94.50"
    PROTO="http"
    [ -n "$SSL_ARGS" ] && PROTO="https"

    echo ""
    echo "======================================================"
    echo "  🚀 i-Lynx OS Web Mode (HTML5) is RUNNING!          "
    echo "======================================================"
    echo "  Open browser on PC, tablet, or phone:               "
    echo "  🌐 URL      : ${PROTO}://${DEVICE_IP}:${PORT}/vnc.html "
    echo "  🔑 Password : ${PASSWORD}                           "
    echo "======================================================"
    exit 0
else
    echo "ERROR: Failed to start websockify. Log output:"
    cat /var/log/websockify.log 2>/dev/null || true
    exit 3
fi
