package com.junnanhao.next.ui.player

import android.content.Context
import com.junnanhao.next.common.BasePresenter
import com.junnanhao.next.common.BaseView
import com.junnanhao.next.data.model.Song

/**
 * Created by Jonas on 2017/5/27.
 * player contract
 */
@Suppress("UNUSED")
interface PlayerContract {
    interface View : BaseView<Presenter> {
        fun showError()
        fun showSongInfo(song: Song)
    }

    interface Presenter : BasePresenter {
        fun like()
        fun next()
        fun pause()
        fun play()
        fun scan()
    }
}