package org.dpdns.bnai.anime.ui.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen() {
    var youtubeTitle by rememberSaveable {
        mutableStateOf("Shadows of Aethelgard - Episode 5: The Crimson Eclipse [Full Hindi Anime]")
    }
    var youtubeDescription by rememberSaveable {
        mutableStateOf(
            "Watch Episode 5 of Shadows of Aethelgard. Generated & verified with AI Anime Studio (WF-07 Publisher)."
        )
    }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var publishSuccessReceipt by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm YouTube Publication",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Text(
                    text = "This will publish to your connected channel. Proceed?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        coroutineScope.launch {
                            isPublishing = true
                            publishSuccessReceipt = null
                            delay(1500)
                            isPublishing = false
                            publishSuccessReceipt =
                                "Published successfully! Platform Receipt ID: YT_VID_98412A (Verified via WF-07)"
                        }
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Publish Studio (S14)",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = youtubeTitle,
                onValueChange = { youtubeTitle = it },
                label = { Text("YouTube Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = youtubeDescription,
                onValueChange = { youtubeDescription = it },
                label = { Text("Description") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isPublishing && youtubeTitle.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Publish Now",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            publishSuccessReceipt?.let { receipt ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = receipt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
