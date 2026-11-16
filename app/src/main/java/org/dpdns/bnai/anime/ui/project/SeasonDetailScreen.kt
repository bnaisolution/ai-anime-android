package org.dpdns.bnai.anime.ui.project

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.SeasonPlan
import org.dpdns.bnai.anime.core.model.intValue
import org.dpdns.bnai.anime.core.model.seasonPlansFromBible
import org.dpdns.bnai.anime.core.model.storyBibleMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.designsystem.PrimaryButton
import org.dpdns.bnai.anime.ui.designsystem.ScreenHeader
import org.dpdns.bnai.anime.ui.designsystem.SecondaryButton
import org.dpdns.bnai.anime.ui.designsystem.SectionHeader
import org.dpdns.bnai.anime.ui.designsystem.StandardCard
import org.dpdns.bnai.anime.ui.designsystem.StatusChip
import org.dpdns.bnai.anime.ui.designsystem.StudioTokens

private val SEASON_STATUSES = listOf("PLANNED", "WRITING", "REVIEW", "LOCKED", "COMPLETE")
private val PROMPT_OVERRIDE_CATEGORIES = listOf("SEASON", "EPISODE", "IMAGE", "AI_DIRECTOR")
private val FALLBACK_GENRES = listOf("Action", "Fantasy", "Dark Fantasy", "Action Fantasy", "Thriller", "Horror", "Romance", "Comedy", "Sci-Fi", "Mystery", "Drama", "Slice of Life", "Mythology", "Music")

