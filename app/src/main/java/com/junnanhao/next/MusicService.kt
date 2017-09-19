package com.junnanhao.next

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.github.ajalt.timberkt.wtf
import com.junnanhao.next.data.MusicProvider
import com.junnanhao.next.data.ObjectBoxMusicSource
import com.junnanhao.next.data.SongsRepository
import com.junnanhao.next.playback.LocalPlayback
import com.junnanhao.next.playback.Playback
import com.junnanhao.next.playback.PlaybackManager
import com.junnanhao.next.playback.QueueManager
import java.lang.ref.WeakReference


/**
 * Created by jonashao on 2017/9/9.
 * Media playback Service
 */

class MusicService : MediaBrowserServiceCompat(),
        PlaybackManager.PlaybackServiceCallback {

    private var mMediaSession: MediaSessionCompat? = null
    private var mStateBuilder: PlaybackStateCompat.Builder? = null
    private lateinit var musicProvider: MusicProvider
    private lateinit var queueManager: QueueManager
    private lateinit var playbackManager: PlaybackManager
    private lateinit var playback: Playback
    private var mMediaNotificationManager: MediaNotificationManager? = null
    private val mDelayedStopHandler = DelayedStopHandler(this)

    override fun onCreate() {
        super.onCreate()
        musicProvider = MusicProvider(ObjectBoxMusicSource(application))
        musicProvider.retrieveMusic()

        queueManager = QueueManager(musicProvider, object : QueueManager.MetadataUpdateListener {
            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                mMediaSession?.setMetadata(metadata)
            }

            override fun onMetadataRetrieveError() {
//                playbackManager.updatePlaybackState(
//                        getString(R.string.error_no_metadata))
            }

            override fun onCurrentQueueIndexUpdated(queueIndex: Int) {
                playbackManager.handlePlayRequest()
            }

            override fun onQueueUpdated(title: String,
                                        newQueue: List<MediaSessionCompat.QueueItem>) {
                mMediaSession?.setQueue(newQueue)
            }
        })

        playback = LocalPlayback(applicationContext, musicProvider)
        playbackManager = PlaybackManager(this, resources,
                musicProvider, queueManager, playback)

        // Create a MediaSessionCompat
        mMediaSession = MediaSessionCompat(applicationContext, LOG_TAG)

        // Set the session's token so that client activities can communicate with it.
        sessionToken = mMediaSession!!.sessionToken

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession!!.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE)
        mMediaSession!!.setPlaybackState(mStateBuilder!!.build())

        // MySessionCallback() has methods that handle callbacks from a media controller
        mMediaSession!!.setCallback(playbackManager.mediaSessionCallback)

        try {
            mMediaNotificationManager = MediaNotificationManager(this)
        } catch (e: RemoteException) {
            throw IllegalStateException("Could not create a MediaNotificationManager", e)
        }

    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?)
            : MediaBrowserServiceCompat.BrowserRoot? {
        // (Optional) Control the level of access for the specified package name.
        // You'll need to write your own logic to do this.
        return if (allowBrowsing(clientPackageName, clientUid)) {
            // Returns a root ID that clients can use with onLoadChildren() to retrieve
            // the content hierarchy.
            MediaBrowserServiceCompat.BrowserRoot(MY_MEDIA_ROOT_ID, null)
        } else {
            // Clients can connect, but this BrowserRoot is an empty hierachy
            // so onLoadChildren returns nothing. This disables the ability to browse for content.
            MediaBrowserServiceCompat.BrowserRoot(EMPTY_MEDIA_ROOT_ID, null)
        }
    }

    private fun allowBrowsing(clientPackageName: String, clientUid: Int): Boolean {
        return true
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaItem>>) {
        if (!musicProvider.isInitialized()) {
            musicProvider.retrieveMediaAsync()
                    .subscribe()
        }
        result.sendResult(emptyList())
    }


    override fun onStartCommand(startIntent: Intent?, flags: Int, startId: Int): Int {
        if (startIntent != null) {
            val action = startIntent.action
            val command = startIntent.getStringExtra(CMD_NAME)
            if (ACTION_CMD == action) {
                if (CMD_PAUSE == command) {
                    playbackManager.handlePauseRequest()
                }
            } else {
                // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                MediaButtonReceiver.handleIntent(mMediaSession, startIntent)
            }
        }
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())
        return Service.START_STICKY
    }


    override fun onPlaybackStart() {

        mMediaSession?.isActive = true
        mDelayedStopHandler.removeCallbacksAndMessages(null)

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(Intent(applicationContext, MusicService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        wtf { "onDestroy" }
        // Service is being killed, so make sure we release our resources
        playbackManager.handleStopRequest(null)
        mMediaNotificationManager?.stopNotification()

        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mMediaSession?.release()
    }

    override fun onNotificationRequired() {
        mMediaNotificationManager?.startNotification()
    }

    override fun onSource() {
        SongsRepository(application).scanMusic()
                .subscribe()
    }


    override fun onPlaybackStop() {
        mMediaSession?.isActive = false

        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())
        stopForeground(true)
    }

    override fun onPlaybackStateUpdated(newState: PlaybackStateCompat) {
        mMediaSession?.setPlaybackState(newState)
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private class DelayedStopHandler constructor(service: MusicService) : Handler() {
        private val mWeakReference: WeakReference<MusicService> = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()

            if (service?.playbackManager?.playback?.isPlaying() == true) {
                wtf { "Ignoring delayed stop since the media player is in use." }
                return
            }
            wtf { "Stopping service with delay handler." }
            service?.stopSelf()
        }
    }

    companion object {
        private val MY_MEDIA_ROOT_ID = "media_root_id"
        private val EMPTY_MEDIA_ROOT_ID = "empty_root_id"

        val ACTION_SCAN = "scan"

        private val LOG_TAG = "NextMusicService"
        // The action of the incoming Intent indicating that it contains a command
        // to be executed (see {@link #onStartCommand})
        val ACTION_CMD = "com.example.android.uamp.ACTION_CMD"
        // The key in the extras of the incoming Intent indicating the command that
        // should be executed (see {@link #onStartCommand})
        val CMD_NAME = "CMD_NAME"
        // A value of a CMD_NAME key in the extras of the incoming Intent that
        // indicates that the music playback should be paused (see {@link #onStartCommand})
        val CMD_PAUSE = "CMD_PAUSE"
        // A value of a CMD_NAME key that indicates that the music playback should switch
        // to local playback from cast playback.
        val CMD_STOP_CASTING = "CMD_STOP_CASTING"


        // Extra on MediaSession that contains the Cast device name currently connected to
        val EXTRA_CONNECTED_CAST = "com.example.android.uamp.CAST_NAME"

        // Delay stopSelf by using a handler.
        private val STOP_DELAY = 30000

    }
}
