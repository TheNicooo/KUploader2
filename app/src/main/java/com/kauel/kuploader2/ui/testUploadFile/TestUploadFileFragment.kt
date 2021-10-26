package com.kauel.kuploader2.ui.testUploadFile

import android.app.Activity
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
import com.kauel.kuploader2.databinding.FragmentTestUploadFileBinding
import com.kauel.kuploader2.utils.*
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@AndroidEntryPoint
class TestUploadFileFragment : Fragment(R.layout.fragment_test_upload_file) {

    //DATA BINDING
    private var _binding: FragmentTestUploadFileBinding? = null
    private val binding get() = _binding!!

    //VIEW MODEL
    private val viewModel: TestUploadFileViewModel by viewModels()

    //VARIABLES
    private var file: File? = null
    private var isUploading: Boolean = false
    private var url: String? = ""
    private var dateStart: String = ""
    private var dateEnd: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTestUploadFileBinding.inflate(inflater, container, false)
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
            rlDateStart.gone()
            rlDateEnd.gone()
            rlTime.gone()

            btnChoosePath.setOnClickListener(View.OnClickListener {
                val intent = Intent()
                    .setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(Intent.createChooser(intent, TITLE_INTENT_FOLDER), CODE_INTENT_FOLDER)
            })

            imgUpload.setOnClickListener {
                try {
                    if (!isUploading) {
                        if (file != null) {
                            isUploading = true
                            showDateStart()
                            uploadFileToServer(file!!)
                        } else {
                            activity?.makeToast(EMPTY_PATH)
                        }
                    } else {
                        activity?.makeToast(UPLOAD_START)
                    }
                } catch (ex: Exception) {
                    val error = ex.localizedMessage
                    appendLog("TestUploadFileFragment setUpView-imgUpload.setOnClickListener $error")
                    activity?.makeToast(error)
                }
            }
        }
    }

    private fun init() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val name = sharedPref.getString("NAME_SERVER", "")
        url = sharedPref.getString("URL", "")
        binding.edtNameServerTesting.text = "$name"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == Activity.RESULT_OK) {
            var gpath: String = Environment.getExternalStorageDirectory().absolutePath
            val filepath =
                File((gpath + File.separator + data?.data?.path).replace("tree/primary:", ""))

            val path = filepath.toString()
            binding.edtPathFiles.text = "RUTA: $path"

            if (filepath.isDirectory) {
                file = filepath.listFiles().first()
            } else {
                Toast.makeText(
                    activity,
                    ERROR_PATH,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun initObservers() {
        viewModel.testUploadLiveData.observeForever { result ->
            when (result) {
                is Resource.Error -> {
                    val error = result.error.toString()
                    appendLog("TestUploadFileFragment initObservers-Error $error")
                    activity?.makeToast(NOTIFICATION_ERROR)
                }
                is Resource.Loading -> {
                    result?.data?.let {
                        showProgressUpload(true, it.size)
                    }
                }
                is Resource.Success -> {
                    showProgressUpload(false)
                    showDateEnd()
                }
            }
        }
    }

    private fun uploadFileToServer(file: File) {
        val url = url + URL_TEST_FILE
        viewModel.uploadTestFile(url, file)
    }

    private fun showProgressUpload(status: Boolean, progress: Int = 0) {
        if (status) {
            binding.apply {
                pbLoadingUpload.visible()
                pbLoadingUpload.progress = progress
            }
        } else {
            binding.apply {
                pbLoadingUpload.gone()
            }
        }
    }

    private fun showDateStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            dateStart = current.format(formatter)

            binding.apply {
                edtDateStart.text = "Inicio: $dateStart"
                rlDateStart.visible()
            }
        }
    }

    private fun showDateEnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            dateEnd = current.format(formatter)

            binding.apply {
                edtDateEnd.text = "Termino: $dateEnd"
                rlDateEnd.visible()
            }

            calculateTime()
        }
    }

    private fun calculateTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            try {

                val date1 =
                    SimpleDateFormat("dd-MM-yyyy hh:mm:ss").parse(dateStart)
                val date2 =
                    SimpleDateFormat("dd-MM-yyyy hh:mm:ss").parse(dateEnd)

                val mills: Long = date2.time - date1.time
                val minutes = mills / 1000 / 60
                val seconds = mills / 1000 % 60

                binding.apply {
                    edtTimer.text = "Tiempo: $minutes : $seconds"
                    rlTime.visible()
                }

            } catch (ex: Exception) {
                val error = ex.localizedMessage
                appendLog("TestUploadFileFragment calculateTime $error")
                activity?.makeToast(error)
            }
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

}