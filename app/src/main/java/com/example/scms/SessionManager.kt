package com.example.scms

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("scms_prefs", Context.MODE_PRIVATE)

    // ✅ Save logged-in user
    fun saveUser(user: User) {
        prefs.edit()
            .putInt("id", user.id)
            .putString("name", user.name)
            .putString("phone", user.phone)
            .apply()
    }

    // ✅ Get logged-in user (null if not logged in)
    fun getUser(): User? {
        val id = prefs.getInt("id", -1)
        if (id == -1) return null

        return User(
            id = id,
            name = prefs.getString("name", "") ?: "",
            phone = prefs.getString("phone", "") ?: ""
        )
    }

    // ✅ Quick login check
    fun clear() {
        prefs.edit().clear().apply()
    }
}
