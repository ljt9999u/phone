package com.example.album.data.api

import com.example.album.data.model.ApiEnvelope
import com.example.album.data.model.AuthData
import com.example.album.data.model.DeviceItem
import com.example.album.data.model.LoginRequest
import com.example.album.data.model.RegisterRequest
import com.example.album.data.model.ResetSecretData
import com.example.album.data.model.ResetSecretRequest
import com.example.album.data.model.UserInfo
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiEnvelope<AuthData>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiEnvelope<AuthData>

    @POST("api/auth/reset-secret-uuid")
    suspend fun resetSecretUuid(@Body body: ResetSecretRequest): ApiEnvelope<ResetSecretData>

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): ApiEnvelope<JsonElement?>

    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") token: String): ApiEnvelope<UserInfo>

    @GET("api/auth/devices")
    suspend fun devices(@Header("Authorization") token: String): ApiEnvelope<List<DeviceItem>>
}
