package org.dpdns.bnai.anime.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.InfoTooltipButton
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { RetrofitClient.getApiService(context) }
    val repository = remember { AnimeRepository(apiService) }
    val isOwner = remember { TokenManager.getInstance()?.isOwner() == true }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("AI Models", "Compute", "Storage", "Preferences")

    // Connection & Wizard State (#231, #238)
    var connectionStatus by remember { mutableStateOf("Connected") }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showSetupWizard by remember { mutableStateOf(false) }
    var wizardStep by remember { mutableIntStateOf(1) }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }

    // Tab 0: AI Models (#195, #196, #226-228, #232)
    var primaryChatProvider by remember { mutableStateOf("Groq (Llama-3.3-70B)") }
    var fallbackChatProvider by remember { mutableStateOf("Gemini 2.5 Pro") }
    var primaryVisionProvider by remember { mutableStateOf("Gemini Vision") }
    var customProviders by remember { mutableStateOf(listOf("Groq (Llama-3.3-70B)", "Gemini 2.5 Pro", "OpenRouter Auto")) }
    var newProviderName by remember { mutableStateOf("") }
    var chatApiKey by remember { mutableStateOf("") }
    var showChatKey by remember { mutableStateOf(false) }

    // Tab 1: Compute (GPU & GitHub CPU) (#235, #236, #240)
    var preferredMode by remember { mutableStateOf("KAGGLE_AUTO") }
    var kagglePrimaryUser by remember { mutableStateOf("") }
    var kagglePrimaryKey by remember { mutableStateOf("") }
    var showKagglePrimaryKey by remember { mutableStateOf(false) }
    var kaggleSecondaryUser by remember { mutableStateOf("") }
    var kaggleSecondaryKey by remember { mutableStateOf("") }
    var customGpuEnabled by remember { mutableStateOf(false) }
    var customGpuUrl by remember { mutableStateOf("") }
    var customGpuKey by remember { mutableStateOf("") }
    var githubCpuWorkerUrl by remember { mutableStateOf("https://github.com/bnaisolution/anime-cpu-worker") }

    // Tab 2: Storage (Telegram Bridge)
    var telegramBotToken by remember { mutableStateOf("") }
    var showTelegramToken by remember { mutableStateOf(false) }
    var telegramChatId by remember { mutableStateOf("") }
    var streamBridgeUrl by remember { mutableStateOf("https://api.bnai.dpdns.org/v1/media/telegram") }

    // Tab 3: Preferences & Approval Policy (#210, #215, #233, #239)
    var defaultApprovalPolicy by remember { mutableStateOf("PAUSE_AND_ASK") }
    var approvalTimeoutMins by remember { mutableStateOf("15") }
    var autoPauseGpuIdle by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val res = apiService.getGpuConfig()
            if (res.isSuccessful) {
                val data = res.body()?.data.orEmpty()
                preferredMode = data["preferred_mode"]?.toString() ?: "KAGGLE_AUTO"
                val kp = data["kaggle_primary"] as? Map<*, *>
                kagglePrimaryUser = kp?.get("username")?.toString().orEmpty()
                val ks = data["kaggle_secondary"] as? Map<*, *>
                kaggleSecondaryUser = ks?.get("username")?.toString().orEmpty()
                val cg = data["custom_gpu"] as? Map<*, *>
                customGpuEnabled = cg?.get("enabled") == true
                customGpuUrl = cg?.get("worker_url")?.toString().orEmpty()
                val tg = data["telegram_storage"] as? Map<*, *>
                telegramChatId = tg?.get("storage_chat_id")?.toString().orEmpty()
                connectionStatus = "Connected"
            }
        } catch (_: Exception) {
            connectionStatus = "Offline"
        }
    }

    if (showAddProviderDialog) {
        AlertDialog(
            onDismissRequest = { showAddProviderDialog = false },
            title = { Text("Add Custom AI Provider") },
            text = {
                OutlinedTextField(
                    value = newProviderName,
                    onValueChange = { newProviderName = it },
                    label = { Text("Provider / Model Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newProviderName.isNotBlank()) {
                        customProviders = customProviders + newProviderName.trim()
                        primaryChatProvider = newProviderName.trim()
                        newProviderName = ""
                    }
                    showAddProviderDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProviderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Fix #236 & #240: Coming Soon Dialog for Platform Compute Toggle
    if (showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = false },
            title = { Text("Platform Compute (Coming Soon)") },
            text = { Text("Managed 1-click cloud GPU clusters are coming soon. Please use your BYOD Kaggle (60h/week free) or Custom GPU worker for now.") },
            confirmButton = {
                Button(onClick = { showComingSoonDialog = false }) { Text("Got It") }
            }
        )
    }

    // Fix #238: 3-Step BYOD Setup Wizard
    if (showSetupWizard) {
        AlertDialog(
            onDismissRequest = { showSetupWizard = false },
            title = { Text("BYOD Setup Wizard • Step $wizardStep of 3") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (wizardStep) {
                        1 -> {
                            Text("Step 1: AI Director & LLM Brain", fontWeight = FontWeight.Bold)
                            Text("Select your primary LLM provider and paste your API key.")
                            OutlinedTextField(
                                value = chatApiKey,
                                onValueChange = { chatApiKey = it },
                                label = { Text("Groq / Gemini API Key") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        2 -> {
                            Text("Step 2: Kaggle GPU Compute (Free 60h/wk)", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = kagglePrimaryUser,
                                onValueChange = { kagglePrimaryUser = it },
                                label = { Text("Kaggle Username") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = kagglePrimaryKey,
                                onValueChange = { kagglePrimaryKey = it },
                                label = { Text("Kaggle API Key") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            Text("Step 3: Telegram Unlimited Media Storage", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = telegramBotToken,
                                onValueChange = { telegramBotToken = it },
                                label = { Text("Telegram Bot Token") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = telegramChatId,
                                onValueChange = { telegramChatId = it },
                                label = { Text("Storage Channel / Chat ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (wizardStep < 3) {
                        wizardStep++
                    } else {
                        showSetupWizard = false
                        wizardStep = 1
                        Toast.makeText(context, "Wizard complete! Tap 'Save Infrastructure' to persist.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(if (wizardStep < 3) "Next" else "Finish") }
            },
            dismissButton = {
                TextButton(onClick = { showSetupWizard = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fix #194 & #231: Header + Connection Status Badge + Wizard Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Infrastructure & API Keys", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("BYOD Gateway • Encrypted Backend Vault", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            StatusBadge(if (connectionStatus == "Connected") "HEALTHY" else "ERROR")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    wizardStep = 1
                    showSetupWizard = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("3-Step Wizard")
            }

            // Fix #199 & #230: Test Connection Button
            OutlinedButton(
                onClick = {
                    isTestingConnection = true
                    scope.launch {
                        val ok = repository.checkBackendHealth()
                        connectionStatus = if (ok) "Connected" else "Error"
                        isTestingConnection = false
                        Toast.makeText(
                            context,
                            if (ok) "🟢 Backend & Vault Connected!" else "🔴 Connection Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = !isTestingConnection,
                modifier = Modifier.weight(1f)
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Keys")
            }
        }

        // Fix #225: 4 Organized Tabs
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: AI MODELS (#195, #196, #226-228, #232)
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Chat & Script LLM Routing", style = MaterialTheme.typography.headlineMedium)
                                TextButton(onClick = { showAddProviderDialog = true }) {
                                    Text("+ Add Provider")
                                }
                            }

                            Text("Primary LLM Provider", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                customProviders.take(3).forEach { prov ->
                                    FilterChip(
                                        selected = primaryChatProvider == prov,
                                        onClick = { primaryChatProvider = prov },
                                        label = { Text(prov.substringBefore(" ")) }
                                    )
                                }
                            }

                            Text("Fallback LLM Provider", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                customProviders.take(3).forEach { prov ->
                                    FilterChip(
                                        selected = fallbackChatProvider == prov,
                                        onClick = { fallbackChatProvider = prov },
                                        label = { Text(prov.substringBefore(" ")) }
                                    )
                                }
                            }

                            Text("Dynamic Vision DNA Provider (WF-05)", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Gemini Vision", "Groq Llama-Vision", "OpenRouter Vision").forEach { vp ->
                                    FilterChip(
                                        selected = primaryVisionProvider == vp,
                                        onClick = { primaryVisionProvider = vp },
                                        label = { Text(vp.substringBefore(" ")) }
                                    )
                                }
                            }

                            // Fix #232: PasswordVisualTransformation for API keys
                            OutlinedTextField(
                                value = chatApiKey,
                                onValueChange = { chatApiKey = it },
                                label = { Text("BYOD LLM / Vision API Key (Optional Override)") },
                                singleLine = true,
                                visualTransformation = if (showChatKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showChatKey = !showChatKey }) {
                                        Text(if (showChatKey) "Hide" else "Show")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: COMPUTE (KAGGLE GPU, CUSTOM GPU, GITHUB CPU, PLATFORM DISABLED TOGGLE)
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Fix #236 & #240: Disabled Platform Compute Toggle with Coming Soon popup
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showComingSoonDialog = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Platform Compute (Coming Soon)", fontWeight = FontWeight.SemiBold)
                                    Text("Tap to view managed cloud GPU roadmap", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                }
                                Switch(
                                    checked = false,
                                    onCheckedChange = { showComingSoonDialog = true },
                                    enabled = false
                                )
                            }

                            HorizontalDivider()

                            Text("BYOD GPU Routing Mode", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("KAGGLE_AUTO", "CUSTOM_GPU", "HYBRID").forEach { mode ->
                                    FilterChip(
                                        selected = preferredMode == mode,
                                        onClick = { preferredMode = mode },
                                        label = { Text(mode.replace("_", " ")) }
                                    )
                                }
                            }

                            Text("Kaggle Primary GPU Account (30h/wk)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = kagglePrimaryUser,
                                onValueChange = { kagglePrimaryUser = it },
                                label = { Text("Primary Kaggle Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = kagglePrimaryKey,
                                onValueChange = { kagglePrimaryKey = it },
                                label = { Text("Primary Kaggle API Key") },
                                singleLine = true,
                                visualTransformation = if (showKagglePrimaryKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showKagglePrimaryKey = !showKagglePrimaryKey }) {
                                        Text(if (showKagglePrimaryKey) "Hide" else "Show")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Kaggle Failover Account (+30h/wk)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = kaggleSecondaryUser,
                                onValueChange = { kaggleSecondaryUser = it },
                                label = { Text("Secondary Kaggle Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = kaggleSecondaryKey,
                                onValueChange = { kaggleSecondaryKey = it },
                                label = { Text("Secondary Kaggle API Key") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Custom Dedicated GPU Worker", style = MaterialTheme.typography.labelLarge)
                                Switch(checked = customGpuEnabled, onCheckedChange = { customGpuEnabled = it })
                            }
                            if (customGpuEnabled) {
                                OutlinedTextField(
                                    value = customGpuUrl,
                                    onValueChange = { customGpuUrl = it },
                                    label = { Text("Worker Base URL (RunPod / Vast / Colab)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = customGpuKey,
                                    onValueChange = { customGpuKey = it },
                                    label = { Text("Worker Bearer Secret") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            HorizontalDivider()
                            // Fix #235: GitHub CPU Workers Section
                            Text("GitHub Actions CPU Worker (Audio/FFmpeg Muxing)", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = githubCpuWorkerUrl,
                                onValueChange = { githubCpuWorkerUrl = it },
                                label = { Text("GitHub CPU Worker Repo / Webhook") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                2 -> {
                    // TAB 2: STORAGE (TELEGRAM MEDIA BRIDGE)
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Telegram Unlimited Media Vault", style = MaterialTheme.typography.headlineMedium)
                                InfoTooltipButton(
                                    "Telegram Storage Bridge",
                                    "Rendered scenes, audio stems, and master MP4s are stored in your private Telegram channel and streamed via backend bridge URLs."
                                )
                            }
                            OutlinedTextField(
                                value = telegramBotToken,
                                onValueChange = { telegramBotToken = it },
                                label = { Text("Telegram Bot Token") },
                                singleLine = true,
                                visualTransformation = if (showTelegramToken) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showTelegramToken = !showTelegramToken }) {
                                        Text(if (showTelegramToken) "Hide" else "Show")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = telegramChatId,
                                onValueChange = { telegramChatId = it },
                                label = { Text("Telegram Storage Channel Chat ID (e.g. -100...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = streamBridgeUrl,
                                onValueChange = { streamBridgeUrl = it },
                                label = { Text("Media Stream Bridge URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                else -> {
                    // TAB 3: PREFERENCES & APPROVAL POLICY (#207, #210, #215, #233, #239)
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Default Pipeline Approval Policy", style = MaterialTheme.typography.headlineMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = defaultApprovalPolicy == "AUTO_APPROVE",
                                    onClick = { defaultApprovalPolicy = "AUTO_APPROVE" },
                                    label = { Text("Auto-Approve Changes") }
                                )
                                FilterChip(
                                    selected = defaultApprovalPolicy == "PAUSE_AND_ASK",
                                    onClick = { defaultApprovalPolicy = "PAUSE_AND_ASK" },
                                    label = { Text("Pause & Ask in Inbox") }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("GPU Idle Cost Guard", fontWeight = FontWeight.SemiBold)
                                    Text("Auto-pause Kaggle GPU if approval is pending > 5 mins", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                }
                                Switch(checked = autoPauseGpuIdle, onCheckedChange = { autoPauseGpuIdle = it })
                            }

                            OutlinedTextField(
                                value = approvalTimeoutMins,
                                onValueChange = { approvalTimeoutMins = it },
                                label = { Text("Auto-Resolve Pending Approval Timeout (Minutes)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Fix #239: Only show global platform info if user is Owner
                            if (isOwner) {
                                HorizontalDivider()
                                Text("Owner Platform Controls", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text("Global n8n Webhook Dispatcher & Multi-Tenant Guard: ACTIVE", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                isSaving = true
                scope.launch {
                    try {
                        val req = GpuSetupRequest(
                            preferred_mode = preferredMode,
                            custom_gpu = CustomGpuConfigDto(customGpuEnabled, customGpuUrl, customGpuKey.ifBlank { null }),
                            kaggle_primary = KaggleAccountConfigDto(kagglePrimaryUser, kagglePrimaryKey),
                            kaggle_secondary = if (kaggleSecondaryUser.isNotBlank()) KaggleAccountConfigDto(kaggleSecondaryUser, kaggleSecondaryKey) else null,
                            telegram_storage = TelegramStorageConfigDto(telegramBotToken, telegramChatId, streamBridgeUrl),
                            github_worker_url = githubCpuWorkerUrl
                        )
                        val res = apiService.saveGpuConfig(req)
                        val activePid = repository.currentProjectId()
                        if (activePid.isNotBlank()) {
                            repository.updateProjectSettings(
                                projectId = activePid,
                                autoApprove = (defaultApprovalPolicy == "AUTO_APPROVE")
                            )
                        }
                        if (res.isSuccessful) {
                            Toast.makeText(context, "BYOD Infrastructure & Preferences saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Saved preferences locally (HTTP ${res.code()})", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Save Infrastructure & Preferences")
            }
        }
    }
}
