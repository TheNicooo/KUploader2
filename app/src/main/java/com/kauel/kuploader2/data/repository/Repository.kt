package com.kauel.kuploader2.data.repository

import androidx.room.withTransaction
import com.kauel.kuploader2.api.ApiService
import com.kauel.kuploader2.api.responseApi.ResponseAPI
import com.kauel.kuploader2.api.responseApi.ResponseFile
import com.kauel.kuploader2.api.server.Server
import com.kauel.kuploader2.data.AppDatabase
import com.kauel.kuploader2.utils.Resource
import com.kauel.kuploader2.utils.fileToMultipart
import com.kauel.kuploader2.utils.isThermalUpload
import com.kauel.kuploader2.utils.networkBoundResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
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
                loginDao.insertLogin(it)
            }
        }
    )

    fun uploadTestImage(file: File, url: String): Flow<Resource<List<ResponseAPI>>> {
        return flow {
            emit(Resource.Loading<List<ResponseAPI>>())
            try {
                var num = 1
                val listMutable = mutableListOf<ResponseAPI>()
                while (num <= 10) {
                    val image = fileToMultipart(file)
                    val response = apiService.uploadTestImage(url, image)
                    listMutable.add(response)
                    emit(Resource.Loading<List<ResponseAPI>>(listMutable))
                    num++
                }
                emit(Resource.Success<List<ResponseAPI>>(listMutable))
            } catch (throwable: Throwable) {
                emit(Resource.Error<List<ResponseAPI>>(throwable))
            }
        }.flowOn(Dispatchers.IO)
    }

    fun uploadImage(
        listFile: List<File>,
        url: String,
        token: String
    ): Flow<Resource<List<ResponseFile>>> {
        return flow {
            emit(Resource.Loading<List<ResponseFile>>())
            val listMutable = mutableListOf<ResponseFile>()
            try {
                var response: ResponseAPI

                val sizeList = listFile.size
                var flagListFile = listFile
                var position = 1

                while (position <= sizeList) {

                    if (flagListFile.isNotEmpty()) {

                        if (isThermalUpload(flagListFile)) {
                            val rgbImage = fileToMultipart(flagListFile[0])
                            val thermalImage = fileToMultipart(flagListFile[1])
                            response =
                                apiService.uploadThermalImage(url, token, rgbImage, thermalImage)

                            listMutable.add(ResponseFile(flagListFile[0], response.status))
                            listMutable.add(ResponseFile(flagListFile[1], response.status))
                            flagListFile = flagListFile.drop(2)

                        } else {
                            val image = fileToMultipart(flagListFile[0])
                            response = apiService.uploadImage(url, token, image)

                            listMutable.add(ResponseFile(flagListFile[0], response.status))
                            flagListFile = flagListFile.drop(1)

                        }

                        emit(Resource.Loading<List<ResponseFile>>(listMutable))
                        position++

                    } else {
                        break
                    }
                }

                emit(Resource.Success<List<ResponseFile>>(listMutable))
            } catch (throwable: Throwable) {
                emit(Resource.Error<List<ResponseFile>>(throwable, listMutable))
            }
        }.flowOn(Dispatchers.IO)
    }

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