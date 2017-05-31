package com.junnanhao.next.player

import android.media.AudioManager
import android.media.MediaPlayer
import com.junnanhao.next.data.model.PlayList
import com.junnanhao.next.data.model.Song
import java.io.IOException


@Suppress("UNUSED")
class Player private constructor() : IPlayer {
    private var player: MediaPlayer = MediaPlayer()
    private var isPause: Boolean = false
    private var playList: PlayList = PlayList()
    private var currentSong: Song? = null

    var playbackCallback: PlaybackCallback? = null
        set

    override fun play(): Boolean {
        if (isPause) {
            player.start()
            isPause = false
            //todo: notify playing status changed
            return true
        }
        if (playList.prepare()) {
            currentSong = playList.currentSong
            try {
                isPause = false
                player.reset()
                player.setAudioStreamType(AudioManager.STREAM_MUSIC)
                player.setDataSource(currentSong?.path)
                player.setOnPreparedListener {
                    playbackCallback?.onPrepared(currentSong)
                    player.start()
                }
                player.prepareAsync()
                player.setOnCompletionListener({ playbackCallback?.onComplete() })
                // todo: notify playing status changed
            } catch (e: IOException) {
                // todo: notify playing status changed
                e.printStackTrace()
                return false
            }
            return true
        }
        return false
    }


    override fun play(song: Song): Boolean {
        playList.reset(song = song)
        isPause = currentSong == song
        return play()
    }

    override fun pause(): Boolean {
        if (player.isPlaying) {
            isPause = true
            player.pause()
            //todo: notify playing status changed
            return true
        }
        return false
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    override fun getProgress(): Int {
        return player.currentPosition
    }

    override fun getPlayingSong(): Song? {
        return currentSong
    }

    override fun seekTo(position: Int): Boolean {
        if (playList.songs.isEmpty()) return false

        if (currentSong != null) {
            if (currentSong!!.duration <= position) {
                complete()
            } else {
                player.seekTo(position)
            }
            return true
        }
        return false
    }

    override fun complete(): Boolean {
        return false
    }

    override fun release() {
        player.release()
    }

    private object Holder {
        val INSTANCE = Player()
    }

    companion object {
        val instance: Player by lazy { Holder.INSTANCE }
    }
}

interface PlaybackCallback {
    fun onComplete()
    fun onPrepared(song: Song?)
}