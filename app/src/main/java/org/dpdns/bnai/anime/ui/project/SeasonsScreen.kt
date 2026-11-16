package org.dpdns.bnai.anime.ui.project

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.SeasonPlan
import org.dpdns.bnai.anime.core.model.intValue
import org.dpdns.bnai.anime.core.model.seasonPlansFromBible
import org.dpdns.bnai.anime.core.model.storyBibleMap
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.PrimaryButton
import org.dpdns.bnai.anime.ui.designsystem.ScreenHeader
import org.dpdns.bnai.anime.ui.designsystem.SecondaryButton
import org.dpdns.bnai.anime.ui.designsystem.SectionHeader
import org.dpdns.bnai.anime.ui.designsystem.StandardCard
import org.dpdns.bnai.anime.ui.designsystem.StatusChip
import org.dpdns.bnai.anime.ui.designsystem.StudioTokens

@Composable
fun SeasonsScreen(
    onOpenSeason: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var activePid by remember { mutableStateOf(repository.currentProjectId()) }
    var bible by remember { mutableStateOf<JsonMap>(emptyMap()) }
    var seasons by remember { mutableStateOf<List<SeasonPlan>>(emptyList()) }
    var totalSeasons by remember { mutableStateOf(1) }
    var episodesPerSeason by remember { mutableStateOf(12) }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun applyBible(nextBible: JsonMap): List<SeasonPlan> {
        val total = nextBible.intValue("total_seasons", 1).coerceIn(1, 100)
        val perSeason = nextBible.intValue("episodes_per_season", 12).coerceIn(1, 500)
        totalSeasons = total
        episodesPerSeason = perSeason
        bible = nextBible
        return seasonPlansFromBible(nextBible, total, perSeason)
    }

    suspend fun syncIfNeeded(plans: List<SeasonPlan>, sourceBible: JsonMap): List<SeasonPlan> {
        val rawCount = (sourceBible["seasons"] as? List<*>)?.size ?: 0
        if (rawCount >= plans.size) return plans
        val response = repository.updateProjectBible(
            activePid,
            mapOf("seasons" to plans.map { it.toJson() })
        )
        return when (response) {
            is Resource.Success -> applyBible(response.data.storyBibleMap().ifEmpty { sourceBible })
            is Resource.Error -> {
                errorMessage = response.message
                plans
            }
            else -> plans
        }
    }

    fun load() {
        val pid = activePid.ifBlank { repository.currentProjectId() }
        if (pid.isBlank()) {
            isLoading = false
            errorMessage = "No active project selected."
            return
        }
        activePid = pid
        scope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.fetchBible(pid)) {
                is Resource.Success -> {
                    val parsed = applyBible(result.data.storyBibleMap())
                    seasons = syncIfNeeded(parsed, bible)
                }
                is Resource.Error -> errorMessage = result.message
                is Resource.Empty -> errorMessage = "Story Bible is empty."
                else -> Unit
            }
            isLoading = false
        }
    }

    fun syncNow() {
        if (isSyncing) return
        isSyncing = true
        scope.launch {
            val current = seasonPlansFromBible(bible, totalSeasons, episodesPerSeason)
            when (val result = repository.updateProjectBible(
                activePid,
                mapOf("seasons" to current.map { it.toJson() })
            )) {
                is Resource.Success -> {
                    seasons = applyBible(result.data.storyBibleMap().ifEmpty { bible })
                    Toast.makeText(context, "Season placeholders synced", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                else -> Unit
            }
            isSyncing = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            selectedProjectIdOverride = activePid,
            onProjectChanged = {
                activePid = it
                load()
            },
            onRefreshClick = { load() }
        )

        ScreenHeader(
            title = "Seasons",
            subtitle = "Season plans live inside the single project Story Bible. Each season keeps its own arc, episode range, genres, and prompt overrides.",
            trailingContent = { StatusChip(label = "$totalSeasons PLANNED") }
        )

        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        } else if (errorMessage != null) {
            StandardCard {
                Text("Could not load seasons", color = StudioTokens.Error, fontWeight = FontWeight.Bold)
                Text(errorMessage.orEmpty(), fontSize = 13.sp)
                SecondaryButton(text = "Retry", onClick = { load() })
            }
        } else {
            StandardCard {
                SectionHeader(
                    title = "Season blueprint",
                    badgeText = "$episodesPerSeason episodes / season"
                )
                Text(
                    text = "One Story Bible for the whole project. Seasons inherit project canon and may add genres or prompt overrides. Full scripts remain inside each episode.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = if (isSyncing) "Syncing..." else "Sync placeholders from series plan",
                    onClick = { syncNow() },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            seasons.sortedBy { it.seasonNo }.forEach { season ->
                StandardCard(onClick = { onOpenSeason(season.seasonNo) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(season.displayTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Episodes ${season.episodeStart}–${season.episodeEnd}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (season.arc.isNotBlank()) {
                                Text(
                                    season.arc,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusChip(label = season.status)
                            if (season.locked) StatusChip(label = "LOCKED")
                        }
                    }
                    if (season.genres.isNotEmpty()) {
                        Text(
                            text = "Season genres: ${season.genres.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = StudioTokens.Primary
                        )
                    }
                    Text(
                        text = "Open season →",
                        fontSize = 12.sp,
                        color = StudioTokens.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            SecondaryButton(
                text = "Back",
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
