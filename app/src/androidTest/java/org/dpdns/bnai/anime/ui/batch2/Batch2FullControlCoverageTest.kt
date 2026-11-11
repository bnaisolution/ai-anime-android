package org.dpdns.bnai.anime.ui.batch2

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dpdns.bnai.anime.core.auth.PersistedChatMessage
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.di.AppContainer
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.ui.advanced.BudgetLedgerScreen
import org.dpdns.bnai.anime.ui.advanced.CanonViewerScreen
import org.dpdns.bnai.anime.ui.advanced.CharacterDeepDiveScreen
import org.dpdns.bnai.anime.ui.advanced.ContinuityCheckerScreen
import org.dpdns.bnai.anime.ui.advanced.JobsMonitorScreen
import org.dpdns.bnai.anime.ui.advanced.LoreExplorerScreen
import org.dpdns.bnai.anime.ui.advanced.VisionAnalysisScreen
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.character.CharacterDetailScreen
import org.dpdns.bnai.anime.ui.character.CharacterListScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.owner.AdminQueuesScreen
import org.dpdns.bnai.anime.ui.owner.AuditLogScreen
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Batch2FullControlCoverageTest {
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

    private fun fakeContainer(
        state: Batch2FakeState = Batch2FakeState(),
        tokenManager: TokenManager = TokenManager(null)
    ): Batch2RecordingContainer =
        Batch2RecordingContainer(fakeState = state, tokenManager = tokenManager)

    private fun setContent(
        container: AppContainer = fakeContainer(),
        content: @Composable () -> Unit
    ) {
        composeRule.setBatch2Content(container, content)
        composeRule.waitForIdle()
    }

    private fun waitForText(label: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(label, substring = substring, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertText(label: String, substring: Boolean = false) {
        val found = composeRule.onAllNodesWithText(label, substring = substring, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        assertTrue("Missing control text: $label", found)
        println("CONTROL_ASSERT_PASS: $label")
    }

    private fun assertDesc(description: String) {
        val found = composeRule.onAllNodesWithContentDescription(description, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        assertTrue("Missing control description: $description", found)
        println("CONTROL_ASSERT_PASS: $description")
    }

    private fun clickText(label: String, substring: Boolean = false) {
        composeRule.onAllNodesWithText(label, substring = substring)[0].performClick()
        composeRule.waitForIdle()
    }

    private fun clickClickableText(label: String, substring: Boolean = false) {
        composeRule.onAllNodes(hasText(label, substring = substring) and hasClickAction())[0].performClick()
        composeRule.waitForIdle()
    }

    private fun clickDesc(description: String) {
        composeRule.onAllNodesWithContentDescription(description, useUnmergedTree = true)[0].performClick()
        composeRule.waitForIdle()
    }

    private fun scrollToText(label: String, substring: Boolean = false) {
        val scrollableCount = composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size
        var lastFailure: Throwable? = null
        for (index in 0 until scrollableCount) {
            try {
                composeRule.onAllNodes(hasScrollAction())[index]
                    .performScrollToNode(hasText(label, substring = substring))
                composeRule.waitForIdle()
                return
            } catch (failure: Throwable) {
                lastFailure = failure
            }
        }
        throw AssertionError("Unable to scroll to control text: $label", lastFailure)
    }

    private fun clickTag(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun budgetLedgerCoversControls() {
        setContent { BudgetLedgerScreen() }
        waitForTag(TestTags.Screen.BUDGET_LEDGER)
        assertText("Budget Ceiling")
        assertText("Consumed")
        assertText("Reserved (Active)")
        assertText("Released")
        assertText("Budget Ceiling (Credits)")
        assertText("Save Budget Ceiling")
    }

    @Test
    fun canonCoversControls() {
        setContent { CanonViewerScreen() }
        waitForTag(TestTags.Screen.CANON)
        waitForText("The sky is red")
        assertText("Add Canon Fact")
        assertText("Commit Canon")
        assertText("Locked Facts")
        assertText("Pending Proposals")
        assertText("Search canon facts or proposals...", substring = true)

        clickDesc("Add Canon Fact")
        waitForText("➕ Add Canon Fact")
        assertText("Immutable Story / Character Fact")
        assertText("Commit Fact")
        assertText("Cancel")
        clickText("Cancel")

        clickText("Pending Proposals")
        waitForText("Canon Change Proposal")
        assertText("✅ Approve")
        assertText("❌ Reject")
    }

    @Test
    fun characterDeepDiveCoversControls() {
        setContent { CharacterDeepDiveScreen("CHAR_001") }
        waitForTag(TestTags.Screen.CHARACTER_DEEP_DIVE)
        waitForText("Code: CHAR_001")
        assertText("Aki")
        assertText("New Ref")
        assertText("Visual DNA (11)")
        assertText("Knowledge & Arc")
        assertText("Powers & Relations")
        assertText("Voice & Ref History")
        clickClickableText("Aki")
        waitForText("Aki")
    }

    @Test
    fun continuityCoversControls() {
        setContent { ContinuityCheckerScreen() }
        waitForTag(TestTags.Screen.CONTINUITY)
        waitForText("Dead character speaks")
        assertText("Entire Project")
        assertText("Run Continuity Scan (Entire Project)")
        assertText("Critical")
        assertText("Warnings")
        assertText("Passed")
        assertText("Fix with AI")

        clickClickableText("Entire Project")
        waitForText("Episode 1")
        assertText("Episode 2")
        assertText("Episode 3")
        clickText("Episode 1")
        assertText("Run Continuity Scan (Episode 1)")
    }

    @Test
    fun jobsMonitorCoversControls() {
        setContent { JobsMonitorScreen() }
        waitForTag(TestTags.Screen.JOBS_MONITOR)
        waitForText("SCENE RENDER")
        assertText("Active Jobs")
        assertText("Retries")
        assertText("1 Pending Actions")
        assertText("Force Retry")

        clickText("Retries")
        waitForText("Trigger Immediate Retry")
        assertText("Trigger Immediate Retry")
        clickText("Active Jobs")
        clickClickableText("SCENE RENDER")
        waitForText("Pipeline Job Inspector")
        assertText("Pipeline Job Inspector")
    }

    @Test
    fun loreCoversControls() {
        setContent { LoreExplorerScreen() }
        waitForTag(TestTags.Screen.LORE_EXPLORER)
        waitForText("World Wiki")
        waitForText("Generate Mysteries with AI")
        assertText("Add via AI")
        assertText("AI Edit")
        assertText("Filter Mysteries...", substring = true)
        assertText("Generate Mysteries with AI")

        clickDesc("Add Lore")
        waitForText("✨ Add / Edit World Wiki via AI")
        assertText("Describe new Mysteries entry", substring = true)
        assertText("Generate & Save")
        assertText("Cancel")
    }

    @Test
    fun visionCoversControls() {
        setContent { VisionAnalysisScreen() }
        waitForTag(TestTags.Screen.VISION_ANALYSIS)
        assertText("QUICK Preview (Zero DB Writes)")
        assertText("SAVE to Character & R4 History")
        assertText("Source Image / Reference URL (https://...)")
        assertText("Target Character Code (e.g. CHAR_001)")
        assertText("Start Vision DNA Analysis")
    }

    @Test
    fun authCoversControls() {
        setContent { AuthScreen(navController = rememberNavController()) }
        waitForTag(TestTags.Screen.AUTH)
        assertText("Studio Email")
        assertText("Password")
        assertText("Show")
        assertText("Sign In to Studio")
        assertText("Create an account")
        assertText("Forgot Password?")
        clickText("Show")
        assertText("Hide")
    }

    @Test
    fun aiBrainEmptyCoversControls() {
        setContent { AIBrainScreen() }
        waitForTag(TestTags.Screen.AI_BRAIN)
        assertText("Ask AI Director about this project...", substring = true)
        assertDesc("Send")
        assertText("Audit Story Bible")
        assertText("Check Continuity")
        assertText("Plan Next Episode")
    }

    @Test
    fun aiBrainConversationCoversControls() {
        val tokenManager = TokenManager(ApplicationProvider.getApplicationContext())
        tokenManager.appendProjectChatMessage(
            BATCH2_PROJECT_ID,
            PersistedChatMessage(
                role = "assistant",
                content = "Run episode",
                suggestedIntent = "GENERATE_EPISODE"
            )
        )
        setContent(fakeContainer(tokenManager = tokenManager)) { AIBrainScreen() }
        waitForTag(TestTags.Screen.AI_BRAIN)
        waitForText("Run episode")
        assertDesc("Clear project chat")
        assertText("Confirm & Execute")
    }

    @Test
    fun characterDetailCoversControls() {
        val character = Character(
            id = "CHAR_001",
            name = "Aki",
            role = "Protagonist",
            visualDna = "Silver hair",
            voiceId = "svara_hi_female_lead_01",
            status = "alive"
        )
        setContent { CharacterDetailScreen(characterId = "CHAR_001", initialCharacter = character) }
        waitForTag(TestTags.Screen.CHARACTER_DETAIL)
        assertText("Voice Engine: Svara TTS")
        assertText("Voice Model ID: svara_hi_female_lead_01")
        assertText("Preview Voice")
        assertText("Save Voice")
        assertText("Open Character Deep Dive (11-Field DNA, Arc, Powers & Relations)")

        clickClickableText("Voice Engine: Svara TTS")
        waitForText("Supertonic TTS")
        assertText("Edge TTS Fallback")
        clickText("Supertonic TTS")
        clickClickableText("Voice Model ID: svara_hi_female_lead_01")
        waitForText("supertonic_ja_villain_01")
        assertText("supertonic_en_narrator_01")
    }

    @Test
    fun characterListCoversControls() {
        setContent { CharacterListScreen() }
        waitForTag(TestTags.Screen.CHARACTERS)
        waitForText("Cast (1)")
        assertText("Search characters by name or role...", substring = true)
        assertText("Aki")
        assertText("ALIVE")
        assertDesc("Preview Voice")
        assertText("Deep Dive")
        assertText("Add Character")

        clickClickableText("Add Character")
        waitForText("➕ Add Character & Auto-Portrait")
        assertText("Character Name")
        assertText("Role (Protagonist / Antagonist / Support)")
        assertText("Create & Gen Image")
        assertText("Cancel")
    }

    @Test
    fun episodePlannerCoversControls() {
        setContent { EpisodePlannerScreen() }
        waitForTag(TestTags.Screen.EPISODES)
        waitForText("Episode 1: Pilot")
        assertText("Select Episode")
        assertText("Scenes")
        assertText("Director Prompt Override")
        assertText("Episode Title")
        assertText("Episode Synopsis")
        assertText("Episode Script / Narrative Outline")
        assertText("Save Script & Metadata (No Generation)")
        assertText("Generate Episode")
        assertText("Regenerate...")

        clickClickableText("Episode 1: Pilot")
        waitForText("Episode 2: New Episode")
        assertText("Episode 2: New Episode")
        clickText("Episode 2: New Episode")

        clickClickableText("Use Project Default Episode Prompt")
        waitForText("Batch Prompt [BIBLE]")
        assertText("Use Project Default Episode Prompt")
        clickText("Batch Prompt [BIBLE]")
        waitForText("Preview Prompt")
        assertText("Preview Prompt")

        scrollToText("Regenerate...")
        clickTag(TestTags.EpisodePlanner.REGENERATE)
        waitForText("Regenerate Episode 2?")
        assertText("Regenerate Episode")
        assertText("Cancel")
    }

    @Test
    fun mediaLibraryCoversControls() {
        setContent { MediaLibraryScreen() }
        waitForTag(TestTags.Screen.MEDIA_LIBRARY)
        waitForText("Scene #1 Video Clip")
        assertText("ALL")
        assertText("CLIPS")
        assertText("CHARACTERS")
        assertText("SCENES")
        assertText("Copy URL")
        assertText("Play")
        assertText("Open Stream")
    }

    @Test
    fun notificationsCoversControls() {
        setContent { NotificationScreen() }
        waitForTag(TestTags.Screen.NOTIFICATIONS)
        waitForText("Render complete")
        assertText("Mark All Read")
        assertText("Episode 1 rendered")
    }

    @Test
    fun adminQueuesCoversControls() {
        setContent { AdminQueuesScreen() }
        waitForTag(TestTags.Screen.ADMIN_QUEUES)
        waitForText("3 Queued Events")
        assertDesc("Info about Pipeline Queues")
        assertText("Off")
        assertText("5s")
        assertText("10s")
        assertText("30s")
        assertText("Transactional Outbox Queue")
        assertText("Active Pipeline Commands")
        assertText("Failed / Dead-Letter Queue")

        clickDesc("Info about Pipeline Queues")
        waitForText("Got it")
        assertText("Got it")
        clickText("Got it")
        clickClickableText("Transactional Outbox Queue")
        waitForText("Transactional Outbox Queue — Queue Inspector")
    }

    @Test
    fun auditLogCoversControls() {
        setContent { AuditLogScreen() }
        waitForTag(TestTags.Screen.AUDIT_LOG)
        assertText("Filter audit events by action or state...", substring = true)
    }

    @Test
    fun databaseCoversControls() {
        setContent { DatabaseScreen() }
        waitForTag(TestTags.Screen.DATABASE)
        waitForText("Test Connection Now")
        assertText("Test Connection Now")
    }

    @Test
    fun ownerOverviewCoversControls() {
        setContent { OwnerOverviewScreen() }
        waitForTag(TestTags.Screen.OWNER_OVERVIEW)
        assertText("🚨 Emergency Stop All GPU Workers")
        clickText("🚨 Emergency Stop All GPU Workers")
        waitForText("🚨 Emergency Stop All Workers?")
        assertText("Halt Fleet Now")
        assertText("Cancel")
    }

    @Test
    fun workerManagerCoversControls() {
        setContent { WorkerManagerScreen() }
        waitForTag(TestTags.Screen.WORKERS)
        assertText("Refresh")
        waitForText("KAGGLE (#worker-1)")
    }

    @Test
    fun workflowManagerCoversControls() {
        setContent { WorkflowManagerScreen() }
        waitForTag(TestTags.Screen.WORKFLOWS)
        assertText("Refresh")
        listOf(
            "WF-00 — Main Router",
            "WF-01 — Bible Architect",
            "WF-02 — Story Generator",
            "WF-03 — Production Pipeline",
            "WF-04 — Admin Panel",
            "WF-05 — Vision Pipeline",
            "WF-06 — Continuity Engine",
            "WF-07 — Publisher",
            "WF-08 — Analytics",
            "WF-09 — Canon Engine",
            "WF-10 — State Machine Manager",
            "WF-11 — QC Gate",
            "WF-12 — Quota Governor",
            "WF-13 — Retry Manager"
        ).forEach { workflow ->
            scrollToText(workflow)
            assertText(workflow)
        }
    }

    @Test
    fun productionCoversControls() {
        val state = Batch2FakeState().apply { jobsStatus = "SUCCEEDED" }
        setContent(fakeContainer(state)) { ProductionScreen() }
        waitForTag(TestTags.Screen.PRODUCTION)
        waitForText("Kaggle GPU Pool", substring = true)
        scrollToText("Kaggle GPU Pool", substring = true)
        assertText("Kaggle GPU Pool", substring = true)
        scrollToText("View Final Video & Quality Check", substring = true)
        assertText("View Final Video & Quality Check", substring = true)
        scrollToText("Pause")
        assertText("Pause")
        assertText("Cancel")
        assertText("Sync")
        scrollToText("Scene Inspector")
        assertText("Scene Inspector")
        assertText("Media Library")
    }

    @Test
    fun sceneDetailCoversControls() {
        setContent { SceneDetailScreen(1) }
        waitForTag(TestTags.Screen.SCENES)
        waitForText("Scene #1")
        assertText("Episode 1")
        assertText("Edit (PATCH)")
        assertText("Regenerate")
        assertText("Open Rendered Clip Stream")

        clickText("Edit (PATCH)")
        waitForText("Edit Scene #1")
        assertText("Scene Description / Action")
        assertText("Visual Prompt")
        assertText("Save Scene (PATCH)")
        assertText("Cancel")
        clickText("Cancel")
    }

    @Test
    fun bibleCoversControls() {
        setContent { BibleScreen() }
        waitForTag(TestTags.Screen.BIBLE)
        waitForText("Master Premise / Logline")
        assertText("Overview & Tone")
        assertText("World & Rules")
        assertText("Power System")
        assertText("Characters & Timeline")
        assertText("Master Premise / Logline")
        assertText("Tone & Genre")
        assertText("Save Bible Draft")
        assertText("AI Expand Bible")

        clickText("World & Rules")
        assertText("World Setting & Geography")
        assertText("Immutable World Rules (Enforced by Canon)")
        clickText("Power System")
        assertText("Power System Rules, Costs & Ceilings")
        clickText("Characters & Timeline")
        assertText("Core Cast Foundations & Factions")
        assertText("Timeline & Season Arc Structure")
    }

    @Test
    fun projectDetailCoversControls() {
        setContent { ProjectDetailScreen() }
        waitForTag(TestTags.Screen.PROJECT_DETAIL)
        waitForText("Canon Status")
        assertDesc("Edit Project Settings")
        assertText("Canon Status")
        assertText("Continuity Check")
        assertText("Episodes")
        assertText("Scenes")
        assertText("Characters")
        assertText("Active Jobs")
        assertText("Pending Approvals")
        assertText("Queued Actions")
        assertText("Generate Next Episode")
        assertText("Story Bible")
        assertText("Episode Planner")
        assertText("Scene Inspector")
        assertText("Character Deep Dive")
        assertText("World Wiki")
        assertText("Vision DNA")
        assertText("Quick Generate")
        assertText("Media Library")
        assertText("Quality Control (QC)")
        assertText("Review Inbox")
        assertText("Publish Studio")
        assertText("Analytics")
        assertText("Quota & Budget")

        clickDesc("Edit Project Settings")
        waitForText("Project Settings")
        assertText("Project Title")
        assertText("Genre")
        assertText("Visual Medium (ANIME / MANHWA / 3D)")
        assertText("Cover Image URL (https://...)")
        assertText("Save Settings")
        assertText("Cancel")
    }

    @Test
    fun projectDetailActiveJobCoversConditionalControl() {
        val state = Batch2FakeState().apply {
            overviewActiveCommandId = "cmd-batch2"
            commandStatus = "RUNNING"
        }
        setContent(fakeContainer(state)) { ProjectDetailScreen() }
        waitForTag(TestTags.Screen.PROJECT_DETAIL)
        waitForText("Open Active Job in Pipeline Monitor")
        assertText("Open Active Job in Pipeline Monitor")
    }

    @Test
    fun publishCoversControls() {
        setContent { PublishScreen() }
        waitForTag(TestTags.Screen.PUBLISH)
        waitForText("Ep 1: Pilot")
        assertText("Viral Auto-Fill")
        assertText("YouTube / Shorts")
        assertText("Connected")
        assertText("Instagram Reels")
        assertText("Connect Insta")
        assertText("Release Title")
        assertText("Viral Description")
        assertText("Viral Tags")
        assertText("Visibility:")
        assertText("Public")
        assertText("Publish to Selected Platforms")

        clickClickableText("Public")
        waitForText("Unlisted")
        assertText("Unlisted")
        assertText("Private")
    }

    @Test
    fun qcCoversControls() {
        setContent { QCScreen() }
        waitForTag(TestTags.Screen.QC)
        waitForText("Episode 1: Pilot")
        assertText("Select Episode for QC")
        assertText("Re-Run Automated QC")
        assertText("Approve QC Gate")
        assertText("Proceed to Publish Studio")

        clickClickableText("Episode 1: Pilot")
        waitForText("Episode 2")
        assertText("Episode 2")
        assertText("Episode 3")
    }

    @Test
    fun reviewInboxCoversControls() {
        setContent { ReviewInboxScreen() }
        waitForTag(TestTags.Screen.REVIEW_INBOX)
        waitForText("✅ Approve")
        assertText("✅ Approve")
        assertText("❌ Reject")

        clickText("❌ Reject")
        waitForText("Reject Approval", substring = true)
        assertText("Reason for rejection")
        assertText("Confirm Reject")
        assertText("Cancel")
    }

    @Test
    fun settingsCoversControls() {
        setContent { SettingsScreen() }
        waitForTag(TestTags.Screen.SETTINGS)
        assertText("AI & Vision")
        assertText("Compute Workers")
        assertText("Storage & Policy")
        assertText("Session")
        assertText("Primary LLM Provider (groq / gemini / openrouter)")
        assertText("Fallback LLM Provider")
        assertText("Vision DNA Provider (gemini / groq / openrouter)")
        assertText("Groq API Key", substring = true)
        assertText("Gemini API Key", substring = true)
        assertText("Save & Verify Configuration")

        clickText("Compute Workers")
        assertText("Preferred Compute Mode")
        assertText("Kaggle Username")
        assertText("Kaggle API Key", substring = true)
        assertText("GitHub CPU Worker Repository")
        assertText("GitHub Actions Token (Masked)")

        clickText("Storage & Policy")
        assertText("Telegram Channel / Chat ID")
        assertText("Telegram Bot Token (Masked)")

        clickText("Session")
        assertText("Test Backend Connection")
    }

    @Test
    fun promptStudioCoversControls() {
        setContent { PromptStudioScreen() }
        waitForTag(TestTags.Screen.PROMPT_STUDIO)
        waitForText("Batch Prompt")
        assertText("BIBLE")
        assertText("EPISODE")
        assertText("IMAGE")
        assertText("AI_DIRECTOR")
        assertText("Enter Edit Mode")
        assertText("Prompt Title (BIBLE)")
        assertText("System Prompt Instructions (BIBLE)")
        assertText("Reset BIBLE to Application Default")
        assertDesc("Delete prompt")

        scrollToText("Reset BIBLE to Application Default")
        clickText("Reset BIBLE to Application Default")
        waitForText("Reset BIBLE Prompt to Default?")
        assertText("Reset to Default")
        assertText("Cancel")
        clickText("Cancel")
        scrollToText("Enter Edit Mode")
        clickTag(TestTags.PromptStudio.EDIT)
        waitForText("Save BIBLE")
        assertText("Save BIBLE")
        assertText("Cancel")
    }

    @Test
    fun studioDashboardCoversControls() {
        setContent { StudioDashboardScreen() }
        waitForTag(TestTags.Screen.STUDIO)
        waitForText("Projects (1)")
        assertText("Create Project")
        assertDesc("Refresh Projects")
        assertText("Production State")
        assertText("Pending Approvals")

        clickClickableText("Production State")
        waitForText("Pipeline Status Details")
        assertText("Pipeline Status Details")
        assertText("Close")
        clickText("Close")

        clickTag(TestTags.Studio.CREATE_PROJECT)
        waitForText("Project Title")
        assertText("Project Title")
        assertText("Create")
        assertText("Cancel")
    }

    @Test
    fun quickToolsSaveCoversControls() {
        val state = Batch2FakeState().apply { commandStatus = "SUCCEEDED" }
        setContent(fakeContainer(state)) { QuickToolsScreen() }
        waitForTag(TestTags.Screen.QUICK_TOOLS)
        assertText("Image Generation")
        assertText("Video Clip Generation")
        assertText("Visual Prompt")
        assertText("Style Preset")
        assertText("Aspect Ratio")
        assertText("Generate Ad-Hoc Image")

        composeRule.onNodeWithTag(TestTags.QuickTools.PROMPT).performTextInput("A city at dusk")
        scrollToText("Generate Ad-Hoc Image")
        clickTag(TestTags.QuickTools.GENERATE)
        waitForText("Save Asset to Project")
        assertText("Save Asset to Project")
    }

    @Test
    fun quickToolsCancelCoversConditionalControl() {
        val state = Batch2FakeState().apply { commandStatus = "QUEUED" }
        setContent(fakeContainer(state)) { QuickToolsScreen() }
        waitForTag(TestTags.Screen.QUICK_TOOLS)
        composeRule.onNodeWithTag(TestTags.QuickTools.PROMPT).performTextInput("A city at dusk")
        scrollToText("Generate Ad-Hoc Image")
        clickTag(TestTags.QuickTools.GENERATE)
        waitForText("Cancel Remote Job")
        assertText("Cancel Remote Job")
    }

    @Test
    fun existingSaveButtonIsEnabledInPromptStudio() {
        setContent { PromptStudioScreen() }
        waitForTag(TestTags.Screen.PROMPT_STUDIO)
        waitForTag(TestTags.PromptStudio.EDIT)
        clickTag(TestTags.PromptStudio.EDIT)
        waitForTag(TestTags.PromptStudio.SAVE)
        composeRule.onNodeWithTag(TestTags.PromptStudio.SAVE).assertIsEnabled()
    }
}
