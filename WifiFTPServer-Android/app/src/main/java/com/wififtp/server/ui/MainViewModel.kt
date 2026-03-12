package com.wififtp.server.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wififtp.server.data.*
import com.wififtp.server.service.FtpEngine
import com.wififtp.server.service.FtpForegroundService
import com.wififtp.server.util.NetworkUtils
import com.wififtp.server.util.QrUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val serverState: ServerState = ServerState(),
    val config: ServerConfig = ServerConfig(),
    val transfers: List<TransferLog> = emptyList(),
    val qrBitmap: Bitmap? = null,
    val snackbarMessage: String? = null,
    val isWifiConnected: Boolean = false,
)


class MainViewModel(
    private val context: Context,
    private val ftpEngine: FtpEngine,
    private val settingsRepo: SettingsRepository,
    private val transferLogDao: TransferLogDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Observe config
        viewModelScope.launch {
            settingsRepo.config.collect { config ->
                _uiState.update { it.copy(config = config) }
            }
        }
        // Observe server running state
        viewModelScope.launch {
            ftpEngine.isRunning.collect { running ->
                val ip = if (running) NetworkUtils.getWifiIpAddress(context) else ""
                val port = _uiState.value.config.port
                val qr = if (running) generateQr(ip, port) else null
                _uiState.update { state ->
                    state.copy(
                        serverState = state.serverState.copy(
                            isRunning = running,
                            isStarting = false,
                            ipAddress = ip,
                            port = port,
                        ),
                        qrBitmap = qr,
                    )
                }
            }
        }
        // Observe connected clients
        viewModelScope.launch {
            ftpEngine.clients.collect { clients ->
                _uiState.update { it.copy(serverState = it.serverState.copy(connectedClients = clients)) }
            }
        }
        // Observe transfer logs
        viewModelScope.launch {
            transferLogDao.getAllLogs().collect { logs ->
                _uiState.update { it.copy(transfers = logs) }
            }
        }
        // WiFi state
        viewModelScope.launch {
            _uiState.update { it.copy(isWifiConnected = NetworkUtils.isWifiConnected(context)) }
        }
    }

    // ── Server Controls ───────────────────────────────────────────────────
    fun startServer() {
        if (_uiState.value.serverState.isRunning || _uiState.value.serverState.isStarting) return
        _uiState.update { it.copy(serverState = it.serverState.copy(isStarting = true)) }
        context.startForegroundService(FtpForegroundService.startIntent(context))
    }

    fun stopServer() {
        context.startService(FtpForegroundService.stopIntent(context))
        showSnackbar("Server stopped")
    }

    // ── Settings ──────────────────────────────────────────────────────────
    fun updatePort(port: Int) = viewModelScope.launch {
        if (port in 1025..65534) settingsRepo.updatePort(port)
        else showSnackbar("Port must be between 1025 and 65534")
    }
    fun updateUsername(v: String) = viewModelScope.launch { settingsRepo.updateUsername(v) }
    fun updatePassword(v: String) = viewModelScope.launch { settingsRepo.updatePassword(v) }
    fun updateAllowAnonymous(v: Boolean) = viewModelScope.launch { settingsRepo.updateAllowAnonymous(v) }
    fun updateRequireAuth(v: Boolean) = viewModelScope.launch { settingsRepo.updateRequireAuth(v) }
    fun updateAutoStart(v: Boolean) = viewModelScope.launch { settingsRepo.updateAutoStart(v) }
    fun updateRunBackground(v: Boolean) = viewModelScope.launch { settingsRepo.updateRunBackground(v) }

    // ── Transfer logs ─────────────────────────────────────────────────────
    fun clearTransferHistory() = viewModelScope.launch { transferLogDao.clearHistory() }

    // ── QR ────────────────────────────────────────────────────────────────
    private fun generateQr(ip: String, port: Int): Bitmap? {
        if (ip.isEmpty() || ip == "0.0.0.0") return null
        return QrUtils.generateQrBitmap(NetworkUtils.ftpUrl(ip, port))
    }

    // ── Snackbar ──────────────────────────────────────────────────────────
    fun showSnackbar(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }
    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    val ftpUrl get() = NetworkUtils.ftpUrl(
        _uiState.value.serverState.ipAddress,
        _uiState.value.serverState.port,
    )
}

class MainViewModelFactory(
    private val context: Context,
    private val ftpEngine: FtpEngine,
    private val settingsRepo: SettingsRepository,
    private val transferLogDao: TransferLogDao,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainViewModel(context, ftpEngine, settingsRepo, transferLogDao) as T
}
