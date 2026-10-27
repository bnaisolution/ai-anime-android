#!/bin/bash
mkdir -p app/src/main/java/org/dpdns/bnai/anime/{ui/theme,ui/navigation,ui/auth,core/network,core/auth}
mkdir -p .github/workflows

# --- CONFIG FILES ---
cat > build.gradle.kts << 'EOF'
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
EOF

cat > settings.gradle.kts << 'EOF'
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "BnaiAnime"
include(":app")
EOF

cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
EOF

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
    kotlinOptions { jvmTarget = "17" }
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
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.dpdns.bnai.anime">
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

cat > .github/workflows/android-build.yml << 'EOF'
name: Build Anime APK
on:
  push:
    branches: [ "main" ]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.7'
      - name: Build Debug APK
        run: gradle assembleDebug --no-daemon
      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: anime-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
EOF

# --- MAIN ACTIVITY ---
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
        setContent {
            AnimeStudioTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
EOF

# --- THEME ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/theme/Color.kt << 'EOF'
package org.dpdns.bnai.anime.ui.theme
import androidx.compose.ui.graphics.Color
val Primary = Color(0xFF3F51B5)
val Secondary = Color(0xFF009688)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/ui/theme/Theme.kt << 'EOF'
package org.dpdns.bnai.anime.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary, onPrimary = Color.White, secondary = Secondary, onSecondary = Color.White,
    background = BackgroundDark, onBackground = Color.White, surface = SurfaceDark, onSurface = Color.White, error = Color(0xFFCF6679)
)
@Composable
fun AnimeStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}
EOF

# --- NETWORK & AUTH ---
cat > app/src/main/java/org/dpdns/bnai/anime/core/network/Resource.kt << 'EOF'
package org.dpdns.bnai.anime.core.network
sealed class Resource<out T> {
    object Idle : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
EOF

cat > app/src/main/java/org/dpdns/bnai/anime/core/auth/TokenManager.kt << 'EOF'
package org.dpdns.bnai.anime.core.auth
import android.content.Context
import android.content.SharedPreferences
class TokenManager(private val prefs: SharedPreferences) {
    fun saveToken(token: String) { prefs.edit().putString("jwt_token", token).apply() }
    fun getToken(): String? = prefs.getString("jwt_token", null)
    fun clearToken() { prefs.edit().remove("jwt_token").apply() }
    companion object {
        @Volatile private var instance: TokenManager? = null
        fun init(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext.getSharedPreferences("anime_prefs", Context.MODE_PRIVATE)).also { instance = it }
            }
        }
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
interface AnimeApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
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
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().header("Accept", "application/json")
                tokenManager.getToken()?.let { req.header("Authorization", "Bearer $it") }
                chain.proceed(req.build())
            }.connectTimeout(30, TimeUnit.SECONDS).build()
        return Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(AnimeApiService::class.java)
    }
}
EOF

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
                if (res.isSuccessful && res.body() != null) {
                    tokenManager.saveToken(res.body()!!.token)
                    _authState.value = Resource.Success(res.body()!!.token)
                } else {
                    _authState.value = Resource.Error("Login failed: HTTP ${res.code()}")
                }
            } catch (e: Exception) {
                _authState.value = Resource.Error(e.message ?: "Network error")
            }
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
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

    LaunchedEffect(state) {
        if (state is Resource.Success) navController.navigate("studio") { popUpTo("auth") { inclusive = true } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("AI Anime Studio", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { vm.login(email, password) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            if (state is Resource.Loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            else Text("Login")
        }
        if (state is Resource.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text((state as Resource.Error).message, color = MaterialTheme.colorScheme.error)
        }
    }
}
EOF

# --- NAVIGATION ---
cat > app/src/main/java/org/dpdns/bnai/anime/ui/navigation/NavGraph.kt << 'EOF'
package org.dpdns.bnai.anime.ui.navigation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.dpdns.bnai.anime.ui.auth.AuthScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") { AuthScreen(navController) }
        composable("studio") { ScreenStub("Studio Dashboard") }
    }
}

@Composable
fun ScreenStub(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name\n(Coming Soon)")
    }
}
EOF

echo "✅ Saara frontend code successfully create ho gaya!"
