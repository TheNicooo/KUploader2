package com.kauel.kuploader2.ui.uploadFile

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.database.getDoubleOrNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kauel.kuploader2.MainActivity
import com.kauel.kuploader2.R
import com.kauel.kuploader2.databinding.FragmentUploadFileBinding
import com.kauel.kuploader2.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.Instant
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.collections.ArrayList


@AndroidEntryPoint
class UploadFileFragment : Fragment(R.layout.fragment_upload_file) {

    private val viewModel: UploadFileViewModel by viewModels()

    private var _binding: FragmentUploadFileBinding? = null
    private val binding get() = _binding!!

    private val listFile: ArrayList<File> = ArrayList()
    private var list: List<String> = ArrayList()
    private var token: String? = ""
    private var url: String? = ""
    private var flagStop: Boolean = false
    private var isUploading: Boolean = false
    private var filepath: File? = null
    private var mBuilder: NotificationCompat.Builder? = null
    private var total = 0
    private var currentUpload = 0

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
            tvNameImage.gone()
            rlCountImage.gone()

            btnChoosePath.setOnClickListener(View.OnClickListener {
                val intent = Intent()
                    .setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(Intent.createChooser(intent, TITLE_INTENT_FOLDER), CODE_INTENT_FOLDER)
            })

            imgUpload.setOnClickListener {
                try {
                    flagStop = false
                    if (!isUploading) {
                        if (filepath != null) {
                            listFiles(filepath!!)
                            binding.imgUpload.setImageDrawable(resources.getDrawable(R.drawable.cloud_upload))
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                createNotificationChannel()
                            }
                            if (listFile.size > 0) {
                                isUploading = true
                                total += listFile.size
                                pbLoadingUpload.visible()
                                lyProgressUpload.visible()
                                uploadFileToServer()
                            } else {
                                activity?.makeToast(EMPTY_PATH)
                            }
                        }
                    } else {
                        activity?.makeToast(UPLOAD_START)
                    }
                } catch (ex: Exception) {
                    val error = ex.localizedMessage
                    appendLog("UploadFileFragment setUpView-imgUpload.setOnClickListener $error")
                    activity?.makeToast(error)
                }
            }

            imgStop.setOnClickListener {
                flagStop = true
                activity?.makeToast(UPLOAD_STOP)
            }

            imgTestUpload.setOnClickListener {
                findNavController().navigate(R.id.action_uploadFileFragment_to_uploadTestFileFragment)
            }
        }
    }

    private fun init() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val name = sharedPref.getString("NAME_SERVER", "")
        token = sharedPref.getString("TOKEN", "")
        url = sharedPref.getString("URL", "")
        val path = sharedPref.getString("PATH_FILE", "")

        binding.edtNameServer.text = "SERVIDOR: $name"
        if (path != "") {
            filepath = File(path)
            binding.edtPathFiles.text = "RUTA: $path"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            var gpath: String = Environment.getExternalStorageDirectory().absolutePath

            filepath =
                File((gpath + File.separator + data?.data?.path).replace("tree/primary:", ""))

            val path = filepath.toString()
            saveData()
            binding.edtPathFiles.text = "RUTA: $path"

            if (filepath!!.isDirectory) {
                createFolder()
            } else
                Toast.makeText(
                    activity,
                    ERROR_PATH,
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    /**
     *
     * */
    private fun upload() {
        try {
            listFiles(filepath!!)
            if (listFile.size > 0) {
                isUploading = true
                total += listFile.size
                uploadFileToServer()
            } else {
                total = 0
                currentUpload = 0
                showProgressUpload(false)
                showCountImage(false)
                showNotificationEnd(3)
                binding.imgUpload.setImageDrawable(resources.getDrawable(R.drawable.cloud_done))
            }
        }
        catch (ex: java.lang.Exception) {
            val error = ex.message
            appendLog("UploadFileFragment upload $error")
            activity?.makeToast(error!!)
        }
    }

    /**
     * Upload file to server
     * */
    private fun uploadFileToServer() {
        val url = url + URL_FILE
        val token = "Bearer $token"

        viewModel.uploadFile(url, token, listFile)
    }

    private fun initObservers() {

        viewModel.uploadLiveData.observeForever { result ->
            when (result) {
                is Resource.Error -> {
                    val error = result.error.toString()
                    appendLog("UploadFileFragment initObservers-Error $error")

                    val response = result.data
                    response?.map {
                        moveFile(it.file, it.fileStatus)
                    }
                    val size = response?.size

                    showProgressUpload(false)
                    showCountImage(false)
                    showNotificationEnd(1)
                    total = 0
                    activity?.makeToast("$error $UPLOAD_ERROR $size")
                }
                is Resource.Loading -> {
                    result?.data?.let {
                        lifecycleScope.launch {
                            var current: Int = if (currentUpload == 0) {
                                it.size
                            } else {
                                currentUpload + it.size
                            }
                            binding.tvNameImage.text = it.last().file.name
                            showProgressUpload(true, current, total)
                            showCountImage(true, current, total)
                        }
                    }
                }
                is Resource.Success -> {

                    val response = result.data
                    response?.map {
                        moveFile(it.file, it.fileStatus)
                    }
                    if (response != null) {
                        currentUpload += response.size
                    }

                    if (flagStop) {
                        showProgressUpload(false)
                        showCountImage(false)
                        showNotificationEnd(2)
                    } else {
                        upload()
                    }
                }
            }
        }
    }

    private fun saveData() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("PATH_FILE", filepath.toString())
            apply()
        }
    }

    private fun listFiles(path: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            list = getStringFileList(path.toString(), ".jpg")
        }
        listFile.clear()
        list.forEach {
            listFile.add(File(path.toString() + File.separator + it))
        }

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getStringFileList(path: String, fileNameFilterPattern: String): List<String> {
        var fileList: List<String> = ArrayList()
        val parentList: MutableSet<String> = HashSet()

        try {
            fileList = listFilesForFolder(filepath!!).stream().map { x: String ->
                x.replace(
                    path,
                    ""
                )
            }.sorted(Comparator.naturalOrder())
                .filter { f: String ->
                    f.endsWith(
                        fileNameFilterPattern
                    ) || f.endsWith(".JPG")
                }
                .collect(Collectors.toList())

            //Collections.sort(fileList);
            fileList.forEach(
                Consumer { x: String ->
                    parentList.add(
                        x.replace(
                            "_.+".toRegex(),
                            ""
                        )
                    )
                }
            )
        } catch (e: Exception) {
            Log.e("catch", e.message!!)
        }
        return fileList
    }

    private fun listFilesForFolder(folder: File): List<String> {
        val listFilesForFolder: MutableList<String> = ArrayList()
        for (fileEntry in folder.listFiles()) {
            if (fileEntry.isDirectory) {
                listFilesForFolder(fileEntry)
            } else {
                listFilesForFolder.add(fileEntry.name)
            }
        }
        listFilesForFolder.sort()
        return listFilesForFolder
    }

    private fun getFileFromUri(contentResolver: ContentResolver, uri: Uri, directory: File): File {
        val prefix = "TMP-"
        val suffix = "-" + directory.name

        val file = File.createTempFile(prefix, suffix, directory)
        file.outputStream().use {
            contentResolver.openInputStream(uri)?.copyTo(it)
        }

        return file
    }

    @Throws(IOException::class)
    fun copyExif(originalPath: String?, newPath: String?) {
        val attributes = arrayOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_SUBSEC_TIME,
            ExifInterface.TAG_WHITE_BALANCE
        )
        val oldExif = ExifInterface(originalPath!!)
        val newExif = ExifInterface(newPath!!)
        if (attributes.isNotEmpty()) {
            for (i in attributes.indices) {
                val value = oldExif.getAttribute(attributes[i])
                if (value != null) newExif.setAttribute(attributes[i], value)
            }
            newExif.saveAttributes()
        }
    }

    private fun showProgressUpload(status: Boolean, numImage: Int? = 0, totalImage: Int? = 0) {
        if (status) {
            binding.apply {
                tvNameImage.visible()

                mBuilder!!.setContentText("Subidas/restantes: $numImage / $totalImage")
                notificationManager.notify(NOTIFICATION_ID, mBuilder!!.build())
            }
        } else {
            binding.apply {
                pbLoadingUpload.gone()
                lyProgressUpload.gone()
                tvNameImage.gone()
            }
        }
    }

    private fun showCountImage(status: Boolean, numImage: Int = 0, totalImage: Int = 0) {
        binding.apply {
            edtCountImage.text = "Subidas/Restantes: $numImage / $totalImage"
            if (status)
                rlCountImage.visible()
            else
                rlCountImage.gone()
        }
    }

    private fun showNotificationEnd(error: Int) {
        when (error) {
            //Error al subir imagenes
            1 -> {
                mBuilder!!.setContentText(NOTIFICATION_ERROR)
                    .setProgress(0, 0, false)

                notificationManager.notify(NOTIFICATION_ID, mBuilder!!.build())
                isUploading = false
            }
            //Subida detenida
            2 -> {
                mBuilder!!.setContentText(NOTIFICATION_UPLOAD_STOP)
                    .setProgress(0, 0, false)

                notificationManager.notify(NOTIFICATION_ID, mBuilder!!.build())
                isUploading = false
            }
            //Subida terminada
            3 -> {
                mBuilder!!.setContentText(NOTIFICATION_UPLOAD_FINISHED)
                    .setProgress(0, 0, false)

                notificationManager.notify(NOTIFICATION_ID, mBuilder!!.build())
                isUploading = false
            }
        }
    }

    private var absolutePath: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
    } else {
        Environment.getExternalStorageDirectory().absolutePath
    }

    private fun moveFile(file: File, status: Boolean) {

        var path: String = if (status) {
            absolutePath + File.separator + FOLDER_HISTORY + File.separator + FOLDER_SUCCESS + File.separator
        } else {
            absolutePath + File.separator + FOLDER_HISTORY + File.separator + FOLDER_ERROR + File.separator
        }

        try {

            file.let { sourceFile ->
                val destinationPath = File(path + file.name)
                if (!destinationPath.exists()) {
                    sourceFile.copyTo(destinationPath)
                    sourceFile.delete()
                } else {
                    sourceFile.delete()
                }
            }

        } catch (e: Exception) {
            activity?.makeToast(e.message.toString())
        }
    }

    private fun createFolder() {
        val path: String = absolutePath + File.separator
        val folder = File(path, FOLDER_HISTORY)
        val folderSuccess = File(folder, FOLDER_SUCCESS)
        val folderError = File(folder, FOLDER_ERROR)
        if (!folder.exists())
            folder.mkdirs()
        if (folder.exists()) {
            if (!folderSuccess.exists())
                folderSuccess.mkdirs()
            if (!folderError.exists())
                folderError.mkdirs()
            activity?.makeToast("Carpetas creadas correctamente!")
        } else {
            activity?.makeToast("Error crear carpeta")
        }
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

    @SuppressLint("WrongConstant")
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
                .setContentTitle(NOTIFICATION_TITLE)
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