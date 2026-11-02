#!/usr/bin/env bash
set -e

# ==============================================================================
# AI CINEMATIC ANIME ENGINE — FRONTEND DEPLOY (Merged Clean Version)
# Includes: GAP #1 (CommandIntent), GAP #3 (lifecycle-compose), GAP #4 (logic), GAP #5 (DTOs)
# ==============================================================================

BASE_DIR="app/src/main/java/org/dpdns/bnai/anime"

# ==============================================================================
# STEP 0: GAP #3 — build.gradle.kts with lifecycle-runtime-compose
# ==============================================================================
cat > app/build.gradle.kts << 'GRADLE_EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.dpdns.bnai.anime"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.dpdns.bnai.anime"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
GRADLE_EOF

echo "OK: app/build.gradle.kts created"

# ==============================================================================
# STEP 1: CLEAN SLATE + Directory Structure
# ==============================================================================
echo "WARNING: Deleting existing BASE_DIR — $BASE_DIR"
rm -rf "$BASE_DIR"
mkdir -p "$BASE_DIR/core/auth" \
         "$BASE_DIR/core/model" \
         "$BASE_DIR/core/network" \
         "$BASE_DIR/ui/theme" \
         "$BASE_DIR/ui/components" \
         "$BASE_DIR/ui/auth" \
         "$BASE_DIR/ui/studio" \
         "$BASE_DIR/ui/project" \
         "$BASE_DIR/ui/character" \
         "$BASE_DIR/ui/episode" \
         "$BASE_DIR/ui/production" \
         "$BASE_DIR/ui/qc" \
         "$BASE_DIR/ui/publish" \
         "$BASE_DIR/ui/review" \
         "$BASE_DIR/ui/brain" \
         "$BASE_DIR/ui/tools" \
         "$BASE_DIR/ui/settings" \
         "$BASE_DIR/ui/owner" \
         "$BASE_DIR/ui/analytics" \
         "$BASE_DIR/ui/library" \
         "$BASE_DIR/ui/notifications" \
         "$BASE_DIR/ui/advanced" \
         "$BASE_DIR/ui/navigation"

echo "OK: Directory structure created"

# ==============================================================================
# STEP 2: THEME (Color, Type, Theme)
# ==============================================================================
cat > "$BASE_DIR/ui/theme/Color.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF10A37F)
val Secondary = Color(0xFF3B82F6)
val BackgroundDark = Color(0xFF0F1115)
val BackgroundLight = Color(0xFFF7F7F8)
val SurfaceDark = Color(0xFF181B22)
val SurfaceLight = Color(0xFFFFFFFF)
val ErrorColor = Color(0xFFEF4444)
val WarningColor = Color(0xFFF59E0B)
val SuccessColor = Color(0xFF10B981)
val TextPrimaryDark = Color(0xFFECECF1)
val TextPrimaryLight = Color(0xFF111827)
EOF

cat > "$BASE_DIR/ui/theme/Type.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)
EOF

cat > "$BASE_DIR/ui/theme/Theme.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Secondary,
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    error = ErrorColor,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Secondary,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    error = ErrorColor,
    onError = Color.White
)

@Composable
fun AnimeStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
EOF

echo "OK: Theme files created"

# ==============================================================================
# STEP 3: MainActivity
# ==============================================================================
cat > "$BASE_DIR/MainActivity.kt" << 'EOF'
package org.dpdns.bnai.anime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.ui.navigation.AppNavGraph
import org.dpdns.bnai.anime.ui.theme.AnimeStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(applicationContext)
        setContent {
            AnimeStudioTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
EOF

echo "OK: MainActivity.kt created"

# ==============================================================================
# STEP 4: TokenManager (with Patch 2 session expiry StateFlow)
# ==============================================================================
cat > "$BASE_DIR/core/auth/TokenManager.kt" << 'EOF'
package org.dpdns.bnai.anime.core.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _sessionExpiredMessage = MutableStateFlow<String?>(null)
    val sessionExpiredMessage: StateFlow<String?> = _sessionExpiredMessage.asStateFlow()

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).apply()
        _sessionExpiredMessage.value = null
    }

    fun getToken(): String? = prefs.getString(KEY_JWT_TOKEN, null)

    fun saveUserMeta(userId: String?, workspaceId: String?, role: String?, email: String? = null) {
        prefs.edit()
            .putString(KEY_USER_ID, userId ?: "")
            .putString(KEY_WORKSPACE_ID, workspaceId ?: "")
            .putString(KEY_USER_ROLE, role ?: "normal_user")
            .putString(KEY_USER_EMAIL, email ?: "")
            .apply()
    }

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getWorkspaceId(): String = prefs.getString(KEY_WORKSPACE_ID, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "normal_user") ?: "normal_user"

    fun isOwner(): Boolean {
        val r = getUserRole().lowercase()
        return r == "owner" || r == "owner_admin" || r == "admin"
    }

    fun saveActiveProjectId(projectId: String) {
        if (projectId.isNotBlank()) {
            prefs.edit().putString(KEY_ACTIVE_PROJECT_ID, projectId).apply()
        }
    }

    fun getActiveProjectId(): String = prefs.getString(KEY_ACTIVE_PROJECT_ID, "") ?: ""

    fun clearToken(expiredMessage: String? = null) {
        prefs.edit().clear().apply()
        _sessionExpiredMessage.value = expiredMessage
    }

    fun consumeSessionExpiredMessage() {
        _sessionExpiredMessage.value = null
    }

    companion object {
        private const val PREFS_NAME = "anime_studio_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_WORKSPACE_ID = "workspace_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ACTIVE_PROJECT_ID = "active_project_id"

        @Volatile
        private var instance: TokenManager? = null

        fun init(context: Context): TokenManager =
            instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }

        fun getInstance(): TokenManager? = instance
    }
}
EOF

echo "OK: TokenManager.kt created"

# ==============================================================================
# STEP 5: Core Models (Project, Character, Episode)
# ==============================================================================
cat > "$BASE_DIR/core/model/Project.kt" << 'EOF'
package org.dpdns.bnai.anime.core.model

data class Project(
    val id: String,
    val name: String,
    val activeEpisode: String,
    val status: String
)
EOF

cat > "$BASE_DIR/core/model/Character.kt" << 'EOF'
package org.dpdns.bnai.anime.core.model

data class Character(
    val id: String,
    val name: String,
    val role: String,
    val visualDna: String,
    val voiceId: String
)
EOF

cat > "$BASE_DIR/core/model/Episode.kt" << 'EOF'
package org.dpdns.bnai.anime.core.model

data class Episode(
    val id: String,
    val episodeNumber: Int,
    val title: String,
    val status: String,
    val synopsis: String? = null,
    val updatedAt: String? = null
)
EOF

echo "OK: Model files created"

# ==============================================================================
# STEP 6: Resource wrapper
# ==============================================================================
cat > "$BASE_DIR/core/network/Resource.kt" << 'EOF'
package org.dpdns.bnai.anime.core.network

sealed class Resource<out T> {
    object Idle : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    object Empty : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
}
EOF

echo "OK: Resource.kt created"

# ==============================================================================
# STEP 7: RetrofitClient (with 401 interceptor + X-Project-Id header)
# ==============================================================================
cat > "$BASE_DIR/core/network/RetrofitClient.kt" << 'EOF'
package org.dpdns.bnai.anime.core.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.dpdns.bnai.anime.core.auth.TokenManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Fastify API Authority BASE_URL (Part 5.1 & Part 15.4)
    const val BASE_URL = "https://api.bnai.dpdns.org/"

    @Volatile
    private var apiServiceInstance: AnimeApiService? = null

    fun create(tokenManager: TokenManager? = null): AnimeApiService {
        val authAndTraceInterceptor = Interceptor { chain ->
            val tm = tokenManager ?: TokenManager.getInstance()
            val activeToken = tm?.getToken()
            val activeProject = tm?.getActiveProjectId().orEmpty()

            val requestBuilder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("X-Request-Id", UUID.randomUUID().toString())

            if (activeProject.isNotBlank()) {
                requestBuilder.header("X-Project-Id", activeProject)
            }
            if (!activeToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $activeToken")
            }

            val response = chain.proceed(requestBuilder.build())

            // Patch 2: Clear token on 401, surface session expiry
            if (response.code == 401) {
                tm?.clearToken("Session expired. Please log in again.")
            }
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authAndTraceInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnimeApiService::class.java)
    }

    fun getApiService(context: Context? = null): AnimeApiService {
        context?.let { TokenManager.init(it.applicationContext) }
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: create(TokenManager.getInstance()).also { apiServiceInstance = it }
        }
    }
}
EOF

echo "OK: RetrofitClient.kt created"

# ==============================================================================
# STEP 8: SharedComponents (ResourceStateBox, StatusBadge, KeyValueMapList)
# ==============================================================================
cat > "$BASE_DIR/ui/components/SharedComponents.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun <T> ResourceStateBox(
    resource: Resource<T>,
    emptyMessage: String = "No records found in the Anime database for this project.",
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Idle, is Resource.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Loading live data from Fastify backend...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        is Resource.Empty -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onRetry) { Text("Refresh") }
                    }
                }
            }
        }
        is Resource.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val isForbidden = resource.code == 403
                    Text(
                        text = if (isForbidden)
                            "Access Denied (403): Insufficient permissions for this project/resource."
                        else resource.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
        is Resource.Success -> content(resource.data)
    }
}

