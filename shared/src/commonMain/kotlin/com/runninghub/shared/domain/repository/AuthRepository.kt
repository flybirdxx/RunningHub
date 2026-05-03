package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.User

sealed class SmsError(message: String) : RuntimeException(message) {
    class WrongCode : SmsError("验证码错误，请重新输入")
    class CodeExpired : SmsError("验证码已过期，请重新获取")
    class AccountNotFound : SmsError("该手机号未注册 RunningHub 账号，请前往 runninghub.cn 注册")
    class RateLimited : SmsError("发送过于频繁，请稍后再试")
    class DailyLimit : SmsError("今日发送次数已达上限，请明日再试")
    class Network(message: String) : SmsError(message)
    class Unknown(message: String) : SmsError(message)
}

interface AuthRepository {
    suspend fun login(phone: String, password: String): Result<User>
    suspend fun sendSmsCode(phone: String): Result<Unit>
    suspend fun smsLogin(phone: String, code: String): Result<User>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun refreshTokenIfNeeded(): Result<String>
    suspend fun getCurrentAuthToken(): String?
    suspend fun getCurrentUserId(): String?
}
