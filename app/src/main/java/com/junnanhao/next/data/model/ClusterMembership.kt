package com.junnanhao.next.data.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne

/**
 * Created by jonashao on 2017/9/26.
 * relationship between cluster and song, and their membership
 */
@Entity
data class ClusterMembership(
        @Id private var id: Long,
        val cluster: ToOne<Cluster>,
        val song: ToOne<Song>,
        var membership: Double
) {
    companion object {
        val MIN_MEMBERSHIP: Double = 0.001
    }
}

//@Entity
//data class ClusterFriendship(
//        @Id private var id: Long,
//        private val one: ToOne<Cluster>,
//        private var friendship: Double,
//        private val another: ToOne<Cluster>
//)