@Composable
fun StatusBadge(label: String) {
    val upper = label.uppercase()
    val bgColor = when {
        upper in listOf(
            "SUCCEEDED", "ACTIVE", "COMMITTED", "PASSED", "APPROVED",
            "ONLINE", "COMPLETED", "HEALTHY", "UP"
        ) -> Color(0xFF10B981)
        upper in listOf(
            "QUEUED", "DISPATCHED", "ACKNOWLEDGED", "RECONCILING",
            "RUNNING", "DRAFT", "WARNING", "PENDING", "BUSY"
        ) -> Color(0xFFF59E0B)
        upper in listOf(
            "FAILED", "ERROR", "BLOCKED", "REJECTED", "OFFLINE", "DOWN"
        ) -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = upper,
            color = bgColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun KeyValueMapList(items: List<JsonMap>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items) { map ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val title = (
                        map["title"] ?: map["name"] ?: map["wf_key"] ?:
                        map["worker_id"] ?: map["action"] ?: map["id"] ?: "Record"
                    ).toString()
                    val status = (
                        map["status"] ?: map["state"] ?: map["verification_status"]
                    )?.toString()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (!status.isNullOrBlank()) StatusBadge(status)
                    }
                    map.entries.forEach { (k, v) ->
                        if (k !in listOf("title", "name", "status", "state") && v != null) {
                            Text("$k: $v", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: SharedComponents.kt created"

echo ""
echo "========== PART 1 COMPLETE =========="
# ==============================================================================
# STEP 9: AnimeApiService.kt (GAP #1 CommandIntent + GAP #5 DTO fixes + GAP #2 endpoints)
# ==============================================================================
cat > "$BASE_DIR/core/network/AnimeApiService.kt" << 'EOF'
package org.dpdns.bnai.anime.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

typealias JsonMap = Map<String, @JvmSuppressWildcards Any?>

// ============================================================================
// ENVELOPE & AUTH
// ============================================================================
data class ApiMeta(val request_id: String? = null)
data class ApiEnvelope<T>(val data: T?, val meta: ApiMeta? = null)

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String? = null,
    val access_token: String? = null,
    @SerializedName("user_id", alternate = ["userId"]) val userId: String? = null,
    @SerializedName("workspace_id", alternate = ["workspaceId"]) val workspaceId: String? = null,
    val email: String? = null,
    val role: String? = "normal_user"
)

data class AuthMeResponse(
    val status: String? = null,
    val user: JsonMap? = null
)

// ============================================================================
// AI BRAIN
// ============================================================================
data class AiChatRequest(
    val message: String,
    val project_id: String? = null,
    val skill_key: String = "ai_admin",
    val request_id: String? = null
)

data class AiChatResponse(
    val request_id: String? = null,
    val status: String? = null,
    val reply: String? = null,
    val message: String? = null,
    val provider: String? = null
)

// ============================================================================
// HEALTH
// ============================================================================
data class HealthResponseDto(
    val ok: Boolean = false,
    val status: String? = null,
    val db: String? = null,
    val database: String? = null,
    val version: String? = null,
    val timestamp: String? = null
)

// ============================================================================
// GAP #1: CommandIntent — EXACT match with backend commands.types.ts (33 values)
// ============================================================================
enum class CommandIntent {
    CREATE_PROJECT,
    OPEN_PROJECT,
    SAVE_BIBLE,
    BIBLE_CHAT,
    QUICK_GENERATE_REEL,
    GENERATE_EPISODE,
    REGENERATE_EPISODE,
    CONTINUE_EPISODE,
    QUICK_GENERATE_IMAGE,
    QUICK_GENERATE_VIDEO,
    SAVE_TO_PROJECT,
    VISION_ANALYSIS,
    CHANGE_CHARACTER_VOICE,
    PREVIEW_VOICE,
    LIST_VOICES,
    RESET_VOICE,
    CHECK_CANON,
    VIEW_CANON,
    APPROVE_CANON_CHANGE,
    REJECT_CANON_CHANGE,
    CHECK_CONTINUITY,
    CHECK_QUOTA,
    RETRY_FAILED,
    VIEW_STATE,
    VIEW_AUDIT,
    RUN_QC,
    PUBLISH_NOW,
    VIEW_ANALYTICS,
    ADMIN_START_KAGGLE,
    ADMIN_STOP_KAGGLE,
    ADMIN_STATUS,
    DB_TEST,
    SYSTEM_HEALTH
}

// ============================================================================
// COMMAND DTOs
// ============================================================================
data class CreateCommandRequest(
    val project_id: String,
    val intent: String,
    val mode: String = "SAVE",
    val episode_no: Int? = null,
    val payload: JsonMap = emptyMap()
)

data class ApiCommandRecordDto(
    val id: String,
    val project_id: String? = null,
    val intent: String? = null,
    val status: String? = null,
    val error_message: String? = null,
    val result: JsonMap? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class SubmitCommandResultDto(
    val status: String? = null,
    val command_id: String? = null,
    val wf_key: String? = null,
    val command: ApiCommandRecordDto? = null
)

// ============================================================================
// GAP #5.2: ProjectDto — no phantom "status" field, use is_active
// ============================================================================
data class ProjectDto(
    val id: String,
    val name: String,
    val is_active: Boolean? = true,
    val story_bible: JsonMap? = null,
    val current_season: Int? = 1,
    val current_episode: Int? = 1
)

data class ProjectOverviewDto(
    val project: ProjectDto? = null,
    val stats: JsonMap? = null,
    val episodes_count: Int? = null,
    val characters_count: Int? = null,
    val pending_approvals: Int? = null,
    val active_jobs: Int? = null,
    val canon_status: String? = null,
    val continuity_status: String? = null
)

// ============================================================================
// EPISODE
// ============================================================================
data class EpisodeDto(
    val id: String,
    val project_id: String? = null,
    val episode_no: Int,
    val title: String? = null,
    val synopsis: String? = null,
    val current_state: String? = null,
    val qc_status: String? = null,
    val updated_at: String? = null
)

// ============================================================================
// SCENE & CLIP
// ============================================================================
data class ClipDto(
    val id: String? = null,
    val scene_order: Int? = null,
    val scene_no: Int? = null,
    val clip_url: String? = null,
    val provider: String? = null,
    val duration_sec: Double? = null,
    val status: String? = null
)

// GAP #5.3: SceneDto — no phantom "title" column (scenes table doesn't have it)
data class SceneDto(
    val id: String,
    val episode_no: Int? = null,
    val scene_order: Int? = null,
    val scene_no: Int? = null,
    val description: String? = null,
    val visual_prompt: String? = null,
    val status: String? = null,
    val clips: List<ClipDto>? = null
)

// ============================================================================
// CHARACTER
// ============================================================================
data class CharacterDto(
    val id: String? = null,
    val character_code: String,
    val name: String,
    val role: String? = null,
    val visual_dna: JsonMap? = null,
    val voice_config: JsonMap? = null
)

// ============================================================================
// GAP #5.4: JobDto — aligned with backend listJobs fields
// ============================================================================
data class JobDto(
    val id: String,
    val project_id: String? = null,
    val intent: String? = null,
    val status: String? = null,
    val episode_no: Int? = null,
    val episode_state: JsonMap? = null,
    val pending_actions: List<JsonMap>? = null,
    val attempt_count: Int? = 0,
    val max_attempts: Int? = 3,
    val error_message: String? = null,
    val created_at: String? = null
)

// ============================================================================
// GAP #5.1: ApprovalDecisionDto — decision_reason mapping, resource_type computed
// ============================================================================
data class ApprovalDecisionDto(
    val id: String,
    val project_id: String? = null,
    val type: String = "APPROVAL",
    val resource_id: String? = null,
    val resource_version: Int = 1,
    val status: String? = null,
    val risk: String? = null,
    @SerializedName("decision_reason", alternate = ["reason"])
    val reason: String? = null,
    val decision: String? = null,
    val exact_diff: JsonMap? = null,
    val created_at: String? = null
) {
    val resource_type: String
        get() = type
}

data class DecideApprovalRequest(
    val decision: String,
    val reason: String,
    val resource_version: Int = 1
)

// ============================================================================
// QUICK MEDIA & VISION
// ============================================================================
data class QuickGenerateRequest(
    val prompt: String,
    val style_preset: String? = "anime_cinematic",
    val aspect_ratio: String? = "16:9",
    val duration_sec: Int? = null
)

data class VisionAnalyzeRequest(
    val media_url: String,
    val analysis_type: String = "GENERAL",
    val prompt: String = "Extract 11-field Visual DNA"
)

// ============================================================================
// PROMPT STUDIO
// ============================================================================
data class SystemPromptDto(
    val id: String,
    val project_id: String? = null,
    val title: String,
    val visual_medium: String = "ANIME",
    val prompt_text: String,
    val is_default: Boolean? = false
)

data class CreateSystemPromptRequest(
    val project_id: String? = null,
    val title: String,
    val visual_medium: String,
    val prompt_text: String,
    val set_active: Boolean = true
)

// ============================================================================
// GPU / KAGGLE / TELEGRAM STORAGE CONFIG
// ============================================================================
data class CustomGpuConfigDto(
    val enabled: Boolean = false,
    val worker_url: String = "",
    val api_key: String? = null
)

data class KaggleAccountConfigDto(
    val username: String = "",
    val api_key: String = "",
    val notebook_slug: String? = "anime-gpu-worker",
    val worker_url: String? = null
)

data class TelegramStorageConfigDto(
    val bot_token: String = "",
    val storage_chat_id: String = "",
    val stream_bridge_base_url: String? = "https://api.bnai.dpdns.org/v1/media/telegram"
)

data class GpuSetupRequest(
    val preferred_mode: String = "KAGGLE_AUTO",
    val models_dataset_slug: String? = "bnaisolution/anime-engine-models-v1",
    val auto_deploy_kaggle: Boolean = true,
    val custom_gpu: CustomGpuConfigDto? = null,
    val kaggle_primary: KaggleAccountConfigDto? = null,
    val kaggle_secondary: KaggleAccountConfigDto? = null,
    val telegram_storage: TelegramStorageConfigDto? = null,
    val github_worker_url: String? = null
)

data class GpuSetupResponseDto(
    val ok: Boolean = false,
    val preferred_mode: String? = null,
    val kaggle_deploy: JsonMap? = null
)

// ============================================================================
// RETROFIT INTERFACE — All endpoints including 13 GAP #2 additions
// ============================================================================
interface AnimeApiService {

    // --- Auth ---
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<AuthMeResponse>

    // --- AI Brain ---
    @POST("api/v1/ai/chat")
    suspend fun chatWithAi(@Body request: AiChatRequest): Response<AiChatResponse>

    // --- Health ---
    @GET("v1/anime/health")
    suspend fun getAnimeHealth(): Response<HealthResponseDto>

    // --- Commands (WF-00) ---
    @POST("v1/anime/commands")
    suspend fun submitCommand(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: CreateCommandRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @GET("v1/anime/commands/{id}")
    suspend fun getCommandStatus(
        @Path("id") commandId: String
    ): Response<ApiEnvelope<ApiCommandRecordDto>>

    // --- Projects & Story ---
    @GET("v1/anime/projects")
    suspend fun getProjects(): Response<ApiEnvelope<List<ProjectDto>>>

    @GET("v1/anime/projects/{id}/overview")
    suspend fun getProjectOverview(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<ProjectOverviewDto>>

    @GET("v1/anime/projects/{id}/episodes")
    suspend fun getProjectEpisodes(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<EpisodeDto>>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}")
    suspend fun getEpisodeDetail(
        @Path("id") projectId: String,
        @Path("episodeNo") episodeNo: Int
    ): Response<ApiEnvelope<EpisodeDto>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}/plan")
    suspend fun getEpisodePlan(
        @Path("id") projectId: String,
        @Path("episodeNo") episodeNo: Int
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/episodes/{episodeNo}/scenes")
    suspend fun getEpisodeScenes(
        @Path("id") projectId: String,
        @Path("episodeNo") episodeNo: Int
    ): Response<ApiEnvelope<List<SceneDto>>>

    @GET("v1/anime/projects/{id}/jobs")
    suspend fun getProjectJobs(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JobDto>>>

    @GET("v1/anime/projects/{id}/jobs/{jobId}")
    suspend fun getJobDetail(
        @Path("id") projectId: String,
        @Path("jobId") jobId: String
    ): Response<ApiEnvelope<JobDto>>

    @GET("v1/anime/projects/{id}/bible")
    suspend fun getProjectBible(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/canon")
    suspend fun getProjectCanon(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/characters")
    suspend fun getProjectCharacters(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<CharacterDto>>>

    @GET("v1/anime/projects/{id}/timeline")
    suspend fun getProjectTimeline(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/analytics")
    suspend fun getProjectAnalytics(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/audit")
    suspend fun getProjectAudit(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    // --- Quick Tools & Vision (WF-05) ---
    @POST("v1/anime/projects/{id}/quick/image")
    suspend fun quickGenerateImage(
        @Path("id") projectId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: QuickGenerateRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @POST("v1/anime/projects/{id}/quick/video")
    suspend fun quickGenerateVideo(
        @Path("id") projectId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: QuickGenerateRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    @POST("v1/anime/projects/{id}/vision/analyze")
    suspend fun analyzeVision(
        @Path("id") projectId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: VisionAnalyzeRequest
    ): Response<ApiEnvelope<SubmitCommandResultDto>>

    // --- Approvals (WF-11) ---
    @GET("v1/anime/projects/{id}/approvals")
    suspend fun getProjectApprovals(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<ApprovalDecisionDto>>>

    @POST("v1/anime/projects/{id}/approvals/{approvalId}/decisions")
    suspend fun decideApproval(
        @Path("id") projectId: String,
        @Path("approvalId") approvalId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: DecideApprovalRequest
    ): Response<ApiEnvelope<ApprovalDecisionDto>>

    // --- Admin & Owner ---
    @GET("v1/anime/admin/workflows")
    suspend fun getAdminWorkflows(
        @Query("project_id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/admin/queues")
    suspend fun getAdminQueues(
        @Query("project_id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/admin/anime/workers")
    suspend fun getAdminWorkers(
        @Query("project_id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/admin/anime/ledger/summary")
    suspend fun getLedgerSummary(
        @Query("project_id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    // --- Prompts & GPU Config ---
    @GET("v1/anime/prompts")
    suspend fun getSystemPrompts(
        @Query("project_id") projectId: String? = null
    ): Response<ApiEnvelope<List<SystemPromptDto>>>

    @POST("v1/anime/prompts")
    suspend fun createSystemPrompt(
        @Body body: CreateSystemPromptRequest
    ): Response<ApiEnvelope<SystemPromptDto>>

    @GET("v1/anime/infrastructure/gpu")
    suspend fun getGpuConfig(): Response<ApiEnvelope<JsonMap>>

    @POST("v1/anime/infrastructure/gpu")
    suspend fun saveGpuConfig(
        @Body request: GpuSetupRequest
    ): Response<ApiEnvelope<GpuSetupResponseDto>>

    // ========================================================================
    // GAP #2: 13 NEW READ ENDPOINTS (must be deployed on backend first)
    // ========================================================================
    @GET("v1/anime/projects/{id}/quota")
    suspend fun getProjectQuota(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<JsonMap>>

    @GET("v1/anime/projects/{id}/continuity")
    suspend fun getProjectContinuity(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/retries")
    suspend fun getProjectRetries(
        @Path("id") projectId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/characters/{characterId}/reference-history")
    suspend fun getCharacterReferenceHistory(
        @Path("id") projectId: String,
        @Path("characterId") characterId: String
    ): Response<ApiEnvelope<List<JsonMap>>>

    @GET("v1/anime/projects/{id}/{category}")
    suspend fun getProjectCategoryList(
        @Path("id") projectId: String,
        @Path("category") category: String
    ): Response<ApiEnvelope<List<JsonMap>>>
}
EOF

echo "OK: AnimeApiService.kt created"

echo ""
echo "========== PART 2 COMPLETE =========="
# ==============================================================================
# STEP 10: AnimeRepository.kt (GAP #4.1 & #5.2 fixes — no ep.id fallback, no phantom status)
# ==============================================================================
cat > "$BASE_DIR/core/network/AnimeRepository.kt" << 'EOF'
package org.dpdns.bnai.anime.core.network

import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project
import retrofit2.Response
import java.util.UUID

class AnimeRepository(
    private val apiService: AnimeApiService = RetrofitClient.getApiService()
) {

    fun currentProjectId(): String =
        TokenManager.getInstance()?.getActiveProjectId().orEmpty()

    private fun <T, R> mapEnvelope(
        response: Response<ApiEnvelope<T>>,
        transform: (T) -> R
    ): Resource<R> {
        if (!response.isSuccessful) {
            return Resource.Error(
                "HTTP ${response.code()}: ${response.message()}",
                response.code()
            )
        }
        val data = response.body()?.data ?: return Resource.Empty
        if (data is Collection<*> && data.isEmpty()) return Resource.Empty
        if (data is Map<*, *> && data.isEmpty()) return Resource.Empty
        return Resource.Success(transform(data))
    }

    // ========================================================================
    // PROJECTS
    // ========================================================================
    suspend fun fetchProjects(): Resource<List<Project>> = try {
        val response = apiService.getProjects()
        if (response.isSuccessful) {
            val dtoList = response.body()?.data.orEmpty()
            if (dtoList.isEmpty()) {
                Resource.Empty
            } else {
                val activeId = currentProjectId()
                if (activeId.isBlank() || dtoList.none { it.id == activeId }) {
                    TokenManager.getInstance()?.saveActiveProjectId(dtoList.first().id)
                }
                Resource.Success(dtoList.map { dto ->
                    Project(
                        id = dto.id,
                        name = dto.name,
                        activeEpisode = "Season ${dto.current_season ?: 1} • Episode ${dto.current_episode ?: 1}",
                        // GAP #5.2: Derive status from is_active — backend has no "status" column
                        status = if (dto.is_active == true) "ACTIVE" else "INACTIVE"
                    )
                })
            }
        } else {
            Resource.Error("HTTP ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Network failure")
    }

    suspend fun fetchProjectOverview(projectId: String): Resource<ProjectOverviewDto> = try {
        mapEnvelope(apiService.getProjectOverview(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load overview")
    }

    // ========================================================================
    // STORY BIBLE
    // ========================================================================
    suspend fun fetchBible(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectBible(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load Story Bible")
    }

    // ========================================================================
    // CHARACTERS
    // ========================================================================
    suspend fun fetchCharacters(projectId: String): Resource<List<Character>> = try {
        mapEnvelope(apiService.getProjectCharacters(projectId)) { list ->
            list.map { dto ->
                val dnaText = dto.visual_dna?.entries?.joinToString("\n") {
                    "${it.key}: ${it.value}"
                } ?: "No visual DNA stored"
                val voiceText = dto.voice_config?.get("voice_id")?.toString() ?: "Unassigned"
                Character(
                    id = dto.character_code.ifBlank { dto.id ?: "UNKNOWN" },
                    name = dto.name,
                    role = dto.role ?: "Cast",
                    visualDna = dnaText,
                    voiceId = voiceText
                )
            }
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load characters")
    }

    // ========================================================================
    // EPISODES
    // ========================================================================
    suspend fun fetchEpisodes(projectId: String): Resource<List<Episode>> = try {
        mapEnvelope(apiService.getProjectEpisodes(projectId)) { list ->
            list.map { dto ->
                Episode(
                    id = dto.id,
                    episodeNumber = dto.episode_no,
                    title = dto.title ?: "Episode ${dto.episode_no}",
                    status = dto.current_state ?: "DRAFT",
                    synopsis = dto.synopsis,
                    updatedAt = dto.updated_at
                )
            }
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load episodes")
    }

    // ========================================================================
    // SCENES
    // ========================================================================
    suspend fun fetchScenes(projectId: String, episodeNo: Int): Resource<List<SceneDto>> = try {
        mapEnvelope(apiService.getEpisodeScenes(projectId, episodeNo)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load scenes")
    }

    // ========================================================================
    // JOBS
    // ========================================================================
    suspend fun fetchJobs(projectId: String): Resource<List<JobDto>> = try {
        mapEnvelope(apiService.getProjectJobs(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load jobs")
    }

    // ========================================================================
    // RETRIES
    // ========================================================================
    suspend fun fetchRetries(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectRetries(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load retry history")
    }

    // ========================================================================
    // APPROVALS
    // ========================================================================
    suspend fun fetchApprovals(projectId: String): Resource<List<ApprovalDecisionDto>> = try {
        mapEnvelope(apiService.getProjectApprovals(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load approvals")
    }

    suspend fun submitApprovalDecision(
        projectId: String,
        approvalId: String,
        decision: String,
        reason: String,
        resourceVersion: Int = 1
    ): Resource<ApprovalDecisionDto> = try {
        val key = "apr-${UUID.randomUUID()}"
        mapEnvelope(
            apiService.decideApproval(
                projectId,
                approvalId,
                key,
                DecideApprovalRequest(decision, reason, resourceVersion)
            )
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to submit decision")
    }

    // ========================================================================
    // AI BRAIN
    // ========================================================================
    suspend fun chatWithBrain(
        message: String,
        projectId: String = currentProjectId()
    ): Resource<AiChatResponse> = try {
        val req = AiChatRequest(
            message = message,
            project_id = projectId.ifBlank { null },
            skill_key = "ai_admin",
            request_id = UUID.randomUUID().toString()
        )
        val response = apiService.chatWithAi(req)
        if (response.isSuccessful && response.body() != null) {
            Resource.Success(response.body()!!)
        } else {
            Resource.Error("AI Brain HTTP ${response.code()}", response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "AI Brain unreachable")
    }

    // ========================================================================
    // COMMANDS (WF-00 — idempotency enforced)
    // ========================================================================
    suspend fun submitStudioCommand(
        projectId: String,
        intent: CommandIntent,
        mode: String = "SAVE",
        episodeNo: Int? = null,
        payload: JsonMap = emptyMap(),
        customIdempotencyKey: String? = null
    ): Resource<SubmitCommandResultDto> {
        if (projectId.isBlank()) {
            return Resource.Error("Please select or create a project first.")
        }
        val idempotencyKey = customIdempotencyKey
            ?: "cmd-${intent.name.lowercase()}-${UUID.randomUUID()}"
        return try {
            val response = apiService.submitCommand(
                idempotencyKey,
                CreateCommandRequest(projectId, intent.name, mode, episodeNo, payload)
            )
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                Resource.Error("Command rejected (HTTP ${response.code()})", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Command dispatch failed")
        }
    }

    suspend fun fetchCommandStatus(commandId: String): Resource<ApiCommandRecordDto> = try {
        mapEnvelope(apiService.getCommandStatus(commandId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Status poll failed")
    }

    // ========================================================================
    // QUICK MEDIA & VISION (WF-05)
    // ========================================================================
    suspend fun triggerQuickMedia(
        projectId: String,
        isVideo: Boolean,
        prompt: String
    ): Resource<SubmitCommandResultDto> = try {
        val key = "quick-${if (isVideo) "vid" else "img"}-${UUID.randomUUID()}"
        val response = if (isVideo) {
            apiService.quickGenerateVideo(
                projectId,
                key,
                QuickGenerateRequest(prompt = prompt, duration_sec = 5)
            )
        } else {
            apiService.quickGenerateImage(
                projectId,
                key,
                QuickGenerateRequest(prompt = prompt)
            )
        }
        mapEnvelope(response) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Quick media request failed")
    }

    suspend fun triggerVisionAnalysis(
        projectId: String,
        mediaUrl: String,
        prompt: String = "Extract 11-field Visual DNA"
    ): Resource<SubmitCommandResultDto> = try {
        val key = "vision-${UUID.randomUUID()}"
        mapEnvelope(
            apiService.analyzeVision(
                projectId,
                key,
                VisionAnalyzeRequest(media_url = mediaUrl, prompt = prompt)
            )
        ) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Vision request failed")
    }

    // ========================================================================
    // ANALYTICS & AUDIT (WF-08)
    // ========================================================================
    suspend fun fetchAnalytics(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectAnalytics(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load analytics")
    }

    suspend fun fetchAuditLogs(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectAudit(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load audit logs")
    }

    // ========================================================================
    // ADMIN — WORKFLOWS, WORKERS, QUEUES, LEDGER
    // ========================================================================
    suspend fun fetchAdminWorkflows(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getAdminWorkflows(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load workflows")
    }

    suspend fun fetchAdminWorkers(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getAdminWorkers(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load workers")
    }

    suspend fun fetchAdminQueues(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getAdminQueues(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load queues")
    }

    suspend fun fetchLedgerSummary(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getLedgerSummary(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load ledger summary")
    }

    // ========================================================================
    // GAP #2 NEW READS — QUOTA, CONTINUITY, CANON, CATEGORIES, REFERENCE HISTORY
    // ========================================================================
    suspend fun fetchQuota(projectId: String): Resource<JsonMap> = try {
        mapEnvelope(apiService.getProjectQuota(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load quota")
    }

    suspend fun fetchCanon(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectCanon(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load canon")
    }

    suspend fun fetchContinuity(projectId: String): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectContinuity(projectId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load continuity checks")
    }

    suspend fun fetchCategoryItems(
        projectId: String,
        categoryPath: String
    ): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getProjectCategoryList(projectId, categoryPath)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load $categoryPath")
    }

    suspend fun fetchCharacterReferenceHistory(
        projectId: String,
        characterId: String
    ): Resource<List<JsonMap>> = try {
        mapEnvelope(apiService.getCharacterReferenceHistory(projectId, characterId)) { it }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Failed to load character reference history")
    }

    // ========================================================================
    // HEALTH
    // ========================================================================
    suspend fun fetchHealthDetail(): Resource<HealthResponseDto> = try {
        val res = apiService.getAnimeHealth()
        if (res.isSuccessful && res.body() != null) {
            Resource.Success(res.body()!!)
        } else {
            Resource.Error("Health HTTP ${res.code()}", res.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Backend unreachable")
    }

    suspend fun checkBackendHealth(): Boolean =
        (fetchHealthDetail() as? Resource.Success)?.data?.ok == true
}
EOF

echo "OK: AnimeRepository.kt created"

echo ""
echo "========== PART 3 COMPLETE =========="
# ==============================================================================
# STEP 11: Auth (ViewModel + Screen) — Patch 2 (401 session expiry) integrated
# ==============================================================================
cat > "$BASE_DIR/ui/auth/AuthViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.AnimeApiService
import org.dpdns.bnai.anime.core.network.LoginRequest
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.RetrofitClient

class AuthViewModel(
    private val apiService: AnimeApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val authState: StateFlow<Resource<String>> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.trim().isEmpty() || password.isEmpty()) {
            _authState.value = Resource.Error("Email and password are required.")
            return
        }
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val response = apiService.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.token?.takeIf { it.isNotBlank() }
                        ?: body?.access_token?.takeIf { it.isNotBlank() }
                    if (token != null) {
                        tokenManager.saveToken(token)
                        tokenManager.saveUserMeta(
                            body?.userId,
                            body?.workspaceId,
                            body?.role,
                            body?.email ?: email.trim()
                        )
                        _authState.value = Resource.Success(token)
                    } else {
                        _authState.value = Resource.Error("Login response did not include a token.")
                    }
                } else {
                    _authState.value = Resource.Error(
                        "Login failed (HTTP ${response.code()})",
                        response.code()
                    )
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(e.localizedMessage ?: "Network error")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            return AuthViewModel(
                RetrofitClient.getApiService(appContext),
                TokenManager.init(appContext)
            ) as T
        }
    }
}
EOF

cat > "$BASE_DIR/ui/auth/AuthScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(LocalContext.current))
) {
    val authState by viewModel.authState.collectAsState()
    val tokenManager = remember { TokenManager.getInstance() }
    val expiredMsg by (
        tokenManager?.sessionExpiredMessage?.collectAsState()
            ?: remember { mutableStateOf(null) }
    )
    val snackbarHostState = remember { SnackbarHostState() }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(expiredMsg) {
        expiredMsg?.let {
            snackbarHostState.showSnackbar(it)
            tokenManager?.consumeSessionExpiredMessage()
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is Resource.Success -> {
                navController.navigate("studio") {
                    popUpTo("auth") { inclusive = true }
                }
            }
            is Resource.Error -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "AI Cinematic Anime Engine",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Standalone Anime Production Studio", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (isPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(if (isPasswordVisible) "Hide" else "Show")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            val isLoading = authState is Resource.Loading
            Button(
                onClick = { viewModel.login(email, password) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign In")
                }
            }
        }
    }
}
EOF

echo "OK: Auth files created"

# ==============================================================================
# STEP 12: Studio (ViewModel + Dashboard)
# ==============================================================================
cat > "$BASE_DIR/ui/studio/StudioViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource

data class HomeSummaryUi(
    val activeProjectId: String = "",
    val recentEpisodes: List<Episode> = emptyList(),
    val pendingApprovalsCount: Int = 0,
    val activeProductionStatus: String = "IDLE",
    val createStatusMessage: String? = null
)

class StudioViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _projectsState = MutableStateFlow<Resource<List<Project>>>(Resource.Loading)
    val projectsState: StateFlow<Resource<List<Project>>> = _projectsState.asStateFlow()

    private val _homeSummary = MutableStateFlow(HomeSummaryUi())
    val homeSummary: StateFlow<HomeSummaryUi> = _homeSummary.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _projectsState.value = Resource.Loading
            val res = repository.fetchProjects()
            _projectsState.value = res
            if (res is Resource.Success && res.data.isNotEmpty()) {
                val activeId = repository.currentProjectId().ifBlank { res.data.first().id }
                loadProjectHomeMetrics(activeId)
            }
        }
    }

    fun selectProject(projectId: String) {
        TokenManager.getInstance()?.saveActiveProjectId(projectId)
        loadProjectHomeMetrics(projectId)
    }

    fun loadProjectHomeMetrics(projectId: String) {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            val episodesRes = repository.fetchEpisodes(projectId)
            val approvalsRes = repository.fetchApprovals(projectId)
            val jobsRes = repository.fetchJobs(projectId)

            val recentEps = (episodesRes as? Resource.Success)?.data?.take(4).orEmpty()
            val pendingCount = (approvalsRes as? Resource.Success)?.data?.count {
                it.status == null || it.status.equals("PENDING", ignoreCase = true)
            } ?: 0
            val latestJobState = (jobsRes as? Resource.Success)?.data?.firstOrNull()?.status ?: "IDLE"

            _homeSummary.value = _homeSummary.value.copy(
                activeProjectId = projectId,
                recentEpisodes = recentEps,
                pendingApprovalsCount = pendingCount,
                activeProductionStatus = latestJobState
            )
        }
    }

    fun createNewProject(projectName: String) {
        val cleanName = projectName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val baseProjectId = repository.currentProjectId().ifBlank {
                "00000000-0000-0000-0000-000000000000"
            }
            when (
                val cmd = repository.submitStudioCommand(
                    projectId = baseProjectId,
                    intent = CommandIntent.CREATE_PROJECT,
                    mode = "SAVE",
                    payload = mapOf("name" to cleanName)
                )
            ) {
                is Resource.Success -> {
                    _homeSummary.value = _homeSummary.value.copy(
                        createStatusMessage = "Create Project command dispatched (${cmd.data.status ?: "QUEUED"})"
                    )
                    loadProjects()
                }
                is Resource.Error -> {
                    _homeSummary.value = _homeSummary.value.copy(
                        createStatusMessage = "Create failed: ${cmd.message}"
                    )
                }
                else -> Unit
            }
        }
    }
}
EOF

cat > "$BASE_DIR/ui/studio/StudioDashboardScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.studio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun StudioDashboardScreen(
    onProjectClick: (String) -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenEpisodes: () -> Unit = {},
    viewModel: StudioViewModel = viewModel()
) {
    val projectsResource by viewModel.projectsState.collectAsState()
    val summary by viewModel.homeSummary.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Anime Project") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createNewProject(newProjectName)
                    newProjectName = ""
                    showCreateDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Project") },
                text = { Text("Create Project") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            summary.createStatusMessage?.let { msg ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )) {
                    Text(msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Production State", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusBadge(summary.activeProductionStatus)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).clickable { onOpenReviews() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pending Approvals", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${summary.pendingApprovalsCount} awaiting review",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }

            if (summary.recentEpisodes.isNotEmpty()) {
                Text("Recent Episodes", style = MaterialTheme.typography.headlineMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(summary.recentEpisodes, key = { it.id }) { ep ->
                        Card(
                            modifier = Modifier.width(200.dp).clickable { onOpenEpisodes() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Ep ${ep.episodeNumber}: ${ep.title}",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                StatusBadge(ep.status)
                            }
                        }
                    }
                }
            }

            ResourceStateBox(
                resource = projectsResource,
                emptyMessage = "No projects found in Anime Supabase. Tap 'Create Project' to begin.",
                onRetry = { viewModel.loadProjects() }
            ) { projects ->
                val activeProject = projects.find { it.id == summary.activeProjectId }
                    ?: projects.firstOrNull()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Projects (${projects.size})", style = MaterialTheme.typography.headlineMedium)
                        Box {
                            OutlinedButton(onClick = { projectDropdownExpanded = true }) {
                                Text("Active: ${activeProject?.name ?: "Select"}")
                            }
                            DropdownMenu(
                                expanded = projectDropdownExpanded,
                                onDismissRequest = { projectDropdownExpanded = false }
                            ) {
                                projects.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = {
                                            viewModel.selectProject(p.id)
                                            projectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(projects, key = { it.id }) { project ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.selectProject(project.id)
                                    onProjectClick(project.id)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(project.name, style = MaterialTheme.typography.headlineMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(project.activeEpisode, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    StatusBadge(project.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: Studio files created"

# ==============================================================================
# STEP 13: Prompt Studio (ViewModel + Screen)
# ==============================================================================
cat > "$BASE_DIR/ui/studio/PromptStudioViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.CreateSystemPromptRequest
import org.dpdns.bnai.anime.core.network.RetrofitClient
import org.dpdns.bnai.anime.core.network.SystemPromptDto

data class PromptStudioUiState(
    val isLoading: Boolean = false,
    val prompts: List<SystemPromptDto> = emptyList(),
    val selectedVisualMedium: String = "ANIME",
    val selectedPrompt: SystemPromptDto? = null,
    val statusMessage: String? = null
)

class PromptStudioViewModel : ViewModel() {
    private val api = RetrofitClient.getApiService()
    private val _uiState = MutableStateFlow(PromptStudioUiState())
    val uiState: StateFlow<PromptStudioUiState> = _uiState.asStateFlow()

    init {
        loadPrompts()
    }

    fun loadPrompts() {
        val projectId = TokenManager.getInstance()?.getActiveProjectId()?.ifBlank { null }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = null)
            try {
                val res = api.getSystemPrompts(projectId)
                if (res.isSuccessful) {
                    val list = res.body()?.data.orEmpty()
                    val active = list.firstOrNull { it.is_default == true } ?: list.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        prompts = list,
                        selectedPrompt = active,
                        selectedVisualMedium = active?.visual_medium
                            ?: _uiState.value.selectedVisualMedium
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Failed to load prompts (HTTP ${res.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = e.localizedMessage ?: "Network error"
                )
            }
        }
    }

    fun setVisualMedium(medium: String) {
        _uiState.value = _uiState.value.copy(selectedVisualMedium = medium)
    }

    fun selectPrompt(prompt: SystemPromptDto) {
        _uiState.value = _uiState.value.copy(
            selectedPrompt = prompt,
            selectedVisualMedium = prompt.visual_medium
        )
    }

    fun createAndActivatePrompt(title: String, promptText: String) {
        if (title.isBlank() || promptText.isBlank()) return
        val projectId = TokenManager.getInstance()?.getActiveProjectId()?.ifBlank { null }
        val medium = _uiState.value.selectedVisualMedium
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = null)
            try {
                val req = CreateSystemPromptRequest(
                    projectId, title.trim(), medium, promptText.trim(), true
                )
                val res = api.createSystemPrompt(req)
                val created = res.body()?.data
                if (res.isSuccessful && created != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        prompts = listOf(created) + _uiState.value.prompts,
                        selectedPrompt = created,
                        statusMessage = "Saved & Activated: ${created.title}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Save failed (HTTP ${res.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = e.localizedMessage ?: "Error saving prompt"
                )
            }
        }
    }
}
EOF

cat > "$BASE_DIR/ui/studio/PromptStudioScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.studio

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PromptStudioScreen(viewModel: PromptStudioViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var title by remember(state.selectedPrompt) {
        mutableStateOf(state.selectedPrompt?.title ?: "")
    }
    var promptText by remember(state.selectedPrompt) {
        mutableStateOf(state.selectedPrompt?.prompt_text ?: "")
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("System Prompt Studio", style = MaterialTheme.typography.headlineLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ANIME", "REAL_HUMAN").forEach { medium ->
                FilterChip(
                    selected = state.selectedVisualMedium == medium,
                    onClick = { viewModel.setVisualMedium(medium) },
                    label = { Text(medium) }
                )
            }
        }
        state.statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Prompt Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            label = { Text("System Prompt Instructions") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.createAndActivatePrompt(title, promptText) },
            enabled = !state.isLoading && title.isNotBlank() && promptText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (state.isLoading) "Saving..." else "Save & Set Active Prompt")
        }
    }
}
EOF

echo "OK: PromptStudio files created"

echo ""
echo "========== PART 4 COMPLETE =========="
# ==============================================================================
# STEP 14: Project (ViewModel + Bible + Overview)
# ==============================================================================
cat > "$BASE_DIR/ui/project/ProjectViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.ProjectOverviewDto
import org.dpdns.bnai.anime.core.network.Resource

data class StoryBibleData(
    val projectId: String,
    val universeTitle: String,
    val worldRules: String,
    val magicSystem: String,
    val toneAndTheme: String
)

class ProjectViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _overviewState = MutableStateFlow<Resource<ProjectOverviewDto>>(Resource.Loading)
    val overviewState: StateFlow<Resource<ProjectOverviewDto>> = _overviewState.asStateFlow()

    private val _bibleState = MutableStateFlow<Resource<StoryBibleData>>(Resource.Loading)
    val bibleState: StateFlow<Resource<StoryBibleData>> = _bibleState.asStateFlow()

    fun fetchOverview(projectId: String) {
        val targetId = projectId.ifBlank { repository.currentProjectId() }
        if (targetId.isBlank()) {
            _overviewState.value = Resource.Error("No active project selected.")
            return
        }
        viewModelScope.launch {
            _overviewState.value = Resource.Loading
            _overviewState.value = repository.fetchProjectOverview(targetId)
        }
    }

    fun fetchBible(projectId: String) {
        val targetId = projectId.ifBlank { repository.currentProjectId() }
        if (targetId.isBlank()) {
            _bibleState.value = Resource.Error("No active project selected.")
            return
        }
        viewModelScope.launch {
            _bibleState.value = Resource.Loading
            when (val res = repository.fetchBible(targetId)) {
                is Resource.Success -> {
                    val data = res.data
                    val rawBible = data["story_bible"] as? Map<*, *> ?: data
                    val title = (
                        rawBible["title"] ?: data["name"] ?: "Project Story Bible"
                    ).toString()
                    val overview = (
                        rawBible["world_overview"] ?: rawBible["logline"] ?: ""
                    ).toString()
                    val powers = (rawBible["magic_system"] ?: "").toString()
                    val tone = (rawBible["tone"] ?: "").toString()
                    _bibleState.value = Resource.Success(
                        StoryBibleData(targetId, title, overview, powers, tone)
                    )
                }
                is Resource.Empty -> _bibleState.value = Resource.Empty
                is Resource.Error -> _bibleState.value = res
                else -> Unit
            }
        }
    }

    fun proposeBibleChange(
        projectId: String,
        summary: String,
        onDone: (String) -> Unit
    ) {
        val targetId = projectId.ifBlank { repository.currentProjectId() }
        viewModelScope.launch {
            when (
                val res = repository.submitStudioCommand(
                    projectId = targetId,
                    intent = CommandIntent.SAVE_BIBLE,
                    mode = "SAVE",
                    payload = mapOf("user_prompt" to summary)
                )
            ) {
                is Resource.Success -> onDone(
                    "WF-01 SAVE_BIBLE Dispatched (${res.data.status ?: "QUEUED"})"
                )
                is Resource.Error -> onDone("Failed: ${res.message}")
                else -> Unit
            }
        }
    }
}
EOF

cat > "$BASE_DIR/ui/project/BibleScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.project

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun BibleScreen(
    projectId: String = TokenManager.getInstance()?.getActiveProjectId().orEmpty(),
    viewModel: ProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val bibleResource by viewModel.bibleState.collectAsState()
    var worldRules by remember { mutableStateOf("") }
    var magicSystem by remember { mutableStateOf("") }
    var toneAndTheme by remember { mutableStateOf("") }

    LaunchedEffect(projectId) { viewModel.fetchBible(projectId) }
    LaunchedEffect(bibleResource) {
        if (bibleResource is Resource.Success) {
            val data = (bibleResource as Resource.Success<StoryBibleData>).data
            worldRules = data.worldRules
            magicSystem = data.magicSystem
            toneAndTheme = data.toneAndTheme
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ResourceStateBox(
            resource = bibleResource,
            emptyMessage = "No Story Bible persisted yet. Enter details below and dispatch WF-01.",
            onRetry = { viewModel.fetchBible(projectId) }
        ) { bible ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(bible.universeTitle, style = MaterialTheme.typography.headlineLarge)
                OutlinedTextField(
                    value = worldRules,
                    onValueChange = { worldRules = it },
                    label = { Text("World Rules & Logline") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = magicSystem,
                    onValueChange = { magicSystem = it },
                    label = { Text("Magic / Power System") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = toneAndTheme,
                    onValueChange = { toneAndTheme = it },
                    label = { Text("Tone & Theme") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.proposeBibleChange(
                            projectId,
                            "World: $worldRules | Powers: $magicSystem | Tone: $toneAndTheme"
                        ) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Story Bible (WF-01)")
                }
            }
        }
    }
}
EOF

cat > "$BASE_DIR/ui/project/ProjectDetailScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun ProjectDetailScreen(
    projectId: String = TokenManager.getInstance()?.getActiveProjectId().orEmpty(),
    onOpenBible: () -> Unit = {},
    onOpenCharacters: () -> Unit = {},
    onOpenEpisodes: () -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    viewModel: ProjectViewModel = viewModel()
) {
    val overviewState by viewModel.overviewState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.fetchOverview(projectId)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Project Overview", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(
            resource = overviewState,
            onRetry = { viewModel.fetchOverview(projectId) }
        ) { overview ->
            val stats = overview.stats.orEmpty()
            val totalEps = stats["total_episodes"] ?: overview.episodes_count ?: 0
            val totalScenes = stats["total_scenes"] ?: 0
            val pendingActs = stats["pending_actions_count"] ?: overview.pending_approvals ?: 0
            val activeJobs = stats["active_jobs_count"] ?: overview.active_jobs ?: 0

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            overview.project?.name ?: "Project $projectId",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // GAP #5.2: status derived from is_active only
                            StatusBadge(
                                if (overview.project?.is_active == true) "ACTIVE" else "INACTIVE"
                            )
                            overview.canon_status?.let { StatusBadge("Canon: $it") }
                            overview.continuity_status?.let { StatusBadge("Continuity: $it") }
                        }
                        Text("Episodes: $totalEps • Scenes: $totalScenes")
                        Text("Active Jobs: $activeJobs • Pending Actions/Approvals: $pendingActs")
                    }
                }
                Button(
                    onClick = onOpenBible,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Story Bible (WF-01)") }
                Button(
                    onClick = onOpenCharacters,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Characters (WF-04)") }
                Button(
                    onClick = onOpenEpisodes,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Episode Planner (WF-02)") }
                OutlinedButton(
                    onClick = onOpenReviews,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Review & Approval Inbox (WF-11)") }
                OutlinedButton(
                    onClick = onOpenAnalytics,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Analytics (WF-08)") }
            }
        }
    }
}
EOF

echo "OK: Project files created"

# ==============================================================================
# STEP 15: Character (List + Detail)
# ==============================================================================
cat > "$BASE_DIR/ui/character/CharacterListScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun CharacterListScreen(
    onCharacterClick: (Character) -> Unit = {},
    onOpenDeepDive: (String) -> Unit = {}
) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<Resource<List<Character>>>(Resource.Loading) }

    fun load() {
        scope.launch {
            state = Resource.Loading
            state = repository.fetchCharacters(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Characters", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(resource = state, onRetry = { load() }) { list ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { ch ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onCharacterClick(ch) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "${ch.name} (${ch.id})",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                "Role: ${ch.role} • Voice: ${ch.voiceId}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onCharacterClick(ch) }) {
                                    Text("Details & Voice Preview")
                                }
                                Button(onClick = { onOpenDeepDive(ch.id) }) {
                                    Text("Deep Dive (4 Tabs)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

cat > "$BASE_DIR/ui/character/CharacterDetailScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.character

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun CharacterDetailScreen(
    characterId: String = "",
    initialCharacter: Character? = null,
    onOpenDeepDive: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AnimeRepository() }
    var character by remember { mutableStateOf(initialCharacter) }
    var isLoading by remember { mutableStateOf(initialCharacter == null) }

    LaunchedEffect(characterId) {
        if (character == null) {
            isLoading = true
            val res = repository.fetchCharacters(repository.currentProjectId())
            if (res is Resource.Success) {
                character = res.data.find { it.id == characterId } ?: res.data.firstOrNull()
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (character == null) {
            Text(
                "Character not found in current project.",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            val ch = character!!
            Text(ch.name, style = MaterialTheme.typography.headlineLarge)
            Text(
                "Code: ${ch.id} • Role: ${ch.role}",
                style = MaterialTheme.typography.bodyLarge
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Visual DNA", style = MaterialTheme.typography.headlineMedium)
                    Text(ch.visualDna, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Assigned Voice ID: ${ch.voiceId}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            projectId = repository.currentProjectId(),
                            intent = CommandIntent.PREVIEW_VOICE,
                            mode = "QUICK",
                            payload = mapOf("character_code" to ch.id)
                        )
                        val msg = if (res is Resource.Success)
                            "WF-04 Voice Preview queued (${res.data.command_id ?: "OK"})"
                        else
                            "Voice preview request failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Preview Voice (WF-04)")
            }
            OutlinedButton(
                onClick = { onOpenDeepDive(ch.id) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Open Character Deep Dive (Knowledge, Voice, Status, Ref History)")
            }
        }
    }
}
EOF

echo "OK: Character files created"

echo ""
echo "========== PART 5 COMPLETE =========="
# ==============================================================================
# STEP 16: EpisodePlannerScreen (GAP #1 CONTINUE_EPISODE + GAP #4.1 no ep.id fallback)
# ==============================================================================
cat > "$BASE_DIR/ui/episode/EpisodePlannerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.episode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun EpisodePlannerScreen(
    onStartProduction: (String) -> Unit = {},
    onOpenScenes: (Int) -> Unit = {}
) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var episodesState by remember { mutableStateOf<Resource<List<Episode>>>(Resource.Loading) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    fun loadEpisodes() {
        scope.launch {
            episodesState = Resource.Loading
            episodesState = repository.fetchEpisodes(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { loadEpisodes() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Episode Planner (WF-02)", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = {
                scope.launch {
                    val res = repository.submitStudioCommand(
                        projectId = repository.currentProjectId(),
                        intent = CommandIntent.GENERATE_EPISODE,
                        mode = "SAVE"
                    )
                    if (res is Resource.Success) {
                        val cmdId = res.data.command?.id ?: res.data.command_id ?: ""
                        statusBanner = "Episode generation queued ($cmdId)"
                        if (cmdId.length == 36) {
                            onStartProduction(cmdId)
                        } else {
                            loadEpisodes()
                        }
                    } else if (res is Resource.Error) {
                        statusBanner = "Error: ${res.message}"
                    }
                }
            }) {
                Text("Generate Episode")
            }
        }

        statusBanner?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }

        ResourceStateBox(resource = episodesState, onRetry = { loadEpisodes() }) { list ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { ep ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onOpenScenes(ep.episodeNumber) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Episode ${ep.episodeNumber}: ${ep.title}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                StatusBadge(ep.status)
                            }
                            if (!ep.synopsis.isNullOrBlank()) {
                                Text(ep.synopsis, style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenScenes(ep.episodeNumber) }
                                ) { Text("Inspect Scenes") }

                                Button(onClick = {
                                    scope.launch {
                                        // GAP #1: use CONTINUE_EPISODE with stage=production
                                        // GAP #4.1: never fallback to ep.id
                                        val res = repository.submitStudioCommand(
                                            projectId = repository.currentProjectId(),
                                            intent = CommandIntent.CONTINUE_EPISODE,
                                            mode = "SAVE",
                                            episodeNo = ep.episodeNumber,
                                            payload = mapOf("stage" to "production")
                                        )
                                        when (res) {
                                            is Resource.Success -> {
                                                val cmdId = res.data.command?.id
                                                    ?: res.data.command_id ?: ""
                                                if (cmdId.length == 36) {
                                                    onStartProduction(cmdId)
                                                } else {
                                                    statusBanner = "Command queued but no valid UUID returned."
                                                }
                                            }
                                            is Resource.Error -> {
                                                statusBanner = "Failed to start production: ${res.message}"
                                            }
                                            else -> Unit
                                        }
                                    }
                                }) { Text("Run Production (WF-03)") }
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: EpisodePlannerScreen.kt created"

# ==============================================================================
# STEP 17: ProductionViewModel (Patch 1 lifecycle polling + backoff + terminal stop)
# ==============================================================================
cat > "$BASE_DIR/ui/production/ProductionViewModel.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.production

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource

data class ProductionState(
    val targetId: String = "",
    val backendStatus: String = "IDLE",
    val isPolling: Boolean = false,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

class ProductionViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _productionState = MutableStateFlow(ProductionState())
    val productionState: StateFlow<ProductionState> = _productionState.asStateFlow()

    companion object {
        val NON_TERMINAL_STATES = setOf(
            "QUEUED", "DISPATCHED", "ACKNOWLEDGED", "RECONCILING", "RUNNING"
        )
        val TERMINAL_STATES = setOf(
            "SUCCEEDED", "FAILED", "MANUAL_REVIEW", "COMPLETED"
        )
    }

    // Patch 1: Called from repeatOnLifecycle(Lifecycle.State.STARTED)
    // - Poll interval: 4000ms (3-5s spec)
    // - Stops on terminal state
    // - Exponential backoff on network failure (3s, 6s, 12s)
    // - Max 150 iterations cap
    suspend fun pollWhileStarted(commandOrJobId: String) {
        val projectId = repository.currentProjectId()
        var consecutiveFailures = 0
        var pollIterations = 0
        val maxIterations = 150

        while (pollIterations < maxIterations) {
            pollIterations++
            _productionState.value = _productionState.value.copy(
                targetId = commandOrJobId,
                isPolling = true
            )

            val resolvedStatus: String? = if (commandOrJobId.length == 36) {
                when (val cmdRes = repository.fetchCommandStatus(commandOrJobId)) {
                    is Resource.Success -> {
                        consecutiveFailures = 0
                        cmdRes.data.status?.uppercase() ?: "QUEUED"
                    }
                    is Resource.Error -> {
                        consecutiveFailures++
                        null
                    }
                    else -> "QUEUED"
                }
            } else {
                when (val jobsRes = repository.fetchJobs(projectId)) {
                    is Resource.Success -> {
                        consecutiveFailures = 0
                        jobsRes.data.firstOrNull()?.status?.uppercase() ?: "IDLE"
                    }
                    is Resource.Error -> {
                        consecutiveFailures++
                        null
                    }
                    is Resource.Empty -> {
                        consecutiveFailures = 0
                        "IDLE"
                    }
                    else -> "IDLE"
                }
            }

            if (resolvedStatus == null) {
                if (consecutiveFailures >= 3) {
                    _productionState.value = ProductionState(
                        targetId = commandOrJobId,
                        backendStatus = "ERROR",
                        isPolling = false,
                        errorMessage = "Network failure after 3 retries. Tap Retry to poll again.",
                        retryCount = consecutiveFailures
                    )
                    return
                }
                val backoffMs = 3000L * (1 shl (consecutiveFailures - 1))
                delay(backoffMs)
                continue
            }

            _productionState.value = ProductionState(
                targetId = commandOrJobId,
                backendStatus = resolvedStatus,
                isPolling = resolvedStatus in NON_TERMINAL_STATES,
                errorMessage = if (resolvedStatus == "FAILED")
                    "Command/Job reported FAILED." else null,
                retryCount = consecutiveFailures
            )

            if (resolvedStatus in TERMINAL_STATES ||
                resolvedStatus !in NON_TERMINAL_STATES
            ) {
                return
            }

            delay(4000L)
        }

        _productionState.value = _productionState.value.copy(
            isPolling = false,
            errorMessage = "Polling paused due to inactivity timeout. Tap Retry to refresh."
        )
    }
}
EOF

echo "OK: ProductionViewModel.kt created"

# ==============================================================================
# STEP 18: ProductionScreen (GAP #3 lifecycle-runtime-compose import)
# ==============================================================================
cat > "$BASE_DIR/ui/production/ProductionScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.production

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun ProductionScreen(
    episodeId: String = "",
    onViewVideoClick: () -> Unit = {},
    onOpenSceneInspector: () -> Unit = {},
    onOpenMediaLibrary: () -> Unit = {},
    viewModel: ProductionViewModel = viewModel()
) {
    val state by viewModel.productionState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pollTrigger by remember { mutableStateOf(0) }

    // Patch 1: Lifecycle-aware polling — pauses in background
    LaunchedEffect(episodeId, pollTrigger, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.pollWhileStarted(episodeId)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Production Command Center (WF-03)", style = MaterialTheme.typography.headlineLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Target Command / Job ID: ${episodeId.ifBlank { "Latest Project Job" }}")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Live Status:", style = MaterialTheme.typography.headlineMedium)
                    StatusBadge(state.backendStatus)
                }
                if (state.isPolling) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Polling backend every 4s (pauses automatically in background)...")
                }
                state.errorMessage?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { pollTrigger++ },
                modifier = Modifier.weight(1f)
            ) { Text("Refresh / Retry Poll") }

            OutlinedButton(
                onClick = onOpenSceneInspector,
                modifier = Modifier.weight(1f)
            ) { Text("Scene Inspector") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenMediaLibrary,
                modifier = Modifier.weight(1f)
            ) { Text("Media Library") }

            Button(
                onClick = onViewVideoClick,
                modifier = Modifier.weight(1f)
            ) { Text("QC & Approvals") }
        }
    }
}
EOF

echo "OK: ProductionScreen.kt created"

# ==============================================================================
# STEP 19: SceneDetailScreen (GAP #5.3 no phantom title field)
# ==============================================================================
cat > "$BASE_DIR/ui/production/SceneDetailScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.production

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SceneDto
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun SceneDetailScreen(initialEpisodeNo: Int = 1) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var episodeNo by remember { mutableStateOf(initialEpisodeNo) }
    var scenesState by remember { mutableStateOf<Resource<List<SceneDto>>>(Resource.Loading) }

    fun load() {
        scope.launch {
            scenesState = Resource.Loading
            scenesState = repository.fetchScenes(repository.currentProjectId(), episodeNo)
        }
    }

    LaunchedEffect(episodeNo) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Scene Inspector (Ep $episodeNo)",
                style = MaterialTheme.typography.headlineLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { if (episodeNo > 1) episodeNo-- }) {
                    Text("- Ep")
                }
                OutlinedButton(onClick = { episodeNo++ }) {
                    Text("+ Ep")
                }
            }
        }

        ResourceStateBox(resource = scenesState, onRetry = { load() }) { scenes ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scenes, key = { it.id }) { sc ->
                    val resolvedSceneNo = sc.scene_no ?: sc.scene_order ?: 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // GAP #5.3: no phantom "title" field on scenes table
                                Text(
                                    "Scene #$resolvedSceneNo",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                sc.status?.let { StatusBadge(it) }
                            }
                            Text(sc.description ?: "No description")
                            sc.clips?.forEach { clip ->
                                val clipSceneNo = clip.scene_no
                                    ?: clip.scene_order
                                    ?: resolvedSceneNo
                                Text(
                                    "Clip (Scene #$clipSceneNo): ${clip.clip_url ?: "Pending render"} " +
                                        "• Provider: ${clip.provider ?: "N/A"} " +
                                        "• ${clip.duration_sec ?: 0.0}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: SceneDetailScreen.kt created"

echo ""
echo "========== PART 6 COMPLETE =========="
# ==============================================================================
# STEP 20: QCScreen (real approvals from backend, no hardcoded checks)
# ==============================================================================
cat > "$BASE_DIR/ui/qc/QCScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.qc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.ApprovalDecisionDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun QCScreen(onProceedToPublish: () -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var approvalsState by remember {
        mutableStateOf<Resource<List<ApprovalDecisionDto>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            approvalsState = Resource.Loading
            approvalsState = repository.fetchApprovals(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Quality Control Gate (WF-11)", style = MaterialTheme.typography.headlineLarge)
        Button(
            onClick = onProceedToPublish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Proceed to Publish Studio (WF-07)")
        }
        ResourceStateBox(
            resource = approvalsState,
            emptyMessage = "No QC gate records found for this project yet.",
            onRetry = { load() }
        ) { list ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${item.type} (${item.resource_id ?: item.id})",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                StatusBadge(item.status ?: "PENDING")
                            }
                            if (!item.reason.isNullOrBlank()) {
                                Text("Note: ${item.reason}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    scope.launch {
                                        repository.submitApprovalDecision(
                                            repository.currentProjectId(),
                                            item.id,
                                            "APPROVED",
                                            "QC Approved"
                                        )
                                        load()
                                    }
                                }) { Text("Approve QC") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        repository.submitApprovalDecision(
                                            repository.currentProjectId(),
                                            item.id,
                                            "REJECTED",
                                            "QC Failed"
                                        )
                                        load()
                                    }
                                }) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: QCScreen.kt created"

# ==============================================================================
# STEP 21: PublishScreen (real PUBLISH_NOW command)
# ==============================================================================
cat > "$BASE_DIR/ui/publish/PublishScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun PublishScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Publish Studio (WF-07)", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Release Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description / Tags") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                scope.launch {
                    val res = repository.submitStudioCommand(
                        projectId = repository.currentProjectId(),
                        intent = CommandIntent.PUBLISH_NOW,
                        mode = "SAVE",
                        payload = mapOf(
                            "title" to title,
                            "description" to description
                        )
                    )
                    statusText = when (res) {
                        is Resource.Success -> "PUBLISH_NOW Command Dispatched (${res.data.command_id ?: res.data.status})"
                        is Resource.Error -> "Publish failed: ${res.message}"
                        else -> null
                    }
                }
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Dispatch PUBLISH_NOW (WF-07)")
        }
        statusText?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
    }
}
EOF

echo "OK: PublishScreen.kt created"

# ==============================================================================
# STEP 22: ReviewInboxScreen (real approvals + decisions)
# ==============================================================================
cat > "$BASE_DIR/ui/review/ReviewInboxScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.ApprovalDecisionDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun ReviewInboxScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var approvalsState by remember {
        mutableStateOf<Resource<List<ApprovalDecisionDto>>>(Resource.Loading)
    }
    var reasonInput by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            approvalsState = Resource.Loading
            approvalsState = repository.fetchApprovals(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Review & Approval Inbox (WF-11)", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = reasonInput,
            onValueChange = { reasonInput = it },
            label = { Text("Decision Reason / Reviewer Note") },
            modifier = Modifier.fillMaxWidth()
        )
        ResourceStateBox(
            resource = approvalsState,
            emptyMessage = "No pending approvals in this project's inbox.",
            onRetry = { load() }
        ) { items ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${item.type} • ${item.resource_id ?: item.id}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                StatusBadge(item.status ?: "PENDING")
                            }
                            item.risk?.let {
                                Text("Risk Level: $it", style = MaterialTheme.typography.labelLarge)
                            }
                            item.exact_diff?.let {
                                Text("Diff: $it", style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    scope.launch {
                                        repository.submitApprovalDecision(
                                            repository.currentProjectId(),
                                            item.id,
                                            "APPROVED",
                                            reasonInput.ifBlank { "Approved from Review Inbox" },
                                            item.resource_version
                                        )
                                        load()
                                    }
                                }) { Text("Approve") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        repository.submitApprovalDecision(
                                            repository.currentProjectId(),
                                            item.id,
                                            "REJECTED",
                                            reasonInput.ifBlank { "Rejected from Review Inbox" },
                                            item.resource_version
                                        )
                                        load()
                                    }
                                }) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: ReviewInboxScreen.kt created"

# ==============================================================================
# STEP 23: AIBrainScreen (real /api/v1/ai/chat)
# ==============================================================================
cat > "$BASE_DIR/ui/brain/AIBrainScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.brain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource

data class BrainMessageUi(
    val isUser: Boolean,
    val content: String,
    val provider: String? = null
)

@Composable
fun AIBrainScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<BrainMessageUi>() }
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI Studio Brain (/api/v1/ai/chat)", style = MaterialTheme.typography.headlineLarge)

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Ask AI Brain about your active project's story, scenes, or pipeline status.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                if (msg.isUser) "You"
                                else "AI Brain ${msg.provider?.let { "($it)" }.orEmpty()}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(msg.content, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Prompt AI Brain...") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val prompt = input.trim()
                    if (prompt.isNotBlank() && !isSending) {
                        input = ""
                        messages.add(BrainMessageUi(true, prompt))
                        isSending = true
                        scope.launch {
                            when (val res = repository.chatWithBrain(prompt)) {
                                is Resource.Success -> {
                                    val replyText = res.data.reply
                                        ?: res.data.message
                                        ?: "Response received."
                                    messages.add(
                                        BrainMessageUi(false, replyText, res.data.provider)
                                    )
                                }
                                is Resource.Error -> {
                                    messages.add(
                                        BrainMessageUi(false, "Error: ${res.message}")
                                    )
                                }
                                else -> Unit
                            }
                            isSending = false
                        }
                    }
                },
                enabled = !isSending && input.isNotBlank(),
                modifier = Modifier.height(54.dp)
            ) {
                Text(if (isSending) "..." else "Send")
            }
        }
    }
}
EOF

echo "OK: AIBrainScreen.kt created"

echo ""
echo "========== PART 7 COMPLETE =========="
# ==============================================================================
# STEP 24: QuickToolsScreen (real quick image/video dispatch)
# ==============================================================================
cat > "$BASE_DIR/ui/tools/QuickToolsScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun QuickToolsScreen(onOpenProductionPoll: (String) -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var lastCommandId by remember { mutableStateOf<String?>(null) }
    var lastStatus by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Quick Media Generator", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Visual Prompt (Anime Scene / Character Shot)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    isRunning = true
                    errorMsg = null
                    scope.launch {
                        when (
                            val res = repository.triggerQuickMedia(
                                projectId = repository.currentProjectId(),
                                isVideo = false,
                                prompt = prompt.trim()
                            )
                        ) {
                            is Resource.Success -> {
                                lastCommandId = res.data.command_id
                                    ?: res.data.command?.id
                                lastStatus = res.data.status ?: "QUEUED"
                            }
                            is Resource.Error -> errorMsg = res.message
                            else -> Unit
                        }
                        isRunning = false
                    }
                },
                enabled = !isRunning && prompt.isNotBlank(),
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Quick Image") }

            Button(
                onClick = {
                    isRunning = true
                    errorMsg = null
                    scope.launch {
                        when (
                            val res = repository.triggerQuickMedia(
                                projectId = repository.currentProjectId(),
                                isVideo = true,
                                prompt = prompt.trim()
                            )
                        ) {
                            is Resource.Success -> {
                                lastCommandId = res.data.command_id
                                    ?: res.data.command?.id
                                lastStatus = res.data.status ?: "QUEUED"
                            }
                            is Resource.Error -> errorMsg = res.message
                            else -> Unit
                        }
                        isRunning = false
                    }
                },
                enabled = !isRunning && prompt.isNotBlank(),
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Quick Video (5s)") }
        }

        errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        lastCommandId?.let { cmdId ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Command ID: $cmdId",
                            style = MaterialTheme.typography.labelLarge
                        )
                        StatusBadge(lastStatus ?: "QUEUED")
                    }
                    Button(
                        onClick = { onOpenProductionPoll(cmdId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Track Live Status in Production Monitor")
                    }
                }
            }
        }
    }
}
EOF

echo "OK: QuickToolsScreen.kt created"

# ==============================================================================
# STEP 25: SettingsScreen (real GPU config load + save)
# ==============================================================================
cat > "$BASE_DIR/ui/settings/SettingsScreen.kt" << 'EOF'
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
EOF

echo "OK: SettingsScreen.kt created"

# ==============================================================================
# STEP 26: OwnerOverviewScreen (real health via /v1/anime/health)
# ==============================================================================
cat > "$BASE_DIR/ui/owner/OwnerOverviewScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.HealthResponseDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun OwnerOverviewScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var healthState by remember {
        mutableStateOf<Resource<HealthResponseDto>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            healthState = Resource.Loading
            healthState = repository.fetchHealthDetail()
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Owner System Overview", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(resource = healthState, onRetry = { load() }) { health ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Anime Backend Health",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        StatusBadge(if (health.ok) "ONLINE" else "ERROR")
                    }
                    Text("Database Connection: ${health.db ?: health.database ?: "verified"}")
                    Text("Engine Version: ${health.version ?: "v6.5"}")
                }
            }
        }
    }
}
EOF

echo "OK: OwnerOverviewScreen.kt created"

# ==============================================================================
# STEP 27: WorkflowManagerScreen (real admin/workflows)
# ==============================================================================
cat > "$BASE_DIR/ui/owner/WorkflowManagerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun WorkflowManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var wfState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            wfState = Resource.Loading
            wfState = repository.fetchAdminWorkflows(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Workflow Registry & Activity (WF-00–WF-13)",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            "Shows configured intent mappings and command execution counts recorded in Anime Supabase (does not claim live n8n toggle state without webhook execution).",
            style = MaterialTheme.typography.bodyLarge
        )
        ResourceStateBox(resource = wfState, onRetry = { load() }) { list ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list) { wf ->
                    val wfKey = (wf["wf_key"] ?: "WF").toString()
                    val intents = wf["intents"]?.toString() ?: "[]"
                    val totalCmds = (wf["total_commands"] as? Number)?.toInt() ?: 0
                    val activeCmds = (wf["active_commands"] as? Number)?.toInt() ?: 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(wfKey, style = MaterialTheme.typography.headlineMedium)
                                StatusBadge(
                                    if (activeCmds > 0) "RUNNING ($activeCmds)" else "CONFIGURED"
                                )
                            }
                            Text("Mapped Intents: $intents")
                            Text("Total Dispatched Commands: $totalCmds • Currently Active: $activeCmds")
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: WorkflowManagerScreen.kt created"

# ==============================================================================
# STEP 28: WorkerManagerScreen (real worker list + start/stop commands)
# ==============================================================================
cat > "$BASE_DIR/ui/owner/WorkerManagerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun WorkerManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var workersState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }
    var actionBanner by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            workersState = Resource.Loading
            workersState = repository.fetchAdminWorkers(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Worker Fleet Manager (WF-04)", style = MaterialTheme.typography.headlineLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            repository.currentProjectId(),
                            CommandIntent.ADMIN_START_KAGGLE,
                            "QUICK"
                        )
                        actionBanner = if (res is Resource.Success)
                            "Start Kaggle command queued (${res.data.command_id ?: "OK"})"
                        else
                            "Start command failed"
                        load()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Start Kaggle Worker") }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            repository.currentProjectId(),
                            CommandIntent.ADMIN_STOP_KAGGLE,
                            "QUICK"
                        )
                        actionBanner = if (res is Resource.Success)
                            "Stop Kaggle command queued (${res.data.command_id ?: "OK"})"
                        else
                            "Stop command failed"
                        load()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Stop Worker") }
        }
        actionBanner?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        ResourceStateBox(
            resource = workersState,
            emptyMessage = "No active worker heartbeats registered in the last 180 seconds for this project.",
            onRetry = { load() }
        ) { list ->
            KeyValueMapList(list)
        }
    }
}
EOF

echo "OK: WorkerManagerScreen.kt created"

# ==============================================================================
# STEP 29: DatabaseScreen (real health detail only, no fabricated metrics)
# ==============================================================================
cat > "$BASE_DIR/ui/owner/DatabaseScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.HealthResponseDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun DatabaseScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var healthState by remember {
        mutableStateOf<Resource<HealthResponseDto>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            healthState = Resource.Loading
            healthState = repository.fetchHealthDetail()
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Database Connectivity Status", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Android is strictly isolated from direct Supabase access and queries health exclusively through Fastify (/v1/anime/health)."
        )
        ResourceStateBox(resource = healthState, onRetry = { load() }) { health ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Anime PostgreSQL Probe",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        StatusBadge(health.db ?: if (health.ok) "UP" else "DOWN")
                    }
                    Text("Backend Service Version: ${health.version ?: "N/A"}")
                }
            }
        }
    }
}
EOF

echo "OK: DatabaseScreen.kt created"

# ==============================================================================
# STEP 30: AuditLogScreen (real audit logs)
# ==============================================================================
cat > "$BASE_DIR/ui/owner/AuditLogScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun AuditLogScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var logsState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            logsState = Resource.Loading
            logsState = repository.fetchAuditLogs(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Project Audit Log", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(resource = logsState, onRetry = { load() }) { logs ->
            KeyValueMapList(logs)
        }
    }
}
EOF

echo "OK: AuditLogScreen.kt created"

echo ""
echo "========== PART 8 COMPLETE =========="
# ==============================================================================
# STEP 31: AnalyticsScreen (real aggregated stats from /analytics)
# ==============================================================================
cat > "$BASE_DIR/ui/analytics/AnalyticsScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun AnalyticsScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var analyticsState by remember {
        mutableStateOf<Resource<JsonMap>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            analyticsState = Resource.Loading
            analyticsState = repository.fetchAnalytics(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Project Analytics (WF-08)", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(resource = analyticsState, onRetry = { load() }) { data ->
            val episodes = data["episodes"] as? Map<*, *>
            val jobs = data["jobs"] as? Map<*, *>
            val ledger = data["ledger"] as? Map<*, *>

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Episodes Breakdown",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Total: ${episodes?.get("total") ?: 0} • " +
                                "Published: ${episodes?.get("published") ?: 0} • " +
                                "QC Passed: ${episodes?.get("qc_passed") ?: 0}"
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Commands & Pipeline Jobs",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Total: ${jobs?.get("total") ?: 0} • " +
                                "Succeeded: ${jobs?.get("succeeded") ?: 0} • " +
                                "Failed: ${jobs?.get("failed") ?: 0} • " +
                                "Manual Review: ${jobs?.get("manual_review") ?: 0}"
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Ledger Cost Summary",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Reserved: ${ledger?.get("reserved_cost") ?: 0} • " +
                                "Consumed: ${ledger?.get("consumed_cost") ?: 0} • " +
                                "Total Committed: ${ledger?.get("total_committed_cost") ?: 0}"
                        )
                    }
                }
            }
        }
    }
}
EOF

echo "OK: AnalyticsScreen.kt created"

# ==============================================================================
# STEP 32: MediaLibraryScreen (real scene clips per episode)
# ==============================================================================
cat > "$BASE_DIR/ui/library/MediaLibraryScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SceneDto
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun MediaLibraryScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var episodeNo by remember { mutableStateOf(1) }
    var scenesState by remember {
        mutableStateOf<Resource<List<SceneDto>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            scenesState = Resource.Loading
            scenesState = repository.fetchScenes(repository.currentProjectId(), episodeNo)
        }
    }

    LaunchedEffect(episodeNo) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Media Library (Ep $episodeNo)",
                style = MaterialTheme.typography.headlineLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { if (episodeNo > 1) episodeNo-- }) {
                    Text("- Ep")
                }
                OutlinedButton(onClick = { episodeNo++ }) {
                    Text("+ Ep")
                }
            }
        }
        ResourceStateBox(
            resource = scenesState,
            emptyMessage = "No rendered scenes or clips found for Episode $episodeNo.",
            onRetry = { load() }
        ) { scenes ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scenes, key = { it.id }) { sc ->
                    val sceneNum = sc.scene_no ?: sc.scene_order ?: 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Scene #$sceneNum",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                StatusBadge(sc.status ?: "DRAFT")
                            }
                            Text(sc.visual_prompt ?: sc.description ?: "No visual prompt")
                            val clips = sc.clips.orEmpty()
                            if (clips.isEmpty()) {
                                Text(
                                    "No rendered clips attached yet.",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                clips.forEach { clip ->
                                    Text(
                                        "Clip Scene #${clip.scene_no ?: clip.scene_order ?: sceneNum}: " +
                                            "${clip.clip_url ?: "No URL"} " +
                                            "(${clip.provider ?: "GPU"}, ${clip.duration_sec ?: 0.0}s)",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: MediaLibraryScreen.kt created"

# ==============================================================================
# STEP 33: NotificationScreen (real audit activity feed)
# ==============================================================================
cat > "$BASE_DIR/ui/notifications/NotificationScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun NotificationScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var feedState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            feedState = Resource.Loading
            feedState = repository.fetchAuditLogs(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Project Activity & Event Feed",
            style = MaterialTheme.typography.headlineLarge
        )
        ResourceStateBox(
            resource = feedState,
            emptyMessage = "No activity events recorded for this project yet.",
            onRetry = { load() }
        ) { events ->
            KeyValueMapList(events)
        }
    }
}
EOF

echo "OK: NotificationScreen.kt created"

# ==============================================================================
# STEP 34: VisionAnalysisScreen (WF-05 real dispatch + poll)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/VisionAnalysisScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.ApiCommandRecordDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun VisionAnalysisScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var mediaUrl by remember { mutableStateOf("") }
    var prompt by remember {
        mutableStateOf("Extract 11-field Visual DNA and character consistency anchors")
    }
    var submissionState by remember {
        mutableStateOf<Resource<ApiCommandRecordDto>?>(null)
    }
    var lastCommandId by remember { mutableStateOf<String?>(null) }

    fun pollCommand(cmdId: String) {
        scope.launch {
            submissionState = Resource.Loading
            submissionState = repository.fetchCommandStatus(cmdId)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vision DNA Analyzer (WF-05)", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = mediaUrl,
            onValueChange = { mediaUrl = it },
            label = { Text("Reference Image / Frame URL") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Analysis Instruction") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    submissionState = Resource.Loading
                    when (
                        val res = repository.triggerVisionAnalysis(
                            repository.currentProjectId(),
                            mediaUrl.trim(),
                            prompt.trim()
                        )
                    ) {
                        is Resource.Success -> {
                            val cmdId = res.data.command_id ?: res.data.command?.id
                            lastCommandId = cmdId
                            if (cmdId != null) pollCommand(cmdId)
                        }
                        is Resource.Error -> {
                            submissionState = Resource.Error(res.message, res.code)
                        }
                        else -> Unit
                    }
                }
            },
            enabled = mediaUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Dispatch WF-05 Vision Analysis")
        }

        submissionState?.let { st ->
            ResourceStateBox(
                resource = st,
                onRetry = { lastCommandId?.let { pollCommand(it) } }
            ) { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Command: ${record.id}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            StatusBadge(record.status ?: "QUEUED")
                        }
                        record.result?.let { Text("Result: $it") }
                        record.error_message?.let {
                            Text("Error: $it", color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(onClick = { pollCommand(record.id) }) {
                            Text("Refresh Result")
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: VisionAnalysisScreen.kt created"

# ==============================================================================
# STEP 35: JobsMonitorScreen (WF-10 + WF-13 real jobs + retries)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/JobsMonitorScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JobDto
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun JobsMonitorScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var jobsState by remember {
        mutableStateOf<Resource<List<JobDto>>>(Resource.Loading)
    }
    var retriesState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun loadAll() {
        val pid = repository.currentProjectId()
        scope.launch {
            jobsState = Resource.Loading
            jobsState = repository.fetchJobs(pid)
        }
        scope.launch {
            retriesState = Resource.Loading
            retriesState = repository.fetchRetries(pid)
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Jobs & Retry Monitor (WF-10 + WF-13)",
            style = MaterialTheme.typography.headlineLarge
        )
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("State Machine (WF-10)") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Retry Manager (WF-13)") }
            )
        }

        if (selectedTab == 0) {
            ResourceStateBox(resource = jobsState, onRetry = { loadAll() }) { jobs ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(jobs, key = { it.id }) { job ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        job.intent ?: job.id,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    StatusBadge(job.status ?: "UNKNOWN")
                                }
                                Text("Job ID: ${job.id} • Ep: ${job.episode_no ?: 1}")
                                Text(
                                    "Created: ${job.created_at ?: "N/A"} • " +
                                        "Pending Actions: ${job.pending_actions?.size ?: 0}"
                                )
                                job.error_message?.let {
                                    Text("Error: $it", color = MaterialTheme.colorScheme.error)
                                }
                                if (job.status?.uppercase() in listOf("FAILED", "MANUAL_REVIEW")) {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            repository.submitStudioCommand(
                                                repository.currentProjectId(),
                                                CommandIntent.RETRY_FAILED,
                                                "QUICK",
                                                payload = mapOf("job_id" to job.id)
                                            )
                                            loadAll()
                                        }
                                    }) { Text("Dispatch WF-13 Retry") }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            ResourceStateBox(
                resource = retriesState,
                emptyMessage = "No WF-13 retry records returned by backend for this project.",
                onRetry = { loadAll() }
            ) { retries ->
                KeyValueMapList(retries)
            }
        }
    }
}
EOF

echo "OK: JobsMonitorScreen.kt created"

# ==============================================================================
# STEP 36: QuotaDashboardScreen (WF-12 real quota)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/QuotaDashboardScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun QuotaDashboardScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var quotaState by remember {
        mutableStateOf<Resource<JsonMap>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            quotaState = Resource.Loading
            quotaState = repository.fetchQuota(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Quota & Usage Governor (WF-12)",
            style = MaterialTheme.typography.headlineLarge
        )
        ResourceStateBox(resource = quotaState, onRetry = { load() }) { map ->
            KeyValueMapList(listOf(map))
        }
    }
}
EOF

