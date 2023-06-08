package com.example.musicplayer.mp3.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.app.Notification.EXTRA_PROGRESS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.musicplayer.R
import com.example.musicplayer.mp3.fragments.Music
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData
import com.example.musicplayer.mp3.model.SongState
import com.example.musicplayer.mp3.model.SongsItem
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MusicService : Service() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackStateBuilder: PlaybackStateCompat.Builder
    private var mediaPlayer=MediaPlayer()
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManagerCompat
    private var isPaused = false
    private var isPlaying = false
    private var songName: String = ""
    private var artistName: String = ""
    private var image:String=""
    private var currentSongIndex: Int = -1
    private var songList: ArrayList<SongsItem>? = null
    private var totalSongsCount = songList?.size ?: 0
    private var songDuration: Int = 0
    private var _currentPosition: Int = 0
    private val musicBroadcastReceiver = MusicBroadcastReceiver()
    private var musicFragment: Music? = null
    private var musicProgressCallback: MusicProgressCallback? = null
    private val handler=Handler()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()

        val musicFragment = Music()
        val args = Bundle().apply {
            putInt(Music.ARG_SONG_DURATION, songDuration)
        }
        musicFragment.arguments = args

        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.isActive = true



        Log.d("onCreate","onCreate")
        notificationManager = NotificationManagerCompat.from(this)
        registerReceiver(musicBroadcastReceiver, IntentFilter().apply {
            Log.d("media",musicBroadcastReceiver.toString())
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_PROGRESS)
            addAction(ACTION_SEEK)
        })
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(
            null, // Use null listener as we don't need audio focus change notifications
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        Log.d("audio",audioManager.toString())
        initPlaybackStateBuilder()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("onStart",startId.toString())
        val action = intent?.getStringExtra(ACTION_KEY)
        songName= intent?.getStringExtra(NAME).toString()
        artistName= intent?.getStringExtra(SONG).toString()
        image=intent?.getStringExtra(IMAGE).toString()

        if (intent != null) {
            songList = intent.getParcelableArrayListExtra("songList")?: ArrayList()
        }
        currentSongIndex = intent?.getIntExtra(CURRENT_SONG_INDEX, -1) ?: -1
        totalSongsCount = songList?.size?:0

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
                        isPlaying = true
                        _song.postValue(SongState(isPlaying = true))
                        startMusicPlayback(songUrl)
                        Log.d("playyy",startId.toString())
                    }
                }

            }
            ACTION_PAUSE -> {
                isPlaying = false
                _song.postValue(SongState(isPlaying = false))
                mediaPlayer.seekTo(mediaPlayer.currentPosition)
                mediaPlayer.pause()
                isPaused = true
            }
            ACTION_NEXT -> {
                retrieveNextSong()
                if (currentSongIndex in 0..totalSongsCount) {
                    val nextSong = songList?.get(currentSongIndex)
                    _currentSong.value = nextSong
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
                    _currentSong.value = previousSong
                    Log.d("currP",currentSongIndex.toString())
                    previousSong?.url?.let { startMusicPlayback(it) }
                    if (previousSong != null) {
                        songName = previousSong.title.toString() // Update songName
                        artistName = previousSong.artist.toString()
                        Log.d("musicha",previousSong.url.toString())
                    }
                }
            }
            ACTION_SEEK -> {
                val seekPosition = intent.getLongExtra(EXTRA_SEEK_POSITION, 0)
                mediaPlayer.seekTo(seekPosition.toInt())
                _currentPosition = seekPosition.toInt()
                musicFragment?.updateSeekBarProgress(_currentPosition)
            }
            ACTION_PROGRESS -> {
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                updateSeekBarProgress(progress)
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

    fun setMusicSeek(seekTo: Int){
        mediaPlayer.seekTo(seekTo)
        _currentPosition = seekTo
        val intent = Intent(ACTION_SEEK)
        intent.putExtra(EXTRA_SEEK_POSITION, seekTo.toLong())
        Log.d("seekPosition",seekTo.toString())

    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(musicBroadcastReceiver)
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
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
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

    private fun updatePlaybackState(state: Int) {
        playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    private fun startMusicPlayback(songUrl: String) {
        try {
            if (songUrl.isNotEmpty()) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(songUrl)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener {
                    Log.d("mp",mediaPlayer.toString())
                    songDuration = mediaPlayer.duration
                    mediaPlayer.start()
                    isPlaying = true
                    _song.postValue(SongState(isPlaying = true))
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
                        return@setOnCompletionListener
                    }
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)

        val timer = Timer()
        val updateProgressTask = object : TimerTask() {
            override fun run() {

            }
        }
        timer.scheduleAtFixedRate(updateProgressTask, 0, 1000)

    }
    private fun updateSeekBarProgress(progress: Int) {
        if (isPlaying) {
            val currentPosition = mediaPlayer.currentPosition
            val progresss = ((currentPosition.toFloat()) / songDuration.toFloat()) * 100
            Log.d("pOnplay",progresss.toString())
            _currentPosition = progresss.toInt()
            mediaPlayer.seekTo(progresss.toInt())

            val progressIntent = Intent()
            progressIntent.action = Music.ACTION_KEY
            progressIntent.putExtra(Music.ARG_SONG_DURATION, currentPosition)
            sendBroadcast(progressIntent)

            val handler = Handler()
            handler.postDelayed({ updateSeekBarProgress(progresss.toInt()) }, 1000)
        }
        musicProgressCallback?.onProgressUpdate(progress)
        musicFragment?.updateSeekBarProgress(progress)
        Log.d("progress",progress.toString())
    }



    private fun initPlaybackStateBuilder() {
        playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
    }

    private fun retrieveNextSong() {
        currentSongIndex = (currentSongIndex + 1) % totalSongsCount
        Log.d("INext",currentSongIndex.toString())
    }
    private fun retrievePreviousSong() {
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
        private const val ACTION_PROGRESS="progress"
        const val ACTION_SEEK = "seek"
        const val EXTRA_SEEK_POSITION = "extra_seek"

        val _currentSong = MutableLiveData<SongsItem?>()
        val _song= MutableLiveData<SongState?>()

    }
    interface MusicProgressCallback {
        fun onProgressUpdate(progress: Int)
    }
}
