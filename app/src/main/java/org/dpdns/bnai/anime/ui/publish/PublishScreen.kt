package org.dpdns.bnai.anime.ui.publish
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun PublishScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Publish Studio") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) { OutlinedTextField(value = "", onValueChange = {}, label = { Text("Title") }); Button(onClick = {}) { Text("Publish Now") } }
    }
}
