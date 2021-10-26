package com.kauel.kuploader2.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kauel.kuploader2.api.login.Login
import com.kauel.kuploader2.api.responseApi.ResponseAPI
import com.kauel.kuploader2.api.server.Server
import com.kauel.kuploader2.utils.Converters

@Database(
    entities = [Login::class,
        Server::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun loginDao(): LoginDao

    abstract fun serverDao(): ServerDao
}