package com.junnanhao.next.ui.player

import com.junnanhao.next.common.BasePresenter
import com.junnanhao.next.common.BaseView
import com.junnanhao.next.data.model.Song

/**
 * Created by Jonas on 2017/5/27.
 * player contract
 */
interface PlayerContract {
    interface View : BaseView<Presenter> {
        fun showError(error:String)
        fun showSongInfo(song: Song?)
        fun showPermissionNotGranted()
        fun showLoading()
        fun showMessage(message: String)
        fun showMessage(resId: Int)
    }

    interface Presenter : BasePresenter {
        fun like()
        fun next()
        fun pause()
        fun play()
        fun duck()
        fun playPause()
        fun scan()
    }
}