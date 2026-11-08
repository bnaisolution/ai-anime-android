package org.dpdns.bnai.anime.ui

import androidx.compose.runtime.staticCompositionLocalOf
import org.dpdns.bnai.anime.core.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided. Wrap this composable in CompositionLocalProvider(LocalAppContainer provides container).")
}
