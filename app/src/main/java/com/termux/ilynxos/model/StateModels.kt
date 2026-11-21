package com.termux.ilynxos.model

sealed class InstallationState {
    object Idle : InstallationState()
    object Checking : InstallationState()
    data class Extracting(val progress: Float, val currentFile: String, val bytesRead: Long, val totalBytes: Long) : InstallationState()
    data class RunningInstallScript(val logLine: String) : InstallationState()
    object Installed : InstallationState()
    data class Error(val message: String) : InstallationState()
}

sealed class SessionState {
    object Stopped : SessionState()
    data class Starting(val step: String) : SessionState()
    data class Running(val startTime: Long, val display: String = ":0") : SessionState()
    data class Error(val message: String) : SessionState()
}
