package com.example.musicplayer.mp3.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentMusicBinding
import com.example.musicplayer.mp3.model.songsItem
import com.example.musicplayer.mp3.service.MusicService

class Music : Fragment()  {
    private lateinit var binding: FragmentMusicBinding
    private lateinit var musicPlayerServiceIntent: Intent
    private var isMusicPlaying = false
    private var currentSong: songsItem?=null
    private var currentSongIndex: Int = -1
    private var songList: ArrayList<songsItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            songList = it.getParcelableArrayList("songList")
            currentSong = it.getParcelable(SONG_ITEM_KEY)
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

        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_music_to_musicList)
        }


        currentSong = arguments?.getParcelable("songItem")

        binding.mPause.visibility=View.GONE
        binding.mStart.setOnClickListener {
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
        binding.mPause.setOnClickListener {
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
        binding.mSkipNext.setOnClickListener {
            if (isMusicPlaying) {
                retrieveSongFromAPI(if (currentSongIndex == totalSongsCount) 0 else currentSongIndex + 1)
//                currentSongIndex = (currentSongIndex + 1) % totalSongsCount
//                currentSong?.id = currentSongIndex.toString()
                Log.d("id", currentSongIndex.toString())
//                retrieveSongFromAPI(currentSongIndex)
            }
            currentSong?.let { updateUI(it) }
        }
        binding.mSkipPrevious.setOnClickListener {
            if (isMusicPlaying){
                retrieveSongFromAPI(if (currentSongIndex == 0) totalSongsCount - 1 else currentSongIndex - 1)
                Log.d("Previous", currentSong?.artist.toString())
            }
            currentSong?.let { it1 -> updateUI(it1) }
        }
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



    private fun updateUI(songsItem: songsItem){
        Glide.with(this).load(songsItem.artwork).into(binding.mImage)
        binding.mArtist.text=songsItem.artist
        binding.mName.text=songsItem.title
        Log.d("Artist", binding.mArtist.toString())
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
        private const val ACTION_KEY = "action"
        private const val ACTION_PLAY = "play"
        private const val ACTION_PAUSE = "pause"
        private const val ACTION_NEXT = "next"
        private const val ACTION_PREVIOUS="previous"
        private const val SONG_ITEM_KEY = "song_item"
        private const val CURRENT_SONG_INDEX = "current_song_index"
        private const val NAME="name"
        private const val SONG="song"
        private const val IMAGE="image"
        private val totalSongsCount = 6

    }



}
