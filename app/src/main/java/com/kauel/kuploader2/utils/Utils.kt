package com.kauel.kuploader2.utils

import android.media.ExifInterface
import android.os.Build
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

fun fileToMultipart(file: File): MultipartBody.Part {

    var imageMultipartBody: MultipartBody.Part? = null
        val requestFile: RequestBody =
            RequestBody.create(
                MediaType.parse("multipart/form-data"),
                file
            )
        val image =
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestFile
            )

        imageMultipartBody = image

    return imageMultipartBody!!
}

/**
 * Validate if it's a load of rgb or thermal image
 * */
private const val sizeMax = 1024

fun isThermalUpload(listFile: List<File>) : Boolean {

    if (listFile.size > 1) {

        val file1 = listFile[0]
        val file2 = listFile[1]

        val sizeKBFile1 = file1.length() / 1024
        val sizeKBFile2 = file2.length() / 1024

        if (sizeKBFile1 > sizeMax) {
            if (sizeKBFile2 < sizeMax) {
                return diffTime(file1, file2) <= 3
            }
        } else {
            if (sizeKBFile2 > sizeMax) {
                return diffTime(file1, file2) <= 3
            }
        }

    }

    return false
}

/**
 * Difference time between date1 and date 2
 * (File 1 and File 2)
 * */
private fun diffTime(file1: File, file2: File): Long {
    var dateFile1: Long? = null
    var dateFile2: Long? = null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val file1Exif = ExifInterface(file1)
        val file2Exif = ExifInterface(file2)
        dateFile1 = file1Exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.replace(":","")?.replace(" ", "")?.toLong()
        dateFile2 = file2Exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.replace(":","")?.replace(" ", "")?.toLong()
    }
    return dateFile1!! - dateFile2!!
}