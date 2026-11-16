package org.dpdns.bnai.anime.ui.studio

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.SeasonPlan
import org.dpdns.bnai.anime.core.model.seasonPlansFromBible
import org.dpdns.bnai.anime.core.model.storyBibleMap
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.PromptVersionDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*
import org.dpdns.bnai.anime.ui.testing.TestTags

private val CATEGORIES = listOf("BIBLE", "SEASON", "EPISODE", "IMAGE", "AI_DIRECTOR", "GENRE", "VISUAL_MEDIUM")
private val CREATIVE_CATEGORIES = setOf("GENRE", "VISUAL_MEDIUM")
private val MEDIA_OPTIONS = listOf("ANIME", "MANHWA", "3D", "2D_ANIMATION", "REAL_HUMAN", "LIVE_ACTION", "CINEMATIC", "HYBRID")
private val FALLBACK_GENRES = listOf("Action", "Fantasy", "Dark Fantasy", "Action Fantasy", "Thriller", "Horror", "Romance", "Comedy", "Sci-Fi", "Mystery", "Drama", "Slice of Life", "Mythology", "Music")
private val FALLBACK_FORMATS = listOf("SERIES", "MOVIE", "SHORT", "MUSIC_VIDEO", "TRAILER")
private val SYSTEM_DEFAULTS = mapOf(
    "BIBLE" to "You are the WF-01 Master Story Bible Architect. Build cohesive anime world rules, power scaling ceilings, and immutable character anchors.",
    "SEASON" to "You are the Season Architect. Build a season arc, escalating goals, character turns, and episode-range blueprint that obey the project Story Bible and never contradict locked canon.",
    "EPISODE" to "You are the WF-02 Episode Screenwriter. Write tight, high-retention multi-scene anime scripts strictly obeying locked Canon and character Visual DNA.",
    "IMAGE" to "Masterpiece cinematic anime keyframe, ultra-detailed cel shading, dynamic volumetric lighting, strict adherence to character 11-field Visual DNA.",
    "AI_DIRECTOR" to "You are the AI Anime Studio Director overseeing StoryWF-00..13. Provide actionable production guidance and never mutate state without user confirmation."
)

private enum class PromptSource { BUILT_IN, CUSTOM, APP_DEFAULT }
private enum class LibraryFilter { ALL, BUILT_IN, CUSTOM }
private enum class PromptScope { PROJECT, GLOBAL }

private data class LibraryPrompt(
    val id: String,
    val title: String,
    val text: String,
    val category: String,
    val source: PromptSource,
    val version: Int = 1,
    val genre: String? = null,
    val format: String? = null,
    val visualMedium: String? = null,
    val isFavorite: Boolean = false,
    val isActive: Boolean = false
)

private data class PromptDraft(
    val id: String? = null,
    val title: String = "",
    val text: String = "",
    val category: String = "BIBLE",
    val source: PromptSource = PromptSource.APP_DEFAULT,
    val version: Int = 1,
    val genre: String? = null,
    val format: String? = null,
    val visualMedium: String = "ANIME",
    val scope: PromptScope = PromptScope.PROJECT,
    val isNew: Boolean = false,
    val isActive: Boolean = false
)

private fun JsonMap.stringValue(key: String): String =
    this[key]?.toString().orEmpty().let { if (it == "null") "" else it }

private fun JsonMap.intValue(key: String): Int =
    (this[key] as? Number)?.toInt() ?: 1

private fun JsonMap.boolValue(key: String): Boolean =
    this[key] == true || this[key]?.toString().equals("true", ignoreCase = true)

private fun normalizePromptKey(value: String?): String =
    value.orEmpty().trim().lowercase().replace(Regex("[^a-z0-9]+"), "")

private fun canonicalMediumKey(value: String): String =
    value.trim().uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').ifBlank { "ANIME" }

private fun sourceLabel(source: PromptSource): String = when (source) {
    PromptSource.BUILT_IN -> "BUILT-IN"
    PromptSource.CUSTOM -> "CUSTOM"
    PromptSource.APP_DEFAULT -> "DEFAULT"
}

private fun buildDefinitionEntries(
    category: String,
    definitions: List<JsonMap>,
    savedById: Map<String, SystemPromptDto>,
    activeGenres: List<String>,
    activeMedium: String
): List<LibraryPrompt> = definitions.map { definition ->
    val title = definition.stringValue("title").ifBlank { definition.stringValue("key") }
    val key = definition.stringValue("key")
    val promptId = definition.stringValue("prompt_id")
    val isCustom = definition.boolValue("custom")
    val entryId = promptId.ifBlank { "builtin:${category}:${key.ifBlank { normalizePromptKey(title) }}" }
    val saved = savedById[promptId]
    val active = if (category == "GENRE") {
        activeGenres.any { normalizePromptKey(it) == normalizePromptKey(title) }
    } else {
        normalizePromptKey(activeMedium) == normalizePromptKey(key.ifBlank { title })
    }
    LibraryPrompt(
        id = entryId,
        title = title,
        text = definition.stringValue("prompt_text"),
        category = category,
        source = if (isCustom) PromptSource.CUSTOM else PromptSource.BUILT_IN,
        version = definition.intValue("version"),
        genre = if (category == "GENRE") title else null,
        visualMedium = if (category == "VISUAL_MEDIUM") key.ifBlank { title } else null,
        isFavorite = saved?.is_favorite == true,
        isActive = active
    )
}

private fun savedPromptToEntry(prompt: SystemPromptDto, activeBiblePromptId: String?): LibraryPrompt = LibraryPrompt(
    id = prompt.id,
    title = prompt.title,
    text = prompt.prompt_text,
    category = (prompt.prompt_category ?: "BIBLE").uppercase(),
    source = PromptSource.CUSTOM,
    version = prompt.version,
    genre = prompt.genre,
    format = prompt.format,
    visualMedium = prompt.visual_medium,
    isFavorite = prompt.is_favorite,
    isActive = (prompt.prompt_category ?: "BIBLE").uppercase() == "BIBLE" && prompt.id == activeBiblePromptId
)

