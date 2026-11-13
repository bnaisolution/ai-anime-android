package org.dpdns.bnai.anime.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.dpdns.bnai.anime.core.di.AppContainer
import org.dpdns.bnai.anime.ui.auth.AuthViewModel
import org.dpdns.bnai.anime.ui.character.CharacterViewModel
import org.dpdns.bnai.anime.ui.preflight.PreflightViewModel
import org.dpdns.bnai.anime.ui.production.ProductionViewModel
import org.dpdns.bnai.anime.ui.project.ProjectViewModel
import org.dpdns.bnai.anime.ui.studio.StudioViewModel

class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) ->
            AuthViewModel(container.apiService, container.tokenManager) as T
        modelClass.isAssignableFrom(CharacterViewModel::class.java) ->
            CharacterViewModel(container.repository) as T
        modelClass.isAssignableFrom(ProductionViewModel::class.java) ->
            ProductionViewModel(container.repository) as T
        modelClass.isAssignableFrom(ProjectViewModel::class.java) ->
            ProjectViewModel(container.repository) as T
        modelClass.isAssignableFrom(StudioViewModel::class.java) ->
            StudioViewModel(container.repository, container.tokenManager) as T
        modelClass.isAssignableFrom(PreflightViewModel::class.java) ->
            PreflightViewModel(container.repository) as T
        else -> throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
    }
}