echo "OK: QuotaDashboardScreen.kt created"

# ==============================================================================
# STEP 37: CanonViewerScreen (WF-09 real canon + commit via APPROVE_CANON_CHANGE)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/CanonViewerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun CanonViewerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var canonState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            canonState = Resource.Loading
            canonState = repository.fetchCanon(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Canon Engine Viewer (WF-09)",
                style = MaterialTheme.typography.headlineLarge
            )
            Button(onClick = {
                scope.launch {
                    repository.submitStudioCommand(
                        repository.currentProjectId(),
                        CommandIntent.APPROVE_CANON_CHANGE,
                        "SAVE"
                    )
                    load()
                }
            }) { Text("Commit Canon") }
        }
        ResourceStateBox(
            resource = canonState,
            emptyMessage = "No persisted canon facts found in canon_facts table for this project.",
            onRetry = { load() }
        ) { facts ->
            KeyValueMapList(facts)
        }
    }
}
EOF

echo "OK: CanonViewerScreen.kt created"

# ==============================================================================
# STEP 38: ContinuityCheckerScreen (WF-06 real check, no fake pass)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/ContinuityCheckerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun ContinuityCheckerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var continuityState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }
    var banner by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            continuityState = Resource.Loading
            continuityState = repository.fetchContinuity(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Continuity Checker (WF-06)",
                style = MaterialTheme.typography.headlineLarge
            )
            Button(onClick = {
                scope.launch {
                    val res = repository.submitStudioCommand(
                        repository.currentProjectId(),
                        CommandIntent.CHECK_CONTINUITY,
                        "SAVE"
                    )
                    banner = if (res is Resource.Success)
                        "WF-06 Continuity check dispatched (${res.data.command_id ?: "QUEUED"})"
                    else
                        "Dispatch failed"
                }
            }) { Text("Run Check") }
        }
        banner?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        ResourceStateBox(
            resource = continuityState,
            emptyMessage = "No executed continuity check records found. Unexecuted checks are never labeled as passed.",
            onRetry = { load() }
        ) { list ->
            KeyValueMapList(list)
        }
    }
}
EOF

