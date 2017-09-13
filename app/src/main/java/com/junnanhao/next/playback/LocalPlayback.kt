package com.junnanhao.next.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import com.github.ajalt.timberkt.wtf
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.junnanhao.next.MusicService
import com.junnanhao.next.data.MusicProvider
import com.junnanhao.next.utils.MediaIDHelper


/**
 * Created by jonashao on 2017/9/11.
 * Playback implemented with ExoPlayer
 */
class LocalPlayback(private val context: Context, private val musicProvider: MusicProvider) : Playback {


    companion object {
        // we don't have audio focus, and can't duck (play at a low volume)
        private val AUDIO_NO_FOCUS_NO_DUCK = 0
        // we don't have focus, but can duck (play at a low volume)
        private val AUDIO_NO_FOCUS_CAN_DUCK = 1
        // we have full audio focus
        private val AUDIO_FOCUSED = 2

        // The volume we set the media player to when we lose audio focus, but are
        // allowed to reduce the volume instead of stopping playback.
        val VOLUME_DUCK = 0.2f
        // The volume we set the media player when we have audio focus.
        val VOLUME_NORMAL = 1.0f
    }

    /**
     * Set the latest playback state as determined by the caller.
     */
    override var state: Int
        get() {
            if (mPlayer == null) {
                PlaybackStateCompat.STATE_NONE
            }
            return when (mPlayer?.playbackState) {
                Player.STATE_IDLE -> PlaybackStateCompat.STATE_PAUSED
                Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                Player.STATE_READY -> if (mPlayer?.playWhenReady == true)
                    PlaybackStateCompat.STATE_PLAYING
                else
                    PlaybackStateCompat.STATE_PAUSED
                Player.STATE_ENDED -> PlaybackStateCompat.STATE_PAUSED
                else -> PlaybackStateCompat.STATE_NONE
            }
        }
        set(value) {}
    /**
     * @return pos if currently playing an item
     */
    override val currentStreamPosition: Long
        get() = mPlayer?.currentPosition ?: 0

    override var currentMediaId: String? = null
        get
        set

    private var mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK

    private var mAudioManager: AudioManager? = null

    private var mPlayOnFocusGain: Boolean = false
    private var mAudioNoisyReceiverRegistered: Boolean = false
    private var mCallback: Playback.Callback? = null
    private val mEventListener = ExoPlayerEventListener()

    init {
        start()
    }

    /**
     * Start/setup the playback.
     * Resources/listeners would be allocated by implementations.
     */
    override fun start() {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    }

