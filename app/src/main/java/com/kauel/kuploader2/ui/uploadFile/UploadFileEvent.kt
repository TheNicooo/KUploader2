package com.kauel.kuploader2.ui.uploadFile

sealed class UploadFileEvent {
    object START: UploadFileEvent()
    object STOP: UploadFileEvent()
}