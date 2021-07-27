package com.kauel.kuploader2.ui.uploadFile

import androidx.lifecycle.*
import com.kauel.kuploader2.api.responseApi.ResponseAPI
import com.kauel.kuploader2.data.repository.Repository
import com.kauel.kuploader2.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UploadFileViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val uploadFileMutableLiveData = MutableLiveData<Resource<ResponseAPI>>()
    val uploadLiveData: LiveData<Resource<ResponseAPI>> = uploadFileMutableLiveData

    fun uploadFile(url: String, token: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            repository.uploadFile(file, url, token).collect {
                uploadFileMutableLiveData.value = it
            }
        }
    }

}