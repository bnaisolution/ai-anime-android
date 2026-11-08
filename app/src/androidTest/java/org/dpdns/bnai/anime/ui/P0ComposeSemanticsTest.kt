package org.dpdns.bnai.anime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.lang.reflect.Proxy
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.di.AppContainer
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeApiService
import org.dpdns.bnai.anime.core.network.AnimeRepositoryApi
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.designsystem.ConfirmationDialog
import org.dpdns.bnai.anime.ui.testing.TestTags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P0ComposeSemanticsTest {
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
    fun authScreenExposesP0Tags() {
        val container = FakeContainer()
        composeRule.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                MaterialTheme {
                    val navController = rememberNavController()
                    AuthScreen(navController = navController)
                }
            }
        }

        composeRule.onNodeWithTag(TestTags.Auth.EMAIL).assertExists()
        composeRule.onNodeWithTag(TestTags.Auth.PASSWORD).assertExists()
        composeRule.onNodeWithTag(TestTags.Auth.SUBMIT).assertExists()
    }

    @Test
    fun resourceStateBoxExposesStateTags() {
        composeRule.setContent {
            MaterialTheme {
                ResourceStateBox(
                    resource = Resource.Empty,
                    onRetry = {}
                ) {}
            }
        }

        composeRule.onNodeWithTag(TestTags.State.EMPTY).assertExists()
        composeRule.onNodeWithTag(TestTags.State.RETRY).assertExists()
    }

    @Test
    fun confirmationDialogExposesDecisionTags() {
        composeRule.setContent {
            MaterialTheme {
                ConfirmationDialog(
                    title = "Confirm",
                    message = "Proceed?",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.Dialog.CONFIRM).assertExists()
        composeRule.onNodeWithTag(TestTags.Dialog.CANCEL).assertExists()
    }

    @Test
    fun projectSelectorExposesNavigationTags() {
        val container = FakeContainer()
        composeRule.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                MaterialTheme {
                    ProjectSelectorBar()
                }
            }
        }

        composeRule.onNodeWithTag(TestTags.Navigation.PROJECT_SELECTOR).assertExists()
    }

    private class FakeContainer : AppContainer {
        override val tokenManager: TokenManager = TokenManager(null)
        override val apiService: AnimeApiService = fakeApiService()
        override val repository: AnimeRepositoryApi = object : AnimeRepositoryApi {
            override fun currentProjectId(): String = ""
            override suspend fun fetchProjects(): Resource<List<Project>> =
                Resource.Success(emptyList())
        }
        override val viewModelFactory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    error("FakeContainer does not create ViewModels")
            }
    }

}

private fun fakeApiService(): AnimeApiService =
    Proxy.newProxyInstance(
        AnimeApiService::class.java.classLoader,
        arrayOf(AnimeApiService::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "toString" -> "FakeAnimeApiService"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            else -> null
        }
    } as AnimeApiService

