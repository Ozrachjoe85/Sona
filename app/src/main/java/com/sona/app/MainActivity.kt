package com.sona.app

import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sona.app.engine.*

class MainActivity : ComponentActivity() {
    private val engine = SonaEngine()
    private val player = SonaAudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SonaApp(engine, player) }
    }
}

data class SonaLine(val path: Path, val color: Color, val thickness: Float, val yOffset: Float)

@Composable
fun SonaApp(engine: SonaEngine, player: SonaAudioPlayer) {
    val context = LocalContext.current
    val lines = remember { mutableStateListOf<SonaLine>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Cyan) }
    var currentThickness by remember { mutableStateOf(15f) }
    var uploadedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            uploadedBitmap = bitmap
            player.playProcedural(engine.analyzeBitmap(bitmap))
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF08080A))) {
        // --- HEADER / ICON ---
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).background(Brush.linearGradient(listOf(Color.Cyan, Color.Magenta)), CircleShape), contentAlignment = Alignment.Center) {
                Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Spacer(Modifier.width(15.dp))
            Text("SONA // OS", color = Color.White, letterSpacing = 4.sp, fontWeight = FontWeight.Light)
        }

        // --- TOOLS ---
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            listOf(Color.Cyan, Color.Magenta, Color.Yellow).forEach { color ->
                Box(Modifier.size(45.dp).clip(CircleShape).background(if (currentColor == color) color else color.copy(alpha = 0.2f)).border(2.dp, color, CircleShape).clickable { currentColor = color })
            }
            Slider(value = currentThickness, onValueChange = { currentThickness = it }, valueRange = 5f..100f, modifier = Modifier.weight(1f))
        }

        // --- CANVAS ---
        Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xFF121215)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(30.dp))) {
            uploadedBitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.4f) }

            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentPath = Path().apply { moveTo(it.x, it.y) } },
                    onDrag = { change, _ ->
                        currentPath?.lineTo(change.position.x, change.position.y)
                        val p = currentPath; currentPath = null; currentPath = p
                        
                        // REAL-TIME FEEDBACK: Play tone based on current Y and Color
                        val freq = 900.0 - (change.position.y / 2.2).coerceIn(0.0, 750.0)
                        val wave = when(currentColor) {
                            Color.Cyan -> "sine"; Color.Magenta -> "square"; Color.Yellow -> "sawtooth"; else -> "sine"
                        }
                        player.playInstantTone(freq, wave)
                    },
                    onDragEnd = {
                        currentPath?.let { lines.add(SonaLine(it, currentColor, currentThickness, 0f)) }
                        currentPath = null
                    }
                )
            }) {
                lines.forEach { drawPath(it.path, it.color, style = Stroke(it.thickness, cap = StrokeCap.Round)) }
                currentPath?.let { drawPath(it, currentColor, style = Stroke(currentThickness, cap = StrokeCap.Round)) }
            }
        }

        // --- ACTIONS ---
        Row(Modifier.fillMaxWidth().padding(bottom = 30.dp), Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { lines.clear(); uploadedBitmap = null }, border = BorderStroke(1.dp, Color.Gray)) { Text("SCRUB", color = Color.White) }
            Button(onClick = { launcher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)) { Text("SCAN") }
            Button(onClick = { player.playProcedural(lines.map { engine.convertLineToNote(it) }) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black)) { Text("COMPOSE") }
        }
    }
}
