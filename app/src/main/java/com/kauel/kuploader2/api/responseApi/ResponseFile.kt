package com.kauel.kuploader2.api.responseApi

import java.io.File

data class ResponseFile(
    val file: File,
    val fileStatus: Boolean
)
