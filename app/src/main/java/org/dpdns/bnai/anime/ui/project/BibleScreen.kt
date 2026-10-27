package org.dpdns.bnai.anime.ui.project

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun BibleScreen(
    projectId: String = "proj_001",
    viewModel: ProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val bibleResource by viewModel.bibleState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.fetchBible(projectId)
    }

    when (val state = bibleResource) {
        is Resource.Loading, is Resource.Idle -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is Resource.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        is Resource.Success -> {
            val bible = state.data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Master Story Bible",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = bible.universeTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )

                OutlinedTextField(
                    value = bible.worldRules,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("World Rules") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bible.magicSystem,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Magic System") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bible.ToneAndThemeText(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tone/Theme") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Propose Bible Change",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private fun StoryBibleData.ToneAndThemeText(): String = this.toneAndTheme
