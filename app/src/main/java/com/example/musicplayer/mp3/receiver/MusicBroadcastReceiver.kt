package com.example.musicplayer.mp3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.musicplayer.mp3.service.MusicService

class MusicBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val serviceIntent = Intent(context, MusicService::class.java)
        serviceIntent.action = action
        context.startService(serviceIntent)
    }
}