@Composable
fun SeasonDetailScreen(
    seasonNo: Int,
    onNavigateBack: () -> Unit = {},
    onOpenPromptStudio: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var activePid by remember { mutableStateOf(repository.currentProjectId()) }
    var seasons by remember { mutableStateOf<List<SeasonPlan>>(emptyList()) }
    var season by remember { mutableStateOf<SeasonPlan?>(null) }
    var projectGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableGenres by remember { mutableStateOf(FALLBACK_GENRES) }
    var prompts by remember { mutableStateOf<List<SystemPromptDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var dirty by remember { mutableStateOf(false) }

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
                    val bible = result.data.storyBibleMap()
                    val total = bible.intValue("total_seasons", 1).coerceIn(1, 100)
                    val perSeason = bible.intValue("episodes_per_season", 12).coerceIn(1, 500)
                    val parsed = seasonPlansFromBible(bible, total, perSeason)
                    seasons = parsed
                    season = parsed.firstOrNull { it.seasonNo == seasonNo } ?: parsed.firstOrNull()
                    if (season == null) errorMessage = "Season $seasonNo could not be prepared."
                }
                is Resource.Error -> errorMessage = result.message
                is Resource.Empty -> errorMessage = "Story Bible is empty."
                else -> Unit
            }
            when (val meta = repository.fetchPromptLibraryMeta(pid)) {
                is Resource.Success -> {
                    projectGenres = meta.data.project_selected_genres
                    if (meta.data.genres.isNotEmpty()) availableGenres = meta.data.genres
                }
                else -> Unit
            }
            when (val promptResult = repository.fetchSystemPrompts(pid)) {
                is Resource.Success -> prompts = promptResult.data
                else -> Unit
            }
            isLoading = false
        }
    }

    LaunchedEffect(seasonNo) { load() }

    fun updateSeason(transform: (SeasonPlan) -> SeasonPlan) {
        val current = season ?: return
        season = transform(current)
        dirty = true
    }

    fun save() {
        val current = season ?: return
        if (isSaving) return
        if (current.episodeStart < 1 || current.episodeEnd < current.episodeStart) {
            Toast.makeText(context, "Episode range must be valid.", Toast.LENGTH_LONG).show()
            return
        }
        val updated = seasons.map { if (it.seasonNo == current.seasonNo) current else it }
        isSaving = true
        scope.launch {
            when (val result = repository.updateProjectBible(
                activePid,
                mapOf("seasons" to updated.map { it.toJson() })
            )) {
                is Resource.Success -> {
                    seasons = updated
                    dirty = false
                    Toast.makeText(context, "Season saved", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                is Resource.Empty -> Toast.makeText(context, "Season was not saved", Toast.LENGTH_LONG).show()
                else -> Unit
            }
            isSaving = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val current = season
        ScreenHeader(
            title = current?.displayTitle ?: "Season $seasonNo",
            subtitle = "Season plan inside the one project Story Bible",
            trailingContent = { StatusChip(label = current?.status ?: "PLANNED") }
        )

        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
            return@Column
        }

        if (errorMessage != null || current == null) {
            StandardCard {
                Text("Could not open season", color = StudioTokens.Error, fontWeight = FontWeight.Bold)
                Text(errorMessage.orEmpty(), fontSize = 13.sp)
                SecondaryButton(text = "Retry", onClick = { load() })
                SecondaryButton(text = "Back", onClick = onNavigateBack)
            }
            return@Column
        }

        StandardCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Season lock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        if (current.locked) "Locked seasons are read-only until unlocked." else "Unlocked seasons can be edited.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = current.locked,
                    onCheckedChange = { checked ->
                        updateSeason { it.copy(locked = checked, status = if (checked) "LOCKED" else if (it.status == "LOCKED") "PLANNED" else it.status) }
                    }
                )
            }
        }

        StandardCard {
            SectionHeader(title = "Season identity", badgeText = "S${current.seasonNo}")
            OutlinedTextField(
                value = current.title,
                onValueChange = { value -> updateSeason { it.copy(title = value) } },
                label = { Text("Season title") },
                singleLine = true,
                readOnly = current.locked,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            SelectionField(
                label = "Status",
                value = current.status,
                options = SEASON_STATUSES,
                enabled = !current.locked,
                onSelect = { value -> updateSeason { it.copy(status = value) } }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = current.episodeStart.toString(),
                    onValueChange = { value ->
                        value.filter(Char::isDigit).toIntOrNull()?.let { start ->
                            updateSeason { it.copy(episodeStart = start) }
                        }
                    },
                    label = { Text("First episode") },
                    singleLine = true,
                    readOnly = current.locked,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = current.episodeEnd.toString(),
                    onValueChange = { value ->
                        value.filter(Char::isDigit).toIntOrNull()?.let { end ->
                            updateSeason { it.copy(episodeEnd = end) }
                        }
                    },
                    label = { Text("Last episode") },
                    singleLine = true,
                    readOnly = current.locked,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        StandardCard {
            SectionHeader(title = "Season arc", badgeText = "ONE BIBLE")
            OutlinedTextField(
                value = current.arc,
                onValueChange = { value -> updateSeason { it.copy(arc = value) } },
                label = { Text("Season arc") },
                minLines = 3,
                readOnly = current.locked,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = current.goals,
                onValueChange = { value -> updateSeason { it.copy(goals = value) } },
                label = { Text("Story goals") },
                minLines = 3,
                readOnly = current.locked,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = current.blueprint,
                onValueChange = { value -> updateSeason { it.copy(blueprint = value) } },
                label = { Text("Season blueprint / beat outline") },
                minLines = 6,
                readOnly = current.locked,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "This is the season blueprint. Full episode scripts stay inside each episode.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StandardCard {
            SectionHeader(title = "Genres", badgeText = "SEASON")
            Text("Project genres inherited", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            if (projectGenres.isEmpty()) {
                Text("No project genres configured.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    projectGenres.forEach { genre ->
                        FilterChip(selected = true, onClick = {}, enabled = false, label = { Text(genre) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Season extra genres", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                availableGenres
                    .filter { genre -> projectGenres.none { it.equals(genre, ignoreCase = true) } }
                    .distinct()
                    .forEach { genre ->
                        val selected = current.genres.any { it.equals(genre, ignoreCase = true) }
                        FilterChip(
                            selected = selected,
                            enabled = !current.locked,
                            onClick = {
                                updateSeason {
                                    if (selected) it.copy(genres = it.genres.filterNot { value -> value.equals(genre, ignoreCase = true) })
                                    else it.copy(genres = it.genres + genre)
                                }
                            },
                            label = { Text(genre) }
                        )
                    }
            }
            Text(
                text = "Effective genres = project genres + season extra genres. Season genres add focus; they do not remove project canon.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StandardCard {
            SectionHeader(title = "Season prompt overrides", badgeText = "OPTIONAL")
            Text(
                text = "Override only when this season needs different instructions. Otherwise the project prompt is inherited.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PROMPT_OVERRIDE_CATEGORIES.forEach { category ->
                val options = prompts.filter { (it.prompt_category ?: "").uppercase() == category }
                PromptOverridePicker(
                    label = category,
                    selectedId = current.promptOverrides.valueFor(category),
                    options = options,
                    enabled = !current.locked,
                    onSelect = { promptId ->
                        updateSeason {
                            it.copy(promptOverrides = it.promptOverrides.withValue(category, promptId))
                        }
                    }
                )
            }
            SecondaryButton(
                text = "Manage prompts in Prompt Studio",
                onClick = { onOpenPromptStudio(current.seasonNo) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()
        PrimaryButton(
            text = if (isSaving) "Saving..." else "Save Season",
            onClick = { save() },
            enabled = !isSaving && dirty,
            isLoading = isSaving,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryButton(
            text = "Back",
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $value", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun PromptOverridePicker(
    label: String,
    selectedId: String?,
    options: List<SystemPromptDto>,
    enabled: Boolean,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = options.firstOrNull { it.id == selectedId }?.title
        ?: if (selectedId.isNullOrBlank()) "Inherit project prompt" else "Missing prompt"
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $selectedTitle", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Inherit project prompt") },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            options.forEach { prompt ->
                DropdownMenuItem(
                    text = { Text("${prompt.title} (v${prompt.version})") },
                    onClick = {
                        expanded = false
                        onSelect(prompt.id)
                    }
                )
            }
        }
    }
}
