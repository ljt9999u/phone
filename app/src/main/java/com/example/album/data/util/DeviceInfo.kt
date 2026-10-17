package com.example.album.data.util

import android.content.Context
import android.os.Build
import android.provider.Settings

/** 注册 / 登录时上报的设备信息 */
object DeviceInfo {

    fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

    fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}
