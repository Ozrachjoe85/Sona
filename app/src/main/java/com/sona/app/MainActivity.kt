package com.sona.app

import android.graphics.Bitmap
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
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val engine = SonaEngine()
    private val player = SonaAudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SonaApp(engine, player) }
    }
}

data class SonaLine(
    val path: Path, 
    val color: Color, 
    val thickness: Float, 
    val yOffset: Float
)

@Composable
fun SonaApp(engine: SonaEngine, player: SonaAudioPlayer) {
    val context = LocalContext.current
    val lines = remember { mutableStateListOf<SonaLine>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Cyan) }
    var currentThickness by remember { mutableStateOf(20f) }
    var uploadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Safety: Prevents audio spam/crashing
    var lastSoundY by remember { mutableStateOf(0f) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            uploadedBitmap = bitmap
            player.playProcedural(engine.analyzeBitmap(bitmap))
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0C))) {
        // --- HEADER ---
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(
                    Brush.linearGradient(listOf(Color.Cyan, Color.Magenta)), CircleShape
                ), contentAlignment = Alignment.Center
            ) {
                Text("S", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text("SONA // SYNTH", color = Color.White, letterSpacing = 2.sp)
        }

        // --- TOOLS ---
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp), 
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(Color.Cyan, Color.Magenta, Color.Yellow).forEach { color ->
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(if (currentColor == color) color else color.copy(alpha = 0.1f))
                        .border(2.dp, color, CircleShape)
                        .clickable { currentColor = color }
                )
            }
            Slider(
                value = currentThickness, 
                onValueChange = { currentThickness = it }, 
                valueRange = 10f..120f, 
                modifier = Modifier.weight(1f)
            )
        }

        // --- CANVAS ---
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(24.dp))
        ) {
            uploadedBitmap?.let { 
                Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.3f) 
            }

            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        currentPath = Path().apply { moveTo(it.x, it.y) }
                        lastSoundY = it.y
                    },
                    onDrag = { change, _ ->
                        currentPath?.lineTo(change.position.x, change.position.y)
                        val p = currentPath; currentPath = null; currentPath = p
                        
                        // RULE: Real-time Sonification with Throttle
                        if (abs(change.position.y - lastSoundY) > 25f) {
                            val freq = 850.0 - (change.position.y / 2.2).coerceIn(0.0, 700.0)
                            val wave = when(currentColor) {
                                Color.Cyan -> "sine"
                                Color.Magenta -> "square"
                                Color.Yellow -> "sawtooth"
                                else -> "sine"
                            }
                            player.playInstantTone(freq, wave)
                            lastSoundY = change.position.y
                        }
                    },
                    onDragEnd = {
                        currentPath?.let { lines.add(SonaLine(it, currentColor, currentThickness, lastSoundY)) }
                        currentPath = null
                    }
                )
            }) {
                lines.forEach { drawPath(it.path, it.color, style = Stroke(it.thickness, cap = StrokeCap.Round)) }
                currentPath?.let { drawPath(it, currentColor, style = Stroke(currentThickness, cap = StrokeCap.Round)) }
            }
        }

        // --- ACTIONS ---
        Row(Modifier.fillMaxWidth().padding(bottom = 40.dp), Arrangement.SpaceEvenly) {
            Button(
                onClick = { lines.clear(); uploadedBitmap = null },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) { Text("SCRUB") }

            Button(
                onClick = { launcher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)
            ) { Text("SCAN") }

            Button(
                onClick = { 
                    val notes = lines.map { engine.convertLineToNote(it) }
                    player.playProcedural(notes) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black)
            ) { Text("PLAY ART") }
        }
    }
}
