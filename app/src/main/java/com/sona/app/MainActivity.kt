package com.sona.app

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sona.app.engine.*

class MainActivity : ComponentActivity() {
    private val engine = SonaEngine()
    private val player = SonaAudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SonaApp(engine, player) }
    }
}

data class Line(val path: Path, val color: Color, val strokeWidth: Float)

@Composable
fun SonaApp(engine: SonaEngine, player: SonaAudioPlayer) {
    val context = LocalContext.current
    val lines = remember { mutableStateListOf<Line>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentThickness by remember { mutableStateOf(8f) }

    // Image Picker Launcher
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            player.play(engine.analyzeBitmap(bitmap))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Toolbar
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green)
            colors.forEach { color ->
                Box(Modifier.size(40.dp).clip(CircleShape).background(color).clickable { currentColor = color })
            }
            Slider(value = currentThickness, onValueChange = { currentThickness = it }, valueRange = 2f..40f, modifier = Modifier.width(100.dp))
        }

        // Canvas
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> currentPath = Path().apply { moveTo(offset.x, offset.y) } },
                    onDrag = { change, _ ->
                        currentPath?.lineTo(change.position.x, change.position.y)
                        val p = currentPath; currentPath = null; currentPath = p
                    },
                    onDragEnd = { currentPath?.let { lines.add(Line(it, currentColor, currentThickness)) }; currentPath = null }
                )
            }) {
                lines.forEach { drawPath(it.path, it.color, style = Stroke(it.strokeWidth)) }
                currentPath?.let { drawPath(it, currentColor, style = Stroke(currentThickness)) }
            }
        }

        // Bottom Controls
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { lines.clear() }) { Text("Clear") }
            Button(onClick = { launcher.launch("image/*") }) { Text("Upload") }
            Button(onClick = { 
                // Play logic based on line count/colors for now
                player.play(engine.generateFromPaths(lines.size)) 
            }) { Text("Hear") }
        }
    }
}
