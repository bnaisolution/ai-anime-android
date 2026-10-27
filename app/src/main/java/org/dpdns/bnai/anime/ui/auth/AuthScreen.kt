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
