package br.com.erommusic.nativeapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class PlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var currentPath: String? = null
    private val channelId = "erom_music_playback"
    private val notificationId = 4102

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Reprodução de música", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val path = intent.getStringExtra(EXTRA_PATH)
                if (!path.isNullOrBlank() && path != currentPath) play(path)
                else if (player?.isPlaying == false) runCatching { player?.start() }
            }
            ACTION_PAUSE -> runCatching { player?.pause() }
            ACTION_RESUME -> runCatching { player?.start() }
            ACTION_STOP -> {
                player?.release()
                player = null
                currentPath = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (player != null) {
            startForeground(notificationId, makeNotification())
        }
        return START_STICKY
    }

    private fun play(path: String) {
        val file = File(path)
        if (!file.isFile) return
        runCatching { player?.release() }
        currentPath = path
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            prepare()
            start()
        }
    }

    private fun makeNotification(): Notification {
        val title = File(currentPath.orEmpty()).nameWithoutExtension.ifBlank { "Erom Music" }
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val pauseAction = if (player?.isPlaying == true) ACTION_PAUSE else ACTION_RESUME
        val pauseLabel = if (player?.isPlaying == true) "Pausar" else "Continuar"
        val control = Intent(this, PlaybackService::class.java).setAction(pauseAction)
        val controlPending = PendingIntent.getService(this, 1, control,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("Erom Music • reprodução local")
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(player?.isPlaying == true)
            .addAction(android.R.drawable.ic_media_pause, pauseLabel, controlPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { player?.release() }
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "br.com.erommusic.nativeapp.PLAY"
        const val ACTION_PAUSE = "br.com.erommusic.nativeapp.PAUSE"
        const val ACTION_RESUME = "br.com.erommusic.nativeapp.RESUME"
        const val ACTION_STOP = "br.com.erommusic.nativeapp.STOP"
        const val EXTRA_PATH = "song_path"
    }
}
