package com.example.musicplayer.mp3.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentMusicListBinding
import com.example.musicplayer.mp3.adapter.Adapter
import com.example.musicplayer.mp3.model.songs
import com.example.musicplayer.mp3.model.songsItem
import com.example.musicplayer.mp3.retrofit.Retrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MusicList : Fragment() {
    private lateinit var binding: FragmentMusicListBinding
    private lateinit var adapter:Adapter
    private lateinit var linearLayoutManager: LinearLayoutManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentMusicListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findNavController()
        Log.d("1","first")
        linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.stackFromEnd = true
        binding.recyclerView.layoutManager=linearLayoutManager
        adapter=Adapter(ArrayList())
        binding.recyclerView.adapter=adapter
        Log.d("adap",adapter.toString())
        Retrofit.apiInterface.getSongs().enqueue(object: Callback<songs> {
            override fun onResponse(call: Call<songs>, response: Response<songs>) {
                val list:ArrayList<songsItem>?=response.body()
                if (list != null) {
                    adapter.setItems(list) // Update the adapter with the new list of songs
                }
            }

            override fun onFailure(call: Call<songs>, t: Throwable) {
                Toast.makeText(context,"nooo", Toast.LENGTH_LONG).show()
            }

        })
        adapter.setOnItemClickListener(object :Adapter.OnItemClickListener{
            override fun onItemClick(song: songsItem) {
                val bundle = bundleOf("songItem" to song)
                findNavController().navigate(R.id.action_musicList_to_music,bundle)

            }
        })


    }

}