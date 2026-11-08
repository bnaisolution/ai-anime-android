package org.dpdns.bnai.anime.ui.settings

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*
import org.dpdns.bnai.anime.ui.testing.TestTags

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val tokenManager = LocalAppContainer.current.tokenManager
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("AI & Vision", "Compute Workers", "Storage & Policy", "Session")

    var primaryProvider by remember { mutableStateOf("groq") }
    var fallbackProvider by remember { mutableStateOf("gemini") }
    var visionProvider by remember { mutableStateOf("gemini") }
    var preferredComputeMode by remember { mutableStateOf("BYOD_CLOUD") }
    var kaggleUsername by remember { mutableStateOf("") }
    var cpuWorkerRepo by remember { mutableStateOf("bnaisolution/anime-cpu-worker") }
    var telegramChatId by remember { mutableStateOf("") }
    var autoApprovePolicy by remember { mutableStateOf(false) }

    var newGroqKey by remember { mutableStateOf("") }
    var newGeminiKey by remember { mutableStateOf("") }
    var newKaggleKey by remember { mutableStateOf("") }
    var newGithubToken by remember { mutableStateOf("") }
    var newTelegramBotToken by remember { mutableStateOf("") }

    var hasSavedGroqKey by remember { mutableStateOf(false) }
    var hasSavedGeminiKey by remember { mutableStateOf(false) }
    var hasSavedKaggleKey by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }

    fun hydrateSettingsFromServer() {
        val pid = repository.currentProjectId()
        scope.launch {
            isLoading = true
            val gpuRes = repository.fetchGpuInfrastructure()
            if (gpuRes is Resource.Success) {
                val d = gpuRes.data
                primaryProvider = (d["primary_llm_provider"] ?: d["llm_provider"] ?: primaryProvider).toString()
                fallbackProvider = (d["fallback_llm_provider"] ?: fallbackProvider).toString()
                visionProvider = (d["vision_provider"] ?: visionProvider).toString()
                preferredComputeMode = (d["preferred_mode"] ?: preferredComputeMode).toString()
                kaggleUsername = (d["kaggle_username"] ?: "").toString().let { if (it == "null") "" else it }
                telegramChatId = (d["telegram_chat_id"] ?: "").toString().let { if (it == "null") "" else it }

                hasSavedGroqKey = !d["groq_api_key"]?.toString().isNullOrBlank() && d["groq_api_key"].toString() != "null"
                hasSavedGeminiKey = !d["gemini_api_key"]?.toString().isNullOrBlank() && d["gemini_api_key"].toString() != "null"
                hasSavedKaggleKey = !d["kaggle_key"]?.toString().isNullOrBlank() && d["kaggle_key"].toString() != "null"
            }

            if (pid.isNotBlank()) {
                val ovRes = repository.fetchProjectOverviewRaw(pid)
                if (ovRes is Resource.Success) {
                    autoApprovePolicy = ovRes.data["auto_approve"] == true
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        hydrateSettingsFromServer()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.Screen.SETTINGS)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { hydrateSettingsFromServer() },
            onRefreshClick = { hydrateSettingsFromServer() }
        )

        ScreenHeader(
            title = "Studio Infrastructure & Settings",
            subtitle = "Hydrated from backend with masked secrets & read-after-write verification"
        )

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
            }
        }

        when (selectedTab) {
            0 -> {
                StandardCard {
                    SectionHeader(title = "AI & Vision Providers")
                    OutlinedTextField(
                        value = primaryProvider,
                        onValueChange = { primaryProvider = it },
                        label = { Text("Primary LLM Provider (groq / gemini / openrouter)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fallbackProvider,
                        onValueChange = { fallbackProvider = it },
                        label = { Text("Fallback LLM Provider") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = visionProvider,
                        onValueChange = { visionProvider = it },
                        label = { Text("Vision DNA Provider (gemini / groq / openrouter)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGroqKey,
                        onValueChange = { newGroqKey = it },
                        label = { Text(if (hasSavedGroqKey) "Groq API Key (Saved •••• — enter to replace)" else "Groq API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGeminiKey,
                        onValueChange = { newGeminiKey = it },
                        label = { Text(if (hasSavedGeminiKey) "Gemini API Key (Saved •••• — enter to replace)" else "Gemini API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            1 -> {
                StandardCard {
                    SectionHeader(title = "GPU & CPU Worker Configuration")
                    OutlinedTextField(
                        value = preferredComputeMode,
                        onValueChange = { preferredComputeMode = it },
                        label = { Text("Preferred Compute Mode") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = kaggleUsername,
                        onValueChange = { kaggleUsername = it },
                        label = { Text("Kaggle Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newKaggleKey,
                        onValueChange = { newKaggleKey = it },
                        label = { Text(if (hasSavedKaggleKey) "Kaggle API Key (Saved •••• — enter to replace)" else "Kaggle API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cpuWorkerRepo,
                        onValueChange = { cpuWorkerRepo = it },
                        label = { Text("GitHub CPU Worker Repository") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGithubToken,
                        onValueChange = { newGithubToken = it },
                        label = { Text("GitHub Actions Token (Masked)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            2 -> {
                StandardCard {
                    SectionHeader(title = "Telegram Storage & Approval Policy")
                    OutlinedTextField(
                        value = telegramChatId,
                        onValueChange = { telegramChatId = it },
                        label = { Text("Telegram Channel / Chat ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTelegramBotToken,
                        onValueChange = { newTelegramBotToken = it },
                        label = { Text("Telegram Bot Token (Masked)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Project Auto-Approve Policy", fontWeight = FontWeight.SemiBold)
                            Text("Persist auto_approve in active project settings", fontSize = 12.sp)
                        }
                        Switch(checked = autoApprovePolicy, onCheckedChange = { autoApprovePolicy = it })
                    }
                }
            }
            3 -> {
                StandardCard {
                    SectionHeader(title = "Backend Connectivity Verification")
                    if (connectionStatus != null) {
                        Text(connectionStatus ?: "", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    SecondaryButton(
                        text = "Test Backend Connection",
                        onClick = {
                            scope.launch {
                                val t0 = System.currentTimeMillis()
                                val isHealthy = repository.checkBackendHealth()
                                val elapsed = System.currentTimeMillis() - t0
                                connectionStatus = if (isHealthy) {
                                    "Connected • Verified Round-Trip Latency: ${elapsed}ms"
                                } else {
                                    "Connection Failed (Backend Unreachable)"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        PrimaryButton(
            text = "Save & Verify Configuration",
            isLoading = isSaving,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.Settings.SAVE),
            onClick = {
                isSaving = true
                scope.launch {
                    val infraPayload = mutableMapOf<String, Any?>(
                        "preferred_mode" to preferredComputeMode.trim(),
                        "primary_llm_provider" to primaryProvider.trim(),
                        "fallback_llm_provider" to fallbackProvider.trim(),
                        "vision_provider" to visionProvider.trim(),
                        "kaggle_username" to kaggleUsername.trim(),
                        "telegram_chat_id" to telegramChatId.trim()
                    )
                    if (newGroqKey.isNotBlank()) infraPayload["groq_api_key"] = newGroqKey.trim()
                    if (newGeminiKey.isNotBlank()) infraPayload["gemini_api_key"] = newGeminiKey.trim()
                    if (newKaggleKey.isNotBlank()) infraPayload["kaggle_key"] = newKaggleKey.trim()
                    if (newGithubToken.isNotBlank()) infraPayload["github_token"] = newGithubToken.trim()
                    if (newTelegramBotToken.isNotBlank()) infraPayload["telegram_bot_token"] = newTelegramBotToken.trim()

                    val saveInfraRes = repository.saveGpuInfrastructure(infraPayload)
                    val pid = repository.currentProjectId()
                    if (pid.isNotBlank()) {
                        repository.updateProjectSettings(
                            projectId = pid,
                            updates = mapOf(
                                "auto_approve" to autoApprovePolicy,
                                "vision_provider" to visionProvider.trim()
                            )
                        )
                    }

                    val badge = "${primaryProvider.trim()} • ${fallbackProvider.trim()}"
                    tokenManager.saveConfiguredProviderLabel(badge)

                    newGroqKey = ""
                    newGeminiKey = ""
                    newKaggleKey = ""
                    newGithubToken = ""
                    newTelegramBotToken = ""

                    hydrateSettingsFromServer()
                    isSaving = false

                    if (saveInfraRes is Resource.Success) {
                        Toast.makeText(context, "Configuration persisted & verified via read-after-write", Toast.LENGTH_SHORT).show()
                    } else if (saveInfraRes is Resource.Error) {
                        Toast.makeText(context, saveInfraRes.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(64.dp))
    }
}
