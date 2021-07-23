package com.kauel.kuploader2.api.login

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "login")
data class Login(
    @SerializedName("accessToken")
    @PrimaryKey val accessToken: String,
    @SerializedName("token")
    val token: Token
)