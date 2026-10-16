package com.erom.music

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var status: TextView
    private lateinit var title: TextView
    private lateinit var play: Button
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        val items = uris.map { uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            MediaItem.Builder().setUri(uri).setMediaId(uri.toString()).build()
        }
        controller?.setMediaItems(items); controller?.prepare(); controller?.play()
        title.text = displayName(uris.first())
        status.text = "${uris.size} música(s) na fila"
        play.text = "Ⅱ Pausar"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16,17,15)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 20); setBackgroundColor(0xFF10110F.toInt()) }
        fun text(s: String, size: Float, color: Int = 0xFFF5F5F0.toInt()) = TextView(this).apply { text=s; textSize=size; setTextColor(color) }
        root.addView(text("EROM MUSIC", 14f, 0xFFB6F36A.toInt()))
        root.addView(text("Sua música. Seu ritmo.", 28f).apply { setPadding(0, 8, 0, 18) })
        val recentLabel = text("TOCADAS RECENTEMENTE", 12f, 0xFFB6F36A.toInt()); recentLabel.setPadding(0,12,0,10); root.addView(recentLabel)
        val cards = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Suas músicas", "Adicionar músicas").forEachIndexed { i, label ->
            val card = TextView(this).apply { text = "♫\n\n$label"; textSize=15f; gravity=Gravity.CENTER; setTextColor(0xFFF5F5F0.toInt()); setBackgroundColor(if(i==0) 0xFF30352B.toInt() else 0xFF252622.toInt()); setPadding(10,10,10,10) }
            val lp = LinearLayout.LayoutParams(0, 145, 1f); if(i==0) lp.marginEnd=10
            cards.addView(card, lp); card.setOnClickListener { picker.launch(arrayOf("audio/*")) }
        }
        root.addView(cards)
        val spacer = Space(this); root.addView(spacer, LinearLayout.LayoutParams(1,0,1f))
        val art = TextView(this).apply { text="♫"; textSize=72f; gravity=Gravity.CENTER; setTextColor(0xFFB6F36A.toInt()); setBackgroundColor(0xFF252622.toInt()) }
        root.addView(art, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 210))
        title = text("Escolha uma música", 21f).apply { setPadding(0,16,0,2) }; root.addView(title)
        status = text("Reprodução local", 14f, 0xFFAAAAA0.toInt()); root.addView(status)
        val controls = LinearLayout(this).apply { gravity=Gravity.CENTER; orientation=LinearLayout.HORIZONTAL }
        val choose = Button(this).apply { text="＋ Músicas"; setOnClickListener { picker.launch(arrayOf("audio/*")) } }
        val prev = Button(this).apply { text="|◀"; setOnClickListener { controller?.seekToPreviousMediaItem() } }
        play = Button(this).apply { text="▶ Tocar"; setOnClickListener { val c=controller ?: return@setOnClickListener; if(c.isPlaying){c.pause();text="▶ Tocar"}else{c.play();text="Ⅱ Pausar"} } }
        val next = Button(this).apply { text="▶|"; setOnClickListener { controller?.seekToNextMediaItem() } }
        listOf(choose,prev,play,next).forEach { controls.addView(it) }; root.addView(controls)
        setContentView(root)
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({ try { controller = controllerFuture?.get() } catch (_: Exception) { status.text="Não foi possível iniciar o serviço de áudio" } }, ContextCompat.getMainExecutor(this))
    }
    private fun displayName(uri: Uri): String = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Música selecionada"
    override fun onDestroy() { controllerFuture?.let { MediaController.releaseFuture(it) }; controller = null; super.onDestroy() }
}