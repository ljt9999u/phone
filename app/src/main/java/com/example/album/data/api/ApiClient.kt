package com.example.album.data.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /**
     * 模拟器访问电脑本机后端用 10.0.2.2；
     * 真机调试可先执行 adb reverse tcp:8080 tcp:8080，再改为 http://127.0.0.1:8080/，
     * 或直接改为电脑的局域网 IP。
     */
    private const val BASE_URL = "http://10.0.2.2:8080/"

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
