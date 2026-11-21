#!/bin/bash
# iLynxOS Wireless Desktop Dependency Installer
# Installs x11vnc, xvfb, openssl, python3, and sets up certificates
set -e

echo "[iLynxOS Wireless] Initializing Wireless Desktop components..."
mkdir -p /root/.vnc /root/.xpra /root/.ilynxos/ssl /var/log /var/run /usr/local/bin 2>/dev/null || true

# 1. Check and install x11vnc & xvfb for dedicated desktop sessions
if ! command -v x11vnc >/dev/null 2>&1 || ! command -v Xvfb >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing x11vnc and xvfb..."
    apt-get update -y
    apt-get install -y --no-install-recommends x11vnc xvfb
else
    echo "[iLynxOS Wireless] x11vnc and xvfb are already installed."
fi

# 2. Check and install openssl for TLS encryption
if ! command -v openssl >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing openssl..."
    apt-get install -y --no-install-recommends openssl
fi

# 3. Generate self-signed TLS certificate for Web Mode
if [ ! -f "/root/.ilynxos/ssl/web.pem" ]; then
    echo "[iLynxOS Wireless] Generating 2048-bit RSA TLS certificate for Web Mode..."
    openssl req -new -newkey rsa:2048 -days 365 -nodes -x509 \
        -subj "/C=US/ST=iLynxOS/L=Local/O=iLynxOS/CN=iLynxOS-Web" \
        -keyout /root/.ilynxos/ssl/web.pem -out /root/.ilynxos/ssl/web.pem 2>/dev/null || true
    chmod 600 /root/.ilynxos/ssl/web.pem 2>/dev/null || true
    echo "[iLynxOS Wireless] TLS certificate generated."
fi

# 4. Check and install python3 and websockify dependencies
if ! command -v python3 >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing python3..."
    apt-get install -y --no-install-recommends python3
fi

echo "[iLynxOS Wireless] Dependencies installation complete."
