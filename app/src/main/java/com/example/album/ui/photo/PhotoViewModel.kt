package com.example.album.ui.photo

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.album.data.api.ApiClient
import com.example.album.data.api.apiMessage
import com.example.album.data.model.ConfirmRequest
import com.example.album.data.model.PhotoItem
import com.example.album.data.prefs.AuthPrefs
import com.example.album.data.util.ImageStore
import com.example.album.data.util.QiniuUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class PhotoViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = AuthPrefs(app)
    private val api = ApiClient.photoApi
    private val client get() = ApiClient.okHttp
    private val bearer get() = "Bearer ${prefs.token}"

    // ---------- 上传 ----------

    var picked by mutableStateOf<ImageStore.PickedMeta?>(null)
        private set
    var uploading by mutableStateOf(false)
        private set
    var uploadStage by mutableStateOf("")
        private set
    var uploadError by mutableStateOf<String?>(null)
        private set

    fun clearUploadError() {
        uploadError = null
    }

    fun onPicked(uri: Uri) {
        viewModelScope.launch {
            uploadError = null
            runCatching { ImageStore.readMeta(getApplication(), uri) }
                .onSuccess { picked = it }
                .onFailure { uploadError = "读取图片失败，请重新选择" }
        }
    }

    fun clearPicked() {
        picked = null
        uploadError = null
    }

    /** 取凭证 → 直传七牛 → 确认入库 */
    fun startUpload() {
        val meta = picked ?: return
        viewModelScope.launch {
            uploading = true
            uploadError = null
            try {
                uploadStage = "正在获取上传凭证…"
                val tokenRes = api.uploadToken(bearer, meta.name)
                val tokenData = tokenRes.data
                if (tokenRes.code != 200 || tokenData == null) {
                    uploadError = tokenRes.msg ?: "获取上传凭证失败"
                    return@launch
                }

                uploadStage = "正在上传…"
                val bytes = ImageStore.readBytes(getApplication(), meta.uri)
                val ok = QiniuUploader.upload(
                    client, tokenData.key, tokenData.uploadToken,
                    meta.name, bytes, meta.mimeType
                )
                if (!ok) {
                    uploadError = "上传到云存储失败，请重试"
                    return@launch
                }

                uploadStage = "正在确认入库…"
                val confirmRes = api.confirm(
                    bearer,
                    ConfirmRequest(
                        qiniuKey = tokenData.key,
                        photoName = meta.name,
                        width = meta.width,
                        height = meta.height
                    )
                )
                if (confirmRes.code == 200) {
                    toastMessage = "上传成功"
                    clearPicked()
                    refreshGallery()
                } else {
                    uploadError = confirmRes.msg ?: "确认入库失败"
                }
            } catch (t: Throwable) {
                uploadError = t.apiMessage()
            } finally {
                uploading = false
                uploadStage = ""
            }
        }
    }

    // ---------- 照片列表 ----------

    val photos = mutableStateListOf<PhotoItem>()
    var total by mutableLongStateOf(0L)
        private set
    var galleryLoading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var endReached by mutableStateOf(false)
        private set
    var galleryError by mutableStateOf<String?>(null)
        private set
    private var currentPage = 0
    private var galleryLoaded = false

    fun loadGalleryIfNeeded() {
        if (!galleryLoaded) {
            galleryLoaded = true
            refreshGallery()
        }
    }

    fun refreshGallery() {
        if (galleryLoading) return
        viewModelScope.launch {
            galleryLoading = true
            galleryError = null
            try {
                val res = api.page(bearer, page = 1, size = 30)
                val data = res.data
                if (res.code == 200 && data != null) {
                    photos.clear()
                    photos.addAll(data.list)
                    total = data.total
                    currentPage = data.page
                    endReached = photos.size >= data.total
                } else {
                    galleryError = res.msg ?: "加载失败"
                }
            } catch (t: Throwable) {
                galleryError = t.apiMessage()
            } finally {
                galleryLoading = false
            }
        }
    }

    fun loadMore() {
        if (endReached || loadingMore || galleryLoading) return
        viewModelScope.launch {
            loadingMore = true
            try {
                val res = api.page(bearer, page = currentPage + 1, size = 30)
                val data = res.data
                if (res.code == 200 && data != null) {
                    photos.addAll(data.list)
                    currentPage = data.page
                    endReached = photos.size >= data.total
                }
            } catch (_: Throwable) {
                // 加载更多失败静默，用户滚动会重试
            } finally {
                loadingMore = false
            }
        }
    }

    // ---------- 下载 / 删除 ----------

    var downloading by mutableStateOf(false)
        private set
    var toastMessage by mutableStateOf<String?>(null)
        private set

    fun consumeToast() {
        toastMessage = null
    }

    /** 重新取签名 URL → 下载 → 写入系统相册 */
    fun download(photo: PhotoItem) {
        if (downloading) return
        viewModelScope.launch {
            downloading = true
            try {
                val urlRes = api.photoUrl(bearer, photo.id)
                val url = urlRes.data?.takeIf { urlRes.code == 200 } ?: photo.url
                if (url.isBlank()) {
                    toastMessage = urlRes.msg ?: "获取下载链接失败"
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) {
                    client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                        if (!resp.isSuccessful) throw IllegalStateException("下载失败（HTTP ${resp.code}）")
                        resp.body?.bytes() ?: throw IllegalStateException("下载内容为空")
                    }
                }
                val mime = when (photo.photoName.substringAfterLast('.', "").lowercase()) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    else -> "image/jpeg"
                }
                val where = ImageStore.saveToGallery(
                    getApplication(), photo.photoName, bytes, mime
                )
                toastMessage = "已保存到 $where"
            } catch (t: Throwable) {
                toastMessage = t.apiMessage()
            } finally {
                downloading = false
            }
        }
    }

    fun delete(photo: PhotoItem) {
        viewModelScope.launch {
            try {
                val res = api.delete(bearer, photo.id)
                if (res.code == 200) {
                    photos.removeAll { it.id == photo.id }
                    total = (total - 1).coerceAtLeast(0)
                    toastMessage = "已删除"
                } else {
                    toastMessage = res.msg ?: "删除失败"
                }
            } catch (t: Throwable) {
                toastMessage = t.apiMessage()
            }
        }
    }
}
