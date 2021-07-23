package com.kauel.kuploader2.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kauel.kuploader2.api.login.Token

class Converters {
    @TypeConverter
    fun restoreListGenreData(objectToRestore: String?): Token? {
        return Gson().fromJson(objectToRestore, object : TypeToken<Token?>() {}.type)
    }

    @TypeConverter
    fun saveListGenreData(objectToSave: Token?): String? {
        return Gson().toJson(objectToSave)
    }
}