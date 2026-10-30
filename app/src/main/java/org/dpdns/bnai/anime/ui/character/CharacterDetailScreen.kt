package org.dpdns.bnai.anime.ui.character
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
@Composable
fun CharacterDetailScreen(character: Character = Character("CHAR_001", "Aarav Vanshi", "Protagonist", "Face: Sharp jawline", "svara_hi_male_hero_01")) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AnimeRepository() }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text(text = character.name.take(2).uppercase(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
        Text(text = character.name, style = MaterialTheme.typography.headlineLarge)
        Text(text = "${character.role} (${character.id})", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Column(modifier = Modifier.padding(16.dp)) { Text("Visual DNA", style = MaterialTheme.typography.headlineMedium); Text(character.visualDna) } }
        Button(onClick = { scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.PREVIEW_VOICE, "QUICK", payload = mapOf("character_code" to character.id)); Toast.makeText(context, "WF-04 Voice Preview requested", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Preview Voice") }
    }
}
