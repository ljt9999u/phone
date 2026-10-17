package com.example.album.data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** 七牛客户端直传（表单上传） */
object QiniuUploader {

    /**
     * bucket `yfdljt` 所属区域的上传域名。
     * 华东 up-z0 / 华北 up-z1 / 华南 up-z2 / 北美 up-na0 / 东南亚 up-as0，
     * 若上传 400/区域错误请按七牛控制台实际区域修改。
     */
    private const val UPLOAD_HOST = "https://up-z2.qiniup.com/"

    /** @return true 上传成功 */
    suspend fun upload(
        client: OkHttpClient,
        key: String,
        uploadToken: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): Boolean = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("token", uploadToken)
            .addFormDataPart("key", key)
            .addFormDataPart(
                "file", fileName,
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            .build()
        client.newCall(Request.Builder().url(UPLOAD_HOST).post(body).build())
            .execute()
            .use { it.isSuccessful }
    }
}
