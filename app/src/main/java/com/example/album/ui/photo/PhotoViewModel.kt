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
import com.example.album.data.model.BatchConfirmRequest
import com.example.album.data.model.BatchDeleteRequest
import com.example.album.data.model.BatchTokensRequest
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

    /** 单次最多 10 张（与后端批量上传限制一致） */
    val pickedPhotos = mutableStateListOf<ImageStore.PickedMeta>()
    var uploading by mutableStateOf(false)
        private set
    var uploadStage by mutableStateOf("")
        private set
    var uploadError by mutableStateOf<String?>(null)
        private set

    fun clearUploadError() {
        uploadError = null
    }

    /** 多选后批量读取元数据；自动去重并按 10 张截断 */
    fun onPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uploadError = null
            val remaining = MAX_BATCH - pickedPhotos.size
            if (remaining <= 0) {
                uploadError = "一次最多上传 $MAX_BATCH 张照片"
                return@launch
            }
            val fresh = uris
                .distinct()
                .filter { uri -> pickedPhotos.none { it.uri == uri } }
                .take(remaining)
            var failed = 0
            fresh.forEach { uri ->
                runCatching { ImageStore.readMeta(getApplication(), uri) }
                    .onSuccess { pickedPhotos += it }
                    .onFailure { failed++ }
            }
            if (uris.size > remaining) {
                uploadError = "一次最多上传 $MAX_BATCH 张照片，已选择前 $remaining 张"
            } else if (failed > 0) {
                uploadError = "$failed 张图片读取失败，请重新选择"
            }
        }
    }

    fun removePicked(meta: ImageStore.PickedMeta) {
        pickedPhotos.remove(meta)
        uploadError = null
    }

    fun clearPicked() {
        pickedPhotos.clear()
        uploadError = null
    }

    /** 1 张走单个接口，2-10 张走批量接口（取凭证 → 逐张直传 → 批量确认） */
    fun startUpload() {
        val metas = pickedPhotos.toList()
        if (metas.isEmpty() || uploading) return
        viewModelScope.launch {
            uploading = true
            uploadError = null
            try {
                if (metas.size == 1) {
                    uploadSingle(metas.first())
                } else {
                    uploadBatch(metas)
                }
            } catch (t: Throwable) {
                uploadError = t.apiMessage()
            } finally {
                uploading = false
                uploadStage = ""
            }
        }
    }

    private suspend fun uploadSingle(meta: ImageStore.PickedMeta) {
        uploadStage = "正在获取上传凭证…"
        val tokenRes = api.uploadToken(bearer, meta.name)
        val tokenData = tokenRes.data
        if (tokenRes.code != 200 || tokenData == null) {
            uploadError = tokenRes.msg ?: "获取上传凭证失败"
            return
        }

        uploadStage = "正在上传…"
        val bytes = ImageStore.readBytes(getApplication(), meta.uri)
        val ok = QiniuUploader.upload(
            client, tokenData.key, tokenData.uploadToken,
            meta.name, bytes, meta.mimeType
        )
        if (!ok) {
            uploadError = "上传到云存储失败，请重试"
            return
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
    }

    private suspend fun uploadBatch(metas: List<ImageStore.PickedMeta>) {
        uploadStage = "正在获取上传凭证…"
        val tokenRes = api.batchTokens(bearer, BatchTokensRequest(metas.map { it.name }))
        val tokens = tokenRes.data
        if (tokenRes.code != 200 || tokens == null || tokens.size != metas.size) {
            uploadError = tokenRes.msg ?: "获取上传凭证失败"
            return
        }

        val items = mutableListOf<ConfirmRequest>()
        metas.forEachIndexed { index, meta ->
            uploadStage = "正在上传 ${index + 1}/${metas.size}…"
            val bytes = ImageStore.readBytes(getApplication(), meta.uri)
            val ok = QiniuUploader.upload(
                client, tokens[index].key, tokens[index].uploadToken,
                meta.name, bytes, meta.mimeType
            )
            if (!ok) {
                uploadError = "第 ${index + 1} 张上传失败，请重试"
                return
            }
            items += ConfirmRequest(
                qiniuKey = tokens[index].key,
                photoName = meta.name,
                width = meta.width,
                height = meta.height
            )
        }

        uploadStage = "正在确认入库…"
        val confirmRes = api.batchConfirm(bearer, BatchConfirmRequest(items))
        if (confirmRes.code == 200) {
            toastMessage = "已上传 ${items.size} 张照片"
            clearPicked()
            refreshGallery()
        } else {
            uploadError = confirmRes.msg ?: "确认入库失败"
        }
    }

    companion object {
        const val MAX_BATCH = 10
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

    // ---------- 多选 / 批量删除 ----------

    var selectionMode by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var batchDeleting by mutableStateOf(false)
        private set

    fun enterSelection() {
        selectionMode = true
        selectedIds = emptySet()
    }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelect(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    /** 已全选则反选，否则全选当前已加载的照片 */
    fun toggleSelectAll() {
        selectedIds = if (photos.isNotEmpty() && selectedIds.size == photos.size) {
            emptySet()
        } else {
            photos.map { it.id }.toSet()
        }
    }

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
                    // 列表刷新后剔除已不存在的选中项
                    selectedIds = selectedIds.intersect(photos.map { it.id }.toSet())
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
                    selectedIds = selectedIds - photo.id
                    toastMessage = "已删除"
                } else {
                    toastMessage = res.msg ?: "删除失败"
                }
            } catch (t: Throwable) {
                toastMessage = t.apiMessage()
            }
        }
    }

    /** 批量删除选中照片（整批原子，任一张失败则全部不删） */
    fun deleteSelected() {
        val ids = selectedIds.toList()
        if (ids.isEmpty() || batchDeleting) return
        viewModelScope.launch {
            batchDeleting = true
            try {
                val res = api.batchDelete(bearer, BatchDeleteRequest(ids))
                if (res.code == 200) {
                    val count = res.data?.deletedCount ?: ids.size
                    photos.removeAll { it.id in ids }
                    total = (total - count).coerceAtLeast(0)
                    selectedIds = emptySet()
                    selectionMode = false
                    toastMessage = "已删除 $count 张照片"
                } else {
                    toastMessage = res.msg ?: "批量删除失败"
                }
            } catch (t: Throwable) {
                toastMessage = t.apiMessage()
            } finally {
                batchDeleting = false
            }
        }
    }
}
