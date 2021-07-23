package com.kauel.kuploader2.ui.uploadFile

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
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
import com.kauel.kuploader2.ui.formServer.FormServerAdapter
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

@AndroidEntryPoint
class UploadFileFragment : Fragment(R.layout.fragment_upload_file) {

    private val viewModel : UploadFileViewModel by viewModels()


    private var _binding : FragmentUploadFileBinding? = null
    private val binding get() = _binding!!

    private val listFile: ArrayList<File> = ArrayList()
    private var token: String? = ""

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
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setUpView() {
        binding.apply {
            btnChoosePath.setOnClickListener(View.OnClickListener {
                val intent = Intent()
                    .setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
            })
        }
    }

    private fun init() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val name = sharedPref.getString("NAME_SERVER", "")
        token = sharedPref.getString("TOKEN", "")
        binding.edtNameServer.text = "SERVER: $name"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            var gpath: String = Environment.getExternalStorageDirectory().absolutePath
            var filepath = File((gpath + File.separator + data?.data?.path).replace("tree/primary:", ""))
            binding.edtPathFiles.text = filepath.toString()

            if (filepath.isDirectory) {
                listFiles(filepath)
                createFolder(File(gpath + File.separator))
            } else
                Toast.makeText(activity, "Error al seleccionar ruta de imagenes!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listFiles(path: File) {
        path.walk().forEach {
            if (it.extension == "jpg" || it.extension == "jpeg")
                listFile.add(it)
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

    private fun uploadImage() {
        //val file = File(listFile[0])
        val requestFile: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), listFile[0])
        val image = MultipartBody.Part.createFormData("file", listFile[0].name, requestFile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}