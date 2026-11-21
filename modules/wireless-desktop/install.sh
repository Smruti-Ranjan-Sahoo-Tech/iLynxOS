#!/bin/bash
# =======================================================
# iLynxOS Wireless Desktop Dependency Installer
# Installs vncserver (tigervnc / tightvnc), x11vnc, xvfb, openssl, python3
# =======================================================
set -e

echo "[iLynxOS Wireless] Initializing Wireless Desktop components..."
mkdir -p /root/.vnc /root/.xpra /root/.ilynxos/ssl /var/log /var/run /usr/local/bin 2>/dev/null || true

# 1. Check and install vncserver (TigerVNC / TightVNC)
if ! command -v vncserver >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing tigervnc-standalone-server..."
    apt-get update -y || true
    apt-get install -y --no-install-recommends tigervnc-standalone-server tigervnc-common || \
    apt-get install -y --no-install-recommends tightvncserver || true
else
    echo "[iLynxOS Wireless] vncserver is already installed."
fi

# 2. Check and install x11vnc & xvfb for screen mirroring and fallbacks
if ! command -v x11vnc >/dev/null 2>&1 || ! command -v Xvfb >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing x11vnc and xvfb..."
    apt-get install -y --no-install-recommends x11vnc xvfb || true
else
    echo "[iLynxOS Wireless] x11vnc and xvfb are already installed."
fi

# 3. Check and install openssl for TLS encryption
if ! command -v openssl >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing openssl..."
    apt-get install -y --no-install-recommends openssl || true
fi

# 4. Generate self-signed TLS certificate for Web Mode
if [ ! -f "/root/.ilynxos/ssl/web.pem" ]; then
    echo "[iLynxOS Wireless] Generating 2048-bit RSA TLS certificate for Web Mode..."
    openssl req -new -newkey rsa:2048 -days 365 -nodes -x509 \
        -subj "/C=US/ST=iLynxOS/L=Local/O=iLynxOS/CN=iLynxOS-Web" \
        -keyout /root/.ilynxos/ssl/web.pem -out /root/.ilynxos/ssl/web.pem 2>/dev/null || true
    chmod 600 /root/.ilynxos/ssl/web.pem 2>/dev/null || true
    echo "[iLynxOS Wireless] TLS certificate generated."
fi

# 5. Check and install python3 and websockify dependencies
if ! command -v python3 >/dev/null 2>&1; then
    echo "[iLynxOS Wireless] Installing python3..."
    apt-get install -y --no-install-recommends python3 || true
fi

# 6. Ensure default xstartup exists
if [ ! -f "/root/.vnc/xstartup" ]; then
    cat << 'EOF' > /root/.vnc/xstartup
#!/bin/bash
unset SESSION_MANAGER
unset DBUS_SESSION_BUS_ADDRESS
export DISPLAY=:1

if command -v startlxqt >/dev/null 2>&1; then
    exec startlxqt
elif command -v openbox-session >/dev/null 2>&1; then
    exec openbox-session
else
    xsetroot -solid "#1e1e2e" 2>/dev/null || true
    if command -v openbox >/dev/null 2>&1; then
        openbox &
    fi
    if command -v pcmanfm-qt >/dev/null 2>&1; then
        pcmanfm-qt --desktop --profile=lxqt &
    fi
    if command -v lxqt-panel >/dev/null 2>&1; then
        lxqt-panel &
    elif command -v xfce4-panel >/dev/null 2>&1; then
        xfce4-panel --sm-client-disable --disable-wm-check &
    fi
    wait
fi
EOF
    chmod +x /root/.vnc/xstartup
fi

echo "[iLynxOS Wireless] Dependencies installation complete."
