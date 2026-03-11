package com.wififtp.server

import android.content.Context
import androidx.room.Room
import com.wififtp.server.data.AppDatabase
import com.wififtp.server.data.TransferLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "ftp_server_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransferLogDao(db: AppDatabase): TransferLogDao = db.transferLogDao()
}
