package com.example.musicplayer.mp3.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.musicplayer.R
import com.example.musicplayer.mp3.fragments.Music
import com.example.musicplayer.mp3.model.songsItem
import com.example.musicplayer.mp3.retrofit.Retrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MusicService : Service() {
    private lateinit var context: Context
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManagerCompat
    private var currentSongIndex = 0
    private var isPaused = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        notificationManager = NotificationManagerCompat.from(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(ACTION_KEY)
        val songItem = intent?.getParcelableExtra<songsItem>(SONG_ITEM_KEY)

        when (action) {
            ACTION_PLAY -> {
                if (isPaused) {
                    mediaPlayer.start()
                    isPaused = false
                } else {
                    if (songItem != null) {
                        currentSongIndex = (songItem.id?.toInt() ?: startMusicPlayback(songItem)) as Int
                    }
                }
            }
            ACTION_PAUSE -> {
                mediaPlayer.pause()
                isPaused = true
            }
            ACTION_NEXT -> {
                currentSongIndex = (currentSongIndex + 1) % totalSongsCount
                if (songItem != null) {
                    startMusicPlayback(songItem)
                }
            }
        }

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotification(): Notification {
        val channelId = "music_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelId)
        }

        val notificationIntent = Intent(this, Music::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        // Create the pending intents for the playback actions
        val playIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PLAY
            putExtra(SONG_ITEM_KEY, currentSongIndex)
        }
        val playPendingIntent = PendingIntent.getService(
            this,
            0,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            0,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this,
            0,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Music")
            .setContentText("Playing Music")
            .setSmallIcon(R.drawable.music_note)
            .setContentIntent(pendingIntent)
            .addAction(NotificationCompat.Action(R.drawable.skip_previous, "Previous", null)) // Add previous button if needed
            .addAction(
                NotificationCompat.Action(
                    R.drawable.baseline_play_circle_filled_24,
                    "Play",
                    playPendingIntent
                )
            )
            .addAction(NotificationCompat.Action(R.drawable.pause, "Pause", pausePendingIntent))
            .addAction(NotificationCompat.Action(R.drawable.skip_next, "Next", nextPendingIntent))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return notification
    }

    private fun startMusicPlayback(songItem: songsItem) {
        songItem.id?.let {
            Retrofit.apiInterface.getSongUrl(it).enqueue(object : Callback<songsItem> {
                override fun onResponse(call: Call<songsItem>, response: Response<songsItem>) {
                    if (response.isSuccessful) {
                        val songUrl = response.body()?.url
                        if (songUrl != null) {
                            mediaPlayer.reset()
                            mediaPlayer.setDataSource(songUrl)
                            mediaPlayer.prepareAsync()
                            mediaPlayer.setOnPreparedListener {
                                mediaPlayer.start()
                            }
                        } else {
                            Log.d("null", "no url")
                            // Handle case when song URL is null
                        }
                    } else {
                        Log.d("err", response.errorBody().toString())
                        // Handle API call failure
                    }
                }

                override fun onFailure(call: Call<songsItem>, t: Throwable) {
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                    // Handle API call failure
                }
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String) {
        val channelName = "Music Channel"
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val ACTION_KEY = "action"
        const val ACTION_PLAY = "play"
        const val ACTION_PAUSE = "pause"
        const val ACTION_NEXT = "next"
        const val SONG_ITEM_KEY = "songItem"
        private val totalSongsCount = 6 // Total number of songs
    }
}

//class MusicService: Service() {
//    private lateinit var mediaPlayer: MediaPlayer
//    private lateinit var notificationManager: NotificationManagerCompat
//    private var currentSongIndex = 0
//    private var isPaused = false
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        mediaPlayer= MediaPlayer()
//        notificationManager= NotificationManagerCompat.from(this)
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val action = intent?.getStringExtra(ACTION_KEY)
//        when (action) {
//            ACTION_PLAY -> {
//                if (isPaused) {
//                    mediaPlayer.start()
//                    isPaused = false
//                } else {
//                    currentSongIndex = intent.getIntExtra(SONG_INDEX_KEY, 0)
//                    startMusicPlayback()
//                }
//            }
//            ACTION_PAUSE -> {
//                mediaPlayer.pause()
//                isPaused = true
//            }
//            ACTION_NEXT -> {
//                currentSongIndex = (currentSongIndex + 1) % totalSongsCount
//                startMusicPlayback()
//            }
//        }
//
//        val notification = createNotification()
//        startForeground(NOTIFICATION_ID, notification)
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        mediaPlayer.release()
//    }
//
//    @SuppressLint("UnspecifiedImmutableFlag")
//    private fun createNotification(): Notification {
//        val channelId = "music_channel"
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            createNotificationChannel(channelId)
//        }
//
//        val notificationIntent = Intent(this, Music::class.java)
//        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
//
//        // Create the pending intents for the playback actions
//        val playIntent = Intent(this, MusicService::class.java).apply {
//            action = ACTION_PLAY
//            putExtra(SONG_INDEX_KEY, Random.nextInt(0,2))
//        }
//        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//
//        val pauseIntent = Intent(this, MusicService::class.java).apply {
//            action = ACTION_PAUSE
//        }
//        val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//
//
//        val nextIntent = Intent(this, MusicService::class.java).apply {
//            action = ACTION_NEXT
//        }
//        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//
//        val notification = NotificationCompat.Builder(this, channelId)
//            .setContentTitle("Music")
//            .setContentText("Playing Music")
//            .setSmallIcon(R.drawable.music_note)
//            .setContentIntent(pendingIntent)
//            .addAction(NotificationCompat.Action(R.drawable.skip_previous, "Previous", null)) // Add previous button if needed
//            .addAction(NotificationCompat.Action(R.drawable.baseline_play_circle_filled_24,"Play", playPendingIntent))
//            .addAction(NotificationCompat.Action(R.drawable.pause,"Pause",pausePendingIntent))
//            .addAction(NotificationCompat.Action(R.drawable.skip_next, "Next", nextPendingIntent))
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//
//        return notification
//    }
//    private fun startMusicPlayback() {
//        Retrofit.apiInterface.getSongs().enqueue(callbac)
//    }
//    private fun getSongResourceId(songIndex: Int): Int {
//        return when (songIndex) {
////            0 -> R.raw.song1 // Replace with the actual resource ID of your first song
////            1 -> R.raw.song2 // Replace with the actual resource ID of your second song
//            else -> throw IllegalArgumentException("Invalid song index: $songIndex")
//        }
//    }
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun createNotificationChannel(channelId: String) {
//        val channelName = "Music Channel"
//        val channel = NotificationChannel(
//            channelId,
//            channelName,
//            NotificationManager.IMPORTANCE_LOW
//        )
//        val notificationManager = getSystemService(NotificationManager::class.java)
//        notificationManager.createNotificationChannel(channel)
//    }
//    companion object {
//        private const val NOTIFICATION_ID = 1
//        private const val ACTION_KEY = "action"
//        private const val ACTION_PLAY = "play"  // Update the type to String
//        private const val ACTION_PAUSE = "pause"
//        private const val ACTION_NEXT = "next"
//        private const val SONG_INDEX_KEY = "songIndex"
//        private val totalSongsCount = 2 // Total number of songs
//    }
//
//}