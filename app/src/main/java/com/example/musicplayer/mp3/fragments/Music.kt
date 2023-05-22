package com.example.musicplayer.mp3.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.musicplayer.databinding.FragmentMusicBinding
import com.example.musicplayer.mp3.model.songs
import com.example.musicplayer.mp3.model.songsItem
import com.example.musicplayer.mp3.retrofit.Retrofit
import com.example.musicplayer.mp3.service.MusicService
import androidx.navigation.fragment.navArgs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random


class Music : Fragment()  {
    private lateinit var binding: FragmentMusicBinding
    private lateinit var musicPlayerServiceIntent: Intent
    private var isMusicPlaying = false
    private var currentSong: songsItem?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
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


        currentSong = arguments?.getParcelable("songItem")

        binding.mPause.visibility=View.GONE
        binding.mStart.setOnClickListener {
            binding.mStart.visibility=View.GONE
            binding.mPause.visibility=View.VISIBLE
            if (!isMusicPlaying) {
                currentSong.let { it1 -> if (it1 != null) {
                    Log.d("iPlay",it1.url.toString())
                }
                    if (it1 != null) {
                        startmusicservice(ACTION_PLAY, it1)
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
                currentSong?.let { it1 -> startmusicservice(ACTION_PAUSE, it1) }
                isMusicPlaying = false
                Log.d("Pause", currentSong?.artist.toString())
            }
        }
        binding.mSkipNext.setOnClickListener {
            if (isMusicPlaying) {
                currentSong?.let { it1 -> startmusicservice(ACTION_NEXT, it1) }
                Log.d("Next", currentSong?.artist.toString())
            }
            currentSong?.let { it1 -> updateUI(it1) }
        }
        binding.mSkipPrevious.setOnClickListener {
            if (isMusicPlaying){
                currentSong?.let { it1 -> startmusicservice(ACTION_PREVIOUS, it1) }
                Log.d("Previous", currentSong?.artist.toString())
            }
            currentSong?.let { it1 -> updateUI(it1) }
        }
    }
    private fun updateUI(songsItem: songsItem){
        Glide.with(this).load(songsItem.artwork).into(binding.mImage)
        binding.mArtist.text=songsItem.artist
        binding.mName.text=songsItem.title
        Log.d("Artist", binding.mArtist.toString())
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startmusicservice(action: String, songsItem:songsItem) {
        musicPlayerServiceIntent = Intent(requireContext(), MusicService::class.java).apply {
            putExtra(ACTION_KEY, action)
            putExtra(SONG_ITEM_KEY, songsItem)
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
        private const val SONG_ITEM_KEY = "songItem"

        fun newInstance(songItem: songsItem): Music {
            val fragment = Music()
            val args = Bundle()
            args.putParcelable(SONG_ITEM_KEY, songItem)
            fragment.arguments = args
            return fragment
        }
    }



}
