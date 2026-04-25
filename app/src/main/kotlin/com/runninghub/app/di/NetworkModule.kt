package com.runninghub.app.di

import com.runninghub.app.data.remote.api.WebAppApi
import com.runninghub.app.data.remote.api.AudioApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * [INPUT]: OkHttpClient, Gson
 * [OUTPUT]: 对外提供 Retrofit 实例及 API 契约服务
 * [POS]: 网络层依赖注入中心
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://www.runninghub.cn/api/"
    // Developer API Key
    // private const val API_KEY = "e1259dcb9b9b4d5faa140bccc6231a91" // Removed as per instruction

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        @ApplicationContext context: android.content.Context
    ): OkHttpClient {
        // We use a direct SharedPreferences access here to avoid circular dependency loop
        // if we tried to inject UserPreferencesRepository which might depend on something else (though it shouldn't).
        // Safest is to just read the prefs directly or perform a lazy look up.
        // Actually, injecting UserPreferencesRepository is fine as it only depends on Context.

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                val apiKey = prefs.getString("api_key", "") ?: ""
                val enterpriseApiKey = prefs.getString("enterprise_api_key", "") ?: ""
                val cookie = prefs.getString("user_cookie", "") ?: ""

                val originalRequest = chain.request()
                val url = originalRequest.url.toString()
                val isAudioApi = url.contains("rhart-audio") || url.contains("/openapi/v2/query")

                val requestBuilder = originalRequest.newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Origin", "https://www.runninghub.cn")

                // Robust Referer Check
                val hasSpecificReferer = originalRequest.headers("Referer").isNotEmpty()
                val isFollowApi = url.contains("/uc/follow/")
                
                if (!hasSpecificReferer && !isFollowApi) {
                    requestBuilder.addHeader("Referer", "https://www.runninghub.cn/")
                }

                // Authorization Strategy: 
                // 1. If Audio API AND Enterprise Key exists -> Use Enterprise Key
                // 2. Else If Cookie exists -> Use Token from Cookie
                // 3. Else If App Key exists -> Use App Key
                
                if (isAudioApi && enterpriseApiKey.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $enterpriseApiKey")
                } else if (cookie.isNotEmpty()) {
                    requestBuilder.addHeader("Cookie", cookie)
                    val tokenMatch = Regex("Rh-AccessToken=([^;]+)", RegexOption.IGNORE_CASE).find(cookie)
                    if (tokenMatch != null) {
                        val token = tokenMatch.groupValues[1].trim()
                        if (token.isNotEmpty()) {
                            requestBuilder.addHeader("Authorization", "Bearer $token")
                        }
                    } else if (apiKey.isNotEmpty()) {
                         requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                    }
                } else if (apiKey.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWebAppApi(retrofit: Retrofit): WebAppApi {
        return retrofit.create(WebAppApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAudioApi(retrofit: Retrofit): AudioApi {
        return retrofit.create(AudioApi::class.java)
    }
}
