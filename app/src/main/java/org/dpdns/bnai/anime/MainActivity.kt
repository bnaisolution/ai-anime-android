package org.dpdns.bnai.anime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.compose.rememberNavController
import org.dpdns.bnai.anime.core.di.DefaultAppContainer
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.navigation.AppNavGraph
import org.dpdns.bnai.anime.ui.theme.AnimeStudioTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            MaestroSeed.handle(this, intent)
        }
        val baseContainer = DefaultAppContainer(applicationContext)
        val container = if (BuildConfig.DEBUG && MaestroSeed.isSeeded(this)) {
            MaestroSeed.container(baseContainer)
        } else {
            baseContainer
        }
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AnimeStudioTheme {
                    val navController = rememberNavController()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { testTagsAsResourceId = true }
                    ) {
                        AppNavGraph(navController = navController)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (BuildConfig.DEBUG && MaestroSeed.handle(this, intent)) {
            recreate()
        }
    }
}
