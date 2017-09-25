package com.junnanhao.next.playback

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import com.github.ajalt.timberkt.wtf
import com.junnanhao.next.data.MusicProvider
import com.junnanhao.next.utils.MediaIDHelper
import com.junnanhao.next.utils.QueueHelper
import java.util.*

/**
 * Simple data provider for queues. Keeps track of a current queue and a current index in the
 * queue. Also provides methods to set the current queue based on common queries, relying on a
 * given MusicProvider to provide the actual media metadata.
 */
class QueueManager(private val mMusicProvider: MusicProvider, val mListener: MetadataUpdateListener) {

    // "Now playing" queue:
    private var mPlayingQueue: List<QueueItem>
        get
    private var mCurrentIndex: Int = 0

    init {
        mPlayingQueue = Collections.synchronizedList(ArrayList())
        mCurrentIndex = 0
    }

    private fun setCurrentQueueIndex(index: Int) {
        if (index >= 0 && index < mPlayingQueue.size) {
            mCurrentIndex = index
//            mListener.onCurrentQueueIndexUpdated(mCurrentIndex)
        }
    }

    fun setCurrentQueueItem(queueId: Long): Boolean {
        // set the current index on queue from the queue Id:
        val index = mPlayingQueue.indexOf(queueId)
        setCurrentQueueIndex(index)
        return index >= 0
    }

    fun setCurrentQueueItem(mediaId: String): Boolean {
        // set the current index on queue from the music Id:
        val index = mPlayingQueue.indexOf(mediaId)
        setCurrentQueueIndex(index)
        updateMetadata()
        return index >= 0
    }

    fun skipQueuePosition(amount: Int): Boolean {
        var index = mCurrentIndex + amount
        if (index < 0) {
            // skip backwards before the first song will keep you on the first song
            index = 0
        } else {
            // skip forwards when in last song will cycle back to start of the queue
            index %= mPlayingQueue.size
        }
        if (!QueueHelper.isIndexPlayable(index, mPlayingQueue)) {
            wtf {
                "Cannot increment queue index by $amount." +
                        " Current=$mCurrentIndex queue length=${mPlayingQueue.size}"
            }
            return false
        }

        mCurrentIndex = index
        return true
    }

    val currentMusic: QueueItem?
        get() = mPlayingQueue.getOrNull(mCurrentIndex)

    val currentQueueSize: Int
        get() = mPlayingQueue.size

    private fun setCurrentQueue(newQueue: List<QueueItem>?,
                                initialMediaId: String? = null) {
        mPlayingQueue = newQueue ?: return
        val index = if (initialMediaId != null)
            mPlayingQueue.indexOf(initialMediaId) else 0

        mCurrentIndex = Math.max(index, 0)
        updateMetadata()

//        mListener.onQueueUpdated(title, newQueue)
    }

    fun setQueueFromState(instance: Calendar?, nothing: Nothing?) {
        setCurrentQueue(QueueHelper.getRandomQueue(mMusicProvider))
    }

    fun setQueueFromSearch(query: String, extras: Bundle): Boolean {
        val queue = QueueHelper.getPlayingQueueFromSearch(query, extras, mMusicProvider)
        setCurrentQueue(queue)
        return !queue.isEmpty()
    }

    fun setQueueFromMusic(mediaId: String) {
        wtf { "setQueueFromMusic $mediaId" }

        // The mediaId used here is not the unique musicId. This one comes from the
        // MediaBrowser, and is actually a "hierarchy-aware mediaID": a concatenation of
        // the hierarchy in MediaBrowser and the actual unique musicID. This is necessary
        // so we can build the correct playing queue, based on where the track was
        // selected from.
        var canReuseQueue = false
        if (isSameBrowsingCategory(mediaId)) {
            canReuseQueue = setCurrentQueueItem(mediaId)
        }
        if (!canReuseQueue) {
            setCurrentQueue(QueueHelper
                    .getPlayingQueue(mediaId, mMusicProvider), mediaId)
        }
    }

    private fun isSameBrowsingCategory(mediaId: String): Boolean {
        val newBrowseHierarchy = MediaIDHelper.getHierarchy(mediaId)
        val current = currentMusic ?: return false
        val currentBrowseHierarchy = MediaIDHelper.getHierarchy(
                current.description.mediaId ?: return false
        )
        return Arrays.equals(newBrowseHierarchy, currentBrowseHierarchy)
    }

    private fun List<QueueItem>.indexOf(queueId: Long): Int {
        forEachIndexed { index, queueItem ->
            if (queueItem.queueId == queueId) {
                return index
            }
        }
        return -1
    }

    private fun List<QueueItem>.indexOf(mediaId: String): Int {
        forEachIndexed { index, queueItem ->
            if (queueItem.description.mediaId == mediaId) {
                return index
            }
        }
        return -1
    }


    fun updateMetadata() {

        if (currentMusic == null) {
            mListener.onMetadataRetrieveError()
            return
        }

        val musicId = MediaIDHelper.extractMusicIDFromMediaID(
                currentMusic!!.description.mediaId)
        val metadata = mMusicProvider.getMusic(musicId) ?: throw IllegalArgumentException("Invalid musicId " + musicId)

        mListener.onMetadataChanged(metadata)
    }


    interface MetadataUpdateListener {
        fun onMetadataChanged(metadata: MediaMetadataCompat)
        fun onMetadataRetrieveError()
        fun onCurrentQueueIndexUpdated(queueIndex: Int)
        fun onQueueUpdated(title: String, newQueue: List<MediaSessionCompat.QueueItem>)
    }
}
