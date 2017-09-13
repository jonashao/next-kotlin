package com.junnanhao.next.data

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Created by jonashao on 2017/9/11.
 * Music provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
class MusicProvider(source: MusicProviderSource) {

    private val _source: MusicProviderSource = source

    fun getMusic(mediaId: String?): MediaMetadataCompat? {
        return if (mMusicListById.containsKey(mediaId)) mMusicListById[mediaId] else null
    }

    private val mMusicListById: ConcurrentMap<String, MediaMetadataCompat>

    init {
        mMusicListById = ConcurrentHashMap<String, MediaMetadataCompat>()

    }

    fun retrieveMusic() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING

                val tracks = _source.iterator()
                while (tracks.hasNext()) {
                    val item = tracks.next()
                    val musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                    mMusicListById.put(musicId, item)
                }
            }
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.INITIALIZED
            }
        }
    }


    @Synchronized
    fun updateMusicArt(musicId: String, albumArt: Bitmap, icon: Bitmap) {
        var metadata = getMusic(musicId)
        metadata = MediaMetadataCompat.Builder(metadata!!)

                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                // example, on the lockscreen background when the media session is active.
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)

                // set small version of the album art in the DISPLAY_ICON. This is used on
                // the MediaDescription and thus it should be small to be serialized if
                // necessary
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)

                .build()

        val mutableMetadata = mMusicListById[musicId] ?:
                throw IllegalStateException("Unexpected error: Inconsistent data structures in " + "MusicProvider")

//        mutableMetadata.mediaMetadata = metadata
    }


    fun isInitialized(): Boolean {
        return mCurrentState == State.INITIALIZED
    }

    fun loadChildren(parentId: String): List<MediaItem> {
        val mediaItems: MutableList<MediaItem> = ArrayList()
        val desc = MediaDescriptionCompat.Builder().setMediaId("2")
                .setTitle("hello")
                .setSubtitle("Koche")
                .build()
        mediaItems.add(MediaItem(desc, 1))
        return mediaItems
    }

    internal enum class State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private var mCurrentState: State = State.NON_INITIALIZED

    fun getShuffledMusic(): List<MediaMetadataCompat> {
        if (mCurrentState != State.INITIALIZED) {
            return emptyList()
        }
        val shuffled = ArrayList<MediaMetadataCompat>(mMusicListById.size)
        shuffled += mMusicListById.values
        Collections.shuffle(shuffled)
        return shuffled
    }

    fun searchMusicByAlbum(album: String?): Iterable<MediaMetadataCompat> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun searchMusicBySongTitle(query: String): Iterable<MediaMetadataCompat> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}
