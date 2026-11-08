package org.dpdns.bnai.anime.ui.batch2

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.designsystem.ConfirmationDialog
import org.dpdns.bnai.anime.ui.navigation.AppNavGraph
import org.dpdns.bnai.anime.ui.testing.TestTags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Batch2AccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController
    private lateinit var container: Batch2FakeContainer

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
        composeRule.runOnUiThread { navController.navigate(route) { launchSingleTop = true } }
        composeRule.waitForIdle()
    }

    @Test
    fun drawerAndNavigationIconsHaveAccessibleLabels() {
        container.tokenManager.saveToken("batch2-token")
        launchGraph()
        composeRule.onNodeWithContentDescription("Open Navigation Drawer").assertExists()
        navigate("project_detail")
        composeRule.onNodeWithContentDescription("Ask AI Director").assertExists()
        navigate("bible")
        composeRule.onNodeWithContentDescription("Navigate Back").assertExists()
    }

    @Test
    fun errorRetryControlHasAccessibleLabel() {
        composeRule.setBatch2Content {
            ResourceStateBox(resource = Resource.Error("offline"), onRetry = {}) {}
        }
        composeRule.onNodeWithContentDescription("Retry").assertExists()
    }

    @Test
    fun projectRefreshControlHasAccessibleLabel() {
        composeRule.setBatch2Content { ProjectSelectorBar(onRefreshClick = {}) }
        composeRule.onNodeWithContentDescription("Refresh project data").assertExists()
    }

    @Test
    fun dialogButtonsExposeTextSemantics() {
        composeRule.setBatch2Content {
            ConfirmationDialog(
                title = "Delete",
                message = "Delete this item?",
                confirmText = "Delete",
                dismissText = "Keep",
                onConfirm = {},
                onDismiss = {}
            )
        }
        composeRule.onNodeWithTag(TestTags.Dialog.CONFIRM).assertTextEquals("Delete")
        composeRule.onNodeWithText("Keep").assertExists()
        composeRule.onNodeWithTag(TestTags.Dialog.CONFIRM).assertExists()
        composeRule.onNodeWithTag(TestTags.Dialog.CANCEL).assertExists()
    }

    @Test
    fun sharedStateControlsAreDiscoverableByTag() {
        composeRule.setBatch2Content {
            ResourceStateBox(resource = Resource.Empty, onRetry = {}) {}
        }
        composeRule.onNodeWithTag(TestTags.State.EMPTY).assertExists()
        composeRule.onNodeWithTag(TestTags.State.RETRY).assertExists()
    }
}
