package org.dpdns.bnai.anime.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.di.AppContainer
import org.dpdns.bnai.anime.core.network.*

class AuthViewModel(
    private val apiService: AnimeApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val authState: StateFlow<Resource<String>> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.trim().isEmpty() || password.isEmpty()) {
            _authState.value = Resource.Error("Email and password are required.")
            return
        }
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = apiService.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.token?.takeIf { it.isNotBlank() }
                        ?: body?.access_token?.takeIf { it.isNotBlank() }
                    if (token != null) {
                        tokenManager.saveSession(
                            accessToken = token,
                            refreshToken = body?.refreshToken,
                            expiresInSeconds = body?.expiresIn,
                            userId = body?.userId,
                            workspaceId = body?.workspaceId,
                            role = body?.role,
                            email = body?.email ?: email.trim()
                        )
                        _authState.value = Resource.Success(token)
                    } else {
                        _authState.value = Resource.Error("Login response did not include a valid token.")
                    }
                } else {
                    _authState.value = Resource.Error("Invalid email or password (HTTP ${response.code()})", response.code())
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(e.localizedMessage ?: "Network connection failed")
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(container.apiService, container.tokenManager) as T
        }
    }
}
