package com.junnanhao.next.data

import android.support.v4.media.MediaMetadataCompat

interface MusicProviderSource {

    companion object {
        val CUSTOM_METADATA_MBID: String = "__mbid__"
    }


    operator fun iterator(): Iterator<MediaMetadataCompat>
}
