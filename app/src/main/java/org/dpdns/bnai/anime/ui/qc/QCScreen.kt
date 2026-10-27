package org.dpdns.bnai.anime.ui.qc
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun QCScreen(onProceed: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("QC Gate") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) { Text("Audio Sync: PASSED"); Button(onClick = onProceed) { Text("Proceed to Publish") } }
    }
}
