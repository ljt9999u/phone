package com.example.album.data.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /** 后端正式域名；本地联调可改回 http://10.0.2.2:8080/（模拟器访问本机） */
    private const val BASE_URL = "https://iphonealbum.mornbussion.site/"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** 供七牛直传 / 图片下载复用的 OkHttp 实例 */
    val okHttp: OkHttpClient get() = client

    val authApi: AuthApi by lazy {
        retrofit().create(AuthApi::class.java)
    }

    val photoApi: PhotoApi by lazy {
        retrofit().create(PhotoApi::class.java)
    }

    private fun retrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
