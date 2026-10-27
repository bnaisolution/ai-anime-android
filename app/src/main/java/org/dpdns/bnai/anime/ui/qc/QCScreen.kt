package org.dpdns.bnai.anime.ui.qc
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
@Composable
fun QCScreen(onProceed: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("QC Gate") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                AndroidView(factory = { ctx -> VideoView(ctx).apply { setMediaController(MediaController(ctx).also { it.setAnchorView(this) }) } }, modifier = Modifier.fillMaxSize())
                Text("Video Preview", color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Text("Audio Sync: PASSED"); Text("Lipsync: PASSED")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onProceed) { Text("Proceed to Publish") }
        }
    }
}
