package com.example.album.data.api

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/** 把网络异常转成用户能看懂的中文提示（后端业务错误直接透出 msg） */
fun Throwable.apiMessage(): String = when (this) {
    is HttpException -> {
        val body = try {
            response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val msg = body?.let {
            runCatching { JSONObject(it).optString("msg") }.getOrNull()
        }
        if (!msg.isNullOrBlank()) msg else "请求失败（HTTP ${code()}）"
    }
    is IOException -> "网络连接失败，请确认后端服务已启动"
    else -> message?.takeIf { it.isNotBlank() } ?: "出了点小问题，请重试"
}
