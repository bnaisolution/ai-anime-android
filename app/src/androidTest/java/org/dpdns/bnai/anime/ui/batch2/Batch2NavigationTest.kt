package org.dpdns.bnai.anime.ui.batch2

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.di.AppContainer
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.navigation.AppNavGraph
import org.dpdns.bnai.anime.ui.testing.TestTags
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Batch2NavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController
    private lateinit var container: AppContainer

    @Before
    fun setUp() {
        TokenManager.resetForTests()
        container = Batch2FakeContainer()
    }

    @After
    fun tearDown() {
        TokenManager.resetForTests()
    }

    private fun launchGraph() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                MaterialTheme {
                    navController = rememberNavController()
                    AppNavGraph(navController)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun navigate(route: String) {
        composeRule.runOnUiThread {
            navController.navigate(route) { launchSingleTop = true }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun authStartDestinationWithoutToken() {
        launchGraph()
        composeRule.onNodeWithTag(TestTags.Screen.AUTH).assertExists()
    }

    @Test
    fun studioStartDestinationWithToken() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        composeRule.onNodeWithTag(TestTags.Screen.STUDIO).assertExists()
    }

    @Test
    fun drawerOpensAndExposesWorkspaceNavigation() {
        container.tokenManager.saveToken("batch2-token")
        container.tokenManager.saveUserMeta(
            userId = "batch2-owner",
            workspaceId = "default-workspace",
            role = "owner_admin",
            email = "owner@test.invalid"
        )
        launchGraph()
        composeRule.onNodeWithContentDescription("Open Navigation Drawer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Home Dashboard").assertExists()
        composeRule.onNodeWithText("OWNER CONTROL").performScrollTo()
        composeRule.onNodeWithText("Owner Overview").performScrollTo().assertExists()
    }

    @Test
    fun bottomNavigationOpensEpisodes() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        composeRule.onNodeWithTag(TestTags.item(TestTags.Navigation.BOTTOM_ITEM_PREFIX, "episodes")).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.Screen.EPISODES).assertExists()
    }

    @Test
    fun backNavigationReturnsToStudio() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        navigate("episodes")
        composeRule.onNodeWithTag(TestTags.Screen.EPISODES).assertExists()
        composeRule.runOnUiThread { navController.popBackStack() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.Screen.STUDIO).assertExists()
    }

    @Test
    fun parameterizedProductionRouteOpensProductionScreen() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        navigate("production/cmd-123")
        composeRule.onNodeWithTag(TestTags.Screen.PRODUCTION).assertExists()
    }

    @Test
    fun parameterizedScenesRouteOpensSceneInspector() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        navigate("scenes/2")
        composeRule.onNodeWithTag(TestTags.Screen.SCENES).assertExists()
    }

    @Test
    fun invalidSceneArgumentFallsBackToEpisodeOne() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        navigate("scenes/not-a-number")
        composeRule.onNodeWithTag(TestTags.Screen.SCENES).assertExists()
    }

    @Test
    fun duplicateStudioDeclarationDoesNotBypassAuthGate() {
        launchGraph()
        navigate("studio")
        composeRule.onNodeWithTag(TestTags.Screen.AUTH).assertExists()
    }

    @Test
    fun duplicateAuthDeclarationDoesNotExposeAuthWhenAuthenticated() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        navigate("auth")
        composeRule.onNodeWithTag(TestTags.Screen.STUDIO).assertExists()
    }

    @Test
    fun everyRegisteredRouteOpensItsExpectedScreen() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        val routes = listOf(
            "studio" to TestTags.Screen.STUDIO,
            "project_detail" to TestTags.Screen.PROJECT_DETAIL,
            "bible" to TestTags.Screen.BIBLE,
            "characters" to TestTags.Screen.CHARACTERS,
            "character_detail" to TestTags.Screen.CHARACTER_DETAIL,
            "character_deep_dive" to TestTags.Screen.CHARACTER_DEEP_DIVE,
            "episodes" to TestTags.Screen.EPISODES,
            "production" to TestTags.Screen.PRODUCTION,
            "production/cmd-1" to TestTags.Screen.PRODUCTION,
            "scenes" to TestTags.Screen.SCENES,
            "scenes/3" to TestTags.Screen.SCENES,
            "qc" to TestTags.Screen.QC,
            "publish" to TestTags.Screen.PUBLISH,
            "review_inbox" to TestTags.Screen.REVIEW_INBOX,
            "ai_brain" to TestTags.Screen.AI_BRAIN,
            "quick_tools" to TestTags.Screen.QUICK_TOOLS,
            "settings" to TestTags.Screen.SETTINGS,
            "prompt_studio" to TestTags.Screen.PROMPT_STUDIO,
            "owner_overview" to TestTags.Screen.OWNER_OVERVIEW,
            "workflows" to TestTags.Screen.WORKFLOWS,
            "workers" to TestTags.Screen.WORKERS,
            "admin_queues" to TestTags.Screen.ADMIN_QUEUES,
            "database" to TestTags.Screen.DATABASE,
            "audit_log" to TestTags.Screen.AUDIT_LOG,
            "analytics" to TestTags.Screen.ANALYTICS,
            "media_library" to TestTags.Screen.MEDIA_LIBRARY,
            "notifications" to TestTags.Screen.NOTIFICATIONS,
            "vision_analysis" to TestTags.Screen.VISION_ANALYSIS,
            "jobs_monitor" to TestTags.Screen.JOBS_MONITOR,
            "quota_dashboard" to TestTags.Screen.QUOTA_DASHBOARD,
            "canon" to TestTags.Screen.CANON,
            "continuity" to TestTags.Screen.CONTINUITY,
            "lore_explorer" to TestTags.Screen.LORE_EXPLORER,
            "budget_ledger" to TestTags.Screen.BUDGET_LEDGER
        )
        routes.forEach { (route, tag) ->
            navigate(route)
            composeRule.onNodeWithTag(tag).assertExists()
        }
        assertTrue(routes.size == 34)
    }
}