echo "OK: ContinuityCheckerScreen.kt created"

echo ""
echo "========== PART 9 COMPLETE =========="
# ==============================================================================
# STEP 39: LoreExplorerScreen (7 tabs — Mysteries, Arcs, Powers, Locations,
#                              Artifacts, Rules, Foreshadowing)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/LoreExplorerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun LoreExplorerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        "Mysteries" to "mysteries",
        "Character Arcs" to "arcs",
        "Powers" to "powers",
        "Locations" to "locations",
        "Artifacts" to "artifacts",
        "Story Rules" to "rules",
        "Foreshadowing" to "foreshadowing"
    )
    var selectedIndex by remember { mutableStateOf(0) }
    var itemsState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun loadCategory(slug: String) {
        scope.launch {
            itemsState = Resource.Loading
            itemsState = repository.fetchCategoryItems(
                repository.currentProjectId(),
                slug
            )
        }
    }

    LaunchedEffect(selectedIndex) {
        loadCategory(tabs[selectedIndex].second)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lore Explorer", style = MaterialTheme.typography.headlineLarge)
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { idx, pair ->
                Tab(
                    selected = selectedIndex == idx,
                    onClick = { selectedIndex = idx },
                    text = { Text(pair.first) }
                )
            }
        }
        ResourceStateBox(
            resource = itemsState,
            emptyMessage = "No ${tabs[selectedIndex].first} records persisted for this project yet.",
            onRetry = { loadCategory(tabs[selectedIndex].second) }
        ) { list ->
            KeyValueMapList(list)
        }
    }
}
EOF

