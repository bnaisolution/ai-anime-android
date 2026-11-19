package org.dpdns.bnai.anime.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SaasPlanDto
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*
import org.dpdns.bnai.anime.ui.testing.TestTags

private data class AiProviderSpec(
    val id: String,
    val label: String,
    val models: List<String>,
    val available: Boolean = true
)

private data class DropdownOption(
    val id: String,
    val label: String,
    val enabled: Boolean = true
)

private val AI_PROVIDERS = listOf(
    AiProviderSpec(
        id = "groq",
        label = "Groq",
        models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")
    ),
    AiProviderSpec(
        id = "gemini",
        label = "Google Gemini",
        models = listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash")
    ),
    AiProviderSpec("openai", "OpenAI-Compatible", emptyList(), available = false),
    AiProviderSpec("nvidia_nim", "NVIDIA NIM", emptyList(), available = false)
)

private fun providerSpec(id: String): AiProviderSpec? = AI_PROVIDERS.firstOrNull { it.id == id }

private fun modelsFor(providerId: String): List<String> = providerSpec(providerId)?.models.orEmpty()

private fun text(value: Any?): String =
    value?.toString()?.takeUnless { it.isBlank() || it == "null" }.orEmpty()

private fun map(value: Any?): Map<*, *>? = value as? Map<*, *>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenOwnerConsole: (() -> Unit)? = null) {
    val context = LocalContext.current
    val tokenManager = LocalAppContainer.current.tokenManager
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()
    val isOwner = tokenManager.isOwner()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("AI & Vision", "Compute Workers", "Storage & Policy", "Database", "Plan & Usage", "Session")

    var primaryProvider by remember { mutableStateOf("") }
    var fallbackProvider by remember { mutableStateOf("") }
    var visionProvider by remember { mutableStateOf("") }
    var primaryModel by remember { mutableStateOf("") }
    var fallbackModel by remember { mutableStateOf("") }
    var visionModel by remember { mutableStateOf("") }

    var gpuChoice by remember { mutableStateOf("KAGGLE_PRIMARY") }
    var cpuChoice by remember { mutableStateOf("GITHUB_CPU") }
    var storageChoice by remember { mutableStateOf("TELEGRAM") }

    var kagglePrimaryUsername by remember { mutableStateOf("") }
    var kaggleSecondaryUsername by remember { mutableStateOf("") }
    var customGpuUrl by remember { mutableStateOf("") }
    var githubWorkerRepo by remember { mutableStateOf("bnaisolution/anime-cpu-worker") }
    var githubWorkerUrl by remember { mutableStateOf("") }
    var ownCpuUrl by remember { mutableStateOf("") }
    var telegramChatId by remember { mutableStateOf("") }
    var autoApprovePolicy by remember { mutableStateOf(false) }

    var tenantDbMode by remember { mutableStateOf("central") }
    var tenantDbStatus by remember { mutableStateOf("active") }
    var tenantDbHost by remember { mutableStateOf("") }
    var tenantDbName by remember { mutableStateOf("") }
    var tenantDbSchemaVersion by remember { mutableIntStateOf(0) }
    var tenantDbError by remember { mutableStateOf<String?>(null) }
    var tenantDbLastChecked by remember { mutableStateOf<String?>(null) }
    var byodEnabled by remember { mutableStateOf(false) }
    var platformDbEnabled by remember { mutableStateOf(false) }
    var databaseDsn by remember { mutableStateOf("") }
    var isDbBusy by remember { mutableStateOf(false) }
    var databaseMessage by remember { mutableStateOf<String?>(null) }

    var newGroqKey by remember { mutableStateOf("") }
    var newGeminiKey by remember { mutableStateOf("") }
    var newKagglePrimaryKey by remember { mutableStateOf("") }
    var newKaggleSecondaryKey by remember { mutableStateOf("") }
    var newCustomGpuKey by remember { mutableStateOf("") }
    var newGithubToken by remember { mutableStateOf("") }
    var newOwnCpuToken by remember { mutableStateOf("") }
    var newTelegramBotToken by remember { mutableStateOf("") }

    var hasSavedGroqKey by remember { mutableStateOf(false) }
    var hasSavedGeminiKey by remember { mutableStateOf(false) }
    var hasSavedKagglePrimaryKey by remember { mutableStateOf(false) }
    var hasSavedKaggleSecondaryKey by remember { mutableStateOf(false) }
    var hasSavedCustomGpuKey by remember { mutableStateOf(false) }
    var hasSavedGithubToken by remember { mutableStateOf(false) }
    var hasSavedOwnCpuToken by remember { mutableStateOf(false) }
    var hasSavedTelegramToken by remember { mutableStateOf(false) }

    var showAddProviderDialog by remember { mutableStateOf(false) }
    var newProviderId by remember { mutableStateOf("groq") }
    var newProviderKey by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isCheckingDatabase by remember { mutableStateOf(false) }
    var databaseHealthy by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }

    var cpuTargetBadge by remember { mutableStateOf<String?>(null) }
    var cpuTestMessage by remember { mutableStateOf<String?>(null) }
    var isTestingCpu by remember { mutableStateOf(false) }

    var saasPlans by remember { mutableStateOf<List<SaasPlanDto>>(emptyList()) }
    var saasPlanName by remember { mutableStateOf("") }
    var saasPlanStatus by remember { mutableStateOf("") }
    var saasRenders by remember { mutableIntStateOf(0) }
    var saasRenderLimit by remember { mutableIntStateOf(0) }
    var saasEstimatedCost by remember { mutableStateOf(0.0) }
    var saasBillingEnabled by remember { mutableStateOf(false) }
    var saasError by remember { mutableStateOf<String?>(null) }
    var isLoadingSaas by remember { mutableStateOf(false) }

    val configuredProviderIds = buildList {
        if (hasSavedGroqKey || newGroqKey.isNotBlank()) add("groq")
        if (hasSavedGeminiKey || newGeminiKey.isNotBlank()) add("gemini")
    }

    fun checkDatabaseConnection() {
        scope.launch {
            isCheckingDatabase = true
            databaseHealthy = repository.checkBackendHealth()
            connectionStatus = if (databaseHealthy) {
                "Central database reachable • backend round-trip verified"
            } else {
                "Database unreachable • check backend gateway"
            }
            isCheckingDatabase = false
        }
    }

    fun applyTenantDatabase(data: Map<String, Any?>) {
        tenantDbMode = text(data["mode"]).ifBlank { "central" }
        tenantDbStatus = text(data["status"]).ifBlank { "active" }
        tenantDbHost = text(data["host"])
        tenantDbName = text(data["database"])
        tenantDbSchemaVersion = (data["schema_version"] as? Number)?.toInt() ?: 0
        tenantDbError = text(data["last_error"]).takeIf { it.isNotBlank() }
        tenantDbLastChecked = text(data["last_checked_at"]).takeIf { it.isNotBlank() }
        byodEnabled = data["byod_enabled"] == true
        platformDbEnabled = data["platform_db_enabled"] == true
    }

    fun loadTenantDatabase() {
        scope.launch {
            when (val res = repository.fetchTenantDatabase()) {
                is Resource.Success -> applyTenantDatabase(res.data)
                is Resource.Error -> databaseMessage = res.message
                else -> Unit
            }
        }
    }

    fun testDatabaseDsn() {
        val dsn = databaseDsn.trim()
        if (dsn.isBlank()) {
            databaseMessage = "Paste a Postgres/Supabase connection string first."
            return
        }
        scope.launch {
            isDbBusy = true
            when (val res = repository.testTenantDatabase(dsn)) {
                is Resource.Success -> {
                    val ok = res.data["ok"] == true
                    val latency = (res.data["latency_ms"] as? Number)?.toLong() ?: 0L
                    databaseMessage = if (ok) {
                        "Connection OK • ${latency}ms • credentials will be encrypted before save"
                    } else {
                        "Connection failed: ${text(res.data["error"]).ifBlank { "unknown error" }}"
                    }
                }
                is Resource.Error -> databaseMessage = res.message
                else -> Unit
            }
            isDbBusy = false
        }
    }

    fun connectDatabaseDsn() {
        val dsn = databaseDsn.trim()
        if (dsn.isBlank()) {
            databaseMessage = "Paste a Postgres/Supabase connection string first."
            return
        }
        scope.launch {
            isDbBusy = true
            when (val res = repository.connectTenantDatabase(dsn)) {
                is Resource.Success -> {
                    applyTenantDatabase(res.data)
                    databaseDsn = ""
                    databaseMessage = "User-owned database connected and encrypted."
                }
                is Resource.Error -> databaseMessage = res.message
                else -> Unit
            }
            isDbBusy = false
        }
    }

    fun checkTenantDatabaseHealth() {
        scope.launch {
            isDbBusy = true
            when (val res = repository.fetchTenantDatabaseHealth()) {
                is Resource.Success -> {
                    applyTenantDatabase(res.data)
                    databaseMessage = if (res.data["reachable"] == true) {
                        "User database reachable • schema v${(res.data["schema_version"] as? Number)?.toInt() ?: 0}"
                    } else {
                        "User database unavailable: ${text(res.data["error"]).ifBlank { "restore required" }}"
                    }
                }
                is Resource.Error -> databaseMessage = res.message
                else -> Unit
            }
            isDbBusy = false
        }
    }

    fun disconnectDatabase() {
        scope.launch {
            isDbBusy = true
            when (val res = repository.disconnectTenantDatabase()) {
                is Resource.Success -> {
                    applyTenantDatabase(res.data)
                    databaseDsn = ""
                    databaseMessage = "User-owned database removed. Workspace is back on the central database."
                }
                is Resource.Error -> databaseMessage = res.message
                else -> Unit
            }
            isDbBusy = false
        }
    }

    fun migrateCurrentProject() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            databaseMessage = "Select a project before migrating it."
            return
        }
        scope.launch {
            isDbBusy = true
            when (val res = repository.migrateTenantProjects(listOf(pid))) {
                is Resource.Success -> databaseMessage = "Migrated ${(res.data["migrated"] as? Number)?.toInt() ?: 0} project(s) to your database."
                is Resource.Error -> databaseMessage = res.message
                else -> Unit
            }
            isDbBusy = false
        }
    }

    fun hydrateSettingsFromServer() {
        val pid = repository.currentProjectId()
        scope.launch {
            isLoading = true
            val gpuRes = repository.fetchGpuInfrastructure()
            if (gpuRes is Resource.Success) {
                val d = gpuRes.data
                val routing = map(d["gpu_routing"])
                val kagglePrimary = map(d["kaggle_primary"]) ?: map(routing?.get("kaggle_primary"))
                val kaggleSecondary = map(d["kaggle_secondary"]) ?: map(routing?.get("kaggle_secondary"))
                val customGpu = map(routing?.get("custom_gpu"))
                val github = map(d["github_worker"])
                val cpuWorker = map(d["cpu_worker"])
                val telegram = map(d["telegram_storage"])

                val hasGroq = text(d["groq_api_key"]).isNotBlank()
                val hasGemini = text(d["gemini_api_key"]).isNotBlank()
                hasSavedGroqKey = hasGroq
                hasSavedGeminiKey = hasGemini

                val loadedProviders = buildList {
                    if (hasGroq) add("groq")
                    if (hasGemini) add("gemini")
                }

                val serverPrimary = text(d["primary_llm_provider"])
                primaryProvider = serverPrimary.takeIf { loadedProviders.contains(it) }.orEmpty()
                val serverFallback = text(d["fallback_llm_provider"])
                fallbackProvider = serverFallback.takeIf { loadedProviders.contains(it) }.orEmpty()
                val serverVision = text(d["vision_provider"])
                visionProvider = serverVision.takeIf { loadedProviders.contains(it) }.orEmpty()

                primaryModel = text(d["primary_llm_model"]).ifBlank {
                    modelsFor(primaryProvider).firstOrNull().orEmpty()
                }
                fallbackModel = text(d["fallback_llm_model"]).ifBlank {
                    modelsFor(fallbackProvider).firstOrNull().orEmpty()
                }
                visionModel = text(d["vision_model"]).ifBlank {
                    modelsFor(visionProvider).firstOrNull().orEmpty()
                }

                kagglePrimaryUsername = text(kagglePrimary?.get("username"))
                kaggleSecondaryUsername = text(kaggleSecondary?.get("username"))
                hasSavedKagglePrimaryKey = text(kagglePrimary?.get("api_key")).isNotBlank()
                hasSavedKaggleSecondaryKey = text(kaggleSecondary?.get("api_key")).isNotBlank()

                customGpuUrl = text(customGpu?.get("worker_url"))
                hasSavedCustomGpuKey = text(customGpu?.get("api_key")).isNotBlank()

                val loadedGpuChoice = text(d["gpu_choice"])
                gpuChoice = when {
                    loadedGpuChoice in setOf("KAGGLE_PRIMARY", "KAGGLE_SECONDARY", "CUSTOM_GPU", "PLATFORM_GPU") -> loadedGpuChoice
                    text(d["preferred_mode"]) == "CUSTOM_GPU" || customGpuUrl.isNotBlank() -> "CUSTOM_GPU"
                    kagglePrimaryUsername.isNotBlank() -> "KAGGLE_PRIMARY"
                    kaggleSecondaryUsername.isNotBlank() -> "KAGGLE_SECONDARY"
                    else -> "KAGGLE_PRIMARY"
                }

                githubWorkerRepo = text(github?.get("repository")).ifBlank { "bnaisolution/anime-cpu-worker" }
                githubWorkerUrl = text(github?.get("worker_url")).ifBlank { text(d["github_worker_url"]) }
                hasSavedGithubToken = text(github?.get("token")).isNotBlank()

                val loadedCpuChoice = text(d["cpu_choice"])
                cpuChoice = when {
                    loadedCpuChoice in setOf("GITHUB_CPU", "OWN_CPU", "PLATFORM_CPU") -> loadedCpuChoice
                    text(cpuWorker?.get("mode")) == "OWN_CPU" || ownCpuUrl.isNotBlank() -> "OWN_CPU"
                    else -> "GITHUB_CPU"
                }
                ownCpuUrl = text(cpuWorker?.get("worker_url"))
                hasSavedOwnCpuToken = text(cpuWorker?.get("token")).isNotBlank()

                storageChoice = text(d["storage_choice"]).ifBlank { "TELEGRAM" }
                telegramChatId = text(telegram?.get("storage_chat_id")).ifBlank { text(d["telegram_chat_id"]) }
                hasSavedTelegramToken = text(telegram?.get("bot_token")).isNotBlank()
            }

            if (pid.isNotBlank()) {
                val overview = repository.fetchProjectOverviewRaw(pid)
                if (overview is Resource.Success) {
                    autoApprovePolicy = overview.data["auto_approve"] == true
                }
            }
            isLoading = false
        }
    }

    fun runCpuTest() {
        if (isTestingCpu) return
        isTestingCpu = true
        cpuTestMessage = null
        scope.launch {
            when (val target = repository.loadCpuTarget()) {
                is Resource.Success -> cpuTargetBadge = target.data.badge
                else -> Unit
            }
            when (val res = repository.testCpuTarget()) {
                is Resource.Success -> {
                    cpuTestMessage = if (res.data.ok) {
                        "CPU target OK${res.data.latencyMs?.let { " • ${it}ms" } ?: ""}"
                    } else {
                        "CPU target failed: ${res.data.error ?: "unreachable"}"
                    }
                }
                is Resource.Error -> cpuTestMessage = res.message
                else -> Unit
            }
            isTestingCpu = false
        }
    }

    fun loadSaas() {
        isLoadingSaas = true
        saasError = null
        scope.launch {
            when (val plans = repository.loadSaasPlans()) {
                is Resource.Success -> {
                    saasPlans = plans.data.plans
                    saasBillingEnabled = plans.data.billing_enabled
                }
                is Resource.Error -> saasError = plans.message
                else -> Unit
            }
            when (val sub = repository.loadSaasSubscription()) {
                is Resource.Success -> {
                    saasPlanName = sub.data.plan_name
                    saasPlanStatus = sub.data.status
                    saasRenderLimit = sub.data.limits["renders_per_month"]?.toInt() ?: 0
                }
                is Resource.Error -> if (saasError == null) saasError = sub.message
                else -> Unit
            }
            when (val usage = repository.loadSaasUsage(30)) {
                is Resource.Success -> {
                    saasRenders = usage.data.renders
                    saasEstimatedCost = usage.data.estimated_cost_usd
                    usage.data.limit?.let { saasRenderLimit = it }
                }
                else -> Unit
            }
            isLoadingSaas = false
        }
    }

    LaunchedEffect(Unit) {
        hydrateSettingsFromServer()
        checkDatabaseConnection()
        loadTenantDatabase()
        loadSaas()
        scope.launch {
            when (val target = repository.loadCpuTarget()) {
                is Resource.Success -> cpuTargetBadge = target.data.badge
                else -> Unit
            }
        }
    }

    fun addProvider(id: String, key: String) {
        when (id) {
            "groq" -> {
                newGroqKey = key
                hasSavedGroqKey = true
            }
            "gemini" -> {
                newGeminiKey = key
                hasSavedGeminiKey = true
            }
        }
        if (primaryProvider.isBlank()) {
            primaryProvider = id
            primaryModel = modelsFor(id).firstOrNull().orEmpty()
        } else if (fallbackProvider.isBlank() && fallbackProvider != primaryProvider) {
            fallbackProvider = id
            fallbackModel = modelsFor(id).firstOrNull().orEmpty()
        }
        if (visionProvider.isBlank()) {
            visionProvider = id
            visionModel = modelsFor(id).firstOrNull().orEmpty()
        }
    }

    if (showAddProviderDialog) {
        AlertDialog(
            onDismissRequest = { showAddProviderDialog = false },
            title = { Text("Add AI Provider") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Groq and Gemini are available now. More providers are shown for the roadmap.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AiDropdown(
                        label = "Provider",
                        selectedId = newProviderId,
                        options = AI_PROVIDERS.map { DropdownOption(it.id, it.label, it.available) },
                        onSelected = { newProviderId = it }
                    )
                    OutlinedTextField(
                        value = newProviderKey,
                        onValueChange = { newProviderKey = it },
                        label = { Text("${providerSpec(newProviderId)?.label ?: "Provider"} API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Settings.AI_ADD_KEY)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newProviderKey.isNotBlank() && providerSpec(newProviderId)?.available == true,
                    onClick = {
                        addProvider(newProviderId, newProviderKey.trim())
                        newProviderKey = ""
                        showAddProviderDialog = false
                    }
                ) { Text("Add Provider") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProviderDialog = false }) { Text("Cancel") }
            }
        )
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
            onRefreshClick = {
                hydrateSettingsFromServer()
                checkDatabaseConnection()
            }
        )

        ScreenHeader(
            title = "Studio Infrastructure & Settings",
            subtitle = "User workspace admin: AI providers, workers, storage, database and session"
        )

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) },
                    modifier = if (title == "Database") Modifier.testTag(TestTags.Settings.DB_TAB) else Modifier
                )
            }
        }

        when (selectedTab) {
            0 -> {
                StandardCard {
                    SectionHeader(title = "AI & Vision Providers", badgeText = if (configuredProviderIds.isEmpty()) "NOT CONFIGURED" else "BYOD")
                    if (configuredProviderIds.isEmpty()) {
                        Text(
                            "No AI provider added yet. Add Groq or Gemini to unlock the model selector.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton(
                            text = "Add AI Provider",
                            onClick = { showAddProviderDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.Settings.AI_ADD)
                        )
                    } else {
                        AiDropdown(
                            label = "Primary AI Provider",
                            selectedId = primaryProvider,
                            options = configuredProviderIds.map { DropdownOption(it, providerSpec(it)?.label ?: it) } +
                                DropdownOption("__add__", "＋ Add more AI provider"),
                            onSelected = { id ->
                                if (id == "__add__") {
                                    showAddProviderDialog = true
                                } else {
                                    primaryProvider = id
                                    primaryModel = modelsFor(id).firstOrNull().orEmpty()
                                }
                            },
                            modifier = Modifier.testTag(TestTags.Settings.AI_PRIMARY)
                        )
                        if (primaryProvider.isNotBlank()) {
                            ModelDropdown(
                                label = "Primary Model",
                                selectedId = primaryModel,
                                providerId = primaryProvider,
                                onSelected = { primaryModel = it }
                            )
                        }

                        AiDropdown(
                            label = "Fallback AI Provider",
                            selectedId = fallbackProvider,
                            options = listOf(DropdownOption("__none__", "No fallback")) +
                                configuredProviderIds.map { DropdownOption(it, providerSpec(it)?.label ?: it) } +
                                DropdownOption("__add__", "＋ Add more AI provider"),
                            onSelected = { id ->
                                when (id) {
                                    "__add__" -> showAddProviderDialog = true
                                    "__none__" -> {
                                        fallbackProvider = ""
                                        fallbackModel = ""
                                    }
                                    else -> {
                                        fallbackProvider = id
                                        fallbackModel = modelsFor(id).firstOrNull().orEmpty()
                                    }
                                }
                            },
                            modifier = Modifier.testTag(TestTags.Settings.AI_FALLBACK)
                        )
                        if (fallbackProvider.isNotBlank()) {
                            ModelDropdown(
                                label = "Fallback Model",
                                selectedId = fallbackModel,
                                providerId = fallbackProvider,
                                onSelected = { fallbackModel = it }
                            )
                        }

                        AiDropdown(
                            label = "Vision DNA Provider",
                            selectedId = visionProvider,
                            options = configuredProviderIds.map { DropdownOption(it, providerSpec(it)?.label ?: it) },
                            onSelected = { id ->
                                visionProvider = id
                                visionModel = modelsFor(id).firstOrNull().orEmpty()
                            }
                        )
                        if (visionProvider.isNotBlank()) {
                            ModelDropdown(
                                label = "Vision Model",
                                selectedId = visionModel,
                                providerId = visionProvider,
                                onSelected = { visionModel = it }
                            )
                        }

                        SecondaryButton(
                            text = "Add Another AI Provider",
                            onClick = { showAddProviderDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            1 -> {
                StandardCard {
                    SectionHeader(title = "GPU Worker", badgeText = if (gpuChoice == "PLATFORM_GPU") "COMING SOON" else "BYOD")
                    Text(
                        "Choose where image, animation, lip-sync and video rendering runs.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionCard(
                        title = "Kaggle Account 1",
                        subtitle = "Free GPU • primary worker",
                        selected = gpuChoice == "KAGGLE_PRIMARY",
                        onClick = { gpuChoice = "KAGGLE_PRIMARY" }
                    )
                    SelectionCard(
                        title = "Kaggle Account 2",
                        subtitle = "Free GPU • secondary worker",
                        selected = gpuChoice == "KAGGLE_SECONDARY",
                        onClick = { gpuChoice = "KAGGLE_SECONDARY" }
                    )
                    SelectionCard(
                        title = "Add your own GPU",
                        subtitle = "Custom worker URL and API key",
                        selected = gpuChoice == "CUSTOM_GPU",
                        onClick = { gpuChoice = "CUSTOM_GPU" }
                    )
                    SelectionCard(
                        title = "Platform GPU",
                        subtitle = "Pay per minute • coming soon",
                        selected = false,
                        enabled = false,
                        onClick = {}
                    )

                    when (gpuChoice) {
                        "KAGGLE_PRIMARY" -> {
                            OutlinedTextField(
                                value = kagglePrimaryUsername,
                                onValueChange = { kagglePrimaryUsername = it },
                                label = { Text("Kaggle Account 1 Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newKagglePrimaryKey,
                                onValueChange = { newKagglePrimaryKey = it },
                                label = { Text(if (hasSavedKagglePrimaryKey) "Kaggle Account 1 API Key (Saved — enter to replace)" else "Kaggle Account 1 API Key") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "KAGGLE_SECONDARY" -> {
                            OutlinedTextField(
                                value = kaggleSecondaryUsername,
                                onValueChange = { kaggleSecondaryUsername = it },
                                label = { Text("Kaggle Account 2 Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newKaggleSecondaryKey,
                                onValueChange = { newKaggleSecondaryKey = it },
                                label = { Text(if (hasSavedKaggleSecondaryKey) "Kaggle Account 2 API Key (Saved — enter to replace)" else "Kaggle Account 2 API Key") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "CUSTOM_GPU" -> {
                            OutlinedTextField(
                                value = customGpuUrl,
                                onValueChange = { customGpuUrl = it },
                                label = { Text("Your GPU Worker URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newCustomGpuKey,
                                onValueChange = { newCustomGpuKey = it },
                                label = { Text(if (hasSavedCustomGpuKey) "GPU API Key (Saved — enter to replace)" else "GPU API Key") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                StandardCard {
                    SectionHeader(title = "CPU Worker", badgeText = if (cpuChoice == "PLATFORM_CPU") "COMING SOON" else "BYOD")
                    Text(
                        "Choose where TTS, FFmpeg and episode assembly run.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionCard(
                        title = "GitHub CPU Worker",
                        subtitle = "GitHub Actions repository",
                        selected = cpuChoice == "GITHUB_CPU",
                        onClick = { cpuChoice = "GITHUB_CPU" }
                    )
                    SelectionCard(
                        title = "Own CPU Worker",
                        subtitle = "Your VPS or custom worker URL",
                        selected = cpuChoice == "OWN_CPU",
                        onClick = { cpuChoice = "OWN_CPU" }
                    )
                    SelectionCard(
                        title = "Platform CPU Worker",
                        subtitle = "Pay per minute • coming soon",
                        selected = false,
                        enabled = false,
                        onClick = {}
                    )

                    when (cpuChoice) {
                        "GITHUB_CPU" -> {
                            OutlinedTextField(
                                value = githubWorkerRepo,
                                onValueChange = { githubWorkerRepo = it },
                                label = { Text("GitHub CPU Worker Repository") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = githubWorkerUrl,
                                onValueChange = { githubWorkerUrl = it },
                                label = { Text("GitHub CPU Worker URL (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newGithubToken,
                                onValueChange = { newGithubToken = it },
                                label = { Text(if (hasSavedGithubToken) "GitHub Actions Token (Saved — enter to replace)" else "GitHub Actions Token") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "OWN_CPU" -> {
                            OutlinedTextField(
                                value = ownCpuUrl,
                                onValueChange = { ownCpuUrl = it },
                                label = { Text("Your CPU Worker URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newOwnCpuToken,
                                onValueChange = { newOwnCpuToken = it },
                                label = { Text(if (hasSavedOwnCpuToken) "CPU Worker Token (Saved — enter to replace)" else "CPU Worker Token") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    cpuTargetBadge?.let {
                        Text("Resolved target: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SecondaryButton(
                        text = if (isTestingCpu) "Testing CPU Target..." else "Test CPU Target",
                        onClick = { runCpuTest() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    cpuTestMessage?.let {
                        Text(
                            it,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            2 -> {
                StandardCard {
                    SectionHeader(title = "Storage Provider", badgeText = "BYOD")
                    Text(
                        "Current release supports Telegram storage. S3-compatible and custom storage are next.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AiDropdown(
                        label = "Storage Provider",
                        selectedId = storageChoice,
                        options = listOf(
                            DropdownOption("TELEGRAM", "Telegram Storage"),
                            DropdownOption("S3_COMPATIBLE", "S3-Compatible (coming soon)", enabled = false),
                            DropdownOption("CUSTOM_HTTP", "Custom HTTP Storage (coming soon)", enabled = false)
                        ),
                        onSelected = { storageChoice = it }
                    )
                    if (storageChoice == "TELEGRAM") {
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
                            label = { Text(if (hasSavedTelegramToken) "Telegram Bot Token (Saved — enter to replace)" else "Telegram Bot Token") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                StandardCard {
                    SectionHeader(title = "Approval Policy")
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
                    SectionHeader(
                        title = "Database Mode",
                        badgeText = if (tenantDbMode == "byod") "MY DATABASE" else "CENTRAL"
                    )
                    Text(
                        "Database settings are workspace-level. Each project stays on the central database until you explicitly migrate it.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectionCard(
                        title = "Central Anime Database",
                        subtitle = "Managed by the platform • default and rollback-safe",
                        selected = tenantDbMode == "central",
                        enabled = true,
                        onClick = {}
                    )
                    SelectionCard(
                        title = "My Database",
                        subtitle = if (byodEnabled) {
                            "Bring your own Postgres/Supabase • DSN encrypted at rest"
                        } else {
                            "Bring your own Postgres/Supabase • disabled by backend flag"
                        },
                        selected = tenantDbMode == "byod",
                        enabled = byodEnabled,
                        onClick = {}
                    )
                    SelectionCard(
                        title = "Platform-Managed Database",
                        subtitle = "Paid • requires explicit owner approval",
                        selected = false,
                        enabled = platformDbEnabled,
                        onClick = {}
                    )
                }

                if (byodEnabled) {
                    StandardCard {
                        SectionHeader(
                            title = "My Postgres / Supabase",
                            badgeText = if (tenantDbMode == "byod") tenantDbStatus.uppercase() else "NOT CONNECTED"
                        )
                        if (tenantDbMode == "byod") {
                            Text(
                                "Host: ${tenantDbHost.ifBlank { "hidden" }} • DB: ${tenantDbName.ifBlank { "hidden" }} • Schema v$tenantDbSchemaVersion",
                                fontSize = 12.sp
                            )
                            tenantDbLastChecked?.let { Text("Last checked: $it", fontSize = 11.sp) }
                            tenantDbError?.let {
                                Text("Last error: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        OutlinedTextField(
                            value = databaseDsn,
                            onValueChange = { databaseDsn = it },
                            label = { Text("Postgres DSN (postgresql://user:pass@host:5432/db?sslmode=require)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.Settings.DB_DSN)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryButton(
                                text = if (isDbBusy) "Testing..." else "Test DSN",
                                enabled = !isDbBusy && databaseDsn.isNotBlank(),
                                onClick = { testDatabaseDsn() },
                                modifier = Modifier.weight(1f).testTag(TestTags.Settings.DB_TEST_DSN)
                            )
                            PrimaryButton(
                                text = if (isDbBusy) "Saving..." else "Connect",
                                enabled = !isDbBusy && databaseDsn.isNotBlank(),
                                onClick = { connectDatabaseDsn() },
                                modifier = Modifier.weight(1f).testTag(TestTags.Settings.DB_CONNECT)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryButton(
                                text = "Health Check",
                                enabled = !isDbBusy && tenantDbMode == "byod",
                                onClick = { checkTenantDatabaseHealth() },
                                modifier = Modifier.weight(1f).testTag(TestTags.Settings.DB_HEALTH)
                            )
                            SecondaryButton(
                                text = "Migrate Current Project",
                                enabled = !isDbBusy && tenantDbMode == "byod",
                                onClick = { migrateCurrentProject() },
                                modifier = Modifier.weight(1f).testTag(TestTags.Settings.DB_MIGRATE)
                            )
                        }
                        if (tenantDbMode == "byod") {
                            DangerButton(
                                text = "Disconnect My Database",
                                enabled = !isDbBusy,
                                onClick = { disconnectDatabase() },
                                modifier = Modifier.fillMaxWidth().testTag(TestTags.Settings.DB_DISCONNECT)
                            )
                        }
                        databaseMessage?.let { Text(it, fontSize = 12.sp) }
                    }
                }

                StandardCard {
                    SectionHeader(
                        title = "Central Database Health",
                        badgeText = if (databaseHealthy) "CONNECTED" else "CHECKING"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Backend / DB Gateway", fontWeight = FontWeight.SemiBold)
                        Text(if (databaseHealthy) "Online" else "Unavailable")
                    }
                    Text(
                        "Schema version: managed • RLS: enforced",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    connectionStatus?.let { Text(it, fontSize = 12.sp) }
                    SecondaryButton(
                        text = if (isCheckingDatabase) "Checking..." else "Test Database Connection",
                        enabled = !isCheckingDatabase,
                        onClick = { checkDatabaseConnection() },
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Settings.DB_TEST)
                    )
                }

                if (isOwner) {
                    StandardCard {
                        SectionHeader(title = "Owner Console", badgeText = "OWNER ONLY")
                        Text(
                            "View all tenants, database health, migrations, workers, audit and platform controls.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton(
                            text = "Open Owner Console",
                            onClick = { onOpenOwnerConsole?.invoke() },
                            enabled = onOpenOwnerConsole != null,
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.Settings.OWNER_CONSOLE)
                        )
                    }
                }
            }

            4 -> {
                StandardCard {
                    SectionHeader(
                        title = "Plan & Usage",
                        badgeText = if (isLoadingSaas) "LOADING" else (saasPlanName.ifBlank { "FREE" }).uppercase()
                    )
                    if (saasError != null) {
                        Text(saasError ?: "", fontSize = 12.sp, color = StudioTokens.Error)
                        SecondaryButton(
                            text = "Retry",
                            onClick = { loadSaas() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(saasPlanName.ifBlank { "Free" }, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Status: ${saasPlanStatus.ifBlank { "active" }} • BYOD-first workspace",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusChip(label = if (saasBillingEnabled) "BILLING ON" else "BILLING OFF")
                        }
                        Text(
                            "Renders this month: $saasRenders${if (saasRenderLimit > 0) " / $saasRenderLimit" else ""}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Estimated provider cost: \$${"%.2f".format(saasEstimatedCost)} (BYOD keys, billed by your provider)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecondaryButton(
                            text = "Refresh Usage",
                            onClick = { loadSaas() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (saasPlans.isNotEmpty()) {
                    StandardCard {
                        SectionHeader(title = "Available Plans", badgeText = "BYOD")
                        saasPlans.forEach { plan ->
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(plan.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (plan.price_inr <= 0.0) "Free" else "₹${plan.price_inr.toInt()}/mo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                plan.description?.let {
                                    Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    if (plan.byod_required) "Bring your own AI, GPU, CPU and storage" else "Platform-managed AI and GPU",
                                    fontSize = 10.sp,
                                    color = StudioTokens.Secondary
                                )
                            }
                        }
                    }
                }
            }

            5 -> {
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
                    Text(
                        "Session is managed by the auth project. Provider and infrastructure secrets are encrypted per user.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        PrimaryButton(
            text = "Save & Verify Configuration",
            isLoading = isSaving || isLoading,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.Settings.SAVE),
            onClick = {
                isSaving = true
                scope.launch {
                    val preferredMode = if (gpuChoice == "CUSTOM_GPU") "CUSTOM_GPU" else "KAGGLE_AUTO"
                    val infraPayload = mutableMapOf<String, Any?>(
                        "preferred_mode" to preferredMode,
                        "auto_deploy_kaggle" to gpuChoice.startsWith("KAGGLE"),
                        "gpu_choice" to gpuChoice,
                        "cpu_choice" to cpuChoice,
                        "storage_choice" to storageChoice,
                        "kaggle_primary" to mapOf(
                            "username" to kagglePrimaryUsername.trim(),
                            "api_key" to newKagglePrimaryKey.trim(),
                            "notebook_slug" to "anime-gpu-worker-primary"
                        ),
                        "kaggle_secondary" to mapOf(
                            "username" to kaggleSecondaryUsername.trim(),
                            "api_key" to newKaggleSecondaryKey.trim(),
                            "notebook_slug" to "anime-gpu-worker-secondary"
                        ),
                        "custom_gpu" to mapOf(
                            "enabled" to (gpuChoice == "CUSTOM_GPU"),
                            "worker_url" to customGpuUrl.trim(),
                            "api_key" to newCustomGpuKey.trim()
                        ),
                        "cpu_worker" to mapOf(
                            "mode" to cpuChoice,
                            "worker_url" to if (cpuChoice == "OWN_CPU") ownCpuUrl.trim() else githubWorkerUrl.trim(),
                            "token" to if (cpuChoice == "OWN_CPU") newOwnCpuToken.trim() else newGithubToken.trim(),
                            "repository" to githubWorkerRepo.trim()
                        ),
                        "telegram_storage" to mapOf(
                            "bot_token" to newTelegramBotToken.trim(),
                            "storage_chat_id" to telegramChatId.trim(),
                            "stream_bridge_base_url" to "https://api.bnai.dpdns.org/v1/media/telegram"
                        ),
                        "github_worker" to mapOf(
                            "worker_url" to githubWorkerUrl.trim(),
                            "token" to newGithubToken.trim(),
                            "repository" to githubWorkerRepo.trim()
                        )
                    )
                    if (primaryProvider.isNotBlank()) {
                        infraPayload["primary_llm_provider"] = primaryProvider
                        infraPayload["primary_llm_model"] = primaryModel
                    }
                    if (fallbackProvider.isNotBlank()) {
                        infraPayload["fallback_llm_provider"] = fallbackProvider
                        infraPayload["fallback_llm_model"] = fallbackModel
                    }
                    if (visionProvider.isNotBlank()) {
                        infraPayload["vision_provider"] = visionProvider
                        infraPayload["vision_model"] = visionModel
                    }
                    if (newGroqKey.isNotBlank()) infraPayload["groq_api_key"] = newGroqKey.trim()
                    if (newGeminiKey.isNotBlank()) infraPayload["gemini_api_key"] = newGeminiKey.trim()

                    val saveInfraRes = repository.saveGpuInfrastructure(infraPayload)
                    val pid = repository.currentProjectId()
                    if (pid.isNotBlank() && visionProvider.isNotBlank()) {
                        repository.updateProjectSettings(
                            projectId = pid,
                            updates = mapOf(
                                "auto_approve" to autoApprovePolicy,
                                "vision_provider" to visionProvider
                            )
                        )
                    }

                    val badge = listOf(primaryProvider, fallbackProvider)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                        .ifBlank { "Not configured" }
                    tokenManager.saveConfiguredProviderLabel("AI: $badge")

                    if (saveInfraRes is Resource.Success) {
                        newGroqKey = ""
                        newGeminiKey = ""
                        newKagglePrimaryKey = ""
                        newKaggleSecondaryKey = ""
                        newCustomGpuKey = ""
                        newGithubToken = ""
                        newOwnCpuToken = ""
                        newTelegramBotToken = ""
                        hydrateSettingsFromServer()
                        Toast.makeText(context, "Configuration persisted & verified via read-after-write", Toast.LENGTH_SHORT).show()
                    } else if (saveInfraRes is Resource.Error) {
                        Toast.makeText(context, saveInfraRes.message, Toast.LENGTH_LONG).show()
                    }
                    isSaving = false
                }
            }
        )

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiDropdown(
    label: String,
    selectedId: String,
    options: List<DropdownOption>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label ?: "Select"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    enabled = option.enabled,
                    onClick = {
                        expanded = false
                        onSelected(option.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    label: String,
    selectedId: String,
    providerId: String,
    onSelected: (String) -> Unit
) {
    val models = modelsFor(providerId)
    if (models.isEmpty()) return
    AiDropdown(
        label = label,
        selectedId = selectedId,
        options = models.map { DropdownOption(it, it) },
        onSelected = onSelected
    )
}

@Composable
private fun SelectionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!enabled) {
                StatusChip(label = "SOON")
            }
        }
    }
}
