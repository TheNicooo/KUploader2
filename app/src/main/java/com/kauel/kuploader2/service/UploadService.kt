package com.kauel.kuploader2.service

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.app.NotificationManagerCompat.from
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.kauel.kuploader2.MainActivity
import com.kauel.kuploader2.R
import com.kauel.kuploader2.ui.uploadFile.UploadFileEvent
import com.kauel.kuploader2.utils.*

class UploadService : LifecycleService() {

    companion object {
        val uploadEvent = MutableLiveData<UploadFileEvent>()
    }

    private lateinit var notificationManager: NotificationManagerCompat

    override fun onCreate() {
        notificationManager = from(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_SERVICE -> startForegroundService()

                ACTION_STOP_SERVICE -> stopService()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        uploadEvent.postValue(UploadFileEvent.START)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        startForeground(NOTIFICATION_ID, getNotificationBuilder().build())
    }

    private fun stopService() {
        //Toast.makeText(this, "STOP SERVICE", Toast.LENGTH_SHORT).show()
        uploadEvent.postValue(UploadFileEvent.STOP)
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(true)
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun getNotificationBuilder() =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Upload files")
            .setContentText("Uploading files")
            .setContentIntent(getActivityPendingIntent())
            .setProgress(100, 0, true)


    private fun getActivityPendingIntent() =
        PendingIntent.getActivity(
            this,
            143,
            Intent(this, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

}