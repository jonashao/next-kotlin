package com.junnanhao.next.data.model

import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Jonas on 2017/5/26.
 * play list
 */
@Suppress("UNUSED")
open class PlayList(var id: Long = 0,
                    var name: String = "",
                    var songs:MutableList<Song> = ArrayList(),
                    var createdAt: Date = Date(),
                    var updatedAt: Date = Date(),
                    var playingIndex: Int = -1
) {
    var numOfSongs: Int = 0
        get() = songs.size

    var currentSong: Song? = null
        get() = songs.getOrNull(playingIndex)

    fun prepare(): Boolean {
        if (songs.isEmpty()) return false
        if (playingIndex == NO_POSITION) playingIndex = 0
        return true
    }

    fun reset(songs: ArrayList<Song> = ArrayList(0), song: Song) {
        this.songs.clear()
        this.songs.addAll(songs)
        this.songs.add(song)
        playingIndex = NO_POSITION
    }

    companion object {
        val NO_POSITION: Int = -1
    }
}