package com.kauel.kuploader2.ui.uploadFile

import android.annotation.SuppressLint
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
    private var flagStop: Boolean = false
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
            lyProgressUpload.gone()

            btnChoosePath.setOnClickListener(View.OnClickListener {
                val intent = Intent()
                    .setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
            })

            imgUpload.setOnClickListener {
                flagStop = false
                if (listFile.size > 0) {
                    uploadFileToServer(listFile.first())
                } else {
                    activity?.makeToast("Seleccionar ruta de fotos!")
                }
            }

            imgStop.setOnClickListener {
                flagStop = true
                activity?.makeToast("Subida de imagenes detenida!")
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
        viewModel.uploadLiveData.observe(viewLifecycleOwner, { result ->
            when (result) {
                is Resource.Error -> {
                    activity?.makeToast(result.error.toString())
                    appendLog(result.error.toString())
                    //binding.pbLoadingUpload.gone()
                    showProgressUpload(false)
                    flagLoading = false
                    //uploadFileToServer(listFile.first())
                }
                is Resource.Loading -> {
                    activity?.makeToast("LOADING")
                    //binding.pbLoadingUpload.visible()
                    showProgressUpload(true)
                    flagLoading = true
                }
                is Resource.Success -> {
                    if (listFile.isNotEmpty()) {
                        if (flagLoading) {
                            moveFile(listFile.first(), true)
                            listFile.removeAt(0)
                            if (listFile.size > 0) {
                                if (flagStop) {
                                    activity?.makeToast("Stop!")
                                    showProgressUpload(false)
                                } else {
                                    activity?.makeToast("Remaining: ${listFile.size}")
                                    flagLoading = false
                                    uploadFileToServer(listFile.first())
                                }
                            }
                        }
                    } else if (listFile.size == 0) {
                        flagLoading = false
                        listFiles(filepath!!)
                        if (flagStop) {
                            activity?.makeToast("Stop!")
                            showProgressUpload(false)
                        } else {
                            if (listFile.size > 0) {
                                uploadFileToServer(listFile.first())
                            } else {
                                activity?.makeToast("No remaining files")
                                //binding.pbLoadingUpload.gone()
                                showProgressUpload(false)
                                binding.imgUpload.setImageDrawable(resources.getDrawable(R.drawable.cloud_done))
                            }
                        }
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

    private fun showProgressUpload(status: Boolean) {
        if (status) {
            binding.apply {
                pbLoadingUpload.visible()
                lyProgressUpload.visible()
            }
        } else {
            binding.apply {
                pbLoadingUpload.gone()
                lyProgressUpload.gone()
            }
        }
    }

    private fun moveFile(file: File, status: Boolean) {
        var path = ""

        if (status) {
            path =
                Environment.getExternalStorageDirectory().absolutePath + File.separator + "History photo upload/"
        }

        file.let { sourceFile ->
            sourceFile.copyTo(File(path + file.name))
            sourceFile.delete()
        }
    }

    private fun createFolder(path: File) {
        val folder = File(path, "History photo upload")
        if (!folder.exists())
            folder.mkdir()
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}