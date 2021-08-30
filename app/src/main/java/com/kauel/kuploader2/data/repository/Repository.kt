package com.kauel.kuploader2.data.repository

import androidx.room.withTransaction
import com.kauel.kuploader2.api.ApiService
import com.kauel.kuploader2.api.server.Server
import com.kauel.kuploader2.data.AppDatabase
import com.kauel.kuploader2.utils.networkBoundResource
import okhttp3.MultipartBody
import javax.inject.Inject

class Repository @Inject constructor(
    private val apiService: ApiService,
    private val appDatabase: AppDatabase
) {
    private val loginDao = appDatabase.loginDao()
    private val serverDao = appDatabase.serverDao()
    private val responseDao = appDatabase.responseDao()

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
                loginDao.insertLogin(it)
            }
        }
    )

//    fun uploadFileFlow(file: File, url: String, token: String): Flow<Resource<ResponseAPI>> {
//
//        return flow {
//
//            emit(Resource.Loading())
//
//            val requestFile: RequestBody =
//                RequestBody.create(MediaType.parse("multipart/form-data"), file)
//            val image = MultipartBody.Part.createFormData("file", file.name, requestFile)
//
//            when (val upload = apiService.uploadImage(url = url, token = token, image = image)) {
//                is Resource.Success -> emit(Resource.Success(upload))
//
//                is Result.Error -> emit(
//                    Resource.Error(
//                        throwable = Throwable.localizedMessage,
//                        data = upload
//                    )
//                )
//            }
//        }
//    }

//    suspend fun getCategories(offset : Int, limit : Int, country : String ): Flow<CustomResult<AlbumsResponse>>
//    return flow {
//        try {
//            emit(CustomResult.Loading(true))
//            val url = "$baseUrl$endpointAlbums?country=$country&limit=$limit&offset=$offset"
//            val response =
//                client.request<AlbumsResponse>(url) {
//                    method = HttpMethod.Get
//                    headers {
//                        append("Accept", "application/json")
//                        append("Authorization", "Bearer $authKey")
//                    }
//                }
//            emit(CustomResult.Success(response))
//        }catch (e : Exception){
//            emit(CustomResult.Error.RecoverableError(e))
//        }
//    }.flowOn(Dispatchers.IO)
//}

    fun uploadFile(file: MultipartBody.Part, url: String, token: String) = networkBoundResource(
        databaseQuery = {
            responseDao.getAllResponse()
        },
        networkCall = {
            apiService.uploadImage(url, token, file)
        },
        saveCallResult = {
            appDatabase.withTransaction {
                responseDao.deleteAllResponse()
                responseDao.uploadFile(it)
            }
        }
    )

    fun uploadTestFile(file: MultipartBody.Part, url: String) = networkBoundResource(
        databaseQuery = {
            responseDao.getAllResponse()
        },
        networkCall = {
            apiService.uploadTestImage(url, file)
        },
        saveCallResult = {
            appDatabase.withTransaction {
                responseDao.deleteAllResponse()
                responseDao.uploadFile(it)
            }
        }
    )

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