package org.dpdns.bnai.anime.core.network
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.dpdns.bnai.anime.core.auth.TokenManager
import java.util.concurrent.TimeUnit
object RetrofitClient {
    private const val BASE_URL = "https://api.bnai.dpdns.org/"
    fun create(tokenManager: TokenManager): AnimeApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().header("Accept", "application/json")
                tokenManager.getToken()?.let { req.header("Authorization", "Bearer $it") }
                chain.proceed(req.build())
            }.connectTimeout(30, TimeUnit.SECONDS).build()
        return Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(AnimeApiService::class.java)
    }
}
