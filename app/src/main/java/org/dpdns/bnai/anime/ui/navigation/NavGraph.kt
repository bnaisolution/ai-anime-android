package org.dpdns.bnai.anime.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.analytics.AnalyticsScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.owner.OwnerOverviewScreen
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") { AuthScreen(navController) }
        composable("studio") { StudioDashboardScreen(onProjectClick = { id -> navController.navigate("project/$id") }) }
        composable("project/{id}") { ProjectDetailScreen(id = it.arguments?.getString("id") ?: "") }
        composable("episodes") { EpisodePlannerScreen(onStartProduction = { id -> navController.navigate("production/$id") }) }
        composable("production/{id}") { ProductionScreen(id = it.arguments?.getString("id") ?: "") }
        composable("library") { MediaLibraryScreen() }
        composable("reviews") { ReviewInboxScreen() }
        composable("qc") { QCScreen(onProceed = { navController.navigate("publish") }) }
        composable("publish") { PublishScreen() }
        composable("analytics") { AnalyticsScreen() }
        composable("brain") { AIBrainScreen() }
        composable("tools") { QuickToolsScreen() }
        composable("notifications") { NotificationScreen() }
        composable("settings") { SettingsScreen() }
        composable("owner") { OwnerOverviewScreen() }
    }
}
