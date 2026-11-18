package org.dpdns.bnai.anime.ui.brain

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.ui.LocalAppContainer

@Composable
fun rememberSharedAIDirectorViewModel(): AIDirectorViewModel {
    val container = LocalAppContainer.current
    val activity = LocalContext.current as? ComponentActivity
    return if (activity != null) {
        viewModel(
            viewModelStoreOwner = activity,
            factory = container.viewModelFactory
        )
    } else {
        viewModel(factory = container.viewModelFactory)
    }
}
