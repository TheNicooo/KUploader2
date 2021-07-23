package com.kauel.kuploader2.api.server

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server")
data class Server (
    val name: String,
    val url: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)