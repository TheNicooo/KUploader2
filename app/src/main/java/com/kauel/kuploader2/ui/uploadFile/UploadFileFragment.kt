package com.kauel.kuploader2.ui.uploadFile

import android.app.Activity.RESULT_OK
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.app.ServiceCompat.stopForeground
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kauel.kuploader2.MainActivity
import com.kauel.kuploader2.R
import com.kauel.kuploader2.databinding.FragmentUploadFileBinding
import com.kauel.kuploader2.service.UploadService
import com.kauel.kuploader2.utils.*
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.Instant


@AndroidEntryPoint
class UploadFileFragment : Fragment(R.layout.fragment_upload_file) {

    private val viewModel: UploadFileViewModel by viewModels()

    private var _binding: FragmentUploadFileBinding? = null
    private val binding get() = _binding!!

    private val listFile: ArrayList<File> = ArrayList()
    private var token: String? = ""
    private var url: String? = ""
    private var flagLoading: Boolean = false
    private var flagStop: Boolean = false
    private var isUploading: Boolean = false
    private var filepath: File? = null
    private var mBuilder: NotificationCompat.Builder? = null

    private lateinit var notificationManager: NotificationManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUploadFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setUpView()
        init()
        initObservers()
        notificationManager =
            activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setUpView() {
        binding.apply {
            pbLoadingUpload.gone()
            lyProgressUpload.gone()

            btnChoosePath.setOnClickListener(View.OnClickListener {
                val intent = Intent()
                    .setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
            })

            imgUpload.setOnClickListener {
                flagStop = false
                if (!isUploading) {
                    if (listFile.size > 0) {
                        isUploading = true
                        uploadFileToServer(listFile.first())
                        //sendCommandToService(ACTION_START_SERVICE)
                    } else {
                        activity?.makeToast("Ruta seleccecionada sin imagenes!")
                        //sendCommandToService(ACTION_STOP_SERVICE)
                    }
                } else {
                    activity?.makeToast("Subida ya iniciada")
                }
            }

            imgStop.setOnClickListener {
                flagStop = true
                activity?.makeToast("Subida de imagenes detenida!")
                //sendCommandToService(ACTION_STOP_SERVICE)
            }
        }
    }

    private fun init() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val name = sharedPref.getString("NAME_SERVER", "")
        token = sharedPref.getString("TOKEN", "")
        url = sharedPref.getString("URL", "")
        binding.edtNameServer.text = "SERVIDOR: $name"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            var gpath: String = Environment.getExternalStorageDirectory().absolutePath
            filepath =
                File((gpath + File.separator + data?.data?.path).replace("tree/primary:", ""))
            val path = filepath.toString()
            binding.edtPathFiles.text = "RUTA: $path"

