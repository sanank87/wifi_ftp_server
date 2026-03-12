package com.wififtp.server

import android.content.Context
import androidx.room.Room
import com.wififtp.server.data.AppDatabase
import com.wififtp.server.data.SettingsRepository
import com.wififtp.server.data.TransferLogDao
import com.wififtp.server.service.FtpEngine

object AppModule {
    private var db: AppDatabase? = null

    fun provideDatabase(ctx: Context): AppDatabase {
        return db ?: Room.databaseBuilder(ctx, AppDatabase::class.java, "ftp_server_db")
            .fallbackToDestructiveMigration()
            .build().also { db = it }
    }

    fun provideTransferLogDao(ctx: Context): TransferLogDao =
        provideDatabase(ctx).transferLogDao()

    fun provideFtpEngine(ctx: Context): FtpEngine =
        FtpEngine(provideTransferLogDao(ctx))

    fun provideSettingsRepository(ctx: Context): SettingsRepository =
        SettingsRepository(ctx)
}
