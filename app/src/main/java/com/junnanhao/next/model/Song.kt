package com.junnanhao.next.model

import io.realm.RealmObject

/**
 * Created by Jonas on 2017/5/26.
 * song
 */
open class Song(var path: String = "",
                var duration: Int = 0,
                var title: String = "",
                var artist: String = "",
                var displayName: String = "") : RealmObject() {

    override fun toString(): String {
        return "Song(path='$path', duration=$duration, title='$title', artist='$artist', displayName='$displayName')"
    }
}