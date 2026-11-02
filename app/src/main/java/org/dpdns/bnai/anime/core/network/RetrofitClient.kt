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
    // Fastify API Authority BASE_URL (Part 5.1 & Part 15.4)
    const val BASE_URL = "https://api.bnai.dpdns.org/"

    @Volatile
    private var apiServiceInstance: AnimeApiService? = null

    fun create(tokenManager: TokenManager? = null): AnimeApiService {
        val authAndTraceInterceptor = Interceptor { chain ->
            val tm = tokenManager ?: TokenManager.getInstance()
            val activeToken = tm?.getToken()
            val activeProject = tm?.getActiveProjectId().orEmpty()

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

            // Patch 2: Clear token on 401, surface session expiry
            if (response.code == 401) {
                tm?.clearToken("Session expired. Please log in again.")
            }
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authAndTraceInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }

    fun getApiService(context: Context? = null): AnimeApiService {
        context?.let { TokenManager.init(it.applicationContext) }
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: create(TokenManager.getInstance()).also { apiServiceInstance = it }
        }
    }
}
