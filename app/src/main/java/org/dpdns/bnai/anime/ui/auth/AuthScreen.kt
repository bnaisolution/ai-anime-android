package org.dpdns.bnai.anime.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(LocalContext.current))
) {
    val authState by viewModel.authState.collectAsState()
    val tokenManager = remember { TokenManager.getInstance() }
    val expiredMsg by (tokenManager?.sessionExpiredMessage?.collectAsState() ?: remember { mutableStateOf(null) })
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
        if (authState is Resource.Success) {
            navController.navigate("studio") {
                popUpTo("auth") { inclusive = true }
            }
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
            // Fix #387: Studio Logo Artwork Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI Anime Studio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("AI Cinematic Anime Studio", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Multi-Agent Story, Production & BYOD Engine", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(24.dp))

            val errorMsg = (authState as? Resource.Error)?.message
            val hasError = errorMsg != null

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Studio Email") },
                singleLine = true,
                isError = hasError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                isError = hasError,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(if (isPasswordVisible) "Hide" else "Show")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Fix #390: Inline red error text below input
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val isLoading = authState is Resource.Loading
            Button(
                onClick = { viewModel.login(email, password) },
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Sign In to Studio")
                }
            }

            // Fix #388 & #389: Create account and Forgot Password links
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    scope.launch { snackbarHostState.showSnackbar("Contact workspace admin or use BYOD registration portal.") }
                }) {
                    Text("Create an account")
                }
                TextButton(onClick = {
                    scope.launch { snackbarHostState.showSnackbar("Password reset link will be sent via Studio Auth.") }
                }) {
                    Text("Forgot Password?")
                }
            }
        }
    }
}
