package com.kauel.kuploader2.ui.formServer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kauel.kuploader2.R
import com.kauel.kuploader2.api.server.Server
import com.kauel.kuploader2.databinding.FragmentFormServerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class FormServerFragment : Fragment(R.layout.fragment_form_server), FormServerAdapter.OnItemClickListener {

    private val viewModel: FormServerViewModel by viewModels()


    private var _binding: FragmentFormServerBinding? = null
    private val binding get() = _binding!!
    private val adapter: FormServerAdapter = FormServerAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFormServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setUpView()
        initObservers()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setUpView() {
        binding.apply {
            btnSaveServer.setOnClickListener(View.OnClickListener {

                val name = binding.edtName.text.toString().trim()
                val url = binding.edtUrl.text.toString().trim()

                if (name.isEmpty()) {
                    binding.edtName.error = "Name required"
                    binding.edtName.requestFocus()
                    return@OnClickListener
                }

                if (url.isEmpty()) {
                    binding.edtUrl.error = "Password required"
                    binding.edtUrl.requestFocus()
                    return@OnClickListener
                }

                viewModel.createServer(Server(name = name, url = url))
//            saveServer(name, url)

            })

            rvListServer.adapter = adapter
            rvListServer.layoutManager = LinearLayoutManager(context)
        }
    }

//    private fun saveServer(name: String, url: String) {
//        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
//        val server = sharedPref.getString("LIST_SERVER","")
//        val serverClass: Server
//        val jsonServer: String?
//
//        if (server?.isNotEmpty() == true) {
//            val list = gson.fromJson(server, Array<Server>::class.java).toList()
//            serverClass = Server(name, url)
//            list.forEach {
//                listServer.add(it)
//            }
//            listServer.add(serverClass)
//            jsonServer = gson.toJson(listServer)
//        } else {
//            serverClass = Server(name, url)
//            listServer.add(serverClass)
//            jsonServer = gson.toJson(listServer)
//        }
//        with (sharedPref.edit()) {
//            putString("LIST_SERVER", jsonServer)
//            apply()
//        }
//        Toast.makeText(activity, "Success!!", Toast.LENGTH_SHORT).show()
//        cleanControls()
//        findNavController().popBackStack()
//    }

    private fun cleanControls() {
        binding.apply {
            edtName.setText("")
            edtUrl.setText("")
        }
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.formServerEvent.collect {
                when (it) {
                    is FormServerEvent.NavigateBackWithResult -> {
                        binding.edtName.clearFocus()
                        cleanControls()
                        //findNavController().popBackStack()
                        Snackbar.make(requireView(), "Success!!!", Snackbar.LENGTH_LONG).show()
                    }
                    is FormServerEvent.ShowInvalidInputMessage -> {
                        Snackbar.make(requireView(), it.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewModel.servers.observe(viewLifecycleOwner, { serverList ->
            adapter.submitList(serverList)
        })
    }

//    private fun showSuccessView(data: Login?) {
//        //showControls()
//        binding.apply {
//            progressBar.gone()
//            //makeToast("Success!!!")
//            //Toast.makeText(this@LoginFragment,"", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun showLoadingView() {
//        //goneControls()
//        binding.progressBar.visible()
//    }
//
//    private fun showErrorView(error: Throwable?) {
//        //showControls()
//        binding.apply {
//            progressBar.gone()
//            //makeToast(ERROR_LOGIN)
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemClick(server: Server) {
        viewModel.deleteServer(server.id)
    }

}