private fun defaultEntry(category: String, medium: String, isActive: Boolean = false): LibraryPrompt = LibraryPrompt(
    id = "appdefault:$category",
    title = "Application Default",
    text = SYSTEM_DEFAULTS[category].orEmpty(),
    category = category,
    source = PromptSource.APP_DEFAULT,
    visualMedium = medium,
    isActive = isActive
)

private fun promptMeta(entry: LibraryPrompt): String = listOfNotNull(
    entry.genre?.takeIf { it.isNotBlank() }?.let { "Genre: $it" },
    entry.visualMedium?.takeIf { it.isNotBlank() && entry.category == "VISUAL_MEDIUM" }?.let { "Medium: $it" },
    entry.format?.takeIf { it.isNotBlank() }?.let { "Format: $it" }
).joinToString("  ·  ")

private fun defaultCreativeText(category: String): String = when (category) {
    "GENRE" -> "Define this genre's mandatory story promises, conflicts, pacing, character archetypes, forbidden clichés, and emotional intensity. Keep the locked visual medium unchanged."
    "VISUAL_MEDIUM" -> "Define this visual medium's authoritative anatomy, line art, color treatment, lighting, texture, framing, motion, and character consistency rules. Do not substitute a generic look."
    else -> SYSTEM_DEFAULTS[category].orEmpty()
}

