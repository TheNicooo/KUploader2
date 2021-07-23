package com.kauel.kuploader2.ui.uploadFile

import androidx.lifecycle.*
import com.kauel.kuploader2.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UploadFileViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    fun uploadFile(url: String, token: String, listImage: ArrayList<File>) =
        viewModelScope.launch {
        }


}