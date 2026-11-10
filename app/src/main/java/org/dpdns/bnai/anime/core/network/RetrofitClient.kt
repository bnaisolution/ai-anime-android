package org.dpdns.bnai.anime.core.network

import android.content.Context
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.dpdns.bnai.anime.core.auth.TokenManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "https://api.bnai.dpdns.org/"

    @Volatile
    private var apiServiceInstance: AnimeApiService? = null
    private val refreshLock = Any()
    private val gson = Gson()

    private data class RefreshOutcome(val session: LoginResponse?, val unauthorized: Boolean)

    private fun refreshBlocking(baseUrl: String, refreshToken: String): RefreshOutcome {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val url = "${baseUrl.trimEnd('/')}/api/v1/auth/refresh"
        val body = gson.toJson(RefreshTokenRequest(refreshToken)).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "application/json")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return RefreshOutcome(null, response.code == 400 || response.code == 401 || response.code == 403)
                }
                val raw = response.body?.string().orEmpty()
                RefreshOutcome(gson.fromJson(raw, LoginResponse::class.java), false)
            }
        } catch (_: Exception) {
            RefreshOutcome(null, false)
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    fun create(
        baseUrl: String = BASE_URL,
        tokenManager: TokenManager? = null,
        client: OkHttpClient? = null
    ): AnimeApiService {
        val authAndTraceInterceptor = Interceptor { chain ->
            val tm = tokenManager ?: TokenManager.getInstance()
            val activeToken = tm.getToken()
            val activeProject = tm.getActiveProjectId()

            val requestBuilder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("X-Request-Id", UUID.randomUUID().toString())

            if (activeProject.isNotBlank()) {
                requestBuilder.header("X-Project-Id", activeProject)
            }
            if (!activeToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $activeToken")
            }

            chain.proceed(requestBuilder.build())
        }

        val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
            val failedAuthorization = response.request.header("Authorization")
            if (failedAuthorization.isNullOrBlank()) return@Authenticator null
            if (response.request.url.encodedPath.contains("/auth/")) return@Authenticator null
            val tm = tokenManager ?: TokenManager.getInstance()
            if (responseCount(response) >= 2) {
                tm.clearToken("Session expired. Please sign in again.")
                return@Authenticator null
            }
            synchronized(refreshLock) {
                val failedToken = failedAuthorization.removePrefix("Bearer ").trim()
                val currentToken = tm.getToken()
                if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                    return@Authenticator response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }
                val refreshToken = tm.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    tm.clearToken("Session expired. Please sign in again.")
                    return@Authenticator null
                }
                val outcome = refreshBlocking(baseUrl, refreshToken)
                if (outcome.unauthorized) {
                    tm.clearToken("Session expired. Please sign in again.")
                    return@Authenticator null
                }
                val session = outcome.session
                val accessToken = session?.token?.takeIf { it.isNotBlank() }
                    ?: session?.access_token?.takeIf { it.isNotBlank() }
                if (accessToken.isNullOrBlank()) return@Authenticator null
                tm.saveSession(
                    accessToken = accessToken,
                    refreshToken = session?.refreshToken ?: refreshToken,
                    expiresInSeconds = session?.expiresIn,
                    userId = session?.userId,
                    workspaceId = session?.workspaceId,
                    role = session?.role,
                    email = session?.email ?: tm.getUserEmail()
                )
                response.request.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            }
        }

        val okHttpClient = (client ?: OkHttpClient.Builder().build())
            .newBuilder()
            .addInterceptor(authAndTraceInterceptor)
            .authenticator(tokenAuthenticator)
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
