#!/data/data/com.termux/files/usr/bin/bash
# If not running in Termux, use standard bash
[ ! -x "/data/data/com.termux/files/usr/bin/bash" ] && exec /bin/bash "$0" "$@"
# =======================================================
# i-Lynx OS 1.0 - High Performance Remote Web Desktop Launcher (HTML5 noVNC)
# =======================================================

# 1. Prevent Android from sleeping (CPU WakeLock)
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock 2>/dev/null || true

# 2. Parse arguments flexibly (order independent)
PORT=""
PASSWORD=""
TARGET_DISP=""
RESOLUTION="1280x720"
USE_TLS=0

if [[ "$1" =~ ^[0-9]{4,5}$ ]] && [ "$1" != "5900" ] && [ "$1" != "5901" ]; then
    PORT="$1"
    PASSWORD="$2"
    USE_TLS="${3:-0}"
    TARGET_DISP="${4:-1}"
    [ -n "$5" ] && RESOLUTION="$5"
else
    for arg in "$@"; do
        if [[ "$arg" =~ ^:[0-9]+$ ]]; then
            TARGET_DISP="${arg#:}"
        elif [[ "$arg" =~ ^[0-9]+x[0-9]+$ ]]; then
            RESOLUTION="$arg"
        elif [[ "$arg" == "tls" || "$arg" == "ssl" || "$arg" == "https" || "$arg" == "1" || "$arg" == "true" ]]; then
            USE_TLS=1
        elif [[ "$arg" =~ ^[0-9]{4,5}$ ]]; then
            if [ "$arg" = "5900" ]; then
                TARGET_DISP="0"
            elif [ "$arg" = "5901" ]; then
                TARGET_DISP="1"
            elif [ -z "$PORT" ]; then
                PORT="$arg"
            fi
        elif [ -z "$PASSWORD" ]; then
            PASSWORD="$arg"
        fi
    done
fi

[ -z "$PORT" ] && PORT="14500"
[ -z "$TARGET_DISP" ] && TARGET_DISP="1"
DISP_NUM="$TARGET_DISP"

if [ "$DISP_NUM" = "0" ]; then
    VNC_PORT="5900"
else
    VNC_PORT="5901"
fi

# Normalize password
WEB_PASS="${PASSWORD:-raja1234}"
if [ -z "$WEB_PASS" ] || [[ "$WEB_PASS" == ilynx* ]] || [ "$WEB_PASS" = "ilinux1719" ]; then
    WEB_PASS="raja1234"
fi

TMPDIR="${TMPDIR:-/data/data/com.termux/files/usr/tmp}"
[ ! -d "$TMPDIR" ] && TMPDIR="/tmp"
export TMPDIR
mkdir -p "$TMPDIR" "$HOME/.ilynxos/ssl" /root/.ilynxos/ssl 2>/dev/null || true

# Ensure lock so parallel calls don't race
LOCKFILE="$TMPDIR/start-web-${PORT}.lock"
exec 201>"$LOCKFILE" 2>/dev/null || true
if command -v flock >/dev/null 2>&1; then
    flock -n 201 || { echo "Web server launcher already running for port $PORT"; exit 0; }
fi

echo "=========================================="
echo " Starting i-Lynx OS Web Desktop (HTML5)..."
echo " Port: $PORT | Display: :$DISP_NUM | VNC Port: $VNC_PORT | Res: $RESOLUTION "
echo "=========================================="

# 3. Locate script directory and noVNC / websockify assets
CURRENT_DIR="$(cd "$(dirname "$0")" 2>/dev/null && pwd)"

