package com.runninghub.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证跨平台 MD5 兼容旧登录协议的输出格式。
 *
 * 这里不测试安全强度，只固定 Android/JVM 与 iOS actual 必须返回同一套小写十六进制摘要，
 * 避免密码登录请求在不同平台生成不同参数。
 */
class HashingTest {

    /**
     * 常规输入应生成 32 位小写十六进制摘要。
     */
    @Test
    fun `md5 returns lowercase hex digest for regular input`() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", md5("abc"))
    }

    /**
     * 空输入也必须被支持，避免 iOS actual 对空数组取地址时崩溃。
     */
    @Test
    fun `md5 supports empty input`() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5(""))
    }
}
