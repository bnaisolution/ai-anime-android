package org.dpdns.bnai.anime.core.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.dpdns.bnai.anime.core.auth.TokenManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "https://api.bnai.dpdns.org/"

    @Volatile
    private var apiServiceInstance: AnimeApiService? = null

    fun create(
        baseUrl: String = BASE_URL,
        tokenManager: TokenManager? = null,
        client: OkHttpClient? = null
    ): AnimeApiService {
        val authAndTraceInterceptor = Interceptor { chain ->
            val tm = tokenManager ?: TokenManager.getInstance()
            val activeToken = tm.getToken()
            val activeProject = tm.getActiveProjectId()
            val requestPath = chain.request().url.encodedPath

            val requestBuilder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("X-Request-Id", UUID.randomUUID().toString())

            if (activeProject.isNotBlank()) {
                requestBuilder.header("X-Project-Id", activeProject)
            }
            if (!activeToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $activeToken")
            }

            val response = chain.proceed(requestBuilder.build())

            if (response.code == 401 && !activeToken.isNullOrBlank() && !requestPath.contains("/auth/login")) {
                val errBody = response.peekBody(512).string().lowercase()
                if (errBody.contains("expired") || errBody.contains("invalid token") || errBody.contains("jwt") || errBody.contains("unauthorized")) {
                    tm.clearToken("Session expired. Please sign in again.")
                }
            }
            response
        }

        val okHttpClient = (client ?: OkHttpClient.Builder().build())
            .newBuilder()
            .addInterceptor(authAndTraceInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }

    fun getApiService(context: Context? = null): AnimeApiService {
        context?.let { TokenManager.init(it.applicationContext) }
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: create(tokenManager = TokenManager.getInstance()).also { apiServiceInstance = it }
        }
    }

    internal fun resetCacheForTests() {
        synchronized(this) {
            apiServiceInstance = null
        }
    }
}