CANDIDATE_DIRS=(
    "$CURRENT_DIR"
    "$CURRENT_DIR/novnc/.."
    "$CURRENT_DIR/app/src/main/assets/wireless-desktop"
    "$CURRENT_DIR/modules/wireless-desktop"
    "$CURRENT_DIR/../app/src/main/assets/wireless-desktop"
    "/data/data/com.termux/files/usr/share"
    "${PREFIX:-/data/data/com.termux/files/usr}/share"
    "/usr/share"
    "/usr/local/share"
    "/usr/local/share/ilynxos/wireless-desktop"
    "/data/data/com.termux.ilynxos/files/containers/iLynxOS/rootfs/usr/local/share/ilynxos/wireless-desktop"
    "/data/user/0/com.termux.ilynxos/files/containers/iLynxOS/rootfs/usr/local/share/ilynxos/wireless-desktop"
)

SCRIPT_DIR=""
NOVNC_DIR=""
WEBSOCKIFY_DIR=""

for dir in "${CANDIDATE_DIRS[@]}"; do
    if [ -d "$dir/novnc" ] && [ -d "$dir/websockify" ]; then
        SCRIPT_DIR="$dir"
        NOVNC_DIR="$dir/novnc"
        WEBSOCKIFY_DIR="$dir/websockify"
        break
    fi
done

if [ -z "$NOVNC_DIR" ]; then
    for dir in "${CANDIDATE_DIRS[@]}"; do
        if [ -d "$dir/novnc" ]; then
            NOVNC_DIR="$dir/novnc"
            SCRIPT_DIR="$dir"
            break
        fi
    done
fi

if [ -z "$NOVNC_DIR" ] || [ ! -d "$NOVNC_DIR" ]; then
    for novnc_path in \
        "/data/data/com.termux/files/usr/share/novnc" \
        "${PREFIX:-/data/data/com.termux/files/usr}/share/novnc" \
        "/usr/share/novnc" \
        "/usr/local/share/novnc"; do
        if [ -d "$novnc_path" ]; then
            NOVNC_DIR="$novnc_path"
            break
        fi
    done
fi

if [ -z "$WEBSOCKIFY_DIR" ]; then
    for dir in "${CANDIDATE_DIRS[@]}"; do
        if [ -d "$dir/websockify" ]; then
            WEBSOCKIFY_DIR="$dir/websockify"
            break
        fi
    done
fi

# 4. Check if VNC backend is listening on 127.0.0.1:$VNC_PORT
is_vnc_ready() {
    python3 -c "import socket; s = socket.socket(); s.settimeout(0.3); s.connect(('127.0.0.1', $VNC_PORT)); s.close()" 2>/dev/null
}

if ! is_vnc_ready; then
    echo "[Web Mode] VNC backend not running on port $VNC_PORT. Starting TigerVNC backend..."
    VNC_STARTER=""
    for s in \
        "$CURRENT_DIR/vncstart.sh" \
        "$CURRENT_DIR/start-vnc.sh" \
        "$SCRIPT_DIR/start-vnc.sh" \
        "$CURRENT_DIR/modules/wireless-desktop/start-vnc.sh" \
        "$CURRENT_DIR/app/src/main/assets/wireless-desktop/start-vnc.sh" \
        "/usr/local/share/ilynxos/wireless-desktop/start-vnc.sh" \
        "/usr/local/bin/start-vnc" \
        "/usr/local/bin/vncstart" \
        "vncstart" \
        "start-vnc"; do
        if [ -x "$s" ] || command -v "$s" >/dev/null 2>&1; then
            VNC_STARTER="$s"
            break
        fi
    done

    if [ -n "$VNC_STARTER" ]; then
        "$VNC_STARTER" "$VNC_PORT" "$WEB_PASS" "$DISP_NUM" "$RESOLUTION" &
    else
        echo "[!] Warning: No VNC starter script found. Falling back to proot vncserver..."
        command -v vncserver >/dev/null 2>&1 && vncserver ":$DISP_NUM" -geometry "$RESOLUTION" -depth 24 -ac &
    fi

    # Wait up to 10 seconds for VNC backend to become available
    for i in $(seq 1 50); do
        if is_vnc_ready; then
            break
        fi
        sleep 0.2
    done
fi

# 5. Stop any existing websockify instance on this port
pkill -9 -f "websockify.*$PORT" 2>/dev/null || true
pkill -9 -f "python3.*websockify.*$PORT" 2>/dev/null || true
sleep 0.2

