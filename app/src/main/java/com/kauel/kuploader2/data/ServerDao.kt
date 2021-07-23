package com.kauel.kuploader2.data

import androidx.room.*
import com.kauel.kuploader2.api.server.Server
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: Server)

    @Update
    suspend fun update(server: Server)

    @Query("SELECT * FROM server")
    fun getAllServer(): Flow<List<Server>>

    @Query("SELECT * FROM server WHERE id = :id")
    fun getServer(id: Int): Flow<Server>

    @Query("DELETE FROM server")
    suspend fun deleteAllServer()

    @Query("DELETE FROM server WHERE id = :id")
    suspend fun deleteServer(id: Int)

}