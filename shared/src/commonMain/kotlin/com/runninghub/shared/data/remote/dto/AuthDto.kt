package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PwdLoginRequest(
    @SerialName("mobile") val mobile: String,
    @SerialName("password") val password: String,
    @SerialName("serviceAgreement") val serviceAgreement: Boolean = true,
    @SerialName("channel") val channel: String? = null,
    @SerialName("inviteCode") val inviteCode: String? = null
)

@Serializable
data class LoginTokenData(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expire_in") val expireIn: String = "",
    @SerialName("identify") val identify: String = "",
    @SerialName("firstLogin") val firstLogin: Boolean = false,
    @SerialName("inviteCodeUsed") val inviteCodeUsed: String? = null
)

@Serializable
data class SmsCodeRequest(
    @SerialName("mobile") val mobile: String
)

@Serializable
data class SmsLoginRequest(
    @SerialName("mobile") val mobile: String,
    @SerialName("code") val code: String
)
