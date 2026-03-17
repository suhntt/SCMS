package com.example.scms

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class OfflineComplaint(
    val photoUri: String?,
    val category: String,
    val address: String,
    val latitude: String,
    val longitude: String,
    val description: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

object OfflineComplaintManager {
    private const val PREFS_NAME = "offline_complaints_prefs"
    private const val KEY_COMPLAINTS = "queued_complaints"

    fun saveComplaintOffline(context: Context, complaint: OfflineComplaint) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(KEY_COMPLAINTS, "[]")
        
        val type = object : TypeToken<MutableList<OfflineComplaint>>() {}.type
        val queue: MutableList<OfflineComplaint> = gson.fromJson(json, type) ?: mutableListOf()
        
        queue.add(complaint)
        prefs.edit().putString(KEY_COMPLAINTS, gson.toJson(queue)).apply()
        Log.d("OfflineManager", "Saved complaint offline. Total queued: ${queue.size}")
    }

    suspend fun syncOfflineComplaints(context: Context) {
        if (!NetworkUtils.isInternetAvailable(context)) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(KEY_COMPLAINTS, "[]")
        val type = object : TypeToken<MutableList<OfflineComplaint>>() {}.type
        val queue: MutableList<OfflineComplaint> = gson.fromJson(json, type) ?: mutableListOf()

        if (queue.isEmpty()) return

        Log.d("OfflineManager", "Syncing ${queue.size} offline complaints to server...")
        val successfulSyncs = mutableListOf<OfflineComplaint>()

        withContext(Dispatchers.IO) {
            for (complaint in queue) {
                try {
                    val photoPart = complaint.photoUri?.let { uriStr ->
                        val uri = Uri.parse(uriStr)
                        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        if (bytes != null) {
                            MultipartBody.Part.createFormData(
                                "photo",
                                "offline_complaint_${complaint.timestamp}.jpg",
                                bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                            )
                        } else null
                    }

                    val response = RetrofitClient.api.submitComplaint(
                        photo = photoPart,
                        category = complaint.category.toRequestBody("text/plain".toMediaTypeOrNull()),
                        address = complaint.address.toRequestBody("text/plain".toMediaTypeOrNull()),
                        latitude = complaint.latitude.toRequestBody("text/plain".toMediaTypeOrNull()),
                        longitude = complaint.longitude.toRequestBody("text/plain".toMediaTypeOrNull()),
                        description = complaint.description.toRequestBody("text/plain".toMediaTypeOrNull()),
                        userId = complaint.userId.toRequestBody("text/plain".toMediaTypeOrNull())
                    )

                    if (response.isSuccessful) {
                        successfulSyncs.add(complaint)
                    }
                } catch (e: Exception) {
                    Log.e("OfflineManager", "Error syncing complaint", e)
                }
            }
        }

        // Remove synced complaints from the SharedPreferences Storage Queue
        queue.removeAll(successfulSyncs)
        prefs.edit().putString(KEY_COMPLAINTS, gson.toJson(queue)).apply()
        Log.d("OfflineManager", "Sync complete. Remaining in queue: ${queue.size}")
    }
}
