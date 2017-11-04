package com.junnanhao.next.data.model

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany

/**
 * Created by jonashao on 2017/9/26.
 * a cluster of songs
 */
@Entity
data class Cluster(
        @Id var id: Long,
        @Backlink val songs: ToMany<ClusterMembership>
//        val similar: ToMany<Map.Entry<Cluster, Double>>
)