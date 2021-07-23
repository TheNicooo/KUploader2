package com.kauel.kuploader2.ui.login


import androidx.lifecycle.*
import com.kauel.kuploader2.api.login.Login
import com.kauel.kuploader2.data.ServerDao
import com.kauel.kuploader2.data.repository.Repository
import com.kauel.kuploader2.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: Repository,
    private val serverDao: ServerDao
) : ViewModel() {

    //API
    private val loginMutableLiveData = MutableLiveData<Resource<Login>>()
    val loginLiveData: LiveData<Resource<Login>> = loginMutableLiveData

    fun login(url: String, email: String, password: String) =
        viewModelScope.launch {
            repository.login(url, email, password).collect {
                loginMutableLiveData.value = it
            }
        }

    private val serverTask = serverDao.getAllServer()
    val servers = serverTask.asLiveData()
}