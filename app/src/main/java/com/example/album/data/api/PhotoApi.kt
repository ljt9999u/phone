package com.example.album.data.api

import com.example.album.data.model.ApiEnvelope
import com.example.album.data.model.ConfirmRequest
import com.example.album.data.model.PhotoItem
import com.example.album.data.model.PhotoPageData
import com.example.album.data.model.UploadTokenData
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PhotoApi {

    @GET("api/photo/upload-token")
    suspend fun uploadToken(
        @Header("Authorization") token: String,
        @Query("fileName") fileName: String? = null
    ): ApiEnvelope<UploadTokenData>

    @POST("api/photo/confirm")
    suspend fun confirm(
        @Header("Authorization") token: String,
        @Body body: ConfirmRequest
    ): ApiEnvelope<PhotoItem>

    @GET("api/photo/page")
    suspend fun page(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiEnvelope<PhotoPageData>

    @GET("api/photo/{id}/url")
    suspend fun photoUrl(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiEnvelope<String>

    @DELETE("api/photo/{id}")
    suspend fun delete(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ApiEnvelope<JsonElement?>
}
