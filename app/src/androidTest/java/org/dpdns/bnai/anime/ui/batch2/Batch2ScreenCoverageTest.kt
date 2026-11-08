package org.dpdns.bnai.anime.ui.batch2

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.ui.advanced.BudgetLedgerScreen
import org.dpdns.bnai.anime.ui.advanced.CanonViewerScreen
import org.dpdns.bnai.anime.ui.advanced.CharacterDeepDiveScreen
import org.dpdns.bnai.anime.ui.advanced.ContinuityCheckerScreen
import org.dpdns.bnai.anime.ui.advanced.JobsMonitorScreen
import org.dpdns.bnai.anime.ui.advanced.LoreExplorerScreen
import org.dpdns.bnai.anime.ui.advanced.QuotaDashboardScreen
import org.dpdns.bnai.anime.ui.advanced.VisionAnalysisScreen
import org.dpdns.bnai.anime.ui.analytics.AnalyticsScreen
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.character.CharacterDetailScreen
import org.dpdns.bnai.anime.ui.character.CharacterListScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.owner.AdminQueuesScreen
import org.dpdns.bnai.anime.ui.owner.DatabaseScreen
import org.dpdns.bnai.anime.ui.owner.OwnerOverviewScreen
import org.dpdns.bnai.anime.ui.owner.WorkerManagerScreen
import org.dpdns.bnai.anime.ui.owner.WorkflowManagerScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.production.SceneDetailScreen
import org.dpdns.bnai.anime.ui.project.BibleScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.studio.PromptStudioScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.testing.TestTags
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Batch2ScreenCoverageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        TokenManager.resetForTests()
    }

    @After
    fun tearDown() {
        TokenManager.resetForTests()
    }

    private fun assertScreen(tag: String, title: String, content: @Composable () -> Unit) {
        composeRule.setBatch2Content { content() }
        composeRule.onNodeWithTag(tag).assertExists()
        composeRule.onAllNodesWithText(title, substring = true)[0].assertExists()
    }

    @Test fun adminQueuesScreenRenders() = assertScreen(TestTags.Screen.ADMIN_QUEUES, "Pipeline Queues") { AdminQueuesScreen() }
    @Test fun aiBrainScreenRenders() = assertScreen(TestTags.Screen.AI_BRAIN, "AI Studio Director") { AIBrainScreen() }
    @Test fun analyticsScreenRenders() = assertScreen(TestTags.Screen.ANALYTICS, "Production & Audience Analytics") { AnalyticsScreen() }
    @Test fun auditLogScreenRenders() = assertScreen(TestTags.Screen.AUDIT_LOG, "Audit Activity Trail") { org.dpdns.bnai.anime.ui.owner.AuditLogScreen() }
    @Test fun authScreenRenders() = assertScreen(TestTags.Screen.AUTH, "AI Cinematic Anime Studio") { AuthScreen(navController = rememberNavController()) }
    @Test fun bibleScreenRenders() = assertScreen(TestTags.Screen.BIBLE, "Story Bible") { BibleScreen() }
    @Test fun budgetLedgerScreenRenders() = assertScreen(TestTags.Screen.BUDGET_LEDGER, "Budget Ledger & Reservations") { BudgetLedgerScreen() }
    @Test fun canonScreenRenders() = assertScreen(TestTags.Screen.CANON, "Canon & Facts") { CanonViewerScreen() }
    @Test fun characterDeepDiveScreenRenders() = assertScreen(TestTags.Screen.CHARACTER_DEEP_DIVE, "Select Character") { CharacterDeepDiveScreen() }
    @Test fun characterDetailScreenRenders() = assertScreen(TestTags.Screen.CHARACTER_DETAIL, "Character Profile") { CharacterDetailScreen() }
    @Test fun characterListScreenRenders() = assertScreen(TestTags.Screen.CHARACTERS, "No characters in this project yet.") { CharacterListScreen() }
    @Test fun continuityScreenRenders() = assertScreen(TestTags.Screen.CONTINUITY, "Continuity Checker") { ContinuityCheckerScreen() }
    @Test fun databaseScreenRenders() = assertScreen(TestTags.Screen.DATABASE, "Database Connectivity") { DatabaseScreen() }
    @Test fun episodePlannerScreenRenders() = assertScreen(TestTags.Screen.EPISODES, "Episode Planner") { EpisodePlannerScreen() }
    @Test fun jobsMonitorScreenRenders() = assertScreen(TestTags.Screen.JOBS_MONITOR, "Pipeline Jobs") { JobsMonitorScreen() }
    @Test fun loreExplorerScreenRenders() = assertScreen(TestTags.Screen.LORE_EXPLORER, "World Wiki") { LoreExplorerScreen() }
    @Test fun mediaLibraryScreenRenders() = assertScreen(TestTags.Screen.MEDIA_LIBRARY, "Project Media Library") { MediaLibraryScreen() }
    @Test fun notificationScreenRenders() = assertScreen(TestTags.Screen.NOTIFICATIONS, "Notifications") { NotificationScreen() }
    @Test fun ownerOverviewScreenRenders() = assertScreen(TestTags.Screen.OWNER_OVERVIEW, "Owner Overview") { OwnerOverviewScreen() }
    @Test fun productionScreenRenders() = assertScreen(TestTags.Screen.PRODUCTION, "Production Pipeline") { ProductionScreen() }
    @Test fun projectDetailScreenRenders() = assertScreen(TestTags.Screen.PROJECT_DETAIL, "Project Detail") { ProjectDetailScreen() }
    @Test fun promptStudioScreenRenders() = assertScreen(TestTags.Screen.PROMPT_STUDIO, "Prompt Studio") { PromptStudioScreen() }
    @Test fun publishScreenRenders() = assertScreen(TestTags.Screen.PUBLISH, "Publish Studio") { PublishScreen() }
    @Test fun qcScreenRenders() = assertScreen(TestTags.Screen.QC, "Quality Control (QC Gate)") { QCScreen() }
    @Test fun quickToolsScreenRenders() = assertScreen(TestTags.Screen.QUICK_TOOLS, "Quick Generate (Ad-Hoc Preview)") { QuickToolsScreen() }
    @Test fun quotaDashboardScreenRenders() = assertScreen(TestTags.Screen.QUOTA_DASHBOARD, "Quota Governor Dashboard (WF-12)") { QuotaDashboardScreen() }
    @Test fun reviewInboxScreenRenders() = assertScreen(TestTags.Screen.REVIEW_INBOX, "Review & Approvals") { ReviewInboxScreen() }
    @Test fun sceneDetailScreenRenders() = assertScreen(TestTags.Screen.SCENES, "Scene Inspector") { SceneDetailScreen(1) }
    @Test fun settingsScreenRenders() = assertScreen(TestTags.Screen.SETTINGS, "Studio Infrastructure & Settings") { SettingsScreen() }
    @Test fun studioDashboardScreenRenders() = assertScreen(TestTags.Screen.STUDIO, "Create Project") { StudioDashboardScreen() }
    @Test fun visionAnalysisScreenRenders() = assertScreen(TestTags.Screen.VISION_ANALYSIS, "Vision DNA Analyzer") { VisionAnalysisScreen() }
    @Test fun workerManagerScreenRenders() = assertScreen(TestTags.Screen.WORKERS, "Compute Worker Registry") { WorkerManagerScreen() }
    @Test fun workflowManagerScreenRenders() = assertScreen(TestTags.Screen.WORKFLOWS, "Workflow Manager (14 Frozen Workflows)") { WorkflowManagerScreen() }
}
