package com.junnanhao.next.data

import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import com.junnanhao.next.data.model.MutableMediaMetadata
import io.reactivex.Observable
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
        return if (mMusicListById.containsKey(mediaId)) mMusicListById[mediaId]?.metadata else null
    }

    private val mMusicListById: ConcurrentMap<String, MutableMediaMetadata>

    init {
        mMusicListById = ConcurrentHashMap<String, MutableMediaMetadata>()
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    fun retrieveMediaAsync(): Observable<Any> {
        return Observable.create { source ->
            if (mCurrentState != State.INITIALIZED) {
                retrieveMusic()
            }
            source.onComplete()
        }
    }

    fun retrieveMusic() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING

                val tracks = _source.iterator()
                while (tracks.hasNext()) {
                    val item = tracks.next()
                    val musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                    val mutableMediaMetadata = MutableMediaMetadata(musicId, item)
                    mMusicListById.put(musicId, mutableMediaMetadata)
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

        mutableMetadata.metadata = metadata
    }


    fun isInitialized(): Boolean {
        return mCurrentState == State.INITIALIZED
    }


    enum class State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private var mCurrentState: State = State.NON_INITIALIZED

    fun getShuffledMusic(): List<MediaMetadataCompat> {
        if (mCurrentState != State.INITIALIZED) {
            return emptyList()
        }
        val shuffled = ArrayList<MutableMediaMetadata>(mMusicListById.size)
        shuffled += mMusicListById.values
        Collections.shuffle(shuffled)
        return shuffled.map { mutableMediaMetadata -> mutableMediaMetadata.metadata }
    }

    fun searchMusicByAlbum(album: String?): Iterable<MediaMetadataCompat> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun searchMusicBySongTitle(query: String): Iterable<MediaMetadataCompat> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}
