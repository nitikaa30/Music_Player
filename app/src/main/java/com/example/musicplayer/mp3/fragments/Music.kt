package com.example.musicplayer.mp3.fragments

import android.app.Notification.EXTRA_PROGRESS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentMusicBinding
import com.example.musicplayer.mp3.model.SongsItem
import com.example.musicplayer.mp3.service.MusicService
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

class Music : Fragment(), MusicService.MusicProgressCallback {
    private lateinit var binding: FragmentMusicBinding
    private lateinit var musicPlayerServiceIntent: Intent
    private var musicService: MusicService?=null
    private lateinit var mediaPlayer: MediaPlayer
    private var musicBroadcastReceiver = MusicBroadcastReceiver()
    private var isMusicPlaying = false
    private var currentSong: SongsItem?=null
    private var currentSongIndex: Int = -1
    private var songList: ArrayList<SongsItem>? = null
    private var songDuration: Int = 0
    private var isMusicSeekingAllowed = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        arguments?.let {
            songList = it.getParcelableArrayList("songList")
            currentSong = it.getParcelable(SONG_ITEM_KEY)
            songDuration = it.getInt(ARG_SONG_DURATION)

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentMusicBinding.inflate(inflater,container,false)
        return binding.root

    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mediaPlayer = MediaPlayer()

        musicBroadcastReceiver = MusicBroadcastReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_PROGRESS)
            addAction(ACTION_SEEK)
        }
        requireActivity().registerReceiver(musicBroadcastReceiver, intentFilter)


        MusicService._currentSong.observe(viewLifecycleOwner) {
            it?.let { item ->
                updateUI(item)
                MusicService._currentSong.value = null
            }
        }
        MusicService._song.observe(viewLifecycleOwner) { songState ->
            songState?.let { state ->
                // Update UI based on the song state
                if (state.isPlaying) {
                    // Update UI for play state
                    binding.mPause.visibility=View.VISIBLE
                    binding.mStart.visibility=View.GONE
                } else {
                    // Update UI for pause state
                    binding.mPause.visibility=View.GONE
                    binding.mStart.visibility=View.VISIBLE
                }
            }
        }

        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_music_to_musicList)
        }


        currentSong = arguments?.getParcelable("songItem")

        binding.mPause.visibility=View.GONE
        binding.mStart.setOnClickListener {
            sendBroadcast(ACTION_PLAY)
        }
        binding.mPause.setOnClickListener {
            sendBroadcast(ACTION_PAUSE)
        }
        binding.mSkipNext.setOnClickListener {
            sendBroadcast(ACTION_NEXT)
        }
        binding.mSkipPrevious.setOnClickListener {
            sendBroadcast(ACTION_PREVIOUS)
        }
        binding.mSlide.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update the progress of the seekbar
                if (fromUser) {
                    sendBroadcast(ACTION_PROGRESS)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isMusicSeekingAllowed = false
                mediaPlayer.seekTo(mediaPlayer.currentPosition)
                sendBroadcast(ACTION_SEEK)
                // Not used
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isMusicSeekingAllowed = true
                if (seekBar != null&& musicService != null) {
                    val seek=seekBar.progress * 1000
                    musicService?.setMusicSeek(seek)
                    sendBroadcast(ACTION_SEEK)
                }
                // Not used
            }
        })
        mediaPlayer.setOnPreparedListener { mp ->
            songDuration= mediaPlayer.duration.milliseconds.absoluteValue.toInt(DurationUnit.SECONDS)
            binding.mSlide.max = mediaPlayer.duration.milliseconds.absoluteValue.toInt(DurationUnit.SECONDS)
            binding.mEndTime.text=mediaPlayer.duration.milliseconds.absoluteValue.toInt(DurationUnit.SECONDS).toString()
            Log.d("songDuration",songDuration.toString())

            val handler = Handler(Looper.getMainLooper())
            val timer = Timer()
            val updateSeekBarTask = object : TimerTask() {
                override fun run() {
                    handler.post {
                        if (isMusicSeekingAllowed) {
                            binding.mSlide.progress = (((mediaPlayer.currentPosition.toFloat())/(mediaPlayer.duration.toFloat()))*100).toInt()
                            Log.d("slidepro",binding.mSlide.progress.toString())

                        }
                    }
                }
            }
            timer.scheduleAtFixedRate(updateSeekBarTask, 0, 1000)
            Log.d("time",timer.toString())
        }
        musicService = MusicService()

        mediaPlayer.setDataSource(currentSong?.url ?: "")
        mediaPlayer.prepareAsync()
        mediaPlayer.start()

    }
    private fun sendBroadcast(action: String) {
        val intent = Intent(action)
        requireActivity().sendBroadcast(intent)
    }
    fun updateSeekBarProgress(progress: Int) {
        binding.mSlide.progress = progress
        Log.d("pff",progress.toString())
    }
    inner class MusicBroadcastReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PLAY -> {
                    binding.mStart.visibility=View.GONE
                    binding.mPause.visibility=View.VISIBLE
                    if (!isMusicPlaying) {
                        currentSongIndex= currentSong?.id?.toInt() ?: 0
                        Log.d("cuIndex",currentSongIndex.toString())
                        currentSong?.url.let { it1 -> if (it1 != null) {
                            Log.d("iPlay",it1.toString())
                        }
                            if (it1 != null) {
                                currentSong?.title?.let { it2 ->
                                    currentSong?.artist?.let { it3 ->
                                        currentSong?.artwork?.let { it4 ->
                                            startmusicservice(ACTION_PLAY, it1,
                                                it2, it3,currentSongIndex.toString(), it4
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        isMusicPlaying = true
                        Log.d("Play", currentSong?.artist.toString())
                    }
                    currentSong?.let { it1 -> updateUI(it1) }
                }
                ACTION_PAUSE -> {
                    binding.mPause.visibility=View.GONE
                    binding.mStart.visibility=View.VISIBLE
                    if (isMusicPlaying) {
                        currentSong?.url?.let { it1 -> currentSong?.title?.let { it2 ->
                            currentSong?.artist?.let { it3 ->
                                currentSong?.id?.let { it4 ->
                                    currentSong?.artwork?.let { it5 ->
                                        startmusicservice(ACTION_PAUSE, it1,
                                            it2, it3, it4, it5
                                        )
                                    }
                                }
                            }
                        } }
                        isMusicPlaying = false
                        Log.d("Pause", currentSong?.artist.toString())
                    }
                }
                ACTION_NEXT ->{
                    if (isMusicPlaying) {
                        retrieveSongFromAPI(if (currentSongIndex == totalSongsCount) 0 else currentSongIndex + 1)
                        Log.d("id", currentSongIndex.toString())
                    }
                    currentSong?.let { updateUI(it) }
                }
                ACTION_PREVIOUS->{
                    if (isMusicPlaying){
                        retrieveSongFromAPI(if (currentSongIndex == 0) totalSongsCount - 1 else currentSongIndex - 1)
                        Log.d("Previous", currentSong?.artist.toString())
                    }
                    currentSong?.let { it1 -> updateUI(it1) }
                }
                ACTION_SEEK -> {
                    val seekPosition = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                    musicService?.setMusicSeek(seekPosition)
                    Log.d("seek",seekPosition.toString())
                }

                ACTION_PROGRESS->{
                    val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                    // Update the seekbar progress
                    binding.mSlide.progress = ((mediaPlayer.currentPosition)/(mediaPlayer.duration))*100
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(musicBroadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun retrieveSongFromAPI(index: Int) {
        currentSongIndex = index
        if (currentSongIndex >= 0 && currentSongIndex < (songList?.size ?: 0)) {
            currentSong = songList?.get(currentSongIndex)
            currentSong?.url?.let { url ->
                currentSong?.title?.let { title ->
                    currentSong?.artist?.let { artist ->
                        currentSong?.artwork?.let {
                            startmusicservice(
                                ACTION_PLAY, url, title, artist, currentSongIndex.toString(), it
                            )
                        }
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (isMusicPlaying) {
            binding.mStart.visibility = View.GONE
            binding.mPause.visibility = View.VISIBLE
        } else {
            binding.mPause.visibility = View.GONE
            binding.mStart.visibility = View.VISIBLE
        }
    }


    private fun updateUI(songsItem: SongsItem){
        Glide.with(this).load(songsItem.artwork).into(binding.mImage)
        binding.mArtist.text=songsItem.artist
        binding.mName.text=songsItem.title
        Log.d("Artist", binding.mArtist.toString())
    }
    fun getCurrentPosition(): Int {
        val position=mediaPlayer.currentPosition
        return position
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startmusicservice(action: String, songUrl:String,sName:String,sArtist:String,id:String,image:String) {
        musicPlayerServiceIntent = Intent(requireContext(), MusicService::class.java).apply {
            putExtra(ACTION_KEY, action)
            putExtra(SONG_ITEM_KEY, songUrl)
            putExtra(NAME,sArtist)
            putExtra(SONG,sName)
            putExtra(IMAGE,image)
            putExtra(CURRENT_SONG_INDEX, currentSongIndex)
            putParcelableArrayListExtra("songList", songList)
            Log.d("item_key", songUrl)
            Log.d("service",action)
        }
        requireActivity().startForegroundService(musicPlayerServiceIntent)
    }


    companion object {
        const val ACTION_KEY = "action"
        private const val ACTION_PLAY = "play"
        private const val ACTION_PAUSE = "pause"
        private const val ACTION_NEXT = "next"
        const val ACTION_PROGRESS = "progress"
        private const val ACTION_PREVIOUS="previous"
        private const val SONG_ITEM_KEY = "song_item"
        const val CURRENT_SONG_INDEX = "current_song_index"
        private const val NAME="name"
        private const val SONG="song"
        private const val IMAGE="image"
        const val POSITION="position"
        const val ACTION_SEEK = "seek"
        const val EXTRA_SEEK_POSITION = "extra_seek"
        private val totalSongsCount = 6
        const val ARG_SONG_DURATION = "duration"

    }

    override fun onProgressUpdate(progress: Int) {
        binding.mSlide.progress=progress
        updateSeekBarProgress(progress)
        Log.d("hey",progress.toString())
    }


}
