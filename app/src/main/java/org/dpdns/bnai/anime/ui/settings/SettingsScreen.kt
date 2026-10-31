package org.dpdns.bnai.anime.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.CustomGpuConfigDto
import org.dpdns.bnai.anime.core.network.GpuSetupRequest
import org.dpdns.bnai.anime.core.network.KaggleAccountConfigDto
import org.dpdns.bnai.anime.core.network.RetrofitClient
import org.dpdns.bnai.anime.core.network.TelegramStorageConfigDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPromptStudio: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val api = remember { RetrofitClient.getApiService() }

    var darkModeEnabled by rememberSaveable { mutableStateOf(true) }
    var telegramAlertsEnabled by rememberSaveable { mutableStateOf(true) }

    // GPU & Kaggle Dual-Account State
    var preferredGpuMode by rememberSaveable { mutableStateOf("KAGGLE_AUTO") }
    var modelsDatasetSlug by rememberSaveable { mutableStateOf("bnaisolution/anime-engine-models-v1") }
    var autoDeployKaggle by rememberSaveable { mutableStateOf(true) }

    var kaggle1Username by rememberSaveable { mutableStateOf("") }
    var kaggle1ApiKey by rememberSaveable { mutableStateOf("") }
    var kaggle2Username by rememberSaveable { mutableStateOf("") }
    var kaggle2ApiKey by rememberSaveable { mutableStateOf("") }

    var customGpuUrl by rememberSaveable { mutableStateOf("") }
    var customGpuApiKey by rememberSaveable { mutableStateOf("") }

    var telegramBotToken by rememberSaveable { mutableStateOf("") }
    var telegramStorageChatId by rememberSaveable { mutableStateOf("") }
    var githubWorkerUrl by rememberSaveable { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GPU, Prompts & Settings (S18)",
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- NAVIGATION TO PROMPT STUDIO ---
            Button(
                onClick = onOpenPromptStudio,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Dynamic System Prompt Studio (Anime / Real Human)")
            }

            // --- DYNAMIC GPU & DUAL-KAGGLE CONFIGURATION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Dynamic GPU Router (Zero-Colab)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = preferredGpuMode == "KAGGLE_AUTO",
                            onClick = { preferredGpuMode = "KAGGLE_AUTO" },
                            label = { Text("Dual Kaggle (60h Free)") }
                        )
                        FilterChip(
                            selected = preferredGpuMode == "CUSTOM_GPU",
                            onClick = { preferredGpuMode = "CUSTOM_GPU" },
                            label = { Text("Custom External GPU") }
                        )
                    }

                    if (preferredGpuMode == "KAGGLE_AUTO") {
                        OutlinedTextField(
                            value = modelsDatasetSlug,
                            onValueChange = { modelsDatasetSlug = it },
                            label = { Text("Pre-Downloaded Models Dataset Slug") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = kaggle1Username,
                            onValueChange = { kaggle1Username = it },
                            label = { Text("Kaggle Primary Username (Account #1)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = kaggle1ApiKey,
                            onValueChange = { kaggle1ApiKey = it },
                            label = { Text("Kaggle Primary API Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = kaggle2Username,
                            onValueChange = { kaggle2Username = it },
                            label = { Text("Kaggle Fallback Username (Account #2)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = kaggle2ApiKey,
                            onValueChange = { kaggle2ApiKey = it },
                            label = { Text("Kaggle Fallback API Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-Deploy LocalTunnel Notebook via API")
                            Switch(checked = autoDeployKaggle, onCheckedChange = { autoDeployKaggle = it })
                        }
                    } else {
                        OutlinedTextField(
                            value = customGpuUrl,
                            onValueChange = { customGpuUrl = it },
                            label = { Text("Custom GPU Worker URL (https://xxxx.loca.lt)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customGpuApiKey,
                            onValueChange = { customGpuApiKey = it },
                            label = { Text("Custom GPU Bearer Token (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = githubWorkerUrl,
                        onValueChange = { githubWorkerUrl = it },
                        label = { Text("GitHub CPU Worker URL (Svara TTS & FFmpeg)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = telegramBotToken,
                        onValueChange = { telegramBotToken = it },
                        label = { Text("Telegram Storage Bot Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = telegramStorageChatId,
                        onValueChange = { telegramStorageChatId = it },
                        label = { Text("Telegram Storage Channel/Chat ID") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                saveStatus = null
                                try {
                                    val req = GpuSetupRequest(
                                        preferred_mode = preferredGpuMode,
                                        models_dataset_slug = modelsDatasetSlug.trim(),
                                        auto_deploy_kaggle = autoDeployKaggle,
                                        custom_gpu = CustomGpuConfigDto(
                                            enabled = preferredGpuMode == "CUSTOM_GPU",
                                            worker_url = customGpuUrl.trim(),
                                            api_key = customGpuApiKey.trim().ifBlank { null }
                                        ),
                                        kaggle_primary = KaggleAccountConfigDto(
                                            username = kaggle1Username.trim(),
                                            api_key = kaggle1ApiKey.trim()
                                        ),
                                        kaggle_secondary = KaggleAccountConfigDto(
                                            username = kaggle2Username.trim(),
                                            api_key = kaggle2ApiKey.trim()
                                        ),
                                        telegram_storage = TelegramStorageConfigDto(
                                            bot_token = telegramBotToken.trim(),
                                            storage_chat_id = telegramStorageChatId.trim()
                                        ),
                                        github_worker_url = githubWorkerUrl.trim().ifBlank { null }
                                    )
                                    val res = api.saveGpuConfig(req)
                                    saveStatus = if (res.isSuccessful) {
                                        "✅ Saved! Kaggle LocalTunnel & GPU Routing active."
                                    } else {
                                        "❌ Failed to save GPU config (${res.code()})"
                                    }
                                } catch (e: Exception) {
                                    saveStatus = "❌ Error: ${e.localizedMessage}"
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSaving) "Deploying & Saving..." else "Save GPU & Telegram Storage Config")
                    }

                    saveStatus?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // --- EXISTING DARK MODE & ALERTS CARDS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", style = MaterialTheme.typography.headlineMedium)
                        Text("Use Deep Indigo & Dark #121212 studio palette", style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(checked = darkModeEnabled, onCheckedChange = { darkModeEnabled = it })
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Telegram Alerts", style = MaterialTheme.typography.headlineMedium)
                        Text("Receive WF-08 milestones & dead-letter incident alerts", style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(checked = telegramAlertsEnabled, onCheckedChange = { telegramAlertsEnabled = it })
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = "Daily Quota Limit: 5000 units (Dual-Kaggle 60h Enabled)",
                onValueChange = {},
                readOnly = true,
                label = { Text("WF-12 Quota Governor") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
