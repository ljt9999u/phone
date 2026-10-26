package com.example.album.data.model

import kotlinx.serialization.Serializable

/** 直传凭证 */
@Serializable
data class UploadTokenData(
    val key: String,
    val uploadToken: String,
    val bucket: String = "",
    val domain: String = "",
    val expireSeconds: Long = 0
)

@Serializable
data class ConfirmRequest(
    val qiniuKey: String,
    val photoName: String,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class PhotoItem(
    val id: Long,
    val photoName: String,
    val fileSize: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val uploadTime: String = "",
    val url: String = ""
)

/** 批量取凭证请求（fileNames 1-10 个） */
@Serializable
data class BatchTokensRequest(
    val fileNames: List<String>
)

/** 批量确认入库请求（items 1-10 个） */
@Serializable
data class BatchConfirmRequest(
    val items: List<ConfirmRequest>
)

/** 批量删除请求（ids 1-100 个） */
@Serializable
data class BatchDeleteRequest(
    val ids: List<Long>
)

@Serializable
data class BatchDeleteData(
    val deletedCount: Int = 0
)

@Serializable
data class PhotoPageData(
    val list: List<PhotoItem> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val size: Int = 20
)

/** 登录设备审计记录 */
@Serializable
data class DeviceItem(
    val id: Long,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val loginIp: String? = null,
    val loginTime: String? = null,
    val logoutTime: String? = null
)
