package com.junnanhao.next.ui.player

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.junnanhao.next.R

/**
 * Created by Jonas on 2017/5/27.
 * player fragment
 */
class PlayerFragment : Fragment() {
    @BindView(R.id.tv_song_title) lateinit var title: TextView

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater!!.inflate(R.layout.frag_player, container, false)
        ButterKnife.bind(this, view)
        title.setText("HELLO, I AM TITLE")
        return view
    }
}