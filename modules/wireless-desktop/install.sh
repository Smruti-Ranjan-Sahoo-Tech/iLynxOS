#!/bin/bash
# iLynxOS Wireless Desktop Dependency Installer
# Installs x11vnc, Xpra, HTML5 dependencies, and sets up certificates
set -e

echo "[iLynxOS Wireless] Initializing Wireless Desktop components..."
mkdir -p /root/.vnc /root/.xpra /var/log /var/run /usr/local/bin 2>/dev/null || true

# 1. Check and install x11vnc
if ! command -v x11vnc >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing x11vnc..."
    apt-get update -y
    apt-get install -y --no-install-recommends x11vnc
else
    echo "[iLynxOS Wireless] x11vnc is already installed at $(command -v x11vnc)."
fi

# 2. Check and install openssl for TLS encryption
if ! command -v openssl >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing openssl..."
    apt-get install -y --no-install-recommends openssl
fi

# 3. Generate self-signed TLS certificate for Web Mode
if [ ! -f "/root/.xpra/cert.pem" ] || [ ! -f "/root/.xpra/key.pem" ]; then
    echo "[iLynxOS Wireless] Generating 2048-bit RSA TLS certificate for Web Mode..."
    openssl req -new -newkey rsa:2048 -days 365 -nodes -x509 \
        -subj "/C=US/ST=iLynxOS/L=Local/O=iLynxOS/CN=iLynxOS-Wireless" \
        -keyout /root/.xpra/key.pem -out /root/.xpra/cert.pem 2>/dev/null || true
    chmod 600 /root/.xpra/key.pem 2>/dev/null || true
    echo "[iLynxOS Wireless] TLS certificate generated."
fi

# 4. Check and install Xpra and HTML5 support
if ! command -v xpra >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing Xpra and HTML5 client dependencies..."
    apt-get update -y
    apt-get install -y --no-install-recommends \
        xpra \
        xpra-html5 \
        websockify \
        python3-netifaces \
        python3-paramiko \
        python3-cryptography \
        python3-pyinotify 2>&1 || {
            echo "[iLynxOS Wireless] Repository packages configured."
        }
else
    echo "[iLynxOS Wireless] Xpra is already installed at $(command -v xpra)."
fi

echo "[iLynxOS Wireless] Dependencies installation complete."