    override fun play(mediaId: String) {
        mPlayOnFocusGain = true
        tryToGetAudioFocus()
        registerAudioNoisyReceiver()
        val mediaHasChanged: Boolean = !TextUtils.equals(mediaId, currentMediaId)
        if (mediaHasChanged) {
            currentMediaId = mediaId
        }
        if (mediaHasChanged || mPlayer == null) {
            releaseResources(false) // release everything except the player
            val musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId)
            val track = musicProvider.getMusic(musicId)
            if (mPlayer == null) {
                val bandwidthMeter = DefaultBandwidthMeter()
                val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
                val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
                mPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)

                mPlayer?.addListener(mEventListener)
            }
            val audioAttributes = AudioAttributes.Builder()
                    .setContentType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        CONTENT_TYPE_MUSIC else 2)
                    .build()

            mPlayer?.audioAttributes = audioAttributes

            val source: Uri? = track?.description?.mediaUri

            // Produces DataSource instances through which media data is loaded.
            val dataSourceFactory = DefaultDataSourceFactory(
                    context, Util.getUserAgent(context, "next"), DefaultBandwidthMeter())
            // Produces Extractor instances for parsing the media data.
            val extractorsFactory = DefaultExtractorsFactory()
            // The MediaSource represents the media to be played.
            val mediaSource = ExtractorMediaSource(
                    source, dataSourceFactory, extractorsFactory, null, null)

            // Prepares media to play (happens on background thread) and triggers
            // {@code onPlayerStateChanged} callback when the stream is ready to play.
            mPlayer?.prepare(mediaSource)
        }
        configurePlayerState()

    }

    override fun pause() {
        // Pause player and cancel the 'foreground service' state.
        mPlayer?.playWhenReady = false
        // While paused, retain the player instance, but give up audio focus.
        releaseResources(false)
        unregisterAudioNoisyReceiver()
    }

    private fun unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            context.unregisterReceiver(mAudioNoisyReceiver)
            mAudioNoisyReceiverRegistered = false
        }
    }

    override fun seekTo(position: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * @return boolean indicating whether the player is playing or is supposed to be
     * playing when we gain audio focus.
     */
    override fun isPlaying(): Boolean {
        return mPlayOnFocusGain || mPlayer?.playWhenReady == true
    }

    private var mPlayer: SimpleExoPlayer? = null


    /**
     * Stop the playback. All resources can be de-allocated by implementations here.
     * @param notifyListeners if true and a callback has been set by setCallback,
     * callback.onPlaybackStatusChanged will be called after changing
     * the state.
     */
    override fun stop(notifyListeners: Boolean) {
        unregisterAudioNoisyReceiver()
    }

    override fun setCallback(callback: Playback.Callback) {
        mCallback = callback
    }


    /**
     * Releases resources used by the service for playback, which is mostly just the WiFi lock for
     * local playback. If requested, the ExoPlayer instance is also released.
     *
     * @param releasePlayer Indicates whether the player should also be released
     */
    private fun releaseResources(releasePlayer: Boolean) {
        wtf { "releaseResources. releasePlayer= $releasePlayer" }

        // Stops and releases player (if requested and available).
        if (releasePlayer && mPlayer != null) {
            mPlayer?.release()
            mPlayer = null
            mPlayOnFocusGain = false
        }
    }

    private fun tryToGetAudioFocus() {
        wtf { "try to get audio focus" }
        val result = mAudioManager?.requestAudioFocus(
                mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
        mCurrentAudioFocusState = if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            AUDIO_FOCUSED
        } else {
            AUDIO_NO_FOCUS_NO_DUCK
        }
    }


    private val mOnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        wtf { "onAudioFocusChange. focusChange= $focusChange" }
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> mCurrentAudioFocusState = AUDIO_FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost audio focus, but will gain it back (shortly), so note whether
                // playback should resume
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
                mPlayOnFocusGain = mPlayer?.playWhenReady ?: false
            }
            AudioManager.AUDIOFOCUS_LOSS ->
                // Lost audio focus, probably "permanently"
                mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK
        }

        if (mPlayer != null) {
            // Update the player state based on the change
            configurePlayerState()
        }
    }

    private fun configurePlayerState() {
        wtf { "configurePlayerState. mCurrentAudioFocusState=$mCurrentAudioFocusState" }
        if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pause()
        } else {
            registerAudioNoisyReceiver()

            mPlayer?.volume = if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                VOLUME_DUCK
            } else VOLUME_NORMAL

            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                mPlayer?.playWhenReady = true
                mPlayOnFocusGain = false
            }
        }
    }

    private val mAudioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private fun registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            context.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter)
            mAudioNoisyReceiverRegistered = true
        }
    }

    private val mAudioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {

                if (isPlaying()) {
                    val i = Intent(context, MusicService::class.java)
                    i.action = MusicService.ACTION_CMD
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE)
                    context.startService(i)
                }
            }
        }
    }

    private inner class ExoPlayerEventListener : Player.EventListener {

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        }

        override fun onLoadingChanged(isLoading: Boolean) {
        }


        override fun onPositionDiscontinuity() {
        }


        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE, Player.STATE_BUFFERING, Player.STATE_READY ->
                    mCallback?.onPlaybackStatusChanged(state)

                Player.STATE_ENDED ->
                    // The media player finished playing the current song.
                    mCallback?.onCompletion()
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            val what: String? = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.message
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.message
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.message
                else -> "Unknown: " + error
            }

            wtf { "ExoPlayer error: what= $what" }
            mCallback?.onError("ExoPlayer error " + what)
        }


    }

}