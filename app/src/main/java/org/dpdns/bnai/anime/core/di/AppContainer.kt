package org.dpdns.bnai.anime.core.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.AnimeApiService
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.AnimeRepositoryApi
import org.dpdns.bnai.anime.core.network.RetrofitClient
import org.dpdns.bnai.anime.ui.AppViewModelFactory

interface AppContainer {
    val tokenManager: TokenManager
    val apiService: AnimeApiService
    val repository: AnimeRepositoryApi
    val viewModelFactory: ViewModelProvider.Factory
}

class DefaultAppContainer(
    context: Context,
    baseUrl: String = RetrofitClient.BASE_URL
) : AppContainer {
    override val tokenManager: TokenManager = TokenManager.init(context.applicationContext)
    override val apiService: AnimeApiService =
        RetrofitClient.create(baseUrl = baseUrl, tokenManager = tokenManager)
    override val repository: AnimeRepositoryApi =
        AnimeRepository(apiService, tokenManager)
    override val viewModelFactory: ViewModelProvider.Factory =
        AppViewModelFactory(this)
}
