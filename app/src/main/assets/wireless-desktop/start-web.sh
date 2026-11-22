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

if [ -z "$PASSWORD" ] || [[ "$PASSWORD" == ilynx* ]] || [ "$PASSWORD" = "ilinux1719" ]; then
    PASSWORD="raja1234"
fi

if [ "$DISPLAY_NUM" = "0" ]; then
    VNC_PORT="5900"
else
    VNC_PORT="5901"
fi

SCRIPT_DIR="/usr/local/share/ilynxos/wireless-desktop"
NOVNC_DIR="$SCRIPT_DIR/novnc"

# Ensure lock so parallel calls don't race
LOCKFILE="/tmp/start-web.lock"
exec 201>"$LOCKFILE" 2>/dev/null || true
if command -v flock >/dev/null 2>&1; then
    flock -n 201 || { echo "Web server launcher already running"; exit 0; }
fi

mkdir -p /var/log /var/run /root/.ilynxos/ssl 2>/dev/null || true

echo "=========================================="
echo " Starting i-Lynx OS Web Desktop (HTML5)... "
echo " Port: $PORT | Display: :$DISPLAY_NUM | VNC Backend: :$VNC_PORT "
echo "=========================================="

# 2. Check if VNC backend is listening on 127.0.0.1:$VNC_PORT
IS_RUNNING=0
if python3 -c "import socket; s = socket.socket(); s.settimeout(0.5); s.connect(('127.0.0.1', $VNC_PORT)); s.close()" 2>/dev/null; then
    IS_RUNNING=1
fi

if [ "$IS_RUNNING" = "0" ]; then
    echo "[Web Mode] VNC backend not running on port $VNC_PORT. Starting TigerVNC backend..."
    if [ -x "$SCRIPT_DIR/start-vnc.sh" ]; then
        "$SCRIPT_DIR/start-vnc.sh" "$VNC_PORT" "$PASSWORD" "$DISPLAY_NUM" "$RESOLUTION"
    elif [ -x "/usr/local/bin/start-vnc" ]; then
        /usr/local/bin/start-vnc "$VNC_PORT" "$PASSWORD" "$DISPLAY_NUM" "$RESOLUTION"
    fi
fi

# Wait up to 5 seconds for VNC backend to become available
for i in $(seq 1 50); do
    if python3 -c "import socket; s = socket.socket(); s.settimeout(0.2); s.connect(('127.0.0.1', $VNC_PORT)); s.close()" 2>/dev/null; then
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

# 5. Launch websockify bound explicitly to 0.0.0.0:$PORT
echo "[Web Mode] Launching optimized noVNC + WebSockify server on 0.0.0.0:$PORT..."
PYTHONPATH="$SCRIPT_DIR" python3 -O -u -m websockify \
    --web="$NOVNC_DIR" \
    $SSL_ARGS \
    --log-file=/var/log/websockify.log \
    "0.0.0.0:$PORT" "127.0.0.1:$VNC_PORT" >/dev/null 2>&1 &

PID=$!
echo "$PID" > /var/run/websockify.pid 2>/dev/null || true

# Verify websockify is listening
for i in $(seq 1 30); do
    if python3 -c "import socket; s = socket.socket(); s.settimeout(0.2); s.connect(('127.0.0.1', $PORT)); s.close()" 2>/dev/null; then
        break
    fi
    sleep 0.1
done

if pgrep -f "websockify" >/dev/null 2>&1; then
    PID=$(pgrep -f "websockify" | head -n 1)
    echo "$PID" > /var/run/websockify.pid 2>/dev/null || true

    DEVICE_IP=$(ifconfig 2>/dev/null | awk '/inet / && $2 !~ /^127\./ {print $2}' | head -n 1)
    [ -z "$DEVICE_IP" ] && DEVICE_IP=$(ip -4 addr show 2>/dev/null | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1)
    [ -z "$DEVICE_IP" ] && DEVICE_IP="10.160.148.192"
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
