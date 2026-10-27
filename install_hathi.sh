#!/bin/bash
# Clean folder
mkdir -p app/src/main/java/org/dpdns/bnai/anime/{ui/theme,ui/navigation,ui/auth,core/network,core/auth,core/model,ui/studio,ui/project,ui/character,ui/episode,ui/production,ui/library,ui/review,ui/qc,ui/publish,ui/analytics,ui/brain,ui/tools,ui/notifications,ui/settings,ui/owner}

# --- CONFIG FILES FIX ---
cat > app/build.gradle.kts << 'EOF'
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "org.dpdns.bnai.anime"
    compileSdk = 34
    defaultConfig {
        applicationId = "org.dpdns.bnai.anime"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "BASE_API_URL", "\"https://api.bnai.dpdns.org/\"")
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17"; freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
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
EOF

cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application android:allowBackup="true" android:label="AI Anime Studio" android:supportsRtl="true" android:theme="@android:style/Theme.Material.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
EOF

# --- THEME ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/theme/Color.kt << 'EOF'
package org.dpdns.bnai.anime.ui.theme
import androidx.compose.ui.graphics.Color
val Primary = Color(0xFF3F51B5)
val Secondary = Color(0xFF009688)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val Error = Color(0xFFCF6679)
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/theme/Type.kt << 'EOF'
package org.dpdns.bnai.anime.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp)
)
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/theme/Theme.kt << 'EOF'
package org.dpdns.bnai.anime.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val DarkColorScheme = darkColorScheme(primary = Primary, onPrimary = Color.White, secondary = Secondary, onSecondary = Color.White, background = BackgroundDark, onBackground = Color.White, surface = SurfaceDark, onSurface = Color.White, error = Error)
@Composable
fun AnimeStudioTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = DarkColorScheme, typography = AppTypography, content = content) }
EOF

# --- CORE ---
cat > app/src/main/java/org/dpdns/bnai/anime/core/network/Resource.kt << 'EOF'
package org.dpdns.bnai.anime.core.network
sealed class Resource<out T> {
    object Idle : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/core/model/Project.kt << 'EOF'
package org.dpdns.bnai.anime.core.model
data class Project(val id: String, val name: String, val activeEpisode: String, val status: String)
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/core/model/Character.kt << 'EOF'
package org.dpdns.bnai.anime.core.model
data class Character(val id: String, val name: String, val role: String, val visualDna: String, val voiceId: String)
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/core/model/Episode.kt << 'EOF'
package org.dpdns.bnai.anime.core.model
data class Episode(val id: String, val episodeNumber: Int, val title: String, val status: String)
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/core/auth/TokenManager.kt << 'EOF'
package org.dpdns.bnai.anime.core.auth
import android.content.Context
import android.content.SharedPreferences
class TokenManager(private val prefs: SharedPreferences) {
    fun saveToken(token: String) { prefs.edit().putString("jwt_token", token).apply() }
    fun getToken(): String? = prefs.getString("jwt_token", null)
    companion object {
        @Volatile private var instance: TokenManager? = null
        fun init(context: Context): TokenManager = instance ?: synchronized(this) { instance ?: TokenManager(context.applicationContext.getSharedPreferences("anime_prefs", Context.MODE_PRIVATE)).also { instance = it } }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/core/network/AnimeApiService.kt << 'EOF'
package org.dpdns.bnai.anime.core.network
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val userId: String)
interface AnimeApiService { @POST("api/v1/auth/login") suspend fun login(@Body request: LoginRequest): Response<LoginResponse> }
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/core/network/RetrofitClient.kt << 'EOF'
package org.dpdns.bnai.anime.core.network
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.dpdns.bnai.anime.core.auth.TokenManager
import java.util.concurrent.TimeUnit
object RetrofitClient {
    private const val BASE_URL = "https://api.bnai.dpdns.org/"
    fun create(tokenManager: TokenManager): AnimeApiService {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request().newBuilder().header("Accept", "application/json")
            tokenManager.getToken()?.let { req.header("Authorization", "Bearer $it") }
            chain.proceed(req.build())
        }.connectTimeout(30, TimeUnit.SECONDS).build()
        return Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(AnimeApiService::class.java)
    }
}
EOF

# --- AUTH ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/auth/AuthViewModel.kt << 'EOF'
package org.dpdns.bnai.anime.ui.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.core.auth.TokenManager
class AuthViewModel(private val api: AnimeApiService, private val tokenManager: TokenManager) : ViewModel() {
    private val _authState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val authState: StateFlow<Resource<String>> = _authState
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            try {
                val res = api.login(LoginRequest(email, password))
                if (res.isSuccessful && res.body() != null) { tokenManager.saveToken(res.body()!!.token); _authState.value = Resource.Success(res.body()!!.token) }
                else { _authState.value = Resource.Error("Login failed: HTTP ${res.code()}") }
            } catch (e: Exception) { _authState.value = Resource.Error(e.message ?: "Network error") }
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/auth/AuthScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.auth
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.RetrofitClient
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun AuthScreen(navController: NavController) {
    val context = LocalContext.current
    val tm = TokenManager.init(context)
    val api = RetrofitClient.create(tm)
    val vm = remember { AuthViewModel(api, tm) }
    val state by vm.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(state) { if (state is Resource.Success) navController.navigate("studio") { popUpTo("auth") { inclusive = true } } }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AI Anime Studio", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { vm.login(email, password) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            if (state is Resource.Loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) else Text("Login")
        }
        if (state is Resource.Error) { Spacer(modifier = Modifier.height(16.dp)); Text((state as Resource.Error).message, color = MaterialTheme.colorScheme.error) }
    }
}
EOF

