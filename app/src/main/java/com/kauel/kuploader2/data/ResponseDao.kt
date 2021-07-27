package com.kauel.kuploader2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kauel.kuploader2.api.responseApi.ResponseAPI
import kotlinx.coroutines.flow.Flow

@Dao
interface ResponseDao {

    @Query("SELECT * FROM response")
    fun getAllResponse(): Flow<ResponseAPI>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun uploadFile(responseAPI: ResponseAPI)

    @Query("DELETE FROM response")
    suspend fun deleteAllResponse()
}