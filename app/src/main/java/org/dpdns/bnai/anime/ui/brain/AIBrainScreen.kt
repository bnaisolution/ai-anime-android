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
