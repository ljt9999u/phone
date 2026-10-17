package com.example.album.data.prefs

import android.content.Context
import com.example.album.data.model.AuthData

/** 登录态本地存储（app 私有目录）。安全码仅存本地便于登录时预填，不随备份导出的场景暂不考虑。 */
class AuthPrefs(context: Context) {

    private val sp = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    val token: String get() = sp.getString(KEY_TOKEN, null) ?: ""
    val secretUuid: String get() = sp.getString(KEY_SECRET, null) ?: ""
    val phone: String get() = sp.getString(KEY_PHONE, null) ?: ""
    val nickname: String get() = sp.getString(KEY_NICKNAME, null) ?: ""
    val userId: Long get() = sp.getLong(KEY_USER_ID, 0L)
    val isAdmin: Boolean get() = sp.getBoolean(KEY_ADMIN, false)
    val isLoggedIn: Boolean get() = token.isNotBlank()

    fun saveSession(data: AuthData) {
        sp.edit()
            .putString(KEY_TOKEN, data.token)
            .putLong(KEY_USER_ID, data.user.id)
            .putString(KEY_PHONE, data.user.phone)
            .putString(KEY_NICKNAME, data.user.nickname ?: "")
            .putBoolean(KEY_ADMIN, data.user.admin)
            .apply()
        if (!data.secretUuid.isNullOrBlank()) {
            saveSecret(data.secretUuid)
        }
    }

    fun saveSecret(uuid: String) {
        sp.edit().putString(KEY_SECRET, uuid).apply()
    }

    /** 仅清除登录态；手机号与安全码保留，便于下次登录预填 */
    fun clearSession() {
        sp.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_NICKNAME)
            .remove(KEY_ADMIN)
            .apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_SECRET = "secret_uuid"
        const val KEY_USER_ID = "user_id"
        const val KEY_PHONE = "phone"
        const val KEY_NICKNAME = "nickname"
        const val KEY_ADMIN = "admin"
    }
}
