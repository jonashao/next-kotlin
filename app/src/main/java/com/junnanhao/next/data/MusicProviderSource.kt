package com.junnanhao.next.data

import android.support.v4.media.MediaMetadataCompat

interface MusicProviderSource {
    operator fun iterator(): Iterator<MediaMetadataCompat>
}
