package org.dpdns.bnai.anime.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.ui.character.CharacterDetailScreen
import org.dpdns.bnai.anime.ui.character.CharacterListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String = "proj_001",
    onOpenEpisodes: () -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {}
) {
    val tabNavController = rememberNavController()
    val tabs = listOf("Overview", "Bible", "Characters")
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Project Workspace: $projectId",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            val route = when (index) {
                                0 -> "overview"
                                1 -> "bible"
                                else -> "characters"
                            }
                            tabNavController.navigate(route) {
                                popUpTo(tabNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    )
                }
            }

            NavHost(
                navController = tabNavController,
                startDestination = "overview",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("overview") {
                    ProjectOverviewTab(
                        projectId = projectId,
                        onOpenEpisodes = onOpenEpisodes,
                        onOpenReviews = onOpenReviews,
                        onOpenAnalytics = onOpenAnalytics
                    )
                }
                composable("bible") {
                    BibleScreen(projectId = projectId)
                }
                composable("characters") {
                    CharacterListScreen(
                        onCharacterClick = { character ->
                            selectedCharacter = character
                            tabNavController.navigate("character_detail")
                        }
                    )
                }
                composable("character_detail") {
                    selectedCharacter?.let { character ->
                        CharacterDetailScreen(character = character)
                    } ?: CharacterDetailScreen()
                }
            }
        }
    }
}

@Composable
private fun ProjectOverviewTab(
    projectId: String,
    onOpenEpisodes: () -> Unit,
    onOpenReviews: () -> Unit,
    onOpenAnalytics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Universe Snapshot ($projectId)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Canon Status: COMMITTED (Snapshot Ep 4)\nContinuity Gate (WF-06): VERIFIED\nActive Target: Season 1, Episode 5",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Button(
            onClick = onOpenEpisodes,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Open S06 Episode Planner", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = onOpenReviews,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Open S11 Review & Approval Inbox", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = onOpenAnalytics,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Open S15 Analytics & Insights", style = MaterialTheme.typography.labelLarge)
        }
    }
}
