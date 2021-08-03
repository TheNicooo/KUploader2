package com.kauel.kuploader2.ui.uploadFile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kauel.kuploader2.api.responseApi.ResponseAPI
import com.kauel.kuploader2.data.repository.Repository
import com.kauel.kuploader2.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class UploadFileViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val uploadFileMutableLiveData = MutableLiveData<Resource<ResponseAPI>>()
    val uploadLiveData: LiveData<Resource<ResponseAPI>> = uploadFileMutableLiveData

    fun uploadFile(url: String, token: String, file: MultipartBody.Part) {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                repository.uploadFile(file, url, token).collect {
                    uploadFileMutableLiveData.postValue(it)
                }
            }
        }
    }

}