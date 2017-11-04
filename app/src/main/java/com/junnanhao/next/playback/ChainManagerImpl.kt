package com.junnanhao.next.playback

import android.support.v4.media.MediaMetadataCompat
import com.junnanhao.next.App
import com.junnanhao.next.data.MusicProvider
import com.junnanhao.next.data.model.Cluster
import com.junnanhao.next.data.model.ClusterMembership
import io.objectbox.Box
import java.util.*

class ChainManagerImpl(private val provider: MusicProvider, application: App) : ChainManager {
    private val current: MediaMetadataCompat?
        get() = provider.getMusic(currentCluster?.songs?.getOrNull(position)?.song?.targetId)
    private var currentCluster: Cluster? = null
    private var position: Int = 0
    private var skipCount: Int = 0
    private val size: Int get() = currentCluster?.songs?.size ?: 0
    private val suit: Boolean get() = skipCount / size < 0.05
    private val membershipBox: Box<ClusterMembership> = application.boxStore
            .boxFor(ClusterMembership::class.java)

    init {
        provider.getShuffledMusic()
    }


    override fun next(isSkip: Boolean): Boolean {
        if (currentCluster == null) return false
        if (position >= size) return false

        if (isSkip) {
            if (suit) {
                val membership = currentCluster!!.songs[position]
                membership.membership /= 2
                if (membership.membership < ClusterMembership.MIN_MEMBERSHIP) {
                    membershipBox.remove(membership)
                }
                membershipBox.put(membership)
                //save membership
                skipCount++
                position++
            } else {
                // decrease relativity between current cluster and current situation
                // change a cluster
            }
        }
        return true
    }

    override fun guess(calendar: Calendar?, nothing: Nothing?) {
        position = 0
        currentCluster = provider.getShuffledCluster(calendar, nothing)
    }

    override fun current(): MediaMetadataCompat? {
        if (current == null) {
            guess(null, null)
        }
        return current
    }

    override fun updateMetaData() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}