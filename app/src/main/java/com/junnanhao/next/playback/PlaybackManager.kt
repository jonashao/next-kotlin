package com.junnanhao.next.playback

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import com.github.ajalt.timberkt.wtf
import com.junnanhao.next.R
import com.junnanhao.next.utils.MediaIDHelper
import java.util.*


/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
class PlaybackManager(playback: Playback, val chainManager: ChainManager,
                      val callback: PlaybackServiceCallback) : Playback.Callback {

    var playback: Playback? = null
        private set


    private val mMediaSessionCallback: MediaSessionCallback

    init {
        mMediaSessionCallback = MediaSessionCallback()
        this.playback = playback
        this.playback?.setCallback(this)
    }

    val mediaSessionCallback: MediaSessionCompat.Callback
        get() = mMediaSessionCallback

    /**
     * Handle a request to play music
     */
    fun handlePlayRequest() {
        wtf { "handlePlayRequest: mState= ${playback?.state}" }
        if (playback?.state == PlaybackStateCompat.STATE_PLAYING) return
        val mediaId: String = chainManager.current()?.description?.mediaId ?: return

        callback.onPlaybackStart()
        playback?.play(mediaId)
        chainManager.updateMetaData()
    }

    /**
     * Handle a request to pause music
     */
    fun handlePauseRequest() {
        if (playback?.isPlaying() ?: return) {
            playback?.pause()
            callback.onPlaybackStop()
        }
    }

    /**
     * Handle a request to stop music
     *
     * @param withError Error message in case the stop has an unexpected cause. The error
     * message will be set in the PlaybackState and will be visible to
     * MediaController clients.
     */
    fun handleStopRequest(withError: String?) {
        wtf { "handleStopRequest: mState=${playback?.state} error=$withError" }
        playback?.stop(true)
        callback.onPlaybackStop()
        updatePlaybackState(withError)
    }


    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    fun updatePlaybackState(error: String?) {
        wtf { "updatePlaybackState, playback state= ${playback?.state}" }
        if (playback == null) return

        val position = playback?.currentStreamPosition ?:
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN

        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(availableActions)

        setCustomAction(stateBuilder)
        var state = playback!!.state

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(1, error)
            state = PlaybackStateCompat.STATE_ERROR
        }

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())

        callback.onPlaybackStateUpdated(stateBuilder.build())

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            callback.onNotificationRequired()
        }
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    override fun onCompletion() {

        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (chainManager.next(false)) {
            handlePlayRequest()
        } else {
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null)
        }
    }


    private fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) {
        val currentMusic = chainManager.current() ?: return
// Set appropriate "Favorite" icon on Custom action:
        val mediaId = currentMusic.description.mediaId ?: return
        val musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId)
//        val favoriteIcon = if (mMusicProvider.isFavorite(musicId))
//            R.drawable.ic_favorite_black_24dp
//        else
//            R.drawable.ic_favorite_border_black_24dp
        val favoriteIcon = R.drawable.ic_favorite_black_24dp

        val customActionExtras = Bundle()
        stateBuilder.addCustomAction(PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_THUMBS_UP, "favorite", favoriteIcon)
                .setExtras(customActionExtras)
                .build())
    }

    private val availableActions: Long
        get() {
            var actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            actions = if (playback?.isPlaying() == true) {
                actions or PlaybackStateCompat.ACTION_PAUSE
            } else {
                actions or PlaybackStateCompat.ACTION_PLAY
            }
            return actions
        }


    override fun onPlaybackStatusChanged(state: Int) {
        updatePlaybackState(null)
    }

    override fun onError(error: String) {
        updatePlaybackState(error)
    }


    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            wtf { "play" }
            if (chainManager.current() == null) {
                chainManager.guess(Calendar.getInstance(), null)
            }
            handlePlayRequest()
        }

        override fun onPause() {
            wtf { "pause" }
            handlePauseRequest()
        }

        override fun onStop() {
            handleStopRequest(null)
        }

        override fun onSkipToNext() {
            wtf { "skip to next" }
            if (playback?.isPlaying() == true) {
                if (chainManager.next(true)) {
                    handlePlayRequest()
                } else {
                    handleStopRequest("Cannot skip")
                }
            }
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val keyEvent = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent == null || keyEvent.action != KeyEvent.ACTION_DOWN) {
                return false
            }
            val keyCode = keyEvent.keyCode
            return when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    onPause()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    onPlay()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    onSkipToNext()
                    true
                }
                else -> super.onMediaButtonEvent(mediaButtonEvent)
            }
        }

        override fun onCustomAction(action: String, extras: Bundle?) {
            if (ACTION_SCAN == action) {
                callback.onSource()
            }

            if (CUSTOM_ACTION_THUMBS_UP == action) {
                val currentMusic = chainManager.current()
                if (currentMusic != null) {
                    val mediaId = currentMusic.description.mediaId
                    if (mediaId != null) {
                        val musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId)
//                        mMusicProvider.setFavorite(musicId, !mMusicProvider.isFavorite(musicId))
                    }
                }
                // playback state needs to be updated because the "Favorite" icon on the
                // custom action will change to reflect the new favorite state.
                updatePlaybackState(null)
            } else {
                wtf { "Unsupported action: $action" }
            }
        }

    }


    interface PlaybackServiceCallback {
        fun onSource()

        fun onPlaybackStart()

        fun onNotificationRequired()

        fun onPlaybackStop()

        fun onPlaybackStateUpdated(newState: PlaybackStateCompat)
    }

    companion object {
        val ACTION_SCAN = "scan"
        // Action to thumbs up a media item
        private val CUSTOM_ACTION_THUMBS_UP = "com.example.android.uamp.THUMBS_UP"
    }
}
