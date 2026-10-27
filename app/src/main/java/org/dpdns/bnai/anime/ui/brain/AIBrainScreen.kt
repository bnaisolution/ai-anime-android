package org.dpdns.bnai.anime.ui.brain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class BrainChatMessage(
    val id: String,
    val senderIsUser: Boolean,
    val text: String,
    val citationSource: String = "Source: Story Bible"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBrainScreen() {
    val messages = remember {
        mutableStateListOf(
            BrainChatMessage(
                id = "msg_1",
                senderIsUser = true,
                text = "Can Aarav cross the Obsidian Rift in Episode 5 without a Soul Anchor?"
            ),
            BrainChatMessage(
                id = "msg_2",
                senderIsUser = false,
                text = "No. According to World Rule #2, no mortal may cross the Obsidian Rift without a bound Soul Anchor. Doing so triggers a Continuity Engine (WF-06) violation.",
                citationSource = "Source: Story Bible"
            )
        )
    }

    var inputText by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Brain & Knowledge (S16)",
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
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubbleRow(message = message)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask AI Brain about Canon, Rules, or Characters...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty()) {
                            val userMsgId = "msg_${messages.size + 1}"
                            val aiMsgId = "msg_${messages.size + 2}"
                            messages.add(
                                BrainChatMessage(
                                    id = userMsgId,
                                    senderIsUser = true,
                                    text = trimmed
                                )
                            )
                            messages.add(
                                BrainChatMessage(
                                    id = aiMsgId,
                                    senderIsUser = false,
                                    text = "Verified against pgvector memory_knowledge & Canon Snapshot: \"$trimmed\" complies with current Season 1 continuity constraints.",
                                    citationSource = "Source: Story Bible"
                                )
                            )
                            inputText = ""
                        }
                    },
                    modifier = Modifier.height(54.dp)
                ) {
                    Text(
                        text = "Send",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleRow(message: BrainChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.senderIsUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (message.senderIsUser) 14.dp else 2.dp,
                bottomEnd = if (message.senderIsUser) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.senderIsUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.senderIsUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(14.dp)
            )
        }

        if (!message.senderIsUser) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = message.citationSource,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