echo "OK: LoreExplorerScreen.kt created"

# ==============================================================================
# STEP 40: CharacterDeepDiveScreen (4 tabs — Knowledge, Voice, Status, Reference)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/CharacterDeepDiveScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun CharacterDeepDiveScreen(characterId: String = "") {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        "Knowledge Graph",
        "Voice Catalog",
        "Status History",
        "Reference History"
    )
    var selectedTab by remember { mutableStateOf(0) }
    var tabState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }
    var resolvedCharId by remember { mutableStateOf(characterId) }

    fun loadTab(index: Int) {
        val pid = repository.currentProjectId()
        scope.launch {
            tabState = Resource.Loading
            if (resolvedCharId.isBlank()) {
                val chars = repository.fetchCharacters(pid)
                if (chars is Resource.Success && chars.data.isNotEmpty()) {
                    resolvedCharId = chars.data.first().id
                }
            }
            tabState = when (index) {
                0 -> repository.fetchCategoryItems(pid, "character-knowledge")
                1 -> repository.fetchCategoryItems(pid, "voices")
                2 -> repository.fetchCategoryItems(pid, "character-status")
                else -> {
                    // GAP #4.2: Never call reference-history with placeholder
                    if (resolvedCharId.isBlank()) {
                        Resource.Empty
                    } else {
                        repository.fetchCharacterReferenceHistory(pid, resolvedCharId)
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedTab, characterId) { loadTab(selectedTab) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Character Deep Dive " +
                (if (resolvedCharId.isNotBlank()) "($resolvedCharId)" else ""),
            style = MaterialTheme.typography.headlineLarge
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }
        ResourceStateBox(
            resource = tabState,
            emptyMessage = "No ${tabs[selectedTab]} records found for this character/project.",
            onRetry = { loadTab(selectedTab) }
        ) { items ->
            KeyValueMapList(items)
        }
    }
}
EOF

echo "OK: CharacterDeepDiveScreen.kt created"

# ==============================================================================
# STEP 41: AdminQueuesScreen (real outbox + command queue counters)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/AdminQueuesScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun AdminQueuesScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var queuesState by remember {
        mutableStateOf<Resource<JsonMap>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            queuesState = Resource.Loading
            queuesState = repository.fetchAdminQueues(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Outbox & Command Queues", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(resource = queuesState, onRetry = { load() }) { data ->
            val outbox = data["outbox"] as? Map<*, *>
            val commands = data["commands"] as? Map<*, *>
            val pendingActions = data["pending_actions_count"] ?: 0

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Outbox Events Dispatcher",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Pending: ${outbox?.get("pending") ?: 0} • " +
                                "Processing: ${outbox?.get("processing") ?: 0}"
                        )
                        Text(
                            "Failed: ${outbox?.get("failed") ?: 0} • " +
                                "Dead Letter: ${outbox?.get("dead_letter") ?: 0}"
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "API Commands Queue",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Queued: ${commands?.get("queued") ?: 0} • " +
                                "Dispatched: ${commands?.get("dispatched") ?: 0}"
                        )
                        Text(
                            "Reconciling: ${commands?.get("reconciling") ?: 0} • " +
                                "Manual Review: ${commands?.get("manual_review") ?: 0}"
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Pending Actions Count: $pendingActions",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        }
    }
}
EOF

echo "OK: AdminQueuesScreen.kt created"

# ==============================================================================
# STEP 42: BudgetLedgerScreen (real ledger summary from admin endpoint)
# ==============================================================================
cat > "$BASE_DIR/ui/advanced/BudgetLedgerScreen.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun BudgetLedgerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var ledgerState by remember {
        mutableStateOf<Resource<JsonMap>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            ledgerState = Resource.Loading
            ledgerState = repository.fetchLedgerSummary(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Budget & Resource Ledger",
            style = MaterialTheme.typography.headlineLarge
        )
        ResourceStateBox(resource = ledgerState, onRetry = { load() }) { data ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Active Project Cost Ledger",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Reserved Cost: ${data["reserved_cost"] ?: 0}")
                    Text("Consumed Cost: ${data["consumed_cost"] ?: 0}")
                    Text("Total Committed Cost: ${data["total_committed_cost"] ?: 0}")
                }
            }
        }
    }
}
EOF

