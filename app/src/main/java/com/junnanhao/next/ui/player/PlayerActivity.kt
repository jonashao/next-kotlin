package com.junnanhao.next.ui.player

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.view.View
import com.junnanhao.next.R
import com.junnanhao.next.common.App
import kotlinx.android.synthetic.main.activity_fullscreen.*
import javax.inject.Inject

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class PlayerActivity : AppCompatActivity() {
    private val mHideHandler = Handler()
    private var mediaSession: MediaSessionCompat? = null
    private val playbackStateBuilder = PlaybackStateCompat.Builder()

    @SuppressLint("InlinedApi")
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private val mHideRunnable = Runnable { hide() }

    @Inject lateinit var mPresenter: PlayerPresenter
        set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val playerFragment = PlayerFragment.instance()
        supportFragmentManager.beginTransaction()
                .add(R.id.fullscreen_content, playerFragment, "player-fragment")
                .commit()

        mediaSession = MediaSessionCompat(this, "NEXT")
        mediaSession?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        mediaSession?.setPlaybackState(playbackStateBuilder.build())
        mediaSession?.setCallback(mediaCallback)
        mediaSession?.isActive = true

        DaggerPlayerComponent.builder()
                .songsRepositoryComponent((application as App).songsRepositoryComponent)
                .playerPresenterModule(PlayerPresenterModule(playerFragment))
                .build()
                .inject(this)

    }

    private val mediaCallback: MediaSessionCompat.Callback = object :
            MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()
            mPresenter.play()
        }

        override fun onPause() {
            super.onPause()
            mPresenter.pause()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            mPresenter.next()
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent == null || keyEvent.action != KeyEvent.ACTION_DOWN) {
                return false
            }
            val keyCode = keyEvent.keyCode
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    onPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    onPlay()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    onSkipToNext()
                    return true
                }
                else -> {
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }


    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }


    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
