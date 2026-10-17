package com.example.album.data.model

import kotlinx.serialization.Serializable

/** 统一响应结构，code 与 HTTP 状态码一致 */
@Serializable
data class ApiEnvelope<T>(
    val code: Int = 0,
    val msg: String? = null,
    val data: T? = null
)

@Serializable
data class UserInfo(
    val id: Long,
    val phone: String,
    val nickname: String? = null,
    val admin: Boolean = false
)

/** 注册 / 登录成功返回；secretUuid 仅注册时非空（仅此一次） */
@Serializable
data class AuthData(
    val token: String,
    val secretUuid: String? = null,
    val user: UserInfo
)

@Serializable
data class RegisterRequest(
    val phone: String,
    val password: String,
    val nickname: String? = null,
    val deviceId: String? = null,
    val deviceName: String? = null
)

@Serializable
data class LoginRequest(
    val phone: String,
    val password: String,
    val secretUuid: String,
    val deviceId: String? = null,
    val deviceName: String? = null
)

@Serializable
data class ResetSecretRequest(
    val phone: String,
    val password: String
)

@Serializable
data class ResetSecretData(
    val secretUuid: String,
    val notice: String? = null
)
