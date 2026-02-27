package com.example.scms

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ===============================
    // USER SIDE
    // ===============================

    @GET("complaints")
    suspend fun getComplaints(): List<Complaint>

    @POST("upvote/{id}")
    suspend fun upvote(
        @Path("id") id: Int
    ): Response<Unit>

    @Multipart
    @POST("complaint")
    suspend fun submitComplaint(
        @Part photo: MultipartBody.Part?,
        @Part("category") category: RequestBody,
        @Part("address") address: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("description") description: RequestBody
    ): Response<Unit>
    @POST("login")
    suspend fun login(
        @Body body: Map<String, String>
    ): Response<LoginResponse>

    @POST("signup")
    suspend fun signup(
        @Body body: Map<String, String>
    ): Response<Map<String, Boolean>>






    // ===============================
    // ADMIN SIDE
    // ===============================

    // ✅ Assign Department
    @PUT("complaint/department/{id}")
    suspend fun assignDepartment(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<Unit>

    // ✅ Mark Complaint as Resolved
    @POST("complaint/resolve/{id}")
    suspend fun markResolved(
        @Path("id") id: Int
    ): Response<Unit>
}
