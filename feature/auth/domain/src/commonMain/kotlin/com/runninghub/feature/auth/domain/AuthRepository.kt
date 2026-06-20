package com.runninghub.feature.auth.domain

import com.runninghub.core.model.User

sealed class SmsError(message: String) : RuntimeException(message) {
    class WrongCode : SmsError("楠岃瘉鐮侀敊璇紝璇烽噸鏂拌緭鍏?")
    class CodeExpired : SmsError("楠岃瘉鐮佸凡杩囨湡锛岃閲嶆柊鑾峰彇")
    class AccountNotFound : SmsError("璇ユ墜鏈哄彿鏈敞鍐?RunningHub 璐﹀彿锛岃鍓嶅線 runninghub.cn 娉ㄥ唽")
    class RateLimited : SmsError("鍙戦€佽繃浜庨绻侊紝璇风◢鍚庡啀璇?")
    class DailyLimit : SmsError("浠婃棩鍙戦€佹鏁板凡杈句笂闄愶紝璇锋槑鏃ュ啀璇?")
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
