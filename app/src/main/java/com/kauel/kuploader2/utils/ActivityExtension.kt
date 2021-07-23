package com.kauel.kuploader2.utils

import android.app.Activity
import android.widget.Toast

fun Activity.makeToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}