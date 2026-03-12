package com.wififtp.server.service

import android.util.Log
import com.wififtp.server.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.*
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File
import org.apache.ftpserver.FtpServer

private const val TAG = "FtpEngine"

class FtpEngine(private val transferLogDao: TransferLogDao) {
    private var ftpServer: FtpServer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _clients = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val clients: StateFlow<List<ConnectedClient>> = _clients.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // ── Start FTP Server ──────────────────────────────────────────────────
    suspend fun start(config: ServerConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (_isRunning.value) return@withContext Result.success(Unit)

            val rootDir = File(config.rootPath).also { it.mkdirs() }

            // ── User Manager ───────────────────────────────────────────────
            val userManager = PropertiesUserManagerImpl()

            // Authenticated user
            val user = BaseUser().apply {
                name          = config.username
                setPassword(config.password)
                homeDirectory = rootDir.absolutePath
                authorities   = listOf(WritePermission())
                maxIdleTime   = 300
            }
            user.enabled = true
            userManager.save(user)

            // Anonymous user (optional)
            if (config.allowAnonymous) {
                val anon = BaseUser().apply {
                    name          = "anonymous"
                    setPassword("")
                    homeDirectory = rootDir.absolutePath
                    authorities   = listOf(WritePermission())
                    maxIdleTime   = 120
                }
                anon.enabled = true
                userManager.save(anon)
            }

            // ── Listener ───────────────────────────────────────────────────
            val listenerFactory = ListenerFactory().apply {
                port = config.port
                isImplicitSsl = false
            }

            // ── Ftplet (event hooks) ───────────────────────────────────────
            val ftplet = object : DefaultFtplet() {
                override fun onConnect(session: FtpSession): FtpletResult {
                    val ip = session.clientAddress?.address?.hostAddress ?: "unknown"
                    Log.d(TAG, "Client connected: $ip")
                    addClient(session.sessionId.toString(), ip)
                    return FtpletResult.DEFAULT
                }

                override fun onDisconnect(session: FtpSession): FtpletResult {
                    val id = session.sessionId.toString()
                    removeClient(id)
                    return FtpletResult.DEFAULT
                }

                override fun onUploadStart(session: FtpSession, request: FtpRequest): FtpletResult {
                    val filename = request.argument ?: return FtpletResult.DEFAULT
                    val ip = session.clientAddress?.address?.hostAddress ?: "unknown"
                    scope.launch {
                        val logId = transferLogDao.insert(
                            TransferLog(
                                filename  = filename,
                                type      = TransferType.UPLOAD,
                                sizeBytes = 0L,
                                clientIp  = ip,
                                status    = TransferStatus.ACTIVE,
                            )
                        )
                        session.setAttribute("upload_log_$filename", logId)
                    }
                    return FtpletResult.DEFAULT
                }

                override fun onUploadEnd(session: FtpSession, request: FtpRequest): FtpletResult {
                    val filename = request.argument ?: return FtpletResult.DEFAULT
                    scope.launch {
                        val logId = session.getAttribute("upload_log_$filename") as? Long
                        logId?.let { transferLogDao.markDone(it) }
                    }
                    return FtpletResult.DEFAULT
                }

                override fun onDownloadStart(session: FtpSession, request: FtpRequest): FtpletResult {
                    val filename = request.argument ?: return FtpletResult.DEFAULT
                    val ip = session.clientAddress?.address?.hostAddress ?: "unknown"
                    scope.launch {
                        val logId = transferLogDao.insert(
                            TransferLog(
                                filename  = filename,
                                type      = TransferType.DOWNLOAD,
                                sizeBytes = 0L,
                                clientIp  = ip,
                                status    = TransferStatus.ACTIVE,
                            )
                        )
                        session.setAttribute("download_log_$filename", logId)
                    }
                    return FtpletResult.DEFAULT
                }

                override fun onDownloadEnd(session: FtpSession, request: FtpRequest): FtpletResult {
                    val filename = request.argument ?: return FtpletResult.DEFAULT
                    scope.launch {
                        val logId = session.getAttribute("download_log_$filename") as? Long
                        logId?.let { transferLogDao.markDone(it) }
                    }
                    return FtpletResult.DEFAULT
                }

                override fun onDeleteStart(session: FtpSession, request: FtpRequest): FtpletResult =
                    FtpletResult.DEFAULT

                override fun onMkdirStart(session: FtpSession, request: FtpRequest): FtpletResult =
                    FtpletResult.DEFAULT
            }

            // ── Assemble Server ────────────────────────────────────────────
            val serverFactory = FtpServerFactory().apply {
                addListener("default", listenerFactory.createListener())
                setUserManager(userManager)
                ftplets = mapOf("main" to ftplet)
            }

            ftpServer = serverFactory.createServer().also { it.start() }
            _isRunning.value = true
            Log.i(TAG, "FTP Server started on port ${config.port}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FTP server", e)
            _isRunning.value = false
            Result.failure(e)
        }
    }

    // ── Stop FTP Server ───────────────────────────────────────────────────
    fun stop() {
        try {
            ftpServer?.let { if (!it.isStopped) it.stop() }
            ftpServer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
        _isRunning.value = false
        _clients.value = emptyList()
        Log.i(TAG, "FTP Server stopped")
    }

    // ── Client tracking ───────────────────────────────────────────────────
    private fun addClient(sessionId: String, ip: String) {
        val client = ConnectedClient(id = sessionId, ipAddress = ip)
        _clients.value = _clients.value + client
    }

    private fun removeClient(sessionId: String) {
        _clients.value = _clients.value.filter { it.id != sessionId }
    }

    fun isRunning() = _isRunning.value
}

// ─── Simple in-memory user manager ───────────────────────────────────────────
class PropertiesUserManagerImpl : UserManager {
    private val users = mutableMapOf<String, User>()

    override fun getUserByName(username: String): User? = users[username]
    override fun getAllUserNames(): Array<String> = users.keys.toTypedArray()
    override fun delete(username: String) { users.remove(username) }
    override fun save(user: User) { users[user.name] = user }
    override fun doesExist(username: String) = users.containsKey(username)
    override fun authenticate(auth: Authentication): User? {
        val username = try {
            auth.javaClass.getMethod("getUsername").invoke(auth) as? String
        } catch (_: Exception) { "anonymous" } ?: "anonymous"
        
        val password = try {
            auth.javaClass.getMethod("getPassword").invoke(auth) as? String
        } catch (_: Exception) { "" } ?: ""

        val user = users[username] ?: users["anonymous"] ?: return null
        val storedPass = (user as? BaseUser)?.getPassword() ?: ""
        return if (username == "anonymous" || storedPass == password) user else null
    }
    override fun getAdminName(): String = users.keys.firstOrNull() ?: "admin"
    override fun isAdmin(username: String): Boolean = username == getAdminName()
}
