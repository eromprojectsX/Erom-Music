package br.com.erommusic.nativeapp

import android.os.Bundle
import android.content.Context
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri

private val Background = Color(0xFF101010)
private val SurfaceDark = Color(0xFF1B1B1B)
private val Green = Color(0xFF1ED760)
private val White = Color(0xFFF8F8F8)
private val Muted = Color(0xFFB3B3B3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16,16,16)
        window.navigationBarColor = android.graphics.Color.rgb(16,16,16)
        setContent { EromMusicApp() }
    }
}

@Composable
private fun EromMusicApp() {
    var selectedTab by remember { mutableStateOf("Início") }
    var search by remember { mutableStateOf("") }
    val context = LocalContext.current
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Green, background = Background, surface = SurfaceDark,
        onPrimary = Color.Black, onBackground = White, onSurface = White
    )) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF080808), contentColor = White) {
                    listOf(
                        Triple("Início", Icons.Default.Home, "Início"),
                        Triple("Buscar", Icons.Default.Search, "Buscar"),
                        Triple("Sua Biblioteca", Icons.Default.LibraryMusic, "Sua Biblioteca")
                    ).forEach { (label, icon, route) ->
                        NavigationBarItem(
                            selected = selectedTab == route,
                            onClick = { selectedTab = route },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green,
                                selectedTextColor = White,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = SurfaceDark
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).background(Background)
            ) {
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
                    IconButton(onClick = { selectedTab = "Buscar" }) {
                        Icon(Icons.Default.Search, "Buscar", tint = White)
                    }
                }
                when (selectedTab) {
                    "Início" -> HomeScreen(onOpenPlaylist = { openSpotifyPlaylist(context) }, onGoLibrary = { selectedTab = "Sua Biblioteca" })
                    "Buscar" -> SearchScreen(search, { search = it }, onOpenPlaylist = { openSpotifyPlaylist(context) })
                    else -> LibraryScreen(onOpenPlaylist = { openSpotifyPlaylist(context) }, onGoHome = { selectedTab = "Início" })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onOpenPlaylist: () -> Unit, onGoLibrary: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text("Boa música, do seu jeito.", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Seu espaço para ouvir e descobrir.", color = Muted, fontSize = 14.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("Sua biblioteca", Icons.Default.LibraryMusic, Modifier.weight(1f), onGoLibrary)
                QuickTile("Playlist Spotify", Icons.Default.PlayCircleFilled, Modifier.weight(1f), onOpenPlaylist)
            }
        }
        item {
            Text("Sua playlist", fontSize = 21.sp, color = White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            PlaylistCard(onOpenPlaylist)
        }
        item {
            Text("Feito para você", fontSize = 21.sp, color = White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF253F32)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, tint = Green, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Minha playlist Spotify", color = White, fontWeight = FontWeight.SemiBold)
                    Text("Acesse sua playlist pública", color = Muted, fontSize = 13.sp)
                }
                IconButton(onClick = onOpenPlaylist) { Icon(Icons.Default.PlayCircleFilled, "Abrir playlist", tint = Green, modifier = Modifier.size(32.dp)) }
            }
        }
    }
}

@Composable
private fun QuickTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceDark).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaylistCard(onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark).clickable(onClick = onOpen).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(84.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1F6D43)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.QueueMusic, null, tint = Green, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Minha playlist Spotify", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Playlist pública • Spotify", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text("TOCAR NO SPOTIFY", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Icon(Icons.Default.PlayCircleFilled, null, tint = Green, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun SearchScreen(query: String, onQueryChange: (String) -> Unit, onOpenPlaylist: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Buscar", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Artistas, músicas ou playlists", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White, unfocusedTextColor = White,
                focusedBorderColor = Green, unfocusedBorderColor = Muted,
                cursorColor = Green, focusedLeadingIconColor = Green,
                unfocusedLeadingIconColor = Muted
            )
        )
        Spacer(Modifier.height(20.dp))
        Text("Sua música", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        PlaylistCard(onOpenPlaylist)
        if (query.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("Para pesquisar o catálogo completo, conecte uma integração de música autorizada.", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LibraryScreen(onOpenPlaylist: () -> Unit, onGoHome: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Sua Biblioteca", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Suas playlists em um só lugar.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        PlaylistCard(onOpenPlaylist)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onGoHome, colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)) {
            Icon(Icons.Default.Home, null)
            Spacer(Modifier.width(8.dp))
            Text("Voltar ao início")
        }
    }
}

 
private fun openSpotifyPlaylist(context: Context) {
    val spotifyIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("spotify:playlist:1ZizK6kXaVPt7bGZcyLMks")
    ).setPackage("com.spotify.music")
    try {
        context.startActivity(spotifyIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Instale o aplicativo Spotify para abrir sua playlist.",
            Toast.LENGTH_LONG
        ).show()
    }
}
