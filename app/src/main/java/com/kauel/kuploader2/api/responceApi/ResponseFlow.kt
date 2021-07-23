package com.kauel.kuploader2.api.responceApi

import androidx.room.Entity
import java.io.File

@Entity(tableName = "response_flow")
data class ResponseFlow(
    val success: ArrayList<File>,
    val error: ArrayList<File>
)
