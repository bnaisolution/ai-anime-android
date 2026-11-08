package org.dpdns.bnai.anime.ui.batch2

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.designsystem.ConfirmationDialog
import org.dpdns.bnai.anime.ui.designsystem.UnsavedChangesDialog
import org.dpdns.bnai.anime.ui.testing.TestTags
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Batch2SharedComponentTest {
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
    fun loadingStateExposesLoadingTag() {
        composeRule.setBatch2Content {
            ResourceStateBox(resource = Resource.Loading) {}
        }
        composeRule.onNodeWithTag(TestTags.State.LOADING).assertExists()
    }

    @Test
    fun emptyStateExposesEmptyAndRetry() {
        composeRule.setBatch2Content {
            ResourceStateBox(resource = Resource.Empty, onRetry = {}) {}
        }
        composeRule.onNodeWithTag(TestTags.State.EMPTY).assertExists()
        composeRule.onNodeWithTag(TestTags.State.RETRY).assertExists()
    }

    @Test
    fun errorStateExposesErrorAndRetry() {
        composeRule.setBatch2Content {
            ResourceStateBox(resource = Resource.Error("network down"), onRetry = {}) {}
        }
        composeRule.onNodeWithTag(TestTags.State.ERROR).assertExists()
        composeRule.onNodeWithTag(TestTags.State.RETRY).assertExists()
        composeRule.onNodeWithText("Failed to Load Data").assertExists()
    }

    @Test
    fun successStateRendersContent() {
        composeRule.setBatch2Content {
            ResourceStateBox(resource = Resource.Success("ready")) { value ->
                androidx.compose.material3.Text("Payload: $value")
            }
        }
        composeRule.onNodeWithText("Payload: ready").assertExists()
    }

    @Test
    fun retryActionInvokesCallback() {
        var retried = false
        composeRule.setBatch2Content {
            ResourceStateBox(resource = Resource.Empty, onRetry = { retried = true }) {}
        }
        composeRule.onNodeWithTag(TestTags.State.RETRY).performClick()
        assertTrue(retried)
    }

    @Test
    fun retryControlHasAccessibleDescription() {
        composeRule.setBatch2Content {
            ResourceStateBox(resource = Resource.Error("offline"), onRetry = {}) {}
        }
        composeRule.onNodeWithContentDescription("Retry").assertExists()
    }

    @Test
    fun confirmationDialogConfirmInvokesCallback() {
        var confirmed = false
        composeRule.setBatch2Content {
            ConfirmationDialog(
                title = "Confirm",
                message = "Proceed?",
                onConfirm = { confirmed = true },
                onDismiss = {}
            )
        }
        composeRule.onNodeWithTag(TestTags.Dialog.CONFIRM).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun confirmationDialogCancelInvokesCallback() {
        var dismissed = false
        composeRule.setBatch2Content {
            ConfirmationDialog(
                title = "Confirm",
                message = "Proceed?",
                onConfirm = {},
                onDismiss = { dismissed = true }
            )
        }
        composeRule.onNodeWithTag(TestTags.Dialog.CANCEL).performClick()
        assertTrue(dismissed)
    }

    @Test
    fun unsavedDialogExposesBothDecisions() {
        composeRule.setBatch2Content {
            UnsavedChangesDialog(onDiscard = {}, onKeepEditing = {})
        }
        composeRule.onNodeWithTag(TestTags.Dialog.UNSAVED_DISCARD).assertExists()
        composeRule.onNodeWithTag(TestTags.Dialog.UNSAVED_KEEP_EDITING).assertExists()
    }

    @Test
    fun projectSelectorExposesNavigationTags() {
        composeRule.setBatch2Content {
            ProjectSelectorBar(onRefreshClick = {})
        }
        composeRule.onNodeWithTag(TestTags.Navigation.PROJECT_SELECTOR).assertExists()
        composeRule.onNodeWithTag(TestTags.Navigation.PROJECT_REFRESH).assertExists()
    }
}
