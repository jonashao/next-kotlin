package com.junnanhao.next

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_fullscreen.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val playerFragment = PlayerFragment.instance()
        supportFragmentManager.beginTransaction()
                .add(R.id.fullscreen_content, playerFragment, "player-fragment")
                .commit()

    }

    override fun onPostResume() {
        super.onPostResume()
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }


    private val mHideHandler = Handler()

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
        private val SAVED_MEDIA_ID = "com.example.android.uamp.MEDIA_ID"
        private val FRAGMENT_TAG = "uamp_list_container"

        val EXTRA_START_FULLSCREEN = "com.example.android.uamp.EXTRA_START_FULLSCREEN"
        /**
         * Optionally used with [.EXTRA_START_FULLSCREEN] to carry a MediaDescription to
         * the [FullScreenPlayerActivity], speeding up the screen rendering
         * while the [android.support.v4.media.session.MediaControllerCompat] is connecting.
         */
        val EXTRA_CURRENT_MEDIA_DESCRIPTION = "com.example.android.uamp.CURRENT_MEDIA_DESCRIPTION"
    }

}
