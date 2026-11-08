package org.dpdns.bnai.anime.ui.batch2

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.character.CharacterListScreen
import org.dpdns.bnai.anime.ui.owner.OwnerOverviewScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.studio.PromptStudioScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.testing.TestTags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Batch2InteractionTest {
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

    @Test
    fun authEmptySubmitShowsValidationError() {
        composeRule.setBatch2Content {
            AuthScreen(navController = rememberNavController())
        }
        composeRule.onNodeWithTag(TestTags.Auth.SUBMIT).performClick()
        composeRule.onNodeWithText("Email and password are required.").assertExists()
    }

    @Test
    fun studioCreateProjectDialogOpensAndCancels() {
        composeRule.setBatch2Content { StudioDashboardScreen() }
        composeRule.onNodeWithTag(TestTags.Studio.CREATE_PROJECT).performClick()
        composeRule.onNodeWithText("Project Title").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Project Title").assertDoesNotExist()
    }

    @Test
    fun settingsTabsSwitchContent() {
        composeRule.setBatch2Content { SettingsScreen() }
        composeRule.onNodeWithText("AI & Vision Providers").assertExists()
        composeRule.onNodeWithText("Compute Workers").performClick()
        composeRule.onNodeWithText("GPU & CPU Worker Configuration").assertExists()
    }

    @Test
    fun promptStudioCategoryTabSwitchesPrompt() {
        composeRule.setBatch2Content { PromptStudioScreen() }
        composeRule.onNodeWithTag(TestTags.item(TestTags.PromptStudio.TAB_PREFIX, "EPISODE")).performClick()
        composeRule.onNodeWithText("Default Episode Screenplay Prompt").assertExists()
    }

    @Test
    fun productionControlTagsExist() {
        composeRule.setBatch2Content { ProductionScreen() }
        composeRule.onNodeWithTag(TestTags.Production.PAUSE).assertExists()
        composeRule.onNodeWithTag(TestTags.Production.CANCEL).assertExists()
        composeRule.onNodeWithTag(TestTags.Production.SYNC).assertExists()
    }

    @Test
    fun projectSettingsDialogOpensAndCancels() {
        composeRule.setBatch2Content { ProjectDetailScreen() }
        composeRule.onNodeWithTag(TestTags.ProjectDetail.SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithText("Project Settings").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Project Settings").assertDoesNotExist()
    }

    @Test
    fun ownerEmergencyStopOpensConfirmation() {
        composeRule.setBatch2Content { OwnerOverviewScreen() }
        composeRule.onNodeWithTag(TestTags.Owner.EMERGENCY_STOP).performClick()
        composeRule.onNodeWithText("Emergency Stop All Workers?").assertExists()
        composeRule.onNodeWithTag(TestTags.Dialog.CONFIRM).assertExists()
        composeRule.onNodeWithTag(TestTags.Dialog.CANCEL).assertExists()
        composeRule.onNodeWithTag(TestTags.Dialog.CANCEL).performClick()
    }

    @Test
    fun characterListAddDialogOpens() {
        composeRule.setBatch2Content { CharacterListScreen() }
        composeRule.onNodeWithText("Add Character").performClick()
        composeRule.onNodeWithText("Character Name").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
    }
}
