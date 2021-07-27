package com.kauel.kuploader2.api.responseApi

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "response")
data class ResponseAPI(
    @SerializedName("status")
    @PrimaryKey val status: Boolean
)
