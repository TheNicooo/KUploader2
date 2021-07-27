package com.kauel.kuploader2.api

import com.kauel.kuploader2.api.login.Login
import com.kauel.kuploader2.api.responseApi.ResponseAPI
import com.kauel.kuploader2.utils.Resource
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST()
    suspend fun userLogin(
        @Url url: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Login

    @Multipart
    @POST()
    suspend fun uploadImage(
        @Url url: String,
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): ResponseAPI
}