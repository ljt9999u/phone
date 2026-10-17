package com.example.album.ui.profile

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.album.data.api.ApiClient
import com.example.album.data.api.apiMessage
import com.example.album.data.model.DeviceItem
import com.example.album.data.model.UserInfo
import com.example.album.data.prefs.AuthPrefs
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = AuthPrefs(app)
    private val api = ApiClient.authApi
    private val bearer get() = "Bearer ${prefs.token}"

    var me by mutableStateOf<UserInfo?>(null)
        private set
    var devices by mutableStateOf<List<DeviceItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    private var loaded = false

    fun loadIfNeeded() {
        if (!loaded) {
            loaded = true
            refresh()
        }
    }

    fun refresh() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            errorMessage = null
            try {
                val meRes = api.me(bearer)
                if (meRes.code == 200 && meRes.data != null) {
                    me = meRes.data
                } else if (meRes.code != 200) {
                    errorMessage = meRes.msg
                }
                val devRes = api.devices(bearer)
                if (devRes.code == 200) {
                    devices = devRes.data ?: emptyList()
                }
            } catch (t: Throwable) {
                errorMessage = t.apiMessage()
            } finally {
                loading = false
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