# --- STUDIO ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/studio/StudioViewModel.kt << 'EOF'
package org.dpdns.bnai.anime.ui.studio
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.Resource
class StudioViewModel : ViewModel() {
    private val _projectsState = MutableStateFlow<Resource<List<Project>>>(Resource.Loading)
    val projectsState: StateFlow<Resource<List<Project>>> = _projectsState
    init { loadProjects() }
    fun loadProjects() {
        viewModelScope.launch {
            _projectsState.value = Resource.Loading; delay(500)
            _projectsState.value = Resource.Success(listOf(Project("proj_001", "Shadows of Aethelgard", "Episode 5: Crimson Eclipse", "RUNNING"), Project("proj_002", "Cyber Samurai", "Episode 2", "REVIEW")))
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/studio/StudioDashboardScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.studio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDashboardScreen(onProjectClick: (String) -> Unit = {}, vm: StudioViewModel = viewModel()) {
    val state by vm.projectsState.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Anime Studio") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Compute Engine", style = MaterialTheme.typography.headlineMedium); Text("SDXL + AnimateDiff + MuseTalk", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50))); Spacer(Modifier.width(8.dp)); Text("Kaggle Status: ONLINE", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelLarge) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Active Productions", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            when (val s = state) {
                is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is Resource.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.data) { p -> Card(Modifier.fillMaxWidth().clickable { onProjectClick(p.id) }) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column { Text(p.name, style = MaterialTheme.typography.headlineMedium); Text(p.activeEpisode) }; Surface(color = Color(0xFF3F51B5).copy(alpha = 0.2f), shape = RoundedCornerShape(50)) { Text(p.status, Modifier.padding(8.dp, 4.dp), color = Color(0xFF3F51B5)) } } } }
                }
                else -> {}
            }
        }
    }
}
EOF

# --- PROJECT & BIBLE ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/project/ProjectViewModel.kt << 'EOF'
package org.dpdns.bnai.anime.ui.project
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.Resource
data class StoryBibleData(val projectId: String, val universeTitle: String, val worldRules: String, val magicSystem: String, val toneAndTheme: String)
class ProjectViewModel : ViewModel() {
    private val _bibleState = MutableStateFlow<Resource<StoryBibleData>>(Resource.Loading)
    val bibleState: StateFlow<Resource<StoryBibleData>> = _bibleState
    fun fetchBible(projectId: String) {
        viewModelScope.launch { _bibleState.value = Resource.Loading; delay(400); _bibleState.value = Resource.Success(StoryBibleData(projectId, "Universe", "1. Three Floating Citadels\n2. No mortal may cross Rift", "Resonance Weaving", "Dark Cinematic Seinen")) }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/project/BibleScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.project
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource
@Composable
fun BibleScreen(projectId: String = "proj_001", vm: ProjectViewModel = viewModel()) {
    val state by vm.bibleState.collectAsState()
    LaunchedEffect(projectId) { vm.fetchBible(projectId) }
    when (val s = state) {
        is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is Resource.Success -> {
            val b = s.data
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Master Story Bible", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(value = b.worldRules, onValueChange = {}, readOnly = true, label = { Text("World Rules") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = b.magicSystem, onValueChange = {}, readOnly = true, label = { Text("Magic System") }, modifier = Modifier.fillMaxWidth())
            }
        }
        else -> {}
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/project/ProjectDetailScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.project
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(id: String) {
    val nav = rememberNavController()
    var tabIndex by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { TopAppBar(title = { Text("Project: $id") }) }) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0; nav.navigate("overview") }, text = { Text("Overview") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1; nav.navigate("bible") }, text = { Text("Bible") })
            }
            NavHost(nav, startDestination = "overview") {
                composable("overview") { Column(Modifier.padding(16.dp)) { Text("Universe Snapshot"); Button(onClick = {}) { Text("Open Episode Planner") } } }
                composable("bible") { BibleScreen(projectId = id) }
            }
        }
    }
}
EOF

