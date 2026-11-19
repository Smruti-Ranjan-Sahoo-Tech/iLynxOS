#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# i-Lynx OS 1.03 (Noble Modern Edition) - LXQt Desktop Environment Launcher
# ==============================================================================

set -e

# Terminal Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

CONTAINER_NAME="iLynxOS"

usage() {
    echo -e "${BOLD}Usage:${NC} $(basename "$0") [OPTIONS]

${BOLD}Options:${NC}
  -d, --desktop    Launch i-Lynx OS with LXQt Desktop Environment (Default)
  -c, --cli        Start an interactive terminal session inside i-Lynx OS
  -k, --kill       Stop running i-Lynx OS, Termux-X11 and PulseAudio sessions
  -h, --help       Show this help message"
    exit 0
}

cleanup_sessions() {
    echo -e "${YELLOW}[*] Cleaning up stale sessions and locks...${NC}"
    pkill -9 -f "com.termux.x11" 2>/dev/null || true
    pkill -9 -f "termux-x11" 2>/dev/null || true
    pkill -9 -f "lxqt" 2>/dev/null || true
    pkill -9 -f "pcmanfm-qt" 2>/dev/null || true
    pkill -9 -f "openbox" 2>/dev/null || true
    pkill -9 -f "xfce" 2>/dev/null || true
    pkill -9 -f "xfwm4" 2>/dev/null || true
    pkill -9 -f "dbus" 2>/dev/null || true
    pulseaudio --kill 2>/dev/null || true
    pkill -9 -f "pulseaudio" 2>/dev/null || true

    # Clean display and socket locks
    rm -rf "$PREFIX"/tmp/.X11-unix "$PREFIX"/tmp/.X0-lock 2>/dev/null || true
    rm -rf /tmp/.X11-unix /tmp/.X0-lock 2>/dev/null || true
    rm -rf "${TMPDIR:-/tmp}"/.X11-unix "${TMPDIR:-/tmp}"/.X0-lock 2>/dev/null || true
    rm -rf ~/.dbus 2>/dev/null || true
    rm -f "$PREFIX"/var/lib/proot-distro/locks/"${CONTAINER_NAME}".lock 2>/dev/null || true
}

# Handle arguments
MODE="desktop"
case "$1" in
    -h|--help)
        usage
        ;;
    -k|--kill|--stop)
        cleanup_sessions
        termux-wake-unlock 2>/dev/null || true
        echo -e "${GREEN}[+] Cleaned up all i-Lynx OS sessions.${NC}"
        exit 0
        ;;
    -c|--cli)
        MODE="cli"
        ;;
    -d|--desktop|"")
        MODE="desktop"
        ;;
    *)
        echo -e "${RED}[!] Unknown option: $1${NC}"
        usage
        ;;
esac

# Check container existence
if ! proot-distro list 2>/dev/null | grep -q "$CONTAINER_NAME"; then
    echo -e "${RED}[!] Container '$CONTAINER_NAME' is not installed in proot-distro.${NC}"
    echo -e "${CYAN}[*] Please run install.sh first to install i-Lynx OS.${NC}"
    exit 1
fi

if [ "$MODE" = "cli" ]; then
    echo -e "${GREEN}[*] Launching i-Lynx OS CLI session...${NC}"
    proot-distro login "$CONTAINER_NAME"
    exit 0
fi

# 1. Acquire Wake Lock to prevent Android doze/sleep
termux-wake-lock 2>/dev/null || true

# Setup exit trap to unlock and clean up
trap 'echo -e "\n${YELLOW}[*] Shutting down i-Lynx OS...${NC}"; cleanup_sessions; termux-wake-unlock 2>/dev/null || true; echo -e "${GREEN}[+] Session finished.${NC}"' EXIT INT TERM

# 2. Pre-cleanup
cleanup_sessions

# 3. Start PulseAudio server
echo -e "${CYAN}[*] Starting PulseAudio sound server...${NC}"
pulseaudio --start --exit-idle-time=-1 2>/dev/null || true
pactl load-module module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1 >/dev/null 2>&1 || true

# 4. Start Termux-X11 Display Server
echo -e "${CYAN}[*] Starting Termux-X11 display server...${NC}"
termux-x11 :0 -ac &

# Wait for X11 socket to become ready
X11_READY=0
for i in $(seq 1 30); do
    if [ -S "$PREFIX/tmp/.X11-unix/X0" ] || [ -S "/tmp/.X11-unix/X0" ]; then
        X11_READY=1
        break
    fi
    sleep 0.1
done

if [ "$X11_READY" -ne 1 ]; then
    echo -e "${YELLOW}[!] Warning: X11 socket wait timed out, attempting to continue...${NC}"
fi
sleep 0.5

# Bring Termux:X11 app to foreground
echo -e "${CYAN}[*] Launching Termux:X11 app...${NC}"
am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1 || true
sleep 0.5

# 5. Boot LXQt Desktop Environment inside PRoot
echo -e "${GREEN}${BOLD}[i-Lynx OS] Booting LXQt Modern Desktop Environment...${NC}"
proot-distro login "$CONTAINER_NAME" --shared-tmp -- bash -c '
    export DISPLAY=:0
    export TMPDIR=/tmp
    export PULSE_SERVER=127.0.0.1
    export LIBGL_ALWAYS_SOFTWARE=1
    export MOZ_ENABLE_WAYLAND=0
    export GDK_BACKEND=x11
    mkdir -p -m 700 /tmp/runtime-root
    export XDG_RUNTIME_DIR=/tmp/runtime-root

    # Clean up DBus locks
    rm -rf /var/run/dbus/pid /run/dbus/pid
    dbus-uuidgen --ensure 2>/dev/null || true

    # Launch official LXQt Desktop Session
    exec dbus-launch --exit-with-session startlxqt
'
