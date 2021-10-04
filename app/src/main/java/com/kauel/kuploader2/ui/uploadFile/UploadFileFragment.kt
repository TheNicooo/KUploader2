package com.kauel.kuploader2.ui.uploadFile

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
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
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kauel.kuploader2.MainActivity
import com.kauel.kuploader2.R
import com.kauel.kuploader2.databinding.FragmentUploadFileBinding
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
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.system.exitProcess


@AndroidEntryPoint
class UploadFileFragment : Fragment(R.layout.fragment_upload_file) {

    private val viewModel: UploadFileViewModel by viewModels()

    private var _binding: FragmentUploadFileBinding? = null
    private val binding get() = _binding!!

    private val listFile: ArrayList<File> = ArrayList()
    private var list: List<String> = ArrayList()
    private var token: String? = ""
    private var url: String? = ""
    private var flagLoading: Boolean = false
    private var flagStop: Boolean = false
    private var flagError: Boolean = true
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
            tvNameImage.gone()
            rlCountImage.gone()

            btnChoosePath.setOnClickListener(View.OnClickListener {
                val intent = Intent()
                    .setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
                //openDocument()
            })

            imgUpload.setOnClickListener {
                try {
                    flagStop = false
                    if (!isUploading) {
                        if (filepath != null) {
                            startProcessUpload()
                        }
                        if (listFile.size > 0) {
                            isUploading = true
                            uploadFileToServer(listFile.first())
                            //sendCommandToService(ACTION_START_SERVICE)
                        } else {
                            activity?.makeToast(EMPTY_PATH)
                            //sendCommandToService(ACTION_STOP_SERVICE)
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
                //sendCommandToService(ACTION_STOP_SERVICE)
            }

            imgTestUpload.setOnClickListener {
                findNavController().navigate(R.id.action_uploadFileFragment_to_uploadTestFileFragment)
            }
        }
    }

    private fun openDocument() {
        Intent().setAction(Intent.ACTION_OPEN_DOCUMENT).also {
            it.type = "image/*|application/pdf"
            val mimeTypes = arrayOf("image/jpg", "image/jpeg", "image/png", "application/pdf")
            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startForResultOpenDocument.launch(Intent.createChooser(it, "Seleccione un archivo"))
        }
    }

    private val startForResultOpenDocument =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {

                val uri = result.data?.data?.normalizeScheme()
                var gpath: String =
                    Environment.getExternalStorageDirectory().absolutePath//Environment.getExternalStorageDirectory().absolutePath

                //val file = File(gpath)

                var path = ""//uri?.split(":")

//                path = if (uri!!.contains("primary")) {
//                    uri.replace("tree/primary:", "")
//                } else {
//                    val split = uri.split(":")
//                    "mnt/extSdCard/" + split[1]
//                }

                val file = File(uri.toString())

                if (file.exists()) {
                    activity?.makeToast("Yes")
                } else {
                    activity?.makeToast("No")
                }

                if (file.isDirectory) {
                    activity?.makeToast("Directory")
                } else {
                    activity?.makeToast("Not directory")
                }

//var path = ""//uri?.split(":")

//                path = if (uri!!.contains("primary")) {
//                    uri.replace("tree/primary:", "")
//                } else {
//                    val split = uri.split(":")
//                    split[1]
//                }
//
//                //.replace("tree/3234-3831:", "")
//                uri.let {
//                    // AQUÍ VÁ LO QUE DESEAS HACER CON EL ARCHIVO
//                    filepath =
//                        File((gpath + File.separator + path))
//
//                    val path = filepath.toString()
//                    saveData()
//                    binding.edtPathFiles.text = "RUTA: $path"
//
//                    if (filepath!!.isDirectory) {
//                        //startProcessUpload()
//                        //createFolder(File(gpath + File.separator))
//                        listFiles(filepath!!)
//                    } else
//                        Toast.makeText(
//                            activity,
//                            ERROR_PATH,
//                            Toast.LENGTH_SHORT
//                        ).show()
//
//                }
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
            //startProcessUpload()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            var gpath: String = Environment.getExternalStorageDirectory().absolutePath

            //3234-3831:

//            gpath = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
//                Environment.getExternalStorageDirectory().absolutePath
//            } else {
//                Environment.getExternalStoragePublicDirectory("").absolutePath
//            }
            filepath =
                File((gpath + File.separator + data?.data?.path).replace("tree/primary:", ""))

            val path = filepath.toString()
            saveData()
            binding.edtPathFiles.text = "RUTA: $path"

            if (filepath!!.isDirectory) {
                //startProcessUpload()
                createFolder()
            } else
                Toast.makeText(
                    activity,
                    ERROR_PATH,
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun startProcessUpload() {
        listFiles(filepath!!)
        binding.imgUpload.setImageDrawable(resources.getDrawable(R.drawable.cloud_upload))
    }

    private fun saveData() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("PATH_FILE", filepath.toString())
            apply()
        }
    }

    private fun listFiles(path: File) {
//        path.walk().forEach {
//            if (it.extension == "jpg" || it.extension == "jpeg") {
//                listFile.add(it)
//            }
//        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            list = getStringFileList(path.toString(), ".jpg")
        }
        listFile.clear()
        list.forEach {
            listFile.add(File(path.toString() + File.separator + it))
        }

    }

//    private fun listFileString() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            list = getStringFileList(filepath.toString(), ".jpg")
//        }
//    }

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

    private fun initObservers() {
        var position = 0

        viewModel.uploadLiveData.observeForever { result ->
            when (result) {
                is Resource.Error -> {
                    val error = result.error.toString()
                    appendLog("UploadFileFragment initObservers-Error $error")
                    if (flagLoading) {
                        flagLoading = false
                        if (flagError) {
                            uploadFileToServer(listFile.first())
                            flagError = false
                        } else {
                            moveFile(listFile.first(), false)
                            listFiles(filepath!!)
                            uploadFileToServer(listFile.first())
                            flagError = true
                        }
                    }
                }
                is Resource.Loading -> {
                    position++
                    showProgressUpload(true, position, (listFile.size - 1))
                    showCountImage(true, position, (listFile.size - 1))
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
                            listFiles(filepath!!)
                            if (listFile.size > 0) {
                                if (flagStop) {
                                    showProgressUpload(false)
                                    showNotificationEnd(2)
                                    flagLoading = false
                                } else {
                                    flagLoading = false
                                    uploadFileToServer(listFile.first())
                                }
                            } else {
                                flagLoading = false
                                showProgressUpload(false)
                                showCountImage(false)
                                showNotificationEnd(3)
                                binding.imgUpload.setImageDrawable(resources.getDrawable(R.drawable.cloud_done))
                                position = 0
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
        binding.tvNameImage.text = file.name

//        val fileTemp = getFileFromUri(requireActivity().contentResolver, file.toUri(), requireActivity().cacheDir)
//        copyExif(file.absolutePath, fileTemp.absolutePath)

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
                pbLoadingUpload.visible()
                lyProgressUpload.visible()
                tvNameImage.visible()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }

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

    var absolutePath: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
                    //listFile.remove(file)
                    sourceFile.delete()
                } else {
                    //listFile.remove(file)
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