# --- CHARACTERS & EPISODE ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/character/CharacterListScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.character
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.model.Character
@Composable
fun CharacterListScreen(onCharacterClick: (Character) -> Unit = {}) {
    val chars = listOf(Character("CHAR_001", "Aarav", "Protagonist", "Face: Sharp | Hair: Black", "svara_hi_male"), Character("CHAR_002", "Meera", "Deuteragonist", "Face: Fierce | Hair: Silver", "svara_hi_female"))
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        items(chars) { c -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onCharacterClick(c) }) { Column(Modifier.padding(16.dp)) { Text(c.name, style = MaterialTheme.typography.headlineMedium); Text(c.role, color = MaterialTheme.colorScheme.secondary) } } }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/episode/EpisodePlannerScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.episode
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePlannerScreen(onStartProduction: (String) -> Unit = {}) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("Start Production?") }, text = { Text("This will consume quota.") }, confirmButton = { Button(onClick = { showDialog = false; onStartProduction("ep_005") }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } })
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Episode Planner") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) { Text("Episodes List"); Button(onClick = { showDialog = true }) { Text("Generate Episode") } }
    }
}
EOF

# --- PRODUCTION ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/production/ProductionViewModel.kt << 'EOF'
package org.dpdns.bnai.anime.ui.production
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
enum class ProductionStage(val title: String) { Queued("Queued"), Rendering("Rendering"), Completed("Completed") }
data class ProductionState(val stage: ProductionStage = ProductionStage.Queued)
class ProductionViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProductionState())
    val state: StateFlow<ProductionState> = _state
    fun start() { viewModelScope.launch { for (s in listOf(ProductionStage.Queued, ProductionStage.Rendering, ProductionStage.Completed)) { _state.value = ProductionState(s); delay(3000) } } }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/production/ProductionScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.production
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(id: String, vm: ProductionViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.start() }
    Scaffold(topBar = { TopAppBar(title = { Text("Production Center") }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.stage != ProductionStage.Completed) CircularProgressIndicator(Modifier.size(72.dp))
            Text("Stage: ${state.stage.title}")
        }
    }
}
EOF

