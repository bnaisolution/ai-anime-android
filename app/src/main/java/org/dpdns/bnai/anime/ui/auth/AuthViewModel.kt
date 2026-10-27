package org.dpdns.bnai.anime.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.AnimeApiService
import org.dpdns.bnai.anime.core.network.LoginRequest
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.RetrofitClient

class AuthViewModel(
    private val apiService: AnimeApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val authState: StateFlow<Resource<String>> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _authState.value = Resource.Error("Email and password are required.")
            return
        }

        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = apiService.login(
                    LoginRequest(
                        email = trimmedEmail,
                        password = password
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.token.isNotBlank()) {
                        tokenManager.saveToken(body.token)
                        _authState.value = Resource.Success(body.token)
                    } else {
                        _authState.value = Resource.Error("Invalid server response: missing token.")
                    }
                } else {
                    _authState.value = Resource.Error("Login failed (HTTP ${response.code()}).")
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(e.localizedMessage ?: "Network error occurred.")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            val tokenManager = TokenManager.init(appContext)
            val apiService = RetrofitClient.getApiService(appContext)
            return AuthViewModel(apiService, tokenManager) as T
        }
    }
}
