package com.kauel.kuploader2.data.repository

import androidx.room.withTransaction
import com.kauel.kuploader2.api.ApiService
import com.kauel.kuploader2.api.responceApi.ResponseAPI
import com.kauel.kuploader2.api.server.Server
import com.kauel.kuploader2.data.AppDatabase
import com.kauel.kuploader2.utils.Resource
import com.kauel.kuploader2.utils.networkBoundResource
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.util.concurrent.Flow
import javax.inject.Inject

class Repository @Inject constructor(
    private val apiService: ApiService,
    private val appDatabase: AppDatabase
) {
    private val loginDao = appDatabase.loginDao()
    private val serverDao = appDatabase.serverDao()

    fun login(url: String, email: String, password: String) = networkBoundResource(
        databaseQuery = {
            loginDao.getAllLogin()
        },
        networkCall = {
            apiService.userLogin(url, email, password)
        },
        saveCallResult = {
            appDatabase.withTransaction {
                loginDao.deleteAllLogin()
                //loginDao.insertLogin(it)
            }
        }
    )

//    fun uploadFile(file: File, url: String, token: String): Flow<Resource<ResponseAPI>> {
//
//        return flow {
//
//            emit(Resource.Loading(null))
//
//            val requestFile: RequestBody =
//                RequestBody.create(MediaType.parse("multipart/form-data"), file)
//            val image = MultipartBody.Part.createFormData("file", file.name, requestFile)
//
//            when (val upload = apiService.uploadImage(url = url, token = token, image = image)) {
//                is Resource.Success(upload.data) -> emit(Resource.Success(upload.data))
//
//                is Resource.Error<T> -> emit(Resource.Error())
//            }
//        }
//    }

    suspend fun insertServer(server: Server) {
        appDatabase.withTransaction {
            serverDao.insert(server)
        }
    }

    suspend fun updateServer(server: Server) {
        appDatabase.withTransaction {
            serverDao.update(server)
        }
    }

    fun getServer(id: Int) {
        serverDao.getServer(id)
    }

    suspend fun deleteServer(id: Int) {
        serverDao.deleteServer(id)
    }
}