# Contributing to iLynxOS

First off, thank you for considering contributing to **iLynxOS**! It's people like you that make open-source software empowering, resilient, and accessible to everyone around the world.

iLynxOS is built to democratize computing by transforming smartphones into full-fledged, standalone personal workstations. Whether you're writing code, fixing bugs, optimizing desktop performance, improving translations, or writing documentation, every single contribution is deeply appreciated.

---

## 📜 Code of Conduct

This project and everyone participating in it is governed by the [iLynxOS Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [s.r.sahoo370@gmail.com](mailto:s.r.sahoo370@gmail.com).

---

## 🚀 How Can I Contribute?

### 1. Reporting Bugs
Before submitting an issue, please search existing issues to see if the problem has already been reported.

When opening a bug report via GitHub Issues:
- Use our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.yml).
- Specify your **Device Model**, **Android Version**, and **iLynxOS APK Version**.
- Provide exact, step-by-step instructions to reproduce the problem.
- Include log output (from Logcat or the native Terminal/Logs card).

### 2. Suggesting Enhancements & Desktop Features
We welcome ideas for new desktop utilities, window manager features, hardware acceleration tweaks, and UI enhancements!
- Use our [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.yml).
- Explain why this feature would be useful to iLynxOS users and developers.
- Detail any specific technical considerations (e.g. PRoot compatibility, X11 server extensions).

### 3. Submitting Pull Requests
- Fork the repository and create your branch from `main`:
  ```bash
  git checkout -b feature/amazing-feature
  # or
  git checkout -b fix/issue-description
  ```
- Make your changes and test them locally on an Android device or emulator.
- Commit your changes using [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat: add PulseAudio sound redirection module`
  - `fix: resolve trackpad double-click drag event propagation`
  - `docs: update wireless desktop connection instructions`
  - `perf: optimize libXlorie buffer swapping on Adreno GPUs`
- Push your branch to GitHub and open a Pull Request against `main`.

---

## 🛠️ Development & Build Setup

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or later
- **JDK 17** (OpenJDK 17 recommended)
- **Android SDK Platform 35** and Build Tools `35.0.0`
- **Android NDK** (r26+ recommended for JNI/C++ modules)
- A connected physical Android device (ARM64 / Android 8.0+) or Android Virtual Device (AVD x86_64)

### Building the Project Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Smruti-Ranjan-Sahoo-Tech/iLynxOS.git
   cd iLynxOS
   ```

2. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Install on Device via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Launch Main Activity:**
   ```bash
   adb shell am start -n com.termux.ilynxos/.ui.MainActivity
   ```

---

## 🏗️ Architecture Guidelines

When developing for iLynxOS, keep in mind:
- **Zero Root Dependency**: iLynxOS operates entirely in user-space using `libproot.so` syscall interception. Never assume or require `su` root permissions.
- **In-Process Display Server**: The X11 display server (`libXlorie`) runs in-process inside the Android app on UNIX domain socket `:0` (and `:1` for virtual desktop remote displays).
- **Jetpack Compose UI**: All Android host UI is built modern, fluent, and edge-to-edge aware with Jetpack Compose Material 3.
- **Wireless Remote Subsystem**: VNC (`5901`) and HTML5 Web Desktop (`14500` via `websockify` + `noVNC`) are modularized in `com.termux.ilynxos.wireless`.

---

## 🌟 Spread the Word!
If you love iLynxOS and want to help it rank #1 and empower more people:
- ⭐ **Star the repo** on GitHub!
- 📢 Share it on Twitter/X, Reddit (`r/android`, `r/linux`, `r/termux`, `r/unixporn`), and Hacker News.
- 💬 Discuss ideas and collaborate in GitHub Discussions!
