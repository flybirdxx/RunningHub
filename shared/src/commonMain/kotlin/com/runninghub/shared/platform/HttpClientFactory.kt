package com.runninghub.shared.platform

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient
