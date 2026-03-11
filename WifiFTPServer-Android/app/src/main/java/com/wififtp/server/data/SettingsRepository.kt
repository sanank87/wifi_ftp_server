package com.wififtp.server.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ftp_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val PORT            = intPreferencesKey("port")
        val USERNAME        = stringPreferencesKey("username")
        val PASSWORD        = stringPreferencesKey("password")
        val ALLOW_ANON      = booleanPreferencesKey("allow_anonymous")
        val REQUIRE_AUTH    = booleanPreferencesKey("require_auth")
        val AUTO_START      = booleanPreferencesKey("auto_start_wifi")
        val RUN_BACKGROUND  = booleanPreferencesKey("run_background")
    }

    val config: Flow<ServerConfig> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            ServerConfig(
                port            = prefs[Keys.PORT] ?: 2121,
                username        = prefs[Keys.USERNAME] ?: "admin",
                password        = prefs[Keys.PASSWORD] ?: "admin123",
                allowAnonymous  = prefs[Keys.ALLOW_ANON] ?: false,
                requireAuth     = prefs[Keys.REQUIRE_AUTH] ?: true,
                autoStartOnWifi = prefs[Keys.AUTO_START] ?: false,
                runInBackground = prefs[Keys.RUN_BACKGROUND] ?: true,
                rootPath        = context.getExternalFilesDir(null)?.absolutePath
                    ?: context.filesDir.absolutePath,
            )
        }

    suspend fun updatePort(port: Int) =
        context.dataStore.edit { it[Keys.PORT] = port }

    suspend fun updateUsername(username: String) =
        context.dataStore.edit { it[Keys.USERNAME] = username }

    suspend fun updatePassword(password: String) =
        context.dataStore.edit { it[Keys.PASSWORD] = password }

    suspend fun updateAllowAnonymous(allow: Boolean) =
        context.dataStore.edit { it[Keys.ALLOW_ANON] = allow }

    suspend fun updateRequireAuth(require: Boolean) =
        context.dataStore.edit { it[Keys.REQUIRE_AUTH] = require }

    suspend fun updateAutoStart(auto: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_START] = auto }

    suspend fun updateRunBackground(bg: Boolean) =
        context.dataStore.edit { it[Keys.RUN_BACKGROUND] = bg }
}
