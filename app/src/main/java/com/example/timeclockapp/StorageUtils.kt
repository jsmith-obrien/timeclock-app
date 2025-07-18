package com.example.timeclockapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File



object StorageUtils {
    private const val USERS_FILE = "users.json"

    fun saveUsers(context: Context, users: List<User>) {
        val json = Gson().toJson(users)
        File(context.filesDir, USERS_FILE).writeText(json)
    }

    fun loadUsers(context: Context): MutableList<User> {
        val json = context.assets.open(USERS_FILE).bufferedReader().use { it.readText() }
        val type = object : TypeToken<MutableList<User>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun savePunches(context: Context, username: String, punches: List<Punch>) {
        val json = Gson().toJson(punches)
        File(context.filesDir, "$username-punches.json").writeText(json)
    }

    fun loadPunches(context: Context, username: String): MutableList<Punch> {
        val file = File(context.filesDir, "$username-punches.json")
        if (!file.exists()) return mutableListOf()
        val json = file.readText()
        val type = object : TypeToken<MutableList<Punch>>() {}.type
        return Gson().fromJson(json, type)
    }
}