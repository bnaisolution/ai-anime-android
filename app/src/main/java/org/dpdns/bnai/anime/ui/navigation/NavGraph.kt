package org.dpdns.bnai.anime.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.production.SceneDetailScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen
import org.dpdns.bnai.anime.ui.owner.*
@Composable
fun AppNavGraph(navController: NavHostController, currentUserRole: String = TokenManager.getInstance()?.getUserRole() ?: "owner") {
    val startRoute = if (!TokenManager.getInstance()?.getToken().isNullOrBlank()) "studio" else "auth"
    NavHost(navController = navController, startDestination = startRoute) {
        composable("auth") { AuthScreen(navController = navController) }
        composable("studio") { StudioDashboardScreen(
            onProjectClick = { id -> TokenManager.getInstance()?.saveActiveProjectId(id); navController.navigate("project/$id") },
            onOpenBrain = { navController.navigate("brain") }, onOpenQuickTools = { navController.navigate("tools") }, onOpenNotifications = {}, onOpenSettings = {}, onOpenOwnerConsole = { navController.navigate("owner") }
        ) }
        composable("project/{projectId}") { b -> ProjectDetailScreen(
            projectId = b.arguments?.getString("projectId") ?: "11111111-1111-1111-1111-111111111111",
            onOpenEpisodes = { navController.navigate("episodes") }, onOpenReviews = { navController.navigate("reviews") }, onOpenAnalytics = {}
        ) }
        composable("episodes") { EpisodePlannerScreen(onStartProduction = { id -> navController.navigate("production/$id") }) }
        composable("production/{episodeId}") { b -> ProductionScreen(
            episodeId = b.arguments?.getString("episodeId") ?: "ep_005",
            onViewVideoClick = { navController.navigate("qc") }, onOpenSceneInspector = { navController.navigate("scenes") }, onOpenMediaLibrary = {}
        ) }
        composable("scenes") { SceneDetailScreen() }
        composable("reviews") { ReviewInboxScreen() }
        composable("qc") { QCScreen(onProceedToPublish = { navController.navigate("publish") }) }
        composable("publish") { PublishScreen() }
        composable("brain") { AIBrainScreen() }
        composable("tools") { QuickToolsScreen() }
        composable("owner") { OwnerOverviewScreen(
            userRole = currentUserRole, onOpenWorkflows = { navController.navigate("owner/workflows") }, onOpenWorkers = { navController.navigate("owner/workers") }, onOpenDatabase = { navController.navigate("owner/database") }, onOpenAuditLogs = { navController.navigate("owner/audit") }
        ) }
        composable("owner/workflows") { WorkflowManagerScreen() }
        composable("owner/workers") { WorkerManagerScreen() }
        composable("owner/database") { DatabaseScreen() }
        composable("owner/audit") { AuditLogScreen() }
    }
}
