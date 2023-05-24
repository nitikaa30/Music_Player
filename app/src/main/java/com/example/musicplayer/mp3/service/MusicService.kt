package com.example.musicplayer.mp3.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.musicplayer.R
import com.example.musicplayer.mp3.fragments.Music
import com.example.musicplayer.mp3.model.songsItem
import java.io.IOException

class MusicService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManagerCompat
    private var isPaused = false
    private var songName: String = ""
    private var artistName: String = ""
    private var image:String=""
    private var currentSongIndex: Int = -1
    private var songList: ArrayList<songsItem>? = null
    private var totalSongsCount = songList?.size ?: 0


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("onCreate","onCreate")
        mediaPlayer = MediaPlayer()
        notificationManager = NotificationManagerCompat.from(this)
        registerReceiver(MusicBroadcastReceiver(), IntentFilter().apply {
            Log.d("media",MusicBroadcastReceiver().toString())
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
        })
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(
            null, // Use null listener as we don't need audio focus change notifications
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        Log.d("audio",audioManager.toString())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("onStart",startId.toString())
        val action = intent?.getStringExtra(ACTION_KEY)
        songName= intent?.getStringExtra(NAME).toString()
        artistName= intent?.getStringExtra(SONG).toString()
        image=intent?.getStringExtra(IMAGE).toString()
        songList = intent?.getParcelableArrayListExtra("songList")
        currentSongIndex = intent?.getIntExtra(CURRENT_SONG_INDEX, -1) ?: -1
        totalSongsCount = songList?.size ?: 0

        Log.d("sAction",action.toString())

        when (action) {
            ACTION_PLAY -> {

                val songUrl = intent.getStringExtra(SONG_ITEM_KEY)
                Log.d("url",songUrl.toString())
                if (songUrl != null) {
                    Log.d("sURL",songUrl)
                }
                Log.d("ActionPlay",action)
                if (isPaused) {
                    Log.d("aPlay",isPaused.toString())
                    mediaPlayer.start()
                    isPaused = false
                } else {
                    if (songUrl != null) {
                        startMusicPlayback(songUrl)
                        Log.d("playyy",startId.toString())
                    }
                }
            }
            ACTION_PAUSE -> {
                mediaPlayer.pause()
                isPaused = true
            }
            ACTION_NEXT -> {
                retrieveNextSong()
                if (currentSongIndex in 0..totalSongsCount) {
                    val nextSong = songList?.get(currentSongIndex)
                    Log.d("currI",currentSongIndex.toString())
                    nextSong?.url?.let { startMusicPlayback(it) }
                    if (nextSong != null) {
                        songName = nextSong.title.toString()// Update songName
                        artistName = nextSong.artist.toString()
                        Log.d("Nextmusic",nextSong.url.toString())
                    }
                }
            }
            ACTION_PREVIOUS -> {
                retrievePreviousSong()
                if (currentSongIndex in 0 until totalSongsCount) {
                    val previousSong = songList?.get(currentSongIndex)
                    Log.d("currP",currentSongIndex.toString())
                    previousSong?.url?.let { startMusicPlayback(it) }
                    if (previousSong != null) {
                        songName = previousSong.title.toString() // Update songName
                        artistName = previousSong.artist.toString()
                        Log.d("musicha",previousSong.url.toString())
                    }
                }
            }
        }
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf() // Stop the service when the app is removed from the recent apps list
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(MusicBroadcastReceiver())
        audioManager.abandonAudioFocus(null)
        mediaPlayer.release()
    }

    @SuppressLint("UnspecifiedImmutableFlag", "ResourceAsColor")
    private fun createNotification(): Notification {
        val channelId = "music_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelId)
        }

        val notificationIntent = Intent(this, Music::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val playIntent = Intent(ACTION_PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(ACTION_PAUSE)
        val pausePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(ACTION_NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val prevIntent = Intent(ACTION_PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIcon = if (isPaused) {
            R.drawable.baseline_play_circle_filled_24 // Show play icon if paused
        } else {
            R.drawable.pause // Show pause icon if playing
        }
        val playPauseText = if (isPaused) {
            "Play" // Show "Play" text if paused
        } else {
            "Pause" // Show "Pause" text if playing
        }



        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(songName)
            .setContentText(artistName)
            .setSmallIcon(R.drawable.music_note)
            .setContentIntent(pendingIntent).setColor(R.color.grey)
            .addAction(R.drawable.skip_previous,"Prev",prevPendingIntent)
            .addAction(playPauseIcon, playPauseText, if (isPaused) playPendingIntent else pausePendingIntent)
            .addAction(R.drawable.skip_next, "Next", nextPendingIntent)
            .build()

        Log.d("channel",channelId)

        return notification
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String) {
        val channel = NotificationChannel(
            channelId,
            "Music Player",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setSound(null, null)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun startMusicPlayback(songUrl: String) {
        try {
            if (songUrl.isNotEmpty()) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(songUrl)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener {
                    Log.d("mp",mediaPlayer.toString())
                    mediaPlayer.start()
                }
            }
            mediaPlayer.setOnCompletionListener {
                retrieveNextSong()
                if (currentSongIndex in 0 until totalSongsCount) {
                    val nextSong = songList?.get(currentSongIndex)
                    nextSong?.url?.let { startMusicPlayback(it) }
                    if (nextSong != null) {
                        songName = nextSong.title.toString()
                        Log.d("retriev",songName)
                        artistName = nextSong.artist.toString()
                    }
                    val notification = createNotification()
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return@setOnCompletionListener
                    }
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private fun retrieveNextSong() {
        currentSongIndex = (currentSongIndex + 1) % totalSongsCount
        Log.d("INext",currentSongIndex.toString())
    }
    private fun retrievePreviousSong() {
//        currentSongIndex--
        if (currentSongIndex == -1) currentSongIndex=totalSongsCount  else currentSongIndex -= 1
        Log.d("total",totalSongsCount.toString())
        Log.d("IPrev",currentSongIndex.toString())
    }

    inner class MusicBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("music", "onReceive")
            val action = intent?.action

            val serviceIntent = Intent(context, MusicService::class.java).apply {
                putExtra(ACTION_KEY, action)
                putExtra(NAME, songName)
                putExtra(SONG, artistName)
                putExtra(IMAGE, image)
                putExtra(CURRENT_SONG_INDEX,currentSongIndex)
                putParcelableArrayListExtra("songList", songList)
            }
            startService(serviceIntent)
        }

    }


    companion object {
        private const val ACTION_KEY = "action"
        private const val SONG_ITEM_KEY = "song_item"
        private const val ACTION_PLAY = "play"
        private const val ACTION_PAUSE = "pause"
        private const val ACTION_PREVIOUS = "previous"
        private const val ACTION_NEXT = "next"
        private const val NOTIFICATION_ID = 1
        private const val CURRENT_SONG_INDEX = "current_song_index"
        private const val NAME="name"
        private const val SONG="song"
        private const val IMAGE="image"
        private val totalSongsCount = 6
    }
}