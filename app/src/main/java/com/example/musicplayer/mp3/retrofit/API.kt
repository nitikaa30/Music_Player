package com.example.musicplayer.mp3.retrofit

import com.example.musicplayer.mp3.model.songs
import com.example.musicplayer.mp3.model.songsItem
import retrofit2.Call
import retrofit2.http.GET

interface API {
    @GET("f0d68f29-3dbc-4b47-8c6c-2f0c587edd87")
    fun getSongs(): Call<songs>

    @GET("f0d68f29-3dbc-4b47-8c6c-2f0c587edd87")
    fun getSongUrl(id:String): Call<songsItem>
}