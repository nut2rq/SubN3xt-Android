package com.subn3xt.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0B1020)
private val Panel = Color(0xFF151D31)
private val Violet = Color(0xFF8B7CFF)
private val Mint = Color(0xFF54E0C1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SubN3xtApp() }
    }
}

@Composable
fun SubN3xtApp() {
    var tab by remember { mutableStateOf("Home") }
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet,
        secondary = Mint, onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Color.White) {
                listOf("Home", "Projects", "AI Studio", "Quality", "Settings").forEach { item ->
                    val icon = when(item) {
                        "Home" -> Icons.Rounded.Home
                        "Projects" -> Icons.Rounded.VideoLibrary
                        "AI Studio" -> Icons.Rounded.AutoAwesome
                        "Quality" -> Icons.Rounded.Verified
                        else -> Icons.Rounded.Settings
                    }
                    NavigationBarItem(selected = tab == item, onClick = { tab = item },
                        icon = { Icon(icon, contentDescription = item) },
                        label = { Text(item, fontSize = 10.sp) })
                }
            }
        }) { padding ->
            when(tab) {
                "Home" -> HomeScreen(Modifier.padding(padding), onNew = { tab = "Projects" })
                "Projects" -> SimpleScreen(Modifier.padding(padding), "Projects", "Your subtitle projects will appear here.", Icons.Rounded.VideoLibrary)
                "AI Studio" -> SimpleScreen(Modifier.padding(padding), "AI Studio", "Whisper transcription and optional Gemini-assisted editing.", Icons.Rounded.AutoAwesome)
                "Quality" -> SimpleScreen(Modifier.padding(padding), "Quality Check", "Review reading speed, line length, timing and overlaps.", Icons.Rounded.Verified)
                else -> SimpleScreen(Modifier.padding(padding), "Settings", "Whisper model • Gemini API • Export folder • Appearance", Icons.Rounded.Settings)
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onNew: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 30.dp, bottom = 24.dp)) {
        item {
            Text("SUBN3XT", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("Studio", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text("Next Generation AI Subtitle Studio", color = Color(0xFF9CA9C5), fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF29285A), Color(0xFF172E45))), RoundedCornerShape(26.dp)).padding(22.dp)) {
                Column {
                    Surface(color = Violet.copy(alpha = .18f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Mint, modifier = Modifier.padding(10.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Turn video into
beautiful subtitles.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Transcribe • Edit • Translate • Export", color = Color(0xFFC2C8E0), fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onNew, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet)) {
                        Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Create New Project", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Text("WORKSPACE", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Projects", "0", Icons.Rounded.VideoLibrary, Modifier.weight(1f))
                StatCard("Ready to export", "0", Icons.Rounded.Subtitles, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
            Text("YOUR WORKFLOW", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            listOf("1  Import video or audio", "2  Transcribe with Whisper", "3  Refine and translate with AI", "4  Quality check and export").forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Mint, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(12.dp)); Text(line, color = Color(0xFFD7DDF0), fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Built for creators. Designed for precision.", color = Color(0xFF77839F), fontSize = 12.sp)
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Mint)
            Spacer(Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color(0xFF9CA9C5), fontSize = 11.sp)
        }
    }
}

@Composable
fun SimpleScreen(modifier: Modifier, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Mint, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color(0xFF9CA9C5), fontSize = 14.sp)
    }
}
