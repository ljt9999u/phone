package com.example.album.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.album.data.api.ApiClient
import com.example.album.data.api.apiMessage
import com.example.album.data.model.LoginRequest
import com.example.album.data.model.RegisterRequest
import com.example.album.data.model.ResetSecretRequest
import com.example.album.data.prefs.AuthPrefs
import com.example.album.data.util.DeviceInfo
import kotlinx.coroutines.launch

/** 注册 / 重置成功后需要展示一次的安全码 */
data class SecretReveal(
    val secretUuid: String,
    val notice: String?,
    val fromRegister: Boolean
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = AuthPrefs(app)
    private val api = ApiClient.authApi

    var loginLoading by mutableStateOf(false)
        private set
    var registerLoading by mutableStateOf(false)
        private set
    var resetLoading by mutableStateOf(false)
        private set
    var loggingOut by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var loggedIn by mutableStateOf(prefs.isLoggedIn)
        private set
    var secretReveal by mutableStateOf<SecretReveal?>(null)
        private set

    fun clearError() {
        errorMessage = null
    }

    fun consumeSecretReveal() {
        secretReveal = null
    }

    /** 注册成功后保存了会话，展示完安全码进入首页 */
    fun enterApp() {
        loggedIn = prefs.isLoggedIn
    }

    /** 安全码重置等场景：会话已被清空，回到登录页 */
    fun exitToLogin() {
        loggedIn = false
    }

    fun login(phone: String, password: String, secretUuid: String) {
        viewModelScope.launch {
            loginLoading = true
            try {
                val res = api.login(
                    LoginRequest(
                        phone = phone,
                        password = password,
                        secretUuid = secretUuid,
                        deviceId = DeviceInfo.deviceId(getApplication()),
                        deviceName = DeviceInfo.deviceName()
                    )
                )
                if (res.code == 200 && res.data != null) {
                    prefs.saveSession(res.data)
                    loggedIn = true
                } else {
                    errorMessage = res.msg ?: "登录失败，请重试"
                }
            } catch (t: Throwable) {
                errorMessage = t.apiMessage()
            } finally {
                loginLoading = false
            }
        }
    }

    fun register(phone: String, password: String, nickname: String) {
        viewModelScope.launch {
            registerLoading = true
            try {
                val res = api.register(
                    RegisterRequest(
                        phone = phone,
                        password = password,
                        nickname = nickname.trim().ifBlank { null },
                        deviceId = DeviceInfo.deviceId(getApplication()),
                        deviceName = DeviceInfo.deviceName()
                    )
                )
                if (res.code == 200 && res.data != null) {
                    prefs.saveSession(res.data)
                    val uuid = res.data.secretUuid
                    if (uuid.isNullOrBlank()) {
                        loggedIn = true
                    } else {
                        secretReveal = SecretReveal(uuid, notice = null, fromRegister = true)
                    }
                } else {
                    errorMessage = res.msg ?: "注册失败，请重试"
                }
            } catch (t: Throwable) {
                errorMessage = t.apiMessage()
            } finally {
                registerLoading = false
            }
        }
    }

    fun resetSecret(phone: String, password: String) {
        viewModelScope.launch {
            resetLoading = true
            try {
                val res = api.resetSecretUuid(ResetSecretRequest(phone, password))
                if (res.code == 200 && res.data != null) {
                    prefs.clearSession() // 旧 token 全部失效
                    prefs.saveSecret(res.data.secretUuid)
                    secretReveal = SecretReveal(
                        res.data.secretUuid,
                        notice = res.data.notice,
                        fromRegister = false
                    )
                } else {
                    errorMessage = res.msg ?: "重置失败，请重试"
                }
            } catch (t: Throwable) {
                errorMessage = t.apiMessage()
            } finally {
                resetLoading = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            loggingOut = true
            try {
                api.logout("Bearer ${prefs.token}")
            } catch (_: Throwable) {
                // 退出登录尽力而为，本地一律清理
            }
            prefs.clearSession()
            loggingOut = false
            loggedIn = false
        }
    }
}
