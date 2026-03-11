package com.wififtp.server.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

// ─── Server Config (DataStore) ──────────────────────────────────────────────
data class ServerConfig(
    val port: Int         = 2121,
    val username: String  = "admin",
    val password: String  = "admin123",
    val allowAnonymous: Boolean = false,
    val requireAuth: Boolean    = true,
    val autoStartOnWifi: Boolean = false,
    val runInBackground: Boolean = true,
    val rootPath: String  = "",   // filled at runtime with filesDir
)

// ─── Server State ───────────────────────────────────────────────────────────
data class ServerState(
    val isRunning: Boolean    = false,
    val isStarting: Boolean   = false,
    val ipAddress: String     = "",
    val port: Int             = 2121,
    val connectedClients: List<ConnectedClient> = emptyList(),
)

// ─── Connected Client ───────────────────────────────────────────────────────
data class ConnectedClient(
    val id: String,
    val ipAddress: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val bytesUploaded: Long   = 0L,
    val bytesDownloaded: Long = 0L,
    val currentFile: String?  = null,
    val transferSpeed: Long   = 0L,  // bytes/sec
)

// ─── Transfer Log (Room entity) ─────────────────────────────────────────────
@Entity(tableName = "transfer_logs")
data class TransferLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filename: String,
    val type: TransferType,
    val sizeBytes: Long,
    val clientIp: String,
    val status: TransferStatus,
    val progress: Int = 0,            // 0–100
    val speedBytesPerSec: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis(),
    val completedMs: Long? = null,
)

enum class TransferType { UPLOAD, DOWNLOAD }
enum class TransferStatus { ACTIVE, DONE, ERROR }

// ─── DAO ────────────────────────────────────────────────────────────────────
@Dao
interface TransferLogDao {
    @Query("SELECT * FROM transfer_logs ORDER BY timestampMs DESC LIMIT 200")
    fun getAllLogs(): Flow<List<TransferLog>>

    @Query("SELECT * FROM transfer_logs WHERE status = 'ACTIVE'")
    fun getActiveLogs(): Flow<List<TransferLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: TransferLog): Long

    @Update
    suspend fun update(log: TransferLog)

    @Query("UPDATE transfer_logs SET status = 'DONE', progress = 100, completedMs = :now WHERE id = :id")
    suspend fun markDone(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE transfer_logs SET status = 'ERROR' WHERE id = :id")
    suspend fun markError(id: Long)

    @Query("UPDATE transfer_logs SET progress = :progress, speedBytesPerSec = :speed WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int, speed: Long)

    @Query("DELETE FROM transfer_logs WHERE status != 'ACTIVE'")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM transfer_logs WHERE status = 'ACTIVE'")
    fun activeCount(): Flow<Int>
}

// ─── Database ───────────────────────────────────────────────────────────────
@Database(entities = [TransferLog::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transferLogDao(): TransferLogDao
}

class Converters {
    @TypeConverter fun fromTransferType(v: TransferType) = v.name
    @TypeConverter fun toTransferType(v: String) = TransferType.valueOf(v)
    @TypeConverter fun fromTransferStatus(v: TransferStatus) = v.name
    @TypeConverter fun toTransferStatus(v: String) = TransferStatus.valueOf(v)
}
