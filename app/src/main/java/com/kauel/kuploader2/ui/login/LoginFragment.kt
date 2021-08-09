package com.kauel.kuploader2.ui.login

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kauel.kuploader2.R
import com.kauel.kuploader2.api.login.Login
import com.kauel.kuploader2.api.server.Server
import com.kauel.kuploader2.databinding.FragmentLoginBinding
import com.kauel.kuploader2.utils.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val list: ArrayList<String> = ArrayList()
    private var listJson: List<Server>? = null
    private var idSpinner: Int = -1
    private var url: String = ""
    private var token: String = ""
    private var nameServer: String = ""
    private var flag: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        init()
        setUpView()
        initObservers()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setUpView() {
        binding.apply {
            btnLogin.setOnClickListener(View.OnClickListener {

                val urlAPI = url + URL_LOGIN
                val email = edtEmail.text.toString().trim()
                val password = edtPassword.text.toString().trim()

                if (email.isEmpty()) {
                    edtEmail.error = "Email required"
                    edtEmail.requestFocus()
                    return@OnClickListener
                }

                if (password.isEmpty()) {
                    edtPassword.error = "Password required"
                    edtPassword.requestFocus()
                    return@OnClickListener
                }

                if (url.isEmpty()) {
                    Toast.makeText(activity, "Agregar servidor", Toast.LENGTH_SHORT).show()
                    spServer.requestFocus()
                    return@OnClickListener
                }

                viewModel.login(urlAPI, email, password)
                flag = true
            })

            btnAddServer.setOnClickListener(View.OnClickListener {
                findNavController().navigate(R.id.action_login_to_userServer)
            })

            spServer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    idSpinner = position
                    nameServer = list[position]
                    Toast.makeText(activity, nameServer, Toast.LENGTH_SHORT).show()
                    repeat(listJson!!.size) {
                        if (listJson!![it].name == nameServer)
                            url = listJson!![it].url
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }

            var show = false

            showPassword.setOnClickListener {
                if (show) {
                    edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    binding.showPassword.setImageDrawable(resources.getDrawable(R.drawable.eye_open))
                    show = false
                } else {
                    edtPassword.inputType = 1
                    binding.showPassword.setImageDrawable(resources.getDrawable(R.drawable.eye_close))
                    show = true
                }
            }
        }

    }

    private fun init() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val email = sharedPref.getString("EMAIL", "")
        val password = sharedPref.getString("PASSWORD", "")
        idSpinner = sharedPref.getInt("ID_SERVER", 0)
        binding.apply {
            edtEmail.setText(email)
            edtPassword.setText(password)
        }
    }

    private fun initObservers() {
        viewModel.loginLiveData.observe(viewLifecycleOwner, { result ->
            when (result) {
                is Resource.Error -> showErrorView(result.error)
                is Resource.Loading -> showLoadingView()
                is Resource.Success -> showSuccessView(result.data)
            }
        })

        viewModel.servers.observe(viewLifecycleOwner, { serverList ->
            if (serverList?.isNotEmpty() == true) {
                list.clear()
                listJson = serverList
                serverList.map {
                    list.add(it.name)
                }
            }
            val adapter: ArrayAdapter<String> =
                ArrayAdapter(requireActivity(), R.layout.support_simple_spinner_dropdown_item, list)
            binding.apply {
                spServer.adapter = adapter
                spServer.setSelection(idSpinner)
            }
        })
    }

    private fun showSuccessView(data: Login?) {
        showControls()
        if (flag) {
            binding.apply {
                progressBar.gone()
                token = data?.accessToken.toString()
                saveUser()
                flag = false
                findNavController().navigate(R.id.action_login_to_uploadFileFragment)
                Toast.makeText(activity, "Success!!!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoadingView() {
        goneControls()
        binding.progressBar.visible()
    }

    private fun showErrorView(error: Throwable?) {
        showControls()
        binding.apply {
            progressBar.gone()
            Toast.makeText(activity, error?.message?.let { messageError(it) }, Toast.LENGTH_SHORT).show()
        }
    }

    private fun messageError(error: String): String {
        return when(error) {
            ERROR_403 -> ERROR_LOGIN

            ERROR_404 -> ERROR_SERVER

            else -> error
        }
    }

    private fun showControls() {
        binding.apply {
            lyCardView.visible()
        }
    }

    private fun goneControls() {
        binding.apply {
            lyCardView.gone()
        }
    }

    private fun saveUser() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("EMAIL", binding.edtEmail.text.toString())
            putString("PASSWORD", binding.edtPassword.text.toString())
            putInt("ID_SERVER", idSpinner)
            putString("TOKEN", token)
            putString("URL", url)
            putString("NAME_SERVER", nameServer)
            apply()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

