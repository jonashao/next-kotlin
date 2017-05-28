package com.junnanhao.next.ui.player

import android.content.Context
import com.junnanhao.next.model.Song

/**
 * Created by Jonas on 2017/5/27.
 * player contract
 */
@Suppress("UNUSED")
interface PlayerContract {
    interface View {
        fun showError()
        fun showSongInfo(song: Song)
    }

    interface Presenter {
        fun like()
        fun next()
        fun pause()
        fun start()
        fun scan(context: Context)
    }
}