            if (filepath!!.isDirectory) {
                listFiles(filepath!!)
                createFolder(File(gpath + File.separator))
                binding.imgUpload.setImageDrawable(resources.getDrawable(R.drawable.cloud_upload))
            } else
                Toast.makeText(
                    activity,
                    "Error al seleccionar ruta de imagenes!",
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun listFiles(path: File) {
        path.walk().forEach {
            if (it.extension == "jpg" || it.extension == "jpeg") {
                listFile.add(it)
            }
        }
    }

    private fun initObservers() {
        viewModel.uploadLiveData.observeForever { result ->
            when (result) {
                is Resource.Error -> {
                    //activity?.makeToast(result.error.toString())
                    appendLog(result.error.toString())
                    showProgressUpload(false)
                    showNotificationEnd(true)
                    flagLoading = false
                    //moveFile(listFile.first(), false)
                    //listFile.removeAt(0)
                    //uploadFileToServer(listFile.first())
                }
                is Resource.Loading -> {
                    //activity?.makeToast("LOADING")
                    showProgressUpload(true)
                    flagLoading = true
                }
                is Resource.Success -> {
                    if (listFile.isNotEmpty()) {
                        if (flagLoading) {
                            if (result.data!!.status) {
                                moveFile(listFile.first(), true)
                            } else {
                                moveFile(listFile.first(), false)
                            }
                            listFile.removeAt(0)
                            if (listFile.size > 0) {
                                if (flagStop) {
                                    //activity?.makeToast("Stop!")
                                    showProgressUpload(false)
                                } else {
                                    //activity?.makeToast("Remaining: ${listFile.size}")
                                    flagLoading = false
                                    uploadFileToServer(listFile.first())
                                }
                            } else {
                                flagLoading = false
                                listFiles(filepath!!)
                                if (flagStop) {
                                    //activity?.makeToast("Stop!")
                                    showProgressUpload(false)
                                    showNotificationEnd(false)
                                } else {
                                    if (listFile.size > 0) {
                                        uploadFileToServer(listFile.first())
                                    } else {
                                        showProgressUpload(false)
                                        showNotificationEnd(false)
                                        binding.imgUpload.setImageDrawable(resources.getDrawable(R.drawable.cloud_done))
                                        //sendCommandToService(ACTION_STOP_SERVICE)
                                    }
                                }
                            }
                        }
                    } else if (listFile.size == 0) {
                        flagLoading = false
                        listFiles(filepath!!)
                        if (flagStop) {
                            //activity?.makeToast("Stop!")
                            showProgressUpload(false)
                            showNotificationEnd(false)
                        } else {
                            if (listFile.size > 0) {
                                uploadFileToServer(listFile.first())
                            } else {
                                //activity?.makeToast("No remaining files")
                                showProgressUpload(false)
                                showNotificationEnd(false)
                                binding.imgUpload.setImageDrawable(resources.getDrawable(R.drawable.cloud_done))
                                //sendCommandToService(ACTION_STOP_SERVICE)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun uploadFileToServer(file: File) {
        val url = url + URL_FILE
        val token = "Bearer $token"

        val requestFile: RequestBody =
            RequestBody.create(
                MediaType.parse("multipart/form-data"),
                file
            )
        val image =
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestFile
            )
        viewModel.uploadFile(url, token, image)
    }

    private fun showProgressUpload(status: Boolean) {
        if (status) {
            binding.apply {
                pbLoadingUpload.visible()
                lyProgressUpload.visible()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }

                notificationManager.notify(NOTIFICATION_ID, mBuilder!!.build())
            }
        } else {
            binding.apply {
                pbLoadingUpload.gone()
                lyProgressUpload.gone()
            }
        }
    }

    private fun showNotificationEnd(error: Boolean) {
        if (error) {
            mBuilder!!.setContentText("Error al subir imagenes")
                .setProgress(0, 0, false)

            notificationManager.notify(NOTIFICATION_ID, mBuilder!!.build())
            isUploading = false
        } else {
            mBuilder!!.setContentText("Subida terminada!")
                .setProgress(0, 0, false)

            notificationManager.notify(NOTIFICATION_ID, mBuilder!!.build())
            isUploading = false
        }
    }

    private fun moveFile(file: File, status: Boolean) {

        var path: String = if (status) {
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "Historial imagenes subidas" + File.separator + "Success/"
        } else {
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "Historial imagenes subidas" + File.separator + "Error/"
        }

        file.let { sourceFile ->
            sourceFile.copyTo(File(path + file.name))
            sourceFile.delete()
        }
    }

    private fun createFolder(path: File) {
        val folder = File(path, "Historial imagenes subidas")
        val folderSuccess = File(folder, "Success")
        val folderError = File(folder, "Error")
        if (!folder.exists())
            folder.mkdir()
        if (!folderSuccess.exists())
            folderSuccess.mkdir()
        if (!folderError.exists())
            folderError.mkdir()
    }

    //Append Log file
    private fun appendLog(text: String?) {
        val logFile = File("sdcard/log.file")
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                // Auto-generated catch block
                e.printStackTrace()
            }
        }
        try {
            var date: String = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val instant = Instant.now()
                date = instant.toString()
            }
            //BufferedWriter for performance, true to set append to file flag
            val buf = BufferedWriter(FileWriter(logFile, true))
            buf.append("$date $text")
            buf.newLine()
            buf.close()
        } catch (e: IOException) {
            // Auto-generated catch block
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
        getNotificationBuilder()
    }

    private fun getNotificationBuilder() {

        mBuilder = context?.let {
            NotificationCompat.Builder(it, NOTIFICATION_CHANNEL_ID)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle("Subida de imagenes")
                .setContentText("Subiendo...")
                .setContentIntent(getActivityPendingIntent())
                .setProgress(100, 100, true)
        }
    }

    private fun getActivityPendingIntent() =
        PendingIntent.getActivity(
            context,
            143,
            Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}