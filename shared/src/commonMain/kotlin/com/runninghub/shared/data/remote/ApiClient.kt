package com.runninghub.shared.data.remote

import com.runninghub.shared.platform.createPlatformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiConfig {
    const val BASE_URL = "https://www.runninghub.cn/api/"
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
}

fun createApiClient(
    getApiKey: () -> String,
    getEnterpriseApiKey: () -> String,
    getCookie: () -> String
): HttpClient {
    return createPlatformHttpClient().config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                prettyPrint = false
            })
        }

        install(Logging) {
            level = LogLevel.HEADERS
        }

        defaultRequest {
            url(ApiConfig.BASE_URL)
            contentType(ContentType.Application.Json)
            headers.append("User-Agent", ApiConfig.USER_AGENT)
            headers.append("Accept", "application/json, text/plain, */*")
            headers.append("Origin", "https://www.runninghub.cn")

            val cookie = getCookie()
            val apiKey = getApiKey()
            val enterpriseApiKey = getEnterpriseApiKey()

            if (cookie.isNotEmpty()) {
                headers.append("Cookie", cookie)
                val tokenMatch = Regex("Rh-AccessToken=([^;]+)", RegexOption.IGNORE_CASE).find(cookie)
                val token = tokenMatch?.groupValues?.getOrNull(1)?.trim()
                if (!token.isNullOrEmpty()) {
                    headers.append("Authorization", "Bearer $token")
                } else if (apiKey.isNotEmpty()) {
                    headers.append("Authorization", "Bearer $apiKey")
                }
            } else if (apiKey.isNotEmpty()) {
                headers.append("Authorization", "Bearer $apiKey")
            }
        }
    }
}
