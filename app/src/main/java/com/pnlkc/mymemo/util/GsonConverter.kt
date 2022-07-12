package com.pnlkc.mymemo.util

import androidx.room.TypeConverter
import com.google.gson.Gson

// room 데이터베이스에 list 저장하려면 필요
class GsonConverter {
    @TypeConverter
    fun listToJson(value: MutableList<String>?) = Gson().toJson(value)

    @TypeConverter
    fun jsonToList(value: String) = Gson().fromJson(value, Array<String>::class.java).toMutableList()
}