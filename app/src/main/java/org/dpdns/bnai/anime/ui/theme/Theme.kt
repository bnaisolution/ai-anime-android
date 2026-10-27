package org.dpdns.bnai.anime.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val DarkColorScheme = darkColorScheme(primary = Primary, onPrimary = Color.White, secondary = Secondary, onSecondary = Color.White, background = BackgroundDark, onBackground = Color.White, surface = SurfaceDark, onSurface = Color.White, error = Error)
@Composable
fun AnimeStudioTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = DarkColorScheme, typography = AppTypography, content = content) }
