package com.junnanhao.next.playback

import android.content.Intent
import android.content.res.Resources
import android.os.AsyncTask
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import com.github.ajalt.timberkt.wtf
import com.junnanhao.next.data.MusicProvider


/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
class PlaybackManager(
        private val mServiceCallback: PlaybackServiceCallback,
        private val mResources: Resources,
        private val mMusicProvider: MusicProvider,
        private val mQueueManager: QueueManager,
        playback: Playback) : Playback.Callback {

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
        val currentMusic = mQueueManager.currentMusic
        mServiceCallback.onPlaybackStart()
        playback?.play(currentMusic?.description?.mediaId ?: return)
    }

    /**
     * Handle a request to pause music
     */
    fun handlePauseRequest() {
        if (playback?.isPlaying() ?: return) {
            playback?.pause()
            mServiceCallback.onPlaybackStop()
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
        playback!!.stop(true)
        mServiceCallback.onPlaybackStop()
        updatePlaybackState(withError)
    }


    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    fun updatePlaybackState(error: String?) {
        wtf { "updatePlaybackState, playback state= ${playback?.state}" }
        var position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
//        if (playback != null && playback!!.isConnected) {
//            position = playback!!.currentStreamPosition
//        }


        val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(availableActions)

        setCustomAction(stateBuilder)
        var state = playback!!.state

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error)
            state = PlaybackStateCompat.STATE_ERROR
        }

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())

        // Set the activeQueueItemId if the current index is valid.
        val currentMusic = mQueueManager.currentMusic
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.queueId)
        }

        mServiceCallback.onPlaybackStateUpdated(stateBuilder.build())

        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            mServiceCallback.onNotificationRequired()
        }
    }

    private fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) {
//        val currentMusic = mQueueManager.currentMusic ?: return
//// Set appropriate "Favorite" icon on Custom action:
//        val mediaId = currentMusic.description.mediaId ?: return
//        val musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId)
//        val favoriteIcon = if (mMusicProvider.isFavorite(musicId))
//            R.drawable.ic_star_on
//        else
//            R.drawable.ic_star_off
//        LogHelper.d(TAG, "updatePlaybackState, setting Favorite custom action of music ",
//                musicId, " current favorite=", mMusicProvider.isFavorite(musicId))
//        val customActionExtras = Bundle()
//        WearHelper.setShowCustomActionOnWear(customActionExtras, true)
//        stateBuilder.addCustomAction(PlaybackStateCompat.CustomAction.Builder(
//                CUSTOM_ACTION_THUMBS_UP, mResources.getString(R.string.favorite), favoriteIcon)
//                .setExtras(customActionExtras)
//                .build())
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

    /**
     * Implementation of the Playback.Callback interface
     */
    override fun onCompletion() {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mQueueManager.skipQueuePosition(1)) {
            handlePlayRequest()
            mQueueManager.updateMetadata()
        } else {
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null)
        }
    }

    override fun onPlaybackStatusChanged(state: Int) {
        updatePlaybackState(null)
    }

    override fun onError(error: String) {
        updatePlaybackState(error)
    }

    override fun setCurrentMediaId(mediaId: String) {
        mQueueManager.setQueueFromMusic(mediaId)
    }


    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     *
     * @param playback switch to this playback
     */
    fun switchToPlayback(playback: Playback?, resumePlaying: Boolean) {
//        if (playback == null) {
//            throw IllegalArgumentException("Playback cannot be null")
//        }
//        // Suspends current state.
//        val oldState = this.playback!!.state
//        val pos = this.playback!!.currentStreamPosition
//        val currentMediaId = this.playback!!.currentMediaId
//        this.playback!!.stop(false)
//        playback.setCallback(this)
//        playback.currentMediaId = currentMediaId
//        playback.seekTo(if (pos < 0) 0 else pos)
//        playback.start()
//        // Swaps instance.
//        this.playback = playback
//        when (oldState) {
//            PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_CONNECTING, PlaybackStateCompat.STATE_PAUSED -> this.playback!!.pause()
//            PlaybackStateCompat.STATE_PLAYING -> {
//                val currentMusic = mQueueManager.currentMusic
//                if (resumePlaying && currentMusic != null) {
//                    this.playback!!.play(currentMusic)
//                } else if (!resumePlaying) {
//                    this.playback!!.pause()
//                } else {
//                    this.playback!!.stop(true)
//                }
//            }
//            PlaybackStateCompat.STATE_NONE -> {
//            }
//            else -> LogHelper.d(TAG, "Default called. Old state is ", oldState)
//        }
    }


    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
