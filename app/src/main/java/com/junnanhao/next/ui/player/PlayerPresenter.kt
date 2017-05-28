package com.junnanhao.next.ui.player

import android.content.Context
import com.junnanhao.next.player.LoadMusicTask
import com.junnanhao.next.player.Player

/**
 * Created by Jonas on 2017/5/28.
 * player presenter
 */
class PlayerPresenter(var view: PlayerContract.View) : PlayerContract.Presenter {
    val player: Player = Player.instance
    override fun scan(context:Context) {
        val loadTask: LoadMusicTask= LoadMusicTask(context)
        loadTask.load()
    }

    override fun like() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun next() {

    }

    override fun pause() {
        player.pause()
    }

    override fun start() {
        player.play()
    }
}