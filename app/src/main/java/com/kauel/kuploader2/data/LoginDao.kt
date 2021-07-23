package com.kauel.kuploader2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kauel.kuploader2.api.login.Login
import kotlinx.coroutines.flow.Flow

@Dao
interface LoginDao {

    @Query("SELECT * FROM login")
    fun getAllLogin(): Flow<Login>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogin(login: Login)

    @Query("DELETE FROM login")
    suspend fun deleteAllLogin()
}