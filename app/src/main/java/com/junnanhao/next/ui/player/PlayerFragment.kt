package com.junnanhao.next.ui.player

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.junnanhao.next.R
import com.junnanhao.next.model.Song
import com.junnanhao.next.player.Player

/**
 * Created by Jonas on 2017/5/27.
 * player fragment
 */
class PlayerFragment : Fragment(), PlayerContract.View {
    @BindView(R.id.tv_song_title) lateinit var title: TextView
    @BindView(R.id.tv_song_artist) lateinit var artist: TextView
    @BindView(R.id.img_art) lateinit var art: ImageView
    var presenter: PlayerContract.Presenter? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater!!.inflate(R.layout.frag_player, container, false)
        ButterKnife.bind(this, view)
        presenter = PlayerPresenter(this)
        title.setText("HELLO, I AM TITLE")
        artist.setText("ARTIEST")
        art.setColorFilter(Color.YELLOW)
        return view
    }

    @OnClick(R.id.container)
    fun play() {
        val player: Player = Player.instance
        val song: Song = Song(path = "http://mr3.doubanio.com/40a64e7cac98fff34c43c03f48d4a62c/0/fm/song/p2722754_128k.mp3", duration = 189000)
        player.play(song)
        System.out.println("toggle playing:" + song.path)
    }

    override fun showError() {
        Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
    }

    override fun showSongInfo(song: Song) {
        title.setText(song.title)
        artist.setText(song.artist)
    }

}