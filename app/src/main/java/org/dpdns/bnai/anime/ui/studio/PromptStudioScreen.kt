package org.dpdns.bnai.anime.ui.studio

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.GenreTemplateDto
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*
import org.dpdns.bnai.anime.ui.testing.TestTags

private val CATEGORIES = listOf("BIBLE", "EPISODE", "IMAGE", "AI_DIRECTOR", "GENRE", "VISUAL_MEDIUM")
private val MEDIA_OPTIONS = listOf("ANIME", "MANHWA", "3D", "2D_ANIMATION", "REAL_HUMAN", "LIVE_ACTION", "CINEMATIC", "HYBRID")
private val FALLBACK_GENRES = listOf("Action", "Fantasy", "Dark Fantasy", "Action Fantasy", "Thriller", "Horror", "Romance", "Comedy", "Sci-Fi", "Mystery", "Drama", "Slice of Life", "Mythology", "Music")
private val FALLBACK_FORMATS = listOf("SERIES", "MOVIE", "SHORT", "MUSIC_VIDEO", "TRAILER")
private val SYSTEM_DEFAULTS = mapOf(
    "BIBLE" to "You are the WF-01 Master Story Bible Architect. Build cohesive anime world rules, power scaling ceilings, and immutable character anchors.",
    "EPISODE" to "You are the WF-02 Episode Screenwriter. Write tight, high-retention multi-scene anime scripts strictly obeying locked Canon and character Visual DNA.",
    "IMAGE" to "Masterpiece cinematic anime keyframe, ultra-detailed cel shading, dynamic volumetric lighting, strict adherence to character 11-field Visual DNA.",
    "AI_DIRECTOR" to "You are the AI Anime Studio Director overseeing StoryWF-00..13. Provide actionable production guidance and never mutate state without user confirmation.",
    "GENRE" to "Define the selected genre's mandatory story promises, conflicts, pacing, character archetypes, and forbidden clichés.",
    "VISUAL_MEDIUM" to "Define the authoritative visual language for the selected medium, including rendering, linework, lighting, texture, framing, and motion rules."
)

