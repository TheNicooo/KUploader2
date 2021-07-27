package com.kauel.kuploader2.ui.uploadFile

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kauel.kuploader2.R
import com.kauel.kuploader2.databinding.FragmentUploadFileBinding
import com.kauel.kuploader2.utils.*
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.*
import java.nio.channels.FileChannel
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

    private var filepath: File? = null


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
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setUpView() {
        binding.apply {
            pbLoadingUpload.gone()

            btnChoosePath.setOnClickListener(View.OnClickListener {
                val intent = Intent()
                    .setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
            })

            imgUpload.setOnClickListener {
                if (listFile.size > 0)
                    uploadFileToServer(listFile.first())
            }
        }
    }

    private fun init() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val name = sharedPref.getString("NAME_SERVER", "")
        token = sharedPref.getString("TOKEN", "")
        url = sharedPref.getString("URL", "")
        binding.edtNameServer.text = "SERVER: $name"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            var gpath: String = Environment.getExternalStorageDirectory().absolutePath
            filepath =
                File((gpath + File.separator + data?.data?.path).replace("tree/primary:", ""))
            binding.edtPathFiles.text = filepath.toString()

            if (filepath!!.isDirectory) {
                listFiles(filepath!!)
                createFolder(File(gpath + File.separator))
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
        viewModel.uploadLiveData.observe(viewLifecycleOwner, { result ->
            when (result) {
                is Resource.Error -> {
                    activity?.makeToast(result.error.toString())
                    appendLog(result.error.toString())
                    binding.pbLoadingUpload.gone()
                    flagLoading = false
                }
                is Resource.Loading -> {
                    activity?.makeToast("LOADING")
                    binding.pbLoadingUpload.visible()
                    flagLoading = true
                }
                is Resource.Success -> {
                    if (listFile.isNotEmpty()) {
                        if (flagLoading) {
                            moveFile(listFile.first(), true)
                            listFile.removeAt(0)
                            if (listFile.size > 0) {
                                activity?.makeToast("Remaining: ${listFile.size}")
                                flagLoading = false
                                uploadFileToServer(listFile.first())
                            }
                        }
                    } else if (listFile.size == 0) {
                        flagLoading = false
                        listFiles(filepath!!)
                        if (listFile.size > 0) {
                            uploadFileToServer(listFile.first())
                        } else {
                            activity?.makeToast("No remaining files")
                            binding.pbLoadingUpload.gone()
                        }
                        //binding.pbLoadingUpload.gone()
                    }
                }
            }
        })
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

    private fun moveFile(file: File, status: Boolean) {
        var path = ""

        if (status) {
            path = Environment.getExternalStorageDirectory().absolutePath + File.separator + "History photo upload/"
        }

        file.let { sourceFile ->
            sourceFile.copyTo(File(path + file.name))
            sourceFile.delete()
        }
    }

//    private val CHANNEL_ID = "channel_id_example_01"
//    private val notificationId = 101
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "Notification Title"
//            val descriptionText = "Notification Description"
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                description = descriptionText
//            }
//            val notificationManager: NotificationManager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//    private fun sendNotification() {
//        val builder = activity?.let {
//            NotificationCompat.Builder(it.baseContext, CHANNEL_ID)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle("File Upload")
//                .setContentText("Upload image")
//                .setPriority(NotificationCompat.PRIORITY_LOW)
//                .setProgress(100, 0, true)
//        }
//
//        with(NotificationManagerCompat.from(this)) {
//            notify(notificationId, builder!!.build())
//        }
//
//    }

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

    private fun createFolder(path: File) {
        val folder = File(path, "History photo upload")
        if (!folder.exists())
            folder.mkdir()

        //Move file
//        val sourcePath = Paths.get("C:/Users/sampleuser/Downloads/test.txt")
//        val targetPath = Paths.get("C:/Users/sampleuser/Documents/test.txt")
//        Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}