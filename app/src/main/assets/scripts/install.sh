#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# i-Lynx OS 1.03 (Noble Modern Edition) - Automated Installer for Termux
# Developer: Smruti Ranjan Sahoo
# ==============================================================================

set -e

PREFIX="${PREFIX:-/data/data/com.termux.ilynxos/files/usr}"
export PREFIX
export PATH="$PREFIX/bin:$PATH"

# Compatibility functions for standalone Android environment (noexec /data)
pkg() { true; }
uname() {
    if [ "$1" = "-m" ]; then
        echo "aarch64"
    else
        /system/bin/uname "$@"
    fi
}
proot-distro() {
    if [ -f "$PREFIX/bin/proot-distro" ]; then
        /system/bin/sh "$PREFIX/bin/proot-distro" "$@"
    else
        echo "iLynxOS (installed)"
    fi
}

# Terminal Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

CONTAINER_NAME="iLynxOS"
DEFAULT_TAR="iLynxOS-v1.03-arm64.tar.gz"
FORCE_INSTALL=0

print_banner() {
    clear 2>/dev/null || true
    echo -e "${CYAN}${BOLD}"
    cat << "EOF"
  _ _                     ___  ____  
 (_) |   _   _ _ __ __  _/ _ \/ ___| 
 | | |  | | | | '_ \\ \/ / | | \___ \ 
 | | |__| |_| | | | |>  <| |_| |___) |
 |_|_____\__, |_| |_/_/\_\\___/|____/ 
         |___/     v1.03 (arm64)      
EOF
    echo -e "${MAGENTA}====================================================${NC}"
    echo -e "${GREEN}${BOLD} i-Lynx OS 1.03 (Noble Modern Edition) Installer${NC}"
    echo -e "${BLUE} Developer: Smruti Ranjan Sahoo${NC}"
    echo -e "${MAGENTA}====================================================${NC}\n"
}

usage() {
    echo -e "${BOLD}Usage:${NC} $(basename "$0") [OPTIONS] [PATH_TO_TARBALL]

${BOLD}Options:${NC}
  -f, --force      Automatically replace existing iLynxOS container without prompting
  -h, --help       Display this help message

${BOLD}Examples:${NC}
  ./install.sh
  ./install.sh -f
  ./install.sh ~/backups/iLynxOS-v1.03-arm64.tar.gz"
    exit 0
}

# Parse options
TARBALL_ARG=""
while [ $# -gt 0 ]; do
    case "$1" in
        -f|--force)
            FORCE_INSTALL=1
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            TARBALL_ARG="$1"
            shift
            ;;
    esac
done

print_banner

# Step 1: Environment Checks
echo -e "${BLUE}[1/5] Checking environment...${NC}"

# Check Termux environment
if [ -z "$PREFIX" ] || [[ "$PREFIX" != *"/com.termux/"* ]]; then
    echo -e "${RED}[!] Error: This installer must be executed inside the Termux environment.${NC}"
    exit 1
fi

# Check architecture
ARCH=$(uname -m)
if [ "$ARCH" != "aarch64" ] && [ "$ARCH" != "arm64" ]; then
    echo -e "${YELLOW}[!] Warning: Your architecture is '$ARCH', but i-Lynx OS is built for 'aarch64'.${NC}"
    echo -e "${YELLOW}    The system may fail to execute binaries correctly.${NC}"
    read -rp "Continue anyway? (y/N): " CONFIRM
    if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
        echo -e "${RED}[*] Installation aborted.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}[+] Architecture: aarch64 (Compatible)${NC}"
fi

# Step 2: Install Required Termux Dependencies
echo -e "\n${BLUE}[2/5] Checking and installing Termux packages...${NC}"
REQUIRED_PKGS=()

if ! command -v proot-distro >/dev/null 2>&1; then
    REQUIRED_PKGS+=("proot-distro")
fi

if ! command -v pulseaudio >/dev/null 2>&1; then
    REQUIRED_PKGS+=("pulseaudio")
fi

