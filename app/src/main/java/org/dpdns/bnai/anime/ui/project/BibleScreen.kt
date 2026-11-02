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
