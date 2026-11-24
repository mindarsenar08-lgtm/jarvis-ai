package com.jarvis.ai

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val Navy = Color(0xFF071321)
private val Panel = Color(0xFF10283B)
private val Cyan = Color(0xFF43D9F5)
private val Muted = Color(0xFF9DB7C9)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { JarvisApp(onSpeak = { speak(it) }) }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "IN")
            tts?.setSpeechRate(0.94f)
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

data class ChatLine(val fromUser: Boolean, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisApp(onSpeak: (String) -> Unit) {
    var selected by remember { mutableStateOf("Home") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("SYSTEM ONLINE • READY") }
    val chats = remember { mutableStateListOf(ChatLine(false, "Hello! I am JARVIS. How can I help you today?")) }
    val scope = rememberCoroutineScope()

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) message = spoken
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, background = Navy, surface = Panel,
        onPrimary = Navy, onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(
            containerColor = Navy,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF091B2B)) {
                    listOf("Home", "Chat", "Studio", "Projects", "Settings").forEach { item ->
                        NavigationBarItem(
                            selected = selected == item,
                            onClick = { selected = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        "Home" -> Icons.Default.Home
                                        "Chat" -> Icons.Default.Chat
                                        "Studio" -> Icons.Default.AutoAwesome
                                        "Projects" -> Icons.Default.Folder
                                        else -> Icons.Default.Settings
                                    }, contentDescription = item
                                )
                            },
                            label = { Text(item, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Cyan,
                                selectedTextColor = Cyan,
                                indicatorColor = Panel,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                "Chat" -> ChatScreen(
                    modifier = Modifier.padding(padding),
                    chats = chats, message = message, onMessage = { message = it },
                    busy = busy,
                    onMic = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
                        }
                        try { speechLauncher.launch(intent) } catch (_: Exception) {
                            status = "VOICE INPUT UNAVAILABLE ON THIS DEVICE"
                        }
                    },
                    onSend = {
                        val prompt = message.trim()
                        if (prompt.isNotEmpty() && !busy) {
                            chats.add(ChatLine(true, prompt))
                            message = ""
                            busy = true
                            status = "JARVIS IS THINKING"
                            scope.launch {
                                val answer = askBackend(prompt)
                                chats.add(ChatLine(false, answer))
                                onSpeak(answer)
                                busy = false
                                status = "SYSTEM ONLINE • READY"
                            }
                        }
                    }
                )
                "Studio" -> InfoScreen("AI STUDIO", "Creative tools are ready to be connected.", listOf(
                    "Image generation — connect an image provider",
                    "Video editing — local media pipeline required",
                    "Captions and subtitles — speech API required",
                    "YouTube assets — manual review before publishing"
                ), Modifier.padding(padding))
                "Projects" -> InfoScreen("PROJECTS", "Your workspace for ongoing work.", listOf(
                    "Project planning", "Task breakdown", "Build notes", "Exportable project history"
                ), Modifier.padding(padding))
                "Settings" -> InfoScreen("AI ENGINE SETTINGS", "Configure your secure JARVIS backend.", listOf(
                    "Backend URL: http://127.0.0.1:8000",
                    "Gemini key belongs on the backend only",
                    "Voice output uses Android TextToSpeech",
                    "Voice input uses Android speech recognition",
                    "Private mode and memory controls: planned integration"
                ), Modifier.padding(padding))
                else -> HomeScreen(status, Modifier.padding(padding), onChat = { selected = "Chat" })
            }
        }
    }
}

@Composable
fun HomeScreen(status: String, modifier: Modifier = Modifier, onChat: () -> Unit) {
    Column(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF06101D), Color(0xFF0B2940), Navy))
        ).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("J.A.R.V.I.S", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("PERSONAL AI SYSTEM", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Icon(Icons.Default.Shield, contentDescription = "Security", tint = Cyan)
        }
        Spacer(Modifier.height(54.dp))
        Box(
            Modifier.size(214.dp).background(
                Brush.radialGradient(listOf(Color(0xFF1A6680), Color(0xFF0D3048), Navy)),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(156.dp).background(Color(0xFF143E55), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GraphicEq, contentDescription = "JARVIS orb", tint = Cyan, modifier = Modifier.size(78.dp))
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("GOOD DAY", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(status, color = Cyan, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(34.dp))
        Button(
            onClick = onChat,
            colors = ButtonDefaults.buttonColors(containerColor = Cyan),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("TALK TO JARVIS", color = Navy, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("QUICK ACCESS", color = Muted, modifier = Modifier.align(Alignment.Start), fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile("AI Chat", Icons.Default.Chat, Modifier.weight(1f), onChat)
            QuickTile("Voice", Icons.Default.Mic, Modifier.weight(1f), onChat)
            QuickTile("Tasks", Icons.Default.CheckCircle, Modifier.weight(1f), onChat)
        }
        Spacer(Modifier.weight(1f))
        Text("JARVIS AI • ANDROID EDITION", color = Muted, fontSize = 10.sp)
    }
}

@Composable
fun QuickTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(92.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, tint = Cyan)
            Spacer(Modifier.height(7.dp))
            Text(label, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun ChatScreen(
    modifier: Modifier,
    chats: List<ChatLine>,
    message: String,
    onMessage: (String) -> Unit,
    busy: Boolean,
    onMic: () -> Unit,
    onSend: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        Text("JARVIS CHAT", color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Voice and text assistant", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(chats) { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (line.fromUser) Color(0xFF15506A) else Panel,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(line.text, Modifier.padding(13.dp), color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            if (busy) item { Text("JARVIS is processing…", color = Cyan, fontSize = 12.sp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMic) { Icon(Icons.Default.Mic, contentDescription = "Speak", tint = Cyan) }
            OutlinedTextField(
                value = message, onValueChange = onMessage,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message JARVIS…", color = Muted) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Cyan, unfocusedBorderColor = Muted
                )
            )
            IconButton(onClick = onSend, enabled = !busy) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Cyan)
            }
        }
    }
}

@Composable
fun InfoScreen(title: String, subtitle: String, rows: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp)) {
        Text(title, color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))
        rows.forEach { row ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) { Text(row, Modifier.padding(16.dp), color = Color.White, fontSize = 13.sp) }
        }
    }
}

private suspend fun askBackend(prompt: String): String = withContext(Dispatchers.IO) {
    try {
        val url = URL("http://127.0.0.1:8000/chat")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        val body = JSONObject().put("message", prompt).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        connection.disconnect()
        JSONObject(response).optString("reply", "Backend returned no reply.")
    } catch (_: Exception) {
        "I could not connect to the JARVIS backend. Start the backend server and check its URL in MainActivity.kt."
    }
}
