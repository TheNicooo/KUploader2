package com.kauel.kuploader2.ui.formServer

sealed class FormServerEvent {
    data class ShowInvalidInputMessage(val message: String) : FormServerEvent()
    data class NavigateBackWithResult(val result: Int) : FormServerEvent()
}