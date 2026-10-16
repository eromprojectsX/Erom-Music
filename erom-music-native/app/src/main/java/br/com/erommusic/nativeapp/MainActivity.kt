package br.com.erommusic.nativeapp

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Locale

private val Background = Color(0xFF101010)
private val SurfaceDark = Color(0xFF1B1B1B)
private val Green = Color(0xFF1ED760)
private val White = Color(0xFFF8F8F8)
private val Muted = Color(0xFFB3B3B3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 16, 16)
        window.navigationBarColor = android.graphics.Color.rgb(16, 16, 16)
        setContent { EromMusicApp() }
    }
}

@Composable
private fun EromMusicApp() {
    val context = LocalContext.current
    val musicFolder = remember { getMusicFolder(context).also { it.mkdirs() } }
    val songs = remember { mutableStateListOf<File>() }
    var currentSong by remember { mutableStateOf<File?>(null) }
    var playing by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Biblioteca") }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun refreshSongs() {
        songs.clear()
        songs.addAll(musicFolder.listFiles()
            ?.filter { it.isFile && it.extension.lowercase(Locale.ROOT) in setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus") }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            ?: emptyList())
    }

    fun playSong(file: File) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { playing = false }
                prepare()
                start()
            }
            currentSong = file
            playing = true
        } catch (_: Exception) {
            playing = false
            Toast.makeText(context, "Não foi possível reproduzir este arquivo.", Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        var imported = 0
        uris.forEach { uri ->
            try {
                val name = safeFileName(context, uri)
                val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (ext !in setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")) return@forEach
                val target = uniqueFile(musicFolder, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                if (target.exists() && target.length() > 0L) imported++
                else target.delete()
            } catch (_: Exception) {
                // Skip unreadable files and continue importing the others.
            }
        }
        refreshSongs()
        Toast.makeText(context, "$" + "{imported} música(s) importada(s).", Toast.LENGTH_LONG).show()
    }

    DisposableEffect(Unit) {
        refreshSongs()
        onDispose { player?.release(); player = null }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Green, background = Background, surface = SurfaceDark,
        onPrimary = Color.Black, onBackground = White, onSurface = White
    )) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF080808), contentColor = White) {
                    listOf(
                        Triple("Biblioteca", Icons.Default.LibraryMusic, "Biblioteca"),
                        Triple("Buscar", Icons.Default.Search, "Buscar")
                    ).forEach { (label, icon, route) ->
                        NavigationBarItem(
                            selected = selectedTab == route,
                            onClick = { selectedTab = route },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green, selectedTextColor = White,
                                unselectedIconColor = Muted, unselectedTextColor = Muted,
                                indicatorColor = SurfaceDark
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(Background)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.GraphicEq, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("EROM", fontSize = 12.sp, color = Green, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("MÚSICA", fontSize = 22.sp, color = White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { importLauncher.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.Add, "Importar músicas", tint = Green)
                    }
                }

                if (selectedTab == "Buscar") {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        placeholder = { Text("Buscar músicas importadas", color = Muted) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White, unfocusedTextColor = White,
                            focusedBorderColor = Green, unfocusedBorderColor = Muted,
                            cursorColor = Green, focusedLeadingIconColor = Green,
                            unfocusedLeadingIconColor = Muted
                        )
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (selectedTab == "Buscar") "Resultados locais" else "Sua biblioteca", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("$" + "{songs.size} música(s) • funciona offline", color = Muted, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("audio/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Importar")
                    }
                }

                Text(
                    "Armazenamento Erom Music • arquivos selecionados por você; WhatsApp não é varrido.",
                    color = Muted, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                val visibleSongs = songs.filter { it.name.contains(search, ignoreCase = true) }
                if (visibleSongs.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().weight(1f).padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.LibraryMusic, null, tint = Green, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(if (songs.isEmpty()) "Sua biblioteca está vazia" else "Nenhuma música encontrada", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Importe arquivos de áudio do dispositivo. Eles serão copiados para a pasta do Erom Music e poderão tocar sem internet.", color = Muted, fontSize = 14.sp)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { importLauncher.launch(arrayOf("audio/*")) }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Escolher músicas")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleSongs, key = { it.absolutePath }) { song ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceDark).clickable { playSong(song) }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF253F32)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = Green, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.nameWithoutExtension, color = White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatBytes(song.length()), color = Muted, fontSize = 12.sp)
                                }
                                IconButton(onClick = { playSong(song) }) {
                                    Icon(
                                        if (currentSong == song && playing) Icons.Default.GraphicEq else Icons.Default.PlayCircleFilled,
                                        "Tocar " + song.nameWithoutExtension, tint = Green, modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                currentSong?.let { song ->
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF080808)).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("TOCANDO AGORA", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(song.nameWithoutExtension, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = {
                            val p = player
                            if (p != null) {
                                if (playing) { p.pause(); playing = false }
                                else { p.start(); playing = true }
                            }
                        }) {
                            Icon(if (playing) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, if (playing) "Pausar" else "Continuar", tint = Green, modifier = Modifier.size(34.dp))
                        }
                        IconButton(onClick = {
                            player?.stop()
                            player?.release()
                            player = null
                            currentSong = null
                            playing = false
                        }) { Icon(Icons.Default.Close, "Parar", tint = Muted) }
                    }
                }
            }
        }
    }
}

private fun getMusicFolder(context: Context): File {
    val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "Music")
    return File(base, "Erom Music")
}

private fun safeFileName(context: Context, uri: Uri): String {
    var name: String? = null
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) name = cursor.getString(0)
    }
    val cleaned = (name ?: "musica_" + System.currentTimeMillis() + ".mp3")
        .substringAfterLast('/').substringAfterLast('\\')
        .replace(Regex("[^\\p{L}\\p{N}._ -]"), "_")
        .trim()
    return cleaned.ifBlank { "musica_" + System.currentTimeMillis() + ".mp3" }
}

private fun uniqueFile(folder: File, name: String): File {
    val safe = name.ifBlank { "musica.mp3" }
    var candidate = File(folder, safe)
    var index = 1
    while (candidate.exists()) {
        val stem = safe.substringBeforeLast('.', safe)
        val ext = safe.substringAfterLast('.', "")
        candidate = File(folder, if (ext.isBlank()) "$" + "{stem} ($index)" else "$" + "{stem} ($index).$" + "{ext}")
        index++
    }
    return candidate
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$" + "{bytes} B"
        bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
        else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
