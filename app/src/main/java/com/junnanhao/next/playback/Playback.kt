package com.junnanhao.next.playback

/**
 * Created by Jonas on 2017/5/26.
 * define interfaces of player
 */

interface Playback {
    /**
     * Start/setup the playback.
     * Resources/listeners would be allocated by implementations.
     */
    fun start()


    fun play(mediaId: String)

    fun pause()

    /**
     * Stop the playback. All resources can be de-allocated by implementations here.
     * @param notifyListeners if true and a callback has been set by setCallback,
     * callback.onPlaybackStatusChanged will be called after changing
     * the state.
     */
    fun stop(notifyListeners: Boolean)


    fun seekTo(position: Long)
    /**
     * @return boolean indicating whether the player is playing or is supposed to be
     * playing when we gain audio focus.
     */
    fun isPlaying(): Boolean



    /**
     * Get the current [android.media.session.PlaybackState.getState]

     * Set the latest playback state as determined by the caller.
     */
    var state: Int

    /**
     * @return pos if currently playing an item
     */
    val currentStreamPosition: Long

    var currentMediaId: String?

    interface Callback {
        /**
         * On current music completed.
         */
        fun onCompletion()

        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        fun onPlaybackStatusChanged(state: Int)

        /**
         * @param error to be added to the PlaybackState
         */
        fun onError(error: String)

        /**
         * @param mediaId being currently played
         */
        fun setCurrentMediaId(mediaId: String)
    }

    fun setCallback(callback: Callback)
}