# --- REVIEW / QC / PUBLISH ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/review/ReviewInboxScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.review
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
data class ApprovalRequestItem(val id: String, val type: String, val summary: String, val status: String = "PENDING")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewInboxScreen() {
    val approvals = remember { mutableStateListOf(ApprovalRequestItem("APR_01", "Canon Change", "Update Commander Meera", "PENDING")) }
    Scaffold(topBar = { TopAppBar(title = { Text("Review Inbox") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            approvals.forEach { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(16.dp)) { Text(item.type); Text(item.summary); Button(onClick = {}) { Text("Approve") } } }
            }
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/qc/QCScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.qc
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
@Composable
fun QCScreen(onProceed: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("QC Gate") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                AndroidView(factory = { ctx -> VideoView(ctx).apply { setMediaController(MediaController(ctx).also { it.setAnchorView(this) }) } }, modifier = Modifier.fillMaxSize())
                Text("Video Preview", color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Text("Audio Sync: PASSED"); Text("Lipsync: PASSED")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onProceed) { Text("Proceed to Publish") }
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/publish/PublishScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.publish
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen() {
    var title by remember { mutableStateOf("Shadows of Aethelgard - Episode 5") }
    Scaffold(topBar = { TopAppBar(title = { Text("Publish Studio") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("YouTube Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = {}) { Text("Publish Now") }
        }
    }
}
EOF

# --- BRAIN / ANALYTICS / TOOLS / SETTINGS / OWNER ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/brain/AIBrainScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.brain
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
data class ChatMessage(val isUser: Boolean, val text: String)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBrainScreen() {
    val messages = remember { mutableStateListOf(ChatMessage(true, "Can Aarav cross the Rift?"), ChatMessage(false, "No. Rule #2 prevents it.")) }
    var input by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("AI Brain") }) }, bottomBar = {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask...") })
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (input.isNotBlank()) { messages.add(ChatMessage(true, input)); input = "" } }) { Text("Send") }
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg -> 
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
                    Card(colors = CardDefaults.cardColors(containerColor = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)) { 
                        Text(msg.text, Modifier.padding(12.dp)) 
                    }
                }
            }
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/analytics/AnalyticsScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.analytics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Analytics") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Views"); Text("148,920", style = MaterialTheme.typography.headlineMedium) } }; Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Watch Time"); Text("64,310m", style = MaterialTheme.typography.headlineMedium) } } } }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Retention Trend"); Canvas(Modifier.fillMaxWidth().height(120.dp)) { val p = listOf(0.4f, 0.6f, 0.7f, 0.9f); val path = Path(); val step = size.width / (p.size - 1); p.forEachIndexed { i, v -> if (i == 0) path.moveTo(i * step, size.height - (v * size.height)) else path.lineTo(i * step, size.height - (v * size.height)) }; drawPath(path, color = androidx.compose.ui.graphics.Color.Green, style = Stroke(5f)) } } } }
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/tools/QuickToolsScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.tools
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickToolsScreen() {
    var output by remember { mutableStateOf("Ready...") }
    Scaffold(topBar = { TopAppBar(title = { Text("Quick Tools") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Box(Modifier.fillMaxWidth().height(150.dp).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("Tap to Upload") }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { output = "Vision Extraction Complete" }, modifier = Modifier.fillMaxWidth()) { Text("Extract Visual DNA") }
            Spacer(Modifier.height(16.dp))
            Text(output)
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/notifications/NotificationScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.notifications
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Notifications") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Card { Column(Modifier.padding(12.dp)) { Text("Lipsync Failed (WF-11)"); Text("Episode 5 failed MuseTalk alignment.") } } }
            item { Card { Column(Modifier.padding(12.dp)) { Text("Episode 4 Published"); Text("Receipt YT_VID_88219B live.") } } }
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/settings/SettingsScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var darkMode by remember { mutableStateOf(true) }
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Dark Mode"); Switch(checked = darkMode, onCheckedChange = { darkMode = it }) }
        }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/owner/OwnerOverviewScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOverviewScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Owner Console") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) { Text("DB Status"); Text("ONLINE", color = androidx.compose.ui.graphics.Color.Green) } }
            Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) { Text("n8n Status"); Text("ONLINE", color = androidx.compose.ui.graphics.Color.Green) } }
            Button(onClick = {}) { Text("Workflows (WF-00 to 13)") }
        }
    }
}
EOF

# --- NAV & MAIN ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/navigation/NavGraph.kt << 'EOF'
package org.dpdns.bnai.anime.ui.navigation
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.analytics.AnalyticsScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.owner.OwnerOverviewScreen
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") { AuthScreen(navController) }
        composable("studio") { StudioDashboardScreen(onProjectClick = { id -> navController.navigate("project/$id") }) }
        composable("project/{id}") { ProjectDetailScreen(id = it.arguments?.getString("id") ?: "") }
        composable("episodes") { EpisodePlannerScreen(onStartProduction = { id -> navController.navigate("production/$id") }) }
        composable("production/{id}") { ProductionScreen(id = it.arguments?.getString("id") ?: "") }
        composable("library") { MediaLibraryScreen() }
        composable("reviews") { ReviewInboxScreen() }
        composable("qc") { QCScreen(onProceed = { navController.navigate("publish") }) }
        composable("publish") { PublishScreen() }
        composable("analytics") { AnalyticsScreen() }
        composable("brain") { AIBrainScreen() }
        composable("tools") { QuickToolsScreen() }
        composable("notifications") { NotificationScreen() }
        composable("settings") { SettingsScreen() }
        composable("owner") { OwnerOverviewScreen() }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/MainActivity.kt << 'EOF'
package org.dpdns.bnai.anime
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import org.dpdns.bnai.anime.ui.navigation.AppNavGraph
import org.dpdns.bnai.anime.ui.theme.AnimeStudioTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnimeStudioTheme { val navController = rememberNavController(); AppNavGraph(navController = navController) } }
    }
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/library/MediaLibraryScreen.kt << 'EOF'
package org.dpdns.bnai.anime.ui.library
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun MediaLibraryScreen() { Scaffold(topBar = { TopAppBar(title = { Text("Media Library") }) }) { pad -> Text("Grid here", modifier = Modifier.padding(pad)) } }
EOF

echo "✅ Hathi (Final Fixed Version) Generate Ho Gaya!"
