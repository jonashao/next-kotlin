package com.junnanhao.next

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.view.GestureDetectorCompat
import android.view.*
import com.github.ajalt.timberkt.wtf
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.frag_player.*
import timber.log.Timber


/**
 * Created by Jonas on 2017/5/27.
 * player fragment
 */
class PlayerFragment : Fragment() {

    private lateinit var mMediaBrowser: MediaBrowserCompat
    private lateinit var mDetector: GestureDetectorCompat
    lateinit var rect: Rect


    companion object {
        fun instance(): PlayerFragment {
            return PlayerFragment()
        }

        private val SWIPE_MIN_DISTANCE = 120
        private val SWIPE_MAX_OFF_PATH = 250
        private val SWIPE_THRESHOLD_VELOCITY = 200
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mMediaBrowser = MediaBrowserCompat(context,
                ComponentName(context, MusicService::class.java),
                mConnectionCallbacks,
                null) // optional Bundle

        mDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                Timber.wtf("fling")
                if (rect.contains(
                        if (e1.x < e2.x) (e1.x).toInt() else e2.x.toInt(),
                        if (e1.y < e2.y) (e1.y).toInt() else e2.y.toInt(),
                        if (e1.x < e2.x) (e2.x).toInt() else e1.x.toInt(),
                        if (e1.y < e2.y) (e2.y).toInt() else e1.y.toInt())) {
                    try {
                        if (Math.abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) {
                            return false
                        }

                        if (e1.x - e2.x > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            // left swipe
                            val mediaController: MediaControllerCompat = MediaControllerCompat
                                    .getMediaController(activity)
                            mediaController.transportControls.skipToNext()
                        } else if (e2.x - e1.x > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            // right swipe
                        }

                    } catch (e: Exception) {
                        wtf { "e:$e" }
                    }
                }
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                val mediaController: MediaControllerCompat = MediaControllerCompat
                        .getMediaController(activity)
                mediaController.transportControls.sendCustomAction(MusicService.ACTION_SCAN, null)
            }
        })

    }

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        checkPermission()
        return inflater!!.inflate(R.layout.frag_player, container, false)
    }

    override fun onResume() {
        super.onResume()
        measure()
    }


    private val mSubscriptionCallback: SubscriptionCallback
            = object : SubscriptionCallback() {

        override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            val song = children.getOrNull(0)?.description
            tv_song_title?.text = song?.title
            tv_song_artist?.text = song?.subtitle

            val mediaController: MediaControllerCompat = MediaControllerCompat
                    .getMediaController(activity)
            mediaController.transportControls.play()
        }

        override fun onError(id: String) {
            wtf { "id=$id" }
        }
    }

    private fun checkPermission() {
        RxPermissions(activity)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    wtf { "permission granted = $granted" }
                })
    }

    private fun measure() {
        val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()
        wm.defaultDisplay.getSize(size)
        rect = Rect(0,
                (size.y * 0.2).toInt(),
                size.x,
                (size.y * 0.8).toInt())
    }

    override fun onStart() {
        super.onStart()
        mMediaBrowser.connect()
    }

    override fun onStop() {
        super.onStop()
        MediaControllerCompat
                .getMediaController(activity)
                ?.unregisterCallback(controllerCallback)
        mMediaBrowser.disconnect()
    }


    private val controllerCallback: MediaControllerCompat.Callback
            = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            wtf { "meta data changed: title = ${metadata?.description?.title}" }
            tv_song_title.text = metadata?.description?.title
            tv_song_artist.text = metadata?.description?.subtitle

        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
        }
    }


    private var mMediaId: String? = null
    private val mConnectionCallbacks = object :
            MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            // Get the token for the MediaSession
            val token = mMediaBrowser.sessionToken

            // Create a MediaControllerCompat
            val mediaController = MediaControllerCompat(activity, token)

            // Save the controller
            MediaControllerCompat.setMediaController(activity, mediaController)

            if (mMediaId == null) {
                mMediaId = mMediaBrowser.root
            }
            mMediaBrowser.subscribe(mMediaId!!, mSubscriptionCallback)
            // Finish building the UI
            buildTransportControls()

        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }

        fun buildTransportControls() {
            // Grab the view for the play/pause button
            val mediaController: MediaControllerCompat = MediaControllerCompat
                    .getMediaController(activity)
//            val metadata: MediaMetadataCompat = mediaController.metadata

            container.setOnClickListener {
                val pbState: PlaybackStateCompat = mediaController.playbackState
                if (pbState.state == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.transportControls.pause()
                } else {
                    mediaController.transportControls.play()
                }
            }
            container?.setOnTouchListener({ _, event ->
                mDetector.onTouchEvent(event)
            })

            // Display the initial state

            // Register a Callback to stay in sync
            mediaController.registerCallback(controllerCallback)
        }
    }


}