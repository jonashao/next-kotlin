package com.junnanhao.next.ui.player

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
import com.junnanhao.next.data.model.Song

/**
 * Created by Jonas on 2017/5/27.
 * player fragment
 */
class PlayerFragment : Fragment(), PlayerContract.View {

    @BindView(R.id.tv_song_title) lateinit var title: TextView
    @BindView(R.id.tv_song_artist) lateinit var artist: TextView
    @BindView(R.id.img_art) lateinit var art: ImageView

    companion object {
        fun instance(): PlayerFragment {
            return PlayerFragment()
        }
    }


    lateinit var mPresenter: PlayerContract.Presenter


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater!!.inflate(R.layout.frag_player, container, false)
        ButterKnife.bind(this, view)
        return view
    }


    override fun setPresenter(presenter: PlayerContract.Presenter) {
        mPresenter = presenter
    }


    @OnClick(R.id.container)
    fun play() {
        mPresenter.start()
    }

    override fun showError() {
        Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
    }

    override fun showSongInfo(song: Song) {
        title.text = song.title
        artist.text = song.artist
    }

}