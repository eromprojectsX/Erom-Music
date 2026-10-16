package br.com.erommusic.nativeapp

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import java.util.zip.ZipInputStream

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
    var showLyrics by remember { mutableStateOf(false) }
    var accentIndex by remember { mutableIntStateOf(0) }
    val accents = listOf(Color(0xFF1ED760), Color(0xFF58A6FF), Color(0xFFB78AFF), Color(0xFFFF5B70), Color(0xFFFFB84D))
    val accent = accents[accentIndex]
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
            val intent = Intent(context, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_PLAY)
                .putExtra(PlaybackService.EXTRA_PATH, file.absolutePath)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            currentSong = file
            playing = true
        } catch (_: Exception) {
            playing = false
            Toast.makeText(context, "Não foi possível iniciar a reprodução.", Toast.LENGTH_LONG).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        var imported = 0
        var failed = 0
        val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
        uris.forEach { uri ->
            try {
                val name = safeFileName(context, uri)
                val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (ext == "zip") {
                    imported += importZip(context, uri, musicFolder, audioExtensions)
                } else if (ext in audioExtensions) {
                    val target = uniqueFile(musicFolder, name)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (target.exists() && target.length() > 0L) imported++ else target.delete()
                }
            } catch (_: Exception) {
                failed++
            }
        }
        refreshSongs()
        val message = if (failed == 0) "${imported} música(s) importada(s)." else "${imported} importada(s); ${failed} arquivo(s) não puderam ser lidos."
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    DisposableEffect(Unit) {
        refreshSongs()
        onDispose { /* A reprodução pertence ao serviço e continua com a tela fechada. */ }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = accent, background = Background, surface = SurfaceDark,
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
                    IconButton(onClick = { importLauncher.launch(arrayOf("audio/*", "application/zip", "application/x-zip-compressed")) }) {
                        Icon(Icons.Default.Add, "Importar músicas", tint = accent)
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
                        Text("${songs.size} música(s) • funciona offline", color = Muted, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("audio/*", "application/zip", "application/x-zip-compressed")) },
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

                val recentSongs = songs.sortedByDescending { it.lastModified() }.take(8)
                if (selectedTab == "Biblioteca" && recentSongs.isNotEmpty()) {
                    Text(
                        "Adicionadas recentemente",
                        color = White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 10.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentSongs, key = { "recent-" + it.absolutePath }) { song ->
                            Column(
                                Modifier.width(148.dp).clickable { playSong(song) },
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF202C3A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(58.dp))
                                    if (currentSong == song) {
                                        Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(24.dp))
                                    }
                                }
                                Text(song.nameWithoutExtension, color = White, fontWeight = FontWeight.SemiBold,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

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
                        Button(onClick = { importLauncher.launch(arrayOf("audio/*", "application/zip", "application/x-zip-compressed")) }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)) {
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
                    Column(
                        Modifier.fillMaxWidth().background(Color(0xFF071A1D)).clickable { showLyrics = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF253F48)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Album, null, tint = accent, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.nameWithoutExtension, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                Text(if (playing) "Tocando agora • Erom Music" else "Pausado • Erom Music", color = Muted, fontSize = 12.sp, maxLines = 1)
                            }
                            IconButton(onClick = { showLyrics = true }) {
                                Icon(Icons.Default.Lyrics, "Ver letra", tint = White)
                            }
                            IconButton(onClick = {
                                val action = if (playing) PlaybackService.ACTION_PAUSE else PlaybackService.ACTION_RESUME
                                context.startService(Intent(context, PlaybackService::class.java).setAction(action))
                                playing = !playing
                            }) {
                                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pausar" else "Continuar", tint = White, modifier = Modifier.size(30.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (playing) 0.38f else 0f },
                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                            color = accent,
                            trackColor = Color(0xFF536168)
                        )
                    }
                }

                if (showLyrics) {
                    val lyricFile = currentSong?.let { File(it.parentFile, it.nameWithoutExtension + ".lrc") }
                    val lyricText = lyricFile?.takeIf { it.isFile }?.readText().orEmpty()
                    AlertDialog(
                        onDismissRequest = { showLyrics = false },
                        containerColor = SurfaceDark,
                        title = { Text(currentSong?.nameWithoutExtension ?: "Letra da música", color = White) },
                        text = {
                            Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                                Text(
                                    if (lyricText.isBlank()) "Nenhuma letra local encontrada. Para usar letras offline, importe um arquivo .lrc com o mesmo nome da música. Traduções podem ser incluídas abaixo da letra original no próprio arquivo." else lyricText,
                                    color = White,
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLyrics = false }) { Text("Fechar", color = accent) }
                        }
                    )
                }
            }
        }
    }
}


private fun importZip(
    context: Context,
    uri: Uri,
    folder: File,
    audioExtensions: Set<String>
): Int {
    var imported = 0
    var totalBytes = 0L
    val maxTotalBytes = 512L * 1024L * 1024L
    val maxEntryBytes = 128L * 1024L * 1024L
    context.contentResolver.openInputStream(uri)?.use { raw ->
        ZipInputStream(raw).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name.replace('\\', '/')
                    val baseName = entryName.substringAfterLast('/')
                    val ext = baseName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    if (baseName.isNotBlank() && ext in audioExtensions && !entryName.startsWith("/") && !entryName.split('/').any { it == ".." }) {
                        val target = uniqueFile(folder, baseName)
                        var entryBytes = 0L
                        try {
                            target.outputStream().use { output ->
                                val buffer = ByteArray(16 * 1024)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    entryBytes += count
                                    totalBytes += count
                                    if (entryBytes > maxEntryBytes || totalBytes > maxTotalBytes) {
                                        throw IllegalStateException("ZIP excede o limite de extração")
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                            if (target.exists() && target.length() > 0L) imported++ else target.delete()
                        } catch (e: Exception) {
                            target.delete()
                            throw e
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    return imported
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
        candidate = File(folder, if (ext.isBlank()) "${stem} ($index)" else "${stem} ($index).${ext}")
        index++
    }
    return candidate
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
        else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
