package org.dpdns.bnai.anime.ui.character
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
@Composable
fun CharacterListScreen(onCharacterClick: (Character) -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    val characters = remember { mutableStateListOf<Character>() }
    LaunchedEffect(Unit) {
        when (val res = repository.fetchCharacters(repository.currentProjectId())) {
            is Resource.Success -> { if (res.data.isNotEmpty()) { characters.clear(); characters.addAll(res.data) } }
            else -> Unit
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(characters, key = { it.id }) { character ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onCharacterClick(character) }, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = character.name, style = MaterialTheme.typography.headlineMedium)
                        Text(text = character.role, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(text = character.id, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
