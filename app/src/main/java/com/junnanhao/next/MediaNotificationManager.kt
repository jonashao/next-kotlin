/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.junnanhao.next

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.RemoteException
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.NotificationCompat
import com.github.ajalt.timberkt.wtf

import com.junnanhao.next.utils.ResourceHelper


/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
class MediaNotificationManager @Throws(RemoteException::class)
constructor(private val mService: MusicService) : BroadcastReceiver() {
    private var mSessionToken: MediaSessionCompat.Token? = null
    private var mController: MediaControllerCompat? = null
    private var mTransportControls: MediaControllerCompat.TransportControls? = null

    private var mPlaybackState: PlaybackStateCompat? = null
    private var mMetadata: MediaMetadataCompat? = null

    private val mNotificationManager: NotificationManagerCompat

    private val mPauseIntent: PendingIntent
    private val mPlayIntent: PendingIntent
    private val mPreviousIntent: PendingIntent
    private val mNextIntent: PendingIntent

    private val mStopCastIntent: PendingIntent

    private val mNotificationColor: Int

    private var mStarted = false

    init {
        updateSessionToken()

        mNotificationColor = ResourceHelper.getThemeColor(mService, R.attr.colorPrimary,
                Color.DKGRAY)

        mNotificationManager = NotificationManagerCompat.from(mService)

        val pkg = mService.packageName
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mPreviousIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mNextIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT)
        mStopCastIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                Intent(ACTION_STOP_CASTING).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT)

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll()
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before [.stopNotification] is called.
     */
    fun startNotification() {
        if (!mStarted) {
            mMetadata = mController?.metadata
            mPlaybackState = mController?.playbackState

            // The notification must be updated after setting started to true
            val notification = createNotification()
            if (notification != null) {
                mController!!.registerCallback(mCb)
                val filter = IntentFilter()
                filter.addAction(ACTION_NEXT)
                filter.addAction(ACTION_PAUSE)
                filter.addAction(ACTION_PLAY)
                filter.addAction(ACTION_PREV)
                filter.addAction(ACTION_STOP_CASTING)
                mService.registerReceiver(this, filter)

                mService.startForeground(NOTIFICATION_ID, notification)
                mStarted = true
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    fun stopNotification() {
        if (mStarted) {
            mStarted = false
            mController!!.unregisterCallback(mCb)
            try {
                mNotificationManager.cancel(NOTIFICATION_ID)
                mService.unregisterReceiver(this)
            } catch (ex: IllegalArgumentException) {
                // ignore if the receiver is not registered.
            }

            mService.stopForeground(true)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        wtf { "Received intent with action $action" }
        when (action) {
            ACTION_PAUSE -> mTransportControls!!.pause()
            ACTION_PLAY -> mTransportControls!!.play()
            ACTION_NEXT -> mTransportControls!!.skipToNext()
            ACTION_PREV -> mTransportControls!!.skipToPrevious()
            ACTION_STOP_CASTING -> {
                val i = Intent(context, MusicService::class.java)
                i.action = MusicService.ACTION_CMD
                i.putExtra(MusicService.CMD_NAME, MusicService.CMD_STOP_CASTING)
                mService.startService(i)
            }
            else -> wtf { "Unknown intent ignored. Action= $action" }
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see [android.media.session.MediaController.Callback.onSessionDestroyed])
     */
    @Throws(RemoteException::class)
    private fun updateSessionToken() {
        val freshToken = mService.sessionToken
        if (mSessionToken == null && freshToken != null || mSessionToken != null && mSessionToken != freshToken) {
            if (mController != null) {
                mController!!.unregisterCallback(mCb)
            }
            mSessionToken = freshToken
            if (mSessionToken != null) {
                mController = MediaControllerCompat(mService, mSessionToken!!)
                mTransportControls = mController!!.transportControls
                if (mStarted) {
                    mController!!.registerCallback(mCb)
                }
            }
        }
    }

    private fun createContentIntent(description: MediaDescriptionCompat?): PendingIntent {
        val openUI = Intent(mService, MainActivity::class.java)
        openUI.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        openUI.putExtra(MainActivity.EXTRA_START_FULLSCREEN, true)
        if (description != null) {
            openUI.putExtra(MainActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, description)
        }
        return PendingIntent.getActivity(mService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private val mCb = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            mPlaybackState = state
            wtf { "Received new playback state $state" }
            if (state.state == PlaybackStateCompat.STATE_STOPPED || state.state == PlaybackStateCompat.STATE_NONE) {
                stopNotification()
            } else {
                val notification = createNotification()
                if (notification != null) {
                    mNotificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mMetadata = metadata
            wtf { "Received new metadata $metadata" }
            val notification = createNotification()
            if (notification != null) {
                mNotificationManager.notify(NOTIFICATION_ID, notification)
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            wtf { "Session was destroyed, resetting to the new session token" }
            try {
                updateSessionToken()
            } catch (e: RemoteException) {
                wtf { "could not connect media controller  $e" }
            }

        }
    }

    private fun createNotification(): Notification? {
        wtf { "updateNotificationMetadata. mMetadata= $mMetadata" }
        if (mMetadata == null || mPlaybackState == null) {
            return null
        }

        val notificationBuilder = NotificationCompat.Builder(mService)
        var playPauseButtonPosition = 0

        // If skip to previous action is enabled
        if (mPlaybackState!!.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {

            notificationBuilder.addAction(R.drawable.ic_skip_previous_black_24dp,
                    mService.getString(R.string.label_previous), mPreviousIntent)

            // If there is a "skip to previous" button, the play/pause button will
            // be the second one. We need to keep track of it, because the MediaStyle notification
            // requires to specify the index of the buttons (actions) that should be visible
            // when in compact view.
            playPauseButtonPosition = 1
        }

        addPlayPauseAction(notificationBuilder)

        // If skip to next action is enabled
        if (mPlaybackState!!.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
            notificationBuilder.addAction(R.drawable.ic_skip_next_black_24dp,
                    mService.getString(R.string.label_next), mNextIntent)
        }

        val description = mMetadata!!.description

        var fetchArtUrl: String? = null
        var art: Bitmap? = null
        if (description.iconUri != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            val artUrl = description.iconUri!!.toString()
            art = AlbumArtCache.instance.getBigImage(artUrl)
            if (art == null) {
                fetchArtUrl = artUrl
                // use a placeholder art while the remote art is being downloaded
                art = BitmapFactory.decodeResource(mService.resources,
                        R.drawable.ic_music)
            }
        }

        notificationBuilder
                .setStyle(NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(
                                *intArrayOf(playPauseButtonPosition))  // show only play/pause in compact view
                        .setMediaSession(mSessionToken))
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_music)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setLargeIcon(art)

        if (mController != null && mController!!.extras != null) {
            val castName = mController!!.extras.getString(MusicService.EXTRA_CONNECTED_CAST)
            if (castName != null) {
                val castInfo = mService.resources
                        .getString(R.string.casting_to_device, castName)
                notificationBuilder.setSubText(castInfo)
                notificationBuilder.addAction(R.drawable.ic_close_black_24dp,
                        mService.getString(R.string.stop_casting), mStopCastIntent)
            }
        }

        setNotificationPlaybackState(notificationBuilder)
        if (fetchArtUrl != null) {
            fetchBitmapFromURLAsync(fetchArtUrl, notificationBuilder)
        }

        return notificationBuilder.build()
    }

    private fun addPlayPauseAction(builder: NotificationCompat.Builder) {

        val label: String
        val icon: Int
        val intent: PendingIntent
        if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING) {
            label = mService.getString(R.string.label_pause)
            icon = R.drawable.ic_pause_black_24dp
            intent = mPauseIntent
        } else {
            label = mService.getString(R.string.label_play)
            icon = R.drawable.ic_play_arrow_black_24dp
            intent = mPlayIntent
        }
        builder.addAction(android.support.v4.app.NotificationCompat.Action(icon, label, intent))
    }

    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        if (mPlaybackState == null || !mStarted) {
            mService.stopForeground(true)
            return
        }
        if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING && mPlaybackState!!.position >= 0) {
            wtf {
                "updateNotificationPlaybackState. updating playback position to " +
                        "${(System.currentTimeMillis() - mPlaybackState!!.position) / 1000} seconds"
            }
            builder
                    .setWhen(System.currentTimeMillis() - mPlaybackState!!.position)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
        } else {
            wtf { "updateNotificationPlaybackState. hiding playback position" }
            builder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false)
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING)
    }

    private fun fetchBitmapFromURLAsync(bitmapUrl: String,
                                        builder: NotificationCompat.Builder) {
        AlbumArtCache.instance.fetch(bitmapUrl, object : AlbumArtCache.FetchListener() {
            override fun onFetched(artUrl: String, bitmap: Bitmap, icon: Bitmap) {
                if (mMetadata != null && mMetadata!!.description.iconUri != null &&
                        mMetadata!!.description.iconUri!!.toString() == artUrl) {
                    // If the media is still the same, update the notification:
                    builder.setLargeIcon(bitmap)
                    mNotificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }
        })
    }

    companion object {

        private val NOTIFICATION_ID = 412
        private val REQUEST_CODE = 100

        val ACTION_PAUSE = "com.junnanhao.next.pause"
        val ACTION_PLAY = "com.junnanhao.next.play"
        val ACTION_PREV = "com.junnanhao.next.prev"
        val ACTION_NEXT = "com.junnanhao.next.next"
        val ACTION_STOP_CASTING = "com.junnanhao.next.stop_cast"
    }
}
