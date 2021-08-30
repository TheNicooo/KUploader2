package com.kauel.kuploader2.ui.testUploadFile

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
class TestUploadFileViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val testUploadFileMutableLiveData = MutableLiveData<Resource<ResponseAPI>>()
    val testUploadLiveData: LiveData<Resource<ResponseAPI>> = testUploadFileMutableLiveData

    fun uploadTestFile(url: String, file: MultipartBody.Part) {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                repository.uploadTestFile(file, url).collect {
                    testUploadFileMutableLiveData.postValue(it)
                }
            }
        }
    }

}