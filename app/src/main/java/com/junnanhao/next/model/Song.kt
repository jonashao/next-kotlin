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
                var displayName: String = "",
                var size: Int = 0,
                var album: String = "") : RealmObject() {

    override fun toString(): String {
        return "Song(path='$path', duration=$duration, title='$title', artist='$artist', displayName='$displayName')"
    }

    fun isSongValid(): Boolean {
        return duration > MIN_DURATION && artist != UNKNOWN
    }

    companion object {
        val MIN_DURATION: Int = 40000
        val UNKNOWN: String = "<unknown>"
    }
}