package com.runninghub.feature.auth.data.remote.dto

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthMappersTest {

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
