package com.kauel.kuploader2.ui.formServer

import androidx.lifecycle.*
import com.kauel.kuploader2.api.server.Server
import com.kauel.kuploader2.data.ServerDao
import com.kauel.kuploader2.data.repository.Repository
import com.kauel.kuploader2.utils.ADD_SERVER_RESULT_OK
import com.kauel.kuploader2.utils.UPDATE_SERVER_RESULT_OK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormServerViewModel @Inject constructor(
    private val repository: Repository,
    private val serverDao: ServerDao
) : ViewModel() {

    private val formServerEventChannel = Channel<FormServerEvent>()
    val formServerEvent = formServerEventChannel.receiveAsFlow()

    fun createServer(newServer: Server) =
        viewModelScope.launch {
            repository.insertServer(newServer)
            // navigate back
            formServerEventChannel.send(FormServerEvent.NavigateBackWithResult(ADD_SERVER_RESULT_OK))
        }

    fun updateServer(updatedServer: Server) =
        viewModelScope.launch {
            repository.updateServer(updatedServer)
            // navigate back
            formServerEventChannel.send(
                FormServerEvent.NavigateBackWithResult(
                    UPDATE_SERVER_RESULT_OK
                )
            )
        }

    private val serverTask = serverDao.getAllServer()
    val servers = serverTask.asLiveData()

    fun deleteServer(id: Int) =
        viewModelScope.launch {
            repository.deleteServer(id)
        }

//        val server = state?.get<Server>("server")
//
//    var serverName = state?.get<String>("serverName") ?: server?.name ?: ""
//        set(value) {
//            field = value
//            state?.set("serverName", value)
//        }
//
//    var serverUrl = state?.get<String>("serverUrl") ?: server?.name ?: ""
//        set(value) {
//            field = value
//            state?.set("serverUrl", value)
//        }


//    fun onSaveClick() = viewModelScope.launch {
//        if (serverName.isEmpty()) {
//            return@launch
//        }
//
//        if (server != null) {
//            val updatedServer = server.copy(name = serverName,url = serverUrl)
//            updateServer(updatedServer)
//        } else {
//            val newServer = Server(name = serverName,url = serverUrl)
//            createServer(newServer)
//        }
//    }

////    private fun showInvalidInputMessage(message: String) = viewModelScope.launch {
////        //addEditTaskEventChannel.send(AddEditTaskEvent.ShowInvalidInputMessage(message))
////        formServerEventChannel.send(FormServerEvent.NavigateBackWithResult(ADD_SERVER_RESULT_OK))
////    }

}

