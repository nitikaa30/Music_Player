package com.example.musicplayer.mp3.retrofit

import com.example.musicplayer.mp3.model.Songs
import com.example.musicplayer.mp3.model.SongsItem
import retrofit2.Call
import retrofit2.http.GET

interface API {
    @GET("f0d68f29-3dbc-4b47-8c6c-2f0c587edd87")
    fun getSongs(): Call<Songs>

    @GET("f0d68f29-3dbc-4b47-8c6c-2f0c587edd87")
    fun getSongUrl(): Call<SongsItem>
}