//            LogHelper.d(TAG, "play")
//            if (mQueueManager.currentMusic == null) {
//                mQueueManager.setRandomQueue()
//            }
            handlePlayRequest()
        }

        override fun onSkipToQueueItem(queueId: Long) {
//            LogHelper.d(TAG, "OnSkipToQueueItem:" + queueId)
            mQueueManager.setCurrentQueueItem(queueId)
        }

        override fun onSeekTo(position: Long) {
//            LogHelper.d(TAG, "onSeekTo:", position)
            playback!!.seekTo(position.toInt().toLong())
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
//            LogHelper.d(TAG, "playFromMediaId mediaId:", mediaId, "  extras=", extras)
            mQueueManager.setQueueFromMusic(mediaId)
            handlePlayRequest()
        }

        override fun onPause() {
//            LogHelper.d(TAG, "pause. current state=" + playback!!.state)
            handlePauseRequest()
        }

        override fun onStop() {
//            LogHelper.d(TAG, "stop. current state=" + playback!!.state)
            handleStopRequest(null)
        }

        override fun onSkipToNext() {
//            LogHelper.d(TAG, "skipToNext")
            if (mQueueManager.skipQueuePosition(1)) {
                handlePlayRequest()
                mQueueManager.updateMetadata()
            } else {
                handleStopRequest("Cannot skip")
            }
        }

        override fun onSkipToPrevious() {
            if (mQueueManager.skipQueuePosition(-1)) {
                handlePlayRequest()
                mQueueManager.updateMetadata()

            } else {
                handleStopRequest("Cannot skip")
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
            if (action ==  ACTION_SCAN) {
               mServiceCallback.onSource()
            }
//            if (CUSTOM_ACTION_THUMBS_UP == action) {
//                LogHelper.i(TAG, "onCustomAction: favorite for current track")
//                val currentMusic = mQueueManager.currentMusic
//                if (currentMusic != null) {
//                    val mediaId = currentMusic.description.mediaId
//                    if (mediaId != null) {
//                        val musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId)
//                        mMusicProvider.setFavorite(musicId, !mMusicProvider.isFavorite(musicId))
//                    }
//                }
//                // playback state needs to be updated because the "Favorite" icon on the
//                // custom action will change to reflect the new favorite state.
//                updatePlaybackState(null)
//            } else {
//                LogHelper.e(TAG, "Unsupported action: ", action)
//            }
        }

        /**
         * Handle free and contextual searches.
         *
         *
         * All voice searches on Android Auto are sent to this method through a connected
         * [android.support.v4.media.session.MediaControllerCompat].
         *
         *
         * Threads and async handling:
         * Search, as a potentially slow operation, should run in another thread.
         *
         *
         * Since this method runs on the main thread, most apps with non-trivial metadata
         * should defer the actual search to another thread (for example, by using
         * an [AsyncTask] as we do here).
         */
        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
//            LogHelper.d(TAG, "playFromSearch  query=", query, " extras=", extras)
//
//            playback!!.state = PlaybackStateCompat.STATE_CONNECTING
//            val successSearch = mQueueManager.setQueueFromSearch(query, extras)
//            if (successSearch) {
//                handlePlayRequest()
//                mQueueManager.updateMetadata()
//            } else {
//                updatePlaybackState("Could not find music")
//            }
        }
    }


    interface PlaybackServiceCallback {
       fun  onSource()

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
