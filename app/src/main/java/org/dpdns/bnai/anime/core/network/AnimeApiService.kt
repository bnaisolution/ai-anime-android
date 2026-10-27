package org.dpdns.bnai.anime.core.network
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val userId: String)
interface AnimeApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
