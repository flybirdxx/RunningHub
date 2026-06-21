package com.runninghub.feature.auth.data.remote.dto

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthMappersTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `login token response decodes captured server field names`() {
        val response = json.decodeFromString<AuthBaseResponseDto<LoginTokenDataDto>>(
            """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "access_token": "access\u002Dtoken",
                    "refresh_token": "refresh\u002Dtoken",
                    "expire_in": "3600",
                    "identify": "web-session-id",
                    "firstLogin": true,
                    "inviteCodeUsed": null
                  },
                  "traceId": "ignored-by-client"
                }
            """.trimIndent()
        )

        assertEquals(0, response.code)
        assertEquals("access-token", response.data?.accessToken)
        assertEquals("refresh-token", response.data?.refreshToken)
        assertEquals("3600", response.data?.expireIn)
        assertEquals("web-session-id", response.data?.identify)
        assertEquals(true, response.data?.firstLogin)
        assertEquals(null, response.data?.inviteCodeUsed)
    }

    @Test
    fun `user dto maps nested member and wallet information`() {
        val user = UserDto(
            id = "user-1",
            nickName = "Runner",
            headIcon = "https://example.com/avatar.png",
            totalCoin = "12",
            memberInfo = MemberInfoDto(
                memberName = "Pro",
                memberRemainingDays = "7",
                expired = false,
            ),
            walletInfo = WalletInfoDto(
                balance = 3.5,
                currency = "USD",
                currencySymbol = "$",
            ),
            apiKey = "api-key",
            fanCount = null,
        ).toDomain()

        assertEquals("user-1", user.id)
        assertEquals("Runner", user.nickName)
        assertEquals("Pro", user.memberInfo?.memberName)
        assertEquals(3.5, user.walletInfo?.balance)
        assertEquals("api-key", user.apiKey)
        assertEquals("0", user.fanCount)
    }

    @Test
    fun `account status preserves server string values`() {
        val status = AccountStatusDto(
            remainCoins = "100.50",
            currentTaskCounts = "2",
            remainMoney = "8.00",
            currency = "CNY",
            apiType = "personal",
        ).toDomain()

        assertEquals("100.50", status.remainCoins)
        assertEquals("2", status.currentTaskCounts)
        assertEquals("8.00", status.remainMoney)
        assertEquals("CNY", status.currency)
        assertEquals("personal", status.apiType)
    }
}
