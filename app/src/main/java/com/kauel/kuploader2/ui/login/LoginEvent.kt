package com.kauel.kuploader2.ui.login

import com.kauel.kuploader2.api.server.Server

sealed class LoginEvent {
    data class ShowUndoDeleteServerMessage(val server: Server) : LoginEvent()
    object NavigateToAddServerScreen : LoginEvent()
    data class NavigateToEditServerScreen(val server: Server) : LoginEvent()
    data class ShowServerSavedConfirmationMessage(val message: String) : LoginEvent()
    object NavigateToDeleteAllCompletedScreen : LoginEvent()
}