@Composable
fun PromptStudioScreen(
    initialTab: String = "BIBLE",
    initialGenre: String? = null,
    initialMedium: String? = null
) {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var categories by remember { mutableStateOf(CATEGORIES) }
    var selectedCatIndex by remember {
        mutableStateOf(CATEGORIES.indexOf(initialTab.uppercase()).coerceAtLeast(0))
    }
    val currentCategory = categories.getOrElse(selectedCatIndex) { CATEGORIES.first() }
    var promptScopeBy by remember { mutableStateOf("this_project") }

    val titlesByCat = remember {
        mutableStateMapOf(
            "BIBLE" to "Default Bible Architect Prompt",
            "EPISODE" to "Default Episode Screenplay Prompt",
            "IMAGE" to "Default Visual Keyframe Prompt",
            "AI_DIRECTOR" to "Default AI Director System Prompt",
            "GENRE" to "Default Genre Story Prompt",
            "VISUAL_MEDIUM" to "Default Visual Medium Prompt"
        )
    }
    val textByCat = remember {
        mutableStateMapOf(
            "BIBLE" to (SYSTEM_DEFAULTS["BIBLE"] ?: ""),
            "EPISODE" to (SYSTEM_DEFAULTS["EPISODE"] ?: ""),
            "IMAGE" to (SYSTEM_DEFAULTS["IMAGE"] ?: ""),
            "AI_DIRECTOR" to (SYSTEM_DEFAULTS["AI_DIRECTOR"] ?: ""),
            "GENRE" to (SYSTEM_DEFAULTS["GENRE"] ?: ""),
            "VISUAL_MEDIUM" to (SYSTEM_DEFAULTS["VISUAL_MEDIUM"] ?: "")
        )
    }
    val baselineByCat = remember {
        mutableStateMapOf(
            "BIBLE" to (SYSTEM_DEFAULTS["BIBLE"] ?: ""),
            "EPISODE" to (SYSTEM_DEFAULTS["EPISODE"] ?: ""),
            "IMAGE" to (SYSTEM_DEFAULTS["IMAGE"] ?: ""),
            "AI_DIRECTOR" to (SYSTEM_DEFAULTS["AI_DIRECTOR"] ?: ""),
            "GENRE" to (SYSTEM_DEFAULTS["GENRE"] ?: ""),
            "VISUAL_MEDIUM" to (SYSTEM_DEFAULTS["VISUAL_MEDIUM"] ?: "")
        )
    }
    val editModeByCat = remember {
        mutableStateMapOf(
            "BIBLE" to false,
            "EPISODE" to false,
            "IMAGE" to false,
            "AI_DIRECTOR" to false,
            "GENRE" to false,
            "VISUAL_MEDIUM" to false
        )
    }

    var savedPrompts by remember { mutableStateOf<List<SystemPromptDto>>(emptyList()) }
    val promptIdByCat = remember { mutableStateMapOf<String, String>() }
    var availableMedia by remember { mutableStateOf(MEDIA_OPTIONS) }
    var selectedMedium by remember { mutableStateOf(initialMedium?.uppercase() ?: "ANIME") }
    var selectedGenre by remember { mutableStateOf(initialGenre) }
    var genrePromptMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var mediumPromptMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val selectedGenreDisplay = selectedGenre ?: "Genre"
    var selectedFormat by remember { mutableStateOf<String?>(null) }
    var genreFilterEnabled by remember { mutableStateOf(false) }
    var availableGenres by remember { mutableStateOf(FALLBACK_GENRES) }
    var availableFormats by remember { mutableStateOf(FALLBACK_FORMATS) }
    var genreTemplates by remember { mutableStateOf<List<GenreTemplateDto>>(emptyList()) }
    var templateIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var pendingTabSwitch by remember { mutableStateOf<Int?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val currentTitle = titlesByCat[currentCategory] ?: ""
    val currentText = textByCat[currentCategory] ?: ""
    val currentBaseline = baselineByCat[currentCategory] ?: ""
    val isEditMode = editModeByCat[currentCategory] ?: false
    val hasUnsavedInCurrentTab = currentText != currentBaseline

    BackHandler(enabled = hasUnsavedInCurrentTab) {
        pendingTabSwitch = -1
    }

    fun loadPromptsForProject() {
        val pid = repository.currentProjectId()
        scope.launch {
            when (val res = repository.fetchSystemPrompts(pid)) {
                is Resource.Success -> {
                    savedPrompts = res.data
                    categories.forEach { cat ->
                        val match = res.data.firstOrNull {
                            (it.prompt_category ?: "BIBLE").uppercase() == cat &&
                                (cat != "GENRE" || selectedGenre == null || it.genre.equals(selectedGenre, ignoreCase = true)) &&
                                (cat != "VISUAL_MEDIUM" || it.visual_medium.equals(selectedMedium, ignoreCase = true))
                        }
                        if (match != null) {
                            promptIdByCat[cat] = match.id
                            if (baselineByCat[cat] == SYSTEM_DEFAULTS[cat]) {
                                titlesByCat[cat] = match.title
                                textByCat[cat] = match.prompt_text
                                baselineByCat[cat] = match.prompt_text
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPromptsForProject()
        when (val meta = repository.fetchPromptLibraryMeta()) {
            is Resource.Success -> {
                if (meta.data.categories.isNotEmpty()) categories = meta.data.categories
                if (meta.data.genres.isNotEmpty()) availableGenres = meta.data.genres
                if (meta.data.media.isNotEmpty()) availableMedia = meta.data.media
                if (meta.data.formats.isNotEmpty()) availableFormats = meta.data.formats
                genreTemplates = meta.data.genre_templates
                genrePromptMap = meta.data.genre_definitions.associate {
                    (it["title"] ?: it["key"] ?: "").toString() to (it["prompt_text"] ?: "").toString()
                }
                mediumPromptMap = meta.data.medium_definitions.associate {
                    (it["key"] ?: it["title"] ?: "").toString().uppercase() to (it["prompt_text"] ?: "").toString()
                }
                if (meta.data.project_selected_genres.isNotEmpty() && selectedGenre == null) {
                    selectedGenre = meta.data.project_selected_genres.first()
                }
                if (meta.data.project_visual_medium.isNotBlank()) selectedMedium = meta.data.project_visual_medium.uppercase()
            }
            else -> {}
        }
    }

    if (pendingTabSwitch != null) {
        UnsavedChangesDialog(
            onDiscard = {
                textByCat[currentCategory] = baselineByCat[currentCategory] ?: ""
                editModeByCat[currentCategory] = false
                val target = pendingTabSwitch ?: 0
                pendingTabSwitch = null
                if (target >= 0) selectedCatIndex = target
            },
            onKeepEditing = { pendingTabSwitch = null }
        )
    }

    if (showResetConfirm) {
        ConfirmationDialog(
            title = "Reset $currentCategory Prompt to Default?",
            message = "This will restore the official application default system prompt for $currentCategory.",
            confirmText = "Reset to Default",
            isDestructive = true,
            onConfirm = {
                showResetConfirm = false
                val defText = when (currentCategory) {
                    "GENRE" -> genrePromptMap[selectedGenre] ?: SYSTEM_DEFAULTS["GENRE"].orEmpty()
                    "VISUAL_MEDIUM" -> mediumPromptMap[selectedMedium] ?: SYSTEM_DEFAULTS["VISUAL_MEDIUM"].orEmpty()
                    else -> SYSTEM_DEFAULTS[currentCategory] ?: ""
                }
                titlesByCat[currentCategory] = "Default $currentCategory Prompt"
                textByCat[currentCategory] = defText
                editModeByCat[currentCategory] = true
            },
            onDismiss = { showResetConfirm = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.Screen.PROMPT_STUDIO)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { loadPromptsForProject() },
            onRefreshClick = { loadPromptsForProject() }
        )

        ScreenHeader(
            title = "Prompt Studio",
            subtitle = "Isolated category editors (BIBLE, EPISODE, IMAGE, AI_DIRECTOR) with View/Edit protection",
            trailingContent = {
                if (hasUnsavedInCurrentTab) {
                    StatusChip(label = "UNSAVED ($currentCategory)")
                } else {
                    StatusChip(label = if (isEditMode) "EDIT MODE" else "VIEW MODE")
                }
            }
        )

        ScrollableTabRow(selectedTabIndex = selectedCatIndex, edgePadding = 0.dp) {
            categories.forEachIndexed { idx, cat ->
                val dirty = (textByCat[cat] ?: "") != (baselineByCat[cat] ?: "")
                Tab(
                    selected = selectedCatIndex == idx,
                    modifier = Modifier.testTag(TestTags.item(TestTags.PromptStudio.TAB_PREFIX, cat)),
                    onClick = {
                        if (selectedCatIndex != idx) {
                            if (hasUnsavedInCurrentTab) {
                                pendingTabSwitch = idx
                            } else {
                                selectedCatIndex = idx
                            }
                        }
                    },
                    text = { Text(if (dirty) "$cat *" else cat) }
                )
            }
        }

        StandardCard {
            SectionHeader(title = "Master Prompt Library")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val idx = availableMedia.indexOf(selectedMedium)
                        selectedMedium = availableMedia[(idx + 1) % availableMedia.size]
                    },
                    modifier = Modifier.testTag(TestTags.PromptStudio.MEDIUM)
                ) {
                    Text("Medium: $selectedMedium")
                }
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("Favorites") },
                    modifier = Modifier.testTag(TestTags.PromptStudio.FAVORITES)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val options = listOf<String?>(null) + availableGenres
                        val idx = options.indexOf(selectedGenre)
                        selectedGenre = options[(idx + 1) % options.size]
                    },
                    modifier = Modifier.testTag(TestTags.PromptStudio.GENRE)
                ) {
                    Text("Genre: ${selectedGenre ?: "Any"}")
                }
                OutlinedButton(
                    onClick = {
                        val options = listOf<String?>(null) + availableFormats
                        val idx = options.indexOf(selectedFormat)
                        selectedFormat = options[(idx + 1) % options.size]
                    },
                    modifier = Modifier.testTag(TestTags.PromptStudio.FORMAT)
                ) {
                    Text("Format: ${selectedFormat ?: "Any"}")
                }
            }
            FilterChip(
                selected = genreFilterEnabled,
                onClick = { genreFilterEnabled = !genreFilterEnabled },
                label = { Text("Match genre/format") },
                modifier = Modifier.testTag(TestTags.PromptStudio.FILTER_GENRE_FORMAT)
            )
            if (genreTemplates.isNotEmpty()) {
                val template = genreTemplates[templateIndex.coerceIn(0, genreTemplates.lastIndex)]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { templateIndex = (templateIndex + 1) % genreTemplates.size },
                        modifier = Modifier.testTag(TestTags.PromptStudio.TEMPLATE)
                    ) {
                        Text("Template: ${template.genre}")
                    }
                    SecondaryButton(
                        text = "Apply Genre Template",
                        onClick = {
                            selectedGenre = template.genre
                            selectedFormat = template.format
                            selectedMedium = template.visual_medium
                            titlesByCat[currentCategory] = template.title
                            textByCat[currentCategory] = template.prompt_text
                            editModeByCat[currentCategory] = true
                        },
                        modifier = Modifier.testTag(TestTags.PromptStudio.APPLY_TEMPLATE)
                    )
                }
            }
            if (currentCategory == "GENRE" && selectedGenre != null) {
                SecondaryButton(
                    text = "Load Built-in Genre Prompt",
                    onClick = {
                        val prompt = genrePromptMap[selectedGenre] ?: ""
                        if (prompt.isNotBlank()) {
                            titlesByCat[currentCategory] = "${selectedGenre} Genre Prompt"
                            textByCat[currentCategory] = prompt
                            baselineByCat[currentCategory] = prompt
                            editModeByCat[currentCategory] = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (currentCategory == "VISUAL_MEDIUM") {
                SecondaryButton(
                    text = "Load Built-in Medium Prompt",
                    onClick = {
                        val prompt = mediumPromptMap[selectedMedium] ?: ""
                        if (prompt.isNotBlank()) {
                            titlesByCat[currentCategory] = "$selectedMedium Visual Medium Prompt"
                            textByCat[currentCategory] = prompt
                            baselineByCat[currentCategory] = prompt
                            editModeByCat[currentCategory] = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search prompts, genres, and text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.PromptStudio.SEARCH)
            )
        }

        StandardCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "$currentCategory Prompt Configuration",
                    modifier = Modifier.weight(1f)
                )
                if (!isEditMode) {
                    OutlinedButton(
                        onClick = { editModeByCat[currentCategory] = true },
                        modifier = Modifier.testTag(TestTags.PromptStudio.EDIT)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enter Edit Mode")
                    }
                }
            }

            if (hasUnsavedInCurrentTab) {
                Surface(
                    color = StudioTokens.Warning.copy(alpha = 0.15f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Unsaved changes in $currentCategory category. Save or Cancel before discarding.",
                        color = StudioTokens.Warning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = currentTitle,
                onValueChange = { if (isEditMode) titlesByCat[currentCategory] = it },
                readOnly = !isEditMode,
                label = { Text("Prompt Title ($currentCategory)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.PromptStudio.TITLE_FIELD)
            )

            OutlinedTextField(
                value = currentText,
                onValueChange = { if (isEditMode) textByCat[currentCategory] = it },
                readOnly = !isEditMode,
                label = { Text("System Prompt Instructions ($currentCategory)") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.PromptStudio.TEXT_FIELD)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    text = "Save $currentCategory",
                    isLoading = isSaving,
                    enabled = isEditMode && currentText.isNotBlank() && !isSaving,
                    onClick = {
                        val pid = when (promptScopeBy) {
                            "this_project" -> repository.currentProjectId()
                            "global" -> ""
                            else -> repository.currentProjectId()
                        }
                        isSaving = true
                        scope.launch {
                            val title = currentTitle.trim().ifBlank { "$currentCategory Prompt" }
                            val existingId = promptIdByCat[currentCategory]
                            val res = if (existingId.isNullOrBlank()) {
                                repository.saveSystemPrompt(
                                    projectId = pid,
                                    title = title,
                                    promptText = currentText.trim(),
                                    visualMedium = selectedMedium,
                                    promptCategory = currentCategory,
                                    genre = if (currentCategory == "GENRE") (selectedGenre ?: title) else selectedGenre,
                                    format = selectedFormat
                                )
                            } else {
                                repository.updateSystemPrompt(
                                    promptId = existingId,
                                    title = title,
                                    promptText = currentText.trim(),
                                    visualMedium = selectedMedium,
                                    promptCategory = currentCategory,
                                    genre = if (currentCategory == "GENRE") (selectedGenre ?: title) else selectedGenre,
                                    format = selectedFormat
                                )
                            }
                            isSaving = false
                            if (res is Resource.Success) {
                                promptIdByCat[currentCategory] = res.data.id
                                baselineByCat[currentCategory] = currentText.trim()
                                editModeByCat[currentCategory] = false
                                loadPromptsForProject()
                                Toast.makeText(context, "$currentCategory prompt saved", Toast.LENGTH_SHORT).show()
                            } else if (res is Resource.Error) {
                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag(TestTags.PromptStudio.SAVE)
                )

                SecondaryButton(
                    text = "Cancel",
                    enabled = isEditMode || hasUnsavedInCurrentTab,
                    onClick = {
                        textByCat[currentCategory] = baselineByCat[currentCategory] ?: ""
                        editModeByCat[currentCategory] = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.PromptStudio.RESET)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset $currentCategory to Application Default")
            }
        }

        val categorySavedList = savedPrompts.filter { prompt ->
            val categoryMatches = (prompt.prompt_category ?: "BIBLE").uppercase() == currentCategory
            val queryMatches = searchQuery.isBlank() ||
                prompt.title.contains(searchQuery, ignoreCase = true) ||
                prompt.prompt_text.contains(searchQuery, ignoreCase = true) ||
                (prompt.genre ?: "").contains(searchQuery, ignoreCase = true)
            val favoriteMatches = !favoritesOnly || prompt.is_favorite
            val genreMatches = !genreFilterEnabled || selectedGenre == null ||
                prompt.genre.equals(selectedGenre, ignoreCase = true)
            val formatMatches = !genreFilterEnabled || selectedFormat == null ||
                prompt.format.equals(selectedFormat, ignoreCase = true)
            categoryMatches && queryMatches && favoriteMatches && genreMatches && formatMatches
        }
        SectionHeader(
            title = "Saved $currentCategory Prompts (${categorySavedList.size})",
            badgeText = currentCategory
        )

        if (categorySavedList.isEmpty()) {
            StandardCard {
                Text(
                    text = "No custom saved prompts for $currentCategory yet.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            categorySavedList.forEach { item ->
                StandardCard(
                    modifier = Modifier.testTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, item.id)),
                    onClick = {
                        promptIdByCat[currentCategory] = item.id
                        titlesByCat[currentCategory] = item.title
                        textByCat[currentCategory] = item.prompt_text
                        baselineByCat[currentCategory] = item.prompt_text
                        selectedMedium = item.visual_medium
                        selectedGenre = item.genre
                        selectedFormat = item.format
                        editModeByCat[currentCategory] = false
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = "v${item.version} | ${item.genre ?: "Any genre"} | ${item.visual_medium}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.prompt_text,
                                maxLines = 2,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                if (item.id.isBlank()) return@IconButton
                                scope.launch {
                                    repository.togglePromptFavorite(item.id)
                                    loadPromptsForProject()
                                }
                            },
                            modifier = Modifier.testTag(TestTags.item(TestTags.PromptStudio.FAVORITE_PREFIX, item.id))
                        ) {
                            Text(
                                text = if (item.is_favorite) "\u2605" else "\u2606",
                                fontSize = 20.sp,
                                color = StudioTokens.Warning
                            )
                        }
                        IconButton(onClick = {
                            val promptId = item.id
                            if (promptId.isBlank()) return@IconButton
                            scope.launch {
                                repository.deleteSystemPrompt(promptId)
                                loadPromptsForProject()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete prompt", tint = StudioTokens.Error)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
