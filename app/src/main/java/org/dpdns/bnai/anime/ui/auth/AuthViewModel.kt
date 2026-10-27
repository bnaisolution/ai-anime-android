package org.dpdns.bnai.anime.ui.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.core.auth.TokenManager
class AuthViewModel(private val api: AnimeApiService, private val tokenManager: TokenManager) : ViewModel() {
    private val _authState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val authState: StateFlow<Resource<String>> = _authState
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val res = api.login(LoginRequest(email, password))
                if (res.isSuccessful && res.body() != null) { tokenManager.saveToken(res.body()!!.token); _authState.value = Resource.Success(res.body()!!.token) }
                else { _authState.value = Resource.Error("Login failed: HTTP ${res.code()}") }
            } catch (e: Exception) { _authState.value = Resource.Error(e.message ?: "Network error") }
        }
    }
}
