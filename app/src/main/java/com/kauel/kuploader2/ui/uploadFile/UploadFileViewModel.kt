package com.kauel.kuploader2.ui.uploadFile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kauel.kuploader2.api.responseApi.ResponseFile
import com.kauel.kuploader2.data.repository.Repository
import com.kauel.kuploader2.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UploadFileViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val uploadFileMutableLiveData = MutableLiveData<Resource<List<ResponseFile>>>()
    val uploadLiveData: LiveData<Resource<List<ResponseFile>>> = uploadFileMutableLiveData

    fun uploadFile(url: String, token: String, listFile: List<File>) {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                repository.uploadImage(listFile, url, token).collect {
                    uploadFileMutableLiveData.postValue(it)
                }
            }
        }
    }

    var fileCount: Int = 0

}