# 6. Handle TLS certificate if requested
SSL_ARGS=""
if [ "$USE_TLS" = "1" ] || [ "$USE_TLS" = "true" ]; then
    SSL_DIR="${HOME}/.ilynxos/ssl"
    mkdir -p "$SSL_DIR" 2>/dev/null || true
    CERT="$SSL_DIR/web.pem"
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

# 7. Configure Python search path for websockify
if [ -n "$SCRIPT_DIR" ]; then
    export PYTHONPATH="$SCRIPT_DIR:$PYTHONPATH"
fi
if [ -n "$WEBSOCKIFY_DIR" ]; then
    export PYTHONPATH="$(dirname "$WEBSOCKIFY_DIR"):$PYTHONPATH"
fi

# 8. Launch websockify bound explicitly to 0.0.0.0:$PORT
echo "[Web Mode] Launching optimized noVNC + WebSockify server on 0.0.0.0:$PORT..."
LOG_FILE="$TMPDIR/websockify.log"
PID_FILE="$TMPDIR/websockify.pid"

python3 -O -u -m websockify \
    --web="$NOVNC_DIR" \
    $SSL_ARGS \
    "0.0.0.0:$PORT" "127.0.0.1:$VNC_PORT" < /dev/null > "$LOG_FILE" 2>&1 &

WEBSOCKIFY_PID=$!
echo "$WEBSOCKIFY_PID" > "$PID_FILE" 2>/dev/null || true

# 9. Verify websockify is listening
WEB_READY=0
for i in $(seq 1 40); do
    if python3 -c "import socket; s = socket.socket(); s.settimeout(0.2); s.connect(('127.0.0.1', $PORT)); s.close()" 2>/dev/null; then
        WEB_READY=1
        break
    fi
    sleep 0.15
done

if [ "$WEB_READY" -eq 1 ] || pgrep -f "websockify" >/dev/null 2>&1; then
    DEVICE_IP=$(ifconfig 2>/dev/null | awk '/inet / && $2 !~ /^127\./ {print $2}' | head -n 1)
    [ -z "$DEVICE_IP" ] && DEVICE_IP=$(ip -4 addr show 2>/dev/null | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1)
    [ -z "$DEVICE_IP" ] && DEVICE_IP=$(python3 -c "import socket; s=socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.connect(('8.8.8.8', 80)); print(s.getsockname()[0]); s.close()" 2>/dev/null)
    [ -z "$DEVICE_IP" ] && DEVICE_IP="10.160.148.192"

    PROTO="http"
    [ "$USE_TLS" = "1" ] && [ -n "$SSL_ARGS" ] && PROTO="https"
    AUTO_URL="${PROTO}://${DEVICE_IP}:${PORT}/vnc.html?autoconnect=true&resize=scale&password=${WEB_PASS}"
    DIRECT_URL="${PROTO}://${DEVICE_IP}:${PORT}/"

    echo ""
    echo "======================================================"
    echo "  🚀 i-Lynx OS Web Desktop (HTML5) is RUNNING!       "
    echo "======================================================"
    echo "  Open browser on PC, tablet, or phone:               "
    echo "                                                      "
    echo "  🌐 Quick URL : $AUTO_URL"
    echo "  🌐 Direct URL: $DIRECT_URL"
    echo "  🔑 Password  : ${WEB_PASS}                          "
    echo "  🖥️ Backend   : 127.0.0.1:${VNC_PORT} (Display :${DISP_NUM})"
    echo "  📐 Canvas    : ${RESOLUTION}                        "
    echo "                                                      "
    echo "  Supported Browsers: Chrome, Firefox, Safari, Edge   "
    echo "                                                      "
    echo "  To stop Web Desktop:                                "
    echo "   run 'webstop' or 'webstop.sh'                      "
    echo "======================================================"
    exit 0
else
    echo "ERROR: Failed to start websockify. Log output:"
    cat /var/log/websockify.log 2>/dev/null || true
    exit 3
fi
