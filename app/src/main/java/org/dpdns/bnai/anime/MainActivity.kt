package org.dpdns.bnai.anime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import org.dpdns.bnai.anime.core.di.DefaultAppContainer
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.navigation.AppNavGraph
import org.dpdns.bnai.anime.ui.theme.AnimeStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = DefaultAppContainer(applicationContext)
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AnimeStudioTheme {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
