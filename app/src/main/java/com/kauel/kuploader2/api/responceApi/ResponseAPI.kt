package com.kauel.kuploader2.api.responceApi

import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(tableName = "response")
data class ResponseAPI(
    @SerializedName("status")
    val status: Boolean
)
