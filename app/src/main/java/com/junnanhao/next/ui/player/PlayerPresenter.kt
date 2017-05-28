package com.junnanhao.next.ui.player

import com.junnanhao.next.player.Player

/**
 * Created by Jonas on 2017/5/28.
 * player presenter
 */
class PlayerPresenter(var view: PlayerContract.View) : PlayerContract.Presenter {
    val player: Player = Player.instance
    override fun scan() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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