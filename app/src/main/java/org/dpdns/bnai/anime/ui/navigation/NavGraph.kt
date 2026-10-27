package org.dpdns.bnai.anime.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.dpdns.bnai.anime.ui.analytics.AnalyticsScreen
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.owner.AuditLogScreen
import org.dpdns.bnai.anime.ui.owner.DatabaseScreen
import org.dpdns.bnai.anime.ui.owner.OwnerOverviewScreen
import org.dpdns.bnai.anime.ui.owner.WorkerManagerScreen
import org.dpdns.bnai.anime.ui.owner.WorkflowManagerScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.production.SceneDetailScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    currentUserRole: String = "owner"
) {
    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        composable("auth") {
            AuthScreen(navController = navController)
        }
        composable("studio") {
            StudioDashboardScreen(
                onProjectClick = { projectId ->
                    navController.navigate("project/$projectId")
                },
                onOpenBrain = { navController.navigate("brain") },
                onOpenQuickTools = { navController.navigate("tools") },
                onOpenNotifications = { navController.navigate("notifications") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenOwnerConsole = { navController.navigate("owner") }
            )
        }
        composable("project/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")
                ?: "11111111-1111-4111-8111-111111111111"
            ProjectDetailScreen(
                projectId = projectId,
                onOpenEpisodes = { navController.navigate("episodes") },
                onOpenReviews = { navController.navigate("reviews") },
                onOpenAnalytics = { navController.navigate("analytics") }
            )
        }
        composable("episodes") {
            EpisodePlannerScreen(
                onStartProduction = { episodeId ->
                    navController.navigate("production/$episodeId")
                }
            )
        }
        composable("production/{episodeId}") { backStackEntry ->
            val episodeId = backStackEntry.arguments?.getString("episodeId") ?: "ep_005"
            ProductionScreen(
                episodeId = episodeId,
                onViewVideoClick = { navController.navigate("qc") },
                onOpenSceneInspector = { navController.navigate("scenes") },
                onOpenMediaLibrary = { navController.navigate("library") }
            )
        }
        composable("scenes") {
            SceneDetailScreen()
        }
        composable("library") {
            MediaLibraryScreen()
        }
        composable("reviews") {
            ReviewInboxScreen()
        }
        composable("qc") {
            QCScreen(
                onProceedToPublish = { navController.navigate("publish") }
            )
        }
        composable("publish") {
            PublishScreen()
        }
        composable("analytics") {
            AnalyticsScreen()
        }
        composable("brain") {
            AIBrainScreen()
        }
        composable("tools") {
            QuickToolsScreen()
        }
        composable("notifications") {
            NotificationScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
        composable("owner") {
            OwnerOverviewScreen(
                userRole = currentUserRole,
                onOpenWorkflows = { navController.navigate("owner/workflows") },
                onOpenWorkers = { navController.navigate("owner/workers") },
                onOpenDatabase = { navController.navigate("owner/database") },
                onOpenAuditLogs = { navController.navigate("owner/audit") }
            )
        }
        composable("owner/workflows") {
            WorkflowManagerScreen()
        }
        composable("owner/workers") {
            WorkerManagerScreen()
        }
        composable("owner/database") {
            DatabaseScreen()
        }
        composable("owner/audit") {
            AuditLogScreen()
        }
    }
}
