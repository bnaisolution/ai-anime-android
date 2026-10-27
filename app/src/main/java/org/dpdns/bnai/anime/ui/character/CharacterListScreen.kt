package org.dpdns.bnai.anime.ui.character
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.model.Character
@Composable
fun CharacterListScreen(onCharacterClick: (Character) -> Unit = {}) {
    val chars = listOf(Character("CHAR_001", "Aarav", "Protagonist", "Face: Sharp | Hair: Black", "svara_hi_male"), Character("CHAR_002", "Meera", "Deuteragonist", "Face: Fierce | Hair: Silver", "svara_hi_female"))
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        items(chars) { c -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onCharacterClick(c) }) { Column(Modifier.padding(16.dp)) { Text(c.name, style = MaterialTheme.typography.headlineMedium); Text(c.role, color = MaterialTheme.colorScheme.secondary) } } }
    }
}
