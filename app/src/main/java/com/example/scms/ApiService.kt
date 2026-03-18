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
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): Response<Unit>

    @Multipart
    @POST("complaint")
    suspend fun submitComplaint(
        @Part photo: MultipartBody.Part?,
        @Part("category") category: RequestBody,
        @Part("address") address: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("description") description: RequestBody,
        @Part("user_id") userId: RequestBody
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
    // 🏆 GAMIFICATION
    // ===============================

    @GET("leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @GET("user/{id}/points")
    suspend fun getUserPoints(
        @Path("id") id: Int
    ): Response<PointsResponse>

    @Multipart
    @POST("user/{id}/profile")
    suspend fun updateProfile(
        @Path("id") id: Int,
        @Part photo: MultipartBody.Part
    ): Response<ProfileUpdateResponse>

    // ===============================
    // ADMIN SIDE
    // ===============================

    @PUT("complaint/department/{id}")
    suspend fun assignDepartment(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<Unit>

    @POST("complaint/resolve/{id}")
    suspend fun markResolved(
        @Path("id") id: Int
    ): Response<Unit>

    // ===============================
    // 🚨 ALERTS
    // ===============================

    @GET("alerts")
    suspend fun getAlerts(): List<Alert>

    // ===============================
    // 🚦 EMERGENCY SOS / ACCIDENTS
    // ===============================

    @POST("accidents")
    suspend fun postAccident(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>
}
