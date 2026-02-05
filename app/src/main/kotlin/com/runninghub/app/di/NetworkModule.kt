package com.runninghub.app.di

import com.runninghub.app.data.remote.api.WebAppApi
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
                val cookie = prefs.getString("user_cookie", "") ?: ""

                val requestBuilder = chain.request().newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                    .addHeader("Accept", "application/json, text/plain, */*")

                if (cookie.isNotEmpty()) {
                    requestBuilder.addHeader("Cookie", cookie)
                    // Extract AccessToken from Cookie for Authorization header if not using API Key
                    // Cookie format: ...; Rh-AccessToken=eyJ...; ...
                    // Update: Case insensitive match for Rh-AccessToken / Rh-Accesstoken
                    val tokenMatch = Regex("Rh-AccessToken=([^;]+)", RegexOption.IGNORE_CASE).find(cookie)
                    if (tokenMatch != null) {
                        val token = tokenMatch.groupValues[1]
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    } else if (apiKey.isNotEmpty()) {
                        // Fallback to API Key if no token in cookie
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
}
