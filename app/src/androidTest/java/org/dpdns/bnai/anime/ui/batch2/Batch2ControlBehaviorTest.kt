package org.dpdns.bnai.anime.ui.batch2

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.character.CharacterListScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.project.BibleScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.studio.PromptStudioScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.testing.TestTags
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Batch2ControlBehaviorTest {
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

    private fun waitForCall(container: Batch2RecordingContainer, method: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            container.recorder.lastCall(method) != null
        }
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun studioCreateProjectSubmitsEnteredName() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { StudioDashboardScreen() }

        composeRule.onNodeWithTag(TestTags.Studio.CREATE_PROJECT).performClick()
        composeRule.onNodeWithTag(TestTags.Studio.PROJECT_NAME_FIELD).performTextInput("New Show")
        composeRule.onNodeWithTag(TestTags.Studio.CREATE_CONFIRM).performClick()

        waitForCall(container, "createProjectSync")
        assertEquals("New Show", container.recorder.lastCall("createProjectSync")?.args?.get(0))
    }

    @Test
    fun reviewApproveSubmitsApprovedDecision() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { ReviewInboxScreen() }
        waitForTag(TestTags.Review.APPROVE)

        composeRule.onNodeWithTag(TestTags.Review.APPROVE).performClick()

        waitForCall(container, "submitApprovalDecision")
        val call = container.recorder.lastCall("submitApprovalDecision")
        assertEquals("approval-1", call?.args?.get(1))
        assertEquals("APPROVED", call?.args?.get(2))
        assertEquals(3, (call?.args?.get(4) as Number).toInt())
    }

    @Test
    fun reviewRejectSubmitsReasonAndDecision() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { ReviewInboxScreen() }
        waitForTag(TestTags.Review.REJECT)

        composeRule.onNodeWithTag(TestTags.Review.REJECT).performClick()
        waitForTag(TestTags.Review.REJECT_REASON)
        composeRule.onNodeWithTag(TestTags.Review.REJECT_REASON).performTextInput("Canon break")
        composeRule.onNodeWithTag(TestTags.Review.REJECT_CONFIRM).performClick()

        waitForCall(container, "submitApprovalDecision")
        val call = container.recorder.lastCall("submitApprovalDecision")
        assertEquals("REJECTED", call?.args?.get(2))
        assertEquals("Canon break", call?.args?.get(3))
    }

    @Test
    fun promptStudioSaveSubmitsEditedPrompt() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { PromptStudioScreen() }
        waitForCall(container, "fetchSystemPrompts")

        composeRule.onNodeWithTag(TestTags.PromptStudio.EDIT).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.PromptStudio.TEXT_FIELD)
            .performScrollTo()
            .performClick()
            .performTextReplacement("Edited prompt")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.PromptStudio.SAVE)
            .performScrollTo()
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)

        waitForCall(container, "updateSystemPrompt")
        val call = container.recorder.lastCall("updateSystemPrompt")
        assertEquals("prompt-1", call?.args?.get(0))
        assertEquals("Edited prompt", call?.args?.get(2))
        assertEquals("BIBLE", call?.args?.get(4))
    }

    @Test
    fun settingsSavePersistsInfrastructureAndProjectPolicy() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { SettingsScreen() }
        waitForCall(container, "fetchGpuInfrastructure")

        composeRule.onNodeWithTag(TestTags.Settings.SAVE).performScrollTo().performClick()

        waitForCall(container, "saveGpuInfrastructure")
        val infra = container.recorder.lastCall("saveGpuInfrastructure")?.args?.get(0) as? Map<*, *>
        assertEquals("BYOD_CLOUD", infra?.get("preferred_mode"))
        assertEquals("groq", infra?.get("primary_llm_provider"))

        waitForCall(container, "updateProjectSettings")
        val settings = container.recorder.lastCall("updateProjectSettings")?.args?.get(1) as? Map<*, *>
        assertEquals(false, settings?.get("auto_approve"))
        assertEquals("gemini", settings?.get("vision_provider"))
    }

    @Test
    fun projectDetailSavePersistsSettings() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { ProjectDetailScreen() }
        waitForCall(container, "fetchProjectOverviewRaw")

        composeRule.onNodeWithTag(TestTags.ProjectDetail.SETTINGS_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.ProjectDetail.SAVE_SETTINGS).performClick()

        waitForCall(container, "updateProjectSettings")
        val updates = container.recorder.lastCall("updateProjectSettings")?.args?.get(1) as? Map<*, *>
        assertEquals("Batch 2 Project", updates?.get("name"))
        assertEquals("ANIME", updates?.get("visual_medium"))
        assertEquals(false, updates?.get("auto_approve"))
    }

    @Test
    fun productionCancelDispatchesStopCommand() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { ProductionScreen() }

        composeRule.onNodeWithTag(TestTags.Production.CANCEL).performClick()

        waitForCall(container, "submitStudioCommand")
        val call = container.recorder.lastCall("submitStudioCommand")
        assertEquals(CommandIntent.ADMIN_STOP_KAGGLE, call?.args?.get(1))
    }

    @Test
    fun characterAddDispatchesBibleCommandWithName() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { CharacterListScreen() }
        waitForCall(container, "fetchCharacters")

        composeRule.onNodeWithText("Add Character").performClick()
        composeRule.onNodeWithTag(TestTags.Character.NAME_FIELD).performTextInput("Ren")
        composeRule.onNodeWithTag(TestTags.Character.ADD_CONFIRM).performClick()

        waitForCall(container, "submitStudioCommand")
        val call = container.recorder.lastCall("submitStudioCommand")
        assertEquals(CommandIntent.SAVE_BIBLE, call?.args?.get(1))
        val payload = call?.args?.get(4) as? Map<*, *>
        assertTrue(payload?.get("user_prompt").toString().contains("Ren"))
    }

    @Test
    fun bibleSaveDraftSubmitsBiblePayload() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { BibleScreen() }

        composeRule.onNodeWithTag(TestTags.Bible.SAVE_DRAFT).performScrollTo().performClick()

        waitForCall(container, "updateProjectBible")
        val payload = container.recorder.lastCall("updateProjectBible")?.args?.get(1) as? Map<*, *>
        assertTrue(payload?.containsKey("premise") == true)
    }

    @Test
    fun episodePlannerSaveMetadataSubmitsPatch() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { EpisodePlannerScreen() }

        composeRule.onNodeWithTag(TestTags.EpisodePlanner.SAVE_METADATA).performScrollTo().performClick()

        waitForCall(container, "updateEpisodeSync")
        val updates = container.recorder.lastCall("updateEpisodeSync")?.args?.get(2) as? Map<*, *>
        assertTrue(updates?.containsKey("script_text") == true)
    }

    @Test
    fun promptStudioSearchFiltersSavedPrompts() {
        val container = Batch2RecordingContainer(
            fakeState = Batch2FakeState().apply {
                systemPrompts = listOf(
                    SystemPromptDto(
                        id = "prompt-alpha",
                        title = "Alpha Bible",
                        prompt_text = "Alpha instructions",
                        prompt_category = "BIBLE"
                    ),
                    SystemPromptDto(
                        id = "prompt-beta",
                        title = "Beta Bible",
                        prompt_text = "Beta instructions",
                        prompt_category = "BIBLE",
                        is_favorite = true
                    )
                )
            }
        )
        composeRule.setBatch2Content(container) { PromptStudioScreen() }
        waitForTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, "prompt-alpha"))

        composeRule.onNodeWithTag(TestTags.PromptStudio.SEARCH).performTextInput("Beta")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Saved BIBLE Prompts (1)").assertExists()
        composeRule.onNodeWithTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, "prompt-beta")).assertExists()
        composeRule.onNodeWithTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, "prompt-alpha")).assertDoesNotExist()
    }

    @Test
    fun promptStudioFavoritesFilterAndToggleDispatch() {
        val container = Batch2RecordingContainer(
            fakeState = Batch2FakeState().apply {
                systemPrompts = listOf(
                    SystemPromptDto(
                        id = "prompt-alpha",
                        title = "Alpha Bible",
                        prompt_text = "Alpha instructions",
                        prompt_category = "BIBLE"
                    ),
                    SystemPromptDto(
                        id = "prompt-beta",
                        title = "Beta Bible",
                        prompt_text = "Beta instructions",
                        prompt_category = "BIBLE",
                        is_favorite = true
                    )
                )
            }
        )
        composeRule.setBatch2Content(container) { PromptStudioScreen() }
        waitForTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, "prompt-alpha"))

        composeRule.onNodeWithTag(TestTags.PromptStudio.FAVORITES).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Saved BIBLE Prompts (1)").assertExists()
        composeRule.onNodeWithTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, "prompt-beta")).assertExists()
        composeRule.onNodeWithTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, "prompt-alpha")).assertDoesNotExist()

        composeRule.onNodeWithTag(TestTags.item(TestTags.PromptStudio.FAVORITE_PREFIX, "prompt-beta"))
            .performScrollTo()
            .performClick()

        waitForCall(container, "togglePromptFavorite")
        assertEquals("prompt-beta", container.recorder.lastCall("togglePromptFavorite")?.args?.get(0))
    }

    @Test
    fun promptStudioMediumSelectionIsForwardedOnSave() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { PromptStudioScreen() }
        waitForTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, "prompt-1"))

        composeRule.onNodeWithTag(TestTags.PromptStudio.MEDIUM).performClick()
        composeRule.onNodeWithText("Medium: REAL_HUMAN").assertExists()

        composeRule.onNodeWithTag(TestTags.PromptStudio.EDIT).performScrollTo().performClick()
        composeRule.onNodeWithTag(TestTags.PromptStudio.TEXT_FIELD)
            .performScrollTo()
            .performClick()
            .performTextReplacement("Medium aware prompt")
        composeRule.onNodeWithTag(TestTags.PromptStudio.SAVE)
            .performScrollTo()
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)

        waitForCall(container, "updateSystemPrompt")
        val call = container.recorder.lastCall("updateSystemPrompt")
        assertEquals("prompt-1", call?.args?.get(0))
        assertEquals("REAL_HUMAN", call?.args?.get(3))
    }

    @Test
    fun studioCreateProjectAttachesSelectedPrompt() {
        val container = Batch2RecordingContainer()
        composeRule.setBatch2Content(container) { StudioDashboardScreen() }

        composeRule.onNodeWithTag(TestTags.Studio.CREATE_PROJECT).performClick()
        waitForTag(TestTags.item(TestTags.Studio.PROMPT_OPTION_PREFIX, "prompt-1"))
        composeRule.onNodeWithTag(TestTags.item(TestTags.Studio.PROMPT_OPTION_PREFIX, "prompt-1")).performClick()
        composeRule.onNodeWithTag(TestTags.Studio.PROJECT_NAME_FIELD).performTextInput("Attached Show")
        composeRule.onNodeWithTag(TestTags.Studio.CREATE_CONFIRM).performClick()

        waitForCall(container, "createProjectSync")
        val call = container.recorder.lastCall("createProjectSync")
        assertEquals("Attached Show", call?.args?.get(0))
        assertEquals(listOf("prompt-1"), call?.args?.get(1))
    }
}
