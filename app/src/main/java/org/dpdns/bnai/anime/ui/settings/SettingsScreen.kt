package org.dpdns.bnai.anime.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.CustomGpuConfigDto
import org.dpdns.bnai.anime.core.network.GpuSetupRequest
import org.dpdns.bnai.anime.core.network.KaggleAccountConfigDto
import org.dpdns.bnai.anime.core.network.RetrofitClient
import org.dpdns.bnai.anime.core.network.TelegramStorageConfigDto

@Composable
fun SettingsScreen() {
    val api = remember { RetrofitClient.getApiService() }
    val scope = rememberCoroutineScope()

    var preferredMode by remember { mutableStateOf("KAGGLE_AUTO") }
    var datasetSlug by remember { mutableStateOf("bnaisolution/anime-engine-models-v1") }
    var kaggle1User by remember { mutableStateOf("") }
    var kaggle1Key by remember { mutableStateOf("") }
    var kaggle1Slug by remember { mutableStateOf("anime-gpu-worker-primary") }
    var kaggle2User by remember { mutableStateOf("") }
    var kaggle2Key by remember { mutableStateOf("") }
    var kaggle2Slug by remember { mutableStateOf("anime-gpu-worker-secondary") }
    var customGpuUrl by remember { mutableStateOf("") }
    var telegramBotToken by remember { mutableStateOf("") }
    var telegramChatId by remember { mutableStateOf("") }
    var githubWorkerUrl by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val res = api.getGpuConfig()
            if (res.isSuccessful) {
                val cfg = res.body()?.data.orEmpty()
                preferredMode = (cfg["preferred_mode"] ?: "KAGGLE_AUTO").toString()
                datasetSlug = (cfg["models_dataset_slug"] ?: datasetSlug).toString()
                statusMsg = "Loaded existing GPU routing configuration."
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GPU, Kaggle & Storage Settings", style = MaterialTheme.typography.headlineLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("KAGGLE_AUTO", "CUSTOM_GPU").forEach { mode ->
                FilterChip(
                    selected = preferredMode == mode,
                    onClick = { preferredMode = mode },
                    label = { Text(mode) }
                )
            }
        }

        OutlinedTextField(
            value = datasetSlug,
            onValueChange = { datasetSlug = it },
            label = { Text("Models Dataset Slug") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Primary Kaggle Account", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = kaggle1User,
            onValueChange = { kaggle1User = it },
            label = { Text("Kaggle Primary Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = kaggle1Key,
            onValueChange = { kaggle1Key = it },
            label = { Text("Kaggle Primary API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = kaggle1Slug,
            onValueChange = { kaggle1Slug = it },
            label = { Text("Primary Kernel Slug") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Secondary Kaggle Account (Optional)", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = kaggle2User,
            onValueChange = { kaggle2User = it },
            label = { Text("Kaggle Secondary Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = kaggle2Key,
            onValueChange = { kaggle2Key = it },
            label = { Text("Kaggle Secondary API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = kaggle2Slug,
            onValueChange = { kaggle2Slug = it },
            label = { Text("Secondary Kernel Slug") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Custom GPU & Telegram Storage Bridge", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = customGpuUrl,
            onValueChange = { customGpuUrl = it },
            label = { Text("Custom GPU Worker URL") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = telegramBotToken,
            onValueChange = { telegramBotToken = it },
            label = { Text("Telegram Bot Token") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = telegramChatId,
            onValueChange = { telegramChatId = it },
            label = { Text("Telegram Storage Chat ID") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = githubWorkerUrl,
            onValueChange = { githubWorkerUrl = it },
            label = { Text("CPU / GitHub Worker URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                isSaving = true
                statusMsg = null
                scope.launch {
                    try {
                        val req = GpuSetupRequest(
                            preferred_mode = preferredMode,
                            models_dataset_slug = datasetSlug.trim(),
                            auto_deploy_kaggle = true,
                            custom_gpu = CustomGpuConfigDto(
                                enabled = preferredMode == "CUSTOM_GPU",
                                worker_url = customGpuUrl.trim()
                            ),
                            kaggle_primary = KaggleAccountConfigDto(
                                kaggle1User.trim(), kaggle1Key.trim(), kaggle1Slug.trim()
                            ),
                            kaggle_secondary = KaggleAccountConfigDto(
                                kaggle2User.trim(), kaggle2Key.trim(), kaggle2Slug.trim()
                            ),
                            telegram_storage = TelegramStorageConfigDto(
                                telegramBotToken.trim(), telegramChatId.trim()
                            ),
                            github_worker_url = githubWorkerUrl.trim().ifBlank { null }
                        )
                        val res = api.saveGpuConfig(req)
                        statusMsg = if (res.isSuccessful) {
                            "Saved GPU & Storage configuration (Mode: ${res.body()?.data?.preferred_mode ?: preferredMode})"
                        } else {
                            "Save failed (HTTP ${res.code()})"
                        }
                    } catch (e: Exception) {
                        statusMsg = "Error: ${e.localizedMessage}"
                    }
                    isSaving = false
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isSaving) "Saving & Deploying..." else "Save GPU & Storage Config")
        }

        statusMsg?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
    }
}
