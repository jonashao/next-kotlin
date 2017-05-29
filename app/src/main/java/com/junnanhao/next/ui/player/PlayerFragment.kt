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
import butterknife.OnTouch
import com.junnanhao.next.R
import com.junnanhao.next.data.model.Song
import android.view.MotionEvent
import android.view.GestureDetector
import android.support.v4.view.GestureDetectorCompat


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

        private val SWIPE_MIN_DISTANCE = 120
        private val SWIPE_MAX_OFF_PATH = 250
        private val SWIPE_THRESHOLD_VELOCITY = 200
    }

    lateinit var mPresenter: PlayerContract.Presenter
    override fun setPresenter(presenter: PlayerContract.Presenter) {
        mPresenter = presenter
    }

    lateinit var mDetector: GestureDetectorCompat

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater!!.inflate(R.layout.frag_player, container, false)
        ButterKnife.bind(this, view)
        mDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                try {
                    if (Math.abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) {
                        return false
                    }
                    if (e1.x - e2.x > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        // left swipe
                        mPresenter.next()
                    } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        // right swipe

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
        mPresenter.next()

        return view
    }

    @OnClick(R.id.container)
    fun play() {
        mPresenter.playPause()
    }

    @OnTouch()
    fun onTouch(m: MotionEvent): Boolean {
        return mDetector.onTouchEvent(m)
    }

    override fun showError() {
        Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
    }

    override fun showSongInfo(song: Song) {
        title.text = song.title
        artist.text = song.artist
    }

}