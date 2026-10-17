package com.example.album.data.util

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 相册相关文件工具 */
object ImageStore {

    data class PickedMeta(
        val uri: Uri,
        val name: String,
        val size: Long,
        val width: Int,
        val height: Int,
        val mimeType: String
    )

    /** 读取选中图片的文件名 / 尺寸 / 宽高 / MIME */
    suspend fun readMeta(context: Context, uri: Uri): PickedMeta = withContext(Dispatchers.IO) {
        var name = "photo_${System.currentTimeMillis()}.jpg"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                    ?.let { c.getString(it) }?.takeIf { it.isNotBlank() }?.let { name = it }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }
                    ?.let { size = c.getLong(it) }
            }
        }
        var w = 0
        var h = 0
        context.contentResolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, opts)
            w = opts.outWidth
            h = opts.outHeight
        }
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        PickedMeta(uri, name, size, w, h, mime)
    }

    suspend fun readBytes(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("无法读取所选图片")
    }

    /** 保存图片到系统相册（API 29+ 走 MediaStore，无需权限；更低版本存应用私有目录） */
    suspend fun saveToGallery(context: Context, name: String, bytes: ByteArray, mime: String): String =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Anzuo"
                    )
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: throw IllegalStateException("保存失败")
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("保存失败")
                "相册 / Pictures/Anzuo"
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: throw IllegalStateException("存储不可用")
                File(dir, name).writeBytes(bytes)
                dir.absolutePath
            }
        }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes / 1048576f)
        bytes >= 1 shl 10 -> "%.0f KB".format(bytes / 1024f)
        else -> "$bytes B"
    }

    fun formatTime(iso: String): String = iso.replace('T', ' ').take(16)
}
