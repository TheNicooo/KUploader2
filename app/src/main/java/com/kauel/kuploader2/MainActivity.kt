package com.kauel.kuploader2

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.kauel.kuploader2.databinding.MainActivityBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionLauncher.launch(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment

        navController = navHostFragment.findNavController()

        setupActionBarWithNavController(navController)

    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("Permission: ", "Granted")
                //Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
            } else {
                Log.i("Permission: ", "Denied")
                //Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show()
            }
        }



    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() or super.onSupportNavigateUp()
    }

}