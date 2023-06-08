package com.example.musicplayer.mp3.model

import android.os.Parcel
import android.os.Parcelable

data class SongsItem(
    val artist: String?,
    val artwork: String?,
    var id: String?,
    val title: String?,
    val url: String?
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(artist)
        parcel.writeString(artwork)
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SongsItem> {
        override fun createFromParcel(parcel: Parcel): SongsItem {
            return SongsItem(parcel)
        }

        override fun newArray(size: Int): Array<SongsItem?> {
            return arrayOfNulls(size)
        }
    }
}