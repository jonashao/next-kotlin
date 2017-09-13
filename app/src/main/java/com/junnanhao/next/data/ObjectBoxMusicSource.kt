package com.junnanhao.next.data

import android.app.Application
import android.support.v4.media.MediaMetadataCompat

/**
 * Created by jonashao on 2017/9/11.
 * Music Provider Source implemented by ObjectBox
 */
class ObjectBoxMusicSource(application: Application) : MusicProviderSource {

    private val source: SongsDataSource = SongsRepository(application)

    override fun iterator(): Iterator<MediaMetadataCompat> {
        if (!source.isInitialized()) {
            source.scanMusic()
        }

        return source.getSongs().map { song ->
            MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.resId.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration.toLong())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.art)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, song.path)
                    .build()
        }.iterator()
    }
}