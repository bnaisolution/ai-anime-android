package org.dpdns.bnai.anime.ui.library
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun MediaLibraryScreen() { Scaffold(topBar = { TopAppBar(title = { Text("Media Library") }) }) { pad -> Text("Grid here", modifier = Modifier.padding(pad)) } }