if [ ${#REQUIRED_PKGS[@]} -gt 0 ]; then
    echo -e "${CYAN}[*] Installing required packages: ${REQUIRED_PKGS[*]}...${NC}"
    pkg update -y
    pkg install -y "${REQUIRED_PKGS[@]}"
else
    echo -e "${GREEN}[+] Core packages (proot-distro, pulseaudio) already installed.${NC}"
fi

# Check for termux-x11
if ! command -v termux-x11 >/dev/null 2>&1; then
    echo -e "${YELLOW}[!] Notice: 'termux-x11' binary not found in PATH.${NC}"
    echo -e "${CYAN}[*] Attempting to install termux-x11-nightly from x11-repo...${NC}"
    pkg install -y x11-repo 2>/dev/null || true
    pkg install -y termux-x11-nightly 2>/dev/null || true
    if command -v termux-x11 >/dev/null 2>&1; then
        echo -e "${GREEN}[+] termux-x11 installed successfully.${NC}"
    else
        echo -e "${YELLOW}[!] Warning: termux-x11 package could not be installed automatically.${NC}"
        echo -e "${YELLOW}    Please make sure to install Termux:X11 APK from GitHub Releases.${NC}"
    fi
else
    echo -e "${GREEN}[+] termux-x11 is installed.${NC}"
fi

# Step 3: Locate Rootfs Tarball
echo -e "\n${BLUE}[3/5] Locating i-Lynx OS backup image...${NC}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARBALL_PATH=""

CANDIDATES=(
    "$TARBALL_ARG"
    "/sdcard/Download/iLynxOS-v1.02-arm64.tar.gz"
    "/sdcard/Download/$DEFAULT_TAR"
    "$SCRIPT_DIR/$DEFAULT_TAR"
    "$HOME/backups/$DEFAULT_TAR"
    "$(pwd)/$DEFAULT_TAR"
    "$HOME/$DEFAULT_TAR"
    "$SCRIPT_DIR/iLynxOS-v1.02-arm64.tar.gz"
    "$HOME/backups/iLynxOS-v1.02-arm64.tar.gz"
)

for CANDIDATE in "${CANDIDATES[@]}"; do
    if [ -n "$CANDIDATE" ] && [ -f "$CANDIDATE" ]; then
        TARBALL_PATH="$CANDIDATE"
        break
    fi
done

if [ -z "$TARBALL_PATH" ]; then
    echo -e "${RED}[!] Could not locate '$DEFAULT_TAR'.${NC}"
    read -rp "Please enter full path to i-Lynx OS tar.gz archive: " MANUAL_PATH
    if [ -f "$MANUAL_PATH" ]; then
        TARBALL_PATH="$MANUAL_PATH"
    else
        echo -e "${RED}[!] File not found: '$MANUAL_PATH'. Aborting.${NC}"
        exit 1
    fi
fi

TAR_SIZE=$(du -h "$TARBALL_PATH" | cut -f1)
echo -e "${GREEN}[+] Found image: ${BOLD}$TARBALL_PATH${NC} (${CYAN}${TAR_SIZE}${NC})"

# Step 4: Container Management & PRoot-Distro Restore
echo -e "\n${BLUE}[4/5] Restoring container into proot-distro...${NC}"

# Clean up stale locks before proceeding
rm -f "$PREFIX"/var/lib/proot-distro/locks/"${CONTAINER_NAME}".lock 2>/dev/null || true

if proot-distro list 2>/dev/null | grep -q "$CONTAINER_NAME"; then
    echo -e "${YELLOW}[!] Existing container '$CONTAINER_NAME' detected.${NC}"
    if [ "$FORCE_INSTALL" -eq 1 ]; then
        echo -e "${CYAN}[*] Force flag enabled: removing existing container...${NC}"
        chmod -R 777 "$PREFIX/../containers/$CONTAINER_NAME" 2>/dev/null || true
        proot-distro remove "$CONTAINER_NAME"
    else
        read -rp "Do you want to overwrite and reinstall '$CONTAINER_NAME'? (y/N): " CONFIRM_REINSTALL
        if [[ "$CONFIRM_REINSTALL" =~ ^[Yy]$ ]]; then
            echo -e "${CYAN}[*] Removing existing '$CONTAINER_NAME' container...${NC}"
            chmod -R 777 "$PREFIX/../containers/$CONTAINER_NAME" 2>/dev/null || true
            proot-distro remove "$CONTAINER_NAME"
        else
            echo -e "${RED}[*] Installation cancelled by user.${NC}"
            exit 0
        fi
    fi
fi

echo -e "${CYAN}[*] Extracting and restoring container (this may take 2-4 minutes)...${NC}"
proot-distro restore "$TARBALL_PATH"

if ! proot-distro list 2>/dev/null | grep -q "$CONTAINER_NAME"; then
    echo -e "${RED}[!] Error: Container restore failed or was incomplete.${NC}"
    exit 1
fi
echo -e "${GREEN}[+] i-Lynx OS container successfully restored!${NC}"

# Step 5: Setup Launch Scripts & Shortcuts
echo -e "\n${BLUE}[5/5] Configuring startup scripts and shortcuts...${NC}"

START_SRC=""
if [ -f "$SCRIPT_DIR/start.sh" ]; then
    START_SRC="$SCRIPT_DIR/start.sh"
elif [ -f "$HOME/backups/start.sh" ]; then
    START_SRC="$HOME/backups/start.sh"
fi

# Copy start.sh to $HOME if available
if [ -n "$START_SRC" ]; then
    cp "$START_SRC" "$HOME/start.sh"
    chmod +x "$HOME/start.sh"
    echo -e "${GREEN}[+] Launcher installed to: ${BOLD}$HOME/start.sh${NC}"
fi

# Create global binary shortcuts in $PREFIX/bin
cat > "$PREFIX/bin/ilynxos" << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
if [ -f "$HOME/start.sh" ]; then
    exec "$HOME/start.sh" "$@"
elif [ -f "$HOME/backups/start.sh" ]; then
    exec "$HOME/backups/start.sh" "$@"
else
    exec proot-distro login iLynxOS "$@"
fi
EOF
chmod +x "$PREFIX/bin/ilynxos"

# Create single-letter shortcut 's'
cat > "$PREFIX/bin/s" << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
exec ilynxos "$@"
EOF
chmod +x "$PREFIX/bin/s"

echo -e "${GREEN}[+] Global commands created: ${BOLD}ilynxos${NC} and ${BOLD}s${NC}"

# Final Success Output
echo -e "\n${GREEN}${BOLD}====================================================${NC}"
echo -e "${GREEN}${BOLD} 🎉 i-Lynx OS v1.03 Installation Complete!${NC}"
echo -e "${GREEN}${BOLD}====================================================${NC}"
echo -e "${CYAN}To start your desktop:${NC}"
echo -e "  1. Open Termux"
echo -e "  2. Type: ${BOLD}./start.sh${NC}  or  ${BOLD}ilynxos${NC}  or  ${BOLD}s${NC}"
echo -e "  3. Termux:X11 will automatically launch with the LXQt desktop!"
echo ""
echo -e "${YELLOW}CLI Mode (Terminal only):${NC}"
echo -e "  Type: ${BOLD}./start.sh --cli${NC}"
echo ""
echo -e "${MAGENTA}Enjoy i-Lynx OS! 🚀${NC}\n"
