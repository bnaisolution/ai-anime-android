package org.dpdns.bnai.anime.ui.studio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*

private val CATEGORIES = listOf("BIBLE", "EPISODE", "IMAGE", "AI_DIRECTOR")

private val SYSTEM_DEFAULTS = mapOf(
    "BIBLE" to "You are the WF-01 Master Story Bible Architect. Build cohesive anime world rules, power scaling ceilings, and immutable character anchors.",
    "EPISODE" to "You are the WF-02 Episode Screenwriter. Write tight, high-retention multi-scene anime scripts strictly obeying locked Canon and character Visual DNA.",
    "IMAGE" to "Masterpiece cinematic anime keyframe, ultra-detailed cel shading, dynamic volumetric lighting, strict adherence to character 11-field Visual DNA.",
    "AI_DIRECTOR" to "You are the AI Anime Studio Director overseeing StoryWF-00..13. Provide actionable production guidance and never mutate state without user confirmation."
)

@Composable
fun PromptStudioScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var selectedCatIndex by remember { mutableStateOf(0) }
    val currentCategory = CATEGORIES[selectedCatIndex]

    // BUG-007 FIX: Independent state maps per category (no shared state leakage across tabs!)
    val titlesByCat = remember {
        mutableStateMapOf(
            "BIBLE" to "Default Bible Architect Prompt",
            "EPISODE" to "Default Episode Screenplay Prompt",
            "IMAGE" to "Default Visual Keyframe Prompt",
            "AI_DIRECTOR" to "Default AI Director System Prompt"
        )
    }
    val textByCat = remember {
        mutableStateMapOf(
            "BIBLE" to (SYSTEM_DEFAULTS["BIBLE"] ?: ""),
            "EPISODE" to (SYSTEM_DEFAULTS["EPISODE"] ?: ""),
            "IMAGE" to (SYSTEM_DEFAULTS["IMAGE"] ?: ""),
            "AI_DIRECTOR" to (SYSTEM_DEFAULTS["AI_DIRECTOR"] ?: "")
        )
    }
    val baselineByCat = remember {
        mutableStateMapOf(
            "BIBLE" to (SYSTEM_DEFAULTS["BIBLE"] ?: ""),
            "EPISODE" to (SYSTEM_DEFAULTS["EPISODE"] ?: ""),
            "IMAGE" to (SYSTEM_DEFAULTS["IMAGE"] ?: ""),
            "AI_DIRECTOR" to (SYSTEM_DEFAULTS["AI_DIRECTOR"] ?: "")
        )
    }
    val editModeByCat = remember {
        mutableStateMapOf(
            "BIBLE" to false,
            "EPISODE" to false,
            "IMAGE" to false,
            "AI_DIRECTOR" to false
        )
    }

    var savedPrompts by remember { mutableStateOf<List<SystemPromptDto>>(emptyList()) }
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
                    CATEGORIES.forEach { cat ->
                        val match = res.data.firstOrNull { (it.prompt_category ?: "BIBLE").uppercase() == cat }
                        if (match != null && baselineByCat[cat] == SYSTEM_DEFAULTS[cat]) {
                            titlesByCat[cat] = match.title
                            textByCat[cat] = match.prompt_text
                            baselineByCat[cat] = match.prompt_text
                        }
                    }
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPromptsForProject()
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
                val defText = SYSTEM_DEFAULTS[currentCategory] ?: ""
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
            CATEGORIES.forEachIndexed { idx, cat ->
                val dirty = (textByCat[cat] ?: "") != (baselineByCat[cat] ?: "")
                Tab(
                    selected = selectedCatIndex == idx,
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

        // BUG-008: Explicit View Mode vs Edit Mode controls
        StandardCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "$currentCategory Prompt Configuration")
                if (!isEditMode) {
                    OutlinedButton(onClick = { editModeByCat[currentCategory] = true }) {
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
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = currentText,
                onValueChange = { if (isEditMode) textByCat[currentCategory] = it },
                readOnly = !isEditMode,
                label = { Text("System Prompt Instructions ($currentCategory)") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
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
                        val pid = repository.currentProjectId()
                        isSaving = true
                        scope.launch {
                            val res = repository.saveSystemPrompt(
                                projectId = pid,
                                title = currentTitle.trim().ifBlank { "$currentCategory Prompt" },
                                promptText = currentText.trim(),
                                visualMedium = "ANIME",
                                promptCategory = currentCategory
                            )
                            isSaving = false
                            if (res is Resource.Success) {
                                baselineByCat[currentCategory] = currentText.trim()
                                editModeByCat[currentCategory] = false
                                loadPromptsForProject()
                                Toast.makeText(context, "$currentCategory prompt saved", Toast.LENGTH_SHORT).show()
                            } else if (res is Resource.Error) {
                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset $currentCategory to Application Default")
            }
        }

        // Saved Prompts Library for Current Category
        val categorySavedList = savedPrompts.filter {
            (it.prompt_category ?: "BIBLE").uppercase() == currentCategory
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
                    onClick = {
                        titlesByCat[currentCategory] = item.title
                        textByCat[currentCategory] = item.prompt_text
                        baselineByCat[currentCategory] = item.prompt_text
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
                                text = item.prompt_text,
                                maxLines = 2,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            val pid = item.id ?: return@IconButton
                            scope.launch {
                                repository.deleteSystemPrompt(pid)
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