@Composable
fun PromptStudioScreen(
    initialTab: String = "BIBLE",
    initialGenre: String? = null,
    initialMedium: String? = null,
    initialSeason: Int? = null
) {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val screenScope = rememberCoroutineScope()

    var selectedCatIndex by remember {
        mutableStateOf(CATEGORIES.indexOf(initialTab.uppercase()).coerceAtLeast(0))
    }
    val currentCategory = CATEGORIES.getOrElse(selectedCatIndex) { CATEGORIES.first() }
    val isCreativeCategory = currentCategory in CREATIVE_CATEGORIES

    var savedPrompts by remember { mutableStateOf<List<SystemPromptDto>>(emptyList()) }
    var genreDefinitions by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var mediumDefinitions by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var availableGenres by remember { mutableStateOf(FALLBACK_GENRES) }
    var availableMedia by remember { mutableStateOf(MEDIA_OPTIONS) }
    var availableFormats by remember { mutableStateOf(FALLBACK_FORMATS) }
    var projectSelectedGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var projectMedium by remember { mutableStateOf(initialMedium?.uppercase() ?: "ANIME") }
    var seasonPlans by remember { mutableStateOf<List<SeasonPlan>>(emptyList()) }
    var selectedSeasonNo by remember { mutableStateOf(initialSeason) }
    var showSeasonMenu by remember { mutableStateOf(false) }
    val selectedSeason = seasonPlans.firstOrNull { it.seasonNo == selectedSeasonNo }
    var activeBiblePromptId by remember { mutableStateOf<String?>(null) }
    var isLoadingLibrary by remember { mutableStateOf(true) }
    var libraryError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var libraryFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var selectedGenreFilter by remember { mutableStateOf<String?>(null) }
    var selectedFormatFilter by remember { mutableStateOf<String?>(null) }
    var selectedMediumFilter by remember { mutableStateOf<String?>(null) }

    var showFilters by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<PromptDraft?>(null) }
    var originalDraft by remember { mutableStateOf<PromptDraft?>(null) }
    var editorMode by remember { mutableStateOf(false) }
    var isSavingPrompt by remember { mutableStateOf(false) }
    var isActivatingPrompt by remember { mutableStateOf(false) }
    var promptVersions by remember { mutableStateOf<List<PromptVersionDto>>(emptyList()) }
    var isLoadingVersions by remember { mutableStateOf(false) }
    var versionsError by remember { mutableStateOf<String?>(null) }

    var addName by remember { mutableStateOf("") }
    var addText by remember { mutableStateOf("") }
    var addBaseId by remember { mutableStateOf<String?>(null) }
    var addGenre by remember { mutableStateOf<String?>(null) }
    var addFormat by remember { mutableStateOf<String?>(null) }
    var addMedium by remember { mutableStateOf<String?>(null) }
    var addScope by remember { mutableStateOf(PromptScope.PROJECT) }
    var addError by remember { mutableStateOf<String?>(null) }

    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filtersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun loadLibrary() {
        val projectId = repository.currentProjectId()
        isLoadingLibrary = true
        libraryError = null
        screenScope.launch {
            when (val promptResult = repository.fetchSystemPrompts(projectId)) {
                is Resource.Success -> savedPrompts = promptResult.data
                is Resource.Empty -> savedPrompts = emptyList()
                is Resource.Error -> Unit
                else -> Unit
            }
            when (val metaResult = repository.fetchPromptLibraryMeta(projectId)) {
                is Resource.Success -> {
                    val meta = metaResult.data
                    if (meta.genres.isNotEmpty()) availableGenres = meta.genres
                    if (meta.media.isNotEmpty()) availableMedia = meta.media
                    if (meta.formats.isNotEmpty()) availableFormats = meta.formats
                    genreDefinitions = meta.genre_definitions
                    mediumDefinitions = meta.medium_definitions
                    projectSelectedGenres = meta.project_selected_genres
                    if (meta.project_visual_medium.isNotBlank()) projectMedium = meta.project_visual_medium.uppercase()
                }
                is Resource.Error -> libraryError = metaResult.message
                is Resource.Empty -> libraryError = "Prompt library metadata is empty."
                else -> Unit
            }
            if (projectId.isNotBlank()) {
                when (val bibleResult = repository.fetchBible(projectId)) {
                    is Resource.Success -> {
                        val bible = bibleResult.data.storyBibleMap()
                        val total = bible.intValue("total_seasons").coerceIn(1, 100)
                        val perSeason = ((bible["episodes_per_season"] as? Number)?.toInt() ?: 12).coerceIn(1, 500)
                        seasonPlans = seasonPlansFromBible(bible, total, perSeason)
                        val wanted = selectedSeasonNo
                        if (wanted == null || seasonPlans.none { it.seasonNo == wanted }) {
                            selectedSeasonNo = seasonPlans.firstOrNull()?.seasonNo
                        }
                    }
                    else -> Unit
                }
                when (val overviewResult = repository.fetchProjectOverview(projectId)) {
                    is Resource.Success -> activeBiblePromptId = overviewResult.data.project?.active_system_prompt_id
                    is Resource.Empty -> activeBiblePromptId = null
                    is Resource.Error -> Unit
                    else -> Unit
                }
            }
            isLoadingLibrary = false
        }
    }

    fun loadVersions(promptId: String) {
        promptVersions = emptyList()
        versionsError = null
        if (promptId.isBlank()) {
            isLoadingVersions = false
            return
        }
        isLoadingVersions = true
        screenScope.launch {
            when (val result = repository.fetchPromptVersions(promptId)) {
                is Resource.Success -> promptVersions = result.data
                is Resource.Empty -> promptVersions = emptyList()
                is Resource.Error -> versionsError = result.message
                else -> Unit
            }
            isLoadingVersions = false
        }
    }

    fun openEntry(entry: LibraryPrompt) {
        val next = PromptDraft(
            id = entry.id.takeIf { entry.source == PromptSource.CUSTOM },
            title = entry.title,
            text = entry.text,
            category = entry.category,
            source = entry.source,
            version = entry.version,
            genre = entry.genre,
            format = entry.format,
            visualMedium = entry.visualMedium ?: projectMedium,
            scope = PromptScope.PROJECT,
            isNew = false,
            isActive = entry.isActive
        )
        draft = next
        originalDraft = next
        editorMode = false
        if (entry.source == PromptSource.CUSTOM) {
            loadVersions(entry.id)
        } else {
            promptVersions = emptyList()
            versionsError = null
            isLoadingVersions = false
        }
        showEditor = true
    }

    fun useEntryAsBase(entry: LibraryPrompt) {
        val next = PromptDraft(
            id = null,
            title = "${entry.title} Custom Prompt",
            text = entry.text,
            category = entry.category,
            source = PromptSource.CUSTOM,
            version = 1,
            genre = entry.genre,
            format = entry.format,
            visualMedium = entry.visualMedium ?: projectMedium,
            scope = PromptScope.PROJECT,
            isNew = true
        )
        draft = next
        originalDraft = next
        editorMode = true
    }

    fun closeEditor() {
        showEditor = false
        draft = null
        originalDraft = null
        editorMode = false
    }

    fun requestCloseEditor() {
        if (editorMode && originalDraft != null && draft != originalDraft) {
            showDiscardDialog = true
        } else {
            closeEditor()
        }
    }

    fun addProjectGenre(genreName: String) {
        val projectId = repository.currentProjectId()
        if (projectId.isBlank()) return
        val updated = (projectSelectedGenres + genreName)
            .distinctBy { normalizePromptKey(it) }
        screenScope.launch {
            repository.updateProjectSettings(projectId, updates = mapOf("genre" to updated.joinToString(", ")))
            loadLibrary()
        }
    }

    fun setProjectMedium(mediumKey: String) {
        val projectId = repository.currentProjectId()
        if (projectId.isBlank()) return
        screenScope.launch {
            repository.updateProjectSettings(projectId, updates = mapOf("visual_medium" to mediumKey))
            loadLibrary()
        }
    }

    fun saveDraft() {
        val current = draft ?: return
        if (current.title.isBlank() || current.text.isBlank() || isSavingPrompt) return
        val projectId = if (current.scope == PromptScope.PROJECT) repository.currentProjectId() else ""
        val genre = if (current.category == "GENRE") current.genre ?: current.title else current.genre
        isSavingPrompt = true
        screenScope.launch {
            val result = if (current.isNew || current.id.isNullOrBlank()) {
                repository.saveSystemPrompt(
                    projectId = projectId,
                    title = current.title.trim(),
                    promptText = current.text.trim(),
                    visualMedium = current.visualMedium,
                    promptCategory = current.category,
                    genre = genre,
                    format = current.format,
                    modelHint = null
                )
            } else {
                repository.updateSystemPrompt(
                    promptId = current.id,
                    title = current.title.trim(),
                    promptText = current.text.trim(),
                    visualMedium = current.visualMedium,
                    promptCategory = current.category,
                    isFavorite = null,
                    genre = genre,
                    format = current.format,
                    modelHint = null
                )
            }
            isSavingPrompt = false
            when (result) {
                is Resource.Success -> {
                    closeEditor()
                    loadLibrary()
                    Toast.makeText(context, "Prompt saved", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                is Resource.Empty -> Toast.makeText(context, "Prompt was not saved", Toast.LENGTH_LONG).show()
                else -> Unit
            }
        }
    }

    fun saveNewCreative() {
        val name = addName.trim()
        if (name.isBlank()) {
            addError = "Name is required."
            return
        }
        if (addText.isBlank()) {
            addError = "Prompt instructions are required."
            return
        }
        val duplicateSavedById = savedPrompts.associateBy { it.id }
        val duplicate = when (currentCategory) {
            "GENRE" -> buildDefinitionEntries(
                "GENRE",
                genreDefinitions,
                duplicateSavedById,
                projectSelectedGenres,
                projectMedium
            ).any {
                normalizePromptKey(it.title) == normalizePromptKey(name) ||
                    normalizePromptKey(it.genre) == normalizePromptKey(name)
            }
            "VISUAL_MEDIUM" -> buildDefinitionEntries(
                "VISUAL_MEDIUM",
                mediumDefinitions,
                duplicateSavedById,
                projectSelectedGenres,
                projectMedium
            ).any {
                normalizePromptKey(it.title) == normalizePromptKey(name) ||
                    normalizePromptKey(it.visualMedium) == normalizePromptKey(name)
            }
            else -> savedPrompts.any {
                (it.prompt_category ?: "BIBLE").uppercase() == currentCategory &&
                    normalizePromptKey(it.title) == normalizePromptKey(name)
            }
        }
        if (duplicate) {
            addError = "This name already exists. Open it from the library instead."
            return
        }
        val projectId = if (addScope == PromptScope.PROJECT) repository.currentProjectId() else ""
        val category = currentCategory
        val genre = when (category) {
            "GENRE" -> name
            else -> addGenre
        }
        val format = when (category) {
            "BIBLE", "SEASON", "EPISODE" -> addFormat
            else -> null
        }
        val medium = when (category) {
            "VISUAL_MEDIUM" -> canonicalMediumKey(name)
            else -> addMedium?.uppercase() ?: projectMedium
        }
        isSavingPrompt = true
        screenScope.launch {
            val result = repository.saveSystemPrompt(
                projectId = projectId,
                title = name,
                promptText = addText.trim(),
                visualMedium = medium,
                promptCategory = category,
                genre = genre,
                format = format,
                modelHint = null
            )
            isSavingPrompt = false
            when (result) {
                is Resource.Success -> {
                    showAddSheet = false
                    addError = null
                    if (category == "GENRE") addProjectGenre(name)
                    if (category == "VISUAL_MEDIUM") setProjectMedium(medium)
                    loadLibrary()
                    Toast.makeText(context, "$name saved", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> addError = result.message
                is Resource.Empty -> addError = "Prompt was not saved."
                else -> Unit
            }
        }
    }

    fun deleteDraft() {
        val current = draft ?: return
        val promptId = current.id ?: return
        if (current.source != PromptSource.CUSTOM || promptId.isBlank()) return
        showDeleteDialog = false
        screenScope.launch {
            when (val result = repository.deleteSystemPrompt(promptId)) {
                is Resource.Success -> {
                    closeEditor()
                    loadLibrary()
                    Toast.makeText(context, "Custom prompt removed", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                is Resource.Empty -> closeEditor()
                else -> Unit
            }
        }
    }

    fun toggleFavorite(entry: LibraryPrompt) {
        if (entry.source != PromptSource.CUSTOM || entry.id.isBlank()) return
        screenScope.launch {
            when (val result = repository.togglePromptFavorite(entry.id)) {
                is Resource.Success -> loadLibrary()
                is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                is Resource.Empty -> Unit
                else -> Unit
            }
        }
    }

    fun activateBiblePrompt(promptId: String) {
        val projectId = repository.currentProjectId()
        if (projectId.isBlank() || promptId.isBlank() || isActivatingPrompt) return
        isActivatingPrompt = true
        screenScope.launch {
            when (val result = repository.activateSystemPrompt(projectId, promptId, "BIBLE")) {
                is Resource.Success -> {
                    activeBiblePromptId = promptId
                    draft = draft?.copy(isActive = true)
                    loadLibrary()
                    Toast.makeText(context, "Story Bible prompt activated", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                is Resource.Empty -> Toast.makeText(context, "Prompt was not activated", Toast.LENGTH_LONG).show()
                else -> Unit
            }
            isActivatingPrompt = false
        }
    }

    fun openAddSheet() {
        addName = ""
        addBaseId = null
        addGenre = selectedGenreFilter
        addFormat = selectedFormatFilter
        addMedium = selectedMediumFilter ?: projectMedium
        addScope = PromptScope.PROJECT
        addError = null
        addText = defaultCreativeText(currentCategory)
        showAddSheet = true
    }

    LaunchedEffect(Unit) { loadLibrary() }

    LaunchedEffect(currentCategory) {
        searchQuery = ""
        favoritesOnly = false
        libraryFilter = LibraryFilter.ALL
        selectedGenreFilter = null
        selectedFormatFilter = null
        selectedMediumFilter = null
    }

    LaunchedEffect(initialGenre, initialMedium) {
        if (!initialGenre.isNullOrBlank()) {
            selectedGenreFilter = initialGenre
        }
        if (!initialMedium.isNullOrBlank()) {
            projectMedium = initialMedium.uppercase()
        }
    }

    val savedById = savedPrompts.associateBy { it.id }
    val genreEntries = buildDefinitionEntries("GENRE", genreDefinitions, savedById, projectSelectedGenres, projectMedium)
    val mediumEntries = buildDefinitionEntries("VISUAL_MEDIUM", mediumDefinitions, savedById, projectSelectedGenres, projectMedium)
    val defaultEntries = emptyList<LibraryPrompt>()
    val savedEntries = if (isCreativeCategory) {
        emptyList()
    } else {
        savedPrompts
            .filter { (it.prompt_category ?: "BIBLE").uppercase() == currentCategory }
            .map { savedPromptToEntry(it, activeBiblePromptId) }
    }
    val allEntries = when (currentCategory) {
        "GENRE" -> genreEntries
        "VISUAL_MEDIUM" -> mediumEntries
        else -> defaultEntries + savedEntries
    }

    val hasGenreFilter = currentCategory in setOf("BIBLE", "SEASON", "EPISODE", "IMAGE")
    val hasFormatFilter = currentCategory in setOf("BIBLE", "SEASON", "EPISODE")
    val hasMediumFilter = currentCategory == "IMAGE"
    val hasAdvancedFilters = hasGenreFilter || hasFormatFilter || hasMediumFilter
    val advancedFilterCount = listOfNotNull(
        selectedGenreFilter.takeIf { hasGenreFilter },
        selectedFormatFilter.takeIf { hasFormatFilter },
        selectedMediumFilter.takeIf { hasMediumFilter }
    ).size

    val builtInFilterCount = allEntries.count {
        if (isCreativeCategory) it.source == PromptSource.BUILT_IN else it.source == PromptSource.APP_DEFAULT
    }
    val savedFilterCount = allEntries.count { it.source == PromptSource.CUSTOM }

    val filteredEntries = allEntries.filter { entry ->
        val sourceMatches = when (libraryFilter) {
            LibraryFilter.ALL -> true
            LibraryFilter.BUILT_IN -> if (isCreativeCategory) {
                entry.source == PromptSource.BUILT_IN
            } else {
                entry.source == PromptSource.APP_DEFAULT
            }
            LibraryFilter.CUSTOM -> entry.source == PromptSource.CUSTOM
        }
        val searchMatches = searchQuery.isBlank() ||
            entry.title.contains(searchQuery, ignoreCase = true) ||
            entry.text.contains(searchQuery, ignoreCase = true) ||
            entry.genre.orEmpty().contains(searchQuery, ignoreCase = true) ||
            entry.visualMedium.orEmpty().contains(searchQuery, ignoreCase = true) ||
            entry.format.orEmpty().contains(searchQuery, ignoreCase = true)
        val favoriteMatches = !favoritesOnly || entry.isFavorite
        val genreMatches = !hasGenreFilter || selectedGenreFilter == null ||
            normalizePromptKey(entry.genre) == normalizePromptKey(selectedGenreFilter)
        val formatMatches = !hasFormatFilter || selectedFormatFilter == null ||
            entry.format.equals(selectedFormatFilter, ignoreCase = true)
        val mediumMatches = !hasMediumFilter || selectedMediumFilter == null ||
            normalizePromptKey(entry.visualMedium) == normalizePromptKey(selectedMediumFilter)
        sourceMatches && searchMatches && favoriteMatches && genreMatches && formatMatches && mediumMatches
    }

    val addLabel = when (currentCategory) {
        "GENRE" -> "+ New genre"
        "VISUAL_MEDIUM" -> "+ New medium"
        else -> "+ New prompt"
    }

    val addBaseOptions = when (currentCategory) {
        "GENRE" -> genreEntries
        "VISUAL_MEDIUM" -> mediumEntries
        else -> defaultEntries
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
            onProjectChanged = { loadLibrary() },
            onRefreshClick = { loadLibrary() }
        )

        ScreenHeader(
            title = "Prompt Studio",
            subtitle = "Browse built-in and saved prompts. Tap a card to view, edit, or use it as a base.",
            trailingContent = {
                StatusChip(label = currentCategory)
            }
        )

        if (seasonPlans.isNotEmpty()) {
            StandardCard {
                SectionHeader(title = "Project & Season Context", badgeText = "ONE BIBLE")
                Text(
                    text = "BIBLE is project-wide. Season prompts and season genres apply only when assigned to a season, and they can never override locked canon.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Project genres (read-only): " +
                        projectSelectedGenres.ifEmpty { listOf("Not set") }.joinToString(", "),
                    fontSize = 11.sp,
                    color = StudioTokens.Primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedSeason?.displayTitle ?: "Project-wide",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = selectedSeason?.let {
                                "Episodes ${it.episodeStart}–${it.episodeEnd}" +
                                    if (it.genres.isNotEmpty()) " · Extra genres: ${it.genres.joinToString(", ")}" else ""
                            } ?: "Select a season to inspect its prompt context.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        OutlinedButton(onClick = { showSeasonMenu = true }) {
                            Text(selectedSeason?.let { "S${it.seasonNo}" } ?: "Season")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = showSeasonMenu, onDismissRequest = { showSeasonMenu = false }) {
                            seasonPlans.forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan.displayTitle) },
                                    onClick = {
                                        selectedSeasonNo = plan.seasonNo
                                        showSeasonMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedCatIndex, edgePadding = 0.dp) {
            CATEGORIES.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCatIndex == index,
                    modifier = Modifier.testTag(TestTags.item(TestTags.PromptStudio.TAB_PREFIX, category)),
                    onClick = { selectedCatIndex = index },
                    text = { Text(category) }
                )
            }
        }

        if (currentCategory == "GENRE") {
            ActiveGenreSummary(
                genres = projectSelectedGenres,
                entries = genreEntries,
                onOpen = { openEntry(it) }
            )
        }

        if (currentCategory == "VISUAL_MEDIUM") {
            ActiveMediumSummary(
                medium = projectMedium,
                entries = mediumEntries,
                onOpen = { openEntry(it) }
            )
        }

        StandardCard {
            SectionHeader(
                title = "Prompt Library",
                badgeText = "${filteredEntries.size} shown"
            )

            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search prompts, genres, media, or text",
                modifier = Modifier.testTag(TestTags.PromptStudio.SEARCH)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = libraryFilter == LibraryFilter.ALL,
                    onClick = { libraryFilter = LibraryFilter.ALL },
                    label = { Text("All (${allEntries.size})") }
                )
                FilterChip(
                    selected = libraryFilter == LibraryFilter.BUILT_IN,
                    onClick = { libraryFilter = LibraryFilter.BUILT_IN },
                    label = {
                        Text(if (isCreativeCategory) "Built-in ($builtInFilterCount)" else "Default ($builtInFilterCount)")
                    }
                )
                FilterChip(
                    selected = libraryFilter == LibraryFilter.CUSTOM,
                    onClick = { libraryFilter = LibraryFilter.CUSTOM },
                    label = { Text("My Saved ($savedFilterCount)") }
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("Favorites") },
                    modifier = Modifier.testTag(TestTags.PromptStudio.FAVORITES)
                )
            }

            if (hasAdvancedFilters) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryButton(
                        text = if (advancedFilterCount > 0) "Filters ($advancedFilterCount)" else "Filters",
                        onClick = { showFilters = true },
                        modifier = Modifier.weight(1f)
                    )
                    if (advancedFilterCount > 0) {
                        SecondaryButton(
                            text = "Clear",
                            onClick = {
                                selectedGenreFilter = null
                                selectedFormatFilter = null
                                selectedMediumFilter = null
                            }
                        )
                    }
                }
            }

            if (isLoadingLibrary) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
                }
            } else if (libraryError != null && allEntries.isEmpty()) {
                Text(
                    text = libraryError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
                SecondaryButton(
                    text = "Retry",
                    onClick = { loadLibrary() },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (filteredEntries.isEmpty()) {
                Text(
                    text = when {
                        searchQuery.isNotBlank() -> "No prompts match your search."
                        libraryFilter == LibraryFilter.CUSTOM -> "No custom prompts yet. Built-in prompts are already active."
                        favoritesOnly -> "No favorite prompts yet."
                        else -> "No prompts available. Try refresh."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filteredEntries.forEach { entry ->
                    PromptLibraryCard(
                        entry = entry,
                        onOpen = { openEntry(entry) },
                        onFavorite = { toggleFavorite(entry) }
                    )
                }
            }

            SecondaryButton(
                text = addLabel,
                onClick = { openAddSheet() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PromptStudio.ADD)
            )
        }

        Spacer(modifier = Modifier.height(64.dp))
    }

    if (showFilters) {
        FiltersSheet(
            sheetState = filtersSheetState,
            hasGenreFilter = hasGenreFilter,
            hasFormatFilter = hasFormatFilter,
            hasMediumFilter = hasMediumFilter,
            selectedGenre = selectedGenreFilter,
            selectedFormat = selectedFormatFilter,
            selectedMedium = selectedMediumFilter,
            genres = availableGenres,
            formats = availableFormats,
            media = availableMedia,
            onGenreSelected = { selectedGenreFilter = it },
            onFormatSelected = { selectedFormatFilter = it },
            onMediumSelected = { selectedMediumFilter = it },
            onClear = {
                selectedGenreFilter = null
                selectedFormatFilter = null
                selectedMediumFilter = null
            },
            onDismiss = { showFilters = false }
        )
    }

    if (showAddSheet) {
        AddPromptSheet(
            sheetState = addSheetState,
            category = currentCategory,
            name = addName,
            text = addText,
            baseOptions = addBaseOptions,
            selectedBaseId = addBaseId,
            genre = addGenre,
            format = addFormat,
            medium = addMedium,
            scope = addScope,
            genres = availableGenres,
            formats = availableFormats,
            media = availableMedia,
            error = addError,
            isSaving = isSavingPrompt,
            onNameChange = { addName = it; addError = null },
            onTextChange = { addText = it; addError = null },
            onBaseSelected = { baseId ->
                addBaseId = baseId
                val selected = addBaseOptions.firstOrNull { it.id == baseId }
                if (selected != null) addText = selected.text
            },
            onGenreChange = { addGenre = it },
            onFormatChange = { addFormat = it },
            onMediumChange = { addMedium = it },
            onScopeChange = { addScope = it },
            onSave = { saveNewCreative() },
            onDismiss = { if (!isSavingPrompt) showAddSheet = false }
        )
    }

    if (showEditor && draft != null) {
        val currentDraft = draft!!
        val currentOriginal = originalDraft
        val canSave = currentDraft.title.isNotBlank() &&
            currentDraft.text.isNotBlank() &&
            (currentDraft.isNew || currentOriginal == null || currentDraft != currentOriginal)
        val activateAction: (() -> Unit)? = if (
            currentDraft.category == "BIBLE" &&
            currentDraft.source == PromptSource.CUSTOM &&
            currentDraft.id != null &&
            !currentDraft.isActive
        ) {
            { currentDraft.id?.let { activateBiblePrompt(it) } }
        } else {
            null
        }
        PromptEditorSheet(
            sheetState = editorSheetState,
            draft = currentDraft,
            editMode = editorMode,
            isSaving = isSavingPrompt,
            canSave = canSave,
            versions = promptVersions,
            isLoadingVersions = isLoadingVersions,
            versionsError = versionsError,
            isActivating = isActivatingPrompt,
            onRetryVersions = { currentDraft.id?.let { loadVersions(it) } },
            onUseVersion = { version ->
                draft = currentDraft.copy(
                    title = if (currentDraft.category in CREATIVE_CATEGORIES) currentDraft.title else version.title,
                    text = version.prompt_text
                )
                editorMode = true
            },
            onActivate = activateAction,
            onDraftChange = { draft = it },
            onEdit = { editorMode = true },
            onUseAsBase = {
                useEntryAsBase(
                    LibraryPrompt(
                        id = currentDraft.id.orEmpty(),
                        title = currentDraft.title,
                        text = currentDraft.text,
                        category = currentDraft.category,
                        source = currentDraft.source,
                        version = currentDraft.version,
                        genre = currentDraft.genre,
                        format = currentDraft.format,
                        visualMedium = currentDraft.visualMedium
                    )
                )
            },
            onSave = { saveDraft() },
            onCancelEdit = {
                if (currentDraft.isNew) {
                    closeEditor()
                } else {
                    draft = originalDraft
                    editorMode = false
                }
            },
            onDelete = { showDeleteDialog = true },
            onDismiss = { requestCloseEditor() }
        )
    }

    if (showDiscardDialog) {
        ConfirmationDialog(
            title = "Discard changes?",
            message = "Your edits to this prompt have not been saved.",
            confirmText = "Discard",
            isDestructive = true,
            onConfirm = {
                showDiscardDialog = false
                closeEditor()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    if (showDeleteDialog) {
        val isCreativeOverride = currentCategory in CREATIVE_CATEGORIES
        ConfirmationDialog(
            title = if (isCreativeOverride) "Remove custom override?" else "Delete custom prompt?",
            message = if (isCreativeOverride) {
                "The built-in prompt will be used again for this ${if (currentCategory == "GENRE") "genre" else "visual medium"}."
            } else {
                "This saved prompt will be permanently deleted."
            },
            confirmText = if (isCreativeOverride) "Remove" else "Delete",
            isDestructive = true,
            onConfirm = { deleteDraft() },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun ActiveGenreSummary(
    genres: List<String>,
    entries: List<LibraryPrompt>,
    onOpen: (LibraryPrompt) -> Unit
) {
    StandardCard {
        SectionHeader(title = "AI will use for this project")
        if (genres.isEmpty()) {
            Text(
                text = "No genres selected yet. Add a genre below or choose one in Project Settings.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            genres.forEach { genre ->
                val entry = entries.firstOrNull {
                    normalizePromptKey(it.title) == normalizePromptKey(genre) ||
                        normalizePromptKey(it.genre) == normalizePromptKey(genre)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(genre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = entry?.let { sourceLabel(it.source) } ?: "NOT FOUND",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry != null) {
                        TextButton(onClick = { onOpen(entry) }) {
                            Text("View")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveMediumSummary(
    medium: String,
    entries: List<LibraryPrompt>,
    onOpen: (LibraryPrompt) -> Unit
) {
    StandardCard {
        SectionHeader(title = "AI will use for this project")
        val entry = entries.firstOrNull {
            normalizePromptKey(it.visualMedium) == normalizePromptKey(medium) ||
                normalizePromptKey(it.title) == normalizePromptKey(medium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(medium.ifBlank { "ANIME" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = entry?.let { sourceLabel(it.source) } ?: "NOT FOUND",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry != null) {
                TextButton(onClick = { onOpen(entry) }) {
                    Text("View")
                }
            }
        }
    }
}

@Composable
private fun PromptLibraryCard(
    entry: LibraryPrompt,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    StandardCard(
        onClick = onOpen,
        modifier = Modifier.testTag(TestTags.item(TestTags.PromptStudio.CARD_PREFIX, entry.id))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(label = sourceLabel(entry.source))
                    if (entry.isActive) StatusChip(label = "ACTIVE")
                    Text(
                        text = "v${entry.version}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val meta = promptMeta(entry)
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = entry.text,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.source == PromptSource.CUSTOM) {
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.testTag(TestTags.item(TestTags.PromptStudio.FAVORITE_PREFIX, entry.id))
                ) {
                    Text(
                        text = if (entry.isFavorite) "★" else "☆",
                        fontSize = 20.sp,
                        color = StudioTokens.Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltersSheet(
    sheetState: SheetState,
    hasGenreFilter: Boolean,
    hasFormatFilter: Boolean,
    hasMediumFilter: Boolean,
    selectedGenre: String?,
    selectedFormat: String?,
    selectedMedium: String?,
    genres: List<String>,
    formats: List<String>,
    media: List<String>,
    onGenreSelected: (String?) -> Unit,
    onFormatSelected: (String?) -> Unit,
    onMediumSelected: (String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Filter prompts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = "Only filters that matter for this tab are shown here.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasGenreFilter) {
                DropdownField(
                    label = "Genre",
                    value = selectedGenre,
                    options = genres,
                    emptyLabel = "Any",
                    onSelect = onGenreSelected,
                    testTag = TestTags.PromptStudio.GENRE
                )
            }
            if (hasFormatFilter) {
                DropdownField(
                    label = "Format",
                    value = selectedFormat,
                    options = formats,
                    emptyLabel = "Any",
                    onSelect = onFormatSelected,
                    testTag = TestTags.PromptStudio.FORMAT
                )
            }
            if (hasMediumFilter) {
                DropdownField(
                    label = "Medium",
                    value = selectedMedium,
                    options = media,
                    emptyLabel = "Any",
                    onSelect = onMediumSelected,
                    testTag = TestTags.PromptStudio.MEDIUM
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "Clear all",
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Done",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AddPromptSheet(
    sheetState: SheetState,
    category: String,
    name: String,
    text: String,
    baseOptions: List<LibraryPrompt>,
    selectedBaseId: String?,
    genre: String?,
    format: String?,
    medium: String?,
    scope: PromptScope,
    genres: List<String>,
    formats: List<String>,
    media: List<String>,
    error: String?,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onBaseSelected: (String?) -> Unit,
    onGenreChange: (String?) -> Unit,
    onFormatChange: (String?) -> Unit,
    onMediumChange: (String?) -> Unit,
    onScopeChange: (PromptScope) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (category) {
        "GENRE" -> "New genre"
        "VISUAL_MEDIUM" -> "New visual medium"
        else -> "New $category prompt"
    }
    val nameLabel = when (category) {
        "GENRE" -> "Genre name"
        "VISUAL_MEDIUM" -> "Visual medium name"
        else -> "Prompt title"
    }
    val selectedBase = baseOptions.firstOrNull { it.id == selectedBaseId }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(nameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            DropdownField(
                label = "Start from",
                value = selectedBase?.title,
                options = baseOptions.map { it.title },
                emptyLabel = "Blank",
                onSelect = { selectedTitle ->
                    onBaseSelected(baseOptions.firstOrNull { it.title == selectedTitle }?.id)
                }
            )
            if (category == "BIBLE" || category == "EPISODE" || category == "IMAGE") {
                DropdownField(
                    label = "Genre tag",
                    value = genre,
                    options = genres,
                    emptyLabel = "Any",
                    onSelect = onGenreChange
                )
            }
            if (category == "BIBLE" || category == "EPISODE") {
                DropdownField(
                    label = "Format tag",
                    value = format,
                    options = formats,
                    emptyLabel = "Any",
                    onSelect = onFormatChange
                )
            }
            if (category == "IMAGE") {
                DropdownField(
                    label = "Medium tag",
                    value = medium,
                    options = media,
                    emptyLabel = "Any",
                    onSelect = onMediumChange
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("Prompt instructions") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            ScopeSelector(scope = scope, onScopeChange = onScopeChange)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = if (isSaving) "Saving..." else "Save",
                    onClick = onSave,
                    enabled = !isSaving && name.isNotBlank() && text.isNotBlank(),
                    isLoading = isSaving,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PromptEditorSheet(
    sheetState: SheetState,
    draft: PromptDraft,
    editMode: Boolean,
    isSaving: Boolean,
    canSave: Boolean,
    versions: List<PromptVersionDto>,
    isLoadingVersions: Boolean,
    versionsError: String?,
    isActivating: Boolean,
    onRetryVersions: () -> Unit,
    onUseVersion: (PromptVersionDto) -> Unit,
    onActivate: (() -> Unit)?,
    onDraftChange: (PromptDraft) -> Unit,
    onEdit: () -> Unit,
    onUseAsBase: () -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(draft.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(label = sourceLabel(draft.source))
                        if (draft.isActive) StatusChip(label = "ACTIVE")
                        Text("v${draft.version}", fontSize = 11.sp)
                    }
                }
                if (!editMode && draft.source == PromptSource.CUSTOM) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit prompt")
                    }
                }
            }

            val meta = promptMeta(
                LibraryPrompt(
                    id = draft.id.orEmpty(),
                    title = draft.title,
                    text = draft.text,
                    category = draft.category,
                    source = draft.source,
                    version = draft.version,
                    genre = draft.genre,
                    format = draft.format,
                    visualMedium = draft.visualMedium
                )
            )
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = draft.title,
                onValueChange = { if (editMode) onDraftChange(draft.copy(title = it)) },
                readOnly = !editMode,
                label = { Text("Prompt title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PromptStudio.TITLE_FIELD)
            )

            if (draft.source == PromptSource.APP_DEFAULT) {
                Text(
                    text = "This is the application starter text. Save it as a custom prompt before editing your own version.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = draft.text,
                onValueChange = { if (editMode) onDraftChange(draft.copy(text = it)) },
                readOnly = !editMode,
                label = { Text("System prompt instructions") },
                minLines = 9,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PromptStudio.TEXT_FIELD)
            )

            if (editMode && draft.isNew) {
                ScopeSelector(
                    scope = draft.scope,
                    onScopeChange = { onDraftChange(draft.copy(scope = it)) }
                )
            }

            if (draft.source == PromptSource.CUSTOM && draft.id != null) {
                VersionHistorySection(
                    versions = versions,
                    isLoading = isLoadingVersions,
                    error = versionsError,
                    onRetry = onRetryVersions,
                    onUseVersion = onUseVersion
                )
            }

            if (!editMode) {
                when (draft.source) {
                    PromptSource.BUILT_IN, PromptSource.APP_DEFAULT -> {
                        PrimaryButton(
                            text = "Use as base for a custom prompt",
                            onClick = onUseAsBase,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    PromptSource.CUSTOM -> {
                        if (onActivate != null) {
                            PrimaryButton(
                                text = if (isActivating) "Activating..." else "Set as active for Story Bible",
                                onClick = onActivate,
                                enabled = !isActivating,
                                isLoading = isActivating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.PromptStudio.ACTIVATE)
                            )
                        }
                        PrimaryButton(
                            text = "Edit prompt",
                            onClick = onEdit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.PromptStudio.EDIT)
                        )
                        SecondaryButton(
                            text = if (draft.category == "GENRE" || draft.category == "VISUAL_MEDIUM") {
                                "Remove custom override"
                            } else {
                                "Delete custom prompt"
                            },
                            onClick = onDelete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.PromptStudio.RESET)
                        )
                    }
                }
                SecondaryButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onCancelEdit,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = if (isSaving) "Saving..." else if (draft.isNew) "Save as custom" else "Update prompt",
                        onClick = onSave,
                        enabled = canSave && !isSaving,
                        isLoading = isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(TestTags.PromptStudio.SAVE)
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionHistorySection(
    versions: List<PromptVersionDto>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onUseVersion: (PromptVersionDto) -> Unit
) {
    var expandedVersionId by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.PromptStudio.VERSIONS),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider()
        Text("Version history", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        when {
            isLoading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Loading versions...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            error != null -> {
                Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                SecondaryButton(text = "Retry versions", onClick = onRetry)
            }
            versions.isEmpty() -> {
                Text(
                    text = "No earlier versions yet. A snapshot is created when this prompt is updated.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                versions.take(20).forEach { version ->
                    val isExpanded = expandedVersionId == version.id
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("v${version.version}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    text = version.created_at.orEmpty().replace("T", " ").take(16),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = {
                                expandedVersionId = if (isExpanded) null else version.id
                            }) {
                                Text(if (isExpanded) "Hide" else "Preview")
                            }
                            TextButton(onClick = { onUseVersion(version) }) {
                                Text("Use")
                            }
                        }
                        if (isExpanded) {
                            Text(
                                text = version.prompt_text,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeSelector(
    scope: PromptScope,
    onScopeChange: (PromptScope) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Save scope", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = scope == PromptScope.PROJECT,
                onClick = { onScopeChange(PromptScope.PROJECT) },
                label = { Text("This project") }
            )
            FilterChip(
                selected = scope == PromptScope.GLOBAL,
                onClick = { onScopeChange(PromptScope.GLOBAL) },
                label = { Text("Global") }
            )
        }
        Text(
            text = "Project prompts apply only to the active project. Global prompts are available to every project.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String?,
    options: List<String>,
    emptyLabel: String,
    onSelect: (String?) -> Unit,
    testTag: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "$label: ${value?.takeIf { it.isNotBlank() } ?: emptyLabel}",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(emptyLabel) },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
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
