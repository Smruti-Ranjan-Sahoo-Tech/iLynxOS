#!/bin/bash
# iLynxOS Wireless Desktop Configuration Definitions

VNC_CONFIG_DIR="/root/.vnc"
XPRA_CONFIG_DIR="/root/.xpra"
LOG_DIR="/var/log"
RUN_DIR="/var/run"

DEFAULT_VNC_PORT=5900
DEFAULT_WEB_PORT=14500

VNC_PASSWD_FILE="$VNC_CONFIG_DIR/passwd"
XPRA_PASSWD_FILE="$XPRA_CONFIG_DIR/password.txt"
XPRA_CERT_FILE="$XPRA_CONFIG_DIR/cert.pem"
XPRA_KEY_FILE="$XPRA_CONFIG_DIR/key.pem"

VNC_PID_FILE="$RUN_DIR/x11vnc.pid"
WEB_PID_FILE="$RUN_DIR/xpra-web.pid"

VNC_LOG_FILE="$LOG_DIR/x11vnc.log"
WEB_LOG_FILE="$LOG_DIR/xpra-web.log"
