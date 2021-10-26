package com.kauel.kuploader2.api.responseApi

import com.google.gson.annotations.SerializedName


data class ResponseAPI(
    @SerializedName("status")
    val status: Boolean
)