echo "OK: BudgetLedgerScreen.kt created"

echo ""
echo "========== PART 10 COMPLETE =========="
# ==============================================================================
# STEP 43: AppNavGraph.kt — Complete Navigation
#   Part 10.2: Bottom Navigation (5 tabs on phone)
#   Part 10.3: Navigation Rail on tablet (>=600dp)
#   Part 10.4: Sectioned Drawer (WORKSPACE / STORY / PRODUCTION / INSIGHTS / OWNER)
#   Patch 2: Auto-redirect to Auth on session expiry
#   Owner role gating: Owner-only destinations hidden for non-owners
# ==============================================================================
cat > "$BASE_DIR/ui/navigation/AppNavGraph.kt" << 'EOF'
package org.dpdns.bnai.anime.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.ui.advanced.AdminQueuesScreen
import org.dpdns.bnai.anime.ui.advanced.BudgetLedgerScreen
import org.dpdns.bnai.anime.ui.advanced.CanonViewerScreen
import org.dpdns.bnai.anime.ui.advanced.CharacterDeepDiveScreen
import org.dpdns.bnai.anime.ui.advanced.ContinuityCheckerScreen
import org.dpdns.bnai.anime.ui.advanced.JobsMonitorScreen
import org.dpdns.bnai.anime.ui.advanced.LoreExplorerScreen
import org.dpdns.bnai.anime.ui.advanced.QuotaDashboardScreen
import org.dpdns.bnai.anime.ui.advanced.VisionAnalysisScreen
import org.dpdns.bnai.anime.ui.analytics.AnalyticsScreen
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.character.CharacterDetailScreen
import org.dpdns.bnai.anime.ui.character.CharacterListScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.owner.AuditLogScreen
import org.dpdns.bnai.anime.ui.owner.DatabaseScreen
import org.dpdns.bnai.anime.ui.owner.OwnerOverviewScreen
import org.dpdns.bnai.anime.ui.owner.WorkerManagerScreen
import org.dpdns.bnai.anime.ui.owner.WorkflowManagerScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.production.SceneDetailScreen
import org.dpdns.bnai.anime.ui.project.BibleScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.studio.PromptStudioScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavGraph(navController: NavHostController) {
    val tokenManager = remember { TokenManager.getInstance() }
    val startDest = if (!tokenManager?.getToken().isNullOrBlank()) "studio" else "auth"
    val expiredMessage by (
        tokenManager?.sessionExpiredMessage?.collectAsState()
            ?: remember { mutableStateOf(null) }
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: startDest
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }

    // Patch 2: Auto-redirect to Auth on session expiry
    LaunchedEffect(expiredMessage) {
        if (!expiredMessage.isNullOrBlank() && currentRoute != "auth") {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Part 10.2: Bottom navigation destinations
    val bottomDestinations = listOf(
        NavItem("studio", "Home", Icons.Default.Home),
        NavItem("project_detail", "Projects", Icons.Default.Folder),
        NavItem("ai_brain", "AI Brain", Icons.Default.AutoAwesome),
        NavItem("production", "Production", Icons.Default.Movie),
        NavItem("episodes", "Episodes", Icons.Default.VideoLibrary)
    )

    // Part 10.4: Sectioned drawer
    val isOwner = tokenManager?.isOwner() == true
    val drawerSections: List<Pair<String, List<Pair<String, String>>>> = remember(isOwner) {
        buildList {
            add(
                "WORKSPACE" to listOf(
                    "studio" to "Home",
                    "project_detail" to "Projects",
                    "ai_brain" to "AI Brain",
                    "quick_tools" to "Quick Tools"
                )
            )
            add(
                "STORY" to listOf(
                    "bible" to "Story Bible",
                    "characters" to "Characters",
                    "character_deep_dive" to "Character Deep Dive",
                    "lore_explorer" to "Lore Explorer",
                    "episodes" to "Episodes",
                    "continuity" to "Continuity",
                    "canon" to "Canon",
                    "vision_analysis" to "Vision Analysis"
                )
            )
            add(
                "PRODUCTION" to listOf(
                    "production" to "Production",
                    "scenes" to "Scene Inspector",
                    "jobs_monitor" to "Jobs",
                    "media_library" to "Media Library",
                    "qc" to "Quality Check",
                    "review_inbox" to "Review Inbox",
                    "publish" to "Publish"
                )
            )
            add(
                "INSIGHTS" to listOf(
                    "analytics" to "Analytics",
                    "notifications" to "Notifications",
                    "audit_log" to "Audit Activity"
                )
            )
            if (isOwner) {
                add(
                    "OWNER CONTROL" to listOf(
                        "owner_overview" to "Owner Overview",
                        "workflows" to "Workflows",
                        "workers" to "Workers",
                        "admin_queues" to "Queues",
                        "quota_dashboard" to "Quota",
                        "budget_ledger" to "Budget Ledger",
                        "database" to "Database",
                        "settings" to "Settings",
                        "prompt_studio" to "Prompt Studio"
                    )
                )
            }
        }
    }

    // Auth screen: standalone NavHost (no drawer, no bottom nav)
    if (currentRoute == "auth") {
        NavHost(navController = navController, startDestination = startDest) {
            composable("auth") { AuthScreen(navController = navController) }
            composable("studio") { StudioDashboardScreen() }
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        "AI Cinematic Anime Engine",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                    HorizontalDivider()
                    drawerSections.forEach { (sectionTitle, routes) ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            sectionTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        routes.forEach { (route, title) ->
                            NavigationDrawerItem(
                                label = { Text(title) },
                                selected = currentRoute == route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("AI Anime Engine") },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Open Navigation Drawer"
                                )
                            }
                        },
                        actions = {
                            // Patch 2: Logout — clear JWT + redirect to Auth
                            TextButton(onClick = {
                                tokenManager?.clearToken()
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }) {
                                Text("Logout", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                bottomBar = {
                    if (!isTablet) {
                        NavigationBar {
                            bottomDestinations.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(item.icon, contentDescription = item.label)
                                    },
                                    label = { Text(item.label) }
                                )
                            }
                            NavigationBarItem(
                                selected = false,
                                onClick = { scope.launch { drawerState.open() } },
                                icon = {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                                },
                                label = { Text("More") }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (isTablet) {
                        NavigationRail {
                            bottomDestinations.forEach { item ->
                                NavigationRailItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(item.icon, contentDescription = item.label)
                                    },
                                    label = { Text(item.label) }
                                )
                            }
                            NavigationRailItem(
                                selected = false,
                                onClick = { scope.launch { drawerState.open() } },
                                icon = {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                                },
                                label = { Text("All") }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("auth") { AuthScreen(navController = navController) }
                            composable("studio") {
                                StudioDashboardScreen(
                                    onProjectClick = {
                                        navController.navigate("project_detail")
                                    },
                                    onOpenReviews = {
                                        navController.navigate("review_inbox")
                                    },
                                    onOpenEpisodes = {
                                        navController.navigate("episodes")
                                    }
                                )
                            }
                            composable("project_detail") {
                                ProjectDetailScreen(
                                    onOpenBible = { navController.navigate("bible") },
                                    onOpenCharacters = { navController.navigate("characters") },
                                    onOpenEpisodes = { navController.navigate("episodes") },
                                    onOpenReviews = { navController.navigate("review_inbox") },
                                    onOpenAnalytics = { navController.navigate("analytics") }
                                )
                            }
                            composable("bible") { BibleScreen() }
                            composable("characters") {
                                CharacterListScreen(
                                    onCharacterClick = { ch ->
                                        selectedCharacter = ch
                                        navController.navigate("character_detail")
                                    },
                                    onOpenDeepDive = { id ->
                                        selectedCharacter = Character(id, id, "", "", "")
                                        navController.navigate("character_deep_dive")
                                    }
                                )
                            }
                            composable("character_detail") {
                                CharacterDetailScreen(
                                    characterId = selectedCharacter?.id.orEmpty(),
                                    initialCharacter = selectedCharacter,
                                    onOpenDeepDive = {
                                        navController.navigate("character_deep_dive")
                                    }
                                )
                            }
                            composable("episodes") {
                                EpisodePlannerScreen(
                                    onStartProduction = { cmdId ->
                                        navController.navigate("production/$cmdId")
                                    },
                                    onOpenScenes = { epNo ->
                                        navController.navigate("scenes/$epNo")
                                    }
                                )
                            }
                            composable("production") {
                                ProductionScreen(
                                    onViewVideoClick = { navController.navigate("qc") },
                                    onOpenSceneInspector = { navController.navigate("scenes") },
                                    onOpenMediaLibrary = { navController.navigate("media_library") }
                                )
                            }
                            composable("production/{cmdId}") { entry ->
                                ProductionScreen(
                                    episodeId = entry.arguments?.getString("cmdId").orEmpty(),
                                    onViewVideoClick = { navController.navigate("qc") },
                                    onOpenSceneInspector = { navController.navigate("scenes") },
                                    onOpenMediaLibrary = { navController.navigate("media_library") }
                                )
                            }
                            composable("scenes") { SceneDetailScreen(1) }
                            composable("scenes/{epNo}") { entry ->
                                val ep = entry.arguments?.getString("epNo")?.toIntOrNull() ?: 1
                                SceneDetailScreen(ep)
                            }
                            composable("qc") {
                                QCScreen(onProceedToPublish = {
                                    navController.navigate("publish")
                                })
                            }
                            composable("publish") { PublishScreen() }
                            composable("review_inbox") { ReviewInboxScreen() }
                            composable("ai_brain") { AIBrainScreen() }
                            composable("quick_tools") {
                                QuickToolsScreen(
                                    onOpenProductionPoll = { cmdId ->
                                        navController.navigate("production/$cmdId")
                                    }
                                )
                            }
                            composable("settings") { SettingsScreen() }
                            composable("prompt_studio") { PromptStudioScreen() }
                            composable("owner_overview") { OwnerOverviewScreen() }
                            composable("workflows") { WorkflowManagerScreen() }
                            composable("workers") { WorkerManagerScreen() }
                            composable("database") { DatabaseScreen() }
                            composable("audit_log") { AuditLogScreen() }
                            composable("analytics") { AnalyticsScreen() }
                            composable("media_library") { MediaLibraryScreen() }
                            composable("notifications") { NotificationScreen() }
                            composable("vision_analysis") { VisionAnalysisScreen() }
                            composable("jobs_monitor") { JobsMonitorScreen() }
                            composable("quota_dashboard") { QuotaDashboardScreen() }
                            composable("canon") { CanonViewerScreen() }
                            composable("continuity") { ContinuityCheckerScreen() }
                            composable("lore_explorer") { LoreExplorerScreen() }
                            composable("character_deep_dive") {
                                CharacterDeepDiveScreen(
                                    characterId = selectedCharacter?.id.orEmpty()
                                )
                            }
                            composable("admin_queues") { AdminQueuesScreen() }
                            composable("budget_ledger") { BudgetLedgerScreen() }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "OK: AppNavGraph.kt created"

# ==============================================================================
# SCRIPT COMPLETE MARKER
# ==============================================================================
echo ""
echo "=============================================================================="
echo "  SCRIPT COMPLETE — All 33 screens, all fixes, all files generated"
echo "=============================================================================="

