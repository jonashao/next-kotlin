package com.junnanhao.next.ui.player

import com.junnanhao.next.data.SongsRepository
import com.junnanhao.next.data.model.Song
import com.junnanhao.next.player.Player
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import java.util.*
import javax.inject.Inject


/**
 * Created by Jonas on 2017/5/28.
 * player presenter
 */
class PlayerPresenter @Inject constructor(
        val mView: PlayerContract.View,
        val mSongsRepository: SongsRepository)
    : PlayerContract.Presenter {

    val player: Player = Player.instance
    val realm: Realm = Realm.getDefaultInstance()

    override fun scan() {
        mSongsRepository.scanMusic()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ next() })
    }

    @Inject
    fun setupListener() {
        mView.setPresenter(this)
    }

    override fun play() {
        player.play()
    }

    override fun playPause() {
        if (player.isPlaying()) {
            pause()
        } else {
            play()
        }
    }


    override fun next() {
        val realm: Realm = Realm.getDefaultInstance()
        val list = realm.where(Song::class.java).findAll()
        if (list.size > 0) {
            val random = Random()
            val song = list.get(random.nextInt(list.size))
            player.play(song)
            mView.showSongInfo(song)
        } else {
            scan()
        }
    }

    override fun like() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun pause() {
        player.pause()
    }

    override fun start() {
        player